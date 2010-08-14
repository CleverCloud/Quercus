/*
 * Copyright (c) 1999-2004 Caucho Technology.  All rights reserved.
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 *
 * $Id: nsapi.cpp,v 1.2 2004/09/29 16:58:24 cvs Exp $
 */

#include <windows.h>
#include <stdio.h>
#include <sys/stat.h>
#include "setup.h"


char *
get_netscape_home()
{
	char buf[1024];
	char hostname[1024];
	char *drive = getenv("SYSTEMDRIVE");
	struct stat st;

	// netscape doesn't seem to put itself in the registry

	if (! drive)
		return "z:";

	WSAData data;
	WSAStartup(MAKEWORD(2, 2), &data);
	hostname[0] = 0;
	gethostname(hostname, sizeof(hostname));

	struct hostent *host = gethostbyname(hostname);
	if (! host)
		return 0;

	sprintf(buf, "%s/iPlanet/Servers/https-%s1", drive, host->h_name);
	
	if (! stat(buf, &st))
		return strdup(buf);

	sprintf(buf, "%s/Netscape/Server4/https-%s", drive, host->h_name);
	
	if (! stat(buf, &st))
		return strdup(buf);

	return 0;
}

static char *
config_init(char *file_name, char *backup_file, char *resin_home)
{
	FILE *is;
	FILE *os;
	char buf[4096];
	
	is = fopen(file_name, "r");

	if (! is)
		return strdup(rsprintf(buf, "Can't find Netscape's %s", file_name));

	os = fopen(backup_file, "w+");

	if (! os) {
		fclose(is);
		return strdup(rsprintf(buf, "Can't write Netscape's %s", backup_file));
	}
	
	int lastInitModule = -1;
	int hasCaucho = 0;
	int line = 0;
	while (fgets(buf, sizeof(buf), is)) {
		fputs(buf, os);
		line++;
		char cmd[1024];
		int args = sscanf(buf, "%s", cmd);
		
		if (strstr(buf, "caucho_status")) {
			hasCaucho = 1;
		}

		if (args >= 1 && ! strcmp(cmd, "Init"))
			lastInitModule = line;
	}

	fclose(is);
	fclose(os);

	if (hasCaucho || lastInitModule < 0)
		return 0;

	is = fopen(backup_file, "r");
	os = fopen(file_name, "w+");

	line = 0;
	int isFirst = 1;
	while (fgets(buf, sizeof(buf), is)) {
		fputs(buf, os);

		line++;
		if (line == lastInitModule) {
			fprintf(os, "Init fn=\"load-modules\" shlib=\"%s/libexec/nsapi.dll\" "
				"funcs=\"caucho_service,caucho_filter,caucho_status\"\n",
				resin_home);
		}
	}

	return 0;
}

char *
configure_netscape(HWND hDlg, char *resin_home_raw, char *netscape_home)
{
	char obj_name[1024];
	char bak_name[1024];
	char resin_home[1024];
	char buf[1024];
	char *status;
	FILE *is;
	FILE *os;
	int i;

	// netscape needs forward slashes
	for (i = 0; resin_home_raw[i]; i++) {
		if (resin_home_raw[i] == '\\')
			resin_home[i] = '/';
		else
			resin_home[i] = resin_home_raw[i];
	}
	resin_home[i] = 0;

	int isEtc = 0;
	
	wsprintf(obj_name, "%s/config/magnus.conf", netscape_home);
	wsprintf(bak_name, "%s/config/magnus.conf.bak", netscape_home);

	status = config_init(obj_name, bak_name, resin_home);
	if (status)
		return status;

	wsprintf(obj_name, "%s/config/obj.conf", netscape_home);
	wsprintf(bak_name, "%s/config/obj.conf.bak", netscape_home);

	status = config_init(obj_name, bak_name, resin_home);
	if (status)
		return status;

	is = fopen(obj_name, "r");

	if (! is)
		return strdup(rsprintf(buf, "Can't find Netscape's %s", obj_name));

	os = fopen(bak_name, "w+");

	if (! os) {
		fclose(is);
		return "Can't write Netscape obj.conf.bak";
	}

	int hasCaucho = 0;
	int line = 0;
	while (fgets(buf, sizeof(buf), is)) {
		fputs(buf, os);
		line++;
		char cmd[1024];
		int args = sscanf(buf, "%s", cmd);
		
		if (strstr(buf, "caucho-status")) {
			hasCaucho = 1;
		}
	}

	fclose(is);
	fclose(os);

	if (hasCaucho)
		return 0;

	is = fopen(bak_name, "r");
	os = fopen(obj_name, "w+");

	line = 0;
	int isFirst = 1;
	while (fgets(buf, sizeof(buf), is)) {
		char cmd[1024];
		int args = sscanf(buf, "%s", cmd);
		if (isFirst && args > 0 && ! strcmp(cmd, "NameTrans")) {
			isFirst = 0;
			fprintf(os, "NameTrans fn=\"caucho_filter\" conf=\"%s/conf/resin.conf\" name=\"resin\"\n",
				resin_home);
			fprintf(os, "NameTrans fn=\"assign-name\" from=\"/caucho-status\" name=\"caucho-status\"\n");
		}

		fputs(buf, os);

		line++;
	}

	fprintf(os, "\n");
	fprintf(os, "<Object name=\"resin\">\n");
	fprintf(os, "Service fn=\"caucho_service\"\n");
	fprintf(os, "</Object>\n\n");
	fprintf(os, "<Object name=\"caucho-status\">\n");
	fprintf(os, "Service fn=\"caucho_status\"\n");
	fprintf(os, "</Object>\n");
	fclose(is);
	fclose(os);

	return 0;
}
