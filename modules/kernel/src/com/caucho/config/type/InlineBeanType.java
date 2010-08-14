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

package com.caucho.config.type;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionTarget;

import org.w3c.dom.Node;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.DependencyBean;
import com.caucho.config.TagName;
import com.caucho.config.annotation.DisableConfig;
import com.caucho.config.annotation.NonEL;
import com.caucho.config.attribute.AddAttribute;
import com.caucho.config.attribute.Attribute;
import com.caucho.config.attribute.CreateAttribute;
import com.caucho.config.attribute.ProgramAttribute;
import com.caucho.config.attribute.PropertyAttribute;
import com.caucho.config.attribute.SetterAttribute;
import com.caucho.config.attribute.TextAttribute;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.InjectionTargetBuilder;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.inject.OwnerCreationalContext;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.PropertyStringProgram;
import com.caucho.config.types.RawString;
import com.caucho.config.xml.XmlBeanAttribute;
import com.caucho.config.xml.XmlBeanConfig;
import com.caucho.config.xml.XmlConfigContext;
import com.caucho.util.L10N;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.PersistentDependency;
import com.caucho.xml.QName;
import com.caucho.xml.QNode;

/**
 * Represents an inline bean type for configuration.
 */
public class InlineBeanType<T> extends ConfigType<T>
{
  private static final L10N L = new L10N(InlineBeanType.class);
  private static final Logger log
    = Logger.getLogger(InlineBeanType.class.getName());
  
  private static final String RESIN_NS
    = "http://caucho.com/ns/resin";

  public static final QName TEXT = new QName("#text");
  public static final QName VALUE = new QName("value");

  private static final Object _introspectLock = new Object();

  private final Class<T> _beanClass;
  
  private HashMap<QName,Attribute> _nsAttributeMap
    = new HashMap<QName,Attribute>();
  
  private HashMap<String,Attribute> _attributeMap
    = new HashMap<String,Attribute>();

  private Constructor<T> _stringConstructor;
  
  private Method _valueOf;
  private Method _setParent;
  private Method _replaceObject;
  private Method _setConfigLocation;
  private Method _setConfigNode;
  
  private Attribute _addText;
  private Attribute _addProgram;
  private Attribute _addContentProgram;
  private Attribute _addBean; // add(Object)
  private Attribute _setProperty;
  
  private boolean _isEL;

  private HashMap<Class<?>,Attribute> _addMethodMap
    = new HashMap<Class<?>,Attribute>();

  private Attribute _addCustomBean;
  private AnnotatedType<T> _annotatedType;
  private ManagedBeanImpl<T> _bean;
  private InjectionTarget<T> _injectionTarget;

  private ArrayList<ConfigProgram> _injectList;
  private ArrayList<ConfigProgram> _initList;

  private boolean _isIntrospecting;
  private boolean _isIntrospected;
  private ArrayList<InlineBeanType<?>> _pendingChildList
    = new ArrayList<InlineBeanType<?>>();

  public InlineBeanType(Class<T> beanClass)
  {
    _beanClass = beanClass;
  }

  /**
   * Returns the given type.
   */
  @Override
  public Class<T> getType()
  {
    return _beanClass;
  }
  
  @Override
  public boolean isEL()
  {
    return _isEL;
  }

  protected void setAddCustomBean(Attribute addCustomBean)
  {
    _addCustomBean = addCustomBean;
  }

  protected void setAddAnnotation(Attribute addAnnotation)
  {
  }

  /**
   * Creates a new instance
   */
  @Override
  public Object create(Object parent, QName name)
  {
    try {
      InjectManager cdiManager
        = InjectManager.create(_beanClass.getClassLoader());

      if (_injectionTarget == null) {
        if (_beanClass.isInterface())
          throw new ConfigException(L.l("{0} cannot be instantiated because it is an interface",
                                        _beanClass.getName()));

        AnnotatedType<T> type = getAnnotatedType();

        InjectionTargetBuilder<T> builder
          = new InjectionTargetBuilder<T>(cdiManager, type);

        builder.setGenerateInterception(false);

        _injectionTarget = builder;

        // _bean.getInjectionPoints();
      }

      InjectionTarget<T> injection = _injectionTarget;
      CreationalContext<T> env = new OwnerCreationalContext<T>(_bean);

      T bean = injection.produce(env);
      injection.inject(bean, env);

      if (_setParent != null
          && parent != null
          && _setParent.getParameterTypes()[0].isAssignableFrom(parent.getClass())) {
        try {
          _setParent.invoke(bean, parent);
        } catch (IllegalArgumentException e) {
          throw ConfigException.create(_setParent,
                                       L.l("{0}: setParent value of '{1}' is not valid",
                                           bean, parent),
                                           e);
        } catch (Exception e) {
          throw ConfigException.create(_setParent, e);
        }
      }

      return bean;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  
  private AnnotatedType<T> getAnnotatedType()
  {
    if (_annotatedType == null) {
      InjectManager cdiManager
        = InjectManager.create(_beanClass.getClassLoader());
  
      _annotatedType = cdiManager.createAnnotatedType(_beanClass);
    }
    
    return _annotatedType;
  }

  /**
   * Returns a constructor with a given number of arguments
   */
  @Override
  public Constructor<T> getConstructor(int count)
  {
    for (Constructor<?> ctor : _beanClass.getConstructors()) {
      if (ctor.getParameterTypes().length == count)
        return (Constructor<T>) ctor;
    }
    
    throw new ConfigException(L.l("{0} does not have any constructor with {1} arguments",
                                  this, count));
  }

  /**
   * Called before the children are configured.
   */
  @Override
  public void beforeConfigure(XmlConfigContext env, Object bean, Node node)
  {
    super.beforeConfigure(env, bean, node);

    if (_setConfigNode != null) {
      try {
        _setConfigNode.invoke(bean, node);
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }

    if (_setConfigLocation != null && node instanceof QNode) {
      String filename = ((QNode) node).getFilename();
      int line = ((QNode) node).getLine();

      try {
        _setConfigLocation.invoke(bean, filename, line);
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }

    if (bean instanceof DependencyBean) {
      DependencyBean dependencyBean = (DependencyBean) bean;
      
      ArrayList<Dependency> dependencyList = env.getDependencyList();
      if (dependencyList != null) {
        for (Dependency depend : dependencyList) {
          dependencyBean.addDependency((PersistentDependency) depend);
        }
      }
    }
  }

  /**
   * Returns the attribute based on the given name.
   */
  @Override
  public Attribute getAttribute(QName name)
  {
    synchronized (_nsAttributeMap) {
      Attribute attr = _nsAttributeMap.get(name);

      if (attr == null) {
        attr = getAttributeImpl(name);

        if (attr != null)
          _nsAttributeMap.put(name, attr);
      }

      return attr;
    }
  }

  protected Attribute getAttributeImpl(QName name)
  {
    // server/2r10 vs jms/2193
    // attr = _attributeMap.get(name.getLocalName().toLowerCase());

    Attribute attr = _attributeMap.get(name.getLocalName());

    if (attr != null)
      return attr;

    String uri = name.getNamespaceURI();

    if (uri == null || ! uri.startsWith("urn:java"))
      return null;

    Class<?> cl = createClass(name);

    if (cl != null) {
      attr = getAddAttribute(cl);

      if (attr != null)
        return attr;
    }

    if (_addCustomBean != null) {
      return _addCustomBean;
    }
    else if (_addBean != null) {
      return _addBean;
    }

    return null;
  }

  @Override
  public Attribute getAddBeanAttribute(QName qName)
  {
    return _addBean;
  }

  /**
   * Returns any add attributes to add arbitrary content
   */
  @Override
  public Attribute getAddAttribute(Class cl)
  {
    if (cl == null)
      return null;

    Attribute attr = _addMethodMap.get(cl);

    if (attr != null) {
      return attr;
    }
    
    for (Class<?> iface : cl.getInterfaces()) {
      attr = getAddAttribute(iface);

      if (attr != null)
        return attr;
    }

    return getAddAttribute(cl.getSuperclass());
  }

  private Class<?> createClass(QName name)
  {
    String uri = name.getNamespaceURI();

    if (uri.equals(RESIN_NS)) {
      return createResinClass(name.getLocalName());
    }

    if (! uri.startsWith("urn:java:"))
      return null;

    String pkg = uri.substring("urn:java:".length());

    return TypeFactory.loadClass(pkg, name.getLocalName());
  }

  private Class<?> createResinClass(String name)
  {
    Class<?> cl = TypeFactory.loadClass("ee", name);

    return cl;
  }

  /**
   * Returns the program attribute.
   */
  @Override
  public Attribute getProgramAttribute()
  {
    if (_setProperty != null)
      return _setProperty;
    else
      return _addProgram;
  }

  /**
   * Returns the content program attribute (program excluding if, choose).
   */
  @Override
  public Attribute getContentProgramAttribute()
  {
    return _addContentProgram;
  }

  /**
   * Initialize the type
   */
  @Override
  public void inject(Object bean)
  {
    introspectInject();
    
    for (int i = 0; i < _injectList.size(); i++)
      _injectList.get(i).inject(bean, null);
  }

  /**
   * Initialize the type
   */
  @Override
  public void init(Object bean)
  {
    introspectInject();
    
    for (int i = 0; i < _initList.size(); i++)
      _initList.get(i).inject(bean, null);
  }

  /**
   * Return true if the object is replaced
   */
  @Override
  public boolean isReplace()
  {
    return _replaceObject != null;
  }
  
  /**
   * Replace the type with the generated object
   */
  @Override
  public Object replaceObject(Object bean)
  {
    if (_replaceObject != null) {
      try {
        return _replaceObject.invoke(bean);
      } catch (Exception e) {
        throw ConfigException.create(_replaceObject, e);
      }
    }
    else
      return bean;
  }
  
  /**
   * Converts the string to the given value.
   */
  public Object valueOf(String text)
  {
    if (_valueOf != null) {
      try {
        return _valueOf.invoke(null, text);
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }
    else if (_stringConstructor != null) {
      try {
        return _stringConstructor.newInstance(text);
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }
    else if (_addText != null) {
      Object bean = create(null, TEXT);
      _addText.setText(bean, TEXT, text);

      inject(bean);
      init(bean);
      
      return bean;
    }
    else if (_addProgram != null || _addContentProgram != null) {
      Object bean = create(null, TEXT);

      inject(bean);
      
      try {
        ConfigProgram program = new PropertyStringProgram(TEXT, text);
        
        if (_addProgram != null)
          _addProgram.setValue(bean, TEXT, program);
        else
          _addContentProgram.setValue(bean, TEXT, program);
      } catch (Exception e) {
        throw ConfigException.create(e);
      }

      init(bean);

      return bean;
    }
    else if ("".equals(text.trim())) {
      Object bean = create(null, TEXT);

      inject(bean);
      init(bean);
      
      return bean;
    }

    throw new ConfigException(L.l("Can't convert to '{0}' from '{1}'.",
                                  _beanClass.getName(), text));
  }

  public boolean isConstructableFromString()
  {
    return _valueOf != null ||
           _stringConstructor != null ||
           _addText != null ||
           _addProgram != null ||
           _addContentProgram != null;
  }

  /**
   * Converts the string to the given value.
   */
  @Override
  public Object valueOf(Object value)
  {
    if (value == null)
      return null;
    else if (value instanceof String)
      return valueOf((String) value);
    else if (_beanClass.isAssignableFrom(value.getClass()))
      return value;
    else if (value.getClass().getName().startsWith("java.lang."))
      return valueOf(String.valueOf(value));
    else
      return value;
  }

  //
  // Introspection
  //

  /**
   * Introspect the bean for configuration
   */
  @Override
  public void introspect()
  {
    // long startTime = System.currentTimeMillis();
    synchronized (_introspectLock) {
      if (_isIntrospecting)
        return;

      _isIntrospecting = true;

      try {
        // ioc/20h4 - after to deal with recursion
        introspectParent();

        //Method []methods = _beanClass.getMethods();
        if (! _isIntrospected) {
          _isIntrospected = true;

          _isEL = ! _beanClass.isAnnotationPresent(NonEL.class);
          
          try {
            Method []methods = _beanClass.getDeclaredMethods();

            introspectMethods(methods);
          } catch (NoClassDefFoundError e) {
            log.fine(_beanClass + " " + e);
          }
        }
      } finally {
        _isIntrospecting = false;
      }
    }

    introspectComplete();
    //long endTime = System.currentTimeMillis();
  }

  private void introspectComplete()
  {
    ArrayList<InlineBeanType> childList = new ArrayList<InlineBeanType>(_pendingChildList);

    // ioc/20h4
    for (InlineBeanType child : childList) {
      child.introspectParent();
      child.introspectComplete();
    }
  }
  
  private boolean isIntrospecting()
  {
    if (_isIntrospecting)
      return true;

    Class parentClass = _beanClass.getSuperclass();
    
    if (parentClass != null) {
      ConfigType parentType = TypeFactory.getType(parentClass);

      if (parentType instanceof InlineBeanType) {
        InlineBeanType parentBean = (InlineBeanType) parentType;

        return parentBean.isIntrospecting();
      }
    }

    return false;
  }

  private void introspectParent()
  {
    Class<?> parentClass = _beanClass.getSuperclass();
    
    if (parentClass != null) {
      ConfigType parentType = TypeFactory.getType(parentClass);

      if (parentType instanceof InlineBeanType<?>) {
        InlineBeanType<?> parentBean = (InlineBeanType<?>) parentType;

        if (! parentBean._isIntrospected)
          parentBean.introspect();

        // ioc/20h4
        if (parentBean.isIntrospecting()) {
          if (! parentBean._pendingChildList.contains(this))
            parentBean._pendingChildList.add(this);
          return;
        }

        if (_setParent == null)
          _setParent = parentBean._setParent;

        if (_replaceObject == null)
          _replaceObject = parentBean._replaceObject;

        if (_setConfigLocation == null)
          _setConfigLocation = parentBean._setConfigLocation;

        if (_setConfigNode == null)
          _setConfigNode = parentBean._setConfigNode;

        if (_addText == null)
          _addText = parentBean._addText;

        if (_addProgram == null)
          _addProgram = parentBean._addProgram;

        if (_addContentProgram == null)
          _addContentProgram = parentBean._addContentProgram;

        if (_setProperty == null)
          _setProperty = parentBean._setProperty;

        if (_addCustomBean == null)
          _addCustomBean = parentBean._addCustomBean;

        for (Map.Entry<QName,Attribute> entry : parentBean._nsAttributeMap.entrySet()) {
          if (_nsAttributeMap.get(entry.getKey()) == null)
            _nsAttributeMap.put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String,Attribute> entry : parentBean._attributeMap.entrySet()) {
          if (_attributeMap.get(entry.getKey()) == null)
            _attributeMap.put(entry.getKey(), entry.getValue());
        }

        _addMethodMap.putAll(parentBean._addMethodMap);
      }
    }
  }

  /**
   * Introspect the beans methods for setters
   */
  public void introspectMethods(Method []methods)
  {
    Constructor []constructors = _beanClass.getConstructors();

    _stringConstructor = findConstructor(constructors, String.class);

    HashMap<String,Method> createMap = new HashMap<String,Method>(8);
    fillCreateMap(createMap, methods);

    HashMap<String,Method> setterMap = new HashMap<String,Method>(8);
    fillSetterMap(setterMap, methods);

    for (Method method : methods) {
      if (method.getAnnotation(DisableConfig.class) != null)
        continue;
      
      Class<?> []paramTypes = method.getParameterTypes();

      String name = method.getName();

      if ("replaceObject".equals(name) && paramTypes.length == 0) {
        _replaceObject = method;
        _replaceObject.setAccessible(true);
        continue;
      }

      if ("valueOf".equals(name)
          && paramTypes.length == 1
          && String.class.equals(paramTypes[0])
          && Modifier.isStatic(method.getModifiers())) {
        _valueOf = method;
        _valueOf.setAccessible(true);
        continue;
      }

      if (Modifier.isStatic(method.getModifiers()))
        continue;

      if (! Modifier.isPublic(method.getModifiers()))
        continue;

      if ((name.equals("addBuilderProgram") || name.equals("addProgram"))
          && paramTypes.length == 1
          && paramTypes[0].equals(ConfigProgram.class)) {
        ConfigType type = TypeFactory.getType(paramTypes[0]);

        _addProgram = new ProgramAttribute(method, type);
      }
      else if (name.equals("addContentProgram")
          && paramTypes.length == 1
          && paramTypes[0].equals(ConfigProgram.class)) {
        ConfigType type = TypeFactory.getType(paramTypes[0]);

        _addContentProgram = new ProgramAttribute(method, type);
      }
      else if ((name.equals("setConfigLocation")
          && paramTypes.length == 2
          && paramTypes[0].equals(String.class)
          && paramTypes[1].equals(int.class))) {
        _setConfigLocation = method;
      }
      else if ((name.equals("setConfigNode")
          && paramTypes.length == 1
          && paramTypes[0].equals(Node.class))) {
        _setConfigNode = method;
      }
      else if ((name.equals("addCustomBean")
          && paramTypes.length == 1
          && paramTypes[0].equals(XmlBeanConfig.class))) {
        ConfigType customBeanType
        = TypeFactory.getType(XmlBeanConfig.class);

        _addCustomBean = new XmlBeanAttribute(method, customBeanType);
      }
      else if ((name.equals("addAnnotation")
          && paramTypes.length == 1
          && paramTypes[0].equals(Annotation.class))) {
        ConfigType customBeanType
        = TypeFactory.getType(XmlBeanConfig.class);

        _addCustomBean = new XmlBeanAttribute(method, customBeanType);
      }
      else if (name.equals("setProperty")
          && paramTypes.length == 2
          && paramTypes[0].equals(String.class)) {
        ConfigType type = TypeFactory.getType(paramTypes[1]);

        PropertyAttribute attr = new PropertyAttribute(method, type);

        _setProperty = attr;
      }
      else if (name.equals("setParent")
          && paramTypes.length == 1) {
        // XXX: use annotation
        _setParent = method;
      }
      else if (name.equals("add")
          && paramTypes.length == 1) {
        ConfigType type = TypeFactory.getType(paramTypes[0]);

        Attribute addAttr = new AddAttribute(method, type);

        _addMethodMap.put(paramTypes[0], addAttr);

        // _addBean = addAttr;
      }
      else if ((name.startsWith("set") || name.startsWith("add"))
          && paramTypes.length == 1
          && createMap.get(name.substring(3)) == null) {
        Class<?> type = paramTypes[0];

        String className = name.substring(3);
        String xmlName = toXmlName(name.substring(3));

        TagName tagName = method.getAnnotation(TagName.class);

        if (tagName != null) {
          for (String propName : tagName.value()) {
            addProp(propName, method);
          }
        }
        else
          addProp(xmlName, method);

        addProp(toCamelName(className), method);
      }
      else if ((name.startsWith("create")
          && paramTypes.length == 0
          && ! void.class.equals(method.getReturnType()))) {
        Class type = method.getReturnType();

        Method setter = setterMap.get(name.substring(6));

        CreateAttribute attr = new CreateAttribute(method, type, setter);

        String xmlName = toXmlName(name.substring(6));

        TagName tagName = method.getAnnotation(TagName.class);

        if (tagName != null) {
          for (String propName : tagName.value()) {
            addProp(propName, attr);
          }
        }
        else {
          addProp(xmlName, attr);
        }
      }
    }
  }
  
  private void addProp(String propName, 
                       Method method)
  {
    Attribute attr;
    
    Class<?> []paramTypes = method.getParameterTypes();
    Class<?> type = paramTypes[0];
    
    if (propName.equals("text")
        && (type.equals(String.class)
            || type.equals(RawString.class))) {
      attr = new TextAttribute(method, type);
      _addText = attr;
      _attributeMap.put("#text", attr);
    }
    else
      attr = new SetterAttribute(method, type);
    
    addProp(propName, attr);
  }
  
  private void addProp(String propName, Attribute attr)
  {
    Attribute oldAttr = _attributeMap.get(propName);
    
    if (oldAttr == null) {
      _attributeMap.put(propName, attr);
    }
    else if (attr.equals(oldAttr)) {
    }
    else if (oldAttr.isConfigurable() && ! attr.isConfigurable()) {
    }
    else if (attr.isConfigurable() && ! oldAttr.isConfigurable()) {
      _attributeMap.put(propName, attr);
    }
    else if (attr.isAssignableFrom(oldAttr)) {
    }
    else if (oldAttr.isAssignableFrom(attr)) {
      _attributeMap.put(propName, attr);
    }
    else {
      log.fine(L.l("{0}: conflicting attribute for '{1}' between {2} and {3}",
                   this, propName, attr, oldAttr));    
    }
    
    // server/2e28 vs jms/2193
    // _attributeMap.put(className, attr);

    if (propName.equals("value")) {
      _attributeMap.put("#text", attr);

      // server/12aa
      if (_addText == null)
        _addText = attr;
    }
  }


  /**
   * Introspect the bean for configuration
   */
  private void introspectInject()
  {
    synchronized (_introspectLock) {
      if (_injectList != null)
        return;

      _injectList = new ArrayList<ConfigProgram>();
      _initList = new ArrayList<ConfigProgram>();
    
      InjectionTargetBuilder.introspectInit(_initList, getAnnotatedType());
    }
  }

  private static Constructor findConstructor(Constructor []constructors,
                                             Class<?> ...types)
  {
    for (Constructor ctor : constructors) {
      Class<?> []paramTypes = ctor.getParameterTypes();

      if (isMatch(paramTypes, types))
        return ctor;
    }

    return null;
  }

  private static boolean isMatch(Class<?> []aTypes, Class<?> []bTypes)
  {
    if (aTypes.length != bTypes.length)
      return false;

    for (int i = aTypes.length - 1; i >= 0; i--) {
      if (! aTypes[i].equals(bTypes[i]))
        return false;
    }

    return true;
  }


  private void fillCreateMap(HashMap<String,Method> createMap,
                             Method []methods)
  {
    for (Method method : methods) {
      String name = method.getName();

      if (name.startsWith("create")
          && ! name.equals("create")
          && method.getParameterTypes().length == 0) {
        createMap.put(name.substring("create".length()), method);
      }
    }
  }

  private void fillSetterMap(HashMap<String,Method> setterMap,
                             Method []methods)
  {
    for (Method method : methods) {
      String name = method.getName();

      if (name.length() > 3
          && (name.startsWith("add") || name.startsWith("set"))
          && method.getParameterTypes().length == 1) {
        setterMap.put(name.substring("set".length()), method);
      }
    }
  }

  private String toXmlName(String name)
  {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);

      if (Character.isUpperCase(ch)
          && i > 0
          && (Character.isLowerCase(name.charAt(i - 1))
              || (i + 1 < name.length()
                  && Character.isLowerCase(name.charAt(i + 1))))) {
        sb.append('-');
      }

      sb.append(Character.toLowerCase(ch));
    }

    return sb.toString();
  }

  private String toCamelName(String name)
  {
    return Introspector.decapitalize(name);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _beanClass.getName() + "]";
  }
}
