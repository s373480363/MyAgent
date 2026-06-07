#Requires -RunAsAdministrator
$ErrorActionPreference = 'Stop'

$jdk = 'D:\mytools\jdk\jdk-21.0.11+10'
$maven = 'D:\mytools\maven\apache-maven-3.9.11'
$backupDir = 'D:\mytools\env-backup'
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupFile = Join-Path $backupDir "hklm-environment-before-jdk-maven-$timestamp.reg"
$logFile = Join-Path $backupDir "system-jdk-maven-env-config-$timestamp.log"

function Write-Log {
    param([string]$Message)
    $line = "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') $Message"
    $line | Tee-Object -FilePath $logFile -Append | Out-Host
}

function Normalize-PathEntry {
    param([string]$PathEntry)
    if ([string]::IsNullOrWhiteSpace($PathEntry)) {
        return ''
    }
    return $PathEntry.Trim().TrimEnd('\')
}

New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

Write-Log "Starting system JDK/Maven environment configuration."
Write-Log "Target JDK: $jdk"
Write-Log "Target Maven: $maven"

if (-not (Test-Path -LiteralPath (Join-Path $jdk 'bin\java.exe'))) {
    throw "Target java.exe not found: $jdk"
}

if (-not (Test-Path -LiteralPath (Join-Path $maven 'bin\mvn.cmd'))) {
    throw "Target mvn.cmd not found: $maven"
}

Write-Log "Exporting HKLM environment backup: $backupFile"
reg export 'HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment' $backupFile /y | Out-Null

[Environment]::SetEnvironmentVariable('JAVA_HOME', $jdk, 'Machine')
[Environment]::SetEnvironmentVariable('MAVEN_HOME', $maven, 'Machine')
Write-Log "Set Machine JAVA_HOME and MAVEN_HOME."

$machinePath = [Environment]::GetEnvironmentVariable('Path', 'Machine')
$entries = @()
if ($machinePath) {
    $entries = $machinePath -split ';' | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
}

$oldEntriesToRemove = @(
    'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin',
    "$jdk\bin",
    "$maven\bin",
    'D:\myproject\MyAgent\9_dependency\tools\jdk21\jdk-21.0.11+10\bin',
    'D:\myproject\MyAgent\9_dependency\tools\maven-3.9.11\apache-maven-3.9.11\bin'
) | ForEach-Object { Normalize-PathEntry $_ }

$filteredEntries = foreach ($entry in $entries) {
    $normalized = Normalize-PathEntry $entry
    if ($normalized -match '^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} Removed Machine Path entry:') {
        Write-Log "Removed invalid log text from Machine Path: $entry"
    } elseif ($oldEntriesToRemove -notcontains $normalized) {
        $entry.Trim()
    } else {
        Write-Log "Removed Machine Path entry: $entry"
    }
}

$newMachinePath = (@("$jdk\bin", "$maven\bin") + @($filteredEntries) |
    Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
    Select-Object -Unique) -join ';'

[Environment]::SetEnvironmentVariable('Path', $newMachinePath, 'Machine')
Write-Log "Updated Machine Path."

$code = @'
using System;
using System.Runtime.InteropServices;
public static class EnvNotify {
  [DllImport("user32.dll", SetLastError=true, CharSet=CharSet.Auto)]
  public static extern IntPtr SendMessageTimeout(IntPtr hWnd, uint Msg, UIntPtr wParam, string lParam, uint fuFlags, uint uTimeout, out UIntPtr lpdwResult);
}
'@
Add-Type -TypeDefinition $code
$result = [UIntPtr]::Zero
[EnvNotify]::SendMessageTimeout([IntPtr]0xffff, 0x001A, [UIntPtr]::Zero, 'Environment', 0x0002, 5000, [ref]$result) | Out-Null
Write-Log "Broadcasted environment change."

$javaExe = Join-Path $jdk 'bin\java.exe'
$mvnCmd = Join-Path $maven 'bin\mvn.cmd'
Write-Log "Verification: java exists=$((Test-Path -LiteralPath $javaExe))"
Write-Log "Verification: mvn exists=$((Test-Path -LiteralPath $mvnCmd))"
Write-Log "Completed system JDK/Maven environment configuration."
