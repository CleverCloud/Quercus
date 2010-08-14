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
import javax.servlet.http.HttpServletResponse;

import com.caucho.server.cache.AbstractCacheEntry;
import com.caucho.server.cache.AbstractCacheFilterChain;

public interface CauchoResponse extends HttpServletResponse {
  /* caucho methods */
  public AbstractResponseStream getResponseStream();
  public void setResponseStream(AbstractResponseStream os);

  public boolean isCauchoResponseStream();
  public String getHeader(String key);
  
  public void setFooter(String key, String value);
  public void addFooter(String key, String value);
  
  public void close() throws IOException;

  // to support the JSP getRemaining
  //  public int getRemaining();

  public boolean getForbidForward();
  public void setForbidForward(boolean forbid);

  public String getStatusMessage();

  public boolean hasError();
  public void setHasError(boolean error);

  public void setSessionId(String id);

  public void killCache();
  public void setNoCache(boolean killCache);
  public void setPrivateCache(boolean isPrivate);
  public void setCacheInvocation(AbstractCacheFilterChain cacheFilterChain);
  public void setMatchCacheEntry(AbstractCacheEntry matchCacheEntry);
  public boolean isNoCacheUnlessVary();

  public AbstractHttpResponse getAbstractHttpResponse();
  /**
   * Return wrapped response
   */
  public ServletResponse getResponse();

  public void setForwardEnclosed(boolean isForwardEnclosed);

  public boolean isForwardEnclosed();
}
