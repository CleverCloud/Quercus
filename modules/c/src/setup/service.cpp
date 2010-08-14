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
 * $Id: service.cpp,v 1.2 2004/09/29 16:58:24 cvs Exp $
 */

#include <windows.h>
#include <stdio.h>
#include "setup.h"

char * 
stop_service(char *name)
{
    SC_HANDLE service;
    SC_HANDLE manager;
    SERVICE_STATUS status;

    manager = OpenSCManager(
                        NULL,                   // machine (NULL == local)
                        NULL,                   // database (NULL == default)
                        SC_MANAGER_ALL_ACCESS   // access required
                        );
    if (! manager)
	     return "Can't open service manager";
    service = OpenService(manager, name, SERVICE_STOP|STANDARD_RIGHTS_REQUIRED);

    if (service == NULL) {
		char buf[1024];

		sprintf(buf, "Can't stop %s", name);
		CloseServiceHandle(manager);
		return strdup(buf);
	}
 
    // try to stop the service
    if (ControlService(service, SERVICE_CONTROL_STOP, &status)) {
		Sleep(1000);
		for (int i = 0; 
			 i < 10 && QueryServiceStatus(service, &status) && 
				 status.dwCurrentState == SERVICE_STOP_PENDING;
			 i++)
			Sleep(1000);
    }

    CloseServiceHandle(service);
	CloseServiceHandle(manager);

	return 0;
}

char * 
start_service(char *name)
{
    SC_HANDLE service;
    SC_HANDLE manager;
 
    manager = OpenSCManager(
                        NULL,                   // machine (NULL == local)
                        NULL,                   // database (NULL == default)
                        SC_MANAGER_ALL_ACCESS   // access required
                        );
    if (! manager)
	     return "Can't open service manager";

    service = OpenService(manager, name, SERVICE_ALL_ACCESS);

    if (service == NULL) {
		CloseServiceHandle(manager);
		return "Can't open service";
	}
 
    // try to start the service
    StartService(service, 0, 0);

    CloseServiceHandle(service);
    CloseServiceHandle(manager);

	return 0;
}
