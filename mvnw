#!/bin/sh
# Maven Wrapper script
# Download and run Maven if not present

set -e

MAVEN_VERSION="3.9.6"
MAVEN_HOME="${HOME}/.m2/wrapper/dists/apache-maven-${MAVEN_VERSION}"

if [ ! -d "${MAVEN_HOME}" ]; then
    echo "Downloading Maven ${MAVEN_VERSION}..."
    mkdir -p "${MAVEN_HOME}"
    curl -fsSL "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
        | tar -xzf - -C "${MAVEN_HOME}" --strip-components=1
fi

exec "${MAVEN_HOME}/bin/mvn" "$@"
