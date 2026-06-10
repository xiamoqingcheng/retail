param(
    [string]$MySqlBin = $env:MYSQL_BIN,
    [string]$PythonExe = $env:PYTHON_EXE,
    [switch]$SkipAdmin
)

$script = Join-Path $PSScriptRoot "retail\start-all.ps1"

if (-not (Test-Path -LiteralPath $script)) {
    Write-Error "Startup script not found: $script"
    exit 1
}

& $script -MySqlBin $MySqlBin -PythonExe $PythonExe -SkipAdmin:$SkipAdmin
