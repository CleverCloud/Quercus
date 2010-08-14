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

package javax.faces.context;

import java.util.*;

import javax.el.*;

import javax.faces.event.PhaseId;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.render.*;

public abstract class FacesContext {
  private static final ThreadLocal<FacesContext> _currentInstance
    = new ThreadLocal<FacesContext>();

  private PhaseId _currentPhaseId;
  
  public abstract Application getApplication();

  public abstract Iterator<String> getClientIdsWithMessages();

  /**
   * @Since 1.2
   */
  public ELContext getELContext()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public abstract ExternalContext getExternalContext();

  public abstract FacesMessage.Severity getMaximumSeverity();

  public abstract Iterator<FacesMessage> getMessages();

  public abstract Iterator<FacesMessage> getMessages(String clientId);

  public abstract RenderKit getRenderKit();

  public abstract boolean getRenderResponse();

  public abstract boolean getResponseComplete();

  public abstract ResponseStream getResponseStream();

  public abstract void setResponseStream(ResponseStream responseStream);

  public abstract ResponseWriter getResponseWriter();

  public abstract void setResponseWriter(ResponseWriter writer);

  public abstract UIViewRoot getViewRoot();

  public abstract void setViewRoot(UIViewRoot root);

  public abstract void addMessage(String clientId,
                                  FacesMessage message);

  public abstract void release();

  public abstract void renderResponse();
  
  public abstract void responseComplete();

  public static FacesContext getCurrentInstance()
  {
    FacesContext context = _currentInstance.get();

    return context;
  }

  protected static void setCurrentInstance(FacesContext context)
  {
    _currentInstance.set(context);
  }
}
