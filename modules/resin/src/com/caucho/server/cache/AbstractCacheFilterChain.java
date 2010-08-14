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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.cache;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.FilterChain;

import com.caucho.server.http.CauchoRequest;
import com.caucho.server.http.CauchoResponse;

/**
 * Represents the final servlet in a filter chain.
 */
abstract public class AbstractCacheFilterChain
  implements FilterChain {
  /**
   * fillFromCache is called when the client needs the entire result, and
   * the result is already in the cache.
   *
   * @param req the servlet request trying to get data from the cache
   * @param response the servlet response which will receive data
   * @param entry the cache entry to use
   */
  abstract public boolean fillFromCache(CauchoRequest req,
                                        CauchoResponse response,
                                        AbstractCacheEntry abstractEntry)
    throws IOException;

  /**
   * Starts the caching after the headers have been sent.
   *
   * @param req the servlet request
   * @param req the servlet response
   * @param keys the saved header keys
   * @param values the saved header values
   * @param contentType the response content type
   * @param charEncoding the response character encoding
   *
   * @return the output stream to store the cache value or null if
   *         uncacheable.
   */
  abstract public AbstractCacheEntry startCaching(CauchoRequest req,
                                                  CauchoResponse res,
                                                  ArrayList<String> keys,
                                                  ArrayList<String> values,
                                                  String contentType,
                                                  String charEncoding,
                                                  long contentLength);

  /**
   * Update the headers when the caching has finished.
   *
   * @param okay if true, the cache if valid
   */
  abstract public void finishCaching(AbstractCacheEntry entry);

  /**
   * Cleanup the cache entry on a failed cache attempt.
   */
  abstract public void killCaching(AbstractCacheEntry entry);
}
