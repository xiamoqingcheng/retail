@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0retail-admin-pro"
set "NPM=%~dp0..\node-v24.14.1-win-x64\npm.cmd"
if exist "%NPM%" (
  call "%NPM%" run dev
) else (
  call npm run dev
)
endlocal
