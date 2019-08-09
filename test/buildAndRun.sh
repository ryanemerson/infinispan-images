#!/bin/bash
reset
cd $INFINISPAN/infinispan-images

VOLUMES_DIR=~/docker/volumes
VOLUME=test-vol

# Cleanup old containers and images
docker container prune -f
docker rmi $(docker images -f "dangling=true" -q)
docker volume rm $VOLUME

# Create volume
docker volume create $VOLUME
sudo cp test/identities.yaml test/config.yaml $VOLUMES_DIR/$VOLUME/_data

# Build and run
cekit build docker
docker run -v $VOLUME:/user-config -e CONFIG_PATH=/user-config/config.yaml -e IDENTITIES_PATH="/user-config/identities.yaml" infinispan/server

