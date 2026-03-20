#!/usr/bin/env powershell
$ErrorActionPreference = 'Stop'

Set-Location $PSScriptRoot\..

docker compose -f docker-compose.local.yml up -d

Write-Host 'Local MySQL is running with persistent volume: khaleo_mysql_data' -ForegroundColor Green
Write-Host 'Data will persist across container restarts unless you remove volumes.' -ForegroundColor Yellow
