#!/usr/bin/env bash
set -uo pipefail

: "${IMAGE:?}"

SUFFIX=${GITHUB_RUN_ID:-local}
APP="feed-service-boot-app-${SUFFIX}"
HOST_PORT=18085
APP_LIMIT_SECONDS=120
HEALTH_TIMEOUT_SECONDS=10

cleanup() {
  docker rm -f "$APP" >/dev/null 2>&1
}
trap cleanup EXIT

docker run -d --name "$APP" -p "${HOST_PORT}:8080" \
  -e SPRING_PROFILES_ACTIVE=qa \
  -e SUPABASE_URL=https://example.supabase.co \
  -e INTERNAL_TOKEN=boot-check-token -e CORS_ALLOWED_ORIGINS=http://localhost:5173 \
  "$IMAGE" >/dev/null || exit 1

verdict=""
for _ in $(seq 1 "$APP_LIMIT_SECONDS"); do
  logs=$(docker logs "$APP" 2>&1)
  if grep -q 'Application run failed' <<<"$logs" || [ "$(docker inspect -f '{{.State.Running}}' "$APP")" != "true" ]; then
    verdict=RED
    break
  fi
  if grep -q 'Started FeedServiceApplication' <<<"$logs"; then
    verdict=GREEN
    break
  fi
  sleep 1
done

if [ "$verdict" = GREEN ]; then
  if health=$(curl -sSf --max-time "$HEALTH_TIMEOUT_SECONDS" "http://localhost:${HOST_PORT}/actuator/health"); then
    echo "health: ${health}"
    jq -e '.status == "UP"' >/dev/null <<<"$health" || verdict=RED
  else
    verdict=RED
  fi
fi

if [ "$verdict" != GREEN ]; then
  echo "::group::application log"
  docker logs "$APP" 2>&1
  echo "::endgroup::"
  echo "::error::native boot check failed: ${verdict:-timed out after ${APP_LIMIT_SECONDS}s}"
  exit 1
fi

echo "native boot check passed"
