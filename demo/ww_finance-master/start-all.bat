@echo off
title WW-Finance One-Click Start

rem ============ EDIT THESE TWO PATHS ============
set REDIS_BIN=D:\SoftWare\Redis-6.2.21-Windows-x64-msys2\Redis-6.2.21-Windows-x64-msys2\redis-server.exe
set NACOS_DIR=D:\SoftWare\nacos-server-2.5.4\nacos\bin
rem ==============================================

set BACKEND_DIR=D:\financial-warehouse\demo\ww_finance-master
set FRONTEND_DIR=D:\financial-warehouse\demo\ww_finance_admin

echo.
echo [1/4] Starting Redis (6379) ...
start "Redis" cmd /k "%REDIS_BIN%"

echo [2/4] Starting Nacos (8848) ...
start "Nacos" cmd /k "cd /d %NACOS_DIR% && call startup.cmd -m standalone"

echo [3/4] Starting backend finance-api (8990) ...
start "Backend" cmd /k "cd /d %BACKEND_DIR% && mvn -pl finance-api -am clean install -DskipTests -q && mvn -pl finance-api spring-boot:run"

echo [4/4] Starting frontend ww_finance_admin (5173) ...
start "Frontend" cmd /k "cd /d %FRONTEND_DIR% && npm run dev"

echo.
echo All commands sent. Each service runs in its own window.
pause
