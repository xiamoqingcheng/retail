@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0retail-ai"
set "PYTHON_EXE=D:\miniconda3\python.exe"
if exist "%PYTHON_EXE%" (
  "%PYTHON_EXE%" main.py
) else (
  python main.py
)
endlocal
