#!/usr/bin/env powershell
# Copy this file to sync-config.ps1 and edit values for your environment.

$SyncConfig = @{
  AwsProfile = 'default'
  AwsRegion = 'ap-southeast-1'

  # Used to discover a running backend EC2 for SSM port forwarding.
  DeployTargetTagKey = 'Role'
  DeployTargetTagValue = 'khaleo-backend'

  # Optional: set directly to skip EC2 lookup by tag.
  BackendInstanceId = ''

  # Secrets Manager id that stores Aurora credentials JSON.
  DbSecretId = 'khaleo/prod/db-credentials'

  # Optional: force Aurora endpoint instead of secret host/jdbc_url.
  AuroraEndpoint = ''

  # Local MySQL source/target.
  LocalDbHost = '127.0.0.1'
  LocalDbPort = 3306
  LocalDbName = 'khaleo_flashcard'
  LocalDbUser = 'khaleo'
  LocalDbPassword = 'khaleo'

  # Local forwarded port to Aurora through SSM.
  LocalForwardPort = 13306

  # Folder to write sql dump files.
  DumpDirectory = 'backend/build/db-sync'
}
