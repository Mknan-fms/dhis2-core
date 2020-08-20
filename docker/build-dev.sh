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
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ARTIFACTS="${DIR}/artifacts"

IMAGE="dhis2/core"
TAG="local"

#
## The Business
#

cd $DIR/..
docker build . --target builder -t dhis2/builder:local
    
docker run \
  -it \
  --rm \
  --name dhis2-incremental-build \
  -v "$PWD"/dhis-2:/usr/src/app \
  -v "$HOME"/.m2:/root/.m2 \
  -w /usr/src/app \
  dhis2/builder:local \
  bash -c "mvn install -T1C -f pom.xml -DskipTests && mvn install -T1C -f dhis-web/pom.xml -DskipTests"

cd $ARTIFACTS
rm -rf ./*
cp -f ../../dhis-2/dhis-web/dhis-web-portal/target/dhis.war ./dhis.war

sha256sum ./dhis.war > ./sha256sum.txt
md5sum ./dhis.war > ./md5sum.txt

cd -
ONLY_DEFAULT=1 ./docker/build-containers.sh $IMAGE:$TAG $TAG

d2 cluster up $D2CLUSTER --image $IMAGE:$TAG
