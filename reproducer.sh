#!/bin/bash
reset
set -e

VERSION="SNAPSHOT"
VOLUMES_DIR=~/docker/volumes
VOLUME=example-vol

# Cleanup old containers and images
docker container prune -f
docker rmi $(docker images -f "dangling=true" -q) || true
docker volume rm $VOLUME || true

# Create volume
docker volume create $VOLUME
sudo cp infinispan.xml  $VOLUMES_DIR/$VOLUME/_data

# Build and run
docker run -it -v example-vol:/user-config --entrypoint "/opt/infinispan/bin/server.sh"  quay.io/infinispan/server:10.0.1.Final -b SITE_LOCAL -c /user-config/infinispan.xml

