# PowerShell script to build the Spring Boot application using the correct JDK.
# This script bypasses issues with mvnw.cmd and environment variables.

Write-Host "Building the backend project..." -ForegroundColor Cyan

# Define the path to the correct JDK's java.exe
$JdkJavaExe = "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot\bin\java.exe"

# Define paths for the Maven wrapper
$MavenWrapperJar = ".\.mvn\wrapper\maven-wrapper.jar"
$ProjectDir = (Get-Location).Path

# Check if the JDK exists
if (-not (Test-Path $JdkJavaExe)) {
    Write-Host "Error: JDK not found at $JdkJavaExe" -ForegroundColor Red
    exit 1
}

# Construct and run the build command
& $JdkJavaExe -classpath $MavenWrapperJar "-Dmaven.multiModuleProjectDirectory=$ProjectDir" org.apache.maven.wrapper.MavenWrapperMain clean install

# Check the exit code of the last command
if ($LastExitCode -eq 0) {
    Write-Host "BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host "You can now run the application using '.\run-app.ps1'"
} else {
    Write-Host "BUILD FAILED. Please check the output above for errors." -ForegroundColor Red
}
