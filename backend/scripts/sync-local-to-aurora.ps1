#!/usr/bin/env powershell
param(
  [string]$ConfigPath = "$PSScriptRoot\sync-config.ps1",
  [switch]$Force
)

$ErrorActionPreference = 'Stop'

function Fail([string]$Message) {
  throw $Message
}

function Require-Command([string]$Name) {
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    Fail "Missing command: $Name"
  }
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

function Wait-ForPort([string]$Host, [int]$Port, [int]$TimeoutSeconds = 20) {
  $started = Get-Date
  while (((Get-Date) - $started).TotalSeconds -lt $TimeoutSeconds) {
    try {
      $ok = Test-NetConnection -ComputerName $Host -Port $Port -WarningAction SilentlyContinue
      if ($ok.TcpTestSucceeded) {
        return
      }
    } catch {
      # ignore and retry
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

function Resolve-AuroraConnection([hashtable]$Cfg, [object]$DbSecret) {
  $hostOverride = Get-ConfigValue $Cfg 'AuroraEndpoint' $false
  $host = if ($hostOverride) { $hostOverride } elseif ($DbSecret.host) { $DbSecret.host } elseif ($DbSecret.endpoint) { $DbSecret.endpoint } else { $null }
  if (-not $host -and $DbSecret.jdbc_url) {
    if ($DbSecret.jdbc_url -match 'jdbc:mysql://([^:/]+)') {
      $host = $matches[1]
    }
  }

  if (-not $host) {
    Fail 'Aurora host not found from config or db secret'
  }

  $port = if ($DbSecret.port) { [int]$DbSecret.port } else { 3306 }
  $dbName = if ($DbSecret.dbname) { $DbSecret.dbname } elseif ($DbSecret.database) { $DbSecret.database } elseif ($DbSecret.dbName) { $DbSecret.dbName } else { $null }
  $username = if ($DbSecret.username) { $DbSecret.username } elseif ($DbSecret.user) { $DbSecret.user } else { $null }
  $password = if ($DbSecret.password) { $DbSecret.password } elseif ($DbSecret.pass) { $DbSecret.pass } else { $null }

  if (-not $dbName -or -not $username -or -not $password) {
    Fail 'Aurora dbname/username/password missing in db secret'
  }

  return @{
    Host = $host
    Port = $port
    DbName = $dbName
    Username = $username
    Password = $password
  }
}

Require-Command 'aws'
Require-Command 'mysqldump'
Require-Command 'mysql'

if (-not (Test-Path $ConfigPath)) {
  Fail "Config file not found: $ConfigPath. Copy sync-config.sample.ps1 to sync-config.ps1 first."
}

. $ConfigPath
if (-not $SyncConfig) {
  Fail 'Config file must define $SyncConfig hashtable'
}

$awsRegion = Get-ConfigValue $SyncConfig 'AwsRegion'
$awsProfile = Get-ConfigValue $SyncConfig 'AwsProfile' $false
$env:AWS_REGION = $awsRegion
if ($awsProfile) {
  $env:AWS_PROFILE = $awsProfile
}

$localHost = Get-ConfigValue $SyncConfig 'LocalDbHost'
$localPort = [int](Get-ConfigValue $SyncConfig 'LocalDbPort')
$localDbName = Get-ConfigValue $SyncConfig 'LocalDbName'
$localUser = Get-ConfigValue $SyncConfig 'LocalDbUser'
$localPass = Get-ConfigValue $SyncConfig 'LocalDbPassword'
$forwardPort = [int](Get-ConfigValue $SyncConfig 'LocalForwardPort')
$secretId = Get-ConfigValue $SyncConfig 'DbSecretId'
$dumpDirectory = Get-ConfigValue $SyncConfig 'DumpDirectory'

if (-not (Test-Path $dumpDirectory)) {
  New-Item -ItemType Directory -Path $dumpDirectory -Force | Out-Null
}

$secret = Get-DbSecret $secretId
$aurora = Resolve-AuroraConnection $SyncConfig $secret
$instanceId = Resolve-BackendInstanceId $SyncConfig

Write-Host "Source (local): $localHost`:$localPort/$localDbName" -ForegroundColor Cyan
Write-Host "Target (aurora): $($aurora.Host):$($aurora.Port)/$($aurora.DbName) via instance $instanceId" -ForegroundColor Yellow

if (-not $Force) {
  $answer = Read-Host 'This will overwrite data on Aurora target DB. Type YES to continue'
  if ($answer -ne 'YES') {
    Write-Host 'Aborted.' -ForegroundColor Red
    exit 1
  }
}

$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$dumpFile = Join-Path $dumpDirectory "local-to-aurora-$timestamp.sql"

$forwardArgs = @(
  'ssm', 'start-session',
  '--target', $instanceId,
  '--document-name', 'AWS-StartPortForwardingSessionToRemoteHost',
  '--parameters', "host=[$($aurora.Host)],portNumber=[$($aurora.Port)],localPortNumber=[$forwardPort]"
)

$forwardProcess = $null
try {
  $forwardProcess = Start-Process -FilePath 'aws' -ArgumentList $forwardArgs -WindowStyle Hidden -PassThru
  Wait-ForPort -Host '127.0.0.1' -Port $forwardPort -TimeoutSeconds 30

  Write-Host "Dumping local DB to $dumpFile" -ForegroundColor Cyan
  & mysqldump -h $localHost -P $localPort -u $localUser "--password=$localPass" --single-transaction --routines --triggers $localDbName > $dumpFile

  Write-Host 'Importing dump into Aurora via tunnel...' -ForegroundColor Cyan
  Get-Content -Raw $dumpFile | & mysql -h 127.0.0.1 -P $forwardPort -u $($aurora.Username) "--password=$($aurora.Password)" $($aurora.DbName)

  Write-Host 'Sync local -> Aurora completed.' -ForegroundColor Green
} finally {
  if ($forwardProcess -and -not $forwardProcess.HasExited) {
    Stop-Process -Id $forwardProcess.Id -Force
  }
}
