#!/usr/bin/env bash

set -Eeuo pipefail

DEPLOY_ROOT="/opt/safefam"
REPO_DIR="${DEPLOY_ROOT}/SafeFam_BE"
LOCK_FILE="${DEPLOY_ROOT}/.deploy.lock"

SERVICE_NAME="spring-backend"
CONTAINER_NAME="safefam-spring-server"

EXPECTED_SHA="${1:?Expected Git commit SHA is required}"

cd "${REPO_DIR}"

echo "[deploy] Waiting for deployment lock"

exec 9>"${LOCK_FILE}"

if ! flock -w 900 9; then
  echo "[deploy] Another BE or AI deployment is still running"
  exit 1
fi

if ! [[ "${EXPECTED_SHA}" =~ ^[0-9a-f]{40}$ ]]; then
  echo "[deploy] Invalid commit SHA: ${EXPECTED_SHA}"
  exit 1
fi

if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "[deploy] Backend repository has uncommitted changes"
  exit 1
fi

git checkout develop

PREVIOUS_SHA="$(git rev-parse HEAD)"

PREVIOUS_IMAGE_ID="$(
  docker inspect \
    --format='{{.Image}}' \
    "${CONTAINER_NAME}" 2>/dev/null || true
)"

PREVIOUS_IMAGE_REF="$(
  docker inspect \
    --format='{{.Config.Image}}' \
    "${CONTAINER_NAME}" 2>/dev/null || true
)"

ROLLBACK_REQUIRED=true

rollback() {
  exit_code=$?

  trap - EXIT
  set +e

  if [ "${exit_code}" -eq 0 ] || [ "${ROLLBACK_REQUIRED}" = false ]; then
    exit "${exit_code}"
  fi

  echo "[rollback] Deployment failed"
  echo "[rollback] Restoring commit: ${PREVIOUS_SHA}"

  cd "${REPO_DIR}"
  git reset --hard "${PREVIOUS_SHA}"

  if [ -n "${PREVIOUS_IMAGE_ID}" ] && [ -n "${PREVIOUS_IMAGE_REF}" ]; then
    echo "[rollback] Restoring image: ${PREVIOUS_IMAGE_ID}"

    if ! docker tag "${PREVIOUS_IMAGE_ID}" "${PREVIOUS_IMAGE_REF}"; then
      echo "[rollback] Failed to restore the previous image tag"
      exit "${exit_code}"
    fi

    if ! docker compose up \
      -d \
      --no-deps \
      --force-recreate \
      "${SERVICE_NAME}"; then
      echo "[rollback] Failed to recreate the previous backend container"
      exit "${exit_code}"
    fi
  else
    echo "[rollback] Previous image is unavailable; rebuilding previous commit"

    if ! docker compose build "${SERVICE_NAME}"; then
      echo "[rollback] Failed to rebuild the previous backend image"
      exit "${exit_code}"
    fi

    if ! docker compose up \
      -d \
      --no-deps \
      "${SERVICE_NAME}"; then
      echo "[rollback] Failed to start the previous backend image"
      exit "${exit_code}"
    fi
  fi

  echo "[rollback] Waiting for backend health check"

  backend_address="$(docker compose port "${SERVICE_NAME}" 8080)"
  health_url="http://${backend_address}/actuator/health"

  for attempt in $(seq 1 30); do
    health_response="$(
      curl \
        --fail \
        --silent \
        --show-error \
        --max-time 5 \
        "${health_url}" 2>/dev/null || true
    )"

    echo "[rollback] Health check ${attempt}/30"

    if echo "${health_response}" | grep -q '"status":"UP"'; then
      echo "[rollback] Previous backend version restored successfully"
      exit "${exit_code}"
    fi

    sleep 5
  done

  echo "[rollback] Previous backend health check failed"
  docker compose logs --tail=100 "${SERVICE_NAME}" || true

  exit "${exit_code}"
}

trap rollback EXIT

echo "[deploy] Previous commit: ${PREVIOUS_SHA}"
echo "[deploy] Requested commit: ${EXPECTED_SHA}"

echo "[deploy] Updating develop branch"

git fetch origin develop:refs/remotes/origin/develop
git pull --ff-only origin develop

DEPLOYED_SHA="$(git rev-parse HEAD)"

if [ "${DEPLOYED_SHA}" != "${EXPECTED_SHA}" ]; then
  echo "[deploy] Commit mismatch"
  echo "[deploy] Expected: ${EXPECTED_SHA}"
  echo "[deploy] Actual:   ${DEPLOYED_SHA}"
  exit 1
fi

echo "[deploy] Validating Docker Compose configuration"

docker compose config --quiet

echo "[deploy] Recording dependency container IDs"

POSTGRES_ID="$(docker compose ps -q postgres)"
RABBITMQ_ID="$(docker compose ps -q rabbitmq)"
REDIS_ID="$(docker compose ps -q redis)"
AI_ID="$(docker compose ps -q fastapi-ai)"

if [ -z "${POSTGRES_ID}" ]; then
  echo "[deploy] PostgreSQL container is not running"
  exit 1
fi

if [ -z "${RABBITMQ_ID}" ]; then
  echo "[deploy] RabbitMQ container is not running"
  exit 1
fi

if [ -z "${REDIS_ID}" ]; then
  echo "[deploy] Redis container is not running"
  exit 1
fi

if [ -z "${AI_ID}" ]; then
  echo "[deploy] FastAPI AI container is not running"
  exit 1
fi

echo "[deploy] Building ${SERVICE_NAME}"

docker compose build "${SERVICE_NAME}"

echo "[deploy] Replacing ${SERVICE_NAME} only"

docker compose up \
  -d \
  --no-deps \
  "${SERVICE_NAME}"

echo "[deploy] Waiting for backend health check"

BACKEND_ADDRESS="$(docker compose port "${SERVICE_NAME}" 8080)"
HEALTH_URL="http://${BACKEND_ADDRESS}/actuator/health"

for attempt in $(seq 1 30); do
  health_response="$(
    curl \
      --fail \
      --silent \
      --show-error \
      --max-time 5 \
      "${HEALTH_URL}" 2>/dev/null || true
  )"

  echo "[deploy] Health check ${attempt}/30"

  if echo "${health_response}" | grep -q '"status":"UP"'; then
    echo "[deploy] Backend health check succeeded"
    break
  fi

  if [ "${attempt}" -eq 30 ]; then
    echo "[deploy] Backend health check failed"
    docker compose logs --tail=100 "${SERVICE_NAME}" || true
    exit 1
  fi

  sleep 5
done

echo "[deploy] Verifying dependency containers were not replaced"

if [ "${POSTGRES_ID}" != "$(docker compose ps -q postgres)" ]; then
  echo "[deploy] PostgreSQL container was replaced unexpectedly"
  exit 1
fi

if [ "${RABBITMQ_ID}" != "$(docker compose ps -q rabbitmq)" ]; then
  echo "[deploy] RabbitMQ container was replaced unexpectedly"
  exit 1
fi

if [ "${REDIS_ID}" != "$(docker compose ps -q redis)" ]; then
  echo "[deploy] Redis container was replaced unexpectedly"
  exit 1
fi

if [ "${AI_ID}" != "$(docker compose ps -q fastapi-ai)" ]; then
  echo "[deploy] FastAPI AI container was replaced unexpectedly"
  exit 1
fi

ROLLBACK_REQUIRED=false
trap - EXIT

echo "[deploy] Deployment completed successfully"
echo "[deploy] Deployed commit: ${DEPLOYED_SHA}"
