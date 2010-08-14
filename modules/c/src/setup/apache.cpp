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
 */

#include <windows.h>
#include <stdio.h>
#include <sys/stat.h>
#include "setup.h"

#define HKEY_APACHE "SOFTWARE\\Apache Group\\Apache"
#define HKEY_APACHE_HOME "ServerRoot"

static char *
reg_query_string(HKEY key, char *subkey, char *value)
{
	char buf[1024];
	DWORD len = sizeof buf;
	DWORD type;
	int rc = RegQueryValueEx(key, subkey, 0, &type, (LPBYTE) buf, &len);

	if (rc != ERROR_SUCCESS || type != REG_SZ)
		return 0;

	strcpy(value, buf);

	return value;
}


static HKEY
reg_lookup(HKEY hkey, char *path)
{
	HKEY newKey;
	DWORD rc;

	rc = RegOpenKeyEx(hkey, path, 0, KEY_QUERY_VALUE|KEY_ENUMERATE_SUB_KEYS, &newKey);
	if (rc != ERROR_SUCCESS)
		return 0;

	return newKey;
}

static int
get_apache_registry(char *home, int size)
{
	HKEY hKeyApache;
	char version[MAX_PATH + 1];
	char bestVersion[1024];
	int index = 0;
	HKEY hKeyVersion = 0;
	
	if (! (hKeyApache = reg_lookup(HKEY_LOCAL_MACHINE, HKEY_APACHE)))
		return 0;

	bestVersion[0] = 0;
	while ((RegEnumKey(hKeyApache, index++, version, sizeof(version))) == ERROR_SUCCESS) {
		if (strcmp(version, bestVersion) > 0)
			strcpy(bestVersion, version);
	}

	if (! (hKeyVersion = reg_lookup(hKeyApache, bestVersion)))
		return 0;

	if (! reg_query_string(hKeyVersion, HKEY_APACHE_HOME, home))
		return 0;

	return 1;
}

char *
get_apache_home()
{
	char buf[1024];
	char newBuf[1024];
	WIN32_FIND_DATA findData;

	if (! get_apache_registry(buf, sizeof(buf))) {
		struct stat st;

		strcpy(newBuf, "\\Program Files\\Apache Group\\Apache2");
		if (! stat(newBuf, &st)) {
		  return strdup(newBuf);
		}


		strcpy(newBuf, "\\Program Files\\Apache Group\\Apache");
		if (! stat(newBuf, &st)) {
		  return strdup(newBuf);
		}

		return 0;
	}

	/*
	 * The Apache registry stores the path in the short form, so
	 * we'll change to the long form so users don't go nuts.
	 */
	char *ptr = buf;
	newBuf[0] = 0;
	if (buf[1] == ':') {
		strncpy(newBuf, buf, 2);
		newBuf[2] = 0;
		ptr += 2;
	}

	while (*ptr) {
		if (*ptr == '/' || *ptr == '\\')
			ptr++;

		char old;
		char *next = 0;
		if ((next = strchr(ptr, '/')) || (next = strchr(ptr, '\\'))) {
			old = *next;
			*next = 0;
		}
		else {
			old = 0;
			next = ptr + strlen(ptr);
		}
		
		FindFirstFile(buf, &findData);
		strcat(newBuf, "\\");
		strcat(newBuf, findData.cFileName);
		if (old)
			*next++ = old;
		ptr = next;
	}

	return strdup(newBuf);
}

char *
configure_apache(HWND hDlg, char *resin_home, char *apache_home)
{
	char buf[1024];
    char esc_resin_home[1024];
	FILE *is;
	FILE *os;
    int i, j;
	int isApache2 = 0;
	char *apache_version = "apache-2.0";

	isApache2 = (strstr(apache_home, "Apache2") != 0);

	if (isApache2)
		apache_version = "apache-2.0";
	else
		apache_version = "apache-1.3";
	
    j = 0;
    for (i = 0; resin_home[i]; i++) {
	if (resin_home[i] == '\\')
		esc_resin_home[j++] = '/';
        else
                esc_resin_home[j++] = resin_home[i];
    }
    esc_resin_home[j] = 0;

	int isEtc = 0;
	
	is = fopen(rsprintf(buf, "%s/etc/httpd.conf", apache_home), "r");
	if (is) {
		isEtc = 1;
		os = fopen(rsprintf(buf, "%s/etc/httpd.conf.bak", apache_home), "w+");
	}
	else {
		is = fopen(rsprintf(buf, "%s/conf/httpd.conf", apache_home), "r");
		if (is)
			os = fopen(rsprintf(buf, "%s/conf/httpd.conf.bak", apache_home), "w+");
	}

	if (! is)
		return "Can't find Apache httpd.conf";
	if (! os) {
		fclose(os);
		return "Can't write Apache httpd.conf.bak";
	}

	int lastAddModule = 0;
	int lastLoadModule = 0;
	int hasCaucho = 0;
	int line = 0;
	while (fgets(buf, sizeof(buf), is)) {
		fputs(buf, os);
		line++;
		char cmd[1024];
		char module[1024];
		char file[1024];
		int args = sscanf(buf, "%s%s%s", cmd, module, file);
		
		if (args >= 2 && ! strcmp(cmd, "LoadModule") && ! strcmp(module, "caucho_module"))
			hasCaucho = 1;

		if (args >= 3 && (! strcmp(cmd, "LoadModule") || ! strcmp(cmd, "#LoadModule")))
			lastLoadModule = line;

		if (args >= 2 && (! strcmp(cmd, "AddModule") || ! strcmp(cmd, "#AddModule")))
			lastAddModule = line;
	}

	if (lastAddModule < lastLoadModule)
	  lastAddModule = lastLoadModule;

	fclose(is);
	fclose(os);

	if (hasCaucho)
		return 0;

	if (isEtc) {
		is = fopen(rsprintf(buf, "%s/etc/httpd.conf.bak", apache_home), "r");
		os = fopen(rsprintf(buf, "%s/etc/httpd.conf", apache_home), "w+");
	}
	else {
		is = fopen(rsprintf(buf, "%s/conf/httpd.conf.bak", apache_home), "r");
		os = fopen(rsprintf(buf, "%s/conf/httpd.conf", apache_home), "w+");
	}

	line = 0;
	while (fgets(buf, sizeof(buf), is)) {
		fputs(buf, os);

		line++;
		if (line == lastLoadModule)
			fprintf(os, "LoadModule caucho_module \"%s/win32/%s/mod_caucho.dll\"\n", 
				esc_resin_home, apache_version);
		if (line == lastAddModule && ! isApache2)
			fprintf(os, "AddModule mod_caucho.c\n");
	}

	if (! lastLoadModule)
		fprintf(os, "LoadModule caucho_module \"%s/win32/%s/mod_caucho.dll\"\n", 
				esc_resin_home, apache_version);
	/*
	if (! lastAddModule && ! isApache2)
		fprintf(os, "AddModule mod_caucho.c\n");
	*/

	fprintf(os, "<IfModule mod_caucho.c>\n");
	/*
	fprintf(os, "  CauchoConfigFile \"%s/conf/resin.conf\"\n", esc_resin_home);
	*/
	fprintf(os, "  ResinConfigServer localhost 6800\n");
	fprintf(os, "  CauchoStatus yes\n");
	fprintf(os, "</IfModule>\n");
	fclose(is);
	fclose(os);

	return 0;
}
