#!/usr/bin/env bash
set -euo pipefail

log() {
  echo "[$(date -u +%Y-%m-%dT%H:%M:%SZ)] $*"
}

fail() {
  log "ERROR: $*" >&2
  exit 1
}

require_command() {
  local cmd="$1"
  command -v "${cmd}" >/dev/null 2>&1 || fail "Missing required command: ${cmd}"
}

json_field() {
  local payload="$1"
  local field_name="$2"
  python3 -c "import json, sys; data=json.loads(sys.argv[1]); value=data.get(sys.argv[2], ''); print(value if value is not None else '')" "$payload" "$field_name"
}

ARTIFACT_URI="${1:-}"
SERVICE_NAME="${2:-flashcard-backend}"
TARGET_JAR_PATH="${3:-/opt/khaleo/flashcard-backend/app.jar}"
RUNTIME_ENV_PATH="${RUNTIME_ENV_PATH:-/opt/khaleo/flashcard-backend/runtime-secrets.env}"
DB_SECRET_ID="${DB_SECRET_ID:-}"
JWT_SECRET_ID="${JWT_SECRET_ID:-}"
SES_SECRET_ID="${SES_SECRET_ID:-}"

if [[ -z "${ARTIFACT_URI}" ]]; then
  fail "Usage: deploy-via-ssm.sh <s3://bucket/key> [service-name] [target-jar-path]"
fi

[[ "${ARTIFACT_URI}" == s3://* ]] || fail "Artifact URI must start with s3://"

require_command aws
require_command python3
require_command sudo
require_command systemctl

trap 'fail "Deployment failed at line $LINENO"' ERR

sudo mkdir -p "$(dirname "${TARGET_JAR_PATH}")"

fetch_secret() {
  local secret_id="$1"
  if [[ -z "${secret_id}" ]]; then
    return 0
  fi
  aws secretsmanager get-secret-value --secret-id "${secret_id}" --query SecretString --output text
}

write_runtime_env() {
  local db_secret jwt_secret ses_secret
  local db_url db_username db_password jwt_value ses_access_key ses_secret_key
  db_secret="$(fetch_secret "${DB_SECRET_ID}")"
  jwt_secret="$(fetch_secret "${JWT_SECRET_ID}")"
  ses_secret="$(fetch_secret "${SES_SECRET_ID}")"

  db_url="$(json_field "${db_secret:-{}}" "url")"
  db_username="$(json_field "${db_secret:-{}}" "username")"
  db_password="$(json_field "${db_secret:-{}}" "password")"
  jwt_value="$(json_field "${jwt_secret:-{}}" "secret")"
  if [[ -z "${jwt_value}" ]]; then
    jwt_value="${jwt_secret}"
  fi
  ses_access_key="$(json_field "${ses_secret:-{}}" "accessKeyId")"
  ses_secret_key="$(json_field "${ses_secret:-{}}" "secretAccessKey")"

  sudo install -d -m 0755 "$(dirname "${RUNTIME_ENV_PATH}")"
  {
    echo "# Generated during deploy at $(date -u +%Y-%m-%dT%H:%M:%SZ)"
    [[ -n "${db_url}" ]] && echo "DB_URL=${db_url}"
    [[ -n "${db_username}" ]] && echo "DB_USERNAME=${db_username}"
    [[ -n "${db_password}" ]] && echo "DB_PASSWORD=${db_password}"
    [[ -n "${jwt_value}" ]] && echo "JWT_SECRET=${jwt_value}"
    [[ -n "${ses_access_key}" ]] && echo "SES_ACCESS_KEY_ID=${ses_access_key}"
    [[ -n "${ses_secret_key}" ]] && echo "SES_SECRET_ACCESS_KEY=${ses_secret_key}"
  } | sudo tee "${RUNTIME_ENV_PATH}" >/dev/null
}

ensure_service_unit() {
  local unit_file="/etc/systemd/system/${SERVICE_NAME}.service"

  if sudo test -f "${unit_file}"; then
    log "Service file already exists: ${unit_file}"
    return 0
  fi

  log "Creating systemd service: ${SERVICE_NAME}"

  cat <<EOF | sudo tee "${unit_file}" >/dev/null
[Unit]
Description=KhaLeo Backend Service
After=network.target

[Service]
Type=simple
WorkingDirectory=$(dirname "${TARGET_JAR_PATH}")
EnvironmentFile=-${RUNTIME_ENV_PATH}
ExecStart=/usr/bin/java -jar ${TARGET_JAR_PATH}
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

  sudo chmod 0644 "${unit_file}"
  sudo systemctl daemon-reload
  sudo systemctl enable "${SERVICE_NAME}"
}

write_runtime_env
log "Runtime secret environment prepared at ${RUNTIME_ENV_PATH}"
sudo aws s3 cp "${ARTIFACT_URI}" "${TARGET_JAR_PATH}"
log "Copied artifact to ${TARGET_JAR_PATH}"
ensure_service_unit
sudo systemctl daemon-reload
sudo systemctl restart "${SERVICE_NAME}" || sudo systemctl start "${SERVICE_NAME}"
sudo systemctl is-active --quiet "${SERVICE_NAME}"
log "Deployment completed for ${SERVICE_NAME} using ${ARTIFACT_URI}"