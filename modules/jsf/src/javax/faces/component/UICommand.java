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

import javax.el.*;

import javax.faces.application.*;
import javax.faces.context.*;
import javax.faces.el.*;
import javax.faces.event.*;

public class UICommand extends UIComponentBase
  implements ActionSource2
{
  public static final String COMPONENT_FAMILY = "javax.faces.Command";
  public static final String COMPONENT_TYPE = "javax.faces.Command";

  private static final HashMap<String,PropEnum> _propMap
    = new HashMap<String,PropEnum>();

  private static final ActionListener []NULL_ACTION_LISTENERS
    = new ActionListener[0];

  private Object _value;
  private ValueExpression _valueExpr;

  private Boolean _immediate;
  private ValueExpression _immediateExpr;
  
  private MethodExpression _actionExpr;

  private ActionListener []_actionListeners = NULL_ACTION_LISTENERS;

  public UICommand()
  {
    setRendererType("javax.faces.Button");
  }

  /**
   * Returns the component family, used to select the renderer.
   */
  public String getFamily()
  {
    return COMPONENT_FAMILY;
  }

  //
  // Properties
  //

  public Object getValue()
  {
    if (_value != null)
      return _value;
    else if (_valueExpr != null)
      return Util.eval(_valueExpr, getFacesContext());
    else
      return null;
  }

  public void setValue(Object value)
  {
    _value = value;
  }

  //
  // Render Properties
  //

  public boolean isImmediate()
  {
    if (_immediate != null)
      return _immediate;
    else if (_immediateExpr != null)
      return Util.evalBoolean(_immediateExpr, getFacesContext());
    else
      return false;
  }

  public void setImmediate(boolean immediate)
  {
    _immediate = immediate;
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
    if ("value".equals(name))
      return _valueExpr;
    else if ("immediate".equals(name))
      return _immediateExpr;
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
    if ("value".equals(name))
      _valueExpr = expr;
    else if ("immediate".equals(name))
      _immediateExpr = expr;

    super.setValueExpression(name, expr);
  }

  //
  // Actions
  //

  @Override
  public void broadcast(FacesEvent event)
  {
    super.broadcast(event);

    if (event instanceof ActionEvent) {
      ActionEvent actionEvent = (ActionEvent) event;

      FacesContext context = FacesContext.getCurrentInstance();

      // jsf/0235, jsf/31h6, jsf/31h8

      ActionListener listener = context.getApplication().getActionListener();

      if (listener != null) {
        listener.processAction(actionEvent);
      }
    }
  }

  @Override
  public void queueEvent(FacesEvent event)
  {
    if (event instanceof ActionEvent) {
      event.setPhaseId(isImmediate()
                       ? PhaseId.APPLY_REQUEST_VALUES
                       : PhaseId.INVOKE_APPLICATION);
    }

    super.queueEvent(event);
  }
  
  /**
   * @deprecated
   */
  public MethodBinding getAction()
  {
    if (_actionExpr == null)
      return null;
    else if (_actionExpr instanceof MethodExpressionAdapter)
      return ((MethodExpressionAdapter) _actionExpr).getBinding();
    else
      return new MethodBindingAdapter(_actionExpr);
  }
  
  /**
   * @deprecated
   */
  public void setAction(MethodBinding action)
  {
    if (action != null)
      _actionExpr = new MethodExpressionAdapter(action);
    else
      _actionExpr = null;
  }

  /**
   * @deprecated
   */
  public MethodBinding getActionListener()
  {
    FacesListener []listeners = getFacesListeners(FacesListener.class);

    for (int i = 0; i < listeners.length; i++) {
      if (listeners[i] instanceof ActionListenerAdapter) {
        return ((ActionListenerAdapter) listeners[i]).getBinding();
      }
    }

    return null;
  }

  /**
   * @deprecated
   */
  public void setActionListener(MethodBinding action)
  {
    if (action == null)
      throw new NullPointerException();

    FacesListener []listeners = getFacesListeners(FacesListener.class);

    for (int i = 0; i < listeners.length; i++) {
      if (listeners[i] instanceof ActionListenerAdapter) {
        listeners[i] = new ActionListenerAdapter(action);
        _actionListeners = null;
        return;
      }
    }

    addActionListener(new ActionListenerAdapter(action));
  }

  public void addActionListener(ActionListener listener)
  {
    if (listener == null)
      throw new NullPointerException();
    
    addFacesListener(listener);
    
    _actionListeners = null;
  }

  public ActionListener []getActionListeners()
  {
    if (_actionListeners == null) {
      _actionListeners =
        (ActionListener[]) getFacesListeners(ActionListener.class);
    }

    return _actionListeners;
  }

  public void removeActionListener(ActionListener listener)
  {
    if (listener == null)
      throw new NullPointerException();
    
    removeFacesListener(listener);
    
    _actionListeners = null;
  }

  public MethodExpression getActionExpression()
  {
    return _actionExpr;
  }

  public void setActionExpression(MethodExpression action)
  {
    if (action == null)
      throw new NullPointerException();
    
    _actionExpr = action;
  }

  //
  // state
  //

  public Object saveState(FacesContext context)
  {
    return new Object[] {
      super.saveState(context),
      _value,
      _immediate,
      saveAttachedState(context, _actionExpr),
    };
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    super.restoreState(context, state[0]);

    int i = 1;
    _value = state[i++];
    _immediate = (Boolean) state[i++];

    _actionListeners = null;

    _actionExpr = (MethodExpression) restoreAttachedState(context, state[i++]);
  }

  //
  // private helpers
  //

  private static enum PropEnum {
    VALUE,
  }

  static class MethodExpressionAdapter
    extends MethodExpression
    implements StateHolder
  {
    private MethodBinding _binding;

    private boolean _isTransient;

    public MethodExpressionAdapter()
    {
    }

    MethodExpressionAdapter(MethodBinding binding)
    {
      _binding = binding;
    }

    MethodBinding getBinding()
    {
      return _binding;
    }

    public boolean isLiteralText()
    {
      return false;
    }

    public String getExpressionString()
    {
      return _binding.getExpressionString();
    }

    public MethodInfo getMethodInfo(ELContext context)
      throws javax.el.PropertyNotFoundException,
             javax.el.MethodNotFoundException,
             ELException
    {
      throw new UnsupportedOperationException();
    }

    public Object invoke(ELContext context,
                         Object[] params)
      throws javax.el.PropertyNotFoundException,
             javax.el.MethodNotFoundException,
             ELException
    {
      return _binding.invoke(FacesContext.getCurrentInstance(), params);
    }

    public boolean isTransient()
    {
      return _isTransient;
    }

    public void setTransient(boolean aTransient)
    {
      _isTransient = aTransient;
    }

    public Object saveState(FacesContext context)
    {
      return saveAttachedState(context, _binding);
    }

    public void restoreState(FacesContext context, Object state)
    {
      _binding = (MethodBinding) restoreAttachedState(context, state);
    }

    public int hashCode()
    {
      return _binding.hashCode();
    }

    public boolean equals(Object o)
    {
      return (this == o);
    }
  }

  static class MethodBindingAdapter extends MethodBinding
  {
    private MethodExpression _expr;

    public MethodBindingAdapter()
    {
    }

    public MethodBindingAdapter(MethodExpression expr)
    {
      _expr = expr;
    }

    public String getExpressionString()
    {
      return _expr.getExpressionString();
    }
  
    /**
   * @deprecated
   */
      public Object invoke(FacesContext context, Object []param)
      throws EvaluationException, javax.faces.el.MethodNotFoundException
    {
      if (context == null)
        throw new NullPointerException();
    
      try {
        return _expr.invoke(context.getELContext(), param);
      } catch (javax.el.MethodNotFoundException e) {
        throw new javax.faces.el.MethodNotFoundException(e);
      } catch (ELException e) {
        if (e.getCause() != null)
          throw new EvaluationException(e.getCause());
        else
          throw new EvaluationException(e);
      } catch (Exception e) {
        throw new EvaluationException(e);
      }
    }

    /**
   * @deprecated
   */
      public Class getType(FacesContext context)
      throws EvaluationException, javax.faces.el.PropertyNotFoundException
    {
      try {
        MethodInfo info = _expr.getMethodInfo(context.getELContext());

        return info.getReturnType();
      } catch (javax.el.MethodNotFoundException e) {
        throw new javax.faces.el.MethodNotFoundException(e);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new EvaluationException(e);
      }
    }

    public String toString()
    {
      return "MethodBindingAdapter[" + _expr.getExpressionString() + "]";
    }
  }

  static class ActionListenerAdapter implements ActionListener, StateHolder {
    private MethodBinding _binding;

    ActionListenerAdapter()
    {
    }

    ActionListenerAdapter(MethodBinding binding)
    {
      _binding = binding;
    }

    MethodBinding getBinding()
    {
      return _binding;
    }

    public void processAction(ActionEvent event)
    {
      _binding.invoke(FacesContext.getCurrentInstance(),
                      new Object[] { event });
    }
    
    public Object saveState(FacesContext context)
    {
      return _binding.getExpressionString();
    }

    public void restoreState(FacesContext context, Object state)
    {
      Application app = context.getApplication();
      
      String expr = (String) state;

      _binding = app.createMethodBinding(expr, new Class[] { ActionEvent.class });
    }

    public boolean isTransient()
    {
      return false;
    }

    public void setTransient(boolean isTransient)
    {
    }

    public String toString()
    {
      return "ActionListenerAdapter[" + _binding + "]";
    }
  }

  static {
    _propMap.put("value", PropEnum.VALUE);
  }
}
