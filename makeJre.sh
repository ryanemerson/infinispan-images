#!/bin/bash
reset
set -e
rm -rf jre
#jlink --no-header-files --no-man-pages --module-path $JAVA_HOME/jmods:lib --add-modules java.base,java.desktop,java.logging,java.management,java.naming,java.security.sasl,java.transaction.xa,java.xml,jdk.unsupported --output jre
#strip -p --strip-unneeded jre/lib/server/libjvm.so
#zip -r runtime.zip jre

id=$(docker create image-name)
docker cp $id:/opt/runtime.zip - > runtime.zip
docker rm -v $id

cekit build docker
docker run infinispan/server
