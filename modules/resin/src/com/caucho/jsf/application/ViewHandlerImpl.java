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

package com.caucho.jsf.application;

import java.io.*;
import java.util.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.context.*;
import javax.faces.render.*;

public class ViewHandlerImpl extends ViewHandler
{
  public Locale calculateLocale(FacesContext context)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public String calculateCharacterEncoding(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();
    
    return "utf-8";
  }

  public String calculateRenderKitId(FacesContext context)
  {
    return RenderKitFactory.HTML_BASIC_RENDER_KIT;
  }

  public UIViewRoot createView(FacesContext context,
                               String viewId)
  {
    return new UIViewRoot();
  }

  public String getActionURL(FacesContext context,
                             String viewId)
  {
    throw new UnsupportedOperationException();
  }

  public String getResourceURL(FacesContext context,
                               String path)
  {
    throw new UnsupportedOperationException();
  }

  public void renderView(FacesContext context,
                         UIViewRoot viewToRender)
    throws IOException, FacesException
  {
  }

  public UIViewRoot restoreView(FacesContext context,
                                String viewId)
    throws FacesException
  {
    return null;
  }

  public void writeState(FacesContext context)
    throws IOException
  {
  }

  public String toString()
  {
    return "ViewHandlerImpl[]";
  }
}
