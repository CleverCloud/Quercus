/*
 * Copyright (c) 1999-2001 Caucho Technology.  All rights reserved.
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
 * $Id: iis.h,v 1.3 2004/12/17 18:29:04 cvs Exp $
 */

/*
 *  Anh LE(ngoc-anh.le@centraliens.net) : 
 *  IIS5.0 allows ISAPI extension to use IIS authentification 
 *  by introducing SF_NOTIFY_AUTH_COMPLETE event notification for ISAPI filter
 *  So we will replace SF_NOTIFY_PREPROC_HEADERS by SF_NOTIFY_AUTH_COMPLETE
 *  for IIS5.0/Windows2000.
 *  Windows 2000 SP1 is strongly recommended because of a bug with this event 
 */

#ifndef _CAUCHO_IIS_H
#define _CAUCHO_IIS_H

// Anh : 
// Only httpfilt.h in the Windows 2000 SDK include the SF_NOTIFY_AUTH_COMPLETE definition

#ifndef SF_NOTIFY_AUTH_COMPLETE
#define SF_NOTIFY_AUTH_COMPLETE             0x04000000
typedef struct _HTTP_FILTER_AUTH_COMPLETE_INFO
{
    // 
    //  For SF_NOTIFY_AUTH_COMPLETE, retrieves the specified header value.
    //  Header names should include the trailing ':'.  The special values
    //  'method', 'url' and 'version' can be used to retrieve the individual
    //  portions of the request line
    // 

    BOOL (WINAPI * GetHeader) (
        struct _HTTP_FILTER_CONTEXT * pfc,
        LPSTR                         lpszName,
        LPVOID                        lpvBuffer,
        LPDWORD                       lpdwSize
        );

    // 
    //  Replaces this header value to the specified value.  To delete a header,
    //  specified a value of '\0'.
    // 

    BOOL (WINAPI * SetHeader) (
        struct _HTTP_FILTER_CONTEXT * pfc,
        LPSTR                         lpszName,
        LPSTR                         lpszValue
        );

    // 
    //  Adds the specified header and value
    // 

    BOOL (WINAPI * AddHeader) (
        struct _HTTP_FILTER_CONTEXT * pfc,
        LPSTR                         lpszName,
        LPSTR                         lpszValue
        );
        
    // 
    //  Get the authenticated user impersonation token
    // 
    
    BOOL (WINAPI * GetUserToken) (
        struct _HTTP_FILTER_CONTEXT * pfc,
        HANDLE *                      phToken
        );
    
    // 
    //  Status code to use when sending response
    // 
    
    DWORD HttpStatus;               
    
    // 
    //  Determines whether to reset auth if URL changed
    // 
    
    BOOL  fResetAuth;             
    
    // 
    //  Reserved
    // 
    
    DWORD dwReserved;            
    
} HTTP_FILTER_AUTH_COMPLETE_INFO, *PHTTP_FILTER_AUTH_COMPLETE_INFO; 
#endif
// End Anh

#endif
