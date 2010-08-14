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
import java.sql.ResultSet;

import javax.el.*;

import javax.faces.context.*;
import javax.faces.event.*;
import javax.faces.model.*;
import javax.faces.FacesException;

import javax.servlet.jsp.jstl.sql.Result;

public class UIData extends UIComponentBase
  implements NamingContainer
{
  public static final String COMPONENT_FAMILY = "javax.faces.Data";
  public static final String COMPONENT_TYPE = "javax.faces.Data";

  private static final HashMap<String,PropEnum> _propMap
    = new HashMap<String,PropEnum>();

  private static final Object[] NULL_ARRAY = new Object[0];

  private DataModel _dataModel;
  private Object _dataModelValue;

  private Integer _first;
  private ValueExpression _firstExpr;

  private Integer _rows;
  private ValueExpression _rowsExpr;

  private Object _value;
  private ValueExpression _valueExpr;

  private String _var;
  private int _rowIndex = -1;

  private ArrayList<ArrayList<State>> _state;

  public UIData()
  {
    setRendererType("javax.faces.Table");
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

  public int getFirst()
  {
    if (_first != null)
      return _first;
    else if (_firstExpr != null)
      return Util.evalInt(_firstExpr, getFacesContext());
    else
      return 0;
  }

  public void setFirst(int first)
  {
    if (first < 0)
      throw new IllegalArgumentException("UIData.setFirst must have a positive value at '" + first + "'");
    _first = first;
  }

  public int getRows()
  {
    int rows;
    
    if (_rows != null)
      rows = _rows;
    else if (_rowsExpr != null)
      rows = Util.evalInt(_rowsExpr, getFacesContext());
    else
      rows = 0;

     return rows;
  }

  public void setRows(int rows)
  {
    if (rows < 0)
      throw new IllegalArgumentException("UIData.setFirst must have a positive value at '" + rows + "'");
    _rows = rows;
  }

  public String getVar()
  {
    return _var;
  }

  public void setVar(String var)
  {
    _var = var;
  }

  public Object getValue()
  {
    Object value;
    
    if (_value != null)
      value = _value;
    else if (_valueExpr != null)
      value = Util.eval(_valueExpr, getFacesContext());
    else
      value = null;
    
    return value;
  }

  public void setValue(Object value)
  {
    _value = value;
    setDataModel(null);
    
  }

  protected DataModel getDataModel()
  {
    if (_dataModel != null)
      return _dataModel;

    _dataModel = createDataModel(getValue());
    
    return _dataModel;
  }

  protected void setDataModel(DataModel dataModel)
  {
    _dataModel = dataModel;
    _state = null;
  }

  private void resetDataModel()
  {
    Object value = getValue();

    if (value != _dataModelValue)
      setDataModel(null);
  }

  private DataModel createDataModel(Object value)
  {
    _dataModelValue = value;
    
    if (value == null)
      return new ArrayDataModel(NULL_ARRAY);
    else if (value instanceof DataModel)
      return (DataModel) value;
    else if (value instanceof List)
      return new ListDataModel((List) value);
    else if (value instanceof ResultSet)
      return new ResultSetDataModel((ResultSet) value);
    else if (value instanceof Result)
      return new ResultDataModel((Result) value);
    else if (value.getClass().isArray())
      return new ArrayDataModel((Object []) value);
    else
      return new ScalarDataModel(value);
  }

  public int getRowIndex()
  {
    return _rowIndex;
  }

  public Object getRowData()
  {
    return getDataModel().getRowData();
  }

  public void setRowIndex(int value)
  {
    if (value < -1)
      throw new IllegalArgumentException("UIData.setRowIndex must not be less than -1 at '" + value + "'");
    
    DataModel dataModel = getDataModel();

    int oldRow = _rowIndex;
    
    _rowIndex = value;

    dataModel.setRowIndex(value);

    if (dataModel.isRowAvailable())
      setRowIndexState(dataModel, oldRow, _rowIndex);
    else
      setRowIndexState(dataModel, oldRow, -1);

    if (_var == null) {
    }
    else if (value >= 0 && dataModel.isRowAvailable()) {
      Object rowData = dataModel.getRowData();
      
      FacesContext context = FacesContext.getCurrentInstance();
      
      context.getExternalContext().getRequestMap().put(_var, rowData);
    }
    else {
      FacesContext context = FacesContext.getCurrentInstance();

      context.getExternalContext().getRequestMap().remove(_var);
    }
  }

  private void setRowIndexState(DataModel model, int oldRow, int newRow)
  {
    if (_state == null)
      _state = new ArrayList<ArrayList<State>>();
    
    setRowIndexState(this, oldRow, newRow, false, 0);
  }

  private int setRowIndexState(UIComponent comp,
                               int oldRow,
                               int newRow,
                               boolean isTransient,
                               int valueIndex)
  {
    //skip self
    if (comp != this)
      comp.setId(comp.getId());

    if (comp.isTransient())
      isTransient = true;
    else if (comp instanceof EditableValueHolder) {
      EditableValueHolder holder = (EditableValueHolder) comp;
      
      if (oldRow >= 0) {
        ArrayList<State> oldList;

        while (_state.size() <= oldRow)
          _state.add(null);

        oldList = _state.get(oldRow);

        if (oldList == null) {
          oldList = new ArrayList<State>();

          _state.set(oldRow, oldList);
        }

        while (oldList.size() < (valueIndex + 1))
          oldList.add(null);

        State state = oldList.get(valueIndex);

        if (state != null)
          state = state.update(holder);
        else
          state = new State(holder);

        oldList.set(valueIndex, state);
      }

      ArrayList<State> newList = null;

      if (newRow >= 0 && newRow < _state.size())
        newList = _state.get(newRow);

      State state;
      
      if (newList != null && valueIndex < newList.size())
        state = newList.get(valueIndex);
      else
        state = null;

      if (state != null)
        state.restore(holder);
      else {
        holder.setSubmittedValue(null);
        holder.setValue(null);
        holder.setLocalValueSet(false);
        holder.setValid(true);
      }

      valueIndex += 1;
    }
    
    if (comp instanceof UIComponentBase) {
      UIComponentBase base = (UIComponentBase) comp;
      
      for (UIComponent child : base.getFacetsAndChildrenArray()) {
        valueIndex = setRowIndexState(child, oldRow, newRow,
                                      isTransient, valueIndex);
      }
    }
    else {
      Iterator<UIComponent> iter = comp.getFacetsAndChildren();

      while (iter.hasNext()) {
        UIComponent child = iter.next();

        valueIndex = setRowIndexState(child, oldRow, newRow,
                                      isTransient, valueIndex);
      }
    }

    return valueIndex;
  }

  public int getRowCount()
  {
    DataModel model = getDataModel();

    if (model != null)
      return model.getRowCount();
    else
      return -1;
  }

  public boolean isRowAvailable()
  {
    DataModel model = getDataModel();

    if (model != null)
      return model.isRowAvailable();
    else
      return false;
  }

  /**
   * Returns the value expression with the given name.
   */
  @Override
  public ValueExpression getValueExpression(String name)
  {
    PropEnum prop = _propMap.get(name);

    if (prop != null) {
      switch (_propMap.get(name)) {
      case VALUE:
        return _valueExpr;
      
      case FIRST:
        return _firstExpr;
      
      case ROWS:
        return _rowsExpr;
      }
    }

    return super.getValueExpression(name);
  }

  /**
   * Sets the value expression with the given name.
   */
  @Override
  public void setValueExpression(String name, ValueExpression expr)
  {
    PropEnum prop = _propMap.get(name);

    if (prop != null) {
      switch (_propMap.get(name)) {
      case VALUE:
        _dataModel = null;
        if (expr != null && expr.isLiteralText()) {
          _value = expr.getValue(null);
          return;
        }
        else
          _valueExpr = expr;
        break;
      
      case FIRST:
        if (expr != null && expr.isLiteralText()) {
          _first = (Integer) expr.getValue(null);
          return;
        }
        else
          _firstExpr = expr;
        break;
      
      case ROWS:
        if (expr != null && expr.isLiteralText()) {
          _rows = (Integer) expr.getValue(null);
          return;
        }
        else
          _rowsExpr = expr;
        break;
      }
    }

    super.setValueExpression(name, expr);
  }

  //
  // Facets
  //

  public UIComponent getHeader()
  {
    return getFacet("header");
  }

  public void setHeader(UIComponent header)
  {
    getFacets().put("header", header);
  }

  public UIComponent getFooter()
  {
    return getFacet("footer");
  }

  public void setFooter(UIComponent footer)
  {
    getFacets().put("footer", footer);
  }

  @Override
  public boolean invokeOnComponent(FacesContext context,
                                   String clientId,
                                   ContextCallback callback)
    throws FacesException
  {
    if (context == null || clientId == null || callback == null)
      throw new NullPointerException();


    if (clientId.equals(this.getClientId(context))) {
      try {
        callback.invokeContextCallback(context, this);

        return true;
      }
      catch (Exception e) {
        throw new FacesException(e);
      }
    }

    String head = getClientId(context) + SEPARATOR_CHAR;

    int oldIdx = getRowIndex();

    String tail = Character.toString(SEPARATOR_CHAR) + oldIdx + SEPARATOR_CHAR; 

    if (head.endsWith(tail))
      head = head.substring(0, head.length() - tail.length() + 1);

    if (! clientId.startsWith(head))
      return false;

    int separatorIdx = clientId.indexOf(SEPARATOR_CHAR, head.length());

    int newIdx;

    try {
      newIdx = Integer.parseInt(clientId.substring(head.length(), separatorIdx));
    }
    catch (Exception e) {
      throw new FacesException("clientId '" +
                               clientId +
                               "' is expected to contain a positive integer at position '" +
                               head.length() +
                               "'");
    } 

    try {
      setRowIndex(newIdx);

      if (! isRowAvailable())
        return false;

      for (Iterator<UIComponent> it = getFacetsAndChildren(); it.hasNext();) {
        if (it.next().invokeOnComponent(context, clientId, callback))
          return true;
      }
    } catch (Exception e) {
      throw new FacesException(e);
    } finally {
      this.setRowIndex(oldIdx);
    }

    return false;
  }

  //
  // overrides

  /**
   * Returns the client-specific id for the component.
   */
  @Override
  public String getClientId(FacesContext context)
  {
    String clientId = super.getClientId(context);

    int rowIndex = getRowIndex();

    if (rowIndex < 0)
      return clientId;
    else
      return clientId + SEPARATOR_CHAR + rowIndex;
  }

  /**
   * Queues the event, wrapping the rowIndex.
   */
  @Override
  public void queueEvent(FacesEvent event)
  {
    int rowIndex = getRowIndex();

    super.queueEvent(new UIDataEventWrapper(event, this, rowIndex));
  }

  /**
   * Broadcasts the event, unwrapping the rowIndex.
   */
  @Override
  public void broadcast(FacesEvent event)
    throws AbortProcessingException
  {
    if (event instanceof UIDataEventWrapper) {
      UIDataEventWrapper wrapper = (UIDataEventWrapper) event;

      event = wrapper.getEvent();
      
      int oldIndex = getRowIndex();
      setRowIndex(wrapper.getRowIndex());

      event.getComponent().broadcast(event);
      
      setRowIndex(oldIndex);
    }
    else
      super.broadcast(event);
  }

  /**
   * Recursively calls the decodes for any children, then calls
   * decode().
   */
  @Override
  public void processDecodes(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    if (! isRendered())
      return;

    setRowIndex(-1);

    if (getFacetCount() > 0) {
      for (UIComponent facet : getFacets().values()) {
        facet.processDecodes(context);
      }
    }

    int childCount = getChildCount();

    if (childCount > 0) {
      List<UIComponent> children = getChildren();

      for (int i = 0; i < children.size(); i++) {
        UIComponent child = children.get(i);

        if (! child.isRendered() && child.getFacetCount() == 0)
          continue;

        for (UIComponent facet : child.getFacets().values()) {
          facet.processDecodes(context);
        }
      }
      
      int first = getFirst();
      int rows = getRows();

      if (rows <= 0)
        rows = Integer.MAX_VALUE;
      
      for (int i = 0; i < rows; i++) {
        setRowIndex(first + i);

        if (! isRowAvailable())
          break;

        for (int j = 0; j < childCount; j++) {
          UIComponent child = children.get(j);

          if (! child.isRendered())
            continue;

          int grandchildCount = child.getChildCount();

          if (grandchildCount > 0) {
            List<UIComponent> grandchildren = child.getChildren();

            for (int k = 0; k < grandchildCount; k++) {
              grandchildren.get(k).processDecodes(context);
            }
          }

          child.decode(context);
        }
      }
    }

    setRowIndex(-1);
    
    decode(context);
  }

  /**
   * Recursively calls the validators for any children, then calls
   * decode().
   */
  @Override
  public void processValidators(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    if (! isRendered())
      return;

    setRowIndex(-1);

    if (getFacetCount() > 0) {
      for (UIComponent facet : getFacets().values()) {
        facet.processValidators(context);
      }
    }

    int childCount = getChildCount();

    if (childCount > 0) {
      List<UIComponent> children = getChildren();

      for (int i = 0; i < children.size(); i++) {
        UIComponent child = children.get(i);

        if (! child.isRendered() && child.getFacetCount() == 0)
          continue;

        for (UIComponent facet : child.getFacets().values()) {
          facet.processValidators(context);
        }
      }
      
      int first = getFirst();
      int rows = getRows();

      if (rows <= 0)
        rows = Integer.MAX_VALUE;

      for (int i = 0; i < rows; i++) {
        setRowIndex(first + i);

        if (! isRowAvailable())
          break;

        for (int j = 0; j < childCount; j++) {
          UIComponent child = children.get(j);

          if (! child.isRendered())
            continue;

          int grandchildCount = child.getChildCount();
          List<UIComponent> grandchildren = child.getChildren();

          for (int k = 0; k < grandchildCount; k++) {
            grandchildren.get(k).processValidators(context);
          }
        }
      }
    }

    setRowIndex(-1);
  }

  /**
   * Recursively calls the updates for any children, then calls
   * decode().
   */
  @Override
  public void processUpdates(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    if (! isRendered())
      return;

    setRowIndex(-1);

    if (getFacetCount() > 0) {
      for (UIComponent facet : getFacets().values()) {
        facet.processUpdates(context);
      }
    }

    int childCount = getChildCount();

    if (childCount > 0) {
      List<UIComponent> children = getChildren();

      for (int i = 0; i < children.size(); i++) {
        UIComponent child = children.get(i);

        if (! child.isRendered() && child.getFacetCount() == 0)
          continue;

        for (UIComponent facet : child.getFacets().values()) {
          facet.processUpdates(context);
        }
      }
      
      int first = getFirst();
      int rows = getRows();

      if (rows <= 0)
        rows = Integer.MAX_VALUE;

      for (int i = 0; i < rows; i++) {
        setRowIndex(first + i);

        if (! isRowAvailable())
          break;

        for (int j = 0; j < childCount; j++) {
          UIComponent child = children.get(j);

          if (! child.isRendered())
            continue;

          int grandchildCount = child.getChildCount();
          List<UIComponent> grandchildren = child.getChildren();

          for (int k = 0; k < grandchildCount; k++) {
            grandchildren.get(k).processUpdates(context);
          }
        }
      }
    }

    setRowIndex(-1);
  }

  /**
   * Recursively calls the encodes for any children, then calls
   * decode().
   */
  @Override
  public void encodeBegin(FacesContext context)
    throws java.io.IOException
  {
    if (context == null)
      throw new NullPointerException();

    if (! isRendered())
      return;

    resetDataModel();

    if (! context.getRenderResponse() || ! context.getResponseComplete())
      _state = null;

    super.encodeBegin(context);
  }

  //
  // state
  //

  public Object saveState(FacesContext context)
  {
    return new Object[] {
      super.saveState(context),

      _value,
      _first,
      _rows,
      _var,
    };
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;
    
    super.restoreState(context, state[0]);

    _value = state[1];
    _first = (Integer) state[2];
    _rows = (Integer) state[3];
    _var = (String) state[4];
  }

  //
  // inner classes
  //

  static class UIDataEventWrapper extends FacesEvent
  {
    private FacesEvent _event;
    private int _rowIndex;

    UIDataEventWrapper(FacesEvent event, UIData component, int rowIndex)
    {
      super(component);

      _event = event;
      _rowIndex = rowIndex;
    }

    FacesEvent getEvent()
    {
      return _event;
    }

    int getRowIndex()
    {
      return _rowIndex;
    }

    public void setPhaseId(PhaseId phaseId)
    {
      _event.setPhaseId(phaseId);
    }

    public PhaseId getPhaseId()
    {
      return _event.getPhaseId();
    }

    public boolean isAppropriateListener(FacesListener listener)
    {
      return _event.isAppropriateListener(listener);
    }

    public void processListener(FacesListener listener)
      throws AbortProcessingException
    {
      ((UIData) getComponent()).setRowIndex(_rowIndex);

      _event.processListener(listener);
    }
  }

  static class State implements java.io.Serializable
  {
    private final Object _submittedValue;
    private final Object _value;
    private final boolean _isLocal;
    private final boolean _isValid;

    State()
    {
      _submittedValue = null;
      _value = null;
      _isLocal = false;
      _isValid = false;
    }
      
    State(EditableValueHolder holder)
    {
      _submittedValue = holder.getSubmittedValue();
      _value = holder.getValue();
      _isLocal = holder.isLocalValueSet();
      _isValid = holder.isValid();
    }

    State update(EditableValueHolder holder)
    {
      if (_submittedValue == holder.getSubmittedValue()
          && _value == holder.getValue()
          && _isLocal == holder.isLocalValueSet()
          && _isValid == holder.isValid())
        return this;
      else
        return new State(holder);
    }

    void restore(EditableValueHolder holder)
    {
      holder.setSubmittedValue(_submittedValue);
      holder.setValue(_value);
      holder.setLocalValueSet(_isLocal);
      holder.setValid(_isValid);
    }
  }

  //
  // private helpers
  //

  private static enum PropEnum {
    VALUE,
    FIRST,
    ROWS,
  }

  static {
    _propMap.put("value", PropEnum.VALUE);
    _propMap.put("first", PropEnum.FIRST);
    _propMap.put("rows", PropEnum.ROWS);
  }
}
