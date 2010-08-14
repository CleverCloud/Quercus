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

package com.caucho.server.http;

import java.io.IOException;

import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.caucho.server.webapp.WebApp;
import com.caucho.vfs.ReadStream;

public interface CauchoRequest extends HttpServletRequest {
  public String getPageURI();
  public String getPageContextPath();
  public String getPageServletPath();
  public String getPagePathInfo();
  public String getPageQueryString();

  public WebApp getWebApp();
  
  public ReadStream getStream() throws IOException;
  public int getRequestDepth(int depth);
  public void setHeader(String key, String value);
  public boolean getVaryCookies();
  public void setVaryCookie(String cookie);
  public boolean getHasCookie();

  public void setSyntheticCacheHeader(boolean isTop);
  public boolean isSyntheticCacheHeader();
  
  public boolean isTop();

  public boolean hasRequest();
  
  public String getSessionId();
  public void setSessionId(String sessionId);
  public boolean isSessionIdFromCookie();
  public HttpSession getMemorySession();
  public Cookie getCookie(String name);
  public void setHasCookie();
  public void killKeepalive();
  public boolean isSuspend(); // XXX: isComplete()?
  public boolean isComet();
  public boolean isDuplex();
  public boolean isKeepaliveAllowed();
  public boolean isClientDisconnect();
  public void clientDisconnect();

  public boolean isLoginRequested();
  public void requestLogin();
  public boolean login(boolean isFail);

  // public HashMap<String,String> setRoleMap(HashMap<String,String> roleMap);

  public ServletResponse getServletResponse();
  public AbstractHttpRequest getAbstractHttpRequest();
}
