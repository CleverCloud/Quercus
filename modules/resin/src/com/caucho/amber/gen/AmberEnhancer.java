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

package com.caucho.amber.gen;

import com.caucho.amber.field.AmberField;
import com.caucho.amber.manager.AmberContainer;
import com.caucho.amber.type.*;
import com.caucho.bytecode.Analyzer;
import com.caucho.bytecode.CodeVisitor;
import com.caucho.bytecode.ConstantPool;
import com.caucho.bytecode.FieldRefConstant;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JavaClass;
import com.caucho.bytecode.JavaMethod;
import com.caucho.bytecode.MethodRefConstant;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaCompiler;
import com.caucho.java.WorkDir;
import com.caucho.java.gen.ClassComponent;
import com.caucho.java.gen.DependencyComponent;
import com.caucho.java.gen.GenClass;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.loader.*;
import com.caucho.loader.enhancer.ClassEnhancer;
import com.caucho.loader.enhancer.EnhancerPrepare;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enhancing the java objects for Amber mapping.
 */
public class AmberEnhancer implements AmberGenerator, ClassEnhancer {
  private static final L10N L = new L10N(AmberEnhancer.class);
  private static final Logger log 
    = Logger.getLogger(AmberEnhancer.class.getName());

  private Path _configDirectory;
  private boolean _useHibernateFiles;

  private AmberContainer _amberContainer;

  private EnhancerPrepare _prepare;
  private Path _workDir;
  private Path _postWorkDir;

  private ArrayList<String> _pendingClassNames = new ArrayList<String>();

  public AmberEnhancer(AmberContainer amberContainer)
  {
    _amberContainer = amberContainer;
    _workDir = WorkDir.getLocalWorkDir().lookup("pre-enhance");
    _postWorkDir = WorkDir.getLocalWorkDir().lookup("post-enhance");

    _prepare = new EnhancerPrepare();
    _prepare.setClassLoader(Thread.currentThread().getContextClassLoader());
    _prepare.setWorkPath(WorkDir.getLocalWorkDir());
    _prepare.addEnhancer(this);
  }

  /**
   * Sets the config directory.
   */
  public void setConfigDirectory(Path dir)
  {
    _configDirectory = dir;
  }

  /**
   * Returns the work directory.
   */
  public Path getWorkDir()
  {
    return _workDir;
  }

  /**
   * Returns the work directory.
   */
  public Path getPostWorkDir()
  {
    return _postWorkDir;
  }

  /**
   * Initialize the enhancer.
   */
  public void init()
    throws Exception
  {
  }

  /**
   * Checks to see if the preloaded class is modified.
   */
  protected boolean isModified(Class preloadedClass)
  {
    try {
      Method init = preloadedClass.getMethod("_caucho_init",
                                             new Class[] { Path.class });


      if (_configDirectory != null)
        init.invoke(null, new Object[] { _configDirectory });
      else
        init.invoke(null, new Object[] { Vfs.lookup() });

      Method isModified = preloadedClass.getMethod("_caucho_is_modified",
                                                   new Class[0]);

      Object value = isModified.invoke(null, new Object[0]);

      if (Boolean.FALSE.equals(value)) {
        loadEntityType(preloadedClass, preloadedClass.getClassLoader());
        return false;
      }
      else
        return true;
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);

      return true;
    }
  }

  /**
   * Returns true if the class should be enhanced.
   */
  public boolean shouldEnhance(String className)
  {
    className = className.replace('/', '.');
    
    int p = className.lastIndexOf('-');

    if (p > 0)
      className = className.substring(0, p);

    p = className.lastIndexOf('$');

    if (p > 0)
      className = className.substring(0, p);

    AbstractEnhancedType type;

    type = _amberContainer.getEntity(className);

    if (type != null && type.isEnhanced())
      return true;

    type = _amberContainer.getMappedSuperclass(className);

    if (type != null && type.isEnhanced())
      return true;

    type = _amberContainer.getEmbeddable(className);

    if (type != null && type.isEnhanced())
      return true;

    type = _amberContainer.getListener(className);

    if (type != null && type.isEnhanced())
      return true;

    return false;

    /*
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();
      try {
      thread.setContextClassLoader(getRawLoader());

      Class baseClass = Class.forName(className, false, getRawLoader());

      type = loadEntityType(baseClass, getRawLoader());
      } catch (ClassNotFoundException e) {
      return false;
      } finally {
      thread.setContextClassLoader(oldLoader);
      }

      if (type == null)
      return false;

      return className.equals(type.getName()) || type.isFieldAccess();
    */
  }

  /**
   * Returns true if the class should be enhanced.
   */
  private EntityType loadEntityType(Class cl, ClassLoader loader)
  {
    EntityType parentType = null;

    for (; cl != null; cl = cl.getSuperclass()) {
      java.net.URL url;

      String className = cl.getName();

      EntityType type = _amberContainer.getEntity(className);

      if (parentType == null)
        parentType = type;

      if (type != null && ! type.startConfigure())
        return type;

      type = loadEntityTypeImpl(cl, loader);

      if (type != null && ! type.startConfigure())
        return type;
    }

    return parentType;
  }

  protected EntityType loadEntityTypeImpl(Class cl, ClassLoader rawLoader)
  {
    return null;
  }

  /**
   * Enhances the class.
   */
  public void preEnhance(JavaClass baseClass)
    throws Exception
  {
    EntityType type = _amberContainer.getEntity(baseClass.getName());

    if (type == null)
      type = _amberContainer.getMappedSuperclass(baseClass.getName());

    if (type != null && type.getParentType() != null) {
      String parentClass = type.getParentType().getInstanceClassName();
      baseClass.setSuperClass(parentClass.replace('.', '/'));
    }
  }

  /**
   * Enhances the class.
   */
  public void enhance(GenClass genClass,
                      JClass baseClass,
                      String extClassName)
    throws Exception
  {
    String className = baseClass.getName();

    EntityType type = _amberContainer.getEntity(className);

    if (type == null)
      type = _amberContainer.getMappedSuperclass(className);

    // Type can be null for subclasses and inner classes that need fixups
    if (type != null) {
      // type is EntityType or MappedSuperclassType

      log.info("Amber enhancing class " + className);

      // XXX: _amberContainerenceUnitenceUnit.configure();

      type.init();

      genClass.addInterfaceName(type.getComponentInterfaceName());

      genClass.addImport("java.util.logging.*");
      genClass.addImport("com.caucho.amber.manager.*");
      genClass.addImport("com.caucho.amber.entity.*");
      genClass.addImport("com.caucho.amber.type.*");

      AmberMappedComponent componentGenerator
        = (AmberMappedComponent) type.getComponentGenerator();
      

      componentGenerator.setRelatedType((EntityType) type);
      componentGenerator.setBaseClassName(baseClass.getName());
      componentGenerator.setExtClassName(extClassName);

      genClass.addComponent(componentGenerator);

      DependencyComponent dependency = genClass.addDependencyComponent();
      dependency.addDependencyList(type.getDependencies());

      return;

      //_amberContainerenceUnitenceUnit.generate();
      // generate(type);

      // compile();

      // XXX: _amberContainerenceUnitenceUnit.initEntityHomes();
    }

    ListenerType listenerType = _amberContainer.getListener(className);

    // Type can be null for subclasses and inner classes that need fixups
    if (listenerType != null) {
      if (log.isLoggable(Level.INFO))
        log.log(Level.INFO, "Amber enhancing class " + className);

      listenerType.init();

      genClass.addInterfaceName("com.caucho.amber.entity.Listener");

      ListenerComponent listener = new ListenerComponent();

      listener.setListenerType(listenerType);
      listener.setBaseClassName(baseClass.getName());
      listener.setExtClassName(extClassName);

      genClass.addComponent(listener);
    }

    EmbeddableType embeddableType = _amberContainer.getEmbeddable(className);

    // Type can be null for subclasses and inner classes that need fixups
    if (embeddableType != null) {
      if (log.isLoggable(Level.INFO))
        log.log(Level.INFO, "Amber enhancing class " + className);

      embeddableType.init();

      genClass.addInterfaceName("com.caucho.amber.entity.Embeddable");

      EmbeddableComponent embeddable = new EmbeddableComponent();

      embeddable.setEmbeddableType(embeddableType);
      embeddable.setBaseClassName(baseClass.getName());
      embeddable.setExtClassName(extClassName);

      genClass.addComponent(embeddable);
    }
  }

  /**
   * Generates the type.
   */
  public void generate(AbstractEnhancedType type)
    throws Exception
  {
    String className = type.getBeanClass().getName();

    if (! isModified(className))
      return;
    
    JavaClassGenerator javaGen = new JavaClassGenerator();

    javaGen.setWorkDir(getWorkDir());

    String extClassName = type.getBeanClass().getName() + "__ResinExt";
    type.setInstanceClassName(extClassName);
    type.setEnhanced(true);

    _pendingClassNames.add(type.getInstanceClassName());

    generateJava(javaGen, type);
  }

  /**
   * Generates the type.
   */
  public void generateJava(JavaClassGenerator javaGen,
                           AbstractEnhancedType type)
    throws Exception
  {
    if (type.isGenerated())
      return;

    type.setGenerated(true);

    _prepare.renameClass(type.getBeanClass().getName(),
                         type.getBeanClass().getName());

    GenClass javaClass = new GenClass(type.getInstanceClassName());

    javaClass.setSuperClassName(type.getBeanClass().getName());

    javaClass.addImport("java.util.logging.*");
    javaClass.addImport("com.caucho.amber.manager.*");
    javaClass.addImport("com.caucho.amber.entity.*");
    javaClass.addImport("com.caucho.amber.type.*");

    ClassComponent componentGenerator
      = type.getComponentGenerator();

    if (componentGenerator instanceof AmberMappedComponent) {
      AmberMappedComponent entityGenerator
        = (AmberMappedComponent) componentGenerator;
      
      // type is EntityType or MappedSuperclassType

      javaClass.addInterfaceName(type.getComponentInterfaceName());

      type.setEnhanced(true);

      entityGenerator.setRelatedType((EntityType) type);
      entityGenerator.setBaseClassName(type.getBeanClass().getName());

      //String extClassName = gen.getBaseClassName() + "__ResinExt";
      // type.setInstanceClassName(extClassName);

      entityGenerator.setExtClassName(type.getInstanceClassName());

      javaClass.addComponent(componentGenerator);
    }
    else if (type instanceof ListenerType) {
      javaClass.addInterfaceName("com.caucho.amber.entity.Listener");

      type.setEnhanced(true);

      ListenerComponent listener = new ListenerComponent();

      listener.setListenerType((ListenerType) type);
      listener.setBaseClassName(type.getBeanClass().getName());

      listener.setExtClassName(type.getInstanceClassName());

      javaClass.addComponent(listener);
    }
    else if (componentGenerator instanceof EmbeddableComponent) {
      EmbeddableComponent embeddable
        = (EmbeddableComponent) componentGenerator;
      
      javaClass.addInterfaceName("com.caucho.amber.entity.Embeddable");

      type.setEnhanced(true);

      embeddable.setEmbeddableType((EmbeddableType) type);
      embeddable.setBaseClassName(type.getBeanClass().getName());

      embeddable.setExtClassName(type.getInstanceClassName());

      javaClass.addComponent(embeddable);
    }

    javaGen.generate(javaClass);

    // _pendingClassNames.add(extClassName);
  }

  private boolean isModified(String className)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      ClassLoader loader = _amberContainer.getParentClassLoader();
      ClassLoader tempLoader
        = ((DynamicClassLoader) loader).getNewTempClassLoader();
      DynamicClassLoader workLoader
        = SimpleLoader.create(tempLoader, getPostWorkDir());
      workLoader.setServletHack(true);

      Class cl = Class.forName(className.replace('/', '.'),
                               false,
                               workLoader);

      thread.setContextClassLoader(tempLoader);

      Method init = cl.getMethod("_caucho_init", new Class[] { Path.class });
      Method modified = cl.getMethod("_caucho_is_modified", new Class[0]);

      init.invoke(null, Vfs.lookup());

      return (Boolean) modified.invoke(null);
    } catch (ClassNotFoundException e) {
      log.log(Level.FINEST, e.toString(), e);
    } catch (NoSuchMethodException e) {
      log.log(Level.FINEST, e.toString(), e);
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    return true;
  }

  /**
   * Compiles the pending classes.
   */
  public void compile()
    throws Exception
  {
    if (_pendingClassNames.size() == 0)
      return;

    ArrayList<String> classNames = new ArrayList<String>(_pendingClassNames);
    _pendingClassNames.clear();

    String []javaFiles = new String[classNames.size()];

    for (int i = 0; i < classNames.size(); i++) {
      String className = classNames.get(i);

      String javaName = className.replace('.', '/') + ".java";

      javaFiles[i] = javaName;
    }

    EntityGenerator gen = new EntityGenerator();
    gen.setSearchPath(_configDirectory);
    // XXX:
    // gen.setClassDir(getPath());

    JavaCompiler compiler = gen.getCompiler();

    compiler.setClassDir(getWorkDir());
    compiler.compileBatch(javaFiles);

    for (int i = 0; i < classNames.size(); i++) {
      String extClassName = classNames.get(i);
      int tail = extClassName.length() - "__ResinExt".length();

      String baseClassName = extClassName.substring(0, tail);

      // fixup(baseClassName, extClassName);
    }
  }

  /**
   * Enhances the class.
   */
  public void postEnhance(JavaClass baseClass)
    throws Exception
  {
    String className = baseClass.getThisClass();

    ArrayList<FieldMap> fieldMaps = new ArrayList<FieldMap>();

    Class thisClass = _amberContainer.loadTempClass(className.replace('/', '.'));

    if (thisClass == null)
      return;

    // Cache entity JClass for next fixup.
    Class entityClass = thisClass;

    // Field-based fixup.
    do {
      BeanType type;

      type = _amberContainer.getEntity(thisClass.getName());

      if (type == null)
        type = _amberContainer.getMappedSuperclass(thisClass.getName());

      if (type == null)
        type = _amberContainer.getEmbeddable(thisClass.getName());

      if (type == null || ! type.isFieldAccess())
        continue;
      
      if (type instanceof EmbeddableType)
        continue;

      if (type instanceof EntityType) {
        EntityType entityType = (EntityType) type;

        /*
        for (AmberField field : entityType.getId().getKeys()) {
          fieldMaps.add(new FieldMap(baseClass, field.getName()));
        }
        */
      }

      for (AmberField field : type.getFields()) {
        fieldMaps.add(new FieldMap(baseClass, field.getName()));
      }
    } while ((thisClass = thisClass.getSuperclass()) != null);

    if (fieldMaps.size() > 0) {
      FieldFixupAnalyzer analyzer = new FieldFixupAnalyzer(fieldMaps);

      for (JavaMethod javaMethod : baseClass.getMethodList()) {
        if (javaMethod.getName().startsWith("__caucho_get_"))
          continue;
        else if (javaMethod.getName().startsWith("__caucho_set_"))
          continue;
        else if (javaMethod.getName().startsWith("__caucho_super_"))
          continue;

        CodeVisitor visitor = new CodeVisitor(baseClass, javaMethod.getCode());

        visitor.analyze(analyzer, true);
      }
    }
  }

  /**
   * Parses the configuration file.
   */
  public void configure(AbstractEnhancedType type)
    throws ConfigException, IOException
  {
  }

  static class FieldMap {
    private int _fieldRef = -1;
    private int _getterRef;
    private int _setterRef;

    FieldMap(com.caucho.bytecode.JavaClass baseClass,
             String fieldName)
    {
      ConstantPool pool = baseClass.getConstantPool();

      FieldRefConstant fieldRef = pool.getFieldRef(fieldName);

      if (fieldRef == null)
        return;

      _fieldRef = fieldRef.getIndex();

      MethodRefConstant methodRef;

      String getterName = "__caucho_get_" + fieldName;

      methodRef = pool.addMethodRef(baseClass.getThisClass(),
                                    getterName,
                                    "()" + fieldRef.getType());

      _getterRef = methodRef.getIndex();

      String setterName = "__caucho_set_" + fieldName;

      methodRef = pool.addMethodRef(baseClass.getThisClass(),
                                    setterName,
                                    "(" + fieldRef.getType() + ")V");

      _setterRef = methodRef.getIndex();
    }

    int getFieldRef()
    {
      return _fieldRef;
    }

    int getGetterRef()
    {
      return _getterRef;
    }

    int getSetterRef()
    {
      return _setterRef;
    }
  }

  static class FieldFixupAnalyzer extends Analyzer {
    private ArrayList<FieldMap> _fieldMap;

    FieldFixupAnalyzer(ArrayList<FieldMap> fieldMap)
    {
      _fieldMap = fieldMap;
    }

    int getGetter(int fieldRef)
    {
      for (int i = _fieldMap.size() - 1; i >= 0; i--) {
        FieldMap fieldMap = _fieldMap.get(i);

        if (fieldMap.getFieldRef() == fieldRef)
          return fieldMap.getGetterRef();
      }

      return -1;
    }

    public void analyze(CodeVisitor visitor)
    {
      switch (visitor.getOpcode()) {
      case CodeVisitor.GETFIELD:
        int getter = getGetter(visitor.getShortArg());

        if (getter > 0) {
          visitor.setByteArg(0, CodeVisitor.INVOKEVIRTUAL);
          visitor.setShortArg(1, getter);
        }
        break;
      case CodeVisitor.PUTFIELD:
        int setter = getSetter(visitor.getShortArg());

        if (setter > 0) {
          visitor.setByteArg(0, CodeVisitor.INVOKEVIRTUAL);
          visitor.setShortArg(1, setter);
        }
        break;
      }
    }

    int getSetter(int fieldRef)
    {
      for (int i = _fieldMap.size() - 1; i >= 0; i--) {
        FieldMap fieldMap = _fieldMap.get(i);

        if (fieldMap.getFieldRef() == fieldRef)
          return fieldMap.getSetterRef();
      }

      return -1;
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _configDirectory + "]";
  }
}
