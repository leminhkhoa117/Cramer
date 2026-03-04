#!/usr/bin/env bash
# Linux/Bash helper to load root .env and run the Spring Boot app
# Automatically rebuilds if source files are newer than the JAR
set -euo pipefail

# Resolve script/root directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"

echo "Running backend (Linux helper) from $SCRIPT_DIR"

if [[ -f "$ENV_FILE" ]]; then
  echo "Loading environment variables from $ENV_FILE"

  # If dos2unix is present, convert line endings in-place to avoid CRLF issues
  if command -v dos2unix >/dev/null 2>&1; then
    dos2unix "$ENV_FILE" >/dev/null 2>&1 || true
  fi

  # Read file line-by-line and export key=value pairs safely
  while IFS= read -r line || [[ -n "$line" ]]; do
    # Remove any trailing CR (from CRLF)
    line=${line%%$'\r'}
    # trim leading/trailing whitespace
    line="${line##[[:space:]]}"
    line="${line%%[[:space:]]}"
    # Skip empty lines and comments
    [[ -z "$line" || ${line:0:1} == '#' ]] && continue
    # Parse KEY=VALUE (support values containing =)
    if [[ "$line" =~ ^([^=]+)=(.*)$ ]]; then
      name="${BASH_REMATCH[1]}"
      value="${BASH_REMATCH[2]}"
      # Strip surrounding quotes if present
      if [[ "$value" =~ ^\"(.*)\"$ ]]; then
        value="${BASH_REMATCH[1]}"
      elif [[ "$value" =~ ^\'(.*)\'$ ]]; then
        value="${BASH_REMATCH[1]}"
      fi
      # Export variable for child processes
      export "$name=$value"
      # Mask secret-like var names in logs
      upname=${name^^}
      if [[ "$upname" == *KEY* || "$upname" == *SECRET* || "$upname" == *PASSWORD* || "$upname" == *TOKEN* || "$upname" == *PRIVATE* ]]; then
        echo "  Exported (masked) $name"
      else
        echo "  Exported $name"
      fi
    fi
  done < "$ENV_FILE"
else
  echo "Warning: .env not found at $ENV_FILE" >&2
fi

# Ensure JAVA_HOME is set or warn
if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "WARNING: JAVA_HOME is not set. Using system java (java -version output below)" >&2
  java -version || true
else
  export PATH="$JAVA_HOME/bin:$PATH"
  echo "Using JAVA_HOME=$JAVA_HOME"
fi

cd "$SCRIPT_DIR"

JAR_FILE="$SCRIPT_DIR/target/cramer-backend-0.0.1-SNAPSHOT.jar"
SRC_DIR="$SCRIPT_DIR/src"
SRC_HASH_FILE="$SCRIPT_DIR/target/.src-hash"

# Function to compute hash of source file list (detects additions/deletions)
compute_src_hash() {
  find "$SRC_DIR" -type f \( -name "*.java" -o -name "*.xml" -o -name "*.properties" -o -name "*.yml" -o -name "*.yaml" \) 2>/dev/null | sort | md5sum | cut -d' ' -f1
}

# Function to check if rebuild is needed
needs_rebuild() {
  # If JAR doesn't exist, definitely rebuild
  if [[ ! -f "$JAR_FILE" ]]; then
    echo "JAR not found."
    return 0
  fi

  # Check if source file list changed (detects additions AND deletions)
  local current_hash
  current_hash=$(compute_src_hash)
  if [[ -f "$SRC_HASH_FILE" ]]; then
    local stored_hash
    stored_hash=$(cat "$SRC_HASH_FILE")
    if [[ "$current_hash" != "$stored_hash" ]]; then
      echo "Source file structure changed (files added or deleted)."
      return 0
    fi
  else
    echo "No source hash found (first run or target cleaned)."
    return 0
  fi

  # Check if any source file is newer than JAR
  local newer_files
  newer_files=$(find "$SRC_DIR" -type f \( -name "*.java" -o -name "*.xml" -o -name "*.properties" -o -name "*.yml" -o -name "*.yaml" \) -newer "$JAR_FILE" 2>/dev/null | head -5)
  
  if [[ -n "$newer_files" ]]; then
    echo "Source files changed since last build:"
    echo "$newer_files" | while read -r f; do echo "  - ${f#$SCRIPT_DIR/}"; done
    return 0
  fi

  # Check if pom.xml is newer than JAR
  if [[ "$SCRIPT_DIR/pom.xml" -nt "$JAR_FILE" ]]; then
    echo "pom.xml changed since last build."
    return 0
  fi

  return 1
}

# Function to build the JAR
build_jar() {
  echo ""
  echo "Building with Maven (may take a while)..."
  if [[ -x "$SCRIPT_DIR/mvnw" ]]; then
    "$SCRIPT_DIR/mvnw" -DskipTests clean package
  elif command -v mvn >/dev/null 2>&1; then
    mvn -DskipTests -f "$SCRIPT_DIR/pom.xml" clean package
  else
    echo "No Maven wrapper or system mvn found. Please install Maven or make 'mvnw' executable." >&2
    exit 1
  fi
  
  # Save source hash after successful build
  compute_src_hash > "$SRC_HASH_FILE"
  echo "Build finished! Source hash saved."
}

# Check if rebuild is needed
if needs_rebuild; then
  build_jar
else
  echo "JAR is up-to-date. Skipping build."
fi

# Run the JAR
echo ""
echo "Starting Spring Boot from JAR: $JAR_FILE"
exec java -jar "$JAR_FILE"
