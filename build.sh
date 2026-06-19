#!/bin/sh
set -e

# openlmis-fulfillment is only distributed as a Docker image, not to Maven.
# The compose `fulfillment` service drops its Spring Boot jar at /fulfillment;
# extract the app classes into a thin jar and install it to the local Maven repo
# (its compile deps live in build.gradle, so the generated POM has none).
# Version comes from OL_FULFILLMENT_VERSION (.env) - see README.
FULFILLMENT_VERSION="${OL_FULFILLMENT_VERSION:?not set; create a .env file with OL_FULFILLMENT_VERSION=9.3.2 (see README)}"
SERVICE_JAR=/fulfillment/service.jar
JAR_BIN=/usr/lib/jvm/java-1.8-openjdk/bin/jar
WORK=/tmp/fulfillment-build
# Explicit local repo path (no reliance on $HOME); passed to Gradle below so its
# mavenLocal() resolves from exactly where we install, whatever user runs the build.
LOCAL_REPO="$WORK/maven-repo"
M2_DIR="$LOCAL_REPO/org/openlmis/openlmis-fulfillment/$FULFILLMENT_VERSION"

echo "Provisioning org.openlmis:openlmis-fulfillment:$FULFILLMENT_VERSION from $SERVICE_JAR"
rm -rf "$WORK"
mkdir -p "$WORK"
unzip -oq "$SERVICE_JAR" 'BOOT-INF/classes/*' -d "$WORK"
( cd "$WORK/BOOT-INF/classes" && "$JAR_BIN" cf "$WORK/openlmis-fulfillment-$FULFILLMENT_VERSION.jar" . )

mkdir -p "$M2_DIR"
cp "$WORK/openlmis-fulfillment-$FULFILLMENT_VERSION.jar" "$M2_DIR/"
cat > "$M2_DIR/openlmis-fulfillment-$FULFILLMENT_VERSION.pom" <<POM
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.openlmis</groupId>
  <artifactId>openlmis-fulfillment</artifactId>
  <version>$FULFILLMENT_VERSION</version>
</project>
POM

# Assemble (and publish) the jar
gradle assemble -Dmaven.repo.local="$LOCAL_REPO"
