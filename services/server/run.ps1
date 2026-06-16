# 单独启动后端 API 服务（Spring Boot，端口 8080）。
# 可独立调试；总启动脚本 ..\..\start.ps1 也会复用本脚本。
$ErrorActionPreference = "Stop"

$ServiceDir = $PSScriptRoot
$RepoRoot = Split-Path -Parent (Split-Path -Parent $ServiceDir)
$BundledMaven = Join-Path $RepoRoot "apache-maven-3.9.14\bin\mvn.cmd"

if (Test-Path -LiteralPath $BundledMaven) {
    $Maven = $BundledMaven
} elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
    $Maven = (Get-Command mvn).Source
} else {
    Write-Error "未找到 Maven：请使用仓库根目录的 apache-maven-3.9.14，或将 mvn 加入 PATH。"
    exit 1
}

Push-Location $ServiceDir
try {
    & $Maven spring-boot:run -q
} finally {
    Pop-Location
}
