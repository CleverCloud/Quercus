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

import javax.faces.component.*;
import javax.faces.context.*;

public abstract class StateManager
{
  public static final String STATE_SAVING_METHOD_CLIENT
    = "client";
  public static final String STATE_SAVING_METHOD_PARAM_NAME
    = "javax.faces.STATE_SAVING_METHOD";
  public static final String STATE_SAVING_METHOD_SERVER
    = "server";

  /**
   * @deprecated
   */
  public SerializedView saveSerializedView(FacesContext context)
  {
    return null;
  }

  /**
   * @Since 1.2
   */
  public Object saveView(FacesContext context)
  {
    SerializedView view = saveSerializedView(context);

    if (view == null)
      return null;

    return new Object[] { view.getStructure(), view.getState() };
  }

  /**
   * @deprecated
   */
  protected Object getTreeStructureToSave(FacesContext context)
  {
    return null;
  }

  /**
   * @deprecated
   */
  protected Object getComponentStateToSave(FacesContext context)
  {
    return null;
  }

  /**
   * @Since 1.2
   */
  public void writeState(FacesContext context,
                         Object state)
    throws IOException
  {
    if (state != null && Object[].class.isAssignableFrom(state.getClass())) {
      Object []data = (Object []) state;

      if (data.length == 2) {
        SerializedView view = new SerializedView(data[0], data[1]);

        writeState(context, view);
      }
    }
  }

  /**
   * @deprecated
   */
  public void writeState(FacesContext context,
                         SerializedView state)
    throws IOException
  {
  }

  public abstract UIViewRoot restoreView(FacesContext context,
                                         String viewId,
                                         String renderKitIt);

  /**
   * @deprecated
   */
  protected UIViewRoot restoreTreeStructure(FacesContext context,
                                            String viewId,
                                            String renderKitId)
  {
    return null;
  }

  /**
   * @deprecated
   */
  protected void restoreComponentState(FacesContext context,
                                       UIViewRoot viewRoot,
                                       String renderKitId)
  {
  }

  public boolean isSavingStateInClient(FacesContext context)
  {
    ExternalContext extContext = context.getExternalContext();
    String value = extContext.getInitParameter(STATE_SAVING_METHOD_PARAM_NAME);
    
    return STATE_SAVING_METHOD_CLIENT.equals(value);
  }

  /**
   * @deprecated
   */
  public class SerializedView {
    private Object _state;
    private Object _structure;

    public SerializedView(Object structure, Object state)
    {
      _structure = structure;
      _state = state;
    }

    public Object getStructure()
    {
      return _structure;
    }

    public Object getState()
    {
      return _state;
    }
  }
}
