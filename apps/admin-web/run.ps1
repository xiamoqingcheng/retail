# 单独启动管理端（Vite dev server，端口 8848）。
# 可独立调试；总启动脚本 ..\..\start.ps1 也会复用本脚本。
$ErrorActionPreference = "Stop"

$AppDir = $PSScriptRoot
$RepoRoot = Split-Path -Parent (Split-Path -Parent $AppDir)
$BundledNpm = Join-Path $RepoRoot "node-v24.14.1-win-x64\npm.cmd"

if (Test-Path -LiteralPath $BundledNpm) {
    $Npm = $BundledNpm
} elseif (Get-Command npm -ErrorAction SilentlyContinue) {
    $Npm = (Get-Command npm).Source
} else {
    Write-Error "未找到 npm：请使用仓库根目录的 node-v24.14.1-win-x64，或将 npm 加入 PATH。"
    exit 1
}

if (-not (Test-Path -LiteralPath (Join-Path $AppDir "node_modules"))) {
    Write-Host "首次运行：安装依赖 (npm install) ..." -ForegroundColor Yellow
    & $Npm install
}

Push-Location $AppDir
try {
    & $Npm run dev
} finally {
    Pop-Location
}
