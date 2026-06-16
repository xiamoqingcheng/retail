# 单独启动 AI 服务（FastAPI/uvicorn，端口 8000）。
# 可独立调试；总启动脚本 ..\..\start.ps1 也会复用本脚本。
# 覆盖 Python：设置环境变量 PYTHON_EXE 指向目标解释器。
$ErrorActionPreference = "Stop"

$ServiceDir = $PSScriptRoot
if ($env:PYTHON_EXE) {
    $Preferred = $env:PYTHON_EXE
} else {
    $Preferred = "D:\Documents\miniconda3\envs\train\python.exe"
}

if (Test-Path -LiteralPath $Preferred) {
    $Python = $Preferred
} elseif (Get-Command python -ErrorAction SilentlyContinue) {
    $Python = (Get-Command python).Source
} else {
    Write-Error "未找到 Python：请设置环境变量 PYTHON_EXE，或将 python 加入 PATH。"
    exit 1
}

Push-Location $ServiceDir
try {
    & $Python main.py
} finally {
    Pop-Location
}
