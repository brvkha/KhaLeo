# Immutable Backend AMI Bake

This folder defines the Packer build used by backend deployment.

## Inputs

- `aws_region`: AWS region for AMI build.
- `subnet_id`: Public subnet used by temporary Packer builder instance.
- `app_jar_path`: Local path to built backend jar.
- `db_secret_id`, `jwt_secret_id`, `ses_secret_id`: Runtime secret IDs written to runtime environment file.
- `runtime_env_path`: Path to runtime environment file consumed by systemd service.
- `app_sha`: Commit SHA used for AMI naming and tags.

## Output

- AMI containing:
  - `/opt/khaleo/flashcard-backend/current.jar`
  - `flashcard-backend.service`
  - `/usr/local/bin/render-runtime-secrets.sh`

## Runtime Behavior

On each service start, `render-runtime-secrets.sh` pulls DB credentials from AWS Secrets Manager and writes `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` into runtime environment file before launching Spring Boot.
