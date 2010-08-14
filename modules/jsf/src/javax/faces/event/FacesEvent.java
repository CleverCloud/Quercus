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

package javax.faces.event;

import java.util.*;

import javax.faces.component.*;

public abstract class FacesEvent extends EventObject
{
  private PhaseId phaseId;

  public FacesEvent(UIComponent component)
  {
    super(component);
    
    if (component == null)
      throw new IllegalArgumentException();

    this.phaseId = PhaseId.ANY_PHASE;
  }

  public UIComponent getComponent()
  {
    return (UIComponent) this.source;
  }

  public PhaseId getPhaseId()
  {
    return this.phaseId;
  }

  public void setPhaseId(PhaseId phaseId)
  {
    if (phaseId == null)
      throw new IllegalArgumentException();
    
    this.phaseId = phaseId;
  }

  public void queue()
  {
    //the spec dictates that the exception be thrown. but Trinidad uses
    //facesevent.queue all over... so removing the check for now.
    getComponent().queueEvent(this);
    //if (! (getComponent() instanceof UIViewRoot))
     // throw new IllegalStateException("FacesEvent component must descent from UIViewRoot to use queue()");
    
    //throw new UnsupportedOperationException();
  }

  public abstract boolean isAppropriateListener(FacesListener listener);

  public abstract void processListener(FacesListener listener)
    throws AbortProcessingException;
}
