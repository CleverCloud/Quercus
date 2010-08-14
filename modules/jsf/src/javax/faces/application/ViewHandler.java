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

public abstract class ViewHandler
{
  public static final String CHARACTER_ENCODING_KEY
    = "javax.faces.request.charset";
  public static final String DEFAULT_SUFFIX
    = ".jsp";
  public static final String DEFAULT_SUFFIX_PARAM_NAME
    = "javax.faces.DEFAULT_SUFFIX";

  public abstract Locale calculateLocale(FacesContext context);

  public String calculateCharacterEncoding(FacesContext context)
  {
    return "UTF-8";
  }

  public abstract String calculateRenderKitId(FacesContext context);

  public abstract UIViewRoot createView(FacesContext context,
                                        String viewId);

  public abstract String getActionURL(FacesContext context,
                                      String viewId);

  public abstract String getResourceURL(FacesContext context,
                                        String path);

  public void initView(FacesContext context)
    throws FacesException
  {
    try {
      String encoding = calculateCharacterEncoding(context);

      if (encoding != null)
        context.getExternalContext().setRequestCharacterEncoding(encoding);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new FacesException(e);
    }
  }

  public abstract void renderView(FacesContext context,
                                  UIViewRoot viewToRender)
    throws IOException, FacesException;

  public abstract UIViewRoot restoreView(FacesContext context,
                                         String viewId)
    throws FacesException;

  public abstract void writeState(FacesContext context)
    throws IOException;
}
