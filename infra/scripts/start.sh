#!/usr/bin/env bash
# scripts/start.sh
# - 각 infra/* 경로로 이동해 docker compose up -d 실행
# - docker compose v2 / docker-compose(v1) 자동 감지
# - 네트워크가 없으면 생성
set -euo pipefail

# detect docker compose command
if docker compose version >/dev/null 2>&1; then
  DOCKER_COMPOSE_CMD="docker compose"
elif docker-compose version >/dev/null 2>&1; then
  DOCKER_COMPOSE_CMD="docker-compose"
else
  echo "ERROR: docker compose 또는 docker-compose가 설치되어 있지 않습니다." >&2
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
NETWORK_NAME="${NETWORK_NAME:-cucommerce_network}"

# ensure network exists
if ! docker network inspect "${NETWORK_NAME}" >/dev/null 2>&1; then
  echo "Creating docker network '${NETWORK_NAME}'..."
  docker network create "${NETWORK_NAME}"
else
  echo "Docker network '${NETWORK_NAME}' exists."
fi

# list of compose directories (order matters if you want)
COMPOSE_DIRS=(
  "${ROOT_DIR}/mysql"
  "${ROOT_DIR}/redis"
  "${ROOT_DIR}/monitoring"
  "${ROOT_DIR}/elk"
)

for d in "${COMPOSE_DIRS[@]}"; do
  if [ -f "${d}/docker-compose.yml" ] || [ -f "${d}/docker-compose.yaml" ]; then
    echo "=== Starting services in ${d} ==="
    pushd "${d}" >/dev/null
    # docker compose will automatically read .env in this directory if present
    $DOCKER_COMPOSE_CMD up -d --quiet-pull
    popd >/dev/null
  else
    echo "Skip: no docker-compose.yml in ${d}"
  fi
done

echo "All requested compose stacks started."
