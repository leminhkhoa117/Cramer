#!/usr/bin/env bash
# Linux/Bash helper to load root .env and run the Spring Boot app like the Windows script
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

if [[ -f "$JAR_FILE" ]]; then
  echo "Starting Spring Boot from JAR: $JAR_FILE"
  exec java -jar "$JAR_FILE"
else
  echo "JAR not found. Building with Maven wrapper (may take a while)..."
  if [[ -x "$SCRIPT_DIR/mvnw" ]]; then
    "$SCRIPT_DIR/mvnw" -DskipTests clean package
  elif command -v mvn >/dev/null 2>&1; then
    mvn -DskipTests -f "$SCRIPT_DIR/pom.xml" clean package
  else
    echo "No Maven wrapper or system mvn found. Please install Maven or make 'mvnw' executable." >&2
    exit 1
  fi
  echo "Build finished; running JAR..."
  exec java -jar "$JAR_FILE"
fi
