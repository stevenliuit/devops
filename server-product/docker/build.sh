#!/bin/bash

set -e

cd ../target
rm -rf docker
cp -r ../docker docker
buildVersion=`ls onedev-*.zip|sed -e 's/onedev-\(.*\).zip/\1/'`

unzip onedev-$buildVersion.zip -d docker
mv docker/onedev-$buildVersion docker/app
cp sandbox/site/lib/mysql* sandbox/site/lib/postgresql* docker/app/site/lib

docker build -f docker/Dockerfile.server -t stevenliuit/devops-server:$buildVersion docker

cp -r agent docker/
docker build -f docker/Dockerfile.agent -t stevenliuit/devops-agent:latest docker
