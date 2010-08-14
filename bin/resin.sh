#! /bin/sh
#
# See contrib/init.resin for /etc/rc.d/init.d startup script
#
# resin.sh         -- execs resin in the foreground
# resin.sh start   -- starts resin in the background
# resin.sh stop    -- stops resin
# resin.sh restart -- restarts resin
#
# resin.sh will return a status code if the wrapper detects an error, but
# some errors, like bind exceptions or Java errors, are not detected.
#
# To install, you'll need to configure JAVA_HOME and RESIN_HOME and
# copy contrib/init.resin to /etc/rc.d/init.d/resin.  Then
# use "unix# /sbin/chkconfig resin on"

if test -n "${JAVA_HOME}"; then
  if test -z "${JAVA_EXE}"; then
    JAVA_EXE=$JAVA_HOME/bin/java
  fi
fi

if test -z "${JAVA_EXE}"; then
  JAVA_EXE=java
fi  

#
# trace script and simlinks to find the wrapper
#
if test -z "${RESIN_HOME}"; then
  script=`/bin/ls -l $0 | awk '{ print $NF; }'`

  while test -h "$script"
  do
    script=`/bin/ls -l $script | awk '{ print $NF; }'`
  done

  bin=`dirname $script`
  RESIN_HOME="$bin/.."
fi  

exec $JAVA_EXE -jar ${RESIN_HOME}/lib/resin.jar $*
