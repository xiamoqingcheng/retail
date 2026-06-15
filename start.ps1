param(
    [string]$MySqlBin = $env:MYSQL_BIN,
    [string]$PythonExe = $env:PYTHON_EXE,
    [switch]$SkipAdmin
)

$ErrorActionPreference = "Continue"

$ProjectDir = $PSScriptRoot
$RetailDir = Join-Path $ProjectDir "retail"
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

$script:ManagedProcesses = New-Object System.Collections.Generic.List[object]
$script:CleanupDone = $false
$script:JobHandle = [IntPtr]::Zero

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

function Initialize-KillOnCloseJob {
    if (-not ("RetailWinJob" -as [type])) {
        Add-Type -TypeDefinition @"
using System;
using System.ComponentModel;
using System.Runtime.InteropServices;

public static class RetailWinJob
{
    private const int JobObjectExtendedLimitInformation = 9;
    private const uint JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE = 0x00002000;

    [StructLayout(LayoutKind.Sequential)]
    private struct JOBOBJECT_BASIC_LIMIT_INFORMATION
    {
        public long PerProcessUserTimeLimit;
        public long PerJobUserTimeLimit;
        public uint LimitFlags;
        public UIntPtr MinimumWorkingSetSize;
        public UIntPtr MaximumWorkingSetSize;
        public uint ActiveProcessLimit;
        public long Affinity;
        public uint PriorityClass;
        public uint SchedulingClass;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct IO_COUNTERS
    {
        public ulong ReadOperationCount;
        public ulong WriteOperationCount;
        public ulong OtherOperationCount;
        public ulong ReadTransferCount;
        public ulong WriteTransferCount;
        public ulong OtherTransferCount;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct JOBOBJECT_EXTENDED_LIMIT_INFORMATION
    {
        public JOBOBJECT_BASIC_LIMIT_INFORMATION BasicLimitInformation;
        public IO_COUNTERS IoInfo;
        public UIntPtr ProcessMemoryLimit;
        public UIntPtr JobMemoryLimit;
        public UIntPtr PeakProcessMemoryUsed;
        public UIntPtr PeakJobMemoryUsed;
    }

    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern IntPtr CreateJobObject(IntPtr lpJobAttributes, string lpName);

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern bool SetInformationJobObject(
        IntPtr hJob,
        int jobObjectInfoClass,
        IntPtr lpJobObjectInfo,
        uint cbJobObjectInfoLength);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool AssignProcessToJobObject(IntPtr hJob, IntPtr hProcess);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool CloseHandle(IntPtr hObject);

    public static IntPtr CreateKillOnCloseJob(string name)
    {
        IntPtr hJob = CreateJobObject(IntPtr.Zero, name);
        if (hJob == IntPtr.Zero)
        {
            throw new Win32Exception(Marshal.GetLastWin32Error());
        }

        JOBOBJECT_EXTENDED_LIMIT_INFORMATION info = new JOBOBJECT_EXTENDED_LIMIT_INFORMATION();
        info.BasicLimitInformation.LimitFlags = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE;

        int length = Marshal.SizeOf(typeof(JOBOBJECT_EXTENDED_LIMIT_INFORMATION));
        IntPtr ptr = Marshal.AllocHGlobal(length);
        try
        {
            Marshal.StructureToPtr(info, ptr, false);
            if (!SetInformationJobObject(hJob, JobObjectExtendedLimitInformation, ptr, (uint)length))
            {
                throw new Win32Exception(Marshal.GetLastWin32Error());
            }
        }
        finally
        {
            Marshal.FreeHGlobal(ptr);
        }

        return hJob;
    }
}
"@
    }

    $jobName = "RetailSystem-" + $PID + "-" + ([Guid]::NewGuid().ToString("N"))
    $script:JobHandle = [RetailWinJob]::CreateKillOnCloseJob($jobName)
}

function Test-Port([int]$Port) {
    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Get-PortOwners([int]$Port) {
    $owners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($owner in $owners) {
        $proc = Get-CimInstance Win32_Process -Filter "ProcessId=$owner" -ErrorAction SilentlyContinue
        if ($proc) {
            [PSCustomObject]@{
                ProcessId = $proc.ProcessId
                Name = $proc.Name
                CommandLine = $proc.CommandLine
            }
        }
    }
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

function Add-ProcessToJob([System.Diagnostics.Process]$Process, [string]$Name) {
    if ($script:JobHandle -eq [IntPtr]::Zero -or -not $Process) {
        return
    }

    try {
        $ok = [RetailWinJob]::AssignProcessToJobObject($script:JobHandle, $Process.Handle)
        if (-not $ok) {
            Warn "$Name was started, but could not be attached to cleanup job"
        }
    } catch {
        Warn "$Name was started, but could not be attached to cleanup job: $($_.Exception.Message)"
    }
}

function Start-ManagedProcess(
    [string]$Name,
    [string]$FilePath,
    [string[]]$ArgumentList,
    [string]$WorkingDirectory,
    [string]$LogPrefix
) {
    $outFile = Join-Path $LogDir "$LogPrefix.out.log"
    $errFile = Join-Path $LogDir "$LogPrefix.err.log"

    $startParams = @{
        FilePath = $FilePath
        WorkingDirectory = $WorkingDirectory
        WindowStyle = "Hidden"
        RedirectStandardOutput = $outFile
        RedirectStandardError = $errFile
        PassThru = $true
    }
    if ($ArgumentList -and $ArgumentList.Count -gt 0) {
        $startParams.ArgumentList = $ArgumentList
    }

    $process = Start-Process @startParams

    Add-ProcessToJob $process $Name
    $script:ManagedProcesses.Add([PSCustomObject]@{
        Name = $Name
        ProcessId = $process.Id
        Log = $outFile
    }) | Out-Null

    Write-Host "  started $Name (PID $($process.Id)), logs: $outFile" -ForegroundColor DarkGray
    return $process
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

function Show-PortAlreadyRunning([string]$Name, [int]$Port) {
    $owners = @(Get-PortOwners $Port)
    if ($owners.Count -eq 0) {
        Ok "$Name is already running on port $Port"
        return
    }

    $ownerText = ($owners | ForEach-Object { "$($_.Name)#$($_.ProcessId)" }) -join ", "
    Warn "$Name port $Port is already in use by $ownerText; this script will not stop that pre-existing process"
}

function Stop-ManagedServices {
    if ($script:CleanupDone) {
        return
    }
    $script:CleanupDone = $true

    Write-Host ""
    Write-Host "=== Stopping managed services ===" -ForegroundColor Cyan

    for ($i = $script:ManagedProcesses.Count - 1; $i -ge 0; $i--) {
        $entry = $script:ManagedProcesses[$i]
        $proc = Get-Process -Id $entry.ProcessId -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Host "  stopping $($entry.Name) (PID $($entry.ProcessId))" -ForegroundColor DarkGray
            Stop-Process -Id $entry.ProcessId -Force -ErrorAction SilentlyContinue
        }
    }

    Start-Sleep -Seconds 1

    if ($script:JobHandle -ne [IntPtr]::Zero) {
        [RetailWinJob]::CloseHandle($script:JobHandle) | Out-Null
        $script:JobHandle = [IntPtr]::Zero
    }

    Write-Host "  [OK] Cleanup complete" -ForegroundColor Green
}

if (-not (Test-Path -LiteralPath $RetailDir)) {
    Write-Error "Retail directory not found: $RetailDir"
    exit 1
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

try {
    Initialize-KillOnCloseJob

    Write-Host "=== Retail System Startup ===" -ForegroundColor Cyan
    Write-Host "Project: $RetailDir"

    Step "[1/6] MySQL 3306"
    if (Test-Port 3306) {
        Show-PortAlreadyRunning "MySQL" 3306
    } elseif ($MySqlServer) {
        Start-ManagedProcess "MySQL" $MySqlServer @("--console") (Split-Path -Parent $MySqlServer) "mysql" | Out-Null
        if (Wait-Port 3306 12) { Ok "MySQL started" } else { Fail "MySQL did not open port 3306" }
    } else {
        Fail "mysqld.exe not found. Set MYSQL_BIN or add MySQL to PATH."
    }

    Step "[2/6] Redis 6379"
    if (Test-Port 6379) {
        Show-PortAlreadyRunning "Redis" 6379
    } elseif ($RedisServer) {
        Start-ManagedProcess "Redis" $RedisServer @() (Split-Path -Parent $RedisServer) "redis" | Out-Null
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
        Show-PortAlreadyRunning "AI service" 8000
    } elseif ($Python) {
        Start-ManagedProcess "AI service" $Python @("main.py") $AiDir "ai" | Out-Null
        if (Wait-Port 8000 35) { Ok "AI service started" } else { Warn "AI service is still starting or failed; check log\ai.err.log" }
    } else {
        Fail "python not found. Set PYTHON_EXE or add Python to PATH."
    }

    Step "[5/6] Backend API 8080"
    if (Test-Port 8080) {
        Show-PortAlreadyRunning "Backend API" 8080
    } elseif ($Maven) {
        Start-ManagedProcess "Backend API" $Maven @("spring-boot:run", "-q") $BackendDir "backend" | Out-Null
        if (Wait-Port 8080 75) { Ok "Backend API started" } else { Warn "Backend API is still starting or failed; check log\backend.err.log" }
    } else {
        Fail "Maven not found. Use bundled apache-maven-3.9.14 or add mvn to PATH."
    }

    Step "[6/6] Admin UI 8848"
    if ($SkipAdmin) {
        Warn "Admin UI skipped"
    } elseif (Test-Port 8848) {
        Show-PortAlreadyRunning "Admin UI" 8848
    } elseif ($Npm) {
        Start-ManagedProcess "Admin UI" $Npm @("run", "dev") $AdminDir "admin" | Out-Null
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
    Write-Host ""
    Write-Host "This window is now supervising started services." -ForegroundColor Green
    Write-Host "Press Ctrl+C to stop all managed services. Closing this terminal also kills them." -ForegroundColor Green

    $reportedExited = @{}
    while ($true) {
        Start-Sleep -Seconds 2
        foreach ($entry in $script:ManagedProcesses) {
            if (-not $reportedExited.ContainsKey($entry.ProcessId)) {
                $proc = Get-Process -Id $entry.ProcessId -ErrorAction SilentlyContinue
                if (-not $proc) {
                    Warn "$($entry.Name) root process exited (PID $($entry.ProcessId)); check $($entry.Log)"
                    $reportedExited[$entry.ProcessId] = $true
                }
            }
        }
    }
} finally {
    Stop-ManagedServices
}
