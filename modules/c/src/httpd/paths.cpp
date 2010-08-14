/*
 * Copyright (c) 1999-2006 Caucho Technology.  All rights reserved.
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
#include "process.h"
#include "stdlib.h"
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <stdarg.h>
#include <io.h>
#include <fcntl.h>
#include "common.h"

#define HKEY_JRE "Software\\JavaSoft\\Java Runtime Environment"
#define HKEY_JDK "Software\\JavaSoft\\Java Development Kit"
#define HKEY_VERSION "CurrentVersion"
#define HKEY_11 "1.1"
#define HKEY_12 "1.2"
#define HKEY_JAVA_HOME "JavaHome"
#define HKEY_RESIN_OLD "Software\\Caucho Technology\\Resin\\1.1"
#define HKEY_RESIN "Software\\Caucho Technology\\Resin\\CurrentVersion"
#define HKEY_RESIN_HOME "Resin Home"

#define BUF_SIZE (32 * 1024)

extern void install_service(char *name);
extern void remove_service(char *name);
static int g_is_dead = 0;
static int g_keepalive_fd = -1;

FILE *out = stdout;
FILE *err = stderr;

void
log(char *fmt, ...)
{
#ifdef DEBUG
	va_list arg;
	va_start(arg, fmt);

	if (0 && err) {
		if (fmt)
			vfprintf(err, fmt, arg);
		fflush(err);
	}
	else {
		FILE *file = fopen("e:/temp/foo.log", "a+");
		if (file && fmt) {
			vfprintf(file, fmt, arg);
			fclose(file);
		}
	}

	va_end(arg);
#endif
}

char *
rsprintf(char *buf, char *fmt, ...)
{
	va_list args;
	
	va_start(args, fmt);
	vsprintf(buf, fmt, args);
	va_end(args);

	return buf;
}

static char *
canonicalize(char *a)
{
	if (! a)
		return 0;

	char *buf = (char *) malloc(strlen(a) + 1);
	char *result = buf;

	for (; *a; a++) {
		if (*a == '/')
			*buf++ = '\\';
		else
			*buf++ = *a;
	}

	*buf = 0;
	if (*result && result[strlen(result) - 1] == '\\')
		result[strlen(result) - 1] = 0;

	return result;
}

char *
concat(char *a, char *b)
{
	char *buf = (char *) malloc(strlen(a) + strlen(b) + 1);
	strcpy(buf, a);
	strcat(buf, b);

	return buf;
}

static char *
find_path(char *exe)
{
	char buf[BUF_SIZE];
	char *path = getenv("PATH");
	struct stat info;

	if (! path)
		return 0;

	int head = 0;
	while (path[head]) {
		int i = 0;
		int tail;
		for (tail = head; path[tail] && path[tail] != ';'; tail++) {
			buf[i++] = path[tail];
		}
		buf[i] = 0;

		strcat(buf, "\\");
		strcat(buf, exe);
		if (! stat(buf, &info)) {
			return strdup(buf);
		}
		head = path[tail] == ';' ? tail + 1 : tail;
	}

	return 0;
}

static char *
get_canonical_path(char *path)
{
	char buf[8192];

	GetCurrentDirectory(sizeof buf, buf);
	if (path[0] == '/' || path[0] == '\\') {
		buf[2] = 0;
		strcat(buf, path);
	} else if ((path[0] >= 'a' && path[0] <= 'z' ||
				path[0] >= 'A' && path[0] <= 'Z') && path[1] == ':') {
		strcpy(buf, path);
	} else {
		strcat(buf, "\\");
		strcat(buf, path);
	}

	for (int i = 0; buf[i]; i++) {
		if (buf[i] == '/')
			buf[i] = '\\';

		// Collapse /./
		if (buf[i] == '\\' && buf[i + 1] == '.' &&
			(buf[i + 2] == 0 || buf[i + 2] == '/' || buf[i + 2] == '\\')) {
			int j;
			for (j = 0; buf[i + j + 2]; j++)
				buf[i + j] = buf[i + j + 2];
			buf[i + j] = 0;
			i--;
		} // Collapse /../ 
		else if (buf[i] == '\\' && buf[i + 1] == '.' && buf[i + 2] == '.' &&
			(buf[i + 3] == 0 || buf[i + 3] == '/' || buf[i + 3] == '\\')) {
		    int j, k;
			for (j = i - 1; j >= 0 && j != '\\'; j--) {
			}
			for (k = 0; buf[i + k + 3]; k++)
				buf[j + k] = buf[i + k + 3];
			buf[j + k] = 0;
			i = j - 1;
		}
	}

	return strdup(buf);
}

static char *
get_parent(char *path)
{
	char buf[BUF_SIZE];
	int i;

	strcpy(buf, path);

	for (i = strlen(path) - 1; i >= 0 && buf[i] != '\\'; i--) {
	}

	buf[i] = 0;

	return strdup(buf);
}

static char *
reg_query_string(HKEY key, char *subkey, char *value)
{
	char buf[BUF_SIZE];
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

	rc = RegOpenKeyEx(hkey, path, 0, KEY_QUERY_VALUE, &newKey);
	if (rc != ERROR_SUCCESS)
		return 0;

	return newKey;
}

static char *
find_jdk_registry(char *jdk)
{
	HKEY hKeyJdk;
	char buf[BUF_SIZE];
	HKEY hKeyVersion = 0;
	
	if (! (hKeyJdk = reg_lookup(HKEY_LOCAL_MACHINE, jdk)))
		return 0;

	if (reg_query_string(hKeyJdk, HKEY_VERSION, buf)) {
		hKeyVersion = reg_lookup(hKeyJdk, buf);
	}
	else if ((hKeyVersion = reg_lookup(hKeyJdk, HKEY_12))) {
	}
	else if ((hKeyVersion = reg_lookup(hKeyJdk, HKEY_11))) {
	}

	if (! hKeyVersion)
		return 0;

	if (! reg_query_string(hKeyVersion, HKEY_JAVA_HOME, buf))
		return 0;

	return strdup(buf);
}

char *
add_classpath(char *cp, char *path)
{
  if (*path == '"')
    path++;
  
  if (! cp)
    cp = strdup(path);
  else {
    char *buf = (char *) malloc(strlen(cp) + strlen(path) + 5);
    strcpy(buf, cp);
    strcat(buf, ";");
    strcat(buf, path);

    free(cp);

    cp = buf;
  }

  if (cp[strlen(cp) - 1] == '"')
    cp[strlen(cp) - 1] = 0;

  return cp;
}


char *
get_java_home(char *resin_home, char *java_home)
{
	char buf[BUF_SIZE];
	struct stat info;
	char *path;

	if (java_home) {
	}
	else if (getenv("JAVA_HOME")) {
		java_home = canonicalize(getenv("JAVA_HOME"));
	}
	else if ((java_home = find_jdk_registry(HKEY_JDK))) {
	}
	else if ((java_home = find_jdk_registry(HKEY_JRE))) {
	}
	else if (! stat(rsprintf(buf, "%s\\jre\\bin\\java.exe", resin_home), &info)) {
		java_home = strdup(rsprintf(buf, "%s\\jre", resin_home));
	}
	else if ((path = find_path("java.exe"))) {
		path = get_canonical_path(path);
		path = get_parent(get_parent(path));
		if (! stat(rsprintf(buf, "%s\\bin\\java.exe", path), &info))
			java_home = path;
	}
	else if ((path = find_path("jre.exe"))) {
		path = get_canonical_path(path);
		path = get_parent(get_parent(path));
		if (! stat(rsprintf(buf, "%s\\bin\\jre.exe", path), &info))
			java_home = path;
	}

	if (! java_home && ! stat("\\java\\lib", &info)) {
		java_home = "\\java";
	}

	if (! java_home && ! stat("\\jre\\lib", &info)) {
		java_home = "\\jre";
	}

	if (! java_home) {
		WIN32_FIND_DATA dir;
		HANDLE hSearch;

		hSearch = FindFirstFile("\\jdk*", &dir);
		if (hSearch != INVALID_HANDLE_VALUE) {
			do {
				if (! stat(rsprintf(buf, "\\%s\\bin\\java.exe", dir.cFileName), &info)) {
					char *test_dir = concat("\\", dir.cFileName);

					if (! java_home || strcmp(java_home, test_dir) < 0)
						java_home = test_dir;
				}
			} while (FindNextFile(hSearch, &dir));
		}

		hSearch = FindFirstFile("\\program files\\java\\jdk*", &dir);
		if (hSearch != INVALID_HANDLE_VALUE) {
			do {
				if (! stat(rsprintf(buf, "\\%s\\bin\\java.exe", dir.cFileName), &info)) {
					char *test_dir = concat("\\", dir.cFileName);

					if (! java_home || strcmp(java_home, test_dir) < 0)
						java_home = test_dir;
				}
			} while (FindNextFile(hSearch, &dir));
		}

	}

	if (! java_home) {
		WIN32_FIND_DATA dir;
		HANDLE hSearch;

		hSearch = FindFirstFile("\\jre*", &dir);
		if (hSearch != INVALID_HANDLE_VALUE) {
			do {
				if (! stat(rsprintf(buf, "\\%s\\bin\\jre.exe", dir.cFileName), &info)) {
					char *test_dir = concat("\\", dir.cFileName);

					if (! java_home || strcmp(java_home, test_dir) < 0)
						java_home = test_dir;
				}
			} while (FindNextFile(hSearch, &dir));
		}

	}

	return java_home;
}

static char *
find_resin_registry()
{
	HKEY hKeyResin;
	char buf[BUF_SIZE];
	
	if (! (hKeyResin = reg_lookup(HKEY_LOCAL_MACHINE, HKEY_RESIN))) {
		LOG(("can't find resin %s\n", HKEY_RESIN));
		return 0;
	}

	if (reg_query_string(hKeyResin, HKEY_RESIN_HOME, buf)) {
		LOG(("resin home %s\n", buf));
		return strdup(buf);
	}
	else {
		LOG(("can't find resin home\n"));
		return 0;
	}
}

char *
get_resin_home(char *resin_home, char *path)
{
	char *program = path;
	char root[1024];
	char buf[BUF_SIZE];
	struct stat info;

	if (resin_home)
		return resin_home;

	GetCurrentDirectory(sizeof(root), root);
	root[2] = 0;

	if (getenv("RESIN_HOME"))
		return canonicalize(getenv("RESIN_HOME"));

	path = get_canonical_path(path);
	path = get_parent(path);

	/*
	 * These files must come first so starting Resin from its
	 * directory will take precedence.
	 */
	if (! stat(rsprintf(buf, "%s\\lib\\resin.jar", path), &info))
		return canonicalize(path);

	path = get_parent(path);

	/*
	 * These files must come first so starting Resin from its
	 * directory will take precedence.
	 */
	if (! stat(rsprintf(buf, "%s\\lib\\resin.jar", path), &info))
		return canonicalize(path);


	/*
	 * XXX: This caused isapi_srun.dll to look at c:\resin
	 * in preference to the value in the registry.
	 *
	 * if (! stat(wsprintf(buf, "%s\\resin\\conf\\resin.conf", root), &info))
	 *	return canonicalize(wsprintf(buf, "%s\\resin", root));
	 */

	if ((resin_home = find_resin_registry()))
		return canonicalize(resin_home);

	WIN32_FIND_DATA dir;
	HANDLE hSearch;
	char *found_path = 0;

	// Returns the last, i.e. most recent, version of Resin
	hSearch = FindFirstFile(rsprintf(buf, "%s\\resin*", root), &dir);
	if (hSearch != INVALID_HANDLE_VALUE) {
		do {
			if (! stat(rsprintf(buf, "%s\\%s\\lib\\resin.jar", root, dir.cFileName), &info)) {
				found_path = canonicalize(rsprintf(buf, "%s\\%s", root, dir.cFileName));
			}
		} while (FindNextFile(hSearch, &dir));
	}

	if (found_path)
		return found_path;
	else
		return canonicalize(resin_home);
}

char *
set_classpath(char *cp, char *resin_home, char *java_home, char *env_classpath)
{
	WIN32_FIND_DATA scanDir;
	HANDLE hSearch;
	char buf[BUF_SIZE];
	struct stat info;

	if (env_classpath)
		cp = add_classpath(cp, env_classpath);

	cp = add_classpath(cp, rsprintf(buf, "%s\\classes", resin_home));
	cp = add_classpath(cp, rsprintf(buf, "%s\\lib\\resin.jar", resin_home));
	if (java_home) {
		if (! stat(rsprintf(buf, "%s\\lib\\classes.zip", java_home), &info))
			cp = add_classpath(cp, rsprintf(buf, "%s\\lib\\classes.zip", java_home));
		if (! stat(rsprintf(buf, "%s\\lib\\tools.jar", java_home), &info))
			cp = add_classpath(cp, rsprintf(buf, "%s\\lib\\tools.jar", java_home));
		if (! stat(rsprintf(buf, "%s\\jre\\lib\\rt.jar", java_home), &info))
			cp = add_classpath(cp, rsprintf(buf, "%s\\jre\\lib\\rt.jar", java_home));
		if (! stat(rsprintf(buf, "%s\\jre\\lib\\i18n.jar", java_home), &info))
			cp = add_classpath(cp, rsprintf(buf, "%s\\jre\\lib\\i18n.jar", java_home));
	}

	hSearch = FindFirstFile(rsprintf(buf, "%s\\lib\\*.jar", resin_home), &scanDir);
	if (hSearch != INVALID_HANDLE_VALUE) {
		do {
			if (strcmp(scanDir.cFileName, "resin.jar") &&
			    strcmp(scanDir.cFileName, "jdk12.jar") &&
			    strcmp(scanDir.cFileName, "servlet.jar") &&
                            strcmp(scanDir.cFileName, "jsdk.jar"))
				cp = add_classpath(cp, rsprintf(buf, "%s\\lib\\%s", resin_home, scanDir.cFileName));
		} while (FindNextFile(hSearch, &scanDir));
	}

	hSearch = FindFirstFile(rsprintf(buf, "%s\\lib\\*.zip", resin_home), &scanDir);
	if (hSearch != INVALID_HANDLE_VALUE) {
		do {
			cp = add_classpath(cp, rsprintf(buf, "%s\\lib\\%s", resin_home, scanDir.cFileName));
		} while (FindNextFile(hSearch, &scanDir));
	}

	if (env_classpath)
		cp = add_classpath(cp, env_classpath);

        char *tail = cp + strlen(cp);
        if (*cp && tail[-1] == '\\')
          tail[-1] = 0;

	return cp;
}

char *
get_java_exe(char *java_home)
{
	char buf[BUF_SIZE];
	struct stat info;

	if (! java_home)
		return "jview.exe";
	else if (! stat(rsprintf(buf, "%s\\bin\\java.exe", java_home), &info))
		return concat(java_home, "\\bin\\java.exe");
	else if (! stat(rsprintf(buf, "%s\\bin\\jre.exe", java_home), &info))
		return concat(java_home, "\\bin\\jre.exe");
	else if (! stat(rsprintf(buf, "%s\\jrockit.exe", java_home), &info))
		return concat(java_home, "\\jrockit.exe");
	else {
		log("Can't find java executable in %s\n", java_home);
		return 0;
	}
}
