param(
    [switch]$SkipStart
)

$ErrorActionPreference = 'Stop'

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendRoot = Split-Path -Parent $scriptRoot

Push-Location $backendRoot
try {
    docker compose -f docker-compose.local.yml down -v
    if (-not $SkipStart) {
        docker compose -f docker-compose.local.yml up -d
    }

    Write-Host 'Local database reset completed.' -ForegroundColor Green
    if ($SkipStart) {
        Write-Host 'Database container is stopped. Start it with: docker compose -f docker-compose.local.yml up -d'
    } else {
        Write-Host 'Database container is running on localhost:3306.'
    }
}
finally {
    Pop-Location
}