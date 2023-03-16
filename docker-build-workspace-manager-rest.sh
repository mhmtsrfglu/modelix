#!/bin/sh

set -e

TAG=$( ./modelix-version.sh )

MODELIX_TARGET_PLATFORM="${MODELIX_TARGET_PLATFORM:=linux/amd64}"

if [ "${CI}" = "true" ]; then
  docker buildx build --platform linux/amd64,linux/arm64 --push -f Dockerfile-workspace-manager-rest \
  -t docker.cppintra.net/ics-inf-docker-all/modelix/modelix-ws-manager-rest:latest -t "docker.cppintra.net/ics-inf-docker-all/modelix/modelix-ws-manager-rest:${TAG}" .
else
  docker build --platform "${MODELIX_TARGET_PLATFORM}" -f Dockerfile-workspace-manager-rest \
  -t docker.cppintra.net/ics-inf-docker-all/modelix/modelix-ws-manager-rest:latest -t "docker.cppintra.net/ics-inf-docker-all/modelix/modelix-ws-manager-rest:${TAG}" .
fi

sed -i.bak -E "s/  wsManagerRest: \".*\"/  wsManagerRest: \"${TAG}\"/" helm/dev.yaml
rm helm/dev.yaml.bak

echo "Preparing to send docker"

(
  docker login docker.cppintra.net/ics-inf-docker-all

  docker push "docker.cppintra.net/ics-inf-docker-all/modelix/modelix-ws-manager-rest:${TAG}"
)
