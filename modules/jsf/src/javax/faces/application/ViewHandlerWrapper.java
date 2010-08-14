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

package javax.faces.application;

import java.io.*;
import java.util.*;

import javax.faces.*;
import javax.faces.component.*;
import javax.faces.context.*;

public abstract class ViewHandlerWrapper extends ViewHandler
{
  protected abstract ViewHandler getWrapped();
  
  public Locale calculateLocale(FacesContext context)
  {
    return getWrapped().calculateLocale(context);
  }

  public String calculateCharacterEncoding(FacesContext context)
  {
    return getWrapped().calculateCharacterEncoding(context);
  }

  public String calculateRenderKitId(FacesContext context)
  {
    return getWrapped().calculateRenderKitId(context);
  }

  public UIViewRoot createView(FacesContext context,
                               String viewId)
  {
    return getWrapped().createView(context, viewId);
  }

  public String getActionURL(FacesContext context, String viewId)
  {
    return getWrapped().getActionURL(context, viewId);
  }
  
  public String getResourceURL(FacesContext context, String path)
  {
    return getWrapped().getResourceURL(context, path);
  }

  public void initView(FacesContext context)
    throws FacesException
  {
    getWrapped().initView(context);
  }

  public void renderView(FacesContext context,
                         UIViewRoot viewToRender)
    throws IOException, FacesException
  {
    getWrapped().renderView(context, viewToRender);
  }

  public UIViewRoot restoreView(FacesContext context,
                                String viewId)
    throws FacesException
  {
    return getWrapped().restoreView(context, viewId);
  }

  public void writeState(FacesContext context)
    throws IOException
  {
    getWrapped().writeState(context);
  }
}
