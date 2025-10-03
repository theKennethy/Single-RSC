@echo off
REM Build Single-RSC with Maven
REM This will compile all code, download dependencies, and create JAR files

echo =========================================
echo   Building Single-RSC with Maven
echo =========================================
echo.

REM Check if Maven is installed
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven is not installed!
    echo Please install Maven first from: https://maven.apache.org/download.cgi
    exit /b 1
)

REM Run Maven build
echo Running: mvn clean package
echo.
mvn clean package

REM Check if build succeeded
if %ERRORLEVEL% EQU 0 (
    echo.
    echo =========================================
    echo   Build successful!
    echo =========================================
    echo.
    echo Output files:
    echo   target\rsc.jar (2 MB^)
    echo   target\rsc-standalone.jar (48 MB^)
    echo.
    echo To run the game:
    echo   run-fx.bat
    echo.
) else (
    echo.
    echo =========================================
    echo   Build failed!
    echo =========================================
    echo.
    echo Please check the error messages above.
    exit /b 1
)
