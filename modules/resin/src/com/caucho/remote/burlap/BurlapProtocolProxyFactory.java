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

package com.caucho.remote.burlap;

import com.caucho.burlap.client.*;
import com.caucho.remote.*;
import com.caucho.remote.client.*;

import java.lang.annotation.Annotation;

/**
 * Burlap factory for creating remote-client proxies
 */
public class BurlapProtocolProxyFactory
  extends AbstractProtocolProxyFactory
{
  private BurlapProxyFactory _factory = new BurlapProxyFactory();

  private String _url;

  /**
   * Sets the proxy URL.
   */
  public void setURL(String url)
  {
    _url = url;
  }

  @Override
  public void setProxyType(Annotation ann)
  {
    BurlapClient client = (BurlapClient) ann;

    setURL(client.url());
  }
  
  /**
   * Creates a new proxy based on an API
   *
   * @param api the api exposed to the client
   */
  public Object createProxy(Class api)
  {
    try {
      return _factory.create(api, _url);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ServiceException(e);
    }
  }
}
