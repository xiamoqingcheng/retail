param(
    [string]$MySqlBin = $env:MYSQL_BIN,
    [string]$PythonExe = $env:PYTHON_EXE,
    [switch]$SkipAdmin
)

$ErrorActionPreference = "Continue"
$RetailDir = $PSScriptRoot
$ProjectDir = Split-Path -Parent $RetailDir
$LogDir = Join-Path $RetailDir "log"
$DbFile = Join-Path $RetailDir "retail_db.sql"
$GoodsFile = Join-Path $RetailDir "retail_goods.sql"
$AiDir = Join-Path $RetailDir "retail-ai"
$BackendDir = Join-Path $RetailDir "retail-server"
$AdminDir = Join-Path $RetailDir "retail-admin-pro"
$BundledMaven = Join-Path $ProjectDir "apache-maven-3.9.14\bin\mvn.cmd"
$BundledNpm = Join-Path $ProjectDir "node-v24.14.1-win-x64\npm.cmd"
$BundledRedis = Join-Path $ProjectDir "Redis-8.6.2-Windows-x64-msys2\Redis-8.6.2-Windows-x64-msys2\redis-server.exe"
$DefaultMySqlBin = "D:\mysql-9.6.0-winx64\bin"
$DefaultPython = "D:\miniconda3\python.exe"

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

function Test-Port([int]$Port) {
    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Wait-Port([int]$Port, [int]$Seconds) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-Port $Port) { return $true }
        Start-Sleep -Seconds 1
    }
    return (Test-Port $Port)
}

function Resolve-CommandPath([string]$PreferredPath, [string]$CommandName) {
    if ($PreferredPath -and (Test-Path -LiteralPath $PreferredPath)) {
        return $PreferredPath
    }

    $cmd = Get-Command $CommandName -ErrorAction SilentlyContinue
    if ($cmd) {
        return $cmd.Source
    }

    return $null
}

function Start-LoggedProcess(
    [string]$Name,
    [string]$FilePath,
    [string[]]$ArgumentList,
    [string]$WorkingDirectory,
    [string]$LogPrefix
) {
    $outFile = Join-Path $LogDir "$LogPrefix.out.log"
    $errFile = Join-Path $LogDir "$LogPrefix.err.log"
    Start-Process -FilePath $FilePath `
        -ArgumentList $ArgumentList `
        -WorkingDirectory $WorkingDirectory `
        -WindowStyle Hidden `
        -RedirectStandardOutput $outFile `
        -RedirectStandardError $errFile | Out-Null
    Write-Host "  started $Name, logs: $outFile" -ForegroundColor DarkGray
}

function Step([string]$Text) {
    Write-Host ""
    Write-Host $Text -ForegroundColor Cyan
}

function Ok([string]$Text) {
    Write-Host "  [OK] $Text" -ForegroundColor Green
}

function Warn([string]$Text) {
    Write-Host "  [WARN] $Text" -ForegroundColor Yellow
}

function Fail([string]$Text) {
    Write-Host "  [FAIL] $Text" -ForegroundColor Red
}

if (-not $MySqlBin) {
    $MySqlBin = $DefaultMySqlBin
}

if (-not $PythonExe) {
    $PythonExe = $DefaultPython
}

$MySqlServer = Resolve-CommandPath (Join-Path $MySqlBin "mysqld.exe") "mysqld"
$MySqlClient = Resolve-CommandPath (Join-Path $MySqlBin "mysql.exe") "mysql"
$RedisServer = Resolve-CommandPath $BundledRedis "redis-server"
$Maven = Resolve-CommandPath $BundledMaven "mvn"
$Npm = Resolve-CommandPath $BundledNpm "npm"
$Python = Resolve-CommandPath $PythonExe "python"

Write-Host "=== Retail System Startup ===" -ForegroundColor Cyan
Write-Host "Project: $RetailDir"

Step "[1/6] MySQL 3306"
if (Test-Port 3306) {
    Ok "MySQL is already running"
} elseif ($MySqlServer) {
    Start-LoggedProcess "MySQL" $MySqlServer @("--console") (Split-Path -Parent $MySqlServer) "mysql"
    if (Wait-Port 3306 12) { Ok "MySQL started" } else { Fail "MySQL did not open port 3306" }
} else {
    Fail "mysqld.exe not found. Set MYSQL_BIN or add MySQL to PATH."
}

Step "[2/6] Redis 6379"
if (Test-Port 6379) {
    Ok "Redis is already running"
} elseif ($RedisServer) {
    Start-LoggedProcess "Redis" $RedisServer @() (Split-Path -Parent $RedisServer) "redis"
    if (Wait-Port 6379 8) { Ok "Redis started" } else { Fail "Redis did not open port 6379" }
} else {
    Fail "redis-server.exe not found. Put Redis in project tools or add it to PATH."
}

Step "[3/6] Database retail_db"
if (-not $MySqlClient) {
    Warn "mysql.exe not found; skip database check"
} elseif (-not (Test-Port 3306)) {
    Warn "MySQL is not running; skip database check"
} else {
    & $MySqlClient -u root -e "USE retail_db; SELECT 1;" 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Ok "retail_db is ready"
    } elseif (Test-Path -LiteralPath $DbFile) {
        Warn "retail_db not found; importing retail_db.sql"
        Get-Content -Encoding UTF8 -LiteralPath $DbFile | & $MySqlClient -u root 2>$null
        if ($LASTEXITCODE -eq 0) {
            Ok "retail_db.sql imported"
            if (Test-Path -LiteralPath $GoodsFile) {
                Get-Content -Encoding UTF8 -LiteralPath $GoodsFile | & $MySqlClient -u root 2>$null
                if ($LASTEXITCODE -eq 0) { Ok "retail_goods.sql imported" } else { Warn "retail_goods.sql import failed" }
            }
        } else {
            Fail "database import failed"
        }
    } else {
        Fail "retail_db.sql not found"
    }
}

Step "[4/6] AI service 8000"
if (Test-Port 8000) {
    Ok "AI service is already running"
} elseif ($Python) {
    Start-LoggedProcess "AI service" $Python @("main.py") $AiDir "ai"
    if (Wait-Port 8000 35) { Ok "AI service started" } else { Warn "AI service is still starting or failed; check log\ai.err.log" }
} else {
    Fail "python not found. Set PYTHON_EXE or add Python to PATH."
}

Step "[5/6] Backend API 8080"
if (Test-Port 8080) {
    Ok "Backend API is already running"
} elseif ($Maven) {
    Start-LoggedProcess "Backend API" $Maven @("spring-boot:run", "-q") $BackendDir "backend"
    if (Wait-Port 8080 75) { Ok "Backend API started" } else { Warn "Backend API is still starting or failed; check log\backend.err.log" }
} else {
    Fail "Maven not found. Use bundled apache-maven-3.9.14 or add mvn to PATH."
}

Step "[6/6] Admin UI 8848"
if ($SkipAdmin) {
    Warn "Admin UI skipped"
} elseif (Test-Port 8848) {
    Ok "Admin UI is already running"
} elseif ($Npm) {
    Start-LoggedProcess "Admin UI" $Npm @("run", "dev") $AdminDir "admin"
    if (Wait-Port 8848 35) { Ok "Admin UI started" } else { Warn "Admin UI is still starting or failed; check log\admin.err.log" }
} else {
    Fail "npm not found. Use bundled node-v24.14.1-win-x64 or add npm to PATH."
}

Write-Host ""
Write-Host "=== Service URLs ===" -ForegroundColor Cyan
Write-Host "Admin:  http://localhost:8848"
Write-Host "API:    http://localhost:8080"
Write-Host "AI:     http://localhost:8000"
Write-Host "Applet: open $RetailDir\retail-customer in WeChat DevTools"
