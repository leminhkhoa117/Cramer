@echo off
REM Run backend locally on Windows. Loads variables from .env and prefers Maven wrapper.
SETLOCAL EnableDelayedExpansion

REM Resolve important directories
SET "SCRIPT_DIR=%~dp0"
FOR %%I IN ("%SCRIPT_DIR%\..") DO SET "ROOT=%%~fI"

REM Prefer repo root .env, fall back to backend/.env if necessary
SET "ENV_FILE=%ROOT%\.env"
IF NOT EXIST "%ENV_FILE%" IF EXIST "%SCRIPT_DIR%\.env" SET "ENV_FILE=%SCRIPT_DIR%\.env"

IF EXIST "%ENV_FILE%" (
  ECHO Loading environment variables from "%ENV_FILE%"
  CALL :LOAD_ENV "%ENV_FILE%"
) ELSE (
  ECHO [WARN] No .env file found at "%ROOT%\.env" or "%SCRIPT_DIR%\.env"
)

REM Add JAVA_HOME to PATH when provided via env file or existing env
IF DEFINED JAVA_HOME (
  ECHO Using JAVA_HOME=%JAVA_HOME%
  SET "PATH=%JAVA_HOME%\bin;%PATH%"
) ELSE (
  ECHO [WARN] JAVA_HOME not set. Falling back to system java.
)

REM Echo effective non-secret vars
IF DEFINED SPRING_DATASOURCE_URL ECHO Using SPRING_DATASOURCE_URL=%SPRING_DATASOURCE_URL%
IF DEFINED SPRING_DATASOURCE_USERNAME ECHO Using SPRING_DATASOURCE_USERNAME=%SPRING_DATASOURCE_USERNAME%
IF DEFINED SERVER_PORT ECHO Using SERVER_PORT=%SERVER_PORT%

REM Pick Maven wrapper if available
SET "MVN_WRAPPER=%SCRIPT_DIR%mvnw.cmd"
SET "USE_WRAPPER=0"
IF EXIST "%MVN_WRAPPER%" (
  SET "MVN_CMD=%MVN_WRAPPER%"
  SET "USE_WRAPPER=1"
) ELSE (
  SET "MVN_CMD=mvn"
  ECHO [WARN] Maven wrapper not found. Falling back to mvn on PATH.
)

ECHO.
ECHO Starting Spring Boot backend...
PUSHD "%SCRIPT_DIR%"
IF "%USE_WRAPPER%"=="1" (
  CALL "%MVN_CMD%" -DskipTests spring-boot:run
) ELSE (
  CALL %MVN_CMD% -DskipTests spring-boot:run
)
SET "EXIT_CODE=%ERRORLEVEL%"
POPD

IF NOT "%EXIT_CODE%"=="0" (
  ECHO [ERROR] Spring Boot exited with code %EXIT_CODE%.
)

ENDLOCAL
PAUSE
GOTO :EOF

:LOAD_ENV
SET "ENV_PATH=%~1"
FOR /F "usebackq tokens=* delims=" %%L IN ("%ENV_PATH%") DO (
  SET "line=%%L"
  FOR /F "tokens=*" %%T IN ("!line!") DO SET "line=%%T"
  IF NOT "!line!"=="" IF NOT "!line:~0,1!"=="#" (
    FOR /F "tokens=1* delims==" %%A IN ("!line!") DO CALL :SET_ENV "%%A" "%%B"
  )
)
EXIT /B

:SET_ENV
SET "key=%~1"
SET "key=%key: =%"
SET "value=%~2"
IF DEFINED value (
  SET "%key%=%value%"
) ELSE (
  SET "%key%="
)
EXIT /B
