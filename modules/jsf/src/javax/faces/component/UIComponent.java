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

package javax.faces.component;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.el.*;

import javax.faces.*;
import javax.faces.context.*;
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.render.*;

public abstract class UIComponent
  implements StateHolder
{
  private static final Logger log
    = Logger.getLogger(UIComponent.class.getName());

  protected Map<String,ValueExpression> bindings;

  public abstract Map<String,Object> getAttributes();

  /**
   * @deprecated
   */
  public abstract ValueBinding getValueBinding(String name);

  /**
   * @deprecated
   */
  public abstract void setValueBinding(String name, ValueBinding binding);

  /**
   * @Since 1.2
   */
  public javax.el.ValueExpression getValueExpression(String name)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * @Since 1.2
   */
  public void setValueExpression(String name, ValueExpression binding)
  {
    throw new UnsupportedOperationException();
  }

  public abstract String getClientId(FacesContext context);

  /**
   * @Since 1.2
   */
  public String getContainerClientId(FacesContext context)
  {
    return getClientId(context);
  }

  public abstract String getFamily();

  public abstract String getId();

  public abstract void setId(String id);

  public abstract UIComponent getParent();

  public abstract void setParent(UIComponent parent);

  public abstract boolean isRendered();

  public abstract void setRendered(boolean rendered);

  public abstract String getRendererType();

  public abstract void setRendererType(String rendererType);

  public abstract boolean getRendersChildren();

  public abstract List<UIComponent> getChildren();

  public abstract int getChildCount();

  public abstract UIComponent findComponent(String expr);

  /**
   * @Since 1.2
   */
  public boolean invokeOnComponent(FacesContext context,
                                   String clientId,
                                   ContextCallback callback)
    throws FacesException
  {
    if (context == null || clientId == null || callback == null)
      throw new NullPointerException();

    if (clientId.equals(getClientId(context))) {
      try {
        callback.invokeContextCallback(context, this);

        return true;
      }
      catch (Exception e) {
        throw new FacesException(e);
      }
    }
    else {
      Iterator<UIComponent> iter = getFacetsAndChildren();

      while (iter.hasNext()) {
        UIComponent comp = iter.next();

        boolean result = comp.invokeOnComponent(context, clientId, callback);
        if (result)
          return true;
      }

      return false;
    }
  }

  public abstract Map<String,UIComponent> getFacets();

  /**
   * @Since 1.2
   */
  public int getFacetCount()
  {
    Map<String,UIComponent> map = getFacets();

    if (map == null)
      return 0;
    else
      return getFacets().size();
  }

  public abstract UIComponent getFacet(String name);

  public abstract Iterator<UIComponent> getFacetsAndChildren();

  public abstract void broadcast(FacesEvent event)
    throws AbortProcessingException;

  public abstract void decode(FacesContext context);

  public abstract void encodeBegin(FacesContext context)
    throws IOException;

  public abstract void encodeChildren(FacesContext context)
    throws IOException;

  public abstract void encodeEnd(FacesContext context)
    throws IOException;

  /**
   * @Since 1.2
   */
  
  /**
   * Encodes all children
   */
  public void encodeAll(FacesContext context)
    throws IOException
  {
    if (context == null)
      throw new NullPointerException();

    if (! isRendered()) {
      return;
    }
    
    encodeBegin(context);

    if (getRendersChildren()) {
      encodeChildren(context);
    }
    else {
      int childCount = getChildCount();

      if (childCount > 0) {
        List<UIComponent> children = getChildren();

        for (int i = 0; i < childCount; i++) {
          UIComponent child = children.get(i);

          child.encodeAll(context);
        }
      }
    }
    
    encodeEnd(context);
  }

  protected abstract void addFacesListener(FacesListener listener);

  protected abstract FacesListener []getFacesListeners(Class cl);

  protected abstract void removeFacesListener(FacesListener listener);

  public abstract void queueEvent(FacesEvent event);

  public abstract void processRestoreState(FacesContext context,
                                           Object state);

  public abstract void processDecodes(FacesContext context);

  public abstract void processValidators(FacesContext context);

  public abstract void processUpdates(FacesContext context);

  public abstract Object processSaveState(FacesContext context);

  protected abstract FacesContext getFacesContext();

  protected abstract Renderer getRenderer(FacesContext context);
}