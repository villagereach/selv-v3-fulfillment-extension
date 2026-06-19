#!/bin/sh
set -e

# openlmis-fulfillment ships only as a Docker image, not to Maven. The compose
# `fulfillment` service drops its jar at /fulfillment; unpack its app classes for
# build.gradle to compile against (transitive deps are declared there).
rm -rf /tmp/fulfillment && mkdir -p /tmp/fulfillment
unzip -oq /fulfillment/service.jar 'BOOT-INF/classes/*' -d /tmp/fulfillment

# Assemble (and publish) the jar
gradle assemble
