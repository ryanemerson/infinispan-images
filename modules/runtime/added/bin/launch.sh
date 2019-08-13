#!/bin/sh
# ===================================================================================
# Entry point for the image which initiates any pre-launch config required before
# executing the server.
# ===================================================================================

set -e
if [ -n "${DEBUG}" ]; then
  set -x
fi

SERVER_ROOT=/opt/infinispan

# If $ISPN_HOME does not comply with SERVER_ROOT, then create a symlink. Necessary in order to allow overriding of location at build time
if [ ${SERVER_ROOT} != ${ISPN_HOME} ]; then
  ln -s ${SERVER_ROOT} ${ISPN_HOME}
fi

java -jar ${ISPN_HOME}/bin/config-generator.jar ${CONFIG_PATH} ${IDENTITIES_PATH} ${ISPN_HOME}/server/conf

if [ -n "${DEBUG}" ]; then
  cat ${SERVER_ROOT}/server/conf/*.xml
fi

exec ${ISPN_HOME}/bin/server.sh
