# PowerShell script to load .env and run Spring Boot application
# Automatically rebuilds if source files are newer than the JAR
# Usage: .\run-app.ps1
# Mirrors the behavior of run-app.sh for Linux

$ErrorActionPreference = "Stop"

# Get script directory and root directory
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir
$EnvFile = Join-Path $RootDir ".env"

Write-Host "Running backend (Windows helper) from $ScriptDir" -ForegroundColor Cyan

# Load .env file from root directory
if (Test-Path $EnvFile) {
    Write-Host "Loading environment variables from $EnvFile" -ForegroundColor Green
    Get-Content $EnvFile | ForEach-Object {
        # IMPORTANT: Remove carriage return (\r) characters that may be present in CRLF files
        # PowerShell's Trim() does NOT remove \r, which causes JWT secret mismatch!
        $line = $_ -replace "`r", ""
        $line = $line.Trim()
        # Skip comments and empty lines
        if ($line -and -not $line.StartsWith('#')) {
            if ($line -match '^([^=]+)=(.*)$') {
                $name = $matches[1].Trim()
                $value = $matches[2].Trim()
                
                # Strip surrounding quotes if present (single or double)
                if ($value -match '^"(.*)"$') {
                    $value = $matches[1]
                } elseif ($value -match "^'(.*)'$") {
                    $value = $matches[1]
                }
                
                # Safety: ensure no hidden \r characters in value
                $value = $value -replace "`r", ""
                
                [Environment]::SetEnvironmentVariable($name, $value, 'Process')
                
                # Mask secret-like var names in logs
                $upname = $name.ToUpper()
                if ($upname -like "*KEY*" -or $upname -like "*SECRET*" -or $upname -like "*PASSWORD*" -or $upname -like "*TOKEN*" -or $upname -like "*PRIVATE*") {
                    Write-Host "  Exported (masked) $name" -ForegroundColor DarkGray
                } else {
                    Write-Host "  Exported $name" -ForegroundColor DarkGray
                }
            }
        }
    }
} else {
    Write-Host "Warning: .env file not found at $EnvFile" -ForegroundColor Yellow
}

# Determine required JDK major version from pom.xml (defaults to 25)
$RequiredJdkMajor = 25
try {
    $pomContent = Get-Content (Join-Path $ScriptDir "pom.xml") -Raw
    if ($pomContent -match '<java\.version>\s*(\d+)\s*</java\.version>') {
        $RequiredJdkMajor = [int]$matches[1]
    }
} catch { }

# Validate JAVA_HOME if set; if it points to a wrong major version, ignore it and re-detect
function Get-JdkMajor($jdkHome) {
    try {
        $releaseFile = Join-Path $jdkHome "release"
        if (Test-Path $releaseFile) {
            $line = (Get-Content $releaseFile | Where-Object { $_ -match '^JAVA_VERSION=' } | Select-Object -First 1)
            if ($line -and $line -match 'JAVA_VERSION="?(\d+)') { return [int]$matches[1] }
        }
    } catch { }
    return $null
}

if ($env:JAVA_HOME) {
    $major = Get-JdkMajor $env:JAVA_HOME
    if ($major -and $major -lt $RequiredJdkMajor) {
        Write-Host "JAVA_HOME points to JDK $major but pom requires $RequiredJdkMajor. Searching for a newer JDK..." -ForegroundColor Yellow
        $env:JAVA_HOME = $null
    }
}

if (-not $env:JAVA_HOME) {
    # Build search list: prefer required major and above, then fall back to common paths
    $searchRoots = @(
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Java",
        "C:\Program Files\Microsoft",
        "C:\Program Files\Zulu",
        "C:\Program Files\Amazon Corretto"
    )

    $candidates = @()
    foreach ($root in $searchRoots) {
        if (Test-Path $root) {
            $candidates += Get-ChildItem $root -Directory -ErrorAction SilentlyContinue | ForEach-Object {
                $m = Get-JdkMajor $_.FullName
                if ($m) { [pscustomobject]@{ Path = $_.FullName; Major = $m } }
            }
        }
    }

    # Pick the highest major >= required; else highest available
    $best = $candidates | Where-Object { $_.Major -ge $RequiredJdkMajor } | Sort-Object Major -Descending | Select-Object -First 1
    if (-not $best) { $best = $candidates | Sort-Object Major -Descending | Select-Object -First 1 }

    if ($best) {
        $env:JAVA_HOME = $best.Path
        if ($best.Major -lt $RequiredJdkMajor) {
            Write-Host "WARNING: Only JDK $($best.Major) found; pom requires $RequiredJdkMajor. Build will fail unless you install JDK $RequiredJdkMajor." -ForegroundColor Red
        } else {
            Write-Host "JAVA_HOME not set. Using detected JDK $($best.Major): $($best.Path)" -ForegroundColor Yellow
        }
    } else {
        Write-Host "Java not found. Please install JDK $RequiredJdkMajor and set JAVA_HOME." -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "Using JAVA_HOME=$env:JAVA_HOME" -ForegroundColor Green
}

# Ensure Java binaries are on PATH
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Change to script directory
Set-Location $ScriptDir

$JarFile = Join-Path $ScriptDir "target\cramer-backend-0.0.1-SNAPSHOT.jar"
$SrcDir = Join-Path $ScriptDir "src"
$PomFile = Join-Path $ScriptDir "pom.xml"
$SrcHashFile = Join-Path $ScriptDir "target\.src-hash"

# Function to compute hash of source file list (detects additions/deletions)
function Get-SrcHash {
    $files = Get-ChildItem -Path $SrcDir -Recurse -Include "*.java", "*.xml", "*.properties", "*.yml", "*.yaml" -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty FullName |
        Sort-Object
    $joined = $files -join "`n"
    $md5 = [System.Security.Cryptography.MD5]::Create()
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($joined)
    $hash = $md5.ComputeHash($bytes)
    return [BitConverter]::ToString($hash) -replace '-', ''
}

# Function to check if rebuild is needed
function Test-NeedsRebuild {
    # If JAR doesn't exist, definitely rebuild
    if (-not (Test-Path $JarFile)) {
        Write-Host "JAR not found." -ForegroundColor Yellow
        return $true
    }

    # Check if source file list changed (detects additions AND deletions)
    $currentHash = Get-SrcHash
    if (Test-Path $SrcHashFile) {
        $storedHash = Get-Content $SrcHashFile -Raw
        $storedHash = $storedHash.Trim()
        if ($currentHash -ne $storedHash) {
            Write-Host "Source file structure changed (files added or deleted)." -ForegroundColor Yellow
            return $true
        }
    } else {
        Write-Host "No source hash found (first run or target cleaned)." -ForegroundColor Yellow
        return $true
    }

    $jarTime = (Get-Item $JarFile).LastWriteTime

    # Check if pom.xml is newer than JAR
    if ((Get-Item $PomFile).LastWriteTime -gt $jarTime) {
        Write-Host "pom.xml changed since last build." -ForegroundColor Yellow
        return $true
    }

    # Check if any source file is newer than JAR
    $newerFiles = Get-ChildItem -Path $SrcDir -Recurse -Include "*.java", "*.xml", "*.properties", "*.yml", "*.yaml" -ErrorAction SilentlyContinue |
        Where-Object { $_.LastWriteTime -gt $jarTime } |
        Select-Object -First 5

    if ($newerFiles) {
        Write-Host "Source files changed since last build:" -ForegroundColor Yellow
        $newerFiles | ForEach-Object {
            $relativePath = $_.FullName.Substring($ScriptDir.Length + 1)
            Write-Host "  - $relativePath" -ForegroundColor DarkYellow
        }
        return $true
    }

    return $false
}

# Function to build the JAR
function Build-Jar {
    Write-Host ""
    Write-Host "Building with Maven (may take a while)..." -ForegroundColor Yellow
    
    $MvnWrapper = Join-Path $ScriptDir "mvnw.cmd"
    
    if (Test-Path $MvnWrapper) {
        & $MvnWrapper -DskipTests clean package
    } elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
        mvn -DskipTests -f $PomFile clean package
    } else {
        Write-Host "No Maven wrapper or system mvn found. Please install Maven or ensure 'mvnw.cmd' exists." -ForegroundColor Red
        exit 1
    }
    
    # Save source hash after successful build
    Get-SrcHash | Out-File -FilePath $SrcHashFile -Encoding UTF8 -NoNewline
    Write-Host "Build finished! Source hash saved." -ForegroundColor Green
}

# Check if rebuild is needed
if (Test-NeedsRebuild) {
    Build-Jar
} else {
    Write-Host "JAR is up-to-date. Skipping build." -ForegroundColor Green
}

# Run the JAR
Write-Host ""
Write-Host "Starting Spring Boot from JAR: $JarFile" -ForegroundColor Green
& java -jar $JarFile
