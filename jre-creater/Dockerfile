FROM registry.access.redhat.com/ubi8/ubi:latest

USER root

RUN dnf search java-11-openjdk-*

RUN dnf --setopt=tsflags=nodocs install -y java-11-openjdk-devel java-11-openjdk-jmods\
    && dnf clean all \
    && rpm -q java-11-openjdk-devel java-11-openjdk-jmods

WORKDIR /opt

RUN jlink --no-header-files --no-man-pages --module-path $JAVA_HOME/jmods:lib --add-modules java.base,java.desktop,java.logging,java.management,java.naming,java.security.sasl,java.transaction.xa,java.xml,jdk.unsupported --output jre

RUN strip -p --strip-unneeded jre/lib/server/libjvm.so

RUN zip -r runtime.zip jre
