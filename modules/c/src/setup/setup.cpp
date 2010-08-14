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
 * $Id: setup.cpp,v 1.4 2004/09/29 16:58:24 cvs Exp $
 */

#include <windows.h>
#include <stdio.h>
#include <sys/stat.h>
#include "resource.h"
#include "setup.h"
#include "../httpd/common.h"

static int g_is_silent;

struct setup_value {
	char *name;
	int id;
	char *value;
} setup_values[] = {
	{ "RESIN_HOME", IDC_RESIN_HOME, "" },
	{ "APACHE_HOME", IDC_APACHE_HOME, "" },
	{ "IIS_HOME", IDC_IIS_HOME, "" },
	{ "NETSCAPE_HOME", IDC_NETSCAPE_HOME, "" },
	{ "WEBSITE_HOME", IDC_WEBSITE_HOME, "" },
	{ 0, 0, 0}
};

void die(char *msg, ...) {}

static char *
get_setup_value(char *name)
{
	for (int i = 0; setup_values[i].name; i++) {
		if (! strcmp(setup_values[i].name, name))
			return setup_values[i].value;
	}

	return 0;
}

static void
set_setup_value(char *name, char *value)
{
	for (int i = 0; setup_values[i].name; i++) {
		if (! strcmp(setup_values[i].name, name)) {
			setup_values[i].value = value;
			return;
		}
	}
}

static void
set_setup_defaults(HWND hDlg)
{
	for (int i = 0; setup_values[i].name; i++) {
		SetDlgItemText(hDlg, setup_values[i].id, setup_values[i].value);
	}

	char *iis = get_setup_value("IIS_HOME");
	if (iis && *iis) {
		CheckDlgButton(hDlg, IDC_IIS_ENABLE, BST_CHECKED);
	}

	char *apache = get_setup_value("APACHE_HOME");
	if (apache && *apache) {
		CheckDlgButton(hDlg, IDC_APACHE_ENABLE, BST_CHECKED);
	}

	char *netscape = get_setup_value("NETSCAPE_HOME");
	if (netscape && *netscape) {
		CheckDlgButton(hDlg, IDC_NETSCAPE_ENABLE, BST_CHECKED);
	}

	char *website = get_setup_value("WEBSITE_HOME");
	if (website && *website) {
		CheckDlgButton(hDlg, IDC_WEBSITE_ENABLE, BST_CHECKED);
	}
}

static int
get_dialog_string(HWND hDlg, int id, char *buf, int size)
{
	HWND hChild = GetDlgItem(hDlg, id);
	if (! hChild)
		return 0;

	GetWindowText(hChild, buf, size);

	return 1;
}

static void
get_setup_defaults(HWND hDlg)
{
	for (int i = 0; setup_values[i].name; i++) {
		char buf[1024];

		if (GetDlgItemText(hDlg, setup_values[i].id, buf, sizeof(buf))) {
			setup_values[i].value = strdup(buf);
		}
	}
}

static int
apply(HWND hDlg)
{
	struct stat st;
	char buf[1024];
	char resin_home[1024];
	char iis_home[1024];
	char apache_home[1024];
	char netscape_home[1024];
	char website_home[1024];

	GetDlgItemText(hDlg, IDC_RESIN_HOME, resin_home, sizeof(resin_home));
	if (resin_home[0] && resin_home[strlen(resin_home) - 1] == '/')
		resin_home[strlen(resin_home) - 1] = 0;

	sprintf(buf, "%s\\win32\\isapi_srun.dll", resin_home);
	if (stat(buf, &st)) {
		sprintf(buf, "%s is an illegal RESIN_HOME.\nRESIN_HOME must have a win32\\isapi_srun.dll\n", resin_home);
		MessageBox(hDlg, buf, NULL, MB_OK);
		return 0;
	}

/*??
	sprintf(buf, "%s\\bin\\isapi_srun.dll", resin_home);
	if (stat(buf, &st)) {
		sprintf(buf, "%s is an illegal RESIN_HOME.\n", resin_home);
		MessageBox(hDlg, buf, NULL, MB_OK);
		return 0;
	}
??*/

	set_resin_home(resin_home);

	if (IsDlgButtonChecked(hDlg, IDC_IIS_ENABLE) == BST_CHECKED) {
		GetDlgItemText(hDlg, IDC_IIS_HOME, iis_home, sizeof(iis_home));
		char *msg = configure_iis(hDlg, resin_home, iis_home);
		if (msg) {
			MessageBox(hDlg, msg, NULL, MB_OK);
			return 0;
		}

		// MessageBox(hDlg, "You may need to restart IIS to see the new changes.", "Information", MB_OK);
	}

	if (IsDlgButtonChecked(hDlg, IDC_APACHE_ENABLE) == BST_CHECKED) {
		GetDlgItemText(hDlg, IDC_APACHE_HOME, apache_home, sizeof(apache_home));
		char *msg = configure_apache(hDlg, resin_home, apache_home);
		if (msg) {
			MessageBox(hDlg, msg, NULL, MB_OK);
			return 0;
		}
	}

	if (IsDlgButtonChecked(hDlg, IDC_NETSCAPE_ENABLE) == BST_CHECKED) {
		GetDlgItemText(hDlg, IDC_NETSCAPE_HOME, netscape_home, sizeof(netscape_home));
		char *msg = configure_netscape(hDlg, resin_home, netscape_home);
		if (msg) {
			MessageBox(hDlg, msg, NULL, MB_OK);
			return 0;
		}
	}

	if (IsDlgButtonChecked(hDlg, IDC_WEBSITE_ENABLE) == BST_CHECKED) {
		GetDlgItemText(hDlg, IDC_WEBSITE_HOME, website_home, sizeof(website_home));
		char *msg = configure_website(hDlg, resin_home, website_home);
		if (msg) {
			MessageBox(hDlg, msg, NULL, MB_OK);
			return 0;
		}
	}

	return 1;
}

static int
remove(HWND hDlg)
{

	if (IsDlgButtonChecked(hDlg, IDC_IIS_ENABLE) == BST_CHECKED) {
		char *msg = remove_iis(hDlg);
		if (msg) {
			MessageBox(hDlg, msg, NULL, MB_OK);
			return 0;
		}
	}

	return 1;
}

#if defined USE_SHELL_DIRBROWSER
static void
select_directory(HWND hDlg, int home, char *title)
{
	BROWSEINFO bi;

}
#else
static void
select_directory(HWND hDlg, int home, char *title)
{
	static char *filter = "Directory\0\0\0\0";
	char buf[1024];

	OPENFILENAME filename;
	memset(&filename, 0, sizeof(filename));
	filename.lStructSize = sizeof(filename);
	filename.hwndOwner = hDlg;
	filename.lpstrFilter = filter;

	filename.lpstrFile = buf;
	filename.nMaxFile = sizeof(buf);
	GetDlgItemText(hDlg, home, buf, sizeof(buf));
	filename.lpstrTitle = title;
	filename.Flags = OFN_PATHMUSTEXIST|OFN_LONGNAMES|OFN_HIDEREADONLY;

	GetOpenFileName(&filename);

	SetDlgItemText(hDlg, home, buf);
}
#endif

static LRESULT CALLBACK
SetupAction(HWND hDlg, UINT message, WPARAM wParam, LPARAM lParam)
{
	int wmId;
	int wmEvent;

	switch (message) {
	case WM_INITDIALOG:
		set_setup_defaults(hDlg);
		SetFocus(GetDlgItem(hDlg, IDC_OK));
		return TRUE;

	case WM_COMMAND:
		wmId = LOWORD(wParam);
		wmEvent = HIWORD(wParam);

		switch (wmId) {
			/*
		case IDC_RESIN_FILE:
			select_directory(hDlg, IDC_RESIN_HOME, "Resin Home");
			return TRUE;

		case IDC_APACHE_FILE:
			select_directory(hDlg, IDC_APACHE_HOME, "Apache Home");
			return TRUE;

		case IDC_IIS_FILE:
			select_directory(hDlg, IDC_IIS_HOME, "IIS Script Directory");
			return TRUE;
*/
		case IDC_APPLY:
			apply(hDlg);
			return TRUE;

		case IDC_REMOVE:
			remove(hDlg);
			return TRUE;

		case IDC_OK:
			if (apply(hDlg))
				EndDialog(hDlg, LOWORD(wParam));
			return TRUE;

		case IDC_CANCEL:
		case WM_DESTROY:
			EndDialog(hDlg, LOWORD(wParam));
			return TRUE;
		}
		break;
	}

	return FALSE;
}

static void 
parse_server_args(int argc, char **argv)
{
	int i;

	for (i = 0; i < argc; i++) {
		if (! strcmp(argv[i], "-silent"))
			g_is_silent = true;
	}
}

int APIENTRY
WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR cmdLine, int nCmdShow)
{
	char program[8192];
	char iis_home[8192];

	if (! GetModuleFileName(NULL, program, sizeof(program)))
		return -1;

#if defined USE_SHELL_DIRBROWSER
	CoInitialize(NULL);								// For SHBrowseForFolder()
#endif

	get_iis_script_dir(iis_home);
	// On some NT systems, the registry returns "d:\inetpub\scripts,,200"
	for (int i = 0; iis_home[i]; i++) {
		if (iis_home[i] == ',')
			iis_home[i] = 0;
	}

	parse_server_args(__argc, __argv);

	char *apache_home = get_apache_home();
	if (! apache_home)
		apache_home = "";
	char *netscape_home = get_netscape_home();
	if (! netscape_home)
		netscape_home = "";
	char *website_home = get_website_home();
	if (! website_home)
		website_home = "";

	set_setup_value("RESIN_HOME", get_resin_home(0, program));
	set_setup_value("APACHE_HOME", apache_home);
	set_setup_value("IIS_HOME", iis_home);
	set_setup_value("NETSCAPE_HOME", netscape_home);
	set_setup_value("WEBSITE_HOME", website_home);

	DialogBox(hInstance, (LPCTSTR) IDD_SETUP_INIT, NULL, (DLGPROC) SetupAction);

	return FALSE;
}
