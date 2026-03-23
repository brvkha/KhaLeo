#!/usr/bin/env powershell
param(
  [string]$ConfigPath = "$PSScriptRoot\sync-config.ps1"
)

$ErrorActionPreference = 'Stop'

Write-Host '========================================' -ForegroundColor Cyan
Write-Host 'Seeding Shared Aurora Database' -ForegroundColor Cyan
Write-Host '========================================' -ForegroundColor Cyan
Write-Host ''

$env:APP_SEED_LOCAL_DEV_ENABLED = 'true'
if (-not $env:APP_SEED_LOCAL_DEV_PASSWORD) {
  $env:APP_SEED_LOCAL_DEV_PASSWORD = 'khaleo'
}

Write-Host 'Seed password: khaleo' -ForegroundColor Yellow
Write-Host 'Default accounts:' -ForegroundColor Yellow
Write-Host '  - khaleo@khaleo.app (learner)' -ForegroundColor Yellow
Write-Host '  - admin@khaleo.app (admin)' -ForegroundColor Yellow
Write-Host ''

& "$PSScriptRoot\run-backend-with-aurora.ps1" -ConfigPath $ConfigPath -SeedSharedData
if ($LASTEXITCODE -eq 0) {
  Write-Host ''
  Write-Host 'Seeding completed successfully!' -ForegroundColor Green
} else {
  Write-Host ''
  Write-Host 'Seeding failed. Check errors above.' -ForegroundColor Red
  exit $LASTEXITCODE
}
