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
 * @author Alex Rojkov
 */

package com.caucho.jsf.dev;

import com.caucho.util.L10N;
import com.caucho.jsf.webapp.FacesServletImpl;
import com.caucho.server.webapp.WebApp;

import javax.el.ValueExpression;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIViewRoot;
import javax.faces.component.ValueHolder;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Array;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class
  JsfDeveloperAid
  implements PhaseListener
{

  public static final String URL_PATTERN = "caucho.jsf.developer.aid";

  private static final Logger log
    = Logger.getLogger(FacesServletImpl.class.getName());

  private static final L10N L = new L10N(JsfDeveloperAid.class);

  private String _developerAidLinkStyle;

  public JsfDeveloperAid()
  {
    WebApp webApp = WebApp.getCurrent();

    _developerAidLinkStyle = webApp.getJsf().getDeveloperAidLinkStyle();
  }

  public void afterPhase(PhaseEvent event)
  {
    final FacesContext context = event.getFacesContext();

    final ExternalContext exContext = context.getExternalContext();

    final Map<String, Object> sessionMap = exContext.getSessionMap();

    Map<String, JsfRequestSnapshot> aidMap
      = (Map<String, JsfRequestSnapshot>) sessionMap.get(
      "caucho.jsf.developer.aid");

    if (aidMap == null) {
      aidMap = new HashMap<String, JsfRequestSnapshot>();

      sessionMap.put("caucho.jsf.developer.aid", aidMap);
    }

    try {
      final UIViewRoot uiViewRoot = context.getViewRoot();

      if (uiViewRoot != null) {
        final String viewId = uiViewRoot.getViewId();
        final String phaseId = event.getPhaseId().toString();

        final ViewRoot viewRoot = (ViewRoot) reflect(context, uiViewRoot);
        viewRoot.setPhase(phaseId);

        //request attributes
        Map<String, Object> requestMap = exContext.getRequestMap();
        Map<String, Bean> requestSnapshot = new HashMap<String, Bean>();

        for (String key : requestMap.keySet()) {
          if (key.startsWith("caucho.") ||
              key.startsWith("com.caucho.") ||
              key.startsWith("javax."))
            continue;

          Bean bean = reflect(requestMap.get(key));

          requestSnapshot.put(key, bean);
        }

        viewRoot.setRequestMap(requestSnapshot);
        

        //session attributes
        Map<String, Bean> sessionSnapshot = new HashMap<String, Bean>();

        for (String key : sessionMap.keySet()) {
          if (key.startsWith("caucho.") ||
              key.startsWith("com.caucho.") ||
              key.startsWith("javax."))
            continue;

          Bean bean = reflect(sessionMap.get(key));

          sessionSnapshot.put(key, bean);
        }

        viewRoot.setSessionMap(sessionSnapshot);

        //application attributes
        Map<String, Object> applicationMap = exContext.getApplicationMap();
        Map<String, Bean> applicationSnapshot = new HashMap<String, Bean>();

        for (String key : applicationMap.keySet()) {
          if (key.startsWith("caucho.") ||
              key.startsWith("com.caucho.") ||
              key.startsWith("javax."))
            continue;

          Bean bean = reflect(applicationMap.get(key));

          applicationSnapshot.put(key, bean);
        }

        viewRoot.setApplicationMap(applicationSnapshot);

        JsfRequestSnapshot snapshot;

        if (PhaseId.RESTORE_VIEW.equals(event.getPhaseId())) {
          snapshot = new JsfRequestSnapshot();

         //headers
          Map<String, String> map = exContext.getRequestHeaderMap();
          snapshot.setHeaderMap(new HashMap<String, String>(map));

          //parameters
          map = exContext.getRequestParameterMap();
          snapshot.setParameterMap(new HashMap<String, String>(map));

          aidMap.put(viewId, snapshot);
        }
        else {
          snapshot = aidMap.get(viewId);
        }

        snapshot.addViewRoot(viewRoot);
      }
    }
    catch (IllegalStateException e) {
      log.log(Level.FINER, e.getMessage(), e);
    }
    catch (Throwable t) {
      log.log(Level.FINER, t.getMessage(), t);
    }
  }

  public void beforePhase(PhaseEvent event)
  {
    if (!PhaseId.RENDER_RESPONSE.equals(event.getPhaseId()))
      return;

    UIViewRoot viewRoot = FacesContext.getCurrentInstance().getViewRoot();

    if (viewRoot == null)
      return;

    JsfDeveloperAidLink link = new JsfDeveloperAidLink();

    link.setStyle(_developerAidLinkStyle);

    viewRoot.getChildren().add(link);
  }

  public PhaseId getPhaseId()
  {
    return PhaseId.ANY_PHASE;
  }

  public Component reflect(FacesContext facesContext, UIComponent uiComponent)
  {
    final Component result;

    if (uiComponent instanceof UIViewRoot) {
      UIViewRoot uiViewRoot = (UIViewRoot) uiComponent;
      result = new ViewRoot();

      ViewRoot viewRoot = (ViewRoot) result;

      viewRoot.setLocale(uiViewRoot.getLocale());
      viewRoot.setRenderKitId(uiViewRoot.getRenderKitId());
    }
    else
      result = new Component();

    result._uiComponentClass = uiComponent.getClass().getSimpleName();
    result._clientId = uiComponent.getClientId(facesContext);
    result._family = uiComponent.getFamily();

    final int childCount = uiComponent.getChildCount();

    if (childCount > 0) {
      List<UIComponent> children = uiComponent.getChildren();

      result._children = new ArrayList<Component>(children.size());

      for (int i = 0; i < childCount; i++) {
        UIComponent child = children.get(i);

        if (!(child instanceof JsfDeveloperAidLink))
          result._children.add(reflect(facesContext, children.get(i)));
      }
    }

    final int facetCount = uiComponent.getFacetCount();

    if (facetCount > 0) {
      Map<String, UIComponent> facets = uiComponent.getFacets();

      result._facets = new HashMap<String, Component>(facets.size());

      Set<String> names = facets.keySet();

      for (String name : names) {
        UIComponent child = facets.get(name);

        result._facets.put(name, reflect(facesContext, child));
      }
    }

    if (uiComponent instanceof ValueHolder) {
      result._isValueHolder = true;

      Object value;

      try {
        value = ((ValueHolder) uiComponent).getValue();
      }
      catch (Throwable t) {
        value = "Failed due to: " + t.getMessage();
      }

      result._value = String.valueOf(value);

      Object localValue;

      try {
        localValue = ((ValueHolder) uiComponent).getLocalValue();
      }
      catch (Throwable t) {
        localValue = "Failed due to: " + t.getMessage();
      }

      result._localValue = String.valueOf(localValue);
    }

    if (uiComponent instanceof EditableValueHolder) {
      result._isEditableValueHolder = true;

      Object submittedValue;

      try {
        submittedValue
          = ((EditableValueHolder) uiComponent).getSubmittedValue();
      }
      catch (Throwable t) {
        submittedValue = "Failed due to: " + t.getMessage();
      }

      if (submittedValue instanceof Object[]) {

        StringBuilder sb = new StringBuilder('[');

        Object []values = (Object[]) submittedValue;

        for (int i = 0; i < values.length; i++) {
          Object value = values[i];

          sb.append(String.valueOf(value));

          if ((i + 1) < values.length)
            sb.append(',');
        }

        sb.append(']');

        result._submittedValue = sb.toString();
      }
      else {
        result._submittedValue = String.valueOf(submittedValue);
      }
    }

    for (Method method : uiComponent.getClass().getMethods()) {
      if (!method.getName().startsWith("get")
          && !method.getName().startsWith("is"))
        continue;
      else if (method.getParameterTypes().length != 0)
        continue;

      String name;

      if (method.getName().startsWith("get"))
        name = method.getName().substring(3);
      else if (method.getName().startsWith("is"))
        name = method.getName().substring(2);
      else
        continue;

      name = Character.toLowerCase(name.charAt(0)) + name.substring(1);

      ValueExpression expr = uiComponent.getValueExpression(name);

      Class type = method.getReturnType();

      if (expr != null) {
        result.setAttribute("expr:" + name, expr.getExpressionString());
      }
      else if (method.getDeclaringClass().equals(UIComponent.class)
               || method.getDeclaringClass().equals(UIComponentBase.class)) {
      }
      else if (name.equals("family") ||
               name.equals("value") ||
               name.equals("localValue") ||
               name.equals("submittedValue")) {
      }
      else if (String.class.equals(type)) {
        try {
          Object value = method.invoke(uiComponent);

          if (value != null)
            result.setAttribute(name, String.valueOf(value));
        }
        catch (Exception e) {
        }
      }
    }

    return result;
  }

  public Bean reflect(Object obj)
  {
    if (obj == null)
      return null;

    final Bean result;

    if (obj instanceof String
        || obj instanceof Boolean
        || obj instanceof Character
        || obj instanceof Number
        || obj instanceof Date
      ) {
      result = new Bean();

      result.setClassName(obj.getClass().getSimpleName());
      result.setToString(obj.toString());
      result.setSimple(true);
    } else if (obj instanceof Object[]) {
      result = new Bean();

      result.setArray(true);
      result.setClassName(obj.getClass().getComponentType().getName());
      result.setLength(Array.getLength(obj));
    }
    else {
      result = new Bean();

      result.setClassName(obj.getClass().getName());
      result.setToString(obj.toString());

      Field []fields = obj.getClass().getDeclaredFields();

      Map<String, String> attributes = new HashMap<String, String>();

      for (Field field : fields) {
        try {
          field.setAccessible(true);

          Object value = field.get(obj);

          attributes.put(field.getName(), String.valueOf(value));
        }
        catch (IllegalAccessException e) {
        }
      }

      result.setAttributes(attributes);
    }

    return result;
  }


  public static class JsfRequestSnapshot
    implements Serializable
  {
    private ViewRoot []_phases;
    private Map<String, String> _parameterMap;
    private Map<String, String> _headerMap;

    public void addViewRoot(ViewRoot viewRoot)
    {
      if (_phases == null) {
        _phases = new ViewRoot[]{viewRoot};
      }
      else {
        ViewRoot []newPhases = new ViewRoot[_phases.length + 1];

        System.arraycopy(_phases, 0, newPhases, 0, _phases.length);

        newPhases[newPhases.length - 1] = viewRoot;

        _phases = newPhases;
      }
    }

    public void setPhases(ViewRoot []phases)
    {
      _phases = phases;
    }

    public ViewRoot[] getPhases()
    {
      return _phases;
    }

    public Map<String, String> getParameterMap()
    {
      return _parameterMap;
    }

    public void setParameterMap(Map<String, String> parameterMap)
    {
      _parameterMap = parameterMap;
    }

    public Map<String, String> getHeaderMap()
    {
      return _headerMap;
    }

    public void setHeaderMap(Map<String, String> headerMap)
    {
      _headerMap = headerMap;
    }

  }

  public static class Bean
    implements Serializable
  {
    private Map<String, String> _attributes;
    private String _className;
    private String _toString;
    private boolean _isArray;
    private int _length;
    private boolean _simple;

    public String getToString()
    {
      return _toString;
    }

    public void setToString(String toString)
    {
      _toString = toString;
    }

    public String getClassName()
    {
      return _className;
    }

    public void setClassName(String className)
    {
      _className = className;
    }

    public Map<String, String> getAttributes()
    {
      return _attributes;
    }

    public void setAttributes(Map<String, String> attributes)
    {
      _attributes = attributes;
    }

    public boolean isArray()
    {
      return _isArray;
    }

    public void setArray(boolean array)
    {
      _isArray = array;
    }

    public int getLength()
    {
      return _length;
    }

    public void setLength(int length)
    {
      _length = length;
    }

    public boolean isSimple()
    {
      return _simple;
    }

    public void setSimple(boolean simple)
    {
      _simple = simple;
    }
  }

  public static class ViewRoot
    extends Component
  {
    private Locale _locale;
    private String _renderKitId;
    private String _phase;
    private Map<String, Bean> _requestMap;
    private Map<String, Bean> _sessionMap;
    private Map<String, Bean> _applicationMap;


    public Locale getLocale()
    {
      return _locale;
    }

    public void setLocale(Locale locale)
    {
      _locale = locale;
    }

    public String getRenderKitId()
    {
      return _renderKitId;
    }

    public void setRenderKitId(String renderKitId)
    {
      _renderKitId = renderKitId;
    }

    public String getPhase()
    {
      return _phase;
    }

    public void setPhase(String phase)
    {
      _phase = phase;
    }

    public Map<String, Bean> getRequestMap()
    {
      return _requestMap;
    }

    public void setRequestMap(Map<String, Bean> requestMap)
    {
      _requestMap = requestMap;
    }

    public Map<String, Bean> getSessionMap()
    {
      return _sessionMap;
    }

    public void setSessionMap(Map<String, Bean> sessionMap)
    {
      _sessionMap = sessionMap;
    }

    public Map<String, Bean> getApplicationMap()
    {
      return _applicationMap;
    }

    public void setApplicationMap(Map<String, Bean> applicationMap)
    {
      _applicationMap = applicationMap;
    }

  }

  public static class Component
    implements Serializable
  {
    private String _uiComponentClass;
    private String _clientId;
    private String _family;
    private String _value;
    private String _localValue;
    private String _submittedValue;
    private boolean _isValueHolder;
    private boolean _isEditableValueHolder;

    private List<Component> _children;
    private Map<String, Component> _facets;
    private Map<String, String> _attributes;


    public List<Component> getChildren()
    {
      return _children;
    }

    public void setChildren(List<Component> children)
    {
      _children = children;
    }

    public Map<String, Component> getFacets()
    {
      return _facets;
    }

    public void setFacets(Map<String, Component> facets)
    {
      _facets = facets;
    }

    public String getUiComponentClass()
    {
      return _uiComponentClass;
    }

    public void setUiComponentClass(String uiComponentClass)
    {
      _uiComponentClass = uiComponentClass;
    }

    public String getClientId()
    {
      return _clientId;
    }

    public void setClientId(String clientId)
    {
      _clientId = clientId;
    }

    public String getFamily()
    {
      return _family;
    }

    public void setFamily(String family)
    {
      _family = family;
    }

    public String getValue()
    {
      return _value;
    }

    public void setValue(String value)
    {
      _value = value;
    }

    public String getLocalValue()
    {
      return _localValue;
    }

    public void setLocalValue(String localValue)
    {
      _localValue = localValue;
    }

    public String getSubmittedValue()
    {
      return _submittedValue;
    }

    public void setSubmittedValue(String submittedValue)
    {
      _submittedValue = submittedValue;
    }

    public boolean isValueHolder()
    {
      return _isValueHolder;
    }

    public void setValueHolder(boolean valueHolder)
    {
      _isValueHolder = valueHolder;
    }

    public boolean isEditableValueHolder()
    {
      return _isEditableValueHolder;
    }

    public void setEditableValueHolder(boolean editableValueHolder)
    {
      _isEditableValueHolder = editableValueHolder;
    }

    public void setAttribute(String name, String value)
    {
      if (_attributes == null)
        _attributes = new HashMap<String, String>();

      _attributes.put(name, value);
    }

    public Map<String, String> getAttributes()
    {
      return _attributes;
    }

    public void setAttributes(Map<String, String> attributes)
    {
      _attributes = attributes;
    }
  }
}