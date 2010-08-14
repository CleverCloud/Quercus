/*
 * Copyright 1999-2008 Caucho Technology.  All rights reserved.
 */

#include <windows.h>
#include <stdio.h>
#include "resource.h"
#include "common.h"

#define BUF_SIZE (32 * 1024)

static char **g_args;
static HINSTANCE g_hInstance;

static DWORD
StartThread(void *arg)
{
	if (g_args)
		spawn_java(g_args[0], g_args);

	exit(0);

	return 0;	
}

static LRESULT CALLBACK
ServerAction(HWND hDlg, UINT message, WPARAM wParam, LPARAM lParam)
{
	int wmId;
	int wmEvent;
	NOTIFYICONDATA iconData;

	switch (message) {
	case WM_INITDIALOG:
		/*
		memset(&iconData, 0, sizeof(iconData));

		iconData.uFlags = NIF_TIP|NIF_ICON;
		iconData.hWnd = hDlg;
		iconData.hIcon = LoadIcon(g_hInstance, MAKEINTRESOURCE(IDI_RESIN));
		printf("IDCON %p %p %p\n", g_hInstance, iconData.hIcon, MAKEINTRESOURCE(IDI_RESIN));
		iconData.uCallbackMessage = IDW_TRAY;
		iconData.cbSize = sizeof(iconData);
		strcpy(iconData.szTip, "Resin web server");

		Shell_NotifyIcon(NIM_ADD, &iconData);
		*/

		set_window(hDlg);
		CheckDlgButton(hDlg, IDC_START, BST_CHECKED);
		return TRUE;

	case WM_COMMAND:
		wmId = LOWORD(wParam);
		wmEvent = HIWORD(wParam);

		switch (wmId) {
		case IDC_START:
			start_server();
			return TRUE;

		case IDC_STOP:
			stop_server();
			return TRUE;

		case IDC_CANCEL:
		case WM_DESTROY:
			/*
			memset(&iconData, 0, sizeof(iconData));

			iconData.hWnd = hDlg;
			iconData.uCallbackMessage = IDW_TRAY;
			iconData.cbSize = sizeof(iconData);

			Shell_NotifyIcon(NIM_DELETE, &iconData);
			*/
			//quit_server();
			stop_server();
			EndDialog(hDlg, LOWORD(wParam));
			return TRUE;
		}
		break;

	case IDW_TRAY:
		return TRUE;
	}

	return FALSE;
}
/*
int main(int argc, char **argv)
{
	char *name = "Resin Web Server";
	char *class_name = "com.caucho.server.http.HttpServer";

	return start_service(name, class_name, argc, argv);
}
*/
int APIENTRY
WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR cmdLine, int nCmdShow)
{
	char *name = "Resin";
    char *full_name = "Resin Web Server";
	char *class_name = "com.caucho.boot.ResinBoot";
	DWORD threadId;

	if (start_service(name, full_name, class_name, __argc, __argv))
		return FALSE;
	g_args = get_server_args(name, full_name, class_name, __argc, __argv);
	if (! g_args)
		return FALSE;


	CreateThread(0, 0, (LPTHREAD_START_ROUTINE) StartThread, 0, 0, &threadId);

	DialogBox(hInstance, (LPCTSTR) IDD_SERVER, NULL, (DLGPROC) ServerAction);
	
	return FALSE;
}

void
main(char **argv, int argc)
{
//	FreeConsole();

	WinMain(0, 0, 0, 0);
}
