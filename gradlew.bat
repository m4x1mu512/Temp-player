@echo off
setlocal

set "APP_HOME=%~dp0"
set "PROPERTIES=%APP_HOME%gradle\wrapper\gradle-wrapper.properties"

if not exist "%PROPERTIES%" (
  echo Gradle wrapper properties not found: "%PROPERTIES%" 1>&2
  exit /b 1
)

for /f "tokens=1,* delims==" %%A in ('findstr /b "distributionUrl=" "%PROPERTIES%"') do set "DISTRIBUTION_URL=%%B"
set "DISTRIBUTION_URL=%DISTRIBUTION_URL:\: =%"
set "DISTRIBUTION_URL=%DISTRIBUTION_URL:\:=:%"

for %%A in ("%DISTRIBUTION_URL%") do set "ARCHIVE_NAME=%%~nxA"
set "DIST_NAME=%ARCHIVE_NAME:.zip=%"
set "GRADLE_VERSION=%DIST_NAME:gradle-=%"
set "GRADLE_VERSION=%GRADLE_VERSION:-bin=%"

if not defined GRADLE_USER_HOME set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "DIST_ROOT=%GRADLE_USER_HOME%\wrapper\dists\%DIST_NAME%-bootstrap"
set "GRADLE_HOME=%DIST_ROOT%\%DIST_NAME%"

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  if not exist "%DIST_ROOT%" mkdir "%DIST_ROOT%"
  echo Downloading Gradle %GRADLE_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%DIST_ROOT%\gradle.zip'; Expand-Archive -LiteralPath '%DIST_ROOT%\gradle.zip' -DestinationPath '%DIST_ROOT%' -Force; Remove-Item -LiteralPath '%DIST_ROOT%\gradle.zip' -Force"
  if errorlevel 1 exit /b %errorlevel%
)

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  echo Gradle distribution was not extracted correctly. 1>&2
  exit /b 1
)

call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %errorlevel%
