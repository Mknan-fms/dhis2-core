#!/usr/bin/env bash

#
## bash environment
#

if test "$BASH" = "" || "$BASH" -uc "a=();true \"\${a[@]}\"" 2>/dev/null; then
    # Bash 4.4, Zsh
    set -euo pipefail
else
    # Bash 4.3 and older chokes on empty arrays with set -u.
    set -eo pipefail
fi
shopt -s nullglob globstar

#
## script environment
#

D2CLUSTER="$1"
IMAGE=dhis2/core
TAG=local

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT="$DIR/.."
ARTIFACTS="$ROOT/docker/artifacts"

#
## The Business
#

# Requires maven to be on the classpath
# Skips clean and test phases

mvn clean install -T1C -DskipTests=true -f $DIR/pom.xml
mvn clean install -T1C -DskipTests=true -f $DIR/dhis-web/pom.xml

rm -rf "$ARTIFACTS/*"
mkdir -p "$ARTIFACTS"
cp -f "$DIR/dhis-web/dhis-web-portal/target/dhis.war" "$ARTIFACTS/dhis.war"

cd $ARTIFACTS
sha256sum ./dhis.war > ./sha256sum.txt
md5sum ./dhis.war > ./md5sum.txt


ONLY_DEFAULT=1 $ROOT/docker/build-containers.sh $IMAGE:$TAG $TAG

d2 cluster up $D2CLUSTER --image $IMAGE:$TAG
d2 cluster logs $D2CLUSTER