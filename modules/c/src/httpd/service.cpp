#ifndef _WINSOCKAPI_
#define _WINSOCKAPI_
#endif
#include <windows.h>
#include <winsock2.h>
#include <winsvc.h>
#include <stdio.h>
#include <stdarg.h>
#include "common.h"

#define BUF_SIZE (32 * 1024)

static SERVICE_STATUS_HANDLE g_status_handle;
static SERVICE_STATUS g_status;
static char *g_name;       // name of service
static char *g_full_name; // display name of service
static char *g_class_name; // name of class
static char *g_user;
static char *g_password;
static int g_argc;
static char **g_argv;
static TCHAR g_error[1024];

extern FILE *err;
extern FILE *out;

extern int exec_java(char *exe, char **args);
extern int run_server(char *name, char *class_name, int argc, char **argv, int is_service);
extern void stop_server();

static WSAEVENT wait_event;

static TCHAR *
format_error(int error)
{
	TCHAR *buf = g_error;

	FormatMessage(FORMAT_MESSAGE_FROM_SYSTEM,
		NULL, error, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
		buf, 1024, NULL);

	return g_error;
}

static int 
report_status(int currentState, int exitCode, int waitHint)
{
    static int checkPoint = 1;
 
    if (currentState == SERVICE_START_PENDING)
      g_status.dwControlsAccepted = 0;
    else
      g_status.dwControlsAccepted = SERVICE_ACCEPT_STOP;

    g_status.dwCurrentState = currentState;
    g_status.dwWin32ExitCode = exitCode;
    g_status.dwWaitHint = waitHint;

    if ((currentState == SERVICE_RUNNING) ||
	(currentState == SERVICE_STOPPED)) {
      g_status.dwCheckPoint = 0;
    }
    else
      g_status.dwCheckPoint = checkPoint++;

    // XXX: On failure, should add to event log
    return SetServiceStatus(g_status_handle, &g_status);
}

void
stop_resin()
{
	/*
	int len = g_argc;

	char **stop_args = (char **) malloc((len + 2) * sizeof(char *));
	memcpy(stop_args, g_argv, len * sizeof(char *));
	stop_args[len] = "stop";
	stop_args[len + 1] = 0;
	char **args = get_server_args(g_name, g_full_name, g_class_name, len + 1, stop_args);

	log("stopping %s\n", g_name);
		report_status(SERVICE_STOPPED, NO_ERROR, 0);
		*/

	if (wait_event) {
		WSASetEvent(wait_event);
	}
}

/*
 * Callback when the SCM calls ControlService()
 *
 * @param dwCtrlCode - type of control requested
 */
static VOID WINAPI service_ctrl(DWORD dwCtrlCode)
{
	log("CTRL %d\n", dwCtrlCode);
	switch(dwCtrlCode) {
        // Stop the service.
        //
        case SERVICE_CONTROL_STOP:
			report_status(SERVICE_STOP_PENDING, NO_ERROR, 0);
			//quit_server();
			stop_resin();
			break;

        // Update the service status.
        //
        case SERVICE_CONTROL_INTERROGATE:
            break;

        // invalid control code
        //
        default:
            break;
    }
}

static void
service_main(int argc, char **argv)
{
	int exit_status = 0;

	g_status_handle = RegisterServiceCtrlHandler(g_name, service_ctrl);
log("HANDLER %x\n", g_status_handle);
	if (! g_status_handle) {
		log("service has no handler\n");
		return;
	}

	int len = g_argc;

	char **start_args = (char **) malloc((len + 2) * sizeof(char *));

	for (int i = 0; i < len; i++)
		start_args[i] = g_argv[i];

	start_args[len] = "start";
	start_args[len + 1] = 0;

	char **stop_args = (char **) malloc((len + 2) * sizeof(char *));

	for (int i = 0; i < len; i++)
		stop_args[i] = g_argv[i];

	stop_args[len] = "stop";
	stop_args[len + 1] = 0;


	g_status.dwServiceType = SERVICE_WIN32_OWN_PROCESS;
	g_status.dwServiceSpecificExitCode = 0;

	report_status(SERVICE_RUNNING, NO_ERROR, 3000);
	char **args = get_server_args(g_name, g_full_name, g_class_name, 
		                          len + 1, start_args);//len + 1, start_args);

	if (args) {
		log("couldn't start %s (status %d)\n", g_name, exit_status);
	}
	else {
		log("started %s (status %d)\n", g_name, exit_status);
		//exit_status = spawn_java(start_args[0], start_args);
		wait_event = WSACreateEvent();
		WSAResetEvent(wait_event);
		WaitForSingleObject(wait_event, INFINITE);
	}
	get_server_args(g_name, g_full_name, g_class_name, 
		                          len + 1, stop_args);//len + 1, start_args);

	report_status(SERVICE_STOPPED, NO_ERROR, 0);
}

int
start_service(char *name, char *full_name, char *class_name, int argc, char **argv)
{
	SERVICE_TABLE_ENTRY dispatch[] = {
		{ TEXT(name), (LPSERVICE_MAIN_FUNCTION) service_main },
		{ NULL, NULL }
	};
	int is_service = 0;

	g_name = name;
	g_full_name = full_name;
	g_class_name = class_name;

	g_argc = argc;
	g_argv = argv;

	if (argc > 1 && ! strcmp(argv[1], "-service"))
		is_service = 1;

	if (is_service && ! StartServiceCtrlDispatcher(dispatch))
		die("Can't start NT service %s.\n%d: %s\n", name,
		    GetLastError(), format_error(GetLastError()));

	return is_service;
}

void
add_path(char *buf, char *path)
{
  int needs_escape = 0;
  int i;
  
	if (! path) {
		strcat(buf, "\"\"");
		return;
	}

	buf += strlen(buf);
	*buf++ = ' ';

	if (path[0] == '\'' || path[0] == '"')
	  needs_escape = 1;

	for (i = 0; path[i]; i++) {
	  if (isspace(path[i]))
	    needs_escape = 1;
	}

	if (needs_escape)
	  *buf++ = '"';

	for (; *path; path++) {
		int ch = *path;

		if (ch == '"' || ch == '\'') {
		}
		else
			*buf++ = ch;
	}

	if (needs_escape)
	  *buf++ = '"';
	
	*buf = 0;
}

/**
 * Installs Resin as a service.
 *
 * @param name service name
 * @param full_name full service name
 * @param service_args arguments to the service
 */
void 
install_service(char *name, char *full_name, char *user, char *password, 
				char **service_args)
{
    SC_HANDLE   service;
    SC_HANDLE   manager;

    TCHAR path[BUF_SIZE];
	TCHAR args[BUF_SIZE];
	TCHAR t_user[BUF_SIZE];
	TCHAR t_password[BUF_SIZE];
	int error;

    if (! GetModuleFileName(NULL, path, sizeof(path)))
		die("Can't get module executable\n%d: %s",
	      GetLastError(), format_error(GetLastError()));

	wsprintf(args, "\"%s\" -service", path);

	if (getenv("CLASSPATH")) {
		strcat(args, " -env-classpath ");
		add_path(args, getenv("CLASSPATH"));
	}

	if (getenv("JAVA_HOME")) {
		strcat(args, " -java_home ");
		add_path(args, getenv("JAVA_HOME"));
	}

	if (getenv("RESIN_HOME")) {
		strcat(args, " -resin_home ");
		add_path(args, getenv("RESIN_HOME"));
	}

	for (int i = 1; service_args[i]; i++) {
		if (! strcmp(service_args[i], "-install") || ! strcmp(service_args[i], "-remove"))
			continue;
		else if (! strcmp(service_args[i], "-install-as") || ! strcmp(service_args[i], "-remove-as")) {
			i++;
			continue;
		}

		strcat(args, " ");
		add_path(args, service_args[i]);
	}

    manager = OpenSCManager(NULL,               // machine (NULL == local)
			    NULL,               // database (NULL == default)
			    SC_MANAGER_ALL_ACCESS);   // access required

   if (! manager)
	   die("Can't open service manager\n%d: %s", 
	       GetLastError(), format_error(GetLastError()));
	/*
   if (! user) {
	   DWORD len = 256;
	   char *buf = (char*)malloc(len);
	   if (GetUserName(buf, &len) == S_OK)
		   user = buf;   
   }
	*/
   
   if (user && ! strchr(user, '\\')) {
	   int i, j;

	   j = 0;
	   t_user[j++] = '.';
	   t_user[j++] = '\\';

	   for (i = 0; user[i]; i++)
		   t_user[j++] = user[i];

	   t_user[j] = 0;
	   user = t_user;
   }

    service = CreateService(
            manager,     // manager
            name,        // service name
            full_name,   // display name
            SERVICE_ALL_ACCESS,         // desired access
            SERVICE_WIN32_OWN_PROCESS,  // service type
            SERVICE_AUTO_START,         // start type
            SERVICE_ERROR_NORMAL,       // error control type
            args,                       // service's binary
            NULL,                       // no load ordering group
            NULL,                       // no tag identifier
            NULL,                       // dependencies
			user,                       // LocalSystem account
			password);                      // no password

	error = GetLastError();
	format_error(GetLastError());
	// Don't automatically start the service
 	if (service)
		CloseServiceHandle(service);

    CloseServiceHandle(manager);

    if (! service)
		die("Can't install \"%s\" as an NT service\n%d: %s",
	      name, error, g_error);
}

void remove_service(char *name)
{
    SC_HANDLE service;
    SC_HANDLE manager;
    SERVICE_STATUS status;

    manager = OpenSCManager(
                        NULL,                   // machine (NULL == local)
                        NULL,                   // database (NULL == default)
                        SC_MANAGER_ALL_ACCESS   // access required
                        );
    if (! manager) {
		die("Can't open service manager\n%d: %s", GetLastError(),
		  format_error(GetLastError()));
    }

    service = OpenService(manager, name, SERVICE_ALL_ACCESS);

    if (service == NULL)
		die("Can't open service\n%d: %s", GetLastError(),
	      format_error(GetLastError()));

    // try to stop the service
    if (ControlService(service, SERVICE_CONTROL_STOP, &status)) {
		Sleep(1000);
		while (QueryServiceStatus(service, &status) &&
		       status.dwCurrentState == SERVICE_STOP_PENDING)
			Sleep(1000);
    }

    // now remove the service
    if (! DeleteService(service)) {
		int error = GetLastError();
		format_error(error);

      CloseServiceHandle(service);
      CloseServiceHandle(manager);
	  die("Can't remove %s as an NT service\n%d: %s", name, error, g_error);
    }

    CloseServiceHandle(service);
    CloseServiceHandle(manager);
}
