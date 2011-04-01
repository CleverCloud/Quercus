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

package com.caucho.config;

import com.caucho.config.attribute.*;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.type.*;
import com.caucho.config.types.*;
import com.caucho.config.xml.XmlConfigContext;
import com.caucho.el.EL;
import com.caucho.el.EnvironmentContext;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.relaxng.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.xml.DOMBuilder;
import com.caucho.xml.QDocument;
import com.caucho.xml.QName;
import com.caucho.xml.QAttr;
import com.caucho.xml.Xml;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.el.ELContext;
import javax.el.ELException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.*;
import java.net.*;
import java.util.HashMap;
import java.util.logging.*;

/**
 * Facade for Resin's configuration builder.
 */
public class Config {
  private static final L10N L = new L10N(Config.class);
  private static final Logger log
    = Logger.getLogger(Config.class.getName());

  private static final EnvironmentLocal<ConfigProperties> _envProperties
    = new EnvironmentLocal<ConfigProperties>();

  // the context class loader of the config
  private ClassLoader _classLoader;

  private boolean _isEL = true;
  private boolean _isIgnoreEnvironment;
  private boolean _allowResinInclude;

  public Config()
  {
    this(Thread.currentThread().getContextClassLoader());
  }

  /**
   * @param loader the class loader environment to use.
   */
  public Config(ClassLoader loader)
  {
    _classLoader = loader;
 }

  /**
   * Set true if resin:include should be allowed.
   */
  public void setResinInclude(boolean useResinInclude)
  {
    _allowResinInclude = useResinInclude;
  }

  /**
   * True if EL expressions are allowed
   */
  public boolean isEL()
  {
    return _isEL;
  }

  /**
   * True if EL expressions are allowed
   */
  public void setEL(boolean isEL)
  {
    _isEL = isEL;
  }

  /**
   * True if environment tags are ignored
   */
  public boolean isIgnoreEnvironment()
  {
    return _isIgnoreEnvironment;
  }

  /**
   * True if environment tags are ignored
   */
  public void setIgnoreEnvironment(boolean isIgnore)
  {
    _isIgnoreEnvironment = isIgnore;
  }

  /**
   * Returns an environment property
   */
  public static Object getProperty(String key)
  {
    ConfigProperties props = _envProperties.get();

    if (props != null)
      return props.get(key);
    else
      return null;
  }

  /**
   * Sets a environment property
   */
  public static void setProperty(String key, Object value)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    setProperty(key, value, loader);
  }

  /**
   * Sets a environment property
   */
  public static void setProperty(String key, Object value, ClassLoader loader)
  {
    ConfigProperties props = _envProperties.getLevel(loader);

    if (props == null) {
      props = createConfigProperties(loader);
    }

    props.put(key, value);
  }

  private static ConfigProperties createConfigProperties(ClassLoader loader)
  {
    EnvironmentClassLoader envLoader
      = Environment.getEnvironmentClassLoader(loader);

    ConfigProperties props = _envProperties.getLevel(envLoader);

    if (props != null)
      return props;

    if (envLoader != null) {
      ConfigProperties parent = createConfigProperties(envLoader.getParent());

      props = new ConfigProperties(parent);
    }
    else
      props = new ConfigProperties(null);

    _envProperties.set(props, envLoader);

    return props;
  }

  /**
   * Configures a bean with a configuration file.
   */
  public Object configure(Object obj, Path path)
    throws ConfigException, IOException
  {
    try {
      QDocument doc = parseDocument(path, null);

      return configure(obj, doc.getDocumentElement());
    } catch (RuntimeException e) {
      throw e;
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Configures a bean with a configuration file.
   */
  public Object configure(Object obj, InputStream is)
    throws Exception
  {
    QDocument doc = parseDocument(is, null);

    return configure(obj, doc.getDocumentElement());
  }

  /**
   * Configures a bean with a configuration file and schema.
   */
  public Object configure(Object obj, Path path, String schemaLocation)
    throws ConfigException
  {
    try {
      Schema schema = findCompactSchema(schemaLocation);

      QDocument doc = parseDocument(path, schema);

      return configure(obj, doc.getDocumentElement());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw LineConfigException.create(e);
    }
  }

  /**
   * Configures a bean with a configuration file and schema.
   */
  public Object configure(Object obj, Path path, Schema schema)
    throws ConfigException
  {
    try {
      QDocument doc = parseDocument(path, schema);

      return configure(obj, doc.getDocumentElement());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Configures a bean with a configuration file.
   */
  public Object configure(Object obj,
                          InputStream is,
                          String schemaLocation)
    throws Exception
  {
    Schema schema = findCompactSchema(schemaLocation);

    QDocument doc = parseDocument(is, schema);

    return configure(obj, doc.getDocumentElement());
  }

  /**
   * Configures a bean with a configuration file.
   */
  public Object configure(Object obj,
                          InputStream is,
                          Schema schema)
    throws Exception
  {
    QDocument doc = parseDocument(is, schema);

    return configure(obj, doc.getDocumentElement());
  }

  /**
   * Configures a bean with a DOM.
   */
  public Object configure(Object obj, Node topNode)
    throws Exception
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      XmlConfigContext builder = createBuilder();

      InjectManager cdiManager = InjectManager.create();

      setProperty("__FILE__", FileVar.__FILE__);
      setProperty("__DIR__", DirVar.__DIR__);

      return builder.configure(obj, topNode);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Configures a bean with a configuration file and schema.
   */
  public void configureBean(Object obj,
                            Path path,
                            String schemaLocation)
    throws Exception
  {
    Schema schema = findCompactSchema(schemaLocation);

    QDocument doc = parseDocument(path, schema);

    configureBean(obj, doc.getDocumentElement());
  }

  /**
   * Configures a bean with a configuration file and schema.
   */
  public void configureBean(Object obj, Path path)
    throws Exception
  {
    QDocument doc = parseDocument(path, null);

    configureBean(obj, doc.getDocumentElement());
  }

  /**
   * Configures a bean with a DOM.  configureBean does not
   * apply init() or replaceObject().
   */
  public void configureBean(Object obj, Node topNode)
    throws Exception
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      XmlConfigContext builder = createBuilder();

      InjectManager webBeans = InjectManager.create();

      setProperty("__FILE__", FileVar.__FILE__);
      setProperty("__DIR__", DirVar.__DIR__);

      builder.configureBean(obj, topNode);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private XmlConfigContext createBuilder()
  {
    return new XmlConfigContext(this);
  }

  /**
   * Configures a bean with a configuration file and schema.
   */
  public void configureBean(Object obj,
                            Path path,
                            Schema schema)
    throws Exception
  {
    QDocument doc = parseDocument(path, schema);

    configureBean(obj, doc.getDocumentElement());
  }

  /**
   * Configures the bean from a path
   */
  private QDocument parseDocument(Path path, Schema schema)
    throws LineConfigException, IOException, org.xml.sax.SAXException
  {
    // server/2d33
    SoftReference<QDocument> docRef = null;//_parseCache.get(path);
    QDocument doc;

    if (docRef != null) {
      doc = docRef.get();

      if (doc != null && ! doc.isModified())
        return doc;
    }

    ReadStream is = path.openRead();

    try {
      doc = parseDocument(is, schema);

      // _parseCache.put(path, new SoftReference<QDocument>(doc));

      return doc;
    } finally {
      is.close();
    }
  }

  /**
   * Configures the bean from an input stream.
   */
  private QDocument parseDocument(InputStream is, Schema schema)
    throws LineConfigException,
           IOException,
           org.xml.sax.SAXException
  {
    QDocument doc = new QDocument();
    DOMBuilder builder = new DOMBuilder();

    builder.init(doc);
    String systemId = null;
    String filename = null;
    if (is instanceof ReadStream) {
      systemId = ((ReadStream) is).getPath().getURL();
      filename = ((ReadStream) is).getPath().getUserPath();
    }

    doc.setSystemId(systemId);
    builder.setSystemId(systemId);
    doc.setRootFilename(filename);
    builder.setFilename(filename);
    builder.setSkipWhitespace(true);

    InputSource in = new InputSource();
    in.setByteStream(is);
    in.setSystemId(systemId);

    Xml xml = new Xml();
    xml.setOwner(doc);
    xml.setResinInclude(_allowResinInclude);
    xml.setFilename(filename);

    if (schema != null) {
      Verifier verifier = schema.newVerifier();
      VerifierFilter filter = verifier.getVerifierFilter();

      filter.setParent(xml);
      filter.setContentHandler(builder);
      filter.setErrorHandler(builder);

      filter.parse(in);
    }
    else {
      xml.setContentHandler(builder);
      xml.parse(in);
    }

    return doc;
  }

  private Schema findCompactSchema(String location)
    throws IOException, ConfigException
  {
    try {
      if (location == null)
        return null;

      Thread thread = Thread.currentThread();
      ClassLoader loader = thread.getContextClassLoader();

      if (loader == null)
        loader = ClassLoader.getSystemClassLoader();

      URL url = loader.getResource(location);

      if (url == null)
        return null;

      Path path = Vfs.lookup(URLDecoder.decode(url.toString()));

      // VerifierFactory factory = VerifierFactory.newInstance("http://caucho.com/ns/compact-relax-ng/1.0");

      CompactVerifierFactoryImpl factory;
      factory = new CompactVerifierFactoryImpl();

      return factory.compileSchema(path);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Returns true if the class can be instantiated.
   */
  public static void checkCanInstantiate(Class beanClass)
    throws ConfigException
  {
    if (beanClass == null)
      throw new ConfigException(L.l("null classes can't be instantiated."));
    else if (beanClass.isInterface())
      throw new ConfigException(L.l("'{0}' must be a concrete class.  Interfaces cannot be instantiated.", beanClass.getName()));
    else if (! Modifier.isPublic(beanClass.getModifiers()))
      throw new ConfigException(L.l("Custom bean class '{0}' is not public.  Bean classes must be public, concrete, and have a zero-argument constructor.", beanClass.getName()));
    else if (Modifier.isAbstract(beanClass.getModifiers()))
      throw new ConfigException(L.l("Custom bean class '{0}' is abstract.  Bean classes must be public, concrete, and have a zero-argument constructor.", beanClass.getName()));

    Constructor []constructors = beanClass.getDeclaredConstructors();

    Constructor constructor = null;

    for (int i = 0; i < constructors.length; i++) {
      if (constructors[i].getParameterTypes().length == 0) {
        constructor = constructors[i];
        break;
      }
    }

    if (constructor == null)
      throw new ConfigException(L.l("Custom bean class '{0}' doesn't have a zero-arg constructor.  Bean classes must be have a zero-argument constructor.", beanClass.getName()));

    if (! Modifier.isPublic(constructor.getModifiers())) {
      throw new ConfigException(L.l("The zero-argument constructor for '{0}' isn't public.  Bean classes must have a public zero-argument constructor.", beanClass.getName()));
    }
  }

  /**
   * Returns true if the class can be instantiated.
   */
  public static void validate(Class cl, Class api)
    throws ConfigException
  {
    checkCanInstantiate(cl);

    if (! api.isAssignableFrom(cl)) {
      throw new ConfigException(L.l("{0} must implement {1}.",
                                    cl.getName(), api.getName()));
    }
  }

  /**
   * Returns true if the class can be instantiated using zero args constructor
   * or constructor that accepts an instance of class passed in type argument
   */
  public static void checkCanInstantiate(Class beanClass,
                                         Class type)
    throws ConfigException
  {
    if (beanClass == null)
      throw new ConfigException(L.l("null classes can't be instantiated."));
    else if (beanClass.isInterface())
      throw new ConfigException(L.l(
        "'{0}' must be a concrete class.  Interfaces cannot be instantiated.",
        beanClass.getName()));
    else if (! Modifier.isPublic(beanClass.getModifiers()))
      throw new ConfigException(L.l(
        "Custom bean class '{0}' is not public.  Bean classes must be public, concrete, and have a zero-argument constructor.",
        beanClass.getName()));
    else if (Modifier.isAbstract(beanClass.getModifiers()))
      throw new ConfigException(L.l(
        "Custom bean class '{0}' is abstract.  Bean classes must be public, concrete, and have a zero-argument constructor.",
        beanClass.getName()));

    Constructor [] constructors = beanClass.getDeclaredConstructors();

    Constructor zeroArgsConstructor = null;

    Constructor singleArgConstructor = null;

    for (int i = 0; i < constructors.length; i++) {
           if (constructors [i].getParameterTypes().length == 0) {
             zeroArgsConstructor = constructors [i];

             if (singleArgConstructor != null)
               break;
           }
           else if (type != null
                    && constructors [i].getParameterTypes().length == 1 &&
                    type.isAssignableFrom(constructors[i].getParameterTypes()[0])) {
             singleArgConstructor = constructors [i];

             if (zeroArgsConstructor != null)
               break;
           }
         }

    if (zeroArgsConstructor == null
        && singleArgConstructor == null)
      if (type != null)
        throw new ConfigException(L.l(
                                      "Custom bean class '{0}' doesn't have a zero-arg constructor, or a constructor accepting parameter of type '{1}'.  Bean class '{0}' must have a zero-argument constructor, or a constructor accepting parameter of type '{1}'",
                                      beanClass.getName(),
                                      type.getName()));
      else
        throw new ConfigException(L.l(
                                      "Custom bean class '{0}' doesn't have a zero-arg constructor.  Bean classes must have a zero-argument constructor.",
                                      beanClass.getName()));


    if (singleArgConstructor != null) {
      if (! Modifier.isPublic(singleArgConstructor.getModifiers()) &&
          (zeroArgsConstructor == null ||
           ! Modifier.isPublic(zeroArgsConstructor.getModifiers()))) {
        throw new ConfigException(L.l(
          "The constructor for bean '{0}' accepting parameter of type '{1}' is not public.  Constructor accepting parameter of type '{1}' must be public.",
          beanClass.getName(),
          type.getName()));
      }
    }
    else if (zeroArgsConstructor != null) {
      if (! Modifier.isPublic(zeroArgsConstructor.getModifiers()))
        throw new ConfigException(L.l(
          "The zero-argument constructor for '{0}' isn't public.  Bean classes must have a public zero-argument constructor.",
          beanClass.getName()));
    }
  }

  public static void validate(Class cl, Class api, Class type)
    throws ConfigException
  {
    checkCanInstantiate(cl, type);

    if (! api.isAssignableFrom(cl)) {
      throw new ConfigException(L.l("{0} must implement {1}.",
                                    cl.getName(), api.getName()));
    }
  }

  /**
   * Sets an attribute with a value.
   *
   * @param obj the bean to be set
   * @param attr the attribute name
   * @param value the attribute value
   */
  public static void setAttribute(Object obj, String attr, Object value)
  {
    ConfigType<?> type = TypeFactory.getType(obj.getClass());

    QName attrName = new QName(attr);
    Attribute attrStrategy = type.getAttribute(attrName);
    if (attrStrategy == null)
      throw new ConfigException(L.l("{0}: '{1}' is an unknown attribute.",
                                    obj.getClass().getName(),
                                    attrName.getName()));

    value = attrStrategy.getConfigType().valueOf(value);

    attrStrategy.setValue(obj, attrName, value);
  }

  /**
   * Sets an attribute with a value.
   *
   * @param obj the bean to be set
   * @param attr the attribute name
   * @param value the attribute value
   */
  public static void setStringAttribute(Object obj, String attr, String value)
    throws Exception
  {
    XmlConfigContext builder = new XmlConfigContext();
    QAttr qAttr = new QAttr(attr);
    qAttr.setValue(value);

    builder.configureAttribute(obj, qAttr);
  }

  public static void init(Object bean)
    throws ConfigException
  {
    try {
      ConfigType type = TypeFactory.getType(bean.getClass());

      type.init(bean);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public static void inject(Object bean)
    throws ConfigException
  {
    try {
      ConfigType type = TypeFactory.getType(bean.getClass());

      type.inject(bean);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public static Object replaceObject(Object bean) throws Exception
  {
    ConfigType type = TypeFactory.getType(bean.getClass());

    return type.replaceObject(bean);
  }

  /**
   * Returns the variable resolver.
   */
  public static ELContext getEnvironment()
  {
    XmlConfigContext builder = XmlConfigContext.getCurrentBuilder();

    if (builder != null) {
      return builder.getELContext();
    }
    else
      return EL.getEnvironment();
  }

  /**
   * Returns the variable resolver.
   */
  public static ConfigELContext getELContext()
  {
    XmlConfigContext builder = XmlConfigContext.getCurrentBuilder();

    if (builder != null) {
      return builder.getELContext();
    }
    else
      return null;
  }

  /**
   * Sets an EL configuration variable.
   */
  public static Object getCurrentVar(String var)
  {
    // return InjectManager.create().findByName(var);
    return getProperty(var);
  }

  /**
   * Evaluates an EL string in the context.
   */
  public static String evalString(String str)
    throws ELException
  {
    return EL.evalString(str, getEnvironment());
  }

  /**
   * Evaluates an EL string in the context.
   */
  public static String evalString(String str, HashMap<String,Object> varMap)
         throws ELException
  {
    return EL.evalString(str, getEnvironment(varMap));
  }

  /**
   * Evaluates an EL boolean in the context.
   */
  public static boolean evalBoolean(String str)
    throws ELException
  {
    return EL.evalBoolean(str, getEnvironment());
  }

  public static ELContext getEnvironment(HashMap<String,Object> varMap)
  {
    if (varMap != null)
      return new EnvironmentContext(varMap);
    else
      return new EnvironmentContext();
  }

  public static ConfigException error(Field field, String msg)
  {
    return new ConfigException(location(field) + msg);
  }

  public static ConfigException error(Method method, String msg)
  {
    return new ConfigException(location(method) + msg);
  }

  public static RuntimeException createLine(String systemId, int line,
                                            Throwable e)
  {
    while (e.getCause() != null
           && (e instanceof InstantiationException
               || e instanceof InvocationTargetException
               || e.getClass().equals(ConfigRuntimeException.class))) {
      e = e.getCause();
    }

    if (e instanceof LineConfigException)
      throw (LineConfigException) e;

    String lines = getSourceLines(systemId, line);
    String loc = systemId + ":" + line + ": ";

    if (e instanceof DisplayableException) {
      return new LineConfigException(loc + e.getMessage() + "\n" + lines, e);
    }
    else
      return new LineConfigException(loc + e + "\n" + lines, e);
  }

  public static String location(Field field)
  {
    String className = field.getDeclaringClass().getName();

    return className + "." + field.getName() + ": ";
  }

  public static String location(Method method)
  {
    String className = method.getDeclaringClass().getName();

    return className + "." + method.getName() + ": ";
  }

  private static String getSourceLines(String systemId, int errorLine)
  {
    if (systemId == null)
      return "";

    ReadStream is = null;
    try {
      is = Vfs.lookup().lookup(systemId).openRead();
      int line = 0;
      StringBuilder sb = new StringBuilder("\n\n");
      String text;
      while ((text = is.readLine()) != null) {
        line++;

        if (errorLine - 2 <= line && line <= errorLine + 2) {
          sb.append(line);
          sb.append(": ");
          sb.append(text);
          sb.append("\n");
        }
      }

      return sb.toString();
    } catch (IOException e) {
      log.log(Level.FINEST, e.toString(), e);

      return "";
    } finally {
      if (is != null)
        is.close();
    }
  }

  static class ConfigProperties {
    private ConfigProperties _parent;
    private HashMap<String,Object> _properties = new HashMap<String,Object>(8);

    ConfigProperties(ConfigProperties parent)
    {
      _parent = parent;
    }

    public Object get(String key)
    {
      Object value = _properties.get(key);

      if (value != null)
        return value;
      else if (_parent != null)
        return _parent.get(key);
      else
        return null;
    }

    public void put(String key, Object value)
    {
      _properties.put(key, value);
    }
  }
}

