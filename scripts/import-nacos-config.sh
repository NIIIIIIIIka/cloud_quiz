#!/bin/sh

set -eu

NACOS_ADDR="${NACOS_ADDR:-http://nacos:8848}"
NACOS_NAMESPACE="${NACOS_NAMESPACE:-dev}"
NACOS_GROUP="${NACOS_GROUP:-dev}"
CONFIG_DIR="${CONFIG_DIR:-/nacos_config}"

log() {
  printf "%s\n" "$1"
}

log "Using Nacos: ${NACOS_ADDR}, namespace: ${NACOS_NAMESPACE}, group: ${NACOS_GROUP}"
log "Config directory: ${CONFIG_DIR}"

if ! ls "${CONFIG_DIR}"/*.yaml >/dev/null 2>&1; then
  log "No .yaml files found in ${CONFIG_DIR}"
  exit 1
fi

log "Waiting for Nacos to be ready..."
retries=60
while ! curl -sf "${NACOS_ADDR}/nacos/v1/console/health/liveness" >/dev/null 2>&1; do
  retries=$((retries - 1))
  if [ "${retries}" -le 0 ]; then
    log "Nacos is not ready after waiting."
    exit 1
  fi
  sleep 2
done
log "Nacos is up."

# Create namespace if it does not exist (safe to call repeatedly)
curl -sf -X POST "${NACOS_ADDR}/nacos/v1/console/namespaces" \
  --data-urlencode "customNamespaceId=${NACOS_NAMESPACE}" \
  --data-urlencode "namespaceName=${NACOS_NAMESPACE}" \
  --data-urlencode "namespaceDesc=Created by import script" \
  >/dev/null || log "Namespace ${NACOS_NAMESPACE} already exists."

for file in "${CONFIG_DIR}"/*.yaml; do
  dataId="$(basename "${file}")"
  log "Publishing ${dataId}..."
  curl -sf -X POST "${NACOS_ADDR}/nacos/v1/cs/configs" \
    --data-urlencode "dataId=${dataId}" \
    --data-urlencode "group=${NACOS_GROUP}" \
    --data-urlencode "tenant=${NACOS_NAMESPACE}" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content@${file}" \
    >/dev/null || {
      log "Failed to publish ${dataId}"
      exit 1
    }
done

log "Nacos config import finished."
