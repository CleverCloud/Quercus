/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package javax.faces.context;

import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;
import java.security.*;

public abstract class ExternalContext {
  public static final String BASIC_AUTH = "BASIC";
  public static final String CLIENT_CERT_AUTH = "CLIENT_CERT";
  public static final String DIGEST_AUTH = "DIGEST";
  public static final String FORM_AUTH = "FORM";

  public abstract void dispatch(String path)
    throws IOException;

  public abstract String encodeActionURL(String url);

  public abstract String encodeNamespace(String name);

  public abstract String encodeResourceURL(String url);

  public abstract Map<String,Object> getApplicationMap();

  public abstract String getAuthType();

  public abstract Object getContext();

  public abstract String getInitParameter(String name);

  public abstract Map getInitParameterMap();

  public abstract String getRemoteUser();

  public abstract Object getRequest();

  /**
   * @Since 1.2
   */
  public void setRequest(Object request)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * @Since 1.2
   */
  public void setRequestCharacterEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public abstract String getRequestContextPath();

  public abstract Map<String,Object> getRequestCookieMap();

  public abstract Map<String,String> getRequestHeaderMap();

  public abstract Map<String,String[]> getRequestHeaderValuesMap();

  public abstract Locale getRequestLocale();

  public abstract Iterator<Locale> getRequestLocales();

  public abstract Map<String,Object> getRequestMap();

  public abstract Map<String,String> getRequestParameterMap();

  public abstract Iterator<String> getRequestParameterNames();

  public abstract Map<String,String[]> getRequestParameterValuesMap();

  public abstract String getRequestPathInfo();

  public abstract String getRequestServletPath();

  /**
   * @Since 1.2
   */
  public String getRequestCharacterEncoding()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * @Since 1.2
   */
  public String getRequestContentType()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * @Since 1.2
   */
  public String getResponseCharacterEncoding()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * @Since 1.2
   */
  public String getResponseContentType()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public abstract URL getResource(String path)
    throws MalformedURLException;

  public abstract InputStream getResourceAsStream(String path);

  public abstract Set<String> getResourcePaths(String path);

  public abstract Object getResponse();

  /**
   * @Since 1.2
   */
  public void setResponse(Object response)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * @Since 1.2
   */
  public void setResponseCharacterEncoding(String encoding)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public abstract Object getSession(boolean create);

  public abstract Map<String,Object> getSessionMap();

  public abstract Principal getUserPrincipal();

  public abstract boolean isUserInRole(String role);

  public abstract void log(String message);
  
  public abstract void log(String message, Throwable exn);

  public abstract void redirect(String url)
    throws IOException;
}
