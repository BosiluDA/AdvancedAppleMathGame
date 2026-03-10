@echo off
echo ================================================
echo  Apple Math Puzzle - Build and Run
echo ================================================

set SRC=src
set OUT=out
set MAIN=applegame.Main

if not exist "%OUT%" mkdir "%OUT%"

echo Compiling Java sources...
javac -encoding UTF-8 -sourcepath "%SRC%" -d "%OUT%" "%SRC%\applegame\*.java"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Compilation failed. Make sure JDK 11+ is installed and javac is on your PATH.
    pause
    exit /b 1
)

echo.
echo Compilation successful!
echo Starting game...
echo.
java -Dfile.encoding=UTF-8 -cp "%OUT%" %MAIN%
pause
