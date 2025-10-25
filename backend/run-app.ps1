# PowerShell script to load .env and run Spring Boot application
# Usage: .\run-app.ps1

$ErrorActionPreference = "Stop"

# Set JAVA_HOME only if not already configured
if (-not $env:JAVA_HOME) {
    $defaultJdk = "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot"
    if (Test-Path $defaultJdk) {
        $env:JAVA_HOME = $defaultJdk
        Write-Host "JAVA_HOME not set. Using default: $defaultJdk" -ForegroundColor Yellow
    } else {
        Write-Host "JAVA_HOME not set and default JDK path not found. Ensure Java 21 is installed and JAVA_HOME is configured." -ForegroundColor Red
        exit 1
    }
}

# Ensure Java binaries are on PATH
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Get script directory
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir

# Load .env file from root directory
$EnvFile = Join-Path $RootDir ".env"

if (Test-Path $EnvFile) {
    Write-Host "Loading environment variables from $EnvFile" -ForegroundColor Green
    Get-Content $EnvFile | ForEach-Object {
        $line = $_.Trim()
        # Skip comments and empty lines
        if ($line -and -not $line.StartsWith('#')) {
            if ($line -match '^([^=]+)=(.*)$') {
                $name = $matches[1].Trim()
                $value = $matches[2].Trim()
                [Environment]::SetEnvironmentVariable($name, $value, 'Process')
                Write-Host "  Set $name" -ForegroundColor DarkGray
            }
        }
    }
} else {
    Write-Host "Warning: .env file not found at $EnvFile" -ForegroundColor Yellow
}

# Run the application
$JarFile = Join-Path $ScriptDir "target\cramer-backend-0.0.1-SNAPSHOT.jar"

if (Test-Path $JarFile) {
    Write-Host "`nStarting Spring Boot application..." -ForegroundColor Green
    & "$env:JAVA_HOME\bin\java.exe" -jar $JarFile
} else {
    Write-Host "Error: JAR file not found at $JarFile" -ForegroundColor Red
    Write-Host "Please build the project first with: .\mvnw.cmd clean package -DskipTests" -ForegroundColor Yellow
    exit 1
}
