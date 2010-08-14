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

package javax.faces.webapp;

import java.util.logging.Logger;

import javax.el.*;

import javax.faces.component.*;
import javax.faces.context.*;

import javax.servlet.jsp.tagext.*;

public abstract class UIComponentTagBase implements JspTag
{
  protected static java.util.logging.Logger log =
    Logger.getLogger(UIComponentTagBase.class.getName());
  
  protected abstract FacesContext getFacesContext();
  
  protected ELContext getELContext()
  {
    return getFacesContext().getELContext();
  }
  
  protected abstract void addChild(UIComponent child);
  
  protected abstract void addFacet(String name);
  
  public abstract void setId(String id);

  public abstract String getComponentType();

  public abstract String getRendererType();

  public abstract UIComponent getComponentInstance();

  public abstract boolean getCreated();

  protected abstract int getIndexOfNextChildTag();
}
