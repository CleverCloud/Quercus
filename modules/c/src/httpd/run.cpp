/*
 * Copyright (c) 1999-2008 Caucho Technology.  All rights reserved.
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
 *
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

#include <windows.h>
#include <winsock.h>
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

#define BUF_SIZE (32 * 1024)

static int g_is_dead;
static int g_is_started;
static int g_keepalive_handle = -1;
static HANDLE g_mutex;
static HWND g_window;
static int g_is_service;
static int g_is_standalone;
static int g_console = 0;
static PROCESS_INFORMATION g_procInfo_buf;
static PROCESS_INFORMATION *g_procInfo;

void
set_standalone(int is_standalone)
{
	g_is_standalone = is_standalone;
}

int
resin_is_service()
{
	return g_is_service;
}

static void
write_event_log(char *msg)
{

}

void
die(char *msg, ...)
{
	va_list args;
	char buf[8192];

	va_start(args, msg);
	vsprintf(buf, msg, args);
	va_end(args);

	if (0 && resin_is_service()) {
		write_event_log(buf);
	}
	else if (err) {
		fprintf(err, "%s\n", buf);
		fflush(err);
	}
	else {
		FILE *file = fopen("e:/temp/foo.log", "a+");
		if (file) {
			fprintf(file, "%s\n", buf);
			fclose(file);
		}
	}

	MessageBox(g_window, buf, "Fatal Error", MB_OK);
	exit(1);
}

void set_window(HWND window)
{
	g_window = window;
}

void
start_server()
{
	g_is_started = 1;
}

// XXX: potential sync issues.
void
stop_server()
{
	g_is_started = 0;
	int handle = g_keepalive_handle;

	g_keepalive_handle = -1;
	if (handle >= 0)
		closesocket(handle);

	fprintf(stderr, "Stopping Resin\n");
	if (g_procInfo) {
		WaitForSingleObject(g_procInfo_buf.hProcess, 5000);
		TerminateProcess(g_procInfo_buf.hProcess, 1);
	}

}

void
quit_server()
{
	g_is_dead = 1;
	stop_server();
}

int
exec_java(char *exe, char **args)
{
	return _spawnv(_P_WAIT, exe, args);
}

BOOL WINAPI handler(DWORD ctrlType)
{
	return 1;
}


int
spawn_java(char *exe, char **args)
{
	g_is_started = 1;

	char cmd[32 * 1024];

	char arg[32 * 1024];
	arg[0] = 0;
	for (int i = 0; args[i]; i++) {
		if (i > 0) {
			strcat(arg, " ");
			add_path(arg, args[i]);
		}
		else
			strcat(arg, args[i]);
	}

    WORD wVersionRequested = MAKEWORD(1,1); 
	WSADATA wsaData;
    if (WSAStartup(wVersionRequested, &wsaData) != 0)
        return 1; 

	while (! g_is_dead) {
		while (! g_is_started) {
			Sleep(1000);
		}
		
		STARTUPINFO startInfo;
		memset(&startInfo, 0, sizeof(startInfo));
		memset(&g_procInfo_buf, 0, sizeof(g_procInfo_buf));	
		startInfo.cb = sizeof(startInfo);

		char childarg[32 * 1024];
		char portname[32];

		strcpy(childarg, arg);

		int flag = 0;
		if (g_is_service) {
			flag = 0; // DETACHED_PROCESS;
			SetConsoleCtrlHandler(handler, TRUE);
		}

		int ok = CreateProcess(0, childarg, 0, 0, FALSE, flag, 
					           0, 0, 
							   &startInfo,
							   &g_procInfo_buf);

		if (! ok) {
			//closesocket(sock);
			fprintf(stderr, "Couldn't start %s %s.\n", cmd, childarg);
			return 1;
		}
		g_procInfo = &g_procInfo_buf;

		//g_keepalive_handle = sock;
		WaitForSingleObject(g_procInfo_buf.hProcess, INFINITE);
		DWORD status;
		GetExitCodeProcess(g_procInfo_buf.hProcess, &status);
		if (g_keepalive_handle >= 0)
			closesocket(g_keepalive_handle);
		g_keepalive_handle = -1;
		CloseHandle(g_procInfo_buf.hThread);
		CloseHandle(g_procInfo_buf.hProcess);

		g_procInfo = 0;

		if (status == 66 || g_is_standalone)
			return status;
                
		if (g_is_started || g_is_service) {
			Sleep(5000);
		}
	}

	return 0;
}

static void
usage(char *name)
{
  die("usage: %s [flags]\n"
	  "  -h                 : this help\n"
	  "  -verbose           : information on launching java\n"
	  "  -java_home <dir>   : sets the JAVA_HOME\n"
	  "  -java_exe <path>   : path to java executable\n"
	  "  -resin_home <dir>  : home of Resin\n"
	  "  -classpath <dir>   : java classpath\n"
	  "  -Jxxx              : JVM arg xxx\n"
	  "  -J-Dfoo=bar        : Set JVM variable\n"
	  "  -Xxxx              : JVM -X parameter\n"
	  "  -install           : install as NT service\n"
	  "  -install-as <name> : install as a named NT service\n"
	  "  -remove            : remove as NT service\n"
	  "  -remove-as <name>  : remove as a named NT service\n"
	  "  -user <name>       : specify username for NT service\n"
	  "  -password <pwd>    : specify password for NT\n"
          "  -conf <resin.conf> : alternate configuration file\n",
      name);
}

static char **
set_jdk_args(char *exe, char *cp,
	     char *resin_home, char *server_root, int jit,
	     char *main, int argc, char **argv, char **java_argv)
{
	char buf[BUF_SIZE];
	int j;

	char **args;
	int i = 0;
	int arg_count = argc;

	for (j = 0; java_argv[j]; j++)
	  arg_count++;

	args = (char **) malloc((arg_count + 16) * (sizeof (char *)));
	
	args[i++] = strdup(rsprintf(buf, "\"%s\"", exe));
	for (j = 0; java_argv[j]; j++)
          args[i++] = strdup(rsprintf(buf, "\"%s\"", java_argv[j]));
	/*
	args[i++] = "-classpath";
        int k = 0;
        buf[k++] = '\"';
        for (j = 0; cp[j]; j++) {
          if (cp[j] != '"')
              buf[k++] = cp[j];
        }
        buf[k++] = '\"';
        buf[k++] = 0;
	args[i++] = strdup(buf);
	*/
	/*
	args[i++] = strdup(rsprintf(buf, "-Dresin.home=\"%s\"", resin_home));
	args[i++] = strdup(rsprintf(buf, "-Dresin.root=\"%s\"", server_root));
	args[i++] = strdup(rsprintf(buf, "-Dserver.root=\"%s\"", server_root));
	*/
	if (! jit) {
		//args[i++] = "-nojit";
		args[i++] = strdup(rsprintf(buf, "-Djava.compiler=NONE"));
	}
	/*
        args[i++] = "-Djava.util.logging.manager=com.caucho.log.LogManagerImpl";
        args[i++] = "-Djavax.management.builder.initial=com.caucho.jmx.MBeanServerBuilderImpl";
		args[i++] = "-Djava.system.class.loader=com.caucho.loader.SystemClassLoader";
		*/
	// args[i++] = main;
	args[i++] = "-Xrs";
	args[i++] = "-jar";
	args[i++] = strdup(rsprintf(buf, "\"%s/lib/resin.jar\"", resin_home));

	args[i++] = "--resin-home";
	args[i++] = strdup(rsprintf(buf, "\"%s\"", resin_home));
	args[i++] = "--root-directory";
	args[i++] = strdup(rsprintf(buf, "\"%s\"", server_root));

	while (argc > 0) {
		args[i++] = strdup(rsprintf(buf, "\"%s\"", argv[0]));
		argc--;
		argv++;
	}
	args[i] = 0;

	return args;
}

static char **
set_ms_args(char *exe, char *cp, char *server_root, int jit, char *main, int argc, char **argv,
			char **java_argv)
{
	char buf[BUF_SIZE];

	char **args = (char **) malloc((12 + argc) * (sizeof (char *)));
	int i = 0;
	args[i++] = strdup(rsprintf(buf, "\"%s\"", exe));
	args[i++] = "/cp:a";
	args[i++] = strdup(rsprintf(buf, "\"%s\"", cp)); 
	args[i++] = strdup(rsprintf(buf, "\"/d:resin.home=%s\"", server_root));
	if (! jit) {
		args[i++] = strdup(rsprintf(buf, "/d:java.compiler=NONE"));
	}
	for (int j = 0; java_argv[j]; j++)
		args[i++] = strdup(rsprintf(buf, "\"%s\"", java_argv[j]));
	args[i++] = main;

	while (argc > 0) {
		args[i++] = strdup(rsprintf(buf, "\"%s\"", argv[0]));
		argc--;
		argv++;
	}
	args[i] = 0;

	return args;
}

char **
get_server_args(char *name, char *full_name, char *main, int argc, char **argv)
{
	char buf[BUF_SIZE];
	char program[BUF_SIZE];

	char *java_exe = 0;
	char *java_home = 0;
	char *resin_home = 0;
	char *server_root = 0;
	char *stdout_file = 0;
	char *stderr_file = 0;
	char *jvm_file = 0;
	char *cp = 0;
	int verbose = 0; // XXX: if create console, this shouldn't be on by default
	int msjava = 0;
	int jit = 1;
	char **java_argv = (char **) malloc((argc + 12) * sizeof(char *));
	int java_argc = 0;
	char **resin_argv = (char **) malloc((argc + 12) * sizeof(char *));
	int resin_argc = 0;
	char *env_classpath = getenv("CLASSPATH");
	char **initial_argv = argv;
	char *user = 0;
	char *password = 0;
	int is_install = 0;

	if (! GetModuleFileName(NULL, program, sizeof(program))) {
		die("Can't get module executable");
		return 0;
	}

	java_argv[0] = 0;
	resin_argv[0] = 0;
	while (argc > 1) {
		if (! strcmp(argv[1], "-verbose")) {
			argc--;
			argv++;
			verbose = 1;
			/*
		} else if (! strncmp(argv[1], "-J", 2)) {
		    java_argv[java_argc++] = strdup(argv[1] + 2);
			argc--;
			argv++;
			*/
		} else if (! strncmp(argv[1], "-D", 2)) {
		    java_argv[java_argc++] = strdup(argv[1]);
			argc--;
			argv++;
		} else if (! strncmp(argv[1], "-X", 2)) {
		    java_argv[java_argc++] = strdup(argv[1]);
			argc--;
			argv++;
		} else if (! strncmp(argv[1], "-E", 2)) {
			putenv(strdup(argv[1] + 2));
			argc--;
			argv++;
		} else if (! strcmp(argv[1], "-user")) {
			user = argv[2];
			argc -= 2;
			argv += 2;
		} else if (! strcmp(argv[1], "-password")) {
			password = argv[2];
			argc -= 2;
			argv += 2;
		} else if (! strcmp(argv[1], "-name")) {
			name = argv[2];
			argc -= 2;
			argv += 2;
		} else if (! strcmp(argv[1], "-display-name")) {
			full_name = argv[2];
			argc -= 2;
			argv += 2;
		} else if (! strcmp(argv[1], "-install")) {
			is_install = 1;
			argv++;
			argc--;
		} else if (! strcmp(argv[1], "-install-as") ||
				   ! strcmp(argv[1], "-install_as")) {
		   name = argv[2];
		   full_name = argv[2];
		   is_install = 1;
		   argv += 2;
		   argc -= 2;
		} else if (! strcmp(argv[1], "-remove")) {
			remove_service(name);
			/*
			sprintf(buf, "Removed %s as an NT service", name);
			MessageBox(0, buf, "Information", MB_OK);
			*/
			fprintf(stdout, "Removed %s as an NT service\n", name);
			exit(0);
		} else if (! strcmp(argv[1], "-remove-as") ||
				   ! strcmp(argv[1], "-remove_as")) {
			remove_service(argv[2]);
			/*
			sprintf(buf, "Removed %s as an NT service", argv[2]);
			MessageBox(0, buf, "Information", MB_OK);
			*/
			fprintf(stdout, "Removed %s as an NT service\n", argv[2]);
			exit(0);
		} else if (! strcmp(argv[1], "-java_home") ||
				   ! strcmp(argv[1], "-java-home")) {
			java_home = strdup(argv[2]);
			argc -= 2;
			argv += 2;
		} else if (! strcmp(argv[1], "-java_exe") ||
			       ! strcmp(argv[1], "-java-exe")) {
			java_exe = strdup(argv[2]);
			argc -= 2;
			argv += 2;
		} else if (! strcmp(argv[1], "-msjava")) {
			msjava = 1;
			argc -= 1;
			argv += 1;
		} else if (! strcmp(argv[1], "-resin_home") ||
			       ! strcmp(argv[1], "-resin-home") ||
				   ! strcmp(argv[1], "--resin-home")) {
			resin_home = strdup(argv[2]);
			argc -= 2;
			argv += 2;
		} else if (! strcmp(argv[1], "-server_root") ||
			   ! strcmp(argv[1], "-server-root") ||
			   ! strcmp(argv[1], "--root-directory")) {
			server_root = strdup(argv[2]);
			argc -= 2;
			argv += 2;
		} else if (! strcmp(argv[1], "-classpath") || ! strcmp(argv[1], "-cp")) {
			cp = add_classpath(cp, argv[2]);
			argc -= 2;
			argv += 2;
		} else if (! strcmp(argv[1], "-env-classpath")) {
			env_classpath = argv[2];
			argc -= 2;
			argv += 2;
		} else if (! strcmp(argv[1], "-stdout")) {
			stdout_file = argv[2];
			argc -= 2;
			argv += 2;
		} else if (! strcmp(argv[1], "-stderr")) {
			stderr_file = argv[2];
			argc -= 2;
			argv += 2;
		} else if (! strcmp(argv[1], "-jvm-log")) {
			jvm_file = argv[2];
			argc -= 2;
			argv += 2;
		} else if (! strcmp(argv[1], "-main")) {
			main = strdup(argv[2]);
			argc -= 2;
			argv += 2;
		} else if (! strcmp(argv[1], "-help") ||
			   ! strcmp(argv[1], "-h")) {
		        usage(name);
			exit(0);
		} else if (! strcmp(argv[1], "-service")) {
			argc -= 1;
			argv += 1;
			g_is_service = 1;
		} else if (! strcmp(argv[1], "-console")) {
			argc -= 1;
			argv += 1;
			g_console = 1;
		} else if (! strcmp(argv[1], "-nojit")) {
			jit = 0;
			argc -= 1;
			argv += 1;
		} else if (! strcmp(argv[1], "-standalone")) {
			g_is_standalone = 1;
			argc -= 1;
			argv += 1;
		} else if (! strcmp(argv[1], "-e") || ! strcmp(argv[1], "-compile")) {
			resin_argv[resin_argc++] = argv[1];
			g_is_standalone = 1;
			while (argc > 1) {
				resin_argv[resin_argc++] = argv[1];
				argc -= 1;
				argv += 1;
			}
		} else if (! strcmp(argv[1], "start")
				   || ! strcmp(argv[1], "stop")
				   || ! strcmp(argv[1], "restart")
				   || ! strcmp(argv[1], "shutdown")) {
		   resin_argv[resin_argc++] = argv[1];
		   argc -= 1;
		   argv += 1;
		   main = "com.caucho.boot.ResinBoot";

		   g_is_standalone = 1;
		   g_console = 0;
		} else {
			resin_argv[resin_argc++] = argv[1];
			argc -= 1;
			argv += 1;
		}
	}

	if (is_install) {
			install_service(name, full_name, user, password, initial_argv);
			fprintf(stdout, "Installed %s as an NT service\n", name);
			/*
			sprintf(buf, "Installed %s as an NT service", name);
			MessageBox(0, buf, "Information", MB_OK);
			*/
			exit(0);
	}

	java_argv[java_argc] = 0;
	resin_argv[resin_argc] = 0;

	resin_home = get_resin_home(resin_home, program);
	if (! resin_home) {
		die("Can't find RESIN_HOME");
		return 0;
	}

	if (! server_root)
		server_root = resin_home;

	if (stderr_file && ! stdout_file)
		stdout_file = stderr_file;
	if (stdout_file && ! stderr_file)
		stderr_file = stdout_file;
	if (! g_is_standalone) {
		AllocConsole();
	}

	if (false && ! g_console && stderr_file) {
		CreateDirectory(rsprintf(buf, "%s/log", server_root), NULL);
		SECURITY_ATTRIBUTES security;
		memset(&security, 0, sizeof(security));
		security.nLength = sizeof(security);
		security.lpSecurityDescriptor = 0;
		security.bInheritHandle = TRUE;

	    char *file_name;
		
		if (! jvm_file || ! *jvm_file)
			file_name = rsprintf(buf, "%s/log/jvm.log", server_root);
		else
			file_name = rsprintf(buf, "%s/%s", server_root, jvm_file);
		HANDLE errfile = CreateFile(file_name, GENERIC_WRITE, 
			                      FILE_SHARE_READ|FILE_SHARE_WRITE,
								  &security,
								  OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL, 0);
		SetStdHandle(STD_ERROR_HANDLE, errfile);
		SetFilePointer(errfile, 0, 0, FILE_END);
		HANDLE outfile = errfile;
		SetStdHandle(STD_OUTPUT_HANDLE, outfile);
		SetFilePointer(outfile, 0, 0, FILE_END);
	}
	
	int fdOut = _open_osfhandle((long) GetStdHandle(STD_OUTPUT_HANDLE), _O_TEXT);
	int fdErr = _open_osfhandle((long) GetStdHandle(STD_ERROR_HANDLE), _O_TEXT);
	if (fdOut >= 0)
		out = fdopen(fdOut, "w");
	if (fdErr >= 0)
		err = fdopen(fdErr, "w");

	if (out && err) {
  	  *stdout = *out;
	  *stderr = *err;
	
	  setvbuf(out, NULL, _IONBF, 0);
	  setvbuf(err, NULL, _IONBF, 0);
	}

	if (msjava)
		java_home = 0;
	else
		java_home = get_java_home(resin_home, java_home);

	cp = set_classpath(cp, resin_home, java_home, env_classpath);
	SetEnvironmentVariable("CLASSPATH", cp);

	char **args = 0;

	if (java_home) {
		if (! java_exe)
			java_exe = get_java_exe(java_home);

		args = set_jdk_args(java_exe, cp, resin_home, server_root, jit, main, resin_argc, resin_argv, java_argv);
	}
	else {
		if (! java_exe)
			java_exe = "jview.exe";
		args = set_ms_args(java_exe, cp, server_root, jit, main, resin_argc, resin_argv, java_argv);
	}

	sprintf(buf, "PATH=%s;%s\\bin;%s\\win32;%s\\win64;\\openssl\\bin", 
		    getenv("PATH"), resin_home, resin_home, resin_home);
	putenv(buf);

	if (! SetCurrentDirectory(server_root)) {
		die("Can't change dir to %s\n", server_root);
		return 0;
	}

	if (verbose) {
		fprintf(stdout, "java:        %s\n", java_exe);
		fprintf(stdout, "JAVA_HOME:   %s\n", java_home);
		fprintf(stdout, "RESIN_HOME:  %s\n", resin_home);
		fprintf(stdout, "SERVER_ROOT: %s\n", server_root);
		fprintf(stdout, "CLASSPATH:   %s\n", cp);
		fprintf(stdout, "PATH:        %s\n", getenv("PATH"));
		for (int i = 0; args[i]; i++)
			fprintf(stdout, "arg %d:      %s\n", i, args[i]);
	}

	//fflush(out)
	//fflush(err);

	if (g_is_standalone) {
		int result = exec_java(java_exe, args);

		if (1 || result) {
			log("service %d\n", g_is_service);
			log("exec %s (status %d)\n", java_exe, result);
			for (int i = 0; args[i]; i++)
				log("  arg-%d: %s\n", i, args[i]);
		}
		if (! g_is_service)
		  exit(result);

		return 0;
	}

	return args;
}
