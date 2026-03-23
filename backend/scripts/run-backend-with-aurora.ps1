#!/usr/bin/env powershell
param(
  [string]$ConfigPath = "$PSScriptRoot\sync-config.ps1",
  [switch]$SeedSharedData
)

$ErrorActionPreference = 'Stop'

function Fail([string]$Message) {
  throw $Message
}

function Get-ConfigValue([hashtable]$Cfg, [string]$Key, [bool]$Required = $true) {
  if ($Cfg.ContainsKey($Key) -and $null -ne $Cfg[$Key] -and "$($Cfg[$Key])" -ne '') {
    return $Cfg[$Key]
  }
  if ($Required) {
    Fail "Missing config value: $Key"
  }
  return $null
}

function Get-DbSecret([string]$SecretId) {
  Write-Host "Retrieving secret: $SecretId" -ForegroundColor Gray
  try {
    $json = aws secretsmanager get-secret-value --secret-id $SecretId --query SecretString --output text 2>&1
    if ($LASTEXITCODE -ne 0 -or -not $json) {
      Write-Host "AWS error or empty secret. Falling back to defaults." -ForegroundColor Yellow
      return @{ dbname = 'khaleo_flashcard'; username = 'admin'; password = '' }
    }
    $result = $json | ConvertFrom-Json
    Write-Host "Secret retrieved successfully" -ForegroundColor Green
    return $result
  } catch {
    Write-Host "Failed to retrieve secret: $_. Using defaults." -ForegroundColor Yellow
    return @{ dbname = 'khaleo_flashcard'; username = 'admin'; password = '' }
  }
}

if (-not (Get-Command aws -ErrorAction SilentlyContinue)) {
  Fail 'Missing command: aws'
}
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
  Fail 'Missing command: mvn'
}

if (-not (Test-Path $ConfigPath)) {
  Fail "Config file not found: $ConfigPath. Copy sync-config.sample.ps1 to sync-config.ps1 first."
}

. $ConfigPath
if (-not $SyncConfig) {
  Fail 'Config file must define $SyncConfig hashtable'
}

$awsRegion = Get-ConfigValue $SyncConfig 'AwsRegion'
$awsProfile = Get-ConfigValue $SyncConfig 'AwsProfile' $false
$dbSecretId = Get-ConfigValue $SyncConfig 'DbSecretId'
$forwardPort = [int](Get-ConfigValue $SyncConfig 'LocalForwardPort')

$env:AWS_REGION = $awsRegion
if ($awsProfile) {
  $env:AWS_PROFILE = $awsProfile
}

# Ensure tunnel is running first.
& "$PSScriptRoot\start-aurora-tunnel.ps1" -ConfigPath $ConfigPath

$dbSecret = Get-DbSecret $dbSecretId
$dbName = if ($dbSecret.dbname) { $dbSecret.dbname } elseif ($dbSecret.database) { $dbSecret.database } elseif ($dbSecret.dbName) { $dbSecret.dbName } else { 'khaleo_flashcard' }
$dbUser = if ($dbSecret.username) { $dbSecret.username } elseif ($dbSecret.user) { $dbSecret.user } else { 'admin' }
$dbPass = if ($dbSecret.password) { $dbSecret.password } elseif ($dbSecret.pass) { $dbSecret.pass } else { '' }

# Validate connection parameters - ensure no null/empty values
if ([string]::IsNullOrWhiteSpace($dbName)) { $dbName = 'khaleo_flashcard' }
if ([string]::IsNullOrWhiteSpace($forwardPort)) { $forwardPort = 13306 }
if ([string]::IsNullOrWhiteSpace($dbUser)) { $dbUser = 'admin' }

Write-Host "DB Connection Config:" -ForegroundColor Cyan
Write-Host "  Database Name: $dbName" -ForegroundColor Gray
Write-Host "  Forward Port:  $forwardPort" -ForegroundColor Gray
Write-Host "  DB User:       $dbUser" -ForegroundColor Gray

# Build JDBC URL using concatenation (not interpolation)
$jdbcUrl = "jdbc:mysql://127.0.0.1:" + $forwardPort + "/" + $dbName + "?useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_URL = $jdbcUrl
$env:DB_USERNAME = $dbUser
$env:DB_PASSWORD = $dbPass

Write-Host "  JDBC URL:      $jdbcUrl" -ForegroundColor Green

if ($SeedSharedData) {
  $env:APP_SEED_LOCAL_DEV_ENABLED = 'true'
  if (-not $env:APP_SEED_LOCAL_DEV_PASSWORD) {
    $env:APP_SEED_LOCAL_DEV_PASSWORD = 'khaleo'
  }
  Write-Host 'Shared Aurora seed enabled for this run.' -ForegroundColor Yellow
  # Also pass as Maven property to ensure it's picked up
  $extraArgs = '-Dapp.seed.local-dev.enabled=true', "-Dapp.seed.local-dev.default-password=$env:APP_SEED_LOCAL_DEV_PASSWORD"
  Write-Host 'Running: mvn spring-boot:run' $extraArgs -ForegroundColor Cyan
  Set-Location "$PSScriptRoot\.."
  mvn spring-boot:run $extraArgs
} else {
  $env:APP_SEED_LOCAL_DEV_ENABLED = 'false'
  Write-Host 'Seed mode disabled.' -ForegroundColor Yellow
  Set-Location "$PSScriptRoot\.."
  mvn spring-boot:run
}
