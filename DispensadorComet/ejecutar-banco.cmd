@echo off
rem ---------------------------------------------------------------------------
rem  Abre el banco de pruebas de las actividades 1 y 2.
rem  Requiere haber compilado antes (NetBeans o compilar-y-probar.cmd).
rem ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0"

if not exist build\classes (
	echo [ERROR] Todavia no hay clases compiladas. Ejecute compilar-y-probar.cmd
	exit /b 1
)

start "Banco de pruebas - Taller 2" javaw -cp build\classes ar.edu.unse.sistemas3.banco.BancoPruebas
