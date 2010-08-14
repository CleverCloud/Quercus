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

import com.caucho.vfs.Dependency;
import com.caucho.vfs.Path;
import com.caucho.vfs.PersistentDependency;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.jsp.HttpJspPage;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Wraps Java JSP files using 'extends' in a page.  Since a JSP file which
 * uses 'extends' does not subclass from Page, we need to wrap it with
 * a Page-compatible class.
 *
 * <p>Because it inherits from Page, the wrapped page still be recompiled
 * when the underlying page changes.
 */
class WrapperPage extends Page {
  private HttpJspPage _child;
  private CauchoPage _childPage;

  WrapperPage(HttpJspPage child)
    throws IOException
  {
    _child = child;
    
    if (_child instanceof CauchoPage)
      _childPage = (CauchoPage) child;
  }

  public void init(Path path)
    throws ServletException
  {
    if (_childPage != null)
      _childPage.init(path);
  }

  public void caucho_init(ServletConfig config)
    throws ServletException
  {
    super.init(config);

    if (_childPage != null)
      _childPage.caucho_init(config);
    else
      _child.init(config);
  }

  /**
   * Forward the initialization to the wrapped page.
   */
  final public void init(ServletConfig config) throws ServletException
  {
    super.init(config);
    
    _child.init(config);
  }

  /**
   * Returns the underlying page.
   */
  public HttpJspPage getWrappedPage()
  {
    return _child;
  }

  @Override
  public boolean _caucho_isModified()
  {
    if (_childPage != null)
      return _childPage._caucho_isModified();
    else
      return false;
  }

  public ArrayList<Dependency> _caucho_getDependList()
  {
    if (_childPage != null)
      return _childPage._caucho_getDependList();

    if (_child instanceof CauchoPage)
      ((CauchoPage)_child)._caucho_getDependList();

    return null;
  }

  public long _caucho_lastModified()
  {
    if (_childPage != null)
      return _childPage._caucho_lastModified();
    else
      return 0;
  }

  /**
   * Forwards the request to the child page.
   */
  public void service(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    _child.service(request, response);
  }

  /**
   * Forward the destruction to the wrapped page.
   */
  final public void destroy()
  {
    try {
      setDead();
      
      _child.destroy();
    } catch (Exception e) {
    }
  }
}
