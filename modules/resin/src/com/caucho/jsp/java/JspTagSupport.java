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

package com.caucho.jsp.java;

import com.caucho.config.ConfigException;
import com.caucho.vfs.Path;
import com.caucho.vfs.PersistentDependency;

import javax.servlet.ServletException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagLibraryInfo;
import java.util.ArrayList;

/**
 * Represents the body for a SimpleTag
 */
abstract public class JspTagSupport extends SimpleTagSupport
{
  abstract public TagInfo _caucho_getTagInfo(TagLibraryInfo taglib)
    throws ConfigException;
  
  abstract public String _caucho_getDynamicAttributes();
  
  public void init(Path path)
    throws ServletException
  {
  }
  
  public ArrayList _caucho_getDependList()
  {
    return new ArrayList();
  }
  
  public void _caucho_addDepend(PersistentDependency depend)
  {
  }
  
  public boolean _caucho_isModified()
  {
    return false;
  }

  @Override
  public String toString()
  {
    return this.getClass().getName() + "[]";
  }
}
