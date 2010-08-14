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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.es;

import com.caucho.es.parser.Parser;
import com.caucho.es.wrapper.Wrapper;
import com.caucho.java.LineMap;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.SimpleLoader;
import com.caucho.util.FreeList;
import com.caucho.util.IntMap;
import com.caucho.util.LruCache;
import com.caucho.vfs.Path;

import java.lang.ref.SoftReference;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Implementation class for the global prototype
 */
public class Global extends ESBase {
  private static Integer LOCK = new Integer(0);
  private static final Logger log = Logger.getLogger(Global.class.getName());

  private final static int OBJECT = 0;
  private final static int FUNCTION = OBJECT + 1;
  private final static int ARRAY = FUNCTION + 1;
  private final static int STRING = ARRAY + 1;
  private final static int BOOL = STRING + 1;
  private final static int NUM = BOOL + 1;
  private final static int DATE = NUM + 1;
  private final static int MATH = DATE + 1;
  private final static int REGEXP = MATH + 1;
  private final static int PACKAGES = REGEXP + 1;
  private final static int CAUCHO = PACKAGES + 1;
  private final static int JAVA = CAUCHO + 1;
  private final static int JAVAX = JAVA + 1;

  private static Global goldGlobal;
  private static IntMap propertyMap;
  
  private ESGlobal global;
  private Global root;

  ESObject objProto;
  ESObject object;

  ESObject funProto;
  ESObject fun;

  ESObject arrayProto;
  ESObject array;

  ESObject stringProto;
  ESObject string;

  ESObject boolProto;
  ESObject bool;

  ESObject numProto;
  ESObject num;

  ESObject dateProto;
  ESObject date;

  ESObject math;

  ESRegexp regexpProto;
  ESRegexpWrapper regExp;

  ESPackage pkg;

  HashMap properties;
  HashMap globalProperties;

  // static lookup of current Global

  private static final ThreadLocal<Global> _globals
    = new ThreadLocal<Global>();
  
  private static Thread lastThread;
  private static Global lastGlobal;

  // bean wrapping

  private final static LruCache<Class,ESBase> _staticWraps
    = new LruCache<Class,ESBase>(256);
  private final static LruCache<Class,ESBase> _staticClassWraps
    = new LruCache<Class,ESBase>(256);

  // cache

  private static FreeList<Call> _freeCalls = new FreeList<Call>(2);
  
  private static HashMap<String,SoftReference<Script>> _runtimeScripts;
  /* = new HashMap<String,SoftReference<Script>>(); */

  private ClassLoader parentLoader;
  private ClassLoader loader;
  private Path scriptPath;
  private Path classDir;
  private HashMap importScripts;
  private HashMap importGlobals;
  
  int markCount;

  /**
   * Null constructor
   */
  private Global(boolean init)
  {
    ESBase.init(null);

    /*
    if (_globals == null) {
      _globals = new ThreadLocal();
      _staticWraps = new LruCache<Class,ESBase>(256);
      _staticClassWraps = new LruCache<Class,ESBase>(256);
      ESId.intern("foo"); // XXX: bogus to fix stupid kaffe static init.
    }
    */

    propertyMap = new IntMap();
    propertyMap.put(ESId.intern("Object"), OBJECT);
    propertyMap.put(ESId.intern("Function"), FUNCTION);
    propertyMap.put(ESId.intern("Array"), ARRAY);
    propertyMap.put(ESId.intern("String"), STRING);
    propertyMap.put(ESId.intern("Boolean"), BOOL);
    propertyMap.put(ESId.intern("Number"), NUM);
    propertyMap.put(ESId.intern("Date"), DATE);
    propertyMap.put(ESId.intern("Math"), MATH);
    propertyMap.put(ESId.intern("RegExp"), REGEXP);
    propertyMap.put(ESId.intern("Packages"), PACKAGES);
    propertyMap.put(ESId.intern("caucho"), CAUCHO);
    propertyMap.put(ESId.intern("java"), JAVA);
    propertyMap.put(ESId.intern("javax"), JAVAX);

    globalProperties = new HashMap();

    object = NativeObject.create(this);
    fun = NativeFunction.create(this);
    object.prototype = funProto;

    int flags = ESBase.DONT_ENUM;
    int allflags = (ESBase.DONT_ENUM|ESBase.DONT_DELETE|ESBase.READ_ONLY);

    array = NativeArray.create(this);
    string = NativeString.create(this);
    bool = NativeBoolean.create(this);
    num = NativeNumber.create(this);
    math = NativeMath.create(this);
    date = NativeDate.create(this);

    regExp = NativeRegexp.create(this);

    pkg = ESPackage.create();

    NativeGlobal.create(this);
    NativeFile.create(this);

    globalProperties.put(ESId.intern("NaN"), ESNumber.create(0.0/0.0));
    globalProperties.put(ESId.intern("Infinity"), ESNumber.create(1.0/0.0));
  }

  /**
   * Creates a new global object for a script thread.
   *
   * @param properties any global properties for the script
   * @param proto a Java prototype object underlying the global object
   * @param classDir work directory where generated classes will go
   * @param scriptPath a path for searching scripts
   * @param parentLoader the parent class loader.
   */
  Global(HashMap properties, Object proto,
         Path classDir, Path scriptPath, ClassLoader parentLoader)
    throws Throwable
  {
    synchronized (LOCK) {
      if (goldGlobal == null)
        goldGlobal = new Global(true);
    }

    root = this;
    this.parentLoader = parentLoader;
    this.loader = SimpleLoader.create(parentLoader, classDir, null);
    this.classDir = classDir;
    this.scriptPath = scriptPath;

    // Object
    objProto = (ESObject) goldGlobal.objProto.resinCopy();
    object = (ESObject) goldGlobal.object.resinCopy();

    // Function

    funProto = (ESObject) goldGlobal.funProto.resinCopy();
    funProto.prototype = objProto;
    object.prototype = funProto;

    fun = (ESObject) goldGlobal.fun.resinCopy();
    fun.prototype = funProto;

    // Array

    arrayProto = (ESObject) goldGlobal.arrayProto.resinCopy();
    arrayProto.prototype = objProto;

    array = (ESObject) goldGlobal.array.resinCopy();
    array.prototype = funProto;

    // String

    stringProto = (ESObject) goldGlobal.stringProto.resinCopy();
    stringProto.prototype = objProto;
      
    string = (ESObject) goldGlobal.string.resinCopy();
    string.prototype = funProto;

    // Boolean

    boolProto = (ESObject) goldGlobal.boolProto.resinCopy();
    boolProto.prototype = objProto;
      
    bool = (ESObject) goldGlobal.bool.resinCopy();
    bool.prototype = funProto;

    // Number

    numProto = (ESObject) goldGlobal.numProto.resinCopy();
    numProto.prototype = objProto;
      
    num = (ESObject) goldGlobal.num.resinCopy();
    num.prototype = funProto;

    // Math

    math = (ESObject) goldGlobal.math.resinCopy();
    math.prototype = objProto;

    // Date

    dateProto = (ESObject) goldGlobal.dateProto.resinCopy();
    dateProto.prototype = objProto;
      
    date = (ESObject) goldGlobal.date.resinCopy();
    date.prototype = funProto;

    // RegExp

    //regexpProto = (ESRegexp) goldGlobal.regexpProto.resinCopy();
    //regexpProto.prototype = objProto;

    //regExp = (ESRegexpWrapper) goldGlobal.regExp.resinCopy();
    //regExp.prototype = funProto;
    //regExp.regexp = regexpProto;

    pkg = ESPackage.create();

    if (proto != null) {
      prototype = objectWrap(proto);
      prototype.prototype = objProto;
    }
    else
      prototype = objProto;

    if (properties != null)
      this.properties = properties;

    globalProperties = goldGlobal.globalProperties;
  }

  Global(Global root)
  {
    this.root = root;

    objProto = root.objProto;
    object = root.object;
    funProto = root.funProto;
    fun = root.fun;
    arrayProto = root.arrayProto;
    array = root.array;
    stringProto = root.stringProto;
    string = root.string;
    boolProto = root.boolProto;
    bool = root.bool;
    numProto = root.numProto;
    num = root.num;
    math = root.math;
    dateProto = root.dateProto;
    date = root.date;
    regexpProto = root.regexpProto;
    regExp = root.regExp;
    pkg = root.pkg;
    properties = root.properties;
    prototype = root.prototype;
    globalProperties = root.globalProperties;
    
    _runtimeScripts = root._runtimeScripts;
  }

  void addProperty(ESId id, ESBase value)
  {
    globalProperties.put(id, value);
  }

  public ESBase getProperty(ESString id)
    throws Throwable
  {
    int index = propertyMap.get(id);

    switch (index) {
    case OBJECT:
      return snap(id, object);

    case FUNCTION:
      return snap(id, fun);

    case ARRAY:
      return snap(id, array);

    case STRING:
      return snap(id, string);

    case BOOL:
      return snap(id, bool);

    case NUM:
      return snap(id, num);

    case DATE:
      return snap(id, date);

    case MATH:
      return snap(id, math);

    case REGEXP:
      return snap(id, getRegexp());

    case PACKAGES:
      return snap(id, pkg);

    case CAUCHO:
      return snap(id, pkg.getProperty("com").getProperty("caucho"));

    case JAVA:
      return snap(id, pkg.getProperty("java"));

    case JAVAX:
      return snap(id, pkg.getProperty("javax"));

    default:
      ESBase value = prototype == null ? null : prototype.getProperty(id);
      Object obj;
      if (value != null && value != esEmpty)
        return snap(id, value);
      else if (properties != null &&
               (obj = properties.get(id.toString())) != null)
        return snap(id, objectWrap(obj));
      else if ((value = (ESBase) globalProperties.get(id)) != null)
        return snap(id, value);
      else {
        return esEmpty;
      }
    }
  }

  private ESBase snap(ESString id, ESBase value)
  {
    if (value == null)
      throw new RuntimeException();

    global.put(id, value, DONT_ENUM);
    return value;
  }

  ESRegexpWrapper getRegexp() 
  { 
    if (regExp != null)
      return regExp; 

    else if (root.regExp != null) {
      regExp = root.regExp;
      return regExp;
    }

    initRegexp();

    return regExp;
  }

  ESRegexp getRegexpProto() 
  { 
    if (regexpProto != null)
      return regexpProto; 

    else if (root.regexpProto != null) {
      regexpProto = root.regexpProto;
      return regexpProto;
    }

    initRegexp();

    return regexpProto;
  }

  private void initRegexp()
  {

    root.regexpProto = (ESRegexp) goldGlobal.regexpProto.resinCopy();
    root.regexpProto.prototype = root.objProto;

    root.regExp = (ESRegexpWrapper) goldGlobal.regExp.resinCopy();
    root.regExp.prototype = root.funProto;
    root.regExp.regexp = root.regexpProto;

    regexpProto = root.regexpProto;
    regExp = root.regExp;
  }

  /**
   * Sets a running script.
   *
   * @param name classname of the script.
   * @param script the script itself.
   */
  public void addScript(String name, Script script)
  {
    if (_runtimeScripts != null)
      _runtimeScripts.put(name, new SoftReference<Script>(script));
  }

  /**
   * Returns the line map for the named class to translate Java line
   * numbers to javascript line numbers.
   *
   * @param className class throwing the error.
   * @return the line map.
   */
  LineMap getLineMap(String className)
  {
    try {
      int p = className.indexOf('$');
      
      if (p > 0)
        className = className.substring(0, p);
    
      Script script = null;
      
      if (_runtimeScripts != null) {
        SoftReference<Script> ref = _runtimeScripts.get(className);

        if (ref != null)
          script = ref.get();
      }

      if (script != null)
        return script.getLineMap();
      else
        return null;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Returns the named script.  If the script has already been loaded,
   * return the old script.
   */
  Script findScript(String className)
    throws Throwable
  {
    Script script = (Script) importScripts.get(className);

    if (script != null)
      return script;
    
    Parser parser = new Parser();
    parser.setScriptPath(getScriptPath());
    parser.setClassLoader(getClassLoader());
    parser.setWorkDir(getClassDir());

    return parser.parse(className);
  }

  /**
   * Returns the global prototype for the current thread.
   */
  public static Global getGlobalProto() 
  {
    return (Global) _globals.get();
  }

  /**
   * Starts execution of a JavaScript thread.
   *
   * @return the old global context for the thread.
   */
  Global begin()
  {
    Global oldGlobal = (Global) _globals.get();
    _globals.set(this);

    return oldGlobal;
  }

  /**
   * Completes execution of a JavaScript thread, restoring the global context.
   *
   * @param oldGlobal the old global context for the thread.
   */
  static void end(Global oldGlobal)
  {
    _globals.set(oldGlobal);
  }
  

  Call getCall()
  {
    Call call = _freeCalls.allocate();
    if (call == null)
      return new Call();
    else {
      call.clear();
      return call;
    }
  }

  void freeCall(Call call)
  {
    call.free();
    _freeCalls.free(call);
  }

  ESBase objectWrap(Object object)
    throws Throwable
  {
    if (object == null)
      return ESBase.esNull;

    Class cl = object.getClass();
    String clName = cl.getName();

    if (object instanceof ESBase)
      return (ESBase) object;
    if (clName.equals("java.lang.String"))
      return new ESString(object.toString());
    if (clName.equals("java.lang.Double"))
      return ESNumber.create(((Double) object).doubleValue());
    if (clName.equals("java.util.Date"))
      return convertDate(object);

    ESBase wrapper;
    synchronized (_staticWraps) {
      wrapper = _staticWraps.get(cl);
    }
    
    if (wrapper == null || ((DynamicClassLoader) wrapper.getClass().getClassLoader()).isDestroyed()) {
      ESBase []values = Wrapper.bean(this, cl);

      if (values == null)
        return ESBase.esNull;
      
      ESBase clWrapper = values[0];
      wrapper = values[1];
      if (wrapper.getClass().getClassLoader().getParent().equals(getClass().getClassLoader())) {
        synchronized (_staticWraps) {
          _staticClassWraps.put(cl, clWrapper);
          _staticWraps.put(cl, wrapper);
        }
      }
    }

    if (wrapper instanceof ESJavaWrapper)
      return ((ESJavaWrapper) wrapper).wrap(object);
    else {
      return ((ESBeanWrapper) wrapper).wrap(object);
    }
  }

  private ESBase convertDate(Object object)
  {
    return ESDate.create(((Date) object).getTime());
  }

  // XXX: backwards -- s/b wrap and staticWrap
  public static ESBase wrap(Object object)
    throws Throwable
  {
    return getGlobalProto().objectWrap(object);
  }

  ESBase classWrap(Class cl)
    throws Throwable
  {
    if (cl == null)
      throw new RuntimeException();

    ESBase clWrapper;

    synchronized (_staticWraps) {
      clWrapper = _staticClassWraps.get(cl);
    }
    
    if (clWrapper == null || ((DynamicClassLoader) clWrapper.getClass().getClassLoader()).isDestroyed()) {
      ESBase []values = Wrapper.bean(this, cl);
      clWrapper = values[0];
      ESBase wrapper = values[1];
      
      synchronized (_staticWraps) {
        _staticWraps.put(cl, wrapper);
        _staticClassWraps.put(cl, clWrapper);
      }
    }

    return clWrapper;
  }

  public ClassLoader getClassLoader()
  {
    return loader != null ? loader : root.loader;
  }

  public ClassLoader getParentLoader()
  {
    return parentLoader != null ? parentLoader : root.parentLoader;
  }

  public Path getClassDir()
  {
    return classDir != null ? classDir : root.classDir;
  }

  public Path getScriptPath()
  {
    return scriptPath != null ? scriptPath : root.scriptPath;
  }

  public void importScript(ESObject global, String name)
    throws Throwable
  {
    if (importScripts == null) {
      importScripts = new HashMap();
      importGlobals = new HashMap();
    }

    ESGlobal scriptGlobal = (ESGlobal) importGlobals.get(name);
    
    if (scriptGlobal == null) {
      if (importScripts.get(name) != null)
        return;
      
      Parser parser = new Parser();
      parser.setScriptPath(getScriptPath());
      parser.setClassLoader(getClassLoader());
      parser.setWorkDir(getClassDir());

      Script script = parser.parse(name);

      importScripts.put(name, script);
      
      scriptGlobal = script.initClass(this);
      
      importGlobals.put(name, scriptGlobal);
      
      scriptGlobal.execute();
    }

    scriptGlobal.export(global);
  }

  ESGlobal getGlobal()
  {
    return global;
  }

  public void setGlobal(ESGlobal global)
  {
    this.global = global;
  }

  public Object toJavaObject()
    throws ESException
  {
    Object o = prototype.toJavaObject();
    return (o == null) ? this : o;
  }

  /*
   * Somewhat bogus for creating based on globals.
   * "this" is the global
   */

  public ESObject createObject()
  {
    return new ESObject("Object", objProto);
  }
  
  /**
   * Somewhat bogus for creating based on globals.
   * "this" is the global
   */
  ESArray createArray()
  {
    ESArray array = new ESArray();

    array.prototype = arrayProto;

    return array;
  }
  
   void clearMark()
   {
     markCount = 0;
   }
  
   int addMark()
   {
     return ++markCount;
   }
}
