@echo off
setlocal
if "%JAVAFX%"=="" (
  echo Set JAVAFX to your JavaFX SDK lib directory, e.g. C:\openjfx\lib
  exit /b 1
)
if not exist out mkdir out
dir /s /b src\*.java > "%TEMP%\aicsl_sources.txt"
javac -d out --module-path "%JAVAFX%" --add-modules javafx.controls -cp "lib\*" @"%TEMP%\aicsl_sources.txt"
if errorlevel 1 exit /b 1
echo Build OK