#!/bin/bash

TAGV=`git describe --tags --abbrev=0`
TAGT=`git describe --tags`
BR=`git describe --all | sed s/"\(.*\)\/\([^/]*\)$"/"\2"/`
DATE=`git log --pretty=format:"%ad" --date=short -1 | sed s/"-"/""/g`

if [[ "$TAGV" == "$TAGT" ]]; then
VER=$TAGV
else
VER=$BR
fi

if [[ -z $1 ]]; then
echo -n $VER
else
sed -i -e s/"@@Version@@"/$VER/ -e s/"@@VersionDate@@"/$DATE/ $1
fi
