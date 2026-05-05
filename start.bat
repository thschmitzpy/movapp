@echo off
title MovApp
set ROOT=%~dp0
set BACKEND_INICIADO=0

echo.
echo  ================================
echo   MovApp - Iniciando servicos...
echo  ================================
echo.

REM Verifica se o backend ja esta rodando na porta 8080
netstat -ano | findstr ":8080 " | findstr LISTENING >nul 2>&1
if %errorlevel%==0 (
    echo [Backend] Ja esta rodando na porta 8080.
) else (
    echo [Backend] Iniciando Spring Boot...
    start "MovApp - Backend" cmd /k "cd /d "%ROOT%" && mvnw.cmd spring-boot:run"
    set BACKEND_INICIADO=1
)

REM Verifica se o frontend ja esta rodando na porta 3000
netstat -ano | findstr ":3000 " | findstr LISTENING >nul 2>&1
if %errorlevel%==0 (
    echo [Frontend] Ja esta rodando na porta 3000.
) else (
    echo [Frontend] Iniciando React...
    start "MovApp - Frontend" cmd /k "cd /d "%ROOT%movapp-front" && npm start"
)

REM Se o backend foi recem iniciado, aguarda ele ficar disponivel antes de abrir o browser
if "%BACKEND_INICIADO%"=="1" (
    echo.
    echo  Aguardando o backend ficar disponivel na porta 8080...
    :AGUARDA_BACKEND
    timeout /t 3 /nobreak >nul
    netstat -ano | findstr ":8080 " | findstr LISTENING >nul 2>&1
    if %errorlevel%==1 goto AGUARDA_BACKEND
    echo  Backend disponivel!
)

echo.
echo  Aguardando o frontend ficar disponivel...
echo  (O navegador abrira automaticamente)
echo.

:AGUARDA_FRONTEND
timeout /t 3 /nobreak >nul
netstat -ano | findstr ":3000 " | findstr LISTENING >nul 2>&1
if %errorlevel%==1 goto AGUARDA_FRONTEND

echo  Frontend disponivel! Abrindo navegador...
start http://localhost:3000

echo.
echo  ================================
echo   MovApp rodando em:
echo   Frontend : http://localhost:3000
echo   Backend  : http://localhost:8080
echo  ================================
echo.
