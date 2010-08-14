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

package com.caucho.quercus.module;

import com.caucho.config.ConfigException;
import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.env.*;
import com.caucho.quercus.expr.ExprFactory;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.marshal.Marshal;
import com.caucho.quercus.marshal.MarshalFactory;
import com.caucho.quercus.program.ClassDef;
import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.quercus.program.JavaArrayClassDef;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class-loader specific context for loaded PHP.
 */
public class ModuleContext
{
  private static L10N L = new L10N(ModuleContext.class);
  private static final Logger log
    = Logger.getLogger(ModuleContext.class.getName());

  private ClassLoader _loader;

  private ModuleContext _parent;

  private HashSet<URL> _serviceClassUrls = new HashSet<URL>();
  private HashSet<URL> _serviceModuleUrls = new HashSet<URL>();

  private HashMap<String, ModuleInfo> _moduleInfoMap
    = new HashMap<String, ModuleInfo>();

  private HashSet<String> _extensionSet
    = new HashSet<String>();

  private ClassDef _stdClassDef;
  private QuercusClass _stdClass;

  private HashMap<String, ClassDef> _staticClasses
    = new HashMap<String, ClassDef>();

  private HashMap<String, JavaClassDef> _javaClassWrappers
    = new HashMap<String, JavaClassDef>();

  private HashMap<String, HashSet<String>> _extensionClasses
    = new HashMap<String, HashSet<String>>();

  protected MarshalFactory _marshalFactory;
  protected ExprFactory _exprFactory;

  /**
   * Constructor.
   */
  private ModuleContext(ClassLoader loader)
  {
    _loader = loader;
    
    _marshalFactory = new MarshalFactory(this);
    _exprFactory = new ExprFactory();
    
    _stdClassDef = new InterpretedClassDef("stdClass", null, new String[0]);
    _stdClass = new QuercusClass(this, _stdClassDef, null);

    _staticClasses.put(_stdClass.getName(), _stdClassDef);
  }

  /**
   * Constructor.
   */
  public ModuleContext(ModuleContext parent, ClassLoader loader)
  {
    this(loader);

    _parent = parent;

    if (parent != null) {
      _serviceClassUrls.addAll(parent._serviceClassUrls);
      _serviceModuleUrls.addAll(parent._serviceModuleUrls);

      _moduleInfoMap.putAll(parent._moduleInfoMap);
      _extensionSet.addAll(parent._extensionSet);
      _staticClasses.putAll(parent._staticClasses);
      _javaClassWrappers.putAll(parent._javaClassWrappers);
      _extensionClasses.putAll(parent._extensionClasses);
    }
  }

  public static ModuleContext getLocalContext(ClassLoader loader)
  {
    throw new UnsupportedOperationException();
    /*
    ModuleContext context = _localModuleContext.getLevel(loader);

    if (context == null) {
      context = new ModuleContext(loader);
      _localModuleContext.set(context, loader);
    }

    return context;
    */
  }

  /**
   * Tests if the URL has already been loaded for the context classes
   */
  public boolean hasServiceClass(URL url)
  {
    return _serviceClassUrls.contains(url);
  }

  /**
   * Adds a URL for the context classes
   */
  public void addServiceClass(URL url)
  {
    _serviceClassUrls.add(url);
  }

  /**
   * Tests if the URL has already been loaded for the context module
   */
  public boolean hasServiceModule(URL url)
  {
    return _serviceModuleUrls.contains(url);
  }

  /**
   * Adds a URL for the context module
   */
  public void addServiceModule(URL url)
  {
    _serviceModuleUrls.add(url);
  }

  /**
   * Adds module info.
   */
  public ModuleInfo addModule(String name, QuercusModule module)
    throws ConfigException
  {
    synchronized (this) {
      ModuleInfo info = _moduleInfoMap.get(name);

      if (info == null) {
        info = new ModuleInfo(this, name, module);
        _moduleInfoMap.put(name, info);
      }

      return info;
    }
  }

  public JavaClassDef addClass(String name, Class type,
                               String extension, Class javaClassDefClass)
    throws NoSuchMethodException,
           InvocationTargetException,
           IllegalAccessException,
           InstantiationException
  {
    synchronized (_javaClassWrappers) {
      JavaClassDef def = _javaClassWrappers.get(name);

      if (def == null) {
        if (log.isLoggable(Level.FINEST)) {
          if (extension == null)
            log.finest(L.l("PHP loading class {0} with type {1}",
                           name,
                           type.getName()));
          else
            log.finest(L.l(
              "PHP loading class {0} with type {1} providing extension {2}",
              name,
              type.getName(),
              extension));
      }

      if (javaClassDefClass != null) {
        Constructor constructor
          = javaClassDefClass.getConstructor(ModuleContext.class,
                                             String.class,
                                             Class.class);

        def = (JavaClassDef) constructor.newInstance(this, name, type);
      }
      else {
        def = JavaClassDef.create(this, name, type);

        if (def == null)
          def = createDefaultJavaClassDef(name, type, extension);
      }
      
      def.setPhpClass(true);

      _javaClassWrappers.put(name, def);
      // _lowerJavaClassWrappers.put(name.toLowerCase(), def);

      _staticClasses.put(name, def);
      // _lowerStaticClasses.put(name.toLowerCase(), def);

      // def.introspect();

      if (extension != null)
        _extensionSet.add(extension);
      }

      return def;
    }
  }

  /**
   * Gets or creates a JavaClassDef for the given class name.
   */
  public JavaClassDef getJavaClassDefinition(Class type, String className)
  {
    JavaClassDef def;
    
    synchronized (_javaClassWrappers) {
      def = _javaClassWrappers.get(className);

      if (def != null)
        return def;

      def = JavaClassDef.create(this, className, type);

      if (def == null)
        def = createDefaultJavaClassDef(className, type);

      _javaClassWrappers.put(className, def);
      _javaClassWrappers.put(type.getName(), def);
    }

    return def;
  }
  
  /**
   * Adds a java class
   */
  public JavaClassDef getJavaClassDefinition(String className)
  {
    // Note, this method must not trigger an introspection to avoid
    // any race conditions.  It is only responsible for creating the
    // wrapper around the class, i.e. it's a leaf node, not a recursive not

    synchronized (_javaClassWrappers) {
      JavaClassDef def = _javaClassWrappers.get(className);

      if (def != null)
        return def;

      try {
        Class type;

        try {
            type = Class.forName(className, false, _loader);
        }
        catch (ClassNotFoundException e) {
          throw new ClassNotFoundException(
            L.l("'{0}' is not a known Java class: {1}",
                className,
                e.toString()), e);
        }

        def = JavaClassDef.create(this, className, type);

        if (def == null)
          def = createDefaultJavaClassDef(className, type);

        _javaClassWrappers.put(className, def);
        _javaClassWrappers.put(type.getName(), def);

        // def.introspect();

        return def;
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new QuercusRuntimeException(e);
      }
    }
  }

  /**
   * Returns a javaClassDef for the given class or null if there is not one.
   */
  public JavaClassDef getJavaClassDefinition(Class javaClass)
  {
    synchronized (_javaClassWrappers) {
      return _javaClassWrappers.get(javaClass.getName());
    }
  }


  protected JavaClassDef createDefaultJavaClassDef(String className,
                                                   Class type)
  {
    if (type.isArray())
      return new JavaArrayClassDef(this, className, type);
    else
      return new JavaClassDef(this, className, type);
  }
  
  protected JavaClassDef createDefaultJavaClassDef(String className,
                                                   Class type,
                                                   String extension)
  {
    if (type.isArray())
      return new JavaArrayClassDef(this, className, type, extension);
    else
      return new JavaClassDef(this, className, type, extension);
  }
  
  /**
   * Finds the java class wrapper.
   */
  /*
  public ClassDef findJavaClassWrapper(String name)
  {
    synchronized (_javaClassWrappers) {
      ClassDef def = _javaClassWrappers.get(name);

      if (def != null)
        return def;

      return _lowerJavaClassWrappers.get(name.toLowerCase());
    }
  }
  */

  public MarshalFactory getMarshalFactory()
  {
    return _marshalFactory;
  }

  public ExprFactory getExprFactory()
  {
    return _exprFactory;
  }
  
  public Marshal createMarshal(Class type,
                               boolean isNotNull,
                               boolean isNullAsFalse)
  {
    return getMarshalFactory().create(type, isNotNull, isNullAsFalse);
  }

  /**
   * Returns an array of the defined functions.
   */
  /*
  public ArrayValue getDefinedFunctions()
  {
    ArrayValue internal = new ArrayValueImpl();

    synchronized (_staticFunctions) {
      for (String name : _staticFunctions.keySet()) {
        internal.put(name);
      }
    }

    return internal;
  }
  */

  /**
   * Returns the stdClass definition.
   */
  public QuercusClass getStdClass()
  {
    return _stdClass;
  }

  /**
   * Returns the class with the given name.
   */
  /*
  public ClassDef findClass(String name)
  {
    synchronized (_staticClasses) {
      ClassDef def = _staticClasses.get(name);

      if (def == null)
        def = _lowerStaticClasses.get(name.toLowerCase());

      return def;
    }
  }
  */

  /**
   * Returns the class maps.
   */
  public HashMap<String, ClassDef> getClassMap()
  {
    synchronized (_staticClasses) {
      return new HashMap<String,ClassDef>(_staticClasses);
    }
  }

  /**
   * Returns the class maps.
   */
  public HashMap<String, JavaClassDef> getWrapperMap()
  {
    synchronized (_javaClassWrappers) {
      return new HashMap<String,JavaClassDef>(_javaClassWrappers);
    }
  }

  /**
   * Returns the module with the given name.
   */
  public QuercusModule findModule(String name)
  {
    ModuleInfo info = _moduleInfoMap.get(name);

    if (info != null)
      return info.getModule();
    else
      return null;
  }

  /**
   * Returns true if an extension is loaded.
   */
  public boolean isExtensionLoaded(String name)
  {
    return _extensionSet.contains(name);
  }

  /**
   * Returns true if an extension is loaded.
   */
  public HashSet<String> getLoadedExtensions()
  {
    return _extensionSet;
  }
  
  /*
   * Adds a class to the extension's list of classes.
   */
  public void addExtensionClass(String ext, String clsName)
  {
    HashSet<String> list = _extensionClasses.get(ext);
    
    if (list == null) {
      list = new HashSet<String>();
      _extensionClasses.put(ext, list);
    }
    
    list.add(clsName);
  }
  
  /*
   * Returns the list of the classes that are part of this extension.
   */
  public HashSet<String> getExtensionClasses(String ext)
  {
    return _extensionClasses.get(ext);
  }

  /**
   * Creates a static function.
   */
  public StaticFunction createStaticFunction(QuercusModule module,
                                             Method method)
  {
    return new StaticFunction(this, module, method);
  }

  public void init()
  {
    initStaticFunctions();
    initStaticClassServices();
    
    //initStaticClasses();
  }

  /**
   * Scans the classpath for META-INF/services/com.caucho.quercus.QuercusModule
   */
  private void initStaticFunctions()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      setContextClassLoader(_loader);
      
      String quercusModule
        = "META-INF/services/com.caucho.quercus.QuercusModule";
      Enumeration<URL> urls = _loader.getResources(quercusModule);

      HashSet<URL> urlSet = new HashSet<URL>();

      // gets rid of duplicate entries found by different classloaders
      while (urls.hasMoreElements()) {
        URL url = urls.nextElement();

        if (! hasServiceModule(url)) {
          addServiceModule(url);

          urlSet.add(url);
        }
      }

      for (URL url : urlSet) {
        InputStream is = null;
        ReadStream rs = null;
        try {
          is = url.openStream();

          rs = new ReadStream(new VfsStream(is, null));

          parseServicesModule(rs);
        } catch (Throwable e) {
          log.log(Level.FINE, e.toString(), e);
        } finally {
          if (rs != null)
            rs.close();
          if (is != null)
            is.close();
        }
      }

    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      setContextClassLoader(oldLoader);
    }
  }

  /**
   * Parses the services file, looking for PHP services.
   */
  private void parseServicesModule(ReadStream in)
    throws IOException, ClassNotFoundException,
           IllegalAccessException, InstantiationException
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    String line;

    while ((line = in.readLine()) != null) {
      int p = line.indexOf('#');

      if (p >= 0)
        line = line.substring(0, p);

      line = line.trim();

      if (line.length() > 0) {
        String className = line;

        try {
          Class cl;
          try {
            cl = Class.forName(className, false, loader);
          }
          catch (ClassNotFoundException e) {
            throw new ClassNotFoundException(L.l(
              "'{0}' not valid {1}", className, e.toString()));
          }

          introspectPhpModuleClass(cl);
        } catch (Throwable e) {
          log.fine("Failed loading " + className + "\n" + e.toString());
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }
  }

  /**
   * Encapsulate setContextClassLoader for contexts where the
   * security manager is set.
   */
  protected void setContextClassLoader(ClassLoader loader)
  {
    Thread thread = Thread.currentThread();
    ClassLoader currentLoader = thread.getContextClassLoader();

    // to avoid security manager in GoogleAppEngine, skip the setting
    // if the loader is the current loader
    if (loader != currentLoader)
      thread.setContextClassLoader(loader);
  }

  /**
   * Returns the configured modules
   */
  public ArrayList<ModuleInfo> getModules()
  {
    synchronized (_moduleInfoMap) {
      return new ArrayList<ModuleInfo>(_moduleInfoMap.values());
    }
  }

  /**
   * Introspects the module class for functions.
   *
   * @param cl the class to introspect.
   */
  private void introspectPhpModuleClass(Class cl)
    throws IllegalAccessException, InstantiationException, ConfigException
  {
    synchronized (_moduleInfoMap) {
      if (_moduleInfoMap.get(cl.getName()) != null)
        return;

      log.finest(getClass().getSimpleName() 
                 + " loading module " 
                 + cl.getName());

      QuercusModule module = (QuercusModule) cl.newInstance();

      ModuleInfo info = addModule(cl.getName(), module);

      /*
      _modules.put(cl.getName(), info);

      if (info.getModule() instanceof ModuleStartupListener)
        _moduleStartupListeners.add((ModuleStartupListener)info.getModule());

      for (String ext : info.getLoadedExtensions())
        _extensionSet.add(ext);

      Map<String, Value> map = info.getConstMap();

      if (map != null)
        _constMap.putAll(map);

      _iniDefinitions.addAll(info.getIniDefinitions());

      synchronized (_staticFunctionMap) {
        for (Map.Entry<String, AbstractFunction> entry
               : info.getFunctions().entrySet()) {
          String funName = entry.getKey();
          AbstractFunction fun = entry.getValue();
      
          _staticFunctionMap.put(funName, fun);

          // _lowerFunMap.put(funName.toLowerCase(), fun);

          int id = getFunctionId(funName);
          _functionMap[id] = fun;
        }
      }
      */
    }
  }

  /**
   * Scans the classpath for META-INF/services/com.caucho.quercus.QuercusClass
   */
  private void initStaticClassServices()
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    try {
      String quercusModule
        = "META-INF/services/com.caucho.quercus.QuercusClass";
      Enumeration<URL> urls = loader.getResources(quercusModule);
      
      HashSet<URL> urlSet = new HashSet<URL>();

      // gets rid of duplicate entries found by different classloaders
      while (urls.hasMoreElements()) {
        URL url = urls.nextElement();

        if (! hasServiceClass(url)) {
          addServiceClass(url);

          urlSet.add(url);
        }
      }

      for (URL url : urlSet) {
        InputStream is = null;
        ReadStream rs = null;
        try {
          is = url.openStream();

          rs = new ReadStream(new VfsStream(is, null));

          parseClassServicesModule(rs);
        } catch (Throwable e) {
          log.log(Level.FINE, e.toString(), e);
        } finally {
          if (rs != null)
            rs.close();
          if (is != null)
            is.close();
        }
      }

    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Parses the services file, looking for PHP services.
   */
  private void parseClassServicesModule(ReadStream in)
    throws IOException, ClassNotFoundException,
           IllegalAccessException, InstantiationException,
           ConfigException, NoSuchMethodException, InvocationTargetException
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    String line;

    while ((line = in.readLine()) != null) {
      int p = line.indexOf('#');

      if (p >= 0)
        line = line.substring(0, p);

      line = line.trim();

      if (line.length() == 0)
        continue;

      String[] args = line.split(" ");

      String className = args[0];

      Class cl;

      try {
        cl = Class.forName(className, false, loader);

        String phpClassName = null;
        String extension = null;
        String definedBy = null;

        for (int i = 1; i < args.length; i++) {
          if ("as".equals(args[i])) {
            i++;
            if (i >= args.length)
              throw new IOException(
                L.l(
                  "expecting Quercus class name after '{0}' "
                  + "in definition for class {1}",
                  "as",
                  className));

            phpClassName = args[i];
          }
          else if ("provides".equals(args[i])) {
            i++;
            if (i >= args.length)
              throw new IOException(
                L.l(
                  "expecting name of extension after '{0}' "
                  + "in definition for class {1}",
                  "extension",
                  className));

            extension = args[i];
          }
          else if ("definedBy".equals(args[i])) {
            i++;
            if (i >= args.length)
              throw new IOException(L.l(
                "expecting name of class implementing JavaClassDef after '{0}' "
                + "in definition for class {1}",
                "definedBy",
                className));

            definedBy = args[i];
          }
          else {
            throw new IOException(L.l(
              "unknown token '{0}' in definition for class {1} ",
              args[i],
              className));
          }
        }

        if (phpClassName == null)
          phpClassName = className.substring(className.lastIndexOf('.') + 1);

        Class javaClassDefClass;

        if (definedBy != null) {
          javaClassDefClass = Class.forName(definedBy, false, loader);
        }
        else
          javaClassDefClass = null;

        introspectJavaClass(phpClassName, cl, extension, javaClassDefClass);
      } catch (Exception e) {
        log.fine("Failed loading " + className + "\n" + e.toString());
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }

  /**
   * Introspects the module class for functions.
   *
   * @param name the php class name
   * @param type the class to introspect.
   * @param extension the extension provided by the class, or null
   * @param javaClassDefClass
   */
  public void introspectJavaClass(String name, Class type, String extension,
                                  Class javaClassDefClass)
    throws IllegalAccessException, InstantiationException, ConfigException,
           NoSuchMethodException, InvocationTargetException
  {
    JavaClassDef def = addClass(name, type, extension, javaClassDefClass);

    synchronized (_javaClassWrappers) {
      _javaClassWrappers.put(name, def);
      _javaClassWrappers.put(type.getName(), def);
      // _lowerJavaClassWrappers.put(name.toLowerCase(), def);
    }

    if (extension != null)
      _extensionSet.add(extension);
  }

  /**
   * Introspects the module class for functions.
   *
   * @param name the php class name
   * @param type the class to introspect.
   * @param extension the extension provided by the class, or null
   */
  public void introspectJavaImplClass(String name,
                                      Class type,
                                      String extension)
    throws IllegalAccessException, InstantiationException, ConfigException
  {
    if (log.isLoggable(Level.FINEST)) {
      if (extension == null)
        log.finest(L.l("Quercus loading class {0} with type {1}",
                       name,
                       type.getName()));
      else
        log.finest(
          L.l("Quercus loading class {0} with type {1} providing extension {2}",
              name,
              type.getName(),
              extension));
    }

    try {
      JavaClassDef def = addClass(name, type, extension, null);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Scans the classpath for META-INF/services/com.caucho.quercus.QuercusClass
   */
  private void initStaticClasses()
  {
    /*
    _stdClassDef = new InterpretedClassDef("stdClass", null, new String[0]);
    _stdClass = new QuercusClass(_stdClassDef, null);

    _staticClasses.put(_stdClass.getName(), _stdClassDef);
    _lowerStaticClasses.put(_stdClass.getName().toLowerCase(), _stdClassDef);

    InterpretedClassDef exn = new InterpretedClassDef("Exception",
                                                      null,
                                                      new String[0]);

    try {
      exn.setConstructor(new StaticFunction(_moduleContext,
                                            null,
                                            Quercus.class.getMethod(
                                              "exnConstructor",
                                              new Class[]{ Env.class,
                                                           Value.class,
                                                           String.class })));
    }
    catch (Exception e) {
      throw new QuercusException(e);
    }

    // QuercusClass exnCl = new QuercusClass(exn, null);

    _staticClasses.put(exn.getName(), exn);
    _lowerStaticClasses.put(exn.getName().toLowerCase(), exn);
    */
  }

  public static Value objectToValue(Object obj)
  {
    if (obj == null)
      return NullValue.NULL;
    else if (Byte.class.equals(obj.getClass())
             || Short.class.equals(obj.getClass())
             || Integer.class.equals(obj.getClass())
             || Long.class.equals(obj.getClass())) {
      return LongValue.create(((Number) obj).longValue());
    } else if (Float.class.equals(obj.getClass())
               || Double.class.equals(obj.getClass())) {
      return DoubleValue.create(((Number) obj).doubleValue());
    } else if (String.class.equals(obj.getClass())) {
      // XXX: i18n
      return new StringBuilderValue((String) obj);
    } else {
      // XXX: unknown types, e.g. Character?

      return null;
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _loader + "]";
  }
}

