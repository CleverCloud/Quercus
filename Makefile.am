ACLOCAL_AMFLAGS=-I m4
resin_home=@resin_home@
resin_conf=@resin_conf@
resin_root=@resin_root@
resin_init_d=@resin_init_d@
resin_pro=@resin_pro@
LIBEXEC=@LIBEXEC@

all	: 
	(cd modules/c/src; $(MAKE))
	if test -r $(resin_pro)/modules/c/src/Makefile; then \
	  cd $(resin_pro)/modules/c/src; $(MAKE); \
	fi

clean	:
	(cd modules/c/src; $(MAKE) clean)
	if test -r $(resin_pro)/modules/c/src/Makefile; then \
	  cd $(resin_pro)/modules/c/src; $(MAKE) clean; \
	fi
	- rm -r $(LIBEXEC)

install	:
	if test "$(resin_home)" != $(abs_builddir); then \
	  mkdir -p $(resin_home)/bin; \
	  mkdir -p $(resin_home)/keys; \
	  mkdir -p $(resin_home)/licenses; \
	  mkdir -p $(resin_home)/lib; \
	  mkdir -p $(resin_home)/$(LIBEXEC); \
	  cp bin/* $(resin_home)/bin; \
	  cp lib/*.jar $(resin_home)/lib; \
	fi
	if test "$(resin_root)" != $(abs_builddir); then \
	  mkdir -p $(resin_root)/webapps; \
	  cp -r $(CP_U_FLAG) webapps/* $(resin_root)/webapps; \
	  mkdir -p $(resin_root)/doc; \
	  cp -r $(CP_U_FLAG) doc/* $(resin_root)/doc; \
	  mkdir -p $(resin_root)/project-jars; \
	  cp -r $(CP_U_FLAG) project-jars/* $(resin_root)/project-jars; \
	  mkdir -p $(resin_log); \
	fi
	if test "$(resin_conf)" != $(abs_builddir)/conf; then \
	  mkdir -p $(resin_conf); \
	  cp $(CP_U_FLAG) conf/app-default.xml $(resin_conf)/app-default.xml; \
	  cp $(CP_U_FLAG) conf/resin.xml $(resin_conf)/resin.xml; \
	fi
	(cd modules/c/src; $(MAKE) install)
	if test -r $(resin_pro)/modules/c/src/Makefile; then \
		(cd $(resin_pro)/modules/c/src; $(MAKE) install) \
	fi
	@ if test -n "$(resin_init_d)" -a "$(resin_home)" != $(abs_builddir); then \
		if test -f "${resin_init_d}"; then\
			if test \( -w "${resin_init_d}" \) -o \( -w `dirname ${resin_init_d}` \); then \
				cp $(CP_U_FLAG) $(abs_builddir)/init.d/resin $(resin_init_d); \
				chmod +x $(resin_init_d) && echo "${resin_init_d} updated."; \
				echo "cp $(CP_U_FLAG) $(abs_builddir)/init.d/resin $(resin_init_d)"; \
			else \
				echo ;\
				echo ;\
				echo "****************************************************************"; \
				echo "WARNING: Unable to install ${resin_init_d}"; \
				echo "         Destination is not writable. Usually, only root has";\
				echo "         permissions to install this file.";\
				echo;\
				echo "         This file is not required, but is used to start Resin"; \
				echo "         at boot time.";\
				echo "****************************************************************"; \
				echo ;\
				echo ;\
			fi; \
		else \
			cp $(abs_builddir)/init.d/resin $(resin_init_d); \
                        chmod +x $(resin_init_d) && echo "${resin_init_d} installed."; \
                fi; \
	fi
