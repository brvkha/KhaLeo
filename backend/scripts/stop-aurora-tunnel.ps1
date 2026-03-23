#!/usr/bin/env powershell

$ErrorActionPreference = 'Stop'
$pidFile = Join-Path $PSScriptRoot '.aurora-tunnel.pid'

if (-not (Test-Path $pidFile)) {
  Write-Host 'No tunnel pid file found.' -ForegroundColor Yellow
  exit 0
}

$pidValue = Get-Content -Raw $pidFile
if (-not $pidValue) {
  Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
  Write-Host 'Tunnel pid file was empty; cleaned up.' -ForegroundColor Yellow
  exit 0
}

$proc = Get-Process -Id ([int]$pidValue) -ErrorAction SilentlyContinue
if ($proc) {
  Stop-Process -Id $proc.Id -Force
  Write-Host "Stopped Aurora tunnel process PID $($proc.Id)." -ForegroundColor Green
} else {
  Write-Host 'Tunnel process not running anymore.' -ForegroundColor Yellow
}

Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
