#!/bin/bash
reset
set -e
oc delete project reproducer || true
oc new-project reproducer
oc create configmap infinispan-configuration --from-file=infinispan-inline-stack.xml
oc create -f reproducer-template.yaml
oc new-app --template=reproducer-template
