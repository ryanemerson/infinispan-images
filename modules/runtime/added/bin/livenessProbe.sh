#!/bin/bash
set -e
HOSTNAME=$(cat /etc/hosts | grep -m 1 $(cat /proc/sys/kernel/hostname) | awk '{print $1;}')
curl --fail --silent --show-error --output /dev/null --head http://$HOSTNAME:11222/rest/v2/cache-managers/DefaultCacheManager/health