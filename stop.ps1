param(
    [switch]$IncludeMysql
)

# 兜底停止脚本：当总启动脚本 start.ps1 的监管窗口已关闭、或服务是用各自 run.ps1 单独拉起时，
# 用本脚本按端口停止本系统的服务进程。
# 为避免误杀，仅当端口占用者的进程名与预期一致时才结束（如 8080 必须是 java）。
# 默认不停 MySQL(3306)，因为它常是系统级共享服务；需要时加 -IncludeMysql。

$ErrorActionPreference = "Continue"

# 端口 -> 预期进程名前缀（小写）
$targets = @(
    @{ Port = 8000; Expect = "python";       Name = "AI service" }
    @{ Port = 8080; Expect = "java";         Name = "Backend API" }
    @{ Port = 8848; Expect = "node";         Name = "Admin UI" }
    @{ Port = 6379; Expect = "redis-server"; Name = "Redis" }
)
if ($IncludeMysql) {
    $targets += @{ Port = 3306; Expect = "mysqld"; Name = "MySQL" }
}

function Get-PortOwners([int]$Port) {
    $owners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($owner in $owners) {
        Get-Process -Id $owner -ErrorAction SilentlyContinue
    }
}

Write-Host "=== Stopping retail services ===" -ForegroundColor Cyan
$stopped = 0

foreach ($t in $targets) {
    $procs = @(Get-PortOwners $t.Port)
    if ($procs.Count -eq 0) {
        Write-Host "  port $($t.Port) ($($t.Name)): free" -ForegroundColor DarkGray
        continue
    }

    foreach ($p in $procs) {
        $procName = $p.ProcessName.ToLower()
        if ($procName -like "$($t.Expect)*") {
            Write-Host "  stopping $($t.Name): $($p.ProcessName)#$($p.Id) on port $($t.Port)" -ForegroundColor DarkGray
            Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue
            $stopped++
        } else {
            Write-Host "  [WARN] port $($t.Port) held by '$($p.ProcessName)#$($p.Id)' (expected '$($t.Expect)'); skipped" -ForegroundColor Yellow
        }
    }
}

Write-Host "  [OK] stopped $stopped process(es)" -ForegroundColor Green
if (-not $IncludeMysql) {
    Write-Host "  (MySQL on 3306 left running; pass -IncludeMysql to stop it too)" -ForegroundColor DarkGray
}
