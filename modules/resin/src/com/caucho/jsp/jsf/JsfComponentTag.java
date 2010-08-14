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

package com.caucho.jsp.jsf;

import java.io.*;

import java.util.*;

import javax.el.*;

import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.webapp.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

public class JsfComponentTag extends UIComponentClassicTagBase
{
  private UIComponent _component;
  private boolean _isCreated;

  public JsfComponentTag(UIComponent component,
                         boolean isCreated,
                         BodyContent bodyContent)
  {
    _component = component;
    _isCreated = isCreated;

    setBodyContent(bodyContent);
  }

  @Override
  public String getRendererType()
  {
    return null;
  }

  @Override
  public String getComponentType()
  {
    return null;
  }

  @Override
  protected boolean hasBinding()
  {
    return false;
  }

  @Override
  public UIComponent getComponentInstance()
  {
    return _component;
  }

  @Override
  public boolean getCreated()
  {
    return _isCreated;
  }

  protected UIComponent createComponent(FacesContext context,
                                        String newId)
    throws JspException
  {
    return null;
  }
    
  protected void setProperties(UIComponent component)
  {
  }
}
