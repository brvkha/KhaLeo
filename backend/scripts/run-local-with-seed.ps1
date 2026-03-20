#!/usr/bin/env powershell
$ErrorActionPreference = 'Stop'

$env:APP_SEED_LOCAL_DEV_ENABLED = 'true'
if (-not $env:APP_SEED_LOCAL_DEV_PASSWORD) {
  $env:APP_SEED_LOCAL_DEV_PASSWORD = 'khaleo'
}

Write-Host 'Starting backend with local/dev seed enabled...' -ForegroundColor Cyan
Write-Host "Seed password: $env:APP_SEED_LOCAL_DEV_PASSWORD" -ForegroundColor Yellow

mvn spring-boot:run
