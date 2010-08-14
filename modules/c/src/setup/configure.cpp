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
 * $Id: configure.cpp,v 1.2 2004/09/29 16:58:24 cvs Exp $
 */

#include <windows.h>
#include <string.h>
#include "setup.h"

#define HKEY_RESIN "Software\\Caucho Technology\\Resin\\CurrentVersion"

HKEY
reg_lookup(HKEY hkey, char *path)
{
	HKEY newKey;
	DWORD rc;

	rc = RegOpenKeyEx(hkey, path, 0, KEY_QUERY_VALUE, &newKey);
	if (rc != ERROR_SUCCESS)
		return 0;

	return newKey;
}

void
reg_set_string(char *path, char *name, char *value)
{
	char buf[1024];
	strcpy(buf, path);
	path = buf;
	HKEY newKey = 0;


	HKEY hkey = HKEY_LOCAL_MACHINE;
	while (*path) {
		DWORD rc;
		DWORD disp;

		char *tail = strchr(path, '\\');
		if (! tail)
			tail = path + strlen(path);
		int oldChar = *tail;
		*tail = 0;

		rc = RegCreateKeyEx(hkey, path, 0, 
							"REG_SZ", REG_OPTION_NON_VOLATILE, KEY_ALL_ACCESS, 
							0, &newKey, &disp);
		if (hkey != HKEY_LOCAL_MACHINE)
			RegCloseKey(hkey);

		if (rc != ERROR_SUCCESS)
			return;

		*tail = oldChar;
		if (oldChar)
			path = tail + 1;
		else
			break;
	
		hkey = newKey;
	}

	RegSetValueEx(newKey, name, 0, REG_SZ, (unsigned char *) value, strlen(value));

	RegCloseKey(newKey);
}

void
set_resin_home(char *resin_home)
{
	reg_set_string(HKEY_RESIN, "Resin Home", resin_home);
}

void
set_resin_registry(char *resin_home)
{
}

