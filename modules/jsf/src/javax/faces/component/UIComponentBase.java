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

import java.beans.*;
import java.lang.reflect.*;
import java.lang.annotation.Annotation;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.render.*;

public abstract class UIComponentBase extends UIComponent
{
  private static final UIComponent []NULL_FACETS_AND_CHILDREN
    = new UIComponent[0];
  
  private static final FacesListener []NULL_FACES_LISTENERS
    = new FacesListener[0];

  private static final HashMap<String,Integer> _rendererToCodeMap
    = new HashMap<String,Integer>();

  private static final HashMap<Integer,String> _codeToRendererMap
    = new HashMap<Integer,String>();
  
  private static final WeakHashMap<Class,HashMap<String,Property>> _compMap
    = new WeakHashMap<Class,HashMap<String,Property>>();

  private String _id;
  private String _clientId;

  private UIComponent _parent;
  
  private String _rendererType;
  private ValueExpression _rendererTypeExpr;
  
  private boolean _isTransient;
  
  private Boolean _isRendered;
  private ValueExpression _isRenderedExpr;
  
  private ValueExpression _bindingExpr;

  private ComponentList _children;
  private ComponentMap _facets;

  private UIComponent []_facetsAndChildren;

  private AttributeMap _attributeMap;
  

  
  private FacesListener []_facesListeners
    = NULL_FACES_LISTENERS;
  
  public Map<String,Object> getAttributes()
  {
    if (_attributeMap == null)
      _attributeMap = new AttributeMap(this);

    return _attributeMap;
  }
  
  /**
   * @deprecated
   */
  public ValueBinding getValueBinding(String name)
  {
    ValueExpression expr = getValueExpression(name);

    if (expr == null)
      return null;
    else if (expr instanceof ValueExpressionAdapter)
      return ((ValueExpressionAdapter) expr).getBinding();
    else
      return new ValueBindingAdapter(expr);
  }

  /**
   * @deprecated
   */
  public void setValueBinding(String name, ValueBinding binding)
  {
    setValueExpression(name, new ValueExpressionAdapter(binding));
  }

  /**
   * Returns the value expression for an attribute
   *
   * @param name the name of the attribute to get
   */
  @Override
  public ValueExpression getValueExpression(String name)
  {
    if (name == null)
      throw new NullPointerException();

    if ("rendered".equals(name))
      return _isRenderedExpr;
    else if ("rendererType".equals(name))
      return _rendererTypeExpr;
    else if ("binding".equals(name))
      return _bindingExpr;
    
    if (bindings != null)
      return bindings.get(name);
    else
      return null;
  }

  /**
   * Sets the value expression for an attribute
   *
   * @param name the name of the attribute to set
   * @param expr the value expression
   */
  @Override
  public void setValueExpression(String name, ValueExpression expr)
  {
    if (name.equals("id"))
      throw new IllegalArgumentException("'id' is not a valid ValueExpression name.");
    else if (name.equals("parent"))
      throw new IllegalArgumentException("'parent' is not a valid ValueExpression name.");
    
    if ("rendered".equals(name)) {
      if (expr.isLiteralText()) {
        _isRendered = Util.booleanValueOf(expr.getValue(null));
        return;
      }
      else
        _isRenderedExpr = expr;
    }
    else if ("rendererType".equals(name)) {
      if (expr.isLiteralText()) {
        _rendererType = String.valueOf(expr.getValue(null));
        return;
      }
      else
        _rendererTypeExpr = expr;
    }
    else if ("binding".equals(name)) {
      _bindingExpr = expr;
    }

    try {
      if (expr != null) {
        if (expr.isLiteralText()) {
          getAttributes().put(name, expr.getValue(null));
        }
        else {
          if (bindings == null)
            bindings = new HashMap<String,ValueExpression>();

          bindings.put(name, expr);
        }
      }
      else if (bindings != null)
        bindings.remove(name);
    } catch (ELException e) {
      throw new FacesException(e);
    }
  }

  /**
   * Returns the client-specific id for the component.
   */
  @Override
  public String getClientId(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    if (_clientId != null)
      return _clientId;

    String parentId = null;

    for (UIComponent ptr = getParent(); ptr != null; ptr = ptr.getParent()) {
      if (ptr instanceof NamingContainer) {
        parentId = ptr.getContainerClientId(context);
        break;
      }
    }

    String myId = _id;

    if (myId == null) {
      myId = context.getViewRoot().createUniqueId();
    }

    if (parentId != null)
      myId = parentId + NamingContainer.SEPARATOR_CHAR + myId;
    
    Renderer renderer = getRenderer(context);
    
    if (renderer != null)
      _clientId = renderer.convertClientId(context, myId);
    else
      _clientId = myId;

    return _clientId;
  }

  public String getId()
  {
    return _id;
  }

  public void setId(String id)
  {
    if (id == null) {
      _id = null;
      _clientId = null;
      return;
    }
    
    int len = id.length();

    if (len == 0)
      throw new IllegalArgumentException();

    char ch = id.charAt(0);

    if (! ('a' <= ch && ch <= 'z' || 'A' <= ch && ch <= 'Z' || ch == '_'))
      throw new IllegalArgumentException();

    for (int i = 1; i < len; i++) {
      ch = id.charAt(i);
      
      if (! ('a' <= ch && ch <= 'z'
             || 'A' <= ch && ch <= 'Z'
             || '0' <= ch && ch <= '9'
             || ch == '_'
             || ch == '-'))
        throw new IllegalArgumentException();
    }

    _id = id;
    _clientId = null;
  }

  public UIComponent getParent()
  {
    return _parent;
  }

  public void setParent(UIComponent parent)
  {
    _parent = parent;
  }

  public boolean isRendered()
  {
    if (_isRendered != null)
      return _isRendered;
    else if (_isRenderedExpr != null)
      return Util.evalBoolean(_isRenderedExpr, getFacesContext());
    else
      return true;
  }

  public void setRendered(boolean isRendered)
  {
    _isRendered = isRendered;
  }

  public String getRendererType()
  {
    if (_rendererType != null)
      return _rendererType;
    else if (_rendererTypeExpr != null)
      return Util.evalString(_rendererTypeExpr, getFacesContext());
    else
      return null;
  }

  public void setRendererType(String rendererType)
  {
    _rendererType = rendererType;
  }

  public boolean getRendersChildren()
  {
    Renderer renderer = getRenderer(getFacesContext());

    if (renderer != null)
      return renderer.getRendersChildren();
    else
      return false;
  }

  public List<UIComponent> getChildren()
  {
    if (_children == null)
      _children = new ComponentList(this);

    return _children;
  }

  public int getChildCount()
  {
    if (_children != null)
      return _children.size();
    else
      return 0;
  }

  public UIComponent findComponent(String expr)
  {
    UIComponent base = null;

    String []values = expr.split(":");
    
    if (values[0].equals("")) {
      for (base = this; base.getParent() != null; base = base.getParent()) {
      }
    }
    else {
      for (base = this;
           base.getParent() != null && ! (base instanceof NamingContainer);
           base = base.getParent()) {
      }
    }

    for (int i = 0; i < values.length; i++) {
      String v = values[i];

      if ("".equals(v))
        continue;

      base = findComponent(base, v);

      if (i + 1 == values.length)
        return base;

      if (base == null)
        return base;

      if (! (base instanceof NamingContainer)) {
        throw new IllegalArgumentException("'" + v + "' in expression '" + expr + "' does not match an intermediate NamingContainer.");
      }
    }
    
    return base;
  }

  private static UIComponent findComponent(UIComponent comp, String id)
  {
    if (id.equals(comp.getId()))
      return comp;

    Iterator iter = comp.getFacetsAndChildren();
    while (iter.hasNext()) {
      UIComponent child = (UIComponent) iter.next();

      if (id.equals(child.getId()))
        return child;
      
      if (! (child instanceof NamingContainer)) {
        UIComponent desc = findComponent(child, id);

        if (desc != null)
          return desc;
      }
    }

    return null;
  }

  public Map<String,UIComponent> getFacets()
  {
    if (_facets == null)
      _facets = new ComponentMap(this);

    return _facets;
  }

  public UIComponent getFacet(String name)
  {
    if (_facets != null)
      return _facets.get(name);
    else
      return null;
  }

  public Iterator<UIComponent> getFacetsAndChildren()
  {
    return new FacetAndChildIterator(getFacetsAndChildrenArray());
  }

  UIComponent []getFacetsAndChildrenArray()
  {
    if (_facetsAndChildren == null) {
      if (_children == null && _facets == null)
        _facetsAndChildren = NULL_FACETS_AND_CHILDREN;
      else {
        int facetCount = getFacetCount();
        int childCount = getChildCount();

        _facetsAndChildren = new UIComponent[facetCount + childCount];

        int i = 0;
        if (_facets != null) {
          for (UIComponent facet : _facets.values()) {
            _facetsAndChildren[i++] = facet;
          }
        }

        for (int j = 0; j < childCount; j++) {
          _facetsAndChildren[i++] = _children.get(j);
        }
      }
    }

    return _facetsAndChildren;
  }

  //
  // Listeners, broadcast and event handling
  //

  protected void addFacesListener(FacesListener listener)
  {
    if (listener == null)
      throw new NullPointerException();
    
    int length = _facesListeners.length;
    
    FacesListener[] newListeners = new FacesListener[length + 1];

    System.arraycopy(_facesListeners, 0, newListeners, 0, length);

    newListeners[length] = listener;

    _facesListeners = newListeners;
  }

  protected FacesListener []getFacesListeners(Class cl)
  {
    if (FacesListener.class.equals(cl))
      return _facesListeners;

    int count = 0;
    for (int i = _facesListeners.length - 1; i >= 0; i--) {
      if (cl.isAssignableFrom(_facesListeners[i].getClass()))
        count++;
    }

    FacesListener []array = (FacesListener []) Array.newInstance(cl, count);
    count = 0;
    for (int i = _facesListeners.length - 1; i >= 0; i--) {
      if (cl.isAssignableFrom(_facesListeners[i].getClass())) {
        array[count++] = _facesListeners[i];
      }
    }

    return array;
  }

  protected void removeFacesListener(FacesListener listener)
  {
    if (listener == null)
      throw new NullPointerException();

    int length = _facesListeners.length;
    for (int i = 0; i < length; i++) {
      if (listener.equals(_facesListeners[i])) {
        FacesListener []newListeners = new FacesListener[length - 1];
        System.arraycopy(_facesListeners, 0, newListeners, 0, i);
        System.arraycopy(_facesListeners, i + 1, newListeners, i,
                         length - i - 1);

        _facesListeners = newListeners;

        return;
      }
    }
  }

  public void queueEvent(FacesEvent event)
  {
    UIComponent parent = getParent();

    if (parent != null)
      parent.queueEvent(event);
    else
      throw new IllegalStateException();
  }

  public void broadcast(FacesEvent event)
    throws AbortProcessingException
  {
    for (int i = 0; i < _facesListeners.length; i++) {
      if (event.isAppropriateListener(_facesListeners[i])) {
        event.processListener(_facesListeners[i]);
      }
    }
  }

  //
  // decoding
  //

  /**
   * Recursively calls the decodes for any children, then calls
   * decode().
   */
  public void processDecodes(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    if (! isRendered())
      return;

    for (UIComponent child : getFacetsAndChildrenArray()) {
      child.processDecodes(context);
    }

    try {
      decode(context);
    } catch (RuntimeException e) {
      context.renderResponse();

      throw e;
    }
  }

  /**
   * Decodes the value of the component.
   */
  @Override
  public void decode(FacesContext context)
  {
    Renderer renderer = getRenderer(context);

    if (renderer != null)
      renderer.decode(context, this);
  }

  //
  // Validation
  //

  @Override
  public void processValidators(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    if (! isRendered())
      return;

    for (UIComponent child : getFacetsAndChildrenArray()) {
      child.processValidators(context);
    }
  }

  //
  // Model updates
  //

  public void processUpdates(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    if (! isRendered())
      return;

    for (UIComponent child : getFacetsAndChildrenArray()) {
      child.processUpdates(context);
    }
  }

  //
  // Encoding
  //

  /**
   * Starts the output rendering for the encoding.
   */
  public void encodeBegin(FacesContext context)
    throws IOException
  {
    if (context == null)
      throw new NullPointerException();

    if (! isRendered())
      return;

    try {
      Renderer renderer = getRenderer(context);

      if (renderer != null)
        renderer.encodeBegin(context, this);

    } catch (IOException e) {
      if (e.getMessage().startsWith("id="))
        throw e;
      else
        throw new IOExceptionWrapper("id=" + getClientId(context)
                                     + " " + e.toString(), e);
    } catch (RuntimeException e) {
      if (e.getMessage() != null
          && e.getMessage().startsWith("id="))
        throw e;
      else
        throw new FacesException("id=" + getClientId(context)
                                 + " " + e.toString(), e);
    }
  }

  public void encodeChildren(FacesContext context)
    throws IOException
  {
    if (context == null)
      throw new NullPointerException();

    if (! isRendered())
      return;

    Renderer renderer = getRenderer(context);

    if (renderer != null)
      renderer.encodeChildren(context, this);
  }

  public void encodeEnd(FacesContext context)
    throws IOException
  {
    if (context == null)
      throw new NullPointerException();

    if (! isRendered())
      return;

    Renderer renderer = getRenderer(context);

    if (renderer != null)
      renderer.encodeEnd(context, this);
  }

  @Override
  protected FacesContext getFacesContext()
  {
    return FacesContext.getCurrentInstance();
  }

  @Override
  protected Renderer getRenderer(FacesContext context)
  {
    String rendererType = getRendererType();

    if (rendererType == null)
      return null;
    
    RenderKit renderKit = context.getRenderKit();

    if (renderKit != null)
      return renderKit.getRenderer(getFamily(), getRendererType());
    else
      return null;
  }

  //
  // Save the state of the component
  //

  public Object processSaveState(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    if (isTransient())
      return null;
    
    UIComponent []facetsAndChildren = getFacetsAndChildrenArray();

    Object []childSaveState = null;

    int childSize = getChildCount();
    int facetSize = getFacetCount();
    int k = 1;
      
    if (childSize > 0) {
      List<UIComponent> children = getChildren();
      
      for (int i = 0; i < childSize; i++) {
        UIComponent child = children.get(i);

        if (child.isTransient())
          continue;

        k++;

        Object childState = child.processSaveState(context);

        if (childState != null) {
          if (childSaveState == null)
            childSaveState = new Object[1 + childSize + 2 * facetSize];
      
          childSaveState[k - 1] = childState;
        }
      }
    }

    if (facetSize > 0) {
      Map<String,UIComponent> facetMap = getFacets();

      for (Map.Entry<String,UIComponent> entry : facetMap.entrySet()) {
        UIComponent facet = entry.getValue();

        if (facet.isTransient())
          continue;

        k += 2;

        Object facetState = facet.processSaveState(context);

        if (facetState != null) {
          if (childSaveState == null)
            childSaveState = new Object[1 + childSize + 2 * facetSize];
      
          childSaveState[k - 2] = entry.getKey();
          childSaveState[k - 1] = facetState;
        }
      }
    }

    Object selfSaveState = saveState(context);

    if (childSaveState != null) {
      childSaveState[0] = selfSaveState;
      return childSaveState;
    }
    else
      return new Object[] { selfSaveState };
  }

  public void processRestoreState(FacesContext context,
                                  Object state)
  {
    if (context == null)
      throw new NullPointerException();

    if (isTransient())
      return;

    UIComponent []facetsAndChildren = getFacetsAndChildrenArray();

    Object []baseState = (Object []) state;

    if (baseState == null)
      return;

    restoreState(context, baseState[0]);

    if (baseState.length == 1)
      return;

    int childSize = getChildCount();
    int facetSize = getFacetCount();
    int k = 1;
      
    if (childSize > 0) {
      List<UIComponent> children = getChildren();
      
      for (int i = 0; i < childSize; i++) {
        UIComponent child = children.get(i);

        if (child.isTransient()) {
          continue;
        }

        k++;

        Object childState;

        if (k <= baseState.length)
          childState = baseState[k - 1];
        else
          childState = null;

        if (childState != null)
          child.processRestoreState(context, childState);
      }
    }
      
    if (facetSize > 0) {
      Map<String,UIComponent> facetMap = getFacets();

      for (; k < baseState.length; k += 2) {
        String facetName = (String) baseState[k];
        Object facetState = baseState[k + 1];

        if (facetName != null && facetState != null) {
          UIComponent facet = facetMap.get(facetName);

          if (facet != null)
            facet.processRestoreState(context, facetState);
        }
      }
    }
  }
  
  public Object saveState(FacesContext context)
  {
    Integer rendererCode = _rendererToCodeMap.get(_rendererType);
    String rendererString = null;

    if (rendererCode == null)
      rendererString = _rendererType;

    Object []savedListeners = saveListeners(context);

    Object []savedBindings = saveBindings(context);
    
    return new Object[] {
      _id,
      savedBindings,
      _isRendered,
      rendererCode,
      rendererString,
      (_attributeMap != null ? _attributeMap.saveState(context) : null),
      savedListeners,
    };
  }

  private Object []saveBindings(FacesContext context) {
    if (bindings == null)
      return null;

    Set<String> keys = bindings.keySet();
    Object [] result = new Object [keys.size() * 2];

    int index = 0;
    for (String key : keys) {
      result [index++] = key;

      ValueExpression valueExpression = bindings.get(key);

      result [index++] = saveAttachedState(context, valueExpression);
    }

    return result;
  }

  private void restoreBindings(FacesContext context, Object stateObj) {
    Object [] state = (Object []) stateObj;

    if (state.length == 0) return;

    bindings = new HashMap<String, ValueExpression>();


    for (int i = 0; i < state.length / 2; i++) {
      int index = i * 2;

      String key = (String) state[index];

      ValueExpression valueExpression 
        = (ValueExpression) restoreAttachedState(context, state [index + 1]);

      bindings.put(key, valueExpression);
    }
  }

  private Object []saveListeners(FacesContext context)
  {
    if (_facesListeners.length > 0) {
      Object []savedListeners = new Object[2 * _facesListeners.length];

      for (int i = 0; i < _facesListeners.length; i++) {
        FacesListener listener = _facesListeners[i];

        int index = 2 * i;

        if (listener instanceof java.io.Serializable) {
          savedListeners[index] = java.io.Serializable.class;
          savedListeners[index + 1] = listener;
        } else if (listener instanceof StateHolder) {
          savedListeners[index] = listener.getClass();
          StateHolder holder = (StateHolder) listener;
          savedListeners[index + 1] = holder.saveState(context);
        } else {
          savedListeners[index] = listener.getClass();
        }
      }

      return savedListeners;
    }

    return null;
  }

  public void restoreState(FacesContext context, Object stateObj)
  {
    Object []state = (Object []) stateObj;

    _id = (String) state[0];

    Object []savedBindings = (Object[]) state[1];

    if (savedBindings != null)
      restoreBindings(context, savedBindings);

    if (bindings != null) {
      for (Map.Entry<String,ValueExpression> entry : bindings.entrySet()) {
        setValueExpression(entry.getKey(), entry.getValue());
      }
    }

    _isRendered = (Boolean) state[2];

    Integer rendererCode = (Integer) state[3];
    String rendererString = (String) state[4];

    if (rendererCode != null)
      _rendererType = _codeToRendererMap.get(rendererCode);
    else
      _rendererType = rendererString;

    Object extMapState = state[5];

    if (extMapState != null) {
      if (_attributeMap == null)
        _attributeMap = new AttributeMap(this);

      _attributeMap.restoreState(context, extMapState);
    }

    Object []savedListeners = (Object []) state[6];

    restoreListeners(context, savedListeners);
  }

  private void restoreListeners(FacesContext context, Object[] savedListeners)
  {
    if (savedListeners != null) {
      _facesListeners = new FacesListener[savedListeners.length / 2];

      for (int i = 0; i < _facesListeners.length; i++) {
        int index = 2 * i;

        Class cl = (Class) savedListeners[index];

        try {
          if (java.io.Serializable.class.equals(cl)) {
            _facesListeners[i] = (FacesListener) savedListeners[index + 1];
          }
          else {
            FacesListener listener = (FacesListener) cl.newInstance();

            if (listener instanceof StateHolder) {
              StateHolder holder = (StateHolder) listener;

              holder.restoreState(context, savedListeners[index + 1]);
            }
            _facesListeners[i] = listener;
          }
        }
        catch (Exception e) {
          throw new FacesException(e);
        }
      }
    }
  }

  private Object saveExprMap(FacesContext context,
                             Map<String,ValueExpression> exprMap)
  {
    if (exprMap == null)
      return null;

    int size = exprMap.size();
    
    Object []values = new Object[3 * size];

    int i = 0;
    for (Map.Entry<String,ValueExpression> entry : exprMap.entrySet()) {
      values[i++] = entry.getKey();

      ValueExpression expr = entry.getValue();
      values[i++] = expr.getExpressionString();
      values[i++] = expr.getExpectedType();
    }

    return values;
  }

  private HashMap<String,ValueExpression>
    restoreExprMap(FacesContext context, Object value)
  {
    if (value == null)
      return null;

    Object []state = (Object[]) value;

    HashMap<String,ValueExpression> map
      = new HashMap<String,ValueExpression>();
    
    Application app = context.getApplication();
    ExpressionFactory exprFactory = app.getExpressionFactory();

    int i = 0;
    while (i < state.length) {
      String key = (String) state[i++];
      String exprString = (String) state[i++];
      Class type = (Class) state[i++];

      ValueExpression expr
        = exprFactory.createValueExpression(context.getELContext(),
                                            exprString,
                                            type);

      map.put(key, expr);
    }

    return map;
  }

  public void setTransient(boolean isTransient)
  {
    _isTransient = isTransient;
  }

  public boolean isTransient()
  {
    return _isTransient;
  }

  private void removeChild(UIComponent child)
  {
    if (_children != null) {
      if (_children.remove(child))
        return;
    }
    
    if (_facets != null) {
      for (Map.Entry<String,UIComponent> entry : _facets.entrySet()) {
        if (entry.getValue() == child) {
          _facets.remove(entry.getKey());
          return;
        }
      }
    }
  }

  public static Object saveAttachedState(FacesContext context,
                                         Object attachedObject)
  {
    if (attachedObject == null)
      return null;
    else if (attachedObject instanceof List) {
      List list = (List) attachedObject;

      ArrayList values = new ArrayList();
      int len = list.size();
      
      for (int i = 0; i < len; i++) {
        values.add(saveAttachedState(context, list.get(i)));
      }

      return values;
    }
    else if (attachedObject instanceof StateHolder)
      return new StateHandle(context, attachedObject);
    else if (attachedObject instanceof Serializable)
      return attachedObject;
    else
      return new StateHandle(context, attachedObject);
  }

  public static Object restoreAttachedState(FacesContext context,
                                            Object stateObject)
  {
    if (stateObject == null)
      return null;
    else if (stateObject instanceof List) {
      List list = (List) stateObject;
      
      ArrayList values = new ArrayList();
      int size = list.size();

      for (int i = 0; i < size; i++) {
        values.add(restoreAttachedState(context, list.get(i)));
      }
      
      return values;
    }
    else if (stateObject instanceof StateHandle)
      return ((StateHandle) stateObject).restore(context);
    else
      return stateObject;
  }
  
  private static class StateHandle implements java.io.Serializable {
    private Class _class;
    private Object _state;

    public StateHandle()
    {
    }

    public StateHandle(FacesContext context, Object value)
    {
      _class = value.getClass();

      if (value instanceof StateHolder)
        _state = ((StateHolder) value).saveState(context);
    }

    public Object restore(FacesContext context)
    {
      try {
        Object value = _class.newInstance();

        if (value instanceof StateHolder)
          ((StateHolder) value).restoreState(context, _state);

        return value;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class ComponentList extends AbstractList<UIComponent>
    implements java.io.Serializable
  {
    private ArrayList<UIComponent> _list = new ArrayList<UIComponent>();
    
    private UIComponentBase _parent;

    ComponentList(UIComponentBase parent)
    {
      _parent = parent;
    }

    @Override
    public boolean add(UIComponent child)
    {
      setParent(child);

      _parent._facetsAndChildren = null;

      boolean result = _list.add(child);

      FacesContext context = FacesContext.getCurrentInstance();

      return result;
    }

    @Override
    public void add(int i, UIComponent child)
    {
      _list.add(i, child);

      setParent(child);

      _parent._facetsAndChildren = null;
    }

    @Override
    public boolean addAll(int i, Collection<? extends UIComponent> list)
    {
      boolean isChange = false;
      
      for (UIComponent child : list) {
        setParent(child);

        _list.add(i++, child);

        isChange = true;
      }

      _parent._facetsAndChildren = null;
      
      return isChange;
    }

    private boolean isPostback(FacesContext context) {
      RenderKitFactory factory
        = (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);

      String renderKitId = context.getViewRoot().getRenderKitId();

      RenderKit renderKit = factory.getRenderKit(context, renderKitId);

      if (renderKit == null)
        renderKit = factory.getRenderKit(context, RenderKitFactory.HTML_BASIC_RENDER_KIT);
      
      return renderKit.getResponseStateManager().isPostback(context);
    }

    @Override
    public UIComponent set(int i, UIComponent child)
    {
      UIComponent old = _list.remove(i);

      if (old != null)
        old.setParent(null);

      setParent(child);

      _list.add(i, child);

      _parent._facetsAndChildren = null;
      
      return old;
    }

    @Override
    public UIComponent remove(int i)
    {
      UIComponent old = _list.remove(i);

      if (old != null) {
        old.setParent(null);
      }

      _parent._facetsAndChildren = null;
      
      return old;
    }

    @Override
    public boolean remove(Object v)
    {
      UIComponent comp = (UIComponent) v;
      
      if (_list.remove(comp)) {
        comp.setParent(null);

        _parent._facetsAndChildren = null;
      
        return true;
      }
      else
        return false;
    }

    @Override
    public UIComponent get(int i)
    {
      return _list.get(i);
    }

    private void setParent(UIComponent child)
    {
      UIComponent parent = child.getParent();

      if (parent == null) {
      }
      else if (parent instanceof UIComponentBase) {
        ((UIComponentBase) parent).removeChild(child);
      }
      else {
        parent.getChildren().remove(child);
      }

      child.setParent(_parent);
    }

    public int size()
    {
      return _list.size();
    }

    public boolean isEmpty()
    {
      return _list.isEmpty();
    }

    public Iterator<UIComponent> iterator()
    {
      return _list.iterator();
    }
  }

  public String toString()
  {
    return getClass().getName() + "[" + getId() + "]";
  }

  private static class ComponentMap extends HashMap<String,UIComponent>
  {
    private UIComponentBase _parent;

    ComponentMap(UIComponentBase parent)
    {
      _parent = parent;
    }

    @Override
    public UIComponent put(String key, UIComponent o)
    {
      if (key == null)
        throw new NullPointerException();

      _parent._facetsAndChildren = null;
      
      UIComponent child = o;

      UIComponent parent = child.getParent();
      if (parent instanceof UIComponentBase) {
        ((UIComponentBase) parent).removeChild(child);
      }

      child.setParent(_parent);

      UIComponent oldChild = super.put(key, o);

      if (oldChild != null && oldChild != o) {
        oldChild.setParent(null);
      }

      return oldChild;
    }

    @Override
    public UIComponent remove(Object key)
    {
      if (key == null)
        throw new NullPointerException();

      _parent._facetsAndChildren = null;

      UIComponent oldChild = super.remove(key);

      if (oldChild != null) {
        oldChild.setParent(null);
      }

      return oldChild;
    }
  }

  private static class FacetAndChildIterator
    implements Iterator<UIComponent> {
    private final UIComponent []_children;
    private int _index;

    FacetAndChildIterator(UIComponent []children)
    {
      _children = children;
    }

    public boolean hasNext()
    {
      return _index < _children.length;
    }

    public UIComponent next()
    {
      if (_index < _children.length)
        return _children[_index++];
      else
        return null;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  private static class AttributeMap extends AbstractMap<String,Object>
    implements Serializable
  {
    private final transient HashMap<String,Property> _propertyMap;
    private HashMap<String,Object> _extMap;
    private Object _obj;

    AttributeMap(Object obj)
    {
      _obj = obj;
      
      Class cl = obj.getClass();
      
      synchronized (cl) {
        HashMap<String,Property> propMap = _compMap.get(cl);

        if (propMap == null) {
          propMap = introspectComponent(cl);
          _compMap.put(cl, propMap);
        }
      
        _propertyMap = propMap;
      }
    }

    Object saveState(FacesContext context)
    {
      return _extMap;
    }

    void restoreState(FacesContext context, Object state)
    {
      _extMap = (HashMap<String,Object>) state;
    }

    public boolean containsKey(String name)
    {
      Property prop = _propertyMap.get(name);

      if (prop != null)
        return false;
      else if (_extMap != null)
        return _extMap.containsKey(name);
      else
        return false;
    }

    @Override
    public Object get(Object v)
    {
      String name = (String) v;
      
      Property prop = _propertyMap.get(name);

      if (prop == null) {
        if (_extMap != null)
          return _extMap.get(name);
        else {
          // XXX: ValueExpression?
          return null;
        }
      }

      Method getter = prop.getGetter();
      
      if (getter == null)
        throw new IllegalArgumentException(name + " is not readable");

      try {
        return getter.invoke(_obj);
      } catch (InvocationTargetException e) {
        throw new FacesException(e.getCause());
      } catch (Exception e) {
        throw new FacesException(e);
      }
    }

    @Override
    public Object put(String name, Object value)
    {
      if (name == null || value == null)
        throw new NullPointerException();

      Property prop = _propertyMap.get(name);

      if (prop == null) {
        if (_extMap == null)
          _extMap = new HashMap<String,Object>(8);

        return _extMap.put(name, value);
      }

      if (prop.getSetter()  == null)
        throw new IllegalArgumentException(name + " is not writable");

      try {
        return prop.getSetter().invoke(_obj, value);
      } catch (Exception e) {
        throw new FacesException(e);
      }
    }

    @Override
    public Object remove(Object name)
    {
      Property prop = _propertyMap.get(name);

      if (prop == null) {
        if (_extMap != null)
          return _extMap.remove(name);
        else
          return null;
      }

      throw new IllegalArgumentException(name + " cannot be removed");
    }

    public Set<Map.Entry<String,Object>> entrySet()
    {
      if (_extMap != null)
        return _extMap.entrySet();
      else
        return Collections.EMPTY_SET;
    }

    private static HashMap<String,Property> introspectComponent(Class cl)
    {
      HashMap<String,Property> map = new HashMap<String,Property>();

      try {
        BeanInfo info = Introspector.getBeanInfo(cl, Object.class);

        for (PropertyDescriptor propDesc : info.getPropertyDescriptors()) {
            Property prop = new Property(propDesc.getReadMethod(),
                                       propDesc.getWriteMethod());

          map.put(propDesc.getName(), prop);
        }
      } catch (Exception e) {
        throw new FacesException(e);
      }

      return map;
    }
  }

  private static class Property {
    private final Method _getter;
    private final Method _setter;

    Property(Method getter, Method setter)
    {
      _getter = getter;
      _setter = setter;
    }

    public Method getGetter()
    {
      return _getter;
    }

    public Method getSetter()
    {
      return _setter;
    }
  }

  private static class ValueExpressionAdapter
    extends ValueExpression
    implements StateHolder
  {
    private ValueBinding _binding;
    private boolean _isTransient;

    ValueExpressionAdapter()
    {
    }

    ValueExpressionAdapter(ValueBinding binding)
    {
      _binding = binding;
    }

    ValueBinding getBinding()
    {
      return _binding;
    }

    public Object getValue(ELContext elContext)
    {
      return _binding.getValue(FacesContext.getCurrentInstance());
    }

    public void setValue(ELContext elContext, Object value)
    {
      _binding.setValue(FacesContext.getCurrentInstance(), value);
    }

    public boolean isReadOnly(ELContext elContext)
    {
      return _binding.isReadOnly(FacesContext.getCurrentInstance());
    }

    public Class getType(ELContext elContext)
    {
      return _binding.getType(FacesContext.getCurrentInstance());
    }

    public Class getExpectedType()
    {
      return Object.class;
    }

    public boolean isLiteralText()
    {
      return false;
    }

    public int hashCode()
    {
      return _binding.getExpressionString().hashCode();
    }

    public boolean equals(Object o)
    {
      if (! (o instanceof ValueExpression))
        return false;

      ValueExpression expr = (ValueExpression) o;
      
      return getExpressionString().equals(expr.getExpressionString());
    }

    public String getExpressionString()
    {
      return _binding.getExpressionString();
    }

    public Object saveState(FacesContext context)
    {
      return saveAttachedState(context, _binding);
    }

    public void restoreState(FacesContext context, Object state)
    {
      _binding = (ValueBinding) restoreAttachedState(context, state);
    }

    public boolean isTransient()
    {
      return _isTransient;
    }

    public void setTransient(boolean isTransient)
    {
      _isTransient = isTransient;
    }

    public String toString()
    {
      return "ValueExpressionAdapter[" + getExpressionString() + "]";
    }
  }

  private static class ValueBindingAdapter extends ValueBinding
  {
    private final ValueExpression _expr;

    ValueBindingAdapter(ValueExpression expr)
    {
      _expr = expr;
    }

    public Object getValue(FacesContext context)
      throws EvaluationException
    {
      return _expr.getValue(context.getELContext());
    }

    public void setValue(FacesContext context, Object value)
      throws EvaluationException
    {
      _expr.setValue(context.getELContext(), value);
    }

    public boolean isReadOnly(FacesContext context)
      throws EvaluationException
    {
      return _expr.isReadOnly(context.getELContext());
    }

    public Class getType(FacesContext context)
      throws EvaluationException
    {
      return _expr.getType(context.getELContext());
    }

    public String getExpressionString()
    {
      return _expr.getExpressionString();
    }

    public String toString()
    {
      return "ValueBindingAdapter[" + _expr + "]";
    }
  }

  static class IOExceptionWrapper extends IOException {
    private Throwable _cause;

    IOExceptionWrapper(String msg, Throwable cause)
    {
      super(msg);

      _cause = cause;
    }

    public Throwable getCause()
    {
      return _cause;
    }
  }

  private static final void addRendererCode(String renderer)
  {
    if (renderer == null || _rendererToCodeMap.get(renderer) != null)
      return;
    
    Integer code = _rendererToCodeMap.size() + 1;
    
    _rendererToCodeMap.put(renderer, code);
    _codeToRendererMap.put(code, renderer);
  }
  
  static {
    addRendererCode(new UIColumn().getRendererType());
    addRendererCode(new UICommand().getRendererType());
    addRendererCode(new UIData().getRendererType());
    addRendererCode(new UIForm().getRendererType());
    addRendererCode(new UIGraphic().getRendererType());
    addRendererCode(new UIInput().getRendererType());
    addRendererCode(new UIMessage().getRendererType());
    addRendererCode(new UIMessages().getRendererType());
    addRendererCode(new UINamingContainer().getRendererType());
    addRendererCode(new UIOutput().getRendererType());
    addRendererCode(new UIPanel().getRendererType());
    addRendererCode(new UIParameter().getRendererType());
    addRendererCode(new UISelectBoolean().getRendererType());
    addRendererCode(new UISelectItem().getRendererType());
    addRendererCode(new UISelectItems().getRendererType());
    addRendererCode(new UISelectMany().getRendererType());
    addRendererCode(new UISelectOne().getRendererType());
    addRendererCode(new UIViewRoot().getRendererType());
    
    addRendererCode(new HtmlColumn().getRendererType());
    addRendererCode(new HtmlCommandButton().getRendererType());
    addRendererCode(new HtmlCommandLink().getRendererType());
    addRendererCode(new HtmlDataTable().getRendererType());
    addRendererCode(new HtmlForm().getRendererType());
    addRendererCode(new HtmlGraphicImage().getRendererType());
    addRendererCode(new HtmlInputHidden().getRendererType());
    addRendererCode(new HtmlInputSecret().getRendererType());
    addRendererCode(new HtmlInputText().getRendererType());
    addRendererCode(new HtmlInputTextarea().getRendererType());
    addRendererCode(new HtmlMessage().getRendererType());
    addRendererCode(new HtmlMessages().getRendererType());
    addRendererCode(new HtmlOutputFormat().getRendererType());
    addRendererCode(new HtmlOutputLabel().getRendererType());
    addRendererCode(new HtmlOutputLink().getRendererType());
    addRendererCode(new HtmlOutputText().getRendererType());
    addRendererCode(new HtmlPanelGrid().getRendererType());
    addRendererCode(new HtmlPanelGroup().getRendererType());
    addRendererCode(new HtmlSelectBooleanCheckbox().getRendererType());
    addRendererCode(new HtmlSelectManyCheckbox().getRendererType());
    addRendererCode(new HtmlSelectManyListbox().getRendererType());
    addRendererCode(new HtmlSelectManyMenu().getRendererType());
    addRendererCode(new HtmlSelectOneListbox().getRendererType());
    addRendererCode(new HtmlSelectOneMenu().getRendererType());
    addRendererCode(new HtmlSelectOneRadio().getRendererType());
  }
}
