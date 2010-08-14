#
# Spec File for package resin-pro
#
# Copyright 2010 Caucho Technologies Inc.
#
%include Solaris.inc

Name:               resin-pro
IPS_Package_Name:   caucho-%{name}-%{version}

Summary:        Caucho Resin 4 Professional JavaEE Application Server
Version:        4.0.8
License:        GPLv2, Caucho Developer Source License v1.1
Url:            http://caucho.com/resin-4.0/
Source:         http://www.caucho.com/download/%{name}-%{version}.tar.gz
Group:          Internet
Distribution:   OpenSolaris
Vendor:         Caucho Technologies Inc.
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
SUNW_Basedir:   %{_basedir}
SUNW_Copyright: %{name}.copyright



# OpenSolaris IPS Manifest Fields
Meta(info.upstream):            Dominik Dorn
Meta(info.maintainer):          Dominik Dorn
Meta(info.repository_url):      svn://svn.caucho.com/resin/trunk/

%description
Resin 4 is Caucho Technologies contribution to the JavaEE6 Application Server
market. It implements the JavaEE6 Web-Profile, provides support for the
PHP Scripting language through its integrated Quercus(R) PHP runtime
environment and supports clustering and easy & efficient binary remoting
through Hessian.


#Package dependencies
%include default-depend.inc

Requires:   SUNWj6dev
Requires:   SUNWopenssl
BuildRequires:   SUNWgcc
BuildRequires:   SUNWbinutils
#Requires:   SUNWant


#defines
%define manifestdir /var/svc/manifest/network
%define startstopdir /lib/svc/method




# Preperation
%prep
%setup -q -n %{name}-%{version}
%patch -p1


# Build
%build
#export CFLAGS="%optflags"
#export LDFLAGS="%{_ldflags}"
./configure --prefix=%{_prefix}                 \
            --enable-jni                        \
            --enable-ssl                        \
            --with-resin-root=%{_prefix}/var/resin        \
            --with-resin-conf=%{_prefix}/etc/resin        \
            --with-resin-log=%{_prefix}/var/log/resin
make


# Installation
%install
# Any previous builds are removed.
rm -rf $RPM_BUILD_ROOT

# Package is built and installed in the buildroot with make install.
DESTDIR=$RPM_BUILD_ROOT/%{_basedir} make install


mkdir -p ${RPM_BUILD_ROOT}%{manifestdir}



# The files that don't need to be installed are removed from the buildroot.
rm -f $RPM_BUILD_ROOT%{_infodir}/dir



%post

if [ -f /lib/svc/share/smf_include.sh ] ; then
    . /lib/svc/share/smf_include.sh
    smf_present
    if [ $? -eq 0 ]; then
	/usr/sbin/svccfg import /var/svc/manifest/network/http-resin4.xml
    fi
fi

exit 0

%preun
# before uninstalling:
# disable resin4 through SMF, if it is running
if [  -f /lib/svc/share/smf_include.sh ] ; then
    . /lib/svc/share/smf_include.sh
    smf_present
    if [ $? -eq 0 ]; then
	if [ `svcs  -H -o STATE svc:/network/http:resin4` != "disabled" ]; then
	    svcadm disable svc:/network/http:resin4
	fi
    fi
fi


%postun
# After uninstalling:
# remove Resins smf-manifest if its still there
if [ -f /lib/svc/share/smf_include.sh ] ; then
    . /lib/svc/share/smf_include.sh
    smf_present
    if [ $? -eq 0 ] ; then
        /usr/sbin/svccfg export svc:/network/http > /dev/null 2>&1
        if [ $? -eq 0 ] ; then
            /usr/sbin/svccfg delete -f svc:/network/http:resin4
        fi
    fi
fi

exit 0

                                                



# cleanup
%clean
# rm -rf $RPM_BUILD_ROOT

# Package contents
%files
%defattr (0755, root, bin)
%attr(2750, root, bin) /lib/svc/method/http-resin4
%attr(2550, root, bin) /var/svc/manifest/network/http-resin4.xml
%dir %attr (0755, root, bin) %{_bindir}
%{_bindir}/*
%{_infodir}/*
%dir %attr(0755, root, sys) %{_datadir}
%dir %attr(0755, root, bin) %{_mandir}
%dir %attr(0755, root, bin) %{_mandir}/*
%config /etc/resin/resin.xml
%config /etc/resin/app-default.xml

%{_mandir}/*/*



# List of changes
%changelog
* Fri Jul 23 2010 - Dominik Dorn <dorn@caucho.com>
- Initial creation of Solaris IPS Package
