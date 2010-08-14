/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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

package com.caucho.jsf.taglib;

import java.io.*;
import java.util.logging.*;

import javax.el.*;

import javax.faces.context.*;
import javax.faces.component.*;
import javax.faces.webapp.*;
import javax.faces.event.PhaseListener;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;

import javax.servlet.*;
import javax.servlet.jsp.*;

import com.caucho.jsf.context.*;
import com.caucho.util.L10N;

/**
 * The f:view tag
 */
public class FacesViewTag extends UIComponentELTag
{
  private static final L10N L = new L10N(FacesViewTag.class);

  private static final Logger log
    = Logger.getLogger(FacesViewTag.class.getName());

  private ValueExpression _renderKitId;
  private ValueExpression _locale;
  private MethodExpression _beforePhase;
  private MethodExpression _afterPhase;

  public String getComponentType()
  {
    return UIViewRoot.COMPONENT_TYPE;
  }

  public String getRendererType()
  {
    return null;
  }

  /**
   * Sets the render kit id
   */
  public void setRenderKitId(ValueExpression value)
  {
    _renderKitId = value;
  }

  /**
   * Sets the locale
   */
  public void setLocale(ValueExpression value)
  {
    _locale = value;
  }

  /**
   * Sets the before-phase method
   */
  public void setBeforePhase(MethodExpression value)
  {
    _beforePhase = value;
  }

  /**
   * Sets the after-phase method
   */
  public void setAfterPhase(MethodExpression value)
  {
    _afterPhase = value;
  }

  public int doStartTag()
    throws JspException
  {
    PageContext pageContext = this.pageContext;
    FacesContext facesContext = FacesContext.getCurrentInstance();
    ServletResponse response = pageContext.getResponse();
    int doStartValue = super.doStartTag();

    response.setLocale(facesContext.getViewRoot().getLocale());

    try {
      if (response instanceof JspResponseWrapper)
        ((JspResponseWrapper) response).flushResponse();
    }
    catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return doStartValue;
  }

  public int doEndTag()
    throws JspException
  {
    return super.doEndTag();
  }

  protected void setProperties(UIComponent component)
  {
    boolean isFiner = log.isLoggable(Level.FINER);

    UIViewRoot viewRoot = (UIViewRoot) component;

    if (_renderKitId != null) {
      if (isFiner)
        log.log(Level.FINER,
                L.l("{0}: setting renderKitId to {1}", viewRoot, _renderKitId));

      viewRoot.setValueExpression("renderKitId", _renderKitId);
    }

    if (_locale != null) {
      if (isFiner)
        log.log(Level.FINE,
                L.l("{0}: setting locale to {1}", viewRoot, _locale));

      viewRoot.setValueExpression("locale", _locale);
    }


    if (_beforePhase != null) {
      viewRoot.addPhaseListener(new BeforePhaseListenerAdapter(_beforePhase));
    }

    if (_afterPhase != null) {
      viewRoot.addPhaseListener(new AfterPhaseListenerAdapter(_afterPhase));
    }
  }

  public static class BeforePhaseListenerAdapter
    extends AbstractPhaseListenerAdapter
  {
    public BeforePhaseListenerAdapter()
    {
      super();
    }

    public BeforePhaseListenerAdapter(MethodExpression methodExpression)
    {
      super(methodExpression);
    }

    @Override
    public void beforePhase(PhaseEvent event)
    {
      if (PhaseId.RESTORE_VIEW.getOrdinal() != event.getPhaseId().getOrdinal())
        _methodExpression.invoke(FacesContext.getCurrentInstance().getELContext(),
                                 new Object[]{event});
    }
  }

  public static class AfterPhaseListenerAdapter
    extends AbstractPhaseListenerAdapter
  {
    public AfterPhaseListenerAdapter()
    {
      super();
    }

    public AfterPhaseListenerAdapter(MethodExpression methodExpression)
    {
      super(methodExpression);
    }

    @Override
    public void afterPhase(PhaseEvent event)
    {
      if (PhaseId.RESTORE_VIEW.getOrdinal() != event.getPhaseId().getOrdinal())
        _methodExpression.invoke(FacesContext.getCurrentInstance().getELContext(),
                                 new Object[]{event});
    }
  }

  public abstract static class AbstractPhaseListenerAdapter
    implements PhaseListener, StateHolder
  {
    protected MethodExpression _methodExpression;
    private boolean _transient;

    public AbstractPhaseListenerAdapter(MethodExpression methodExpression)
    {
      _methodExpression = methodExpression;
    }

    public AbstractPhaseListenerAdapter()
    {
    }

    public void afterPhase(PhaseEvent event)
    {
    }

    public void beforePhase(PhaseEvent event)
    {
    }

    public PhaseId getPhaseId()
    {
      return PhaseId.ANY_PHASE;
    }

    public Object saveState(FacesContext context)
    {
      return _methodExpression;
    }

    public void restoreState(FacesContext context, Object state)
    {
      _methodExpression = (MethodExpression) state;

    }

    public boolean isTransient()
    {
      return _transient;
    }

    public void setTransient(boolean isTransient)
    {
      _transient = isTransient;
    }
  }

}
