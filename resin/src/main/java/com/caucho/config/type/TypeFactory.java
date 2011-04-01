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

import com.caucho.config.*;
import com.caucho.config.attribute.*;
import com.caucho.config.program.*;
import com.caucho.config.types.RawString;
import com.caucho.config.types.AnnotationConfig;
import com.caucho.config.xml.XmlBeanConfig;
import com.caucho.config.xml.XmlBeanType;
import com.caucho.el.Expr;
import com.caucho.loader.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.xml.QName;

import java.beans.*;
import java.io.*;
import java.net.URL;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.*;
import java.util.regex.Pattern;
import java.util.logging.*;

import javax.annotation.*;
import javax.el.*;
import javax.sql.*;

import org.w3c.dom.Node;

/**
 * Factory for returning type strategies.
 */
public class TypeFactory implements AddLoaderListener
{
  private static final Logger log
    = Logger.getLogger(TypeFactory.class.getName());
  private static L10N L = new L10N(TypeFactory.class);

  private static final String RESIN_NS = "http://caucho.com/ns/resin";

  private static final HashMap<Class<?>,ConfigType<?>> _primitiveTypes
    = new HashMap<Class<?>,ConfigType<?>>();

  private static final EnvironmentLocal<TypeFactory> _localFactory
    = new EnvironmentLocal<TypeFactory>();

  private static final ClassLoader _systemClassLoader;

  private static final Object _introspectLock = new Object();

  private final EnvironmentClassLoader _loader;
  private final TypeFactory _parent;

  private final HashSet<URL> _configSet
    = new HashSet<URL>();

  private final HashMap<String,ArrayList<String>> _packageImportMap
    = new HashMap<String,ArrayList<String>>();

  private final HashMap<String,ConfigType> _typeMap
    = new HashMap<String,ConfigType>();

  private final HashMap<String,XmlBeanType> _customBeanMap
    = new HashMap<String,XmlBeanType>();

  private final ConcurrentHashMap<QName,ConfigType> _attrMap
    = new ConcurrentHashMap<QName,ConfigType>();
  
  private final ConcurrentHashMap<QName,Class<?>> _customClassMap
    = new ConcurrentHashMap<QName,Class<?>>();

  private final HashMap<QName,Attribute> _listAttrMap
    = new HashMap<QName,Attribute>();

  private final HashMap<QName,Attribute> _setAttrMap
    = new HashMap<QName,Attribute>();

  private final ConcurrentHashMap<QName,Attribute> _envAttrMap
    = new ConcurrentHashMap<QName,Attribute>();

  private final HashMap<String,NamespaceConfig> _nsMap
    = new HashMap<String,NamespaceConfig>();

  private final HashSet<URL> _driverTypeSet
    = new HashSet<URL>();

  private final HashMap<String,HashMap<String,String>> _driverTypeMap
    = new HashMap<String,HashMap<String,String>>();

  private final AtomicBoolean _isInInit = new AtomicBoolean();

  private TypeFactory(ClassLoader loader)
  {
    _loader = Environment.getEnvironmentClassLoader(loader);
    
    _localFactory.set(this, loader);

    if (_loader != null) {
      _parent = getFactory(_loader.getParent());

      _loader.addLoaderListener(this);
    }
    else
      _parent = null;

    init(loader);
  }

  /**
   * Returns the appropriate strategy.
   */
  public static ConfigType<?> getType(Object bean)
  {
    if (bean instanceof XmlBeanConfig)
      return ((XmlBeanConfig) bean).getConfigType();
    else if (bean instanceof AnnotationConfig)
      return ((AnnotationConfig) bean).getConfigType();

    return getType(bean.getClass());
  }

  /**
   * Returns the appropriate strategy.
   */
  public static <T> ConfigType<T> getType(Class<T> type)
  {
    TypeFactory factory = getFactory(type.getClassLoader());

    return factory.getConfigTypeImpl(type);
  }

  /**
   * Returns the appropriate strategy.
   */
  public static ConfigType<?> getType(Type type)
  {
    return getType((Class) type);
  }

  /**
   * Returns the appropriate strategy.
   */
  public static Class<?> loadClass(QName qName)
  {
    return getFactory().loadClassImpl(qName);
  }

  /**
   * Returns the appropriate strategy.
   */
  public static Class<?> loadClass(String pkg, String name)
  {
    return getFactory().loadClassImpl(pkg, name);
  }

  public static TypeFactory create()
  {
    return getFactory();
  }

  public static TypeFactory getFactory()
  {
    return getFactory(Thread.currentThread().getContextClassLoader());
  }

  public static TypeFactory getFactory(ClassLoader loader)
  {
    if (loader == null)
      loader = _systemClassLoader;

    TypeFactory factory = _localFactory.getLevel(loader);

    if (factory == null) {
      factory = new TypeFactory(loader);
      _localFactory.set(factory, loader);
      factory.init(loader);
    }

    return factory;
  }

  /**
   * Returns an environment type.
   */
  public ConfigType<?> getEnvironmentType(QName name)
  {
    ConfigType<?> type = _attrMap.get(name);

    if (type != null)
      return type == NotFoundConfigType.NULL ? null : type;

    type = getEnvironmentTypeRec(name);

    if (type != null) {
      return type;
    }

    if (! "".equals(name.getNamespaceURI())) {
      type = getEnvironmentType(new QName(name.getLocalName()));

      if (type != null) {
        _attrMap.put(name, type);

        return type;
      }
    }

    _attrMap.put(name, NotFoundConfigType.NULL);

    return null;
  }

  /**
   * Returns an environment type.
   */
  protected ConfigType getEnvironmentTypeRec(QName name)
  {
    ConfigType type = _attrMap.get(name);

    if (type != null) {
      return type == NotFoundConfigType.NULL ? null : type;
    }

    if (_parent != null)
      type = _parent.getEnvironmentTypeRec(name);

    if (type != null) {
      _attrMap.put(name, type);

      return type;
    }

    String uri = name.getNamespaceURI();

    NamespaceConfig ns = _nsMap.get(uri);

    if (ns != null) {
      ns.loadBeans();

      type = ns.getBean(name.getLocalName());

      if (type != null) {
        _attrMap.put(name, type);

        return type;
      }
    }

    if (RESIN_NS.equals(uri))
      uri = "urn:java:ee";

    if (uri != null && uri.startsWith("urn:java:")) {
      String pkg = uri.substring("urn:java:".length());
      String className = name.getLocalName();

      Class<?> cl = loadClassImpl(pkg, className);

      if (cl != null) {
        type = getType(cl);
        
        _attrMap.put(name, type);
        
        return type;
      }
    }

    _attrMap.put(name, NotFoundConfigType.NULL);

    return null;
  }

  /**
   * Returns an environment type.
   */
  public Attribute getListAttribute(QName name)
  {
    synchronized (_listAttrMap) {
      Attribute attr = _listAttrMap.get(name);

      if (attr != null)
        return attr;

      ConfigType<?> type = getEnvironmentType(name);

      if (type == null)
        return null;

      attr = new ListValueAttribute(type);

      _listAttrMap.put(name, attr);

      return attr;
    }
  }

  /**
   * Returns an environment type.
   */
  public Attribute getSetAttribute(QName name)
  {
    synchronized (_setAttrMap) {
      Attribute attr = _setAttrMap.get(name);

      if (attr != null)
        return attr;

      ConfigType<?> type = getEnvironmentType(name);

      if (type == null)
        return null;

      attr = new SetValueAttribute(type);

      _setAttrMap.put(name, attr);

      return attr;
    }
  }

  /**
   * Returns an environment type.
   */
  public Attribute getEnvironmentAttribute(QName name)
  {
    Attribute attr = _envAttrMap.get(name);

    if (attr != null)
      return attr;

    ConfigType type = getEnvironmentType(name);

    if (type == null)
      return null;

    if (type instanceof FlowBeanType)
      attr = new FlowAttribute(type);
    else
      attr = new EnvironmentAttribute(type);

    _envAttrMap.put(name, attr);

    return attr;
  }

  private Class<?> loadClassImpl(QName qName)
  {
    Class<?> cl = _customClassMap.get(qName);
    
    if (cl != null)
      return cl == void.class ? null : cl;
    
    String uri = qName.getNamespaceURI();
    String localName = qName.getLocalName();

    if (! uri.startsWith("urn:java:"))
      throw new IllegalStateException(L.l("'{0}' is an unexpected namespace, expected 'urn:java:...'", uri));

    String packageName = uri.substring("uri:java:".length());
    
    cl = loadClassImpl(packageName, localName);
    
    if (cl != null)
      _customClassMap.put(qName, cl);
    else
      _customClassMap.put(qName, void.class);
    
    return cl;
  }
  
  private Class<?> loadClassImpl(String pkg, String name)
  {
    ClassLoader loader = _loader;

    if (_loader == null)
      loader = _systemClassLoader;

    ArrayList<String> pkgList = loadPackageList(pkg);
    
    DynamicClassLoader dynLoader = null;
    
    if (loader instanceof DynamicClassLoader)
      dynLoader = (DynamicClassLoader) loader;

    for (String pkgName : pkgList) {
      try {
        Class<?> cl;
        
        if (dynLoader != null)
          cl = dynLoader.loadClassImpl(pkgName + '.' + name, false);
        else
          cl = Class.forName(pkgName + '.' + name, false, loader);

        if (cl != null)
          return cl;
      } catch (ClassNotFoundException e) {
        log.log(Level.ALL, e.toString(), e);
      }
    }

    return null;
  }

  private ArrayList<String> loadPackageList(String pkg)
  {
    synchronized (_packageImportMap) {
      ArrayList<String> pkgList = _packageImportMap.get(pkg);

      if (pkgList != null)
        return pkgList;

      pkgList = new ArrayList<String>();
      pkgList.add(pkg);

      InputStream is = null;
      try {
        ClassLoader loader = _loader;

        if (loader == null)
          loader = _systemClassLoader;

        is = loader.getResourceAsStream(pkg.replace('.', '/') + "/namespace");

        if (is != null) {
          ReadStream in = Vfs.openRead(is);
          String line;
          while ((line = in.readLine()) != null) {
            for (String name : line.split("[ \t\r\n]+")) {
              if (! "".equals(name)) {
                if (! pkgList.contains(name))
                  pkgList.add(name);
              }
            }
          }
        }
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      } finally {
        IoUtil.close(is);
      }

      _packageImportMap.put(pkg, pkgList);

      return pkgList;
    }
  }

  private ConfigType getConfigTypeImpl(Class type)
  {
    synchronized (_introspectLock) {
      ConfigType strategy = _typeMap.get(type.getName());

      if (strategy == null) {
        strategy = _primitiveTypes.get(type);

        if (strategy == null)
          strategy = createType(type);

        _typeMap.put(type.getName(), strategy);

        strategy.introspect();
      }

      return strategy;
    }
  }

  private ConfigType createType(Class<?> type)
  {
    PropertyEditor editor = null;

    if (ConfigType.class.isAssignableFrom(type)) {
      try {
        return (ConfigType) type.newInstance();
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }
    else if ((editor = findEditor(type)) != null)
      return new PropertyEditorType(type, editor);
    else if (type.getEnumConstants() != null)
      return new EnumType(type);
    else if (Set.class.isAssignableFrom(type))
      return new SetType(type);
    else if (Collection.class.isAssignableFrom(type)
             && ! Queue.class.isAssignableFrom(type)) {
      // jms/2300
      return new ListType(type);
    }
    else if (Map.class.isAssignableFrom(type)
             && type.getName().startsWith("java.util")) {
      return new MapType(type);
    }
    else if (EnvironmentBean.class.isAssignableFrom(type))
      return new EnvironmentBeanType(type);
    else if (FlowBean.class.isAssignableFrom(type))
      return new FlowBeanType(type);
    else if (type.isArray()) {
      Class<?> compType = type.getComponentType();

      return new ArrayType(getType(compType), compType);
    }
    else if (Annotation.class.isAssignableFrom(type)) {
      return new AnnotationInterfaceType(type);
    }
    else if (type.isInterface()) {
      return new InterfaceType(type);
    }
    else if (type == ConfigProgram.class)
      return new ConfigProgramType(type);
    else if (Modifier.isAbstract(type.getModifiers())) {
      return new AbstractBeanType(type);
    }
    else
      return new InlineBeanType(type);
  }

  /**
   * Returns the Java bean property editor
   */
  private static PropertyEditor findEditor(Class<?> type)
  {
    // none of the caucho classes has a ProperyEditorManager

    if (type.getName().startsWith("com.caucho"))
      return null;
    else
      return PropertyEditorManager.findEditor(type);
  }

  /**
   * Returns the appropriate strategy.
   */
  public static <T> XmlBeanType<T> getCustomBeanType(Class<T> type)
  {
    TypeFactory factory = getFactory(type.getClassLoader());

    return factory.getCustomBeanTypeImpl(type);
  }

  private <T> XmlBeanType<T> getCustomBeanTypeImpl(Class<T> type)
  {
    synchronized (_customBeanMap) {
      XmlBeanType<?> beanType = _customBeanMap.get(type.getName());

      if (beanType == null) {
        beanType = new XmlBeanType<T>(type);
        _customBeanMap.put(type.getName(), beanType);
      }

      return (XmlBeanType<T>) beanType;
    }
  }

  /**
   * Initialize the type strategy factory with files in META-INF/caucho
   *
   * @param loader the owning class loader
   * @throws Exception
   */
  private void init(ClassLoader loader)
  {
    if (! _isInInit.getAndSet(true))
      return;

    try {
      _nsMap.clear();
      _driverTypeSet.clear();
      _driverTypeMap.clear();

      Enumeration<URL> urls
        = loader.getResources("META-INF/caucho/com.caucho.config.namespace.xml");

      while (urls.hasMoreElements()) {
        URL url = urls.nextElement();

        if (hasConfig(url))
          continue;

        _configSet.add(url);

        InputStream is = url.openStream();

        try {
          new Config(loader).configure(this, is);
        } finally {
          is.close();
        }
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    } finally {
      _isInInit.set(false);
    }
  }

  protected boolean hasConfig(URL url)
  {
    if (_configSet.contains(url))
      return true;
    else if (_parent != null)
      return _parent.hasConfig(url);
    else
      return false;
  }

  /**
   * Returns a driver by the url
   */
  public Class getDriverClassByUrl(Class api, String url)
  {
    String scheme;

    int p = url.indexOf(':');
    if (p >= 0)
      scheme = url.substring(0, p);
    else
      scheme = url;

    String typeName = getDriverType(api.getName(), scheme);

    if (typeName == null) {
      ArrayList<String> schemes = new ArrayList<String>();

      getDriverSchemes(schemes, api.getName());

      Collections.sort(schemes);

      throw new ConfigException(L.l("'{0}' is an unknown scheme for driver '{1}'.  The available schemes are '{2}'",
                                    scheme, api.getName(), schemes));
    }

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Class cl = Class.forName(typeName, false, loader);

      if (! api.isAssignableFrom(cl))
        throw new ConfigException(L.l("'{0}' is not assignable to '{1}' for scheme '{2}'",
                                      cl.getName(), api.getName(),
                                      scheme));

      return cl;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigException(L.l("'{0}' is an undefined class for scheme '{1}'",
                                    typeName, scheme), e);
    }
  }

  /**
   * Returns a driver by the scheme
   */
  public Class getDriverClassByScheme(Class api, String scheme)
  {
    String typeName = getDriverType(api.getName(), scheme);

    if (typeName == null) {
      ArrayList<String> schemes = new ArrayList<String>();

      getDriverSchemes(schemes, api.getName());

      Collections.sort(schemes);

      throw new ConfigException(L.l("'{0}' is an unknown scheme for driver '{1}'.  The available schemes are '{2}'",
                                    scheme, api.getName(), schemes));
    }

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Class cl = Class.forName(typeName, false, loader);

      if (! api.isAssignableFrom(cl))
        throw new ConfigException(L.l("'{0}' is not assignable to '{1}' for scheme '{2}'",
                                      cl.getName(), api.getName(),
                                      scheme));

      return cl;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigException(L.l("'{0}' is an undefined class for scheme '{1}'",
                                    typeName, scheme), e);
    }
  }

  public ContainerProgram getUrlProgram(String url)
  {
    String properties = "";

    int p = url.indexOf(':');
    if (p >= 0) {
      properties = url.substring(p + 1);
    }
    else
      return null;

    String []props = properties.split("[;]");

    if (props.length == 0)
      return null;

    ContainerProgram program = new ContainerProgram();
    for (String prop : props) {
      if (prop.length() == 0)
        continue;

      String []values = prop.split("[=]");

      if (values.length != 2)
        throw new ConfigException(L.l("'{0}' is an invalid URL.  Bean URL syntax is 'scheme:prop1=value1;prop2=value2'", url));

      program.addProgram(new PropertyStringProgram(values[0], values[1]));
    }

    return program;
  }

  /**
   * Returns the classname of the given driver.
   *
   * @param apiType the driver API
   * @param scheme the configuration scheme
   */
  public String getDriverType(String apiType, String scheme)
  {
    HashMap<String,String> driverMap = getDriverTypeMap(apiType);

    return driverMap.get(scheme);
  }

  /**
   * Returns a list of schemes supported by the api type.
   *
   * @param schemes the return list of schemes scheme
   * @param apiType the driver API
   */
  public void getDriverSchemes(ArrayList<String> schemes, String apiType)
  {
    HashMap<String,String> driverMap = getDriverTypeMap(apiType);

    ClassLoader loader = _loader;
    if (_loader == null)
      loader = _systemClassLoader;

    for (Map.Entry<String,String> entry : driverMap.entrySet()) {
      String scheme = entry.getKey();
      String type = entry.getValue();

      try {
        Class cl = Class.forName(type, false, loader);

        if (cl != null)
          schemes.add(scheme);
      } catch (Exception e) {
        log.finest(apiType + " schemes: " + e.toString());
      }
    }
  }

  /**
   * Loads the map for a driver.
   */
  private HashMap<String,String> getDriverTypeMap(String apiType)
  {
    synchronized (_driverTypeMap) {
      HashMap<String,String> driverMap = _driverTypeMap.get(apiType);

      if (driverMap == null) {
        driverMap = new HashMap<String,String>();

        if (_parent != null)
          driverMap.putAll(_parent.getDriverTypeMap(apiType));

        loadDriverTypeMap(driverMap, apiType);

        _driverTypeMap.put(apiType, driverMap);
      }

      return driverMap;
    }
  }

  /**
   * Reads the drivers from the META-INF/caucho
   */
  private void loadDriverTypeMap(HashMap<String,String> driverMap,
                                 String apiType)
  {
    try {
      ClassLoader loader = _loader;

      if (loader == null)
        loader = _systemClassLoader;

      Enumeration<URL> urls
        = loader.getResources("META-INF/caucho/com.caucho.config.uri/"
                              + apiType);

      while (urls.hasMoreElements()) {
        URL url = urls.nextElement();

        if (hasDriver(url))
          continue;

        _driverTypeSet.add(url);

        InputStream is = url.openStream();

        try {
          Properties props = new Properties();

          props.load(is);

          for (Map.Entry entry : props.entrySet()) {
            driverMap.put((String) entry.getKey(), (String) entry.getValue());
          }
        } finally {
          is.close();
        }
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  protected boolean hasDriver(URL url)
  {
    synchronized (_driverTypeSet) {
      if (_driverTypeSet.contains(url))
        return true;
      else if (_parent != null)
        return _parent.hasDriver(url);
      else
        return false;
    }
  }

  //
  // AddLoaderListener
  //

  public boolean isEnhancer()
  {
    return false;
  }

  /**
   * Called with the loader config changes.
   */
  public void addLoader(EnvironmentClassLoader loader)
  {
    init(loader);
  }

  //
  // Configuration methods
  //

  /**
   * Adds an new environment attribute.
   */
  public NamespaceConfig createNamespace()
  {
    return new NamespaceConfig();
  }

  /**
   * Adds an new environment attribute.
   */
  public void addNamespace(NamespaceConfig ns)
  {
    _nsMap.put(ns.getName(), ns);

    if (ns.isDefault())
      _nsMap.put(ns.getName(), ns);
  }

  // configuration types
  public class NamespaceConfig {
    private String _ns = "";
    private boolean _isDefault;
    private Path _path;

    private AtomicBoolean _isBeansLoaded = new AtomicBoolean();

    private HashMap<String,BeanConfig> _beanMap
      = new HashMap<String,BeanConfig>();

    public void setName(String ns)
    {
      if ("default".equals(ns))
        ns = "";

      _ns = ns;
    }

    public String getName()
    {
      return _ns;
    }

    public void setDefault(boolean isDefault)
    {
      _isDefault = isDefault;
    }

    public boolean isDefault()
    {
      return _isDefault;
    }

    public void setPath(String path)
    {
      if (path.indexOf(':') < 0)
        _path = Vfs.lookup("classpath:" + path);
      else
        _path = Vfs.lookup(path);
    }

    public Path getPath()
    {
      return _path;
    }

    public void loadBeans()
    {
      if (_isBeansLoaded.getAndSet(true))
        return;

      try {
        new Config().configure(this, _path);
      } catch (IOException e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    public ConfigType getBean(String name)
    {
      BeanConfig beanConfig = _beanMap.get(name);

      if (beanConfig != null)
        return beanConfig.getConfigType();
      else
        return null;
    }

    public BeanConfig createBean()
    {
      return new BeanConfig(_ns, _isDefault);
    }

    public void addBean(BeanConfig bean)
    {
      _beanMap.put(bean.getName(), bean);
    }

    public FlowConfig createFlow()
    {
      return new FlowConfig(_ns, _isDefault);
    }

    public void addFlow(FlowConfig flow)
    {
      _beanMap.put(flow.getName(), flow);
    }
  }

  public class BeanConfig {
    private String _ns;
    private boolean _isDefault;

    private String _name;
    private String _className;

    private ConfigType<?> _configType;
    private ClassLoader _loader;

    BeanConfig(String ns, boolean isDefault)
    {
      _ns = ns;
      _isDefault = isDefault;
      _loader = Thread.currentThread().getContextClassLoader();
    }

    public void setName(String name)
    {
      _name = name;
    }

    public String getName()
    {
      return _name;
    }

    public void setClass(String className)
    {
      _className = className;
    }

    public ConfigType<?> getConfigType()
    {
      try {
        if (_configType == null) {
          Class<?> cl = Class.forName(_className, false, _loader);

          ConfigType<?> type = createType(cl);

          type.introspect();

          _configType = type;
        }

        return _configType;
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }

    @PostConstruct
    public void init()
    {
      if (_name == null)
        throw new ConfigException(L.l("bean requires a 'name' attribute"));

      if (_className == null)
        throw new ConfigException(L.l("bean requires a 'class' attribute"));
    }
  }

  public class FlowConfig extends BeanConfig {
    FlowConfig(String ns, boolean isDefault)
    {
      super(ns, isDefault);
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _loader + "]";
  }

  static {
    _primitiveTypes.put(boolean.class, BooleanPrimitiveType.TYPE);
    _primitiveTypes.put(byte.class, BytePrimitiveType.TYPE);
    _primitiveTypes.put(short.class, ShortPrimitiveType.TYPE);
    _primitiveTypes.put(int.class, IntegerPrimitiveType.TYPE);
    _primitiveTypes.put(long.class, LongPrimitiveType.TYPE);
    _primitiveTypes.put(float.class, FloatPrimitiveType.TYPE);
    _primitiveTypes.put(double.class, DoublePrimitiveType.TYPE);
    _primitiveTypes.put(char.class, CharacterPrimitiveType.TYPE);

    _primitiveTypes.put(Boolean.class, BooleanType.TYPE);
    _primitiveTypes.put(Byte.class, ByteType.TYPE);
    _primitiveTypes.put(Short.class, ShortType.TYPE);
    _primitiveTypes.put(Integer.class, IntegerType.TYPE);
    _primitiveTypes.put(Long.class, LongType.TYPE);
    _primitiveTypes.put(Float.class, FloatType.TYPE);
    _primitiveTypes.put(Double.class, DoubleType.TYPE);
    _primitiveTypes.put(Character.class, CharacterType.TYPE);

    _primitiveTypes.put(Object.class, ObjectType.TYPE);

    _primitiveTypes.put(String.class, StringType.TYPE);
    _primitiveTypes.put(RawString.class, RawStringType.TYPE);

    _primitiveTypes.put(String[].class, StringArrayType.TYPE);

    _primitiveTypes.put(Class.class, ClassType.TYPE);
    _primitiveTypes.put(Path.class, PathType.TYPE);
    _primitiveTypes.put(File.class, FileType.TYPE);
    _primitiveTypes.put(URL.class, UrlType.TYPE);
    _primitiveTypes.put(Pattern.class, PatternType.TYPE);
    _primitiveTypes.put(Level.class, LevelBuilder.TYPE);
    _primitiveTypes.put(Locale.class, LocaleType.TYPE);
    _primitiveTypes.put(Node.class, NodeType.TYPE);
    _primitiveTypes.put(QDate.class, QDateType.TYPE);
    _primitiveTypes.put(Date.class, DateType.TYPE);
    _primitiveTypes.put(Properties.class, PropertiesType.TYPE);
    
    _primitiveTypes.put(Expr.class, ExprType.TYPE);

    // _primitiveTypes.put(DataSource.class, DataSourceType.TYPE);

    _primitiveTypes.put(MethodExpression.class, MethodExpressionType.TYPE);

    ClassLoader systemClassLoader = null;

    try {
      systemClassLoader = ClassLoader.getSystemClassLoader();
    } catch (Exception e) {
    }

    _systemClassLoader = systemClassLoader;
  }
}
