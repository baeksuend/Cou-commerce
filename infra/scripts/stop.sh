#!/usr/bin/env bash
# scripts/stop.sh
# - 각 infra/* 경로로 이동해 docker compose down --volumes 실행
set -euo pipefail

if docker compose version >/dev/null 2>&1; then
  DOCKER_COMPOSE_CMD="docker compose"
elif docker-compose version >/dev/null 2>&1; then
  DOCKER_COMPOSE_CMD="docker-compose"
else
  echo "ERROR: docker compose 또는 docker-compose가 설치되어 있지 않습니다." >&2
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

COMPOSE_DIRS=(
  "${ROOT_DIR}/mysql"
  "${ROOT_DIR}/redis"
)

for d in "${COMPOSE_DIRS[@]}"; do
  if [ -f "${d}/docker-compose.yml" ] || [ -f "${d}/docker-compose.yaml" ]; then
    echo "=== Stopping services in ${d} ==="
    pushd "${d}" >/dev/null
    $DOCKER_COMPOSE_CMD down --volumes
    popd >/dev/null
  else
    echo "Skip: no docker-compose.yml in ${d}"
  fi
done

echo "All requested compose stacks stopped."
