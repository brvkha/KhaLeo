#!/usr/bin/env bash
set -euo pipefail

ARTIFACT_URI="${1:-}"
SERVICE_NAME="${2:-flashcard-backend}"
TARGET_JAR_PATH="${3:-/opt/khaleo/flashcard-backend/app.jar}"

if [[ -z "${ARTIFACT_URI}" ]]; then
  echo "Usage: deploy-via-ssm.sh <s3://bucket/key> [service-name] [target-jar-path]" >&2
  exit 1
fi

sudo mkdir -p "$(dirname "${TARGET_JAR_PATH}")"
sudo aws s3 cp "${ARTIFACT_URI}" "${TARGET_JAR_PATH}"
sudo systemctl daemon-reload
sudo systemctl restart "${SERVICE_NAME}"
sudo systemctl is-active --quiet "${SERVICE_NAME}"
echo "Deployment completed for ${SERVICE_NAME} using ${ARTIFACT_URI}"