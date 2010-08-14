AC_INIT(modules/c/src/resin_os/boot.c)
AC_PREFIX_DEFAULT(/usr/local/resin)
AC_CONFIG_AUX_DIR(automake)	

FULL_VERSION="Resin $VERSION -- `date`"
AC_SUBST(FULL_VERSION)

dnl Get os info
AC_CANONICAL_SYSTEM

AC_CONFIG_MACRO_DIR([m4])
AM_INIT_AUTOMAKE(resin, 4.0.2)

dnl Checks for programs.
AC_PROG_CC
AC_PROG_LD
AC_PROG_EGREP

dnl Checks for poll.h
AC_CHECK_HEADER(sys/poll.h, [ CFLAGS="$CFLAGS -DPOLL"; poll=1 ])
dnl Checks for epoll.h
AC_CHECK_HEADER(sys/epoll.h, [ CFLAGS="$CFLAGS -DEPOLL"; epoll=1 ])

#
# Resin home
#

if test "$prefix" == "NONE"; then
  resin_home="$ac_default_prefix"
else
  resin_home="$prefix"
fi  
AC_SUBST(resin_home)

#
# --with-resin-root
#

AC_ARG_WITH(resin-root,
[  --with-resin-root=DIR  resin data directory
], [
resin_root=${withval}
])

if test -z "$resin_root"; then
  if test "$prefix" == "NONE"; then
    resin_root="/var/www"
  else
    resin_root="$prefix"
  fi
fi

AC_SUBST(resin_root)

#
# --with-resin-conf
#
AC_SUBST(resin_conf)

AC_ARG_WITH(resin-conf,
[  --with-resin-conf=DIR  resin configuration directory
], [
resin_conf=${withval}
])

if test -z "$resin_conf"; then
  if test "$prefix" == "NONE"; then
    resin_conf="/etc/resin"
  else
    resin_conf="$prefix/conf"
  fi
fi

#
# --with-resin-log
#
AC_SUBST(resin_log)

AC_ARG_WITH(resin-log,
[  --with-resin-log=DIR  resin log directory
], [
resin_log=${withval}
])

if test -z "$resin_log"; then
  if test "$prefix" == "NONE"; then
    resin_log="/var/log/resin"
  else
    resin_log="$prefix/log"
  fi
fi

if test -d "/etc/init.d/rc.d"; then
  resin_init_d="/etc/init.d/rc.d/resin"
elif test -d "/etc/init.d"; then
  resin_init_d="/etc/init.d/resin"
fi

AC_SUBST(resin_init_d)

resin_pro=.
resin_os=../resin_os
is_dev=""

if test -r "../pro/modules/c/src/resin/resin.h"; then
  resin_pro="../pro"
  resin_os="../../../../../resin/modules/c/src/resin_os"
  is_dev="true"
fi  

AC_SUBST(resin_os)
AC_SUBST(resin_pro)

#
# --with-java-home
#
AC_ARG_WITH(java-home,
[  --with-java-home=DIR  java home
], [
JAVA_HOME=${withval}
])

if test -z "$JAVA_HOME"; then
  java=`which java 2>/dev/null`
  if test $? = "0"; then
    while test -h "$java"
    do
      head=`dirname $java`
      tail=`/bin/ls -l $java | awk '{ print $NF; }'`
      if test -f "$tail"; then
        java=$tail
      else
        java=$head/$tail
      fi
    done

    javabin=`dirname $java`
    JAVA_HOME=`cd $javabin/..; pwd`

    # we're in $JAVA_HOME/jre
    if test -f "$JAVA_HOME/lib/rt.jar"; then
      JAVA_HOME=`cd $JAVA_HOME/..; pwd`
    elif test -d "/usr/java"; then
      JAVA_HOME=/usr/java
    elif test -f "/System/Library/Frameworks/JavaVM.framework"; then
      JAVA_HOME="/System/Library/Frameworks/JavaVM.framework"
    fi
  elif test -d "/usr/java"; then
    JAVA_HOME=/usr/java
  elif test -f "/System/Library/Frameworks/JavaVM.framework"; then
    JAVA_HOME="/System/Library/Frameworks/JavaVM.framework"
  fi
fi

echo $ac_n "checking for JAVA_HOME... $JAVA_HOME"

if test ! -d "$JAVA_HOME"; then
  AC_MSG_ERROR([Can't find valid JAVA_HOME ${JAVA_HOME}])
fi

AC_SUBST(JAVA_HOME)

JAVA_EXE="$JAVA_HOME/bin/java"

$JAVA_EXE -d64 -version 1>/dev/null 2>/dev/null

if test $? = "0"; then
  JAVA_EXE="$JAVA_EXE -d64"
fi

AC_SUBST(JAVA_EXE)

#
# libtool stuff
#  
AC_PROG_LIBTOOL			    
if test -z "${LTFLAGS}"; then
  LTFLAGS="--silent"
fi
	
LIBTOOL_SCRIPT="`pwd`/libtool"
LIBTOOL="${LIBTOOL_SCRIPT} ${LTFLAGS}"    
SH_LIBTOOL="${SHELL} ${LIBTOOL_SCRIPT} ${LTFLAGS}"
SHLTCFLAGS=""
LTCFLAGS=""

AC_SUBST(LTFLAGS)	
AC_SUBST(LIBTOOL_SCRIPT)	
AC_SUBST(LIBTOOL)	
AC_SUBST(SH_LIBTOOL)	
AC_SUBST(LTCFLAGS)	
AC_SUBST(EGREP)
     
INCLUDES=""
plugins=common
LIBEXEC=libexec
		
AC_SUBST(plugins)
AC_SUBST(resin_pro_plugins)
AC_SUBST(LIBEXEC)
dnl should be unnecessary
AC_SUBST(LD)

ACLOCAL=aclocal	
AUTOMAKE=automake
AC_SUBST(ACLOCAL)	
AC_SUBST(AUTOMAKE)	

#
# additional CFLAGS
#
# for solaris        
CFLAGS="$CFLAGS -D_POSIX_PTHREAD_SEMANTICS"                          

#
# --with-apache-src
#
AC_ARG_WITH(apache-src,
[  --with-apache-src=DIR  Apache src static compilation
], [
apache_src=${withval}

if test -d "${apache_src}/modules"; then
  a=b
elif test -d "${apache_src}/src/modules"; then
  apache_src=${apache_src}/src
else
  AC_MSG_ERROR([Can't find valid Apache source ${apache_src}])
fi

echo "using Apache source in ${apache_src}"

mkdir ${apache_src}/modules/caucho 2> /dev/null
cp modules/c/src/common/*.c ${apache_src}/modules/caucho
cp modules/c/src/common/*.h ${apache_src}/modules/caucho
cp modules/c/src/apache/*.c ${apache_src}/modules/caucho

cat > ${apache_src}/modules/caucho/Makefile.tmpl <<'END'
LIB=libcaucho.a
OBJS=mod_caucho.o stream.o config.o registry.o memory.o

OBJS_PIC=mod_caucho.lo stream.lo config.lo registry.lo memory.lo

$(OBJS) $(OBJS_PIC): Makefile

all: lib

lib: $(LIB)

libcaucho.a: $(OBJS)
	rm -f $@
	ar cr $@ $(OBJS)
	$(RANLIB) $@

libcaucho.o: $(OBJS)
	$(LD) $(LDFLAGS) -r -o $@ $(OBJS)

libcaucho.so: $(OBJS_PIC)
	rm -f $@
	$(LD_SHLIB) $(LDFLAGS_SHLIB) -o $@ $(OBJS_PIC) $(LIBS_SHLIB) 

.SUFFIXES: .o .lo

.c.o:
	$(CC) -c $(INCLUDES) $(CFLAGS) $<

.c.lo:
	$(CC) -c $(INCLUDES) $(CFLAGS) $(CFLAGS_SHLIB) $< && mv $*.o $*.lo

# DO NOT REMOVE
mod_caucho.o: cse.h
mod_caucho.lo: cse.h
stream.o: cse.h
stream.lo: cse.h
config.o: cse.h
config.lo: cse.h
registry.o: cse.h
registry.lo: cse.h
memory.o: cse.h
memory.lo: cse.h

END

cat > ${apache_src}/modules/caucho/Makefile.libdir <<'END'
# dummy
END

#grep caucho ${apache_src}/src/Configuration.tmpl >/dev/null 2>/dev/null
#if test "$?" -ne 0; then
#  cat >> ${apache_src}/src/Configuration.tmpl <<'END'
## mod_caucho incorporates Resin into Apache.
#
## AddModule modules/caucho/libcaucho.a
#END
#fi

])
#
# --with-apache
#
AC_ARG_WITH(apache,
[  --with-apache=DIR  the Apache root directory
], [
apache_dir=${withval}

if test "${apache_dir}" = "yes"; then
  apache_dir=/usr/local/apache
fi

if test ! -d "${apache_dir}"; then
  AC_MSG_ERROR([--with-apache=${apache_dir} is not a valid Apache directory])
fi   

apache=1
])

#
# --with-apxs
#
AC_ARG_WITH(apxs,
[  --with-apxs=PATH  the Apache configuration utility
], [
apxs=${withval}

if test "${apxs}" = "yes"; then
  apxs=apxs
fi

apache=1
])

#
# --with-apache-eapi
#
AC_ARG_WITH(apache-eapi,
[  --with-apache-eapi=DIR  Apache eapi support
], [
apache_eapi=${withval}
])

#
# --with-apache-include
#
AC_ARG_WITH(apache-include,
[  --with-apache-include=DIR  the Apache include directory
], [
apache_include=${withval}

if test "${apache_include}" = "yes"; then
    apache_include=
fi

apache=1
])

#
# --with-apache-libexec
#
AC_ARG_WITH(apache-libexec,
[  --with-apache-libexec=DIR  the Apache module directory
], [
apache_libexec=${withval}

if test "${apache_libexec}" = "yes"; then
    apache_libexec=
fi

apache=1
])

#
# --with-apache-conf
#
AC_ARG_WITH(apache-conf,
[  --with-apache-conf=DIR  the Apache configuration
], [
apache_conf=${withval}

if test "${apache_conf}" = "yes"; then
    apache_conf=
fi

apache=1
])

apache_cflags=

#
# --with-apache-cflags
#
AC_ARG_WITH(apache-cflags,
[  --with-apache-cflags=flags  flags for compiling mod_caucho
], [
apache_cflags=${withval}

apache=1
])

#
# check that apxs is okay
#
if test -n "${apxs}"; then
  ${apxs} -q PREFIX >/dev/null 2>/dev/null
  if test "$?" -ne 0; then
    AC_MSG_ERROR(bad apxs ${apxs})
  fi
elif test -x "${apache_bin}/apxs"; then
  apxs=${apache_bin}/apxs 
elif test -x "${apache_dir}/bin/apxs"; then
  apxs=${apache_dir}/bin/apxs 
elif test -x "${apache_dir}/sbin/apxs"; then
  apxs=${apache_dir}/sbin/apxs 
elif test -x "/sbin/apxs"; then
  apxs=/sbin/apxs 
elif test -x "/usr/sbin/apxs"; then
  apxs=/usr/sbin/apxs 
fi
   
#
# heuristics for finding the Apache include directory
#
if test -z "${apache_include}" -a -n "${apxs}"; then
  apache_include=`${apxs} -q INCLUDEDIR`
fi

if test -z "${apache_include}" -a -r "${apache_dir}/include/httpd.h"; then
  apache_include=${apache_dir}/include
fi

if test -n "${apache_include}"; then	
  APACHE_INC="-I${apache_include}"
fi
  
#
# deal with stronghold
#
if test -z "${apache_include}" -a -r "${apache_dir}/src/include/httpd.h"; then
  apache_include=${apache_dir}/src/include

  APACHE_INC="-I${apache_dir}/src/include"
  APACHE_INC="-I${apache_dir}/ssl/include $APACHE_INC"
  APACHE_INC="-I${apache_dir}/src/os/unix $APACHE_INC"
fi

if test -n "$apache" -a ! -r ${apache_include}/httpd.h; then
  AC_MSG_ERROR([Can't find Apache include directory ${apache_include}])
fi

#
# heuristics for finding the Apache bin directory
#
if test -n "${apache_bin}"; then
  a=b
elif test -x "${apxs}"; then
  apache_bin=`${apxs} -q SBINDIR`
  apache_exe="${apache_bin}/`${apxs} -q TARGET`"
elif test -n "${apache_dir}"; then
  apache_bin="${apache_dir}/bin"
fi

if test -n "${apache_exe}"; then
 a=b;
elif test -z "${apache_bin}"; then
 a=b;
elif test -x "${apache_bin}/httpd"; then
 apache_exe="${apache_bin}/httpd"   	
elif test -x "${apache_bin}/apache"; then
 apache_exe="${apache_bin}/apache"   	
elif test -x "${apache_bin}/httpd2"; then
 apache_exe="${apache_bin}/httpd2"
elif test -x "${apache_bin}/apache2"; then
 apache_exe="${apache_bin}/apache2"
fi

if test -n "${apache_exe}" -a ! -x "${apache_exe}"; then
  AC_MSG_ERROR([Can't find Apache binary in directory ${apache_exe}])
fi

#
# heuristics for finding the Apache module directory
#
if test -z "${apache_libexec}" -a -n "${apxs}"; then
  apache_libexec=`${apxs} -q LIBEXECDIR`
fi

if test -z "${apache_libexec}" -a -d "${apache_dir}/libexec"; then
  apache_libexec=${apache_dir}/libexec
fi

if test -z "${apache_libexec}" -a -d "${apache_dir}/modules"; then
  apache_libexec=${apache_dir}/modules
fi

if test -n "$apache" -a ! -d "${apache_libexec}"; then
  AC_MSG_ERROR([Can't find Apache module directory ${apache_libexec}])
fi

#
# Heuristics for finding the Apache configuration directory
#

if test -z "${apache_conf}" -a -n "${apxs}"; then
  apache_confdir=`${apxs} -q SYSCONFDIR`
  
  if test -r "${apache_confdir}"/httpd.conf; then
    apache_conf=${apache_confdir}/httpd.conf
  elif test -r "${apache_confdir}"/apache.conf; then
    apache_conf=${apache_confdir}/apache.conf
  elif test -r "${apache_confdir}"/httpsd.conf; then
    apache_conf=${apache_confdir}/httpsd.conf
  fi
fi

if test -z "${apache_conf}" -a -r "${apache_dir}/conf/httpd.conf"; then
  apache_conf=${apache_dir}/conf/httpd.conf
fi

if test -z "${apache_conf}" -a -r "${apache_dir}/etc/httpd.conf"; then
  apache_conf=${apache_dir}/etc/httpd.conf
fi

if test -n "$apache" -a ! -r "${apache_conf}"; then
  AC_MSG_ERROR([Can't find Apache module configuration ${apache_conf}])
fi

#
# check for apr
#
if test -x "${apxs}"; then
   apr_bin=`${apxs} -q APR_BINDIR 2> /dev/null`
   if test -x "${apr_bin}/apr-config"; then
     apr_inc=`"${apr_bin}/apr-config" --includes`
     APACHE_INC="${APACHE_INC} ${apr_inc}"
   elif test -x "${apr_bin}/apr-1-config"; then
     apr_inc=`"${apr_bin}/apr-1-config" --includes`
     APACHE_INC="${APACHE_INC} ${apr_inc}"
   fi
fi
	
#
# Apache CFLAGS
#

if test -z "$apache_cflags" -a -x "${apxs}"; then
   if test -x "${apr_bin}/apr-config"; then
     apache_cflags="$apache_cflags `${apr_bin}/apr-config --cppflags`"
     apache_cflags="$apache_cflags `${apr_bin}/apr-config --cflags`"
   elif test -x "${apr_bin}/apr-1-config"; then
     apache_cflags="$apache_cflags `${apr_bin}/apr-1-config --cppflags`"
     apache_cflags="$apache_cflags `${apr_bin}/apr-1-config --cflags`"
   else
     apache_cflags="`${apxs} -q CFLAGS`"
   fi
 fi
	
if test -z "$apache_cflags" -a -x "${apache_exe}"; then
   flags=`${apache_exe} -V | grep EAPI`
   if test -n "$flags"; then
     apache_cflags=-DEAPI
   fi
fi

if test -z "$apache_dir" -a -x "${apxs}"; then
   apache_dir=`${apxs} -q PREFIX`
fi

AC_SUBST(APACHE_INC)
AC_SUBST(apache_libexec)
AC_SUBST(apache_conf)
AC_SUBST(apache_dir)
AC_SUBST(apache_cflags)

#
# fill in compilation stuff
#
SO=so

is_gcc=`${CC} -v 2>&1 | grep gcc`
is_gnu_ld=`${LD} -v 2>&1 | grep GNU`

#
# Use apxs if it's available
#
if test -z "${LD_SHLIB}"; then
  if test -n "${apxs}"; then
    CFLAGS_SHLIB=`${apxs} -q CFLAGS_SHLIB`
    LD_SHLIB=`${apxs} -q LD_SHLIB`
    LDFLAGS_SHLIB=`${apxs} -q LDFLAGS_SHLIB`
    LIBS_SHLIB=`${apxs} -q LIBS_SHLIB`

    echo "Using shared library flags from ${apxs}"
  fi
fi

#
# Java 1.6 check
#

echo $ECHO_N "checking if ${JAVA_EXE} is Java 1.6... "

tmpname="/tmp/java$$.out"
	       	
$JAVA_EXE -version 2> $tmpname

grep "1\.6" $tmpname 1> /dev/null
if test "$?" != "0"; then
  echo "no"
  java_version=`cat $tmpname`
  AC_MSG_ERROR([Java 1.6 required. ${JAVA_EXE} returned: ${java_version}])
fi

echo "yes"

#
# 64bit heuristics
#
echo $ECHO_N "checking if Java is 64-bit... "

tmpname="/tmp/java$$.out"
	       	
#$JAVA_HOME/bin/java -version 2> $tmpname

$JAVA_EXE -version 2> $tmpname

grep "64-Bit" $tmpname 1> /dev/null
if test "$?" = "0"; then
   b64_jni=true
fi

grep "amd64-64" $tmpname 1> /dev/null
if test "$?" = "0"; then
   b64_jni=true
fi

grep "x86_64" $tmpname 1> /dev/null
if test "$?" = "0"; then
   b64_jni=true
fi

grep "s390x-64" $tmpname 1> /dev/null
if test "$?" = "0"; then
   b64_jni=true
fi
               	
$JAVA_HOME/bin/sparcv9/java -version 2> $tmpname
grep "64-Bit" $tmpname 1> /dev/null
     
if test "$?" = "0"; then
   b64_jni=true
fi

# Require user to use --enable-64bit if the version is optional
# in the javac  
#  
#$JAVA_HOME/bin/java -d64 -version 2> $tmpname
#grep "64-Bit" $tmpname 1> /dev/null
    
#if test "$?" = "0"; then
#   b64_jni=true
#fi

if test -n "$b64_jni"; then	
   echo "yes"
else   
   echo "no"
fi

rm -f $tmpname	

#
# --enable-64bit
#
AC_ARG_ENABLE(64bit,
[  --enable-64bit    Enable 64 bit],
[case "${enableval}" in
 yes) b64=true; b64_jni=true ;;
 no)  b64=""; b64_jni="" ;;
 *) AC_MSG_ERROR(bad value ${enableval} for --enable-64bit) ;;
 esac],[])

#
# --enable-64bit-jni
#
AC_ARG_ENABLE(64bit-jni,
[  --enable-64bit-jni    Enable 64 bit JNI only (not Apache)],
[case "${enableval}" in
 yes) b64_jni=true ;;
 no)  b64_jni="" ;;
 *) AC_MSG_ERROR(bad value ${enableval} for --enable-64bit-jni) ;;
 esac],[])

#
# --enable-64bit-plugin
#
AC_ARG_ENABLE(64bit-plugin,
[  --enable-64bit-plugin    Enable 64 bit plugin (Apache) (not JNI)],
[case "${enableval}" in
 yes) b64=true ;;
 no)  b64="" ;;
 *) AC_MSG_ERROR(bad value ${enableval} for --enable-64bit-plugin) ;;
 esac],[])

if test -n "${b64}"; then
   b64_jni=true
fi   

if test -n "${b64_jni}"; then        
   CFLAGS="$CFLAGS -DB64"
   LIBEXEC=libexec64
fi

#
# --enable-lfs
#
AC_ARG_ENABLE(lfs,
[  --enable-lfs            Enable large file source support],
[case "${enableval}" in
 yes) lfs=true ;;
 no)  lfs="";;
 *) AC_MSG_ERROR(bad value ${enableval} for --enable-lfs) ;;
 esac],[])

if test -n "${lfs}"; then
  CFLAGS_JNI="$CFLAGS_JNI -D_LARGEFILE_SOURCE -D_LARGEFILE64_SOURCE -D_FILE_OFFSET_BITS=64"
fi

#
# fill in based on known configurations
#
if test -z "${LD_SHLIB}"; then
  LD_SHLIB=${LD}	
  CFLAGS_SHLIB="-fpic"
  LDFLAGS_SHLIB="-shared"

  case "$target_os" in
    linux*)
      LD_SHLIB=${CC}

      if test -n "$b64"; then
        CFLAGS="$CFLAGS -m64 -fPIC"
        LDFLAGS_SHLIB="-shared -fPIC"
      fi
      if test -n "$b64_jni"; then
        CFLAGS_JNI="$CFLAGS_JNI -m64 -fPIC"
        LDFLAGS_SHLIB="-shared -fPIC"
      fi
      ;;

    *solaris*)
      if test -n "$is_gcc"; then
        CFLAGS_SHLIB="-fPIC"

        if test -n "$b64"; then
          CFLAGS="$CFLAGS -m64 -G -fPIC"
          LDFLAGS_SHLIB="-m64"
        fi
        if test -n "$b64_jni"; then
          CFLAGS_JNI="$CFLAGS_JNI -m64 -G -fPIC"
          LDFLAGS_SHLIB="-m64"
        fi
      else
        CFLAGS_SHLIB="-KPIC"

        if test -n "$b64"; then
          CFLAGS="$CFLAGS -xarch=v9"
        fi
        if test -n "$b64_jni"; then
          CFLAGS_JNI="$CFLAGS_JNI -xarch=v9"
        fi
      fi

      if test -n "$is_gnu_ld"; then
        LDFLAGS_SHLIB="$LDFLAGS_SHLIB -shared"
      else
        LDFLAGS_SHLIB="$LDFLAGS_SHLIB -shared -fPIC"
      fi
      ;;

    darwin*)
      CFLAGS_SHLIB="-DSHARED_MODULE"
      LD_SHLIB=${CC}
      LDFLAGS_SHLIB="-dynamiclib -undefined suppress -flat_namespace"
      if test -n "$b64"; then
        CFLAGS="$CFLAGS -arch x86_64"
        LDFLAGS_SHLIB="-arch x86_64 $LDFLAGS_SHLIB"
      fi
      if test -n "$b64_jni"; then
        CFLAGS_JNI="$CFLAGS_JNI -m64"
        LDFLAGS_SHLIB="-arch x86_64 $LDFLAGS_SHLIB"
      fi
      ;;

    *freebsd*)
      CFLAGS="$CFLAGS -fPIC"
      ;;

    *aix*)
      if test -z "$is_gcc"; then
        CFLAGS_SHLIB="-bexpall"
      else
        CFLAGS_SHLIB="-fpic"
      fi
      LDFLAGS_SHLIB=""
      ;;

    *hpux*)
      if test -z "$is_gcc"; then
        CFLAGS_SHLIB="+z"
      else
        CFLAGS_SHLIB="-fpic"
      fi
      LDFLAGS_SHLIB="-b"
      ;;
  esac

  case "$target_cpu" in
    x86_64)
           if test -z "$b64_jni"; then
  	     CFLAGS_JNI="$CFLAGS_JNI -m32"
  	     LDFLAGS_SHLIB="$LDFLAGS_SHLIB -melf_i386"
   	   fi
	;;
  esac	
fi

case "$target_os" in
  linux*)
        CFLAGS="$CFLAGS -DHAS_SOCK_TIMEOUT"
	;;
  darwin*)
        CFLAGS="$CFLAGS -DHAS_SOCK_TIMEOUT"
	;;
esac        

if test -z "$apache"; then
  a=b
elif test -r "${apache_include}/apr_thread_mutex.h" -o -n "${apr_inc}"; then
  echo "Using Apache 2.0 configuration ${apache_conf}"

  plugins="$plugins apache2"
else
  AC_MSG_ERROR([Can't find valid Apache directory ${apache_inc}])
fi

if test -n "$apache_cflags"; then
  echo "Using Apache CFLAGS: $apache_cflags"
fi

#
# Using Perl if it's available.
#
PERL=`which perl`
if test -z "${LD_SHLIB}"; then
  if test -z "${LD_SHLIB}" -a "x`$PERL -V:dlsrc 2>/dev/null | grep dlopen`" != "x"; then
    PCC="`$PERL -V:cc | cut -d\' -f2`"
    if test "${PCC}" = "${CC}"; then
      CFLAGS_SHLIB="`$PERL -V:cccdlflags | cut -d\' -f2`"
      LDFLAGS_SHLIB="`$PERL -V:lddlflags | cut -d\' -f2`"
      LD_SHLIB="`$PERL -V:ld | cut -d\' -f2`"
 
      has_clflags=1

      echo "Using shared library flags from Perl"
    fi
  fi
fi

#
# Check if cp accepts -u
#
CP=`which cp`
echo $ECHO_N "checking if $CP accepts -u... "

touch /tmp/cp.test.$$.src
$CP -u /tmp/cp.test.$$.src /tmp/cp.test.$$.dest 1>/dev/null 2>/dev/null
if test $? = "0"; then
  CP_U_FLAG=-u
  AC_SUBST(CP_U_FLAG)
  echo "yes"
else
  echo "no"
fi
rm -f /tmp/cp.test.$$.src /tmp/cp.test.$$.dest

#if test -z "${LD_SHLIB}"; then
#  AC_MSG_ERROR([Can't determine compilation flags.]);
#fi

#
# OS which can handle the Resin launcher
#
resin_pro_plugin=
if test -r "${resin_pro}/modules/c/src/resin/jni_vfs.c"; then
   resin_pro_plugin="resin"
fi
	
resin_ssl_plugin=
if test -r "${resin_pro}/modules/c/src/resinssl/ssl.c"; then
   resin_ssl_plugin="resinssl"
fi

resin_pro_plugins=

case "$target_cpu" in
  i?86)
	CPU=i386
	resin_pro_plugins="$resin_pro_plugin $resin_ssl_plugin"
	;;

  sparc*)
	if test -n "${b64_jni}"; then
  	  CPU=sparcv9
	else
  	  CPU=sparc
	fi
	resin_pro_plugins="$resin_pro_plugin $resin_ssl_plugin"
	;;

  x86_64)
	CPU=$target_cpu
	resin_pro_plugins="$resin_pro_plugin $resin_ssl_plugin"
	;;
	
  *)
	CPU=$target_cpu
	;;
esac

case "$target_os" in
  linux*)
	jni_os=linux
        PROXY_LIBS="-lpthread -lc"
	;;

  *solaris*)
	jni_os=solaris
	CFLAGS="$CFLAGS -D__SOLARIS__"
	PROXY_LIBS='-lnsl -lsocket -lthread'
	;;

  *freebsd*)
	PROXY_LIBS='-lpthread'
        jni_os=freebsd        
        
	tmp=`ldd $JAVA_HOME/bin/java | sed 's/.*=>//' | grep -c libc_r`
	tmp=$tmp `ldd $JAVA_HOME/bin/java | sed 's/.*=>//' | grep -c libpthread`
	tmp=$tmp `ldd $JAVA_HOME/bin/java | sed 's/.*=>//' | grep -c libthr`

	case "$tmp" in
	100)
		PROXY_LIBS='-lc_r';
		;;
	010)
		PROXY_LIBS='-lpthread';
		;;
	001)
		PROXY_LIBS='-lthr';
		;;
	*)
		PROXY_LIBS='-lpthread'
		echo "Using -lpthread as default"
		;;
	esac	
	;;

  *darwin*)
	LD=gcc
	SO=jnilib
	resin_pro_plugins="$resin_pro_plugin $resin_ssl_plugin"
	;;

  *hpux*)
	if test -r "$JAVA_HOME/include/hpux/jni_md.h"; then
	  jni_os=hpux
        else
	  jni_os=hp-ux
        fi
	;;

  *aix*)
        jni_os=aix
	;;
esac

AC_SUBST(CPU)
AC_SUBST(OS)
AC_SUBST(SO)
AC_SUBST(CFLAGS)
AC_SUBST(CFLAGS_SHLIB)
AC_SUBST(LD_SHLIB)
AC_SUBST(LDFLAGS_SHLIB)
AC_SUBST(LIBS_SHLIB)
AC_SUBST(PROXY_LIBS)
AC_SUBST(SSL_LIBS)

#
# --with-jni-include
#
AC_ARG_WITH(jni-include,
[  --with-jni-include="-Idir -Idir"  jni include string
], [
JNI_INCLUDE=${withval}
JNI="yes"
])

#
# --enable-jni
#
AC_ARG_ENABLE(jni,
[  --enable-jni    Turn on jni],
[case "${enableval}" in
 yes) JNI=true ;;
 no)  JNI=false ;; 
 *) AC_MSG_ERROR(bad value ${enableval} for --enable-jni) ;;
 esac],[JNI=""])

JNI_INCLUDE_DIR=""

if test "$JNI" != "false" ; then
  if test -n "$JNI_INCLUDE"; then
    JNI=yes
    for x in $JNI_INCLUDE; do
      y=${x:2}
      if test -n "$JNI_INCLUDE_DIR"; then
        JNI_INCLUDE_DIR="$JNI_INCLUDE_DIR $y"
      else
        JNI_INCLUDE_DIR="$y"
      fi
    done;
  elif test -r "$JAVA_HOME/include/jni_md.h"; then
    JNI_INCLUDE_DIR="${JAVA_HOME}/include"
    JNI_INCLUDE="-I${JNI_INCLUDE_DIR}"
    JNI=yes
    echo "checking for JNI in $JAVA_HOME/include ... found"
  elif test -r "$JAVA_HOME/include/$jni_os/jni_md.h"; then
    JNI_INCLUDE_DIR="${JAVA_HOME}/include"
    JNI_INCLUDE="-I$JAVA_HOME/include -I$JAVA_HOME/include/$jni_os"
    JNI=yes
    echo "checking for JNI in $JAVA_HOME/include/$jni_os ... found"
  elif test -r "/System/Library/Frameworks/JavaVM.framework/Headers/jni_md.h"; then
    # Darwin
    echo "checking for JNI in /System/Library/Frameworks/JavaVM.framework/Headers ... found"
    JNI_INCLUDE_DIR="/System/Library/Frameworks/JavaVM.framework/Headers"
    JNI_INCLUDE="-I${JNI_INCLUDE_DIR}"
    JNI=yes
  elif test -r "$JAVA_HOME/../Headers/jni_md.h"; then
    # Darwin
    echo "checking for JNI in $JAVA_HOME/../Headers ... found"
    JNI_INCLUDE_DIR="$JAVA_HOME/../Headers"
    JNI_INCLUDE="-I${JNI_INCLUDE_DIR}"
    JNI=yes
  elif test -r "$JAVA_HOME/include/jni.h"; then
    JNI_INCLUDE_DIR="$JAVA_HOME/include"
    JNI_INCLUDE="-I${JNI_INCLUDE_DIR}"
    JNI=yes
    echo "checking for JNI in $JAVA_HOME/include ... found"
  else
    JNI=""
    AC_MSG_ERROR([

    *** Can't find JNI directory in JAVA_HOME=${JAVA_HOME}
    *** JNI is expected in ${JAVA_HOME}/include/$jni_os
  ])
  fi

  HAS_JVMTI=""

  for x in $JNI_INCLUDE_DIR; do
    if test -r "$x/jvmti.h"; then
      HAS_JVMTI="yes"
    fi
  done;

  if test -n "$HAS_JVMTI"; then
    echo "Using JVMTI for class reloading"
    
    CFLAGS="$CFLAGS -DHAS_JVMTI"
  else
    AC_MSG_ERROR([Can't find JVMTI directory ${JNI_INCLUDE_DIR}/jvmti.h])
  fi
fi

if test "$JNI" = "false" ; then
  resin_pro_plugins=""
  echo "JNI disabled by --disable-jni"
elif test -n "$JNI"; then
  plugins="$plugins resin_os"
else
  resin_pro_plugins=""
  echo "Can't find JNI include files in JAVA_HOME: $JAVA_HOME"
fi

AC_SUBST(JNI)
AC_SUBST(JNI_INCLUDE)
	
#
# --enable-ssl
#
AC_ARG_ENABLE(ssl,
[  --enable-ssl    Turn on ssl],
[case "${enableval}" in
 yes) ENABLE_SSL=true ;;
 no)  ENABLE_SSL="false" ;;
 *) AC_MSG_ERROR(bad value ${enableval} for --enable-ssl) ;;
 esac],[ENABLE_SSL=""])

#
# --with-openssl
#
AC_ARG_WITH(openssl,
[  --with-openssl=DIR  Openssl directory
], [
OPENSSL=${withval}
OPENSSL_INCLUDE=${OPENSSL}/include	
OPENSSL_LIB=${OPENSSL}/lib	
])

#
# --with-openssl-include
#
AC_ARG_WITH(openssl-include,
[  --with-openssl-include=DIR  Openssl include directory
], [
OPENSSL_INCLUDE=${withval}

if test ! -r "${OPENSSL_INCLUDE}/openssl/ssl23.h"; then
  AC_MSG_ERROR([Can't find valid OpenSSL include ${OPENSSL_INCLUDE}])
fi
])

#
# --with-openssl-lib
#
AC_ARG_WITH(openssl-lib,
[  --with-openssl-lib=DIR  Openssl lib directory
], [
OPENSSL_LIB=${withval}

if test ! -r "${OPENSSL_LIB}/libssl.a" -a \
        ! -r "${OPENSSL_LIB}/libssl.dylib" -a \
	! -r "${OPENSSL_LIB}/libssl.so"; then
  AC_MSG_ERROR([Can't find valid OpenSSL library ${OPENSSL_LIB}/libssl.*])
fi
])

if test "${ENABLE_SSL}" = false; then	
  foo=bar
elif test -n "${OPENSSL_INCLUDE}"; then
  foo=bar
elif test -r ${OPENSSL}/include/openssl/ssl23.h; then
  OPENSSL_INCLUDE=${OPENSSL}/include
elif test -r /usr/include/openssl/ssl23.h; then
  OPENSSL_INCLUDE=/usr/include
elif test -r /usr/local/include/openssl/ssl23.h; then
  OPENSSL_INCLUDE=/usr/local/include
elif test -r /usr/local/ssl/include/openssl/ssl23.h; then
  OPENSSL_INCLUDE=/usr/local/ssl/include
fi

if test "${ENABLE_SSL}" = false; then	
  foo=bar
  
elif test -n "${OPENSSL_LIB}"; then
  foo=bar
  
elif test -n "${b64_jni}" -a -r /lib64/libcrypto.so; then
  OPENSSL_LIB=/lib64
elif test -n "${b64_jni}" -a -r /usr/lib64/libcrypto.so; then
  OPENSSL_LIB=/usr/lib64
  
elif test -r /lib/libcrypto.so; then
  OPENSSL_LIB=/lib
elif test -r /lib/libcrypto.dylib; then
  OPENSSL_LIB=/lib
  
elif test -r /usr/lib/libcrypto.so; then
  OPENSSL_LIB=/usr/lib
elif test -r /usr/lib/libcrypto.dylib; then
  OPENSSL_LIB=/usr/lib
  
elif test -r /usr/local/lib/libcrypto.so; then
  OPENSSL_LIB=/usr/local/lib
elif test -r /usr/lib/libcrypto.dylib; then
  OPENSSL_LIB=/usr/local/lib
  
elif test -r /usr/local/ssl/lib/libcrypto.so; then
  OPENSSL_LIB=/usr/local/ssl/lib
elif test -r /usr/local/ssl/lib/libcrypto.dylib; then
  OPENSSL_LIB=/usr/local/ssl/lib
elif test -r ${OPENSSL}/lib/libcrypto.a; then
  OPENSSL_LIB=${OPENSSL}/lib
elif test -r /usr/local/ssl/lib/libcrypto.a; then
  OPENSSL_LIB=/usr/local/ssl/lib
elif test -r /usr/local/lib/libssl.a; then
  OPENSSL_LIB=/usr/local/lib
elif test -r /usr/lib/libssl.a; then
  OPENSSL_LIB=/usr/lib
fi

if test -z "${OPENSSL_LIB}"; then	
  foo=bar
elif test -r "${OPENSSL_LIB}/libssl.so"; then	
  SSL_LIBS="${SSL_LIBS} -lssl"
elif test -r "${OPENSSL_LIB}/libssl.dylib"; then	
  SSL_LIBS="${SSL_LIBS} -lssl"
elif test -r "${OPENSSL_LIB}/libssl3.so"; then	
  SSL_LIBS="${SSL_LIBS} -lssl3"
elif test -r "${OPENSSL_LIB}/libssl3.dylib"; then	
  SSL_LIBS="${SSL_LIBS} -lssl3"
elif test -r "${OPENSSL_LIB}/libssl.a"; then	
  SSL_LIBS="${SSL_LIBS} ${OPENSSL_LIB}/libssl.a"
elif test -r "${OPENSSL_LIB}/libssl3.a"; then	
  SSL_LIBS="${SSL_LIBS} ${OPENSSL_LIB}/libssl3.a"
else
  # probably an error
  SSL_LIBS="${SSL_LIBS} -lssl"
fi

if test -z "${OPENSSL_LIB}"; then	
  foo=bar
elif test -r "${OPENSSL_LIB}/libcrypto.so"; then	
  SSL_LIBS="${SSL_LIBS} -lcrypto"
elif test -r "${OPENSSL_LIB}/libcrypto.dylib"; then	
  SSL_LIBS="${SSL_LIBS} -lcrypto"
elif test -r "${OPENSSL_LIB}/libcrypto.a"; then	
  SSL_LIBS="${SSL_LIBS} ${OPENSSL_LIB}/libcrypto.a"
else
  # probably an error
  SSL_LIBS="${SSL_LIBS} -lcrypto"
fi

SSL_OBJ=ssl_stub.o

if test "${ENABLE_SSL}" = false; then	
  echo "Openssl is not enabled"
elif test -z "${OPENSSL_LIB}"; then
  echo "Openssl library was not found"
elif test ! -r "${OPENSSL_LIB}/libcrypto.so" -a \
          ! -r "${OPENSSL_LIB}/libcrypto.dylib" -a \
          ! -r "${OPENSSL_LIB}/libcrypto.a" ; then
  AC_MSG_ERROR([Can't find valid OpenSSL library in ${OPENSSL_LIB}])
elif test ! -r "${OPENSSL_INCLUDE}/openssl/ssl23.h"; then
  AC_MSG_ERROR([Can't find valid OpenSSL include in ${OPENSSL_INCLUDE}])
else
  echo "Using openssl include in ... ${OPENSSL_INCLUDE}"
  echo "Using openssl lib in ... ${OPENSSL_LIB}"
  echo "Using openssl libraries ... ${SSL_LIBS}"

  cat <<END
END

  if test -x "${apache_dir}/bin/httpd"; then
    apache_ssl=`${apache_dir}/bin/httpd -l 2>&1 | grep ssl`
    if test -n "$apache_ssl"; then
      CFLAGS="$CFLAGS -DOPENSSL"
      echo "Enabling ssl for mod_caucho" 
    fi
  elif test -x "${apache_dir}/bin/httpd2"; then
    apache_ssl=`${apache_dir}/bin/httpd2 -l 2>&1 | grep ssl`
    if test -n "$apache_ssl"; then
      CFLAGS="$CFLAGS -DOPENSSL"
      echo "Enabling ssl for mod_caucho" 
    fi
  fi

  if test -r "${OPENSSL_INCLUDE}/openssl/engine.h"; then
    CFLAGS_OPENSSL="$OPENSSL_CFLAGS -DSSL_ENGINE"
  fi

  if test -n "${OPENSSL_INCLUDE}"; then
    INCLUDES="$INCLUDES -I${OPENSSL_INCLUDE}"
  fi
  SSL_OBJ=ssl.o
  LDFLAGS_SHLIB="${LDFLAGS_SHLIB} -L${OPENSSL_LIB}"
  # PROXY_LIBS="$PROXY_LIBS ${OPENSSL_LIB}/libssl.a ${OPENSSL_LIB}/libcrypto.a"
fi

if test -d /usr/kerberos/include; then
   INCLUDES="$INCLUDES -I/usr/kerberos/include"
fi

#
# test the open ssl
#

if test -n "${OPENSSL_LIB}"; then	
cat >conftest.$ac_ext <<_ACEOF
#include <openssl/ssl.h>
#include <openssl/rsa.h>       
#include <openssl/err.h>

int main(int argc, char **argv)
{
  SSLeay_add_ssl_algorithms();
  SSL_load_error_strings();
  
#if OPENSSL_VERSION_NUMBER >= 0x00907000L
    if (0)
      ERR_print_errors_cb(0, 0);
#endif

  printf("%s\n", OPENSSL_VERSION_TEXT);

  return 0;
}
_ACEOF

${CC} -o conftest ${CFLAGS_OPENSSL} ${CFLAGS} ${INCLUDES} conftest.$ac_ext -L${OPENSSL_LIB} ${SSL_LIBS} ${PROXY_LIBS} 1>/dev/null 2>/dev/null

if test "$?" != 0; then
  ${CC} -o conftest ${CFLAGS_OPENSSL} ${CFLAGS} ${INCLUDES} conftest.$ac_ext -L${OPENSSL_LIB} ${SSL_LIBS} ${PROXY_LIBS}

  AC_MSG_WARN(Can't compile SSL.  Check compilation flags: ${CC} ${CFLAGS_OPENSSL} ${CFLAGS} ${INCLUDES} -L${OPENSSL_LIB} ${SSL_LIBS} ${PROXY_LIBS})
  
  resin_ssl_plugin=""
else
  openssl_version=`./conftest`

  if test "$?" != 0; then
    AC_MSG_WARN(Can't execute SSL.  Check that load version in the library path: ${LD_LIBRARY_PATH} matches the expected version.)
  fi
fi  
	
rm -f conftest conftest.$ac_ext
fi   

AC_SUBST(INCLUDES)
AC_SUBST(CFLAGS_OPENSSL)
AC_SUBST(CFLAGS_JNI)
AC_SUBST(SSL_OBJ)

PERL="perl"
#
# --with-perl
#
AC_ARG_WITH(perl,
[  --with-perl=DIR  Perl binary location
], [
PERL=${withval}
])

AC_SUBST(PERL)

#
# --enable-debug
#
AC_ARG_ENABLE(debug,
[  --enable-debug    Turn on debugging],
[case "${enableval}" in
 yes) debug=true ;;
 no)  debug=false ;;
 *) AC_MSG_ERROR(bad value ${enableval} for --enable-debug) ;;
 esac],[debug=false])

if test "$debug" = true; then
  CFLAGS="$CFLAGS -DDEBUG -Wall"
  HARDCORE_CFLAGS="$HARDCORE_CFLAGS -DDEBUG"
fi

#
# final cleanup
#

if test -z "${is_dev}"; then
  plugins="$plugins $resin_pro_plugins"
  resin_pro_plugins="$plugins"
fi

AC_OUTPUT([
Makefile
modules/c/src/Makefile
modules/c/src/common/Makefile
modules/c/src/apache2/Makefile
modules/c/src/resin_os/Makefile
init.d/resin
], [])

if test -n "${resin_pro_plugins}"; then
  AC_OUTPUT([
    ${resin_pro}/modules/c/src/Makefile
    ${resin_pro}/modules/c/src/resin/Makefile
    ${resin_pro}/modules/c/src/resinssl/Makefile
  ])
fi

echo ""
echo ""
echo "Resin Configuration summary:"
echo ""
echo "  RESIN_HOME: ${resin_home}"
echo "      root: ${resin_root}"
echo "      conf: ${resin_conf}"
echo "      log : ${resin_log}"

if test -n "resin_init_d"; then
  echo "      init: ${resin_init_d}"
fi

if test "$JNI" = "false" ; then
  echo 
  echo "  *** JNI disabled by --disable-jni"
elif test -z "$JNI"; then
  echo "  *** No JNI has been found in JAVA_HOME=${JAVA_HOME}"
  echo "  *** JNI libraries cannot be compiled"
else  
  echo "  JAVA_HOME: ${JAVA_HOME}"
  echo ""

  if test -n "$b64_jni"; then
    echo "    + 64-bit JNI in ${JNI_INCLUDE}"
  else  
    echo "    + 32-bit JNI in ${JNI_INCLUDE}"
  fi

  echo "    + JNI CFLAGS: ${CFLAGS}"

  if test -n "$epoll"; then
    echo "    + epoll() for keepalives"
  elif test -n "$poll"; then
    echo "    + poll() for keepalives"
  else
    echo "    + select() for keepalives"
  fi

  if test -n "$OPENSSL_INCLUDE"; then
    echo "    + ${openssl_version}"
    echo "        OpenSSL include: ${OPENSSL_INCLUDE}"
    echo "        OpenSSL lib: ${OPENSSL_LIB}"
    echo "        OpenSSL libraries: ${SSL_LIBS}"
  else
    echo "    + not using OpenSSL"
  fi
fi

if test -n "${apache}"; then
  echo ""
  echo "  Apache: ${apache_dir}"
  echo "      conf: ${apache_conf}"
  echo "      libexec: ${apache_libexec}"
  echo "      include: ${apache_include}"
  echo "      CFLAGS: ${apache_cflags}"
fi

echo ""
