/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.servlets.ssi;

import com.caucho.VersionFactory;
import com.caucho.util.Alarm;
import com.caucho.util.IntMap;
import com.caucho.util.QDate;
import com.caucho.vfs.Path;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Represents a SSI variable
 */
public class VarExpr extends SSIExpr {
  private static final int ATTRIBUTE = 0;
  private static final int HTTP_ = ATTRIBUTE + 1;
  
  private static final int SERVER_SOFTWARE = HTTP_ + 1;
  private static final int SERVER_NAME = SERVER_SOFTWARE + 1;
  private static final int SERVER_ADDR = SERVER_NAME + 1;
  private static final int SERVER_PORT = SERVER_ADDR + 1;
  private static final int REMOTE_ADDR = SERVER_PORT + 1;
  private static final int REMOTE_PORT = REMOTE_ADDR + 1;
  private static final int REMOTE_USER = REMOTE_PORT + 1;
  private static final int AUTH_TYPE = REMOTE_USER + 1;
  private static final int GATEWAY_INTERFACE = AUTH_TYPE + 1;
  private static final int SERVER_PROTOCOL = GATEWAY_INTERFACE + 1;
  private static final int REQUEST_METHOD = SERVER_PROTOCOL + 1;
  private static final int QUERY_STRING = REQUEST_METHOD + 1;
  private static final int REQUEST_URI = QUERY_STRING + 1;
  private static final int SCRIPT_FILENAME = REQUEST_URI + 1;
  private static final int SCRIPT_NAME = SCRIPT_FILENAME + 1;
  private static final int PATH_INFO = SCRIPT_NAME + 1;
  private static final int PATH_TRANSLATED = PATH_INFO + 1;
  private static final int CONTENT_LENGTH = PATH_TRANSLATED + 1;
  private static final int CONTENT_TYPE = CONTENT_LENGTH + 1;
  
  private static final int DATE_GMT = CONTENT_TYPE + 1;
  private static final int DATE_LOCAL = DATE_GMT + 1;
  private static final int DOCUMENT_NAME = DATE_LOCAL + 1;
  private static final int DOCUMENT_URI = DOCUMENT_NAME + 1;
  private static final int LAST_MODIFIED = DOCUMENT_URI + 1;
  private static final int USER_NAME = LAST_MODIFIED + 1;

  private static final IntMap _varMap = new IntMap();
  
  private final int _code;
  
  private final String _var;

  private final Path _path;

  VarExpr(String var, Path path)
  {
    int code = _varMap.get(var.toLowerCase());

    if (code > 0) {
    }
    else if (var.startsWith("HTTP_")) {
      var = var.substring(5).replace('_', '-').toLowerCase();
      code = HTTP_;
    }
    else
      code = ATTRIBUTE;

    _code = code;
    _var = var;

    _path = path;
  }

  /**
   * Evaluate as a string.
   */
  public String evalString(HttpServletRequest request,
                           HttpServletResponse response)
  {
    String fmt;
    String value = null;
    Object attr = null;
    
    switch (_code) {
    case ATTRIBUTE:
      {
        attr = request.getParameter(_var);
        if (attr == null)
          attr = request.getAttribute(_var);
        value = String.valueOf(attr);
        break;
      }

    case HTTP_:
      value = request.getHeader(_var);
      break;

    case SERVER_SOFTWARE:
      value = "Resin/" + VersionFactory.getVersion();
      break;

    case SERVER_NAME:
    case SERVER_ADDR:
      value = request.getServerName();
      break;

    case SERVER_PORT:
      value = String.valueOf(request.getServerPort());
      break;

    case REMOTE_ADDR:
      value = request.getServerName();
      break;

    case REMOTE_PORT:
      value = String.valueOf(request.getServerPort());
      break;

    case REMOTE_USER:
      value = request.getRemoteUser();
      break;

    case AUTH_TYPE:
      value = request.getAuthType();
      break;

    case GATEWAY_INTERFACE:
      value = "CGI/1.1";
      break;

    case SERVER_PROTOCOL:
      value = request.getProtocol();
      break;

    case REQUEST_METHOD:
      value = request.getMethod();
      break;

    case QUERY_STRING:
      value = request.getQueryString();
      break;

    case REQUEST_URI:
      value = request.getRequestURI();
      break;

    case PATH_INFO:
      value = request.getPathInfo();
      break;

    case PATH_TRANSLATED:
      value = request.getRealPath(request.getPathInfo());
      break;

    case CONTENT_LENGTH:
      value = String.valueOf(request.getContentLength());
      break;

    case CONTENT_TYPE:
      value = request.getHeader("Content-Type");
      break;

    case DATE_GMT:
      fmt = (String) request.getAttribute("caucho.ssi.timefmt");
      if (fmt == null)
        fmt = "%Y-%m-%d %H:%M:%S";
      value = QDate.formatGMT(Alarm.getCurrentTime(), fmt);
      break;

    case DATE_LOCAL:
      fmt = (String) request.getAttribute("caucho.ssi.timefmt");
      if (fmt == null)
        fmt = "%Y-%m-%d %H:%M:%S";
      
      value = QDate.formatLocal(Alarm.getCurrentTime(), fmt);
      break;

    case DOCUMENT_NAME:
      value = _path.getTail();
      break;

    case DOCUMENT_URI:
      value = request.getRequestURI();
      break;

    case LAST_MODIFIED:
      fmt = (String) request.getAttribute("caucho.ssi.timefmt");
      if (fmt == null)
        fmt = "%Y-%m-%d %H:%M:%S";
      
      value = QDate.formatLocal(_path.getLastModified(), fmt);
      break;

    case USER_NAME:
    default:
      break;
    }

    if (value != null)
      return value;
    else
      return "(null)";
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _var + "]";
  }

  static {
    _varMap.put("server_software", SERVER_SOFTWARE);
    _varMap.put("server_name", SERVER_NAME);
    _varMap.put("server_addr", SERVER_ADDR);
    _varMap.put("server_port", SERVER_PORT);
    _varMap.put("remote_addr", REMOTE_ADDR);
    _varMap.put("remote_port", REMOTE_PORT);
    _varMap.put("remote_user", REMOTE_USER);
    _varMap.put("auth_type", AUTH_TYPE);
    _varMap.put("gateway_interface", GATEWAY_INTERFACE);
    _varMap.put("server_protocol", SERVER_PROTOCOL);
    _varMap.put("request_method", REQUEST_METHOD);
    _varMap.put("query_string", QUERY_STRING);
    _varMap.put("request_uri", REQUEST_URI);
    _varMap.put("script_filename", SCRIPT_FILENAME);
    _varMap.put("script_name", SCRIPT_NAME);
    _varMap.put("path_info", PATH_INFO);
    _varMap.put("path_translated", PATH_TRANSLATED);
    _varMap.put("content_length", CONTENT_LENGTH);
    _varMap.put("content_type", CONTENT_TYPE);
    
    _varMap.put("date_gmt", DATE_GMT);
    _varMap.put("date_local", DATE_LOCAL);
    _varMap.put("document_name", DOCUMENT_NAME);
    _varMap.put("document_uri", DOCUMENT_URI);
    _varMap.put("last_modified", LAST_MODIFIED);
    _varMap.put("user_name", USER_NAME);
  }
}
