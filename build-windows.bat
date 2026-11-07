@echo off
setlocal
title Arachne Plus - build and install
cd /d "%~dp0"
echo.
echo  ==========================================
echo    Arachne Plus - build and install
echo  ==========================================
echo.

where java >nul 2>nul
if errorlevel 1 goto needjava
set "JV="
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do if not defined JV set "JV=%%~v"
for /f "tokens=1 delims=." %%a in ("%JV%") do set "JMAJOR=%%a"
if "%JMAJOR%"=="" goto needjava
if %JMAJOR% LSS 25 goto needjava
echo Java %JV% found. Good.
goto build

:needjava
echo Minecraft 26.2 mods need Java 25 to build, and this computer does not have it yet.
echo.
where winget >nul 2>nul
if errorlevel 1 goto manualjava
echo I can install it for you. It is the free, official "Eclipse Temurin" Java.
choice /m "Install Java 25 now"
if errorlevel 2 goto manualjava
winget install -e --id EclipseAdoptium.Temurin.25.JDK --accept-package-agreements --accept-source-agreements
echo.
echo Java is installed. Now CLOSE this window and double-click build-windows.bat again.
pause
exit /b 0

:manualjava
echo.
echo Please install "Temurin 25 (JDK)" from the page that is about to open,
echo then close this window and double-click build-windows.bat again.
start "" "https://adoptium.net/temurin/releases/?version=25"
pause
exit /b 1

:build
echo.
echo Building the mod. The first time this downloads a lot, so it can take
echo several minutes. Please wait...
echo.
call gradlew.bat build
if errorlevel 1 goto failed

set "JAR="
for %%f in (build\libs\arachneplus-*.jar) do (
  echo %%~nxf | findstr /i /c:"-sources" >nul
  if errorlevel 1 set "JAR=%%f"
)
if not defined JAR goto failed

set "MODS=%APPDATA%\.minecraft\mods"
if not exist "%MODS%" mkdir "%MODS%"
del /q "%MODS%\arachneplus-*.jar" >nul 2>nul
copy /y "%JAR%" "%MODS%\" >nul
echo.
echo Installed: %JAR%
echo        to: %MODS%

dir /b "%MODS%\fabric-api-*.jar" >nul 2>nul
if errorlevel 1 (
  echo.
  echo Fabric API is also needed. Downloading it for you...
  powershell -NoProfile -ExecutionPolicy Bypass -File "tools\get-fabric-api.ps1" "%MODS%"
  if errorlevel 1 echo Please download Fabric API for 26.2 yourself from https://modrinth.com/mod/fabric-api and put it in the mods folder.
)

echo.
echo  ==========================================
echo    ALL DONE
echo    Start Minecraft with your Fabric 26.2 profile.
echo    In game, press Right Shift for the settings screen.
echo  ==========================================
start "" "%MODS%"
pause
exit /b 0

:failed
echo.
echo Something went wrong with the build.
echo Please copy the red error text above and report it to the mod author.
pause
exit /b 1
