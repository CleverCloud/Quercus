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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */


package com.caucho.spring.quercus;

import java.util.logging.*;

import javax.servlet.*;

import com.caucho.quercus.env.*;
import com.caucho.quercus.module.*;

import com.caucho.util.L10N;

import org.springframework.web.context.*;
import org.springframework.web.context.support.*;

public class SpringModule
  extends AbstractQuercusModule
{
  private static final L10N L = new L10N(SpringModule.class);

  private static final Logger log
    = Logger.getLogger(SpringModule.class.getName());

  private WebApplicationContext _webAppContext;

  /**
   * Returns the spring bean with the given name.
   */
  public Object spring_bean(Env env, String name)
  {
    WebApplicationContext context = _webAppContext;

    if (context == null) {
      ServletContext webApp = env.getServletContext();
      
      context = WebApplicationContextUtils.getWebApplicationContext(webApp);
      _webAppContext = context;

      if (context == null) {
	env.notice(L.l("Can't find Spring context in '{0}'", webApp));

	return null;
      }
    }

    return context.getBean(name);
  }
}
