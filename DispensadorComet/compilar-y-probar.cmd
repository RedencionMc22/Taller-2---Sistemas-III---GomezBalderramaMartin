@echo off
rem ---------------------------------------------------------------------------
rem  Compila el proyecto y ejecuta las pruebas JUnit 5 sin necesidad de Maven.
rem  Requiere un JDK 17 o superior en el PATH.
rem ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0"

set JUNIT=lib\junit-platform-console-standalone-1.11.4.jar
if not exist "%JUNIT%" (
	echo [ERROR] No se encuentra %JUNIT%
	exit /b 1
)

if not exist build\classes mkdir build\classes
if not exist build\test-classes mkdir build\test-classes

echo Compilando clases de produccion...
powershell -NoProfile -Command "$m = Get-ChildItem -Recurse -File src\main\java\*.java | ForEach-Object { $_.FullName }; & javac --release 17 -encoding UTF-8 -d build\classes $m"
if errorlevel 1 exit /b 1

echo Compilando clases de prueba...
powershell -NoProfile -Command "$m = Get-ChildItem -Recurse -File src\test\java\*.java | ForEach-Object { $_.FullName }; & javac --release 17 -encoding UTF-8 -cp ('build\classes;' + (Resolve-Path '%JUNIT%')) -d build\test-classes $m"
if errorlevel 1 exit /b 1

echo.
echo Ejecutando pruebas...
java -jar "%JUNIT%" execute --class-path "build\classes;build\test-classes" --scan-class-path build\test-classes --details=summary --reports-dir build\reports --disable-ansi-colors
exit /b %errorlevel%
