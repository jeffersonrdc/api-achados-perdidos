#!/bin/sh
# Limpeza segura pós-deploy — não remove volumes nem imagens em uso.
# Variáveis opcionais: IMAGE_CI, KEEP_CI_IMAGES (default 3), BUILD_CACHE_UNTIL (default 168h)

set -u

KEEP_CI_IMAGES="${KEEP_CI_IMAGES:-3}"
BUILD_CACHE_UNTIL="${BUILD_CACHE_UNTIL:-168h}"

echo ">> Limpeza Docker..."

echo "--- Antes ---"
docker system df 2>/dev/null || true

docker container prune -f --filter until=24h 2>/dev/null || true
docker image prune -f 2>/dev/null || true

if [ -n "${IMAGE_CI:-}" ]; then
  REPO="${IMAGE_CI%%:ci-*}"
  if [ "$REPO" != "$IMAGE_CI" ]; then
    docker images "$REPO" --format '{{.Tag}}' 2>/dev/null \
      | grep '^ci-' \
      | sort -r \
      | awk -v keep="$KEEP_CI_IMAGES" 'NR > keep { print }' \
      | while read -r tag; do
          [ -n "$tag" ] || continue
          echo "   removendo ${REPO}:${tag}"
          docker rmi -f "${REPO}:${tag}" 2>/dev/null || true
        done
  fi
  docker rmi -f "$IMAGE_CI" 2>/dev/null || true
fi

docker builder prune -f --filter "until=${BUILD_CACHE_UNTIL}" 2>/dev/null || true
docker network prune -f 2>/dev/null || true

echo "--- Depois ---"
docker system df 2>/dev/null || true
echo ">> Limpeza concluída."
