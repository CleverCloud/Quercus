/*
 * Copyright (c) 1999 Caucho Technology.  All rights reserved.
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
 * @author Robert Denny
 *
 * $Id: website.cpp,v 1.0 ***************** MANUAL *****************
 */

#include <windows.h>
#include <stdio.h>
#include <sys/stat.h>
#include "setup.h"

#define ROOT_KEY "Software\\Denny\\WebServer\\CurrentVersion"
#define ASSOC_KEY "Software\\Denny\\WebServer\\CurrentVersion\\AssocMap"
#define TYPE_KEY "Software\\Denny\\WebServer\\CurrentVersion\\TypeMap"

static HKEY
reg_lookup(HKEY hkey, char *path)
{
	HKEY newKey;
	DWORD rc;

	rc = RegOpenKeyEx(hkey, path, 0, 
			KEY_SET_VALUE|KEY_QUERY_VALUE|KEY_ENUMERATE_SUB_KEYS, &newKey);
	if (rc != ERROR_SUCCESS)
		return 0;

	return newKey;
}

static char *
reg_query_string(HKEY key, char *name, char *value)
{
	char buf[1024];
	DWORD len = sizeof buf;
	DWORD type;
	int rc = RegQueryValueEx(key, name, 0, &type, (LPBYTE) buf, &len);

	if (rc != ERROR_SUCCESS || type != REG_SZ)
		return 0;

	strcpy(value, buf);

	return value;
}

char *
get_website_home()
{
	char buf[1024];
	HKEY hKeyRoot;

	hKeyRoot = reg_lookup(HKEY_LOCAL_MACHINE, ROOT_KEY);
	if(hKeyRoot == 0)
		return 0;
	if(reg_query_string(hKeyRoot, "ServerRoot", buf) == 0)
		return 0;
	RegCloseKey(hKeyRoot);
	return strdup(buf);
}

char *
configure_website(HWND hDlg, char *resin_home, char *website_home)
{
	char src_name[1024];
	char dst_name[1024];
	FILE *src_file;
	FILE *dst_file;
	HKEY hKey;
	char buf[1024];
	char *cp;
	int len;

	//
	// Copy the ISAPI DLL to WebSite's home directory
	//
	sprintf(src_name, "%s\\bin\\isapi_srun.dll", resin_home);
	sprintf(dst_name, "%s\\isapi_srun.dll", website_home);

	dst_file = fopen(dst_name, "w+b");
	if (! dst_file)
		return "You must stop WebSite for setup to install the Resin ISAPI connector.";

	src_file = fopen(src_name, "rb");
	if (! src_file) {
		fclose(dst_file);
		return "Can't open isapi_srun.dll in RESIN_HOME";
	}

	while ((len = fread(buf, 1, sizeof(buf), src_file)) > 0) {
		fwrite(buf, 1, len, dst_file);
	}

	fclose(src_file);
	fclose(dst_file);

	//
	// Create the associations and server-side content type so that
	// WebSite knows what to do with .jsp, .xml, and .xtp files.
	//
	hKey = reg_lookup(HKEY_LOCAL_MACHINE, ASSOC_KEY);
	if(hKey == 0)
		return "Can't find WebSite association data in registry";
	
	len = strlen(dst_name) + 1;
	RegSetValueEx(hKey, ".jsp", 0, REG_SZ, (CONST BYTE *)dst_name, len);
	RegSetValueEx(hKey, ".xml", 0, REG_SZ, (CONST BYTE *)dst_name, len);
	RegSetValueEx(hKey, ".xtp", 0, REG_SZ, (CONST BYTE *)dst_name, len);
	RegCloseKey(hKey);

	hKey = reg_lookup(HKEY_LOCAL_MACHINE, TYPE_KEY);
	if(hKey == 0)
		return "Can't find WebSite type data in registry";
	
	cp = "wwwserver/isapi";
	len = strlen(cp) + 1;
	RegSetValueEx(hKey, ".jsp", 0, REG_SZ, (CONST BYTE *)cp, len);
	RegSetValueEx(hKey, ".xml", 0, REG_SZ, (CONST BYTE *)cp, len);
	RegSetValueEx(hKey, ".xtp", 0, REG_SZ, (CONST BYTE *)cp, len);
	RegCloseKey(hKey);

	//
	// Add isapi_srun.dll to the LoadLibrary list (unless already there).
	//
	hKey = reg_lookup(HKEY_LOCAL_MACHINE, ROOT_KEY);
	if(hKey == 0)
		return "Can't find WebSite root data in registry";
	if(reg_query_string(hKey, "LoadLibrary", buf) == 0)
		return 0;
	if(strstr(buf, "isapi_srun.dll") == NULL)			// If not already appended
	{
		if(buf[0] != '\0')								// If existing LoadLibrary stuff
			strcat(buf, ";");							// Insert delimiter
		strcat(buf, dst_name);
		RegSetValueEx(hKey, "LoadLibrary", 0, REG_SZ, (CONST BYTE *)buf, (strlen(buf) + 1));
	}
	RegCloseKey(hKey);

	return 0;
}
