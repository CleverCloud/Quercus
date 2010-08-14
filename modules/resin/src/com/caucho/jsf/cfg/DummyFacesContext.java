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

package com.caucho.jsf.cfg;

import java.util.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.context.*;
import javax.faces.component.*;
import javax.faces.render.*;

import javax.servlet.*;
import javax.servlet.http.*;

public class DummyFacesContext extends FacesContext
{
  DummyFacesContext()
  {
  }
  
  public Application getApplication()
  {
    throw new UnsupportedOperationException();
  }

  public ExternalContext getExternalContext()
  {
    throw new UnsupportedOperationException();
  }

  public RenderKit getRenderKit()
  {
    throw new UnsupportedOperationException();
  }

  public ResponseStream getResponseStream()
  {
    throw new UnsupportedOperationException();
  }

  public void setResponseStream(ResponseStream responseStream)
  {
    throw new UnsupportedOperationException();
  }

  public ResponseWriter getResponseWriter()
  {
    throw new UnsupportedOperationException();
  }

  public void setResponseWriter(ResponseWriter writer)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the root of the UI component tree.
   */
  public UIViewRoot getViewRoot()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets the root of the UI component tree.
   */
  public void setViewRoot(UIViewRoot root)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * If true the facelet will skip to the render phase.
   */
  @Override
  public boolean getRenderResponse()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Ask the lifecycle to skip to the render phase.
   */
  @Override
  public void renderResponse()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Return true if the lifecycle should skip the response phase.
   */
  @Override
  public boolean getResponseComplete()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Ask the lifecycle to skip the response phase.
   */
  @Override
  public void responseComplete()
  {
    throw new UnsupportedOperationException();
  }

  public void addMessage(String clientId,
                         FacesMessage message)
  {
    throw new UnsupportedOperationException();
  }

  public Iterator<String> getClientIdsWithMessages()
  {
    throw new UnsupportedOperationException();
  }

  public FacesMessage.Severity getMaximumSeverity()
  {
    throw new UnsupportedOperationException();
  }

  public Iterator<FacesMessage> getMessages()
  {
    throw new UnsupportedOperationException();
  }

  public Iterator<FacesMessage> getMessages(String clientId)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * @Since 1.2
   */
  @Override
  public ELContext getELContext()
  {
    throw new UnsupportedOperationException();
  }

  public void release()
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return "DummyFacesContext[]";
  }
}
