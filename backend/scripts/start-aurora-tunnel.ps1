#!/usr/bin/env powershell
param(
  [string]$ConfigPath = "$PSScriptRoot\sync-config.ps1",
  [switch]$ForceRestart
)

$ErrorActionPreference = 'Stop'
$pidFile = Join-Path $PSScriptRoot '.aurora-tunnel.pid'

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

function Wait-ForPort([string]$Hostname, [int]$Port, [int]$TimeoutSeconds = 30) {
  $started = Get-Date
  while (((Get-Date) - $started).TotalSeconds -lt $TimeoutSeconds) {
    try {
      $ok = Test-NetConnection -ComputerName $Host -Port $Port -WarningAction SilentlyContinue
      if ($ok.TcpTestSucceeded) {
        return
      }
    } catch {
      # retry
    }
    Start-Sleep -Seconds 1
  }
  Fail "Timeout waiting for $Host`:$Port"
}

function Resolve-BackendInstanceId([hashtable]$Cfg) {
  $explicit = Get-ConfigValue $Cfg 'BackendInstanceId' $false
  if ($explicit) {
    return $explicit
  }

  $tagKey = Get-ConfigValue $Cfg 'DeployTargetTagKey'
  $tagValue = Get-ConfigValue $Cfg 'DeployTargetTagValue'

  $instanceId = aws ec2 describe-instances `
    --filters "Name=tag:$tagKey,Values=$tagValue" "Name=instance-state-name,Values=running" `
    --query "Reservations[].Instances[].InstanceId" --output text

  if (-not $instanceId -or $instanceId -eq 'None') {
    Fail "No running backend instance found for tag $tagKey=$tagValue"
  }

  return ($instanceId -split '\s+')[0]
}

function Get-DbSecret([string]$SecretId) {
  $json = aws secretsmanager get-secret-value --secret-id $SecretId --query SecretString --output text
  if (-not $json) {
    Fail "Empty secret string for $SecretId"
  }
  return ($json | ConvertFrom-Json)
}

function Resolve-AuroraHost([hashtable]$Cfg, [object]$DbSecret) {
  $hostOverride = Get-ConfigValue $Cfg 'AuroraEndpoint' $false
  if ($hostOverride) {
    return $hostOverride
  }
  if ($DbSecret.host) {
    return $DbSecret.host
  }
  if ($DbSecret.endpoint) {
    return $DbSecret.endpoint
  }
  if ($DbSecret.jdbc_url -and $DbSecret.jdbc_url -match 'jdbc:mysql://([^:/]+)') {
    return $matches[1]
  }
  Fail 'Aurora host not found from config or db secret'
}

if (-not (Get-Command aws -ErrorAction SilentlyContinue)) {
  Fail 'Missing command: aws'
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

if ((Test-Path $pidFile) -and -not $ForceRestart) {
  $existingPid = Get-Content -Raw $pidFile
  if ($existingPid) {
    $existing = Get-Process -Id ([int]$existingPid) -ErrorAction SilentlyContinue
    if ($existing) {
      Write-Host "Aurora tunnel is already running (PID $existingPid)." -ForegroundColor Yellow
      exit 0
    }
  }
}

if ((Test-Path $pidFile) -and $ForceRestart) {
  $existingPid = Get-Content -Raw $pidFile
  if ($existingPid) {
    Stop-Process -Id ([int]$existingPid) -Force -ErrorAction SilentlyContinue
  }
  Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}

$instanceId = Resolve-BackendInstanceId $SyncConfig
$dbSecret = Get-DbSecret $dbSecretId
$auroraHost = Resolve-AuroraHost $SyncConfig $dbSecret
$auroraPort = if ($dbSecret.port) { [int]$dbSecret.port } else { 3306 }

Write-Host "Opening SSM tunnel: 127.0.0.1:$forwardPort -> "${auroraHost}:$auroraPort" via $instanceId" -ForegroundColor Cyan

$forwardArgs = @(
  'ssm', 'start-session',
  '--target', $instanceId,
  '--document-name', 'AWS-StartPortForwardingSessionToRemoteHost',
  '--parameters', "host=[$auroraHost],portNumber=[$auroraPort],localPortNumber=[$forwardPort]"
)

$forwardProcess = Start-Process -FilePath 'aws' -ArgumentList $forwardArgs -WindowStyle Hidden -PassThru
Set-Content -Path $pidFile -Value $forwardProcess.Id

Wait-ForPort -Hostname '127.0.0.1' -Port $forwardPort -TimeoutSeconds 30
Write-Host "Aurora tunnel ready on localhost:$forwardPort (PID $($forwardProcess.Id))." -ForegroundColor Green
