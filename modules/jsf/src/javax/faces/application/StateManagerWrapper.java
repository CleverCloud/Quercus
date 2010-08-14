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

public abstract class StateManagerWrapper extends StateManager
{
  abstract protected StateManager getWrapped();
  
  /**
   * @deprecated
   */
  public SerializedView saveSerializedView(FacesContext context)
  {
    return getWrapped().saveSerializedView(context);
  }

  /**
   * @Since 1.2
   */
  public Object saveView(FacesContext context)
  {
    return getWrapped().saveView(context);
  }

  /**
   * @deprecated
   */
  protected Object getTreeStructureToSave(FacesContext context)
  {
    return getWrapped().getTreeStructureToSave(context);
  }

  /**
   * @deprecated
   */
  protected Object getComponentStateToSave(FacesContext context)
  {
    return getWrapped().getComponentStateToSave(context);
  }

  /**
   * @Since 1.2
   */
  public void writeState(FacesContext context,
                         Object state)
    throws IOException
  {
    getWrapped().writeState(context, state);
  }

  /**
   * @deprecated
   */
  public void writeState(FacesContext context,
                         SerializedView state)
    throws IOException
  {
    getWrapped().writeState(context, state);
  }

  public UIViewRoot restoreView(FacesContext context,
                                String viewId,
                                String renderKitId)
  {
    return getWrapped().restoreView(context, viewId, renderKitId);
  }

  /**
   * @deprecated
   */
  protected UIViewRoot restoreTreeStructure(FacesContext context,
                                            String viewId,
                                            String renderKitId)
  {
    return getWrapped().restoreTreeStructure(context, viewId, renderKitId);
  }

  /**
   * @deprecated
   */
  protected void restoreComponentState(FacesContext context,
                                       UIViewRoot viewRoot,
                                       String renderKitId)
  {
    getWrapped().restoreComponentState(context, viewRoot, renderKitId);
  }

  public boolean isSavingStateInClient(FacesContext context)
  {
    return getWrapped().isSavingStateInClient(context);
  }
}
