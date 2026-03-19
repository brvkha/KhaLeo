#!/usr/bin/env bash
set -euo pipefail

SERVICE_NAME="flashcard-backend"
BASE_DIR="/opt/khaleo/flashcard-backend"
TARGET_JAR_PATH="${BASE_DIR}/current.jar"
RUNTIME_ENV_PATH="${RUNTIME_ENV_PATH:-${BASE_DIR}/runtime-secrets.env}"

sudo dnf install -y java-17-amazon-corretto-headless awscli python3

sudo mkdir -p "${BASE_DIR}"
sudo mv /tmp/current.jar "${TARGET_JAR_PATH}"
sudo chmod 0644 "${TARGET_JAR_PATH}"

cat <<'SCRIPT' | sudo tee /usr/local/bin/render-runtime-secrets.sh >/dev/null
#!/usr/bin/env bash
set -euo pipefail

RUNTIME_ENV_PATH="${RUNTIME_ENV_PATH:-/opt/khaleo/flashcard-backend/runtime-secrets.env}"

if [[ ! -f "${RUNTIME_ENV_PATH}" ]]; then
  exit 0
fi

set -a
# shellcheck disable=SC1090
source "${RUNTIME_ENV_PATH}"
set +a

if [[ -z "${DB_SECRET_ID:-}" ]]; then
  exit 0
fi

SECRET_JSON=$(aws secretsmanager get-secret-value \
  --secret-id "${DB_SECRET_ID}" \
  --query SecretString \
  --output text)

DB_LINES=$(python3 - "${SECRET_JSON}" <<'PY'
import json
import sys

raw = sys.argv[1].strip()
if not raw:
    raise SystemExit("DB secret is empty")

secret = json.loads(raw)

def pick(*keys):
    for key in keys:
        value = secret.get(key)
        if isinstance(value, str) and value.strip():
            return value.strip()
        if isinstance(value, (int, float)):
            return str(value)
    return ""

jdbc_url = pick("jdbc_url", "jdbcUrl", "url")
host = pick("host", "hostname", "endpoint")
port = pick("port") or "3306"
db_name = pick("dbname", "dbName", "database", "database_name")
username = pick("username", "user")
password = pick("password", "pass")

if not jdbc_url and host and db_name:
    jdbc_url = f"jdbc:mysql://{host}:{port}/{db_name}?useSSL=true&requireSSL=true&verifyServerCertificate=true"

if not jdbc_url:
    raise SystemExit("DB secret missing JDBC URL or host/database fields")

if "localhost" in jdbc_url or "127.0.0.1" in jdbc_url:
    raise SystemExit("DB URL must not point to localhost")

if not username or not password:
    raise SystemExit("DB secret missing username/password")

print(f"DB_URL={jdbc_url}")
print(f"DB_USERNAME={username}")
print(f"DB_PASSWORD={password}")
PY
)

TMP_FILE=$(mktemp)

{
  grep -E -v '^(DB_URL|DB_USERNAME|DB_PASSWORD)=' "${RUNTIME_ENV_PATH}" || true
  echo "${DB_LINES}"
} > "${TMP_FILE}"

sudo install -m 600 "${TMP_FILE}" "${RUNTIME_ENV_PATH}"
rm -f "${TMP_FILE}"
SCRIPT

sudo chmod +x /usr/local/bin/render-runtime-secrets.sh

cat <<EOF | sudo tee /etc/systemd/system/${SERVICE_NAME}.service >/dev/null
[Unit]
Description=KhaLeo Backend
After=network.target

[Service]
Type=simple
WorkingDirectory=${BASE_DIR}
EnvironmentFile=-${RUNTIME_ENV_PATH}
ExecStartPre=/usr/local/bin/render-runtime-secrets.sh
ExecStart=/usr/bin/java -jar ${TARGET_JAR_PATH}
Restart=always
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
EOF

cat <<EOF | sudo tee "${RUNTIME_ENV_PATH}" >/dev/null
DB_SECRET_ID=${DB_SECRET_ID}
JWT_SECRET_ID=${JWT_SECRET_ID}
SES_SECRET_ID=${SES_SECRET_ID}
RUNTIME_ENV_PATH=${RUNTIME_ENV_PATH}
EOF

sudo chmod 600 "${RUNTIME_ENV_PATH}"
sudo systemctl daemon-reload
sudo systemctl enable "${SERVICE_NAME}.service"

echo "Commit SHA: ${APP_SHA:-unknown}" | sudo tee ${BASE_DIR}/build-info.txt >/dev/null
