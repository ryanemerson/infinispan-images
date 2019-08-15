#!/bin/bash
set -e
HOSTNAME=$(cat /etc/hosts | grep -m 1 $(cat /proc/sys/kernel/hostname) | awk '{print $1;}')
curl --fail --silent --show-error -X GET http://$HOSTNAME:11222/rest/v2/cache-managers/DefaultCacheManager/health \
 | grep -Po '"health_status":.*?[^\\]",' \
 | grep -q '\"HEALTHY\"'
