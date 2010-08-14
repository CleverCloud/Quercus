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

package javax.faces.render;

import java.io.*;
import java.util.*;

import javax.faces.component.*;
import javax.faces.convert.*;
import javax.faces.context.*;

public abstract class Renderer {
  public void decode(FacesContext context,
                     UIComponent component)
  {
  }

  public void encodeBegin(FacesContext context, UIComponent component)
    throws IOException
  {
  }

  public void encodeChildren(FacesContext context, UIComponent component)
    throws IOException
  {
    int size = component.getChildCount();
    
    if (size > 0) {
      List<UIComponent> children = component.getChildren();

      for (int i = 0; i < size; i++) {
        UIComponent child = children.get(i);

        child.encodeAll(context);
      }
    }
  }

  public void encodeEnd(FacesContext context, UIComponent component)
    throws IOException
  {
  }

  public String convertClientId(FacesContext context, String clientId)
  {
    return clientId;
  }

  public boolean getRendersChildren()
  {
    return false;
  }

  public Object getConvertedValue(FacesContext context,
                                  UIComponent component,
                                  Object submittedValue)
    throws ConverterException
  {
    if (context == null || component == null)
      throw new NullPointerException();
    
    return submittedValue;
  }
}
