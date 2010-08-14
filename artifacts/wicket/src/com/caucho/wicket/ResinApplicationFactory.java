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

package com.caucho.wicket;

import com.caucho.config.*;
import com.caucho.util.*;
import com.caucho.config.inject.*;

import org.apache.wicket.protocol.http.*;

/**
 * Factory for creating wicket application objects with Resin injection.
 */
public class ResinApplicationFactory implements IWebApplicationFactory
{
  private static final L10N L = new L10N(ResinApplicationFactory.class);
  
  private static final String APP_NAME = "applicationClassName";
  
  /**
   * Create a new instance of the <code>WebApplication</code> with
   * WebBeans enabled.
   */
  public WebApplication createApplication(WicketFilter filter)
  {
    String className
      = filter.getFilterConfig().getInitParameter(APP_NAME);

    if (className == null)
      throw new ConfigException(L.l("filter does not define '{0}'",
				    APP_NAME));

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      Class cl = Class.forName(className, false, loader);

      InjectManager webBeans = InjectManager.create();

      return (WebApplication) webBeans.getReference(cl);
    } catch (ConfigException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigException(L.l("Can't load application class '{0}'\n{1}",
				    className, e, e));
    }
  }
}
