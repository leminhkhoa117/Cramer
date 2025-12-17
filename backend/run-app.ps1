# PowerShell script to load .env and run Spring Boot application
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
        $line = $_.Trim()
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

# Check JAVA_HOME
if (-not $env:JAVA_HOME) {
    # Try common JDK paths on Windows
    $commonPaths = @(
        "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot",
        "C:\Program Files\Java\jdk-21",
        "C:\Program Files\Eclipse Adoptium\jdk-21*",
        "C:\Program Files\Microsoft\jdk-21*"
    )
    
    $foundJdk = $null
    foreach ($path in $commonPaths) {
        $resolved = Get-Item $path -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($resolved) {
            $foundJdk = $resolved.FullName
            break
        }
    }
    
    if ($foundJdk) {
        $env:JAVA_HOME = $foundJdk
        Write-Host "JAVA_HOME not set. Using detected: $foundJdk" -ForegroundColor Yellow
    } else {
        Write-Host "WARNING: JAVA_HOME is not set. Using system java..." -ForegroundColor Yellow
        try {
            java -version
        } catch {
            Write-Host "Java not found. Please install Java 21 and set JAVA_HOME." -ForegroundColor Red
            exit 1
        }
    }
} else {
    Write-Host "Using JAVA_HOME=$env:JAVA_HOME" -ForegroundColor Green
}

# Ensure Java binaries are on PATH
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Change to script directory
Set-Location $ScriptDir

$JarFile = Join-Path $ScriptDir "target\cramer-backend-0.0.1-SNAPSHOT.jar"

if (Test-Path $JarFile) {
    Write-Host "`nStarting Spring Boot from JAR: $JarFile" -ForegroundColor Green
    & java -jar $JarFile
} else {
    Write-Host "JAR not found. Building with Maven wrapper (may take a while)..." -ForegroundColor Yellow
    
    $MvnWrapper = Join-Path $ScriptDir "mvnw.cmd"
    
    if (Test-Path $MvnWrapper) {
        & $MvnWrapper -DskipTests clean package
    } elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
        mvn -DskipTests -f (Join-Path $ScriptDir "pom.xml") clean package
    } else {
        Write-Host "No Maven wrapper or system mvn found. Please install Maven or ensure 'mvnw.cmd' exists." -ForegroundColor Red
        exit 1
    }
    
    Write-Host "Build finished; running JAR..." -ForegroundColor Green
    & java -jar $JarFile
}
