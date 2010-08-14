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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

/*
 *  Anh LE(ngoc-anh.le@centraliens.net) : 
 *  IIS5.0 allows ISAPI extension to use IIS authentification 
 *  by introducing SF_NOTIFY_AUTH_COMPLETE event notification for ISAPI filter
 *  So we will replace SF_NOTIFY_PREPROC_HEADERS by SF_NOTIFY_AUTH_COMPLETE
 *  for IIS5.0/Windows2000.
 *  Windows 2000 SP1 is strongly recommended because of a bug with this event 
 */
#include <windows.h>
#include <stdio.h>
#include <httpext.h>
#include <httpfilt.h>
#include <tchar.h>
#include "isapi_srun.h"
#include "../httpd/common.h"
#include "iis.h"
extern "C" {
#include "../common/cse.h"
}

#define ISAPI_SCRIPT "/scripts/isapi_srun.dll"

#define URL_SIZE 8192

// the script url size needs to be larger to handle the /script prefix
#define SCRIPT_URL_SIZE (URL_SIZE + 1024)

extern int cse_handle_request(config_t *, EXTENSION_CONTROL_BLOCK *);
extern void log(char *fmt, ...);

static config_t *g_config;

static HINSTANCE g_hInstance;
static int g_is_iis5 = -1;
static char *g_dll_name;

void die(char *msg, ...) {}

static HINSTANCE AfxGetResourceHandle()
{
	return g_hInstance;
}

static int
parseResinIni(FILE *file, config_t *config)
{
	char key[1024];
	int has_host = 0;

	while (! feof(file)) {
		int ch;
		int i = 0;

		for (ch = fgetc(file); ch > 0 && ! isspace(ch); ch = fgetc(file))
			key[i++] = ch;
		key[i] = 0;
			
		for (; ch == ' ' || ch == '\t'; ch = fgetc(file)) {
		}

		if (! strcmp(key, "ResinConfigServer")) {
		  char host[1024];
		  int port;
		  
		  i = 0;
		  for (; ch > 0 && ! isspace(ch); ch = fgetc(file)) {
		    host[i++] = ch;
		  }
		  host[i] = 0;

		  for (; ch == ' ' || ch == '\t'; ch = fgetc(file)) {
		  }
		  
		  port = 0;
		  for (; ch >= '0' && ch <= '9'; ch = fgetc(file))
			port = port * 10 + ch - '0';

		  if (port == 0)
		    port = 6800;

		  /*
		  cse_add_host(&config->config_cluster, host, port);
		  */
		  cse_add_config_server((mem_pool_t *) config->p, config, host, port);

		  has_host = 1;
		}
		else if (! strcmp(key, "IISPriority")) {
		  char priority[1024];
		  
		  i = 0;
		  for (; ch > 0 && ch != ' ' && ch != '\t' && ch != '\n';
		       ch = fgetc(file))
			priority[i++] = ch;
		  priority[i] = 0;

		  config->iis_priority = strdup(priority);
		}
		else if (! strcmp(key, "OverrideIISAuthentication")) {
		  char status[1024];
		  
		  i = 0;
		  for (; ch > 0 && ch != ' ' && ch != '\t' && ch != '\n';
		       ch = fgetc(file))
			status[i++] = ch;
		  status[i] = 0;

		  if (! strcmp(status, "yes") || ! strcmp(status, "true"))
		    config->override_iis_authentication = 1;
		}
		else if (! strcmp(key, "CauchoStatus")) {
		  char status[1024];
		  
		  i = 0;
		  for (; ch > 0 && ch != ' ' && ch != '\t' && ch != '\n';
		       ch = fgetc(file))
			status[i++] = ch;
		  status[i] = 0;

		  if (! strcmp(status, "yes") || ! strcmp(status, "true"))
		    config->enable_caucho_status = 1;
		}
		
		for (; ch > 0 && ch != '\n'; ch = fgetc(file)) {
		}
	}

	return has_host;
}

static void
findResinIni(char *pwd, config_t *config)
{
	char resinIni[1024];

	sprintf(resinIni, "%s/resin.ini", pwd);

	LOG(("%s as resinIni\n", resinIni));

	FILE *file = fopen(resinIni, "r");

	if (file) {
		int found = parseResinIni(file, config);
		fclose(file);
		if (found)
			return;
	}
	else
	  config->enable_caucho_status = 1;

	cse_add_config_server((mem_pool_t *) config->p, config, "localhost", 6800);
}

BOOL WINAPI GetExtensionVersion(HSE_VERSION_INFO* pVer)
{
	LOG(("start extension\n"));

	// Load description string
	TCHAR sz[HSE_MAX_EXT_DLL_NAME_LEN+1];
	LoadString(AfxGetResourceHandle(), IDS_SERVER, sz, HSE_MAX_EXT_DLL_NAME_LEN);
	_tcscpy(pVer->lpszExtensionDesc, sz);

	char dllBuffer[1024];
	char *dllName = dllBuffer;
	GetModuleFileName(AfxGetResourceHandle(), dllName, sizeof(dllBuffer));
	int i;

	if (g_config)
		return TRUE;

	// find DLL directory
	for (i = strlen(dllName) - 1; i >= 0 && dllName[i] != '/' && dllName[i] != '\\'; i--) {
	}
/*
	for (i--; i >= 0 && dllName[i] != '/' && dllName[i] != '\\'; i--) {
	}
*/
	if (i < 0)
		i = 0;
	dllName[i] = 0;
	if (! strncmp(dllName, "\\\\?\\", 4))
		dllName += 4;
	g_config = (config_t *) malloc(sizeof (config_t));
	memset(g_config, 0, sizeof(config_t));
	cse_init_config(g_config);

	findResinIni(dllName, g_config);
	cse_init_config(g_config);

	return TRUE;
}

///////////////////////////////////////////////////////////////////////
// CIis_srunExtension command handlers

DWORD WINAPI HttpExtensionProc(EXTENSION_CONTROL_BLOCK *pECB)
{
	if (cse_handle_request(g_config, pECB))
		return HSE_STATUS_SUCCESS;
	else
		return HSE_STATUS_ERROR;
}

BOOL WINAPI TerminateExtension(DWORD dwFlags)
{
	LOG(("terminate\n"));

	cse_close_sockets(g_config);

	return TRUE;
}

BOOL WINAPI TerminateFilter(DWORD dwFlags)
{
	LOG(("terminate filter\n"));

	cse_close_sockets(g_config);

	return TRUE;
}


static void 
log_event(char *msg)
{
    char **strings = (char **) malloc(2 * sizeof(char *));
	strings[0] = msg;
	strings[1] = 0;

	HANDLE handle = RegisterEventSource(0, "Application");
	if (! handle)
		return;

	ReportEvent(handle, EVENTLOG_INFORMATION_TYPE,
		      0, 1, 0, 1, 0, 
			  (const char **) strings, 0);

	DeregisterEventSource(handle);
}

BOOL WINAPI GetFilterVersion(HTTP_FILTER_VERSION *pVer)
{
	LOG(("start filter\n"));

	// log_event("start iis_srun filter");

	TCHAR sz[SF_MAX_FILTER_DESC_LEN+1];
	LoadString(AfxGetResourceHandle(), IDS_SERVER, sz, SF_MAX_FILTER_DESC_LEN);
	_tcscpy(pVer->lpszFilterDesc, sz);

	char dllName[1024];
	GetModuleFileName(AfxGetResourceHandle(), dllName, sizeof(dllName));
	int i;

	if (g_config)
		return TRUE;

	for (i = strlen(dllName) - 1; i >= 0 && dllName[i] != '/' && dllName[i] != '\\'; i--) {
	}

	if (i < 0)
		i = 0;
	dllName[i] = 0;

	g_config = (config_t *) malloc(sizeof (config_t));
	memset(g_config, 0, sizeof(config_t));

	cse_init_config(g_config);

	findResinIni(dllName, g_config);
	LOG(("loading %s as config %x\n", dllName, g_config));

	pVer->dwFilterVersion = HTTP_FILTER_REVISION;
	// Anh : Add SF_NOTIFY_AUTH_COMPLETE for IIS5.0
	pVer->dwFlags = (SF_NOTIFY_PREPROC_HEADERS
			 |SF_NOTIFY_LOG 
			 |SF_NOTIFY_AUTH_COMPLETE);
	if (g_config->override_iis_authentication)
		pVer->dwFlags |= SF_NOTIFY_AUTHENTICATION;


	if (! g_config->iis_priority)
		pVer->dwFlags |= SF_NOTIFY_ORDER_DEFAULT;
	else if (! stricmp(g_config->iis_priority, "low"))
		pVer->dwFlags |= SF_NOTIFY_ORDER_LOW;
	else if (! stricmp(g_config->iis_priority, "medium"))
		pVer->dwFlags |= SF_NOTIFY_ORDER_MEDIUM;
	else if (! stricmp(g_config->iis_priority, "high"))
		pVer->dwFlags |= SF_NOTIFY_ORDER_HIGH;
	else
		pVer->dwFlags |= SF_NOTIFY_ORDER_DEFAULT;

	return TRUE;
}

DWORD WINAPI HttpFilterProc(PHTTP_FILTER_CONTEXT pfc, DWORD notificationType,
			    LPVOID pvNotification)
{
	char url[URL_SIZE];
	char host[1024];
	char port_buf[80];
	int port = 0;
	unsigned long size;

	char query = 0;
	unsigned int query_index = 0;
	HTTP_FILTER_PREPROC_HEADERS *headers=NULL;;
	HTTP_FILTER_AUTH_COMPLETE_INFO *AuthComp=NULL;; 

	if (g_is_iis5 < 0) {
		char version[1024];
		size = sizeof(version);
		g_is_iis5 = 0;

		if (pfc->GetServerVariable(pfc, "SERVER_SOFTWARE", version, &size)) {
			LOG(("IIS version %s\n", version));

			g_is_iis5 = atof(version + strlen("Microsoft-IIS/")) >= 5.0;
		}
		else {
			LOG(("Can't Get SERVER_SOFTWARE %d\n",GetLastError()));
		}
	}
	
	switch (notificationType) {
	case SF_NOTIFY_PREPROC_HEADERS:
		LOG(("HttpFilterProc: SF_NOTIFY_PREPROC_HEADERS\n"));
		if (g_is_iis5)
		  break;
		
		headers = (HTTP_FILTER_PREPROC_HEADERS *) pvNotification;

		size = sizeof(host);
		host[0] = 0;
		pfc->GetServerVariable(pfc, "SERVER_NAME", host, &size);

		size = sizeof(port_buf);
		if (pfc->GetServerVariable(pfc, "SERVER_PORT", port_buf, &size) && size > 0) {
			port = atoi(port_buf);
		}

		size = sizeof(url);
		if (headers->GetHeader(pfc, "URL", url, &size) && size > 0) {
			url[size] = 0;
			for (query_index = 0;
			     query_index < size;
			     query_index++) {
				if (url[query_index] == '?') {
					query = url[query_index];
					url[query_index] = 0;
					break;
				}
			}

			DWORD request_time = GetTickCount() / 1000;

			if (cse_match_request(g_config, host, port, url, 1, request_time) ||
				g_config->enable_caucho_status &&
				! strcmp(url, "/caucho-status")) {
				char newurl[SCRIPT_URL_SIZE];

			if (! pfc->pFilterContext) {
				pfc->pFilterContext = pfc->AllocMem(pfc, SCRIPT_URL_SIZE, 0);
				if (! pfc->pFilterContext) {
				  SetLastError(ERROR_NOT_ENOUGH_MEMORY);
				  return SF_STATUS_REQ_ERROR;
				}
				((char *) pfc->pFilterContext)[0] = 0;
			}

				url[query_index] = query;
				strcpy(newurl, ISAPI_SCRIPT);
				strcat(newurl, url);
				headers->SetHeader(pfc, "URL", newurl);  
				strcpy((char *) pfc->pFilterContext, url);
				((char *) pfc->pFilterContext)[query_index] = 0;
			}
		}
      break;

	case SF_NOTIFY_AUTH_COMPLETE:
		LOG(("HttpFilterProc: SF_NOTIFY_AUTH_COMPLETE\n"));
		AuthComp = (HTTP_FILTER_AUTH_COMPLETE_INFO *) pvNotification;
		size = sizeof(host);
		host[0] = 0;
		pfc->GetServerVariable(pfc, "SERVER_NAME", host, &size);

		size = sizeof(port_buf);
		if (pfc->GetServerVariable(pfc, "SERVER_PORT", port_buf, &size) && size > 0) {
			port = atoi(port_buf);
		}


		size = sizeof(url);
		if (AuthComp->GetHeader(pfc, "URL", url, &size) && size > 0) {
			url[size] = 0;
			for (query_index = 0; query_index < size; query_index++) {
				if (url[query_index] == '?') {
					query = url[query_index];
					url[query_index] = 0;
					break;
				}
			}

         DWORD request_time = GetTickCount() / 1000;
 
		if (cse_match_request(g_config, host, port, url, 1, request_time) ||
			g_config->enable_caucho_status &&
			! strcmp(url, "/caucho-status")) {
			char newurl[SCRIPT_URL_SIZE];
			if (! pfc->pFilterContext) {
				pfc->pFilterContext = pfc->AllocMem(pfc, SCRIPT_URL_SIZE, 0);
				if (! pfc->pFilterContext) {
				  SetLastError(ERROR_NOT_ENOUGH_MEMORY);
				return SF_STATUS_REQ_ERROR;
				}
				((char *) pfc->pFilterContext)[0] = 0;
			}
	
				url[query_index] = query;
				strcpy(newurl, ISAPI_SCRIPT);
				strcat(newurl, url);
				AuthComp->SetHeader(pfc, "URL", newurl); 
				strcpy((char *) pfc->pFilterContext, url);
				((char *) pfc->pFilterContext)[query_index] = 0;
			}
		}
      break;
	
	case SF_NOTIFY_LOG:
		LOG(("NOTIFY-LOG %p\n", pfc->pFilterContext));
		if (pfc->pFilterContext && ((char *) pfc->pFilterContext)[0]) {
			char *pch = (char *) pfc->pFilterContext;
			LOG(("NOTIFY_LOG %s\n", pch ? pch : "null"));
			HTTP_FILTER_LOG *pLog = (HTTP_FILTER_LOG *) pvNotification;
			pLog->pszTarget = pch;
		}
		break;

	default:
		LOG(("Log %d\n", notificationType));
		break;
 	}

	return SF_STATUS_REQ_NEXT_NOTIFICATION;
}

BOOL WINAPI DllMain(HINSTANCE hInst, ULONG ulReason, LPVOID lpReserved)
{
	if (ulReason == DLL_PROCESS_ATTACH) {
		g_hInstance = hInst;
	}

	return TRUE;
}                 
