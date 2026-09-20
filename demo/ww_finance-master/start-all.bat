@echo off
title WW-Finance One-Click Start

rem ============ EDIT THESE TWO PATHS ============
set REDIS_BIN=D:\SoftWare\Redis-6.2.21-Windows-x64-msys2\Redis-6.2.21-Windows-x64-msys2\redis-server.exe
set NACOS_DIR=D:\SoftWare\nacos-server-2.5.4\nacos\bin
rem ==============================================

set BACKEND_DIR=D:\financial-warehouse\demo\ww_finance-master
set FRONTEND_DIR=D:\financial-warehouse\demo\ww_finance_admin
set USER_FRONTEND_DIR=D:\financial-warehouse\demo\ww_finance_front

echo.
echo [1/7] Starting Redis (6379) ...
start "Redis" cmd /k "%REDIS_BIN%"

echo [2/7] Starting Nacos (8848) ...
start "Nacos" cmd /k "cd /d %NACOS_DIR% && call startup.cmd -m standalone"

echo [3/7] Starting backend finance-api (8990) ...
start "Backend" cmd /k "cd /d %BACKEND_DIR% && mvn -pl finance-api -am clean install -DskipTests -q && mvn -pl finance-api spring-boot:run"

echo [4/7] Starting admin frontend ww_finance_admin (5173) ...
start "AdminFE" cmd /k "cd /d %FRONTEND_DIR% && npm run dev"

echo [5/7] Starting mock bank ww_bank (9090) ...
start "MockBank" cmd /k "java -Xmx256m -jar %BACKEND_DIR%\ww_bank-1.0.0.jar"

echo [6/7] Starting ChromaDB vector DB (8000) ...
start "ChromaDB" cmd /k "call D:\SoftWare\miniconda3\Scripts\activate.bat rag && chroma run --path D:\chromadb_data --host 0.0.0.0 --port 8000"

echo [7/7] Starting user frontend ww_finance_front (5174) ...
start "UserFE" cmd /k "cd /d %USER_FRONTEND_DIR% && npm run dev"

echo.
echo All commands sent. Each service runs in its own window.
pause
