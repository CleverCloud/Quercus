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

import java.util.*;
import java.util.logging.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.context.*;
import javax.faces.event.*;
import javax.faces.webapp.*;
import javax.faces.lifecycle.*;

public class UIViewRoot extends UIComponentBase
{
  private static final Logger log
    = Logger.getLogger(UIViewRoot.class.getName());
  
  public static final String COMPONENT_FAMILY = "javax.faces.ViewRoot";
  public static final String COMPONENT_TYPE = "javax.faces.ViewRoot";
  public static final String UNIQUE_ID_PREFIX = "j_id";

  private String _renderKitId;
  private ValueExpression _renderKitIdExpr;

  private String _viewId;
  private int _unique;

  private Locale _locale;
  private ValueExpression _localeExpr;

  private ArrayList<PhaseListener> _phaseListeners;
  
  private ArrayList<FacesEvent> _eventList;

  private MethodExpression _beforePhaseListener;
  private MethodExpression _afterPhaseListener;

  private Lifecycle _lifecycle;

  public UIViewRoot()
  {
  }

  public String getFamily()
  {
    return COMPONENT_FAMILY;
  }

  public void setAfterPhaseListener(MethodExpression expr)
  {
    _afterPhaseListener = expr;
  }

  public MethodExpression getAfterPhaseListener()
  {
    return _afterPhaseListener;
  }

  public void setBeforePhaseListener(MethodExpression expr)
  {
    _beforePhaseListener = expr;
  }

  public MethodExpression getBeforePhaseListener()
  {
    return _beforePhaseListener;
  }
  
  public String getRenderKitId()
  {
    if (_renderKitId != null)
      return _renderKitId;
    else if (_renderKitIdExpr != null)
      return Util.evalString(_renderKitIdExpr, getFacesContext());
    else
      return null;
  }
  
  public void setRenderKitId(String renderKitId)
  {
    _renderKitId = renderKitId;
  }

  public String getViewId()
  {
    return _viewId;
  }
  
  public void setViewId(String value)
  {
    _viewId = value;
  }

  public void setLocale(Locale locale)
  {
    _locale = locale;
  }

  public Locale getLocale()
  {
    if (_locale != null)
      return _locale;

    Locale locale = null;
    FacesContext context = getFacesContext();
    
    if (_localeExpr != null)
      locale = toLocale(Util.eval(_localeExpr, context));

    if (locale == null) {
      ViewHandler viewHandler = context.getApplication().getViewHandler();

      locale = viewHandler.calculateLocale(context);
    }

    return locale;
  }

  //
  // expression map override
  //

  /**
   * Returns the value expression with the given name.
   */
  @Override
  public ValueExpression getValueExpression(String name)
  {
    if ("renderKitId".equals(name))
      return _renderKitIdExpr;
    else if ("locale".equals(name))
      return _localeExpr;
    else {
      return super.getValueExpression(name);
    }
  }

  /**
   * Sets the value expression with the given name.
   */
  @Override
  public void setValueExpression(String name, ValueExpression expr)
  {
    if ("renderKitId".equals(name)) {
      if (expr != null && expr.isLiteralText()) {
        _renderKitId = (String) expr.getValue(null);
        return;
      }
      else
        _renderKitIdExpr = expr;
    }
    else if ("locale".equals(name)) {
      if (expr != null && expr.isLiteralText()) {
        _locale = toLocale(expr.getValue(null));
        return;
      }
      else
        _localeExpr = expr;
    }

    super.setValueExpression(name, expr);
  }

  public void addPhaseListener(PhaseListener listener)
  {
    if (_phaseListeners == null)
      _phaseListeners = new ArrayList<PhaseListener>();

    _phaseListeners.add(listener);
  }

  public void removePhaseListener(PhaseListener listener)
  {
    if (_phaseListeners != null)
      _phaseListeners.remove(listener);
  }

  public String createUniqueId()
  {
    return UNIQUE_ID_PREFIX + _unique++;
  }

  /**
   * Process the application.
   */
  public void processApplication(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    if (_beforePhaseListener != null || _phaseListeners != null)
      beforePhase(context, PhaseId.INVOKE_APPLICATION);

    broadcastEvents(PhaseId.INVOKE_APPLICATION);

    if (_afterPhaseListener != null || _phaseListeners != null)
      afterPhase(context, PhaseId.INVOKE_APPLICATION);
  }

  /**
   * Process the decodes.
   */
  public void processDecodes(FacesContext context)
  {
    if (_beforePhaseListener != null || _phaseListeners != null)
      beforePhase(context, PhaseId.APPLY_REQUEST_VALUES);

    super.processDecodes(context);

    broadcastEvents(PhaseId.APPLY_REQUEST_VALUES);
    
    if (_afterPhaseListener != null || _phaseListeners != null)
      afterPhase(context, PhaseId.APPLY_REQUEST_VALUES);
  }

  /**
   * Process the updates.
   */
  public void processUpdates(FacesContext context)
  {
    if (_beforePhaseListener != null || _phaseListeners != null)
      beforePhase(context, PhaseId.UPDATE_MODEL_VALUES);

    super.processUpdates(context);

    broadcastEvents(PhaseId.UPDATE_MODEL_VALUES);

    if (_afterPhaseListener != null || _phaseListeners != null)
      afterPhase(context, PhaseId.UPDATE_MODEL_VALUES);
  }

  /**
   * Process the validators.
   */
  @Override
  public void processValidators(FacesContext context)
  {
    if (_beforePhaseListener != null || _phaseListeners != null)
      beforePhase(context, PhaseId.PROCESS_VALIDATIONS);

    super.processValidators(context);

    broadcastEvents(PhaseId.PROCESS_VALIDATIONS);
    
    if (_afterPhaseListener != null || _phaseListeners != null)
      afterPhase(context, PhaseId.PROCESS_VALIDATIONS);
  }

  /**
   * Begin rendering
   */
  @Override
  public void encodeBegin(FacesContext context)
    throws java.io.IOException
  {
    if (_beforePhaseListener != null || _phaseListeners != null)
      beforePhase(context, PhaseId.RENDER_RESPONSE);

    super.encodeBegin(context);
  }

  /**
   * Begin rendering
   */
  @Override
  public void encodeEnd(FacesContext context)
    throws java.io.IOException
  {
    super.encodeEnd(context);
    
    if (_afterPhaseListener != null || _phaseListeners != null)
      afterPhase(context, PhaseId.RENDER_RESPONSE);
  }


  @Override
  public void queueEvent(FacesEvent event)
  {
    if (_eventList == null)
      _eventList = new ArrayList<FacesEvent>();

    if (log.isLoggable(Level.FINE))
      log.fine(this + " queueEvent " + event);

    _eventList.add(event);
  }

  private void broadcastEvents(PhaseId phaseId)
  {
    if (_eventList != null) {
      for (int i = 0; i < _eventList.size(); i++) {
        FacesEvent event = _eventList.get(i);
        PhaseId eventPhaseId = event.getPhaseId();

        if (phaseId.equals(eventPhaseId)
            || PhaseId.ANY_PHASE.equals(eventPhaseId)) {
          event.getComponent().broadcast(event);
          _eventList.remove(i);
          i--;
        }
      }
    }
  }

  private void afterPhase(FacesContext context, PhaseId phaseId)
  {
    Lifecycle lifecycle = getLifecycle(context);
    PhaseEvent event = new PhaseEvent(context, phaseId, lifecycle);

    if (_afterPhaseListener != null) {
      try {
        _afterPhaseListener.invoke(context.getELContext(),
                                   new Object[] { event });
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new FacesException(e);
      }
    }

    if (_phaseListeners != null) {
      for (int i = 0; i < _phaseListeners.size(); i++) {
        PhaseListener listener = _phaseListeners.get(i);

        listener.afterPhase(event);
      }
    }
  }

  private void beforePhase(FacesContext context, PhaseId phaseId)
  {
    Lifecycle lifecycle = getLifecycle(context);
    PhaseEvent event = new PhaseEvent(context, phaseId, lifecycle);

    if (_beforePhaseListener != null) {
      try {
        _beforePhaseListener.invoke(context.getELContext(),
                                   new Object[] { event });
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new FacesException(e);
      }
    }

    if (_phaseListeners != null) {
      for (int i = 0; i < _phaseListeners.size(); i++) {
        PhaseListener listener = _phaseListeners.get(i);

        listener.beforePhase(event);
      }
    }
  }

  private Lifecycle getLifecycle(FacesContext context)
  {
    if (_lifecycle == null) {
      LifecycleFactory factory = (LifecycleFactory)
        FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);

      ExternalContext extContext = context.getExternalContext();
      String id = extContext.getInitParameter(FacesServlet.LIFECYCLE_ID_ATTR);
    
      if (id == null)
        id = LifecycleFactory.DEFAULT_LIFECYCLE;
    
      _lifecycle = factory.getLifecycle(id);
    }

    return _lifecycle;
  }

  //
  // state
  //

  public Object saveState(FacesContext context)
  {
    return new Object[] {
      super.saveState(context),
      _viewId,
      _renderKitId,
      _locale == null ? null : _locale.toString(),
      _unique,
      _afterPhaseListener,
      _beforePhaseListener,
      saveAttachedState(context, _phaseListeners),
    };
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    super.restoreState(context, state[0]);

    _viewId = (String) state[1];
    _renderKitId = (String) state[2];
    _locale = toLocale((String) state[3]);
    _unique = (Integer) state[4];
    _afterPhaseListener = (MethodExpression) state[5];
    _beforePhaseListener = (MethodExpression) state[6];
    _phaseListeners = (ArrayList) restoreAttachedState(context, state[7]);
  }
  
  private Locale toLocale(Object value)
  {
    if (value instanceof Locale)
      return (Locale) value;
    else if (value instanceof String) {
      String sValue = (String) value;
      String []values = sValue.split("[-_]");

      if (values.length > 2)
        return new Locale(values[0], values[1], values[2]);
      else if (values.length > 1)
        return new Locale(values[0], values[1]);
      else
        return new Locale(sValue);
    }
    else if (value == null)
      return null;
    else
      return (Locale) value;
  }

  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getViewId() + "]";
  }
}
