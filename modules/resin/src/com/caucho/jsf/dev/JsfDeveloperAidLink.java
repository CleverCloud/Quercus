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
 * @author Alex Rojkov
 */

package com.caucho.jsf.dev;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import java.io.IOException;

public class JsfDeveloperAidLink
  extends UIComponentBase
{
  private String _style;

  public String getFamily() {
    return null;
  }

  public void setStyle(String style)
  {
    _style = style;
  }

  public String getStyle()
  {
    return _style;
  }

  public void encodeBegin(FacesContext context)
    throws IOException
  {
    String contextPath = context.getExternalContext().getRequestContextPath();

    ResponseWriter out = context.getResponseWriter();

    out.startElement("a", this);

    if (_style == null)
      out.writeAttribute("style", "position:absolute; bottom:0; right:0", null);
    else
      out.writeAttribute("style", _style, null);

    out.writeAttribute("href", contextPath + "/caucho.jsf.developer.aid", null);

    out.writeText("JSF Dev Aid", null);

    out.endElement("a");
  }

  public void encodeChildren(FacesContext context)
    throws IOException
  {
  }


  public void encodeEnd(FacesContext context)
    throws IOException
  {
  }

  public boolean getRendersChildren()
  {
    return true;
  }

  public boolean isRendered()
  {
    return true;
  }

  public boolean isTransient()
  {
    return true;
  }
}
