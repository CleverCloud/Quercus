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

package com.caucho.jsp;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Enumeration;
import java.util.Hashtable;

class JspServletConfig implements ServletConfig  {
  private Hashtable init;
  private ServletContext _context;
  private String _name;

  JspServletConfig(ServletContext context, Hashtable init, String name)
  {
    if (init == null)
      init = new Hashtable();
    this.init = init;
    _context = context;
    _name = name;

    if (context == null)
      throw new NullPointerException();
  }

  public String getServletName()
  {
    return _name;
  }

  public ServletContext getServletContext()
  {
    return _context;
  }
  
  public String getInitParameter(String name)
  {
    return (String) init.get(name);
  }
  
  public Enumeration getInitParameterNames()
  { 
    return init.keys();
  }
}
