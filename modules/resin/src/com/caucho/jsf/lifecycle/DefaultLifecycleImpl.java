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

package com.caucho.jsf.lifecycle;

import java.util.*;

import javax.faces.*;
import javax.faces.context.*;
import javax.faces.event.*;
import javax.faces.lifecycle.*;

/**
 * The default lifecycle implementation
 */
public class DefaultLifecycleImpl extends Lifecycle
{
  private ArrayList<PhaseListener> _phaseList = new ArrayList<PhaseListener>();
  private PhaseListener []_phaseListeners = new PhaseListener[0];
  
  public DefaultLifecycleImpl()
  {
  }

  public void addPhaseListener(PhaseListener listener)
  {
    synchronized (_phaseList) {
      _phaseList.add(listener);
      _phaseListeners = new PhaseListener[_phaseList.size()];
      _phaseList.toArray(_phaseListeners);
    }
  }
  
  public PhaseListener []getPhaseListeners()
  {
    return _phaseListeners;
  }
  
  public void removePhaseListener(PhaseListener listener)
  {
    synchronized (_phaseList) {
      _phaseList.remove(listener);
      _phaseListeners = new PhaseListener[_phaseList.size()];
      _phaseList.toArray(_phaseListeners);
    }
  }

  public void execute(FacesContext context)
    throws FacesException
  {
    if (context.getResponseComplete() || context.getRenderResponse())
      return;
  }
  
  public void render(FacesContext context)
    throws FacesException
  {
    if (context.getResponseComplete())
      return;
  }

  public String toString()
  {
    return "DefaultLifecycleImpl[]";
  }
}
