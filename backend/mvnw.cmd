@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM
@REM Optional ENV vars
@REM   MVNW_REPOURL - repo url base for downloading maven distribution
@REM   MVNW_USERNAME/MVNW_PASSWORD - user and password for downloading maven
@REM   MVNW_VERBOSE - true: enable verbose log; others: silence the output
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET __MVNW_CMD__=
@SET __MVNW_ERROR__=
@SET __MVNW_PSMODULEP_SAVE=%PSModulePath%
@SET PSModulePath=
@FOR /F "usebackq tokens=1* delims==" %%A IN (`SET`) DO @IF "%%A"=="JAVA_HOME" (SET __MVNW_CMD__=%%B)
@SET PSModulePath=%__MVNW_PSMODULEP_SAVE%
@SET __MVNW_PSMODULEP_SAVE=

@IF "%__MVNW_CMD__%"=="" (
    @FOR /F "usebackq delims=" %%i IN (`where java 2^>nul`) DO @SET "__MVNW_CMD__=%%i"
)

@IF "%__MVNW_CMD__%"=="" (
    @ECHO ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
    @ECHO Please set the JAVA_HOME variable in your environment to match the
    @ECHO location of your Java installation.
    @GOTO error
)

@SET __MVNW_BASEDIR__=%~dp0
@SET __MVNW_BASEDIR__=%__MVNW_BASEDIR__:~0,-1%
@IF NOT EXIST "%__MVNW_BASEDIR__%\.mvn\wrapper\maven-wrapper.jar" (
    @IF "%MVNW_VERBOSE%"=="true" (
        @ECHO Couldn't find %__MVNW_BASEDIR__%\.mvn\wrapper\maven-wrapper.jar, downloading it ...
        @ECHO Downloading from: %MVNW_REPOURL%
    )
    @powershell -NoProfile -ExecutionPolicy Bypass -Command "& {$scriptDir='%__MVNW_BASEDIR__%'; $jarFile='%__MVNW_BASEDIR__%\.mvn\wrapper\maven-wrapper.jar'; $wrapperUrl='https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar'; if (Test-Path $jarFile) { Write-Host 'Found maven-wrapper.jar'; } else { Write-Host 'Downloading maven-wrapper.jar...'; [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri $wrapperUrl -OutFile $jarFile; Write-Host 'Finished downloading maven-wrapper.jar'; }}"
    @IF ERRORLEVEL 1 (
        @ECHO ERROR: Failed to download maven-wrapper.jar
        @GOTO error
    )
)

@SET __MVNW_WRAPPER_JAR__=%__MVNW_BASEDIR__%\.mvn\wrapper\maven-wrapper.jar
@SET __MVNW_MAVEN_PROPS__=%__MVNW_BASEDIR__%\.mvn\wrapper\maven-wrapper.properties

"%__MVNW_CMD__%" %JVM_CONFIG_MAVEN_PROPS% %MAVEN_OPTS% -classpath "%__MVNW_WRAPPER_JAR__%" "-Dmaven.multiModuleProjectDirectory=%__MVNW_BASEDIR__%" org.apache.maven.wrapper.MavenWrapperMain %*
@IF ERRORLEVEL 1 GOTO error
@GOTO end

:error
@SET __MVNW_ERROR__=1

:end
@endlocal & set ERROR_CODE=%__MVNW_ERROR__%

@IF NOT "%MAVEN_SKIP_RC%"=="" @GOTO skipRcPost
@REM check for post script, once with legacy .bat ending and once with .cmd ending
@IF EXIST "%USERPROFILE%\mavenrc_post.bat" @CALL "%USERPROFILE%\mavenrc_post.bat"
@IF EXIST "%USERPROFILE%\mavenrc_post.cmd" @CALL "%USERPROFILE%\mavenrc_post.cmd"
:skipRcPost

@REM pause the script if MAVEN_BATCH_PAUSE is set to 'on'
@IF "%MAVEN_BATCH_PAUSE%"=="on" PAUSE

@IF "%MAVEN_TERMINATE_CMD%"=="on" EXIT %ERROR_CODE%

@cmd /C EXIT /B %ERROR_CODE%
