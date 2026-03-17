#!/usr/bin/env bash
set -euo pipefail

log() { echo "[$(date -u +%Y-%m-%dT%H:%M:%SZ)] $*"; }
fail() { log "ERROR: $*" >&2; exit 1; }

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Missing command: $1"
}

ARTIFACT_URI="${1:-}"
SERVICE_NAME="${2:-flashcard-backend}"
BASE_DIR="/opt/khaleo/flashcard-backend"
CURRENT_JAR="${BASE_DIR}/current.jar"
NEW_JAR="${BASE_DIR}/new.jar"
BACKUP_JAR="${BASE_DIR}/backup.jar"
RUNTIME_ENV_PATH="${RUNTIME_ENV_PATH:-${BASE_DIR}/runtime-secrets.env}"
HEALTH_URL="${HEALTH_URL:-http://localhost:8080/actuator/health}"

[[ -z "$ARTIFACT_URI" ]] && fail "Missing artifact"
[[ "$ARTIFACT_URI" == s3://* ]] || fail "Artifact must be s3://"

require_command aws
require_command java
require_command systemctl
require_command curl

trap 'fail "Failed at line $LINENO"' ERR

sudo mkdir -p "$BASE_DIR"

# ======================
# 1. DOWNLOAD NEW VERSION
# ======================
log "Downloading new artifact..."
sudo aws s3 cp "$ARTIFACT_URI" "$NEW_JAR"

# ======================
# 2. BACKUP CURRENT
# ======================
if [[ -f "$CURRENT_JAR" ]]; then
  log "Backing up current version..."
  sudo cp "$CURRENT_JAR" "$BACKUP_JAR"
fi

# ======================
# 3. SWAP VERSION
# ======================
log "Switching to new version..."
sudo mv "$NEW_JAR" "$CURRENT_JAR"

# ======================
# 4. ENSURE SERVICE
# ======================
UNIT_FILE="/etc/systemd/system/${SERVICE_NAME}.service"

if ! sudo test -f "$UNIT_FILE"; then
  log "Creating systemd service..."

  cat <<EOF | sudo tee "$UNIT_FILE" >/dev/null
[Unit]
Description=KhaLeo Backend
After=network.target

[Service]
Type=simple
WorkingDirectory=${BASE_DIR}
EnvironmentFile=-${RUNTIME_ENV_PATH}
ExecStart=/usr/bin/java -jar ${CURRENT_JAR}
Restart=always
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
EOF

  sudo systemctl daemon-reload
  sudo systemctl enable "$SERVICE_NAME"
fi

# ======================
# 5. RESTART SERVICE
# ======================
log "Restarting service..."
sudo systemctl restart "$SERVICE_NAME" || sudo systemctl start "$SERVICE_NAME"

# ======================
# 6. HEALTH CHECK
# ======================
log "Waiting for service health..."

for i in {1..20}; do
  if curl -fs "$HEALTH_URL" | grep -q "UP"; then
    log "Service is healthy ✅"
    exit 0
  fi
  sleep 2
done

# ======================
# 7. ROLLBACK
# ======================
log "Health check failed ❌ → rolling back..."

if [[ -f "$BACKUP_JAR" ]]; then
  sudo cp "$BACKUP_JAR" "$CURRENT_JAR"
  sudo systemctl restart "$SERVICE_NAME"
  log "Rollback completed"
else
  fail "No backup available"
fi

fail "Deployment failed after rollback"