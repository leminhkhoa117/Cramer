@echo off
REM Run backend locally on Windows. Loads variables from repo root .env file if present.
SETLOCAL EnableDelayedExpansion

REM determine repo root (one level up from backend folder)
SET SCRIPT_DIR=%~dp0
FOR %%I IN ("%SCRIPT_DIR%\..") DO SET "ROOT=%%~fI"

REM load .env if exists
IF EXIST "%ROOT%\.env" (
  FOR /F "usebackq tokens=* delims=" %%L IN ("%ROOT%\.env") DO (
    SET "line=%%L"
    REM trim leading spaces
    FOR /F "tokens=*" %%T IN ("!line!") DO SET "line=%%T"
    REM skip comments and empty lines
    IF NOT "!line!"=="" IF NOT "!line:~0,1!"=="#" (
      REM split at first '=' into key and value (value may contain =)
      FOR /F "tokens=1* delims==" %%A IN ("!line!") DO (
        SET "key=%%A"
        SET "val=%%B"
        SET "key=!key: =!"
        IF DEFINED val (
          SET "!key!=!val!"
        )
      )
    )
  )
) ELSE (
  ECHO No .env found at "%ROOT%\.env"
)

  REM Echo effective vars (without printing password)
ECHO Using SPRING_DATASOURCE_URL=%SPRING_DATASOURCE_URL%
ECHO Using SPRING_DATASOURCE_USERNAME=%SPRING_DATASOURCE_USERNAME%
ECHO Using SERVER_PORT=%SERVER_PORT%

  REM Run the app with mvn (skip tests)
cd /d "%~dp0"
mvn -DskipTests spring-boot:run
IF ERRORLEVEL 1 (
  ECHO Maven/spring-boot failed with errorlevel %ERRORLEVEL%.
)
ENDLOCAL

PAUSE
