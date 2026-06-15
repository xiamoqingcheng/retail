$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$BuildDir = Join-Path $Root 'build'
$CMake = (Get-Command cmake -ErrorAction Stop).Source
$Cxx = (Get-Command g++ -ErrorAction Stop).Source
$Ninja = (Get-Command ninja -ErrorAction SilentlyContinue).Source

function Invoke-Checked {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments
    )
    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code ${LASTEXITCODE}: $FilePath $($Arguments -join ' ')"
    }
}

if ($Ninja) {
    Invoke-Checked $CMake -S $Root -B $BuildDir -G Ninja "-DCMAKE_MAKE_PROGRAM=$Ninja" "-DCMAKE_CXX_COMPILER=$Cxx" -DCMAKE_BUILD_TYPE=Release
} else {
    $Make = Join-Path (Split-Path -Parent $Cxx) 'make.exe'
    Invoke-Checked $CMake -S $Root -B $BuildDir -G 'MinGW Makefiles' "-DCMAKE_MAKE_PROGRAM=$Make" "-DCMAKE_CXX_COMPILER=$Cxx" -DCMAKE_BUILD_TYPE=Release
}

Invoke-Checked $CMake --build $BuildDir --config Release

$Exe = Join-Path $BuildDir 'retail-camera-agent.exe'
Write-Host "Built: $Exe"
