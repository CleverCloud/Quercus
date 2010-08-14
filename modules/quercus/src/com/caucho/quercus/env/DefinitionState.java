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

package com.caucho.quercus.env;

import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.QuercusException;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.program.ClassDef;
import com.caucho.quercus.program.UnsetFunction;
import com.caucho.util.Crc64;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Represents the state of the definitions: functions, classes and
 * constants.
 */
public final class DefinitionState {
  private static final L10N L = new L10N(DefinitionState.class);
  private static final Logger log
    = Logger.getLogger(DefinitionState.class.getName());
  
  private static final
    LruCache<ClassKey,SoftReference<QuercusClass>> _classCache
    = new LruCache<ClassKey,SoftReference<QuercusClass>>(4096);

  private final QuercusContext _quercus;

  private boolean _isStrict;

  private HashMap<String, AbstractFunction> _funMap;

  private HashMap<String, AbstractFunction> _lowerFunMap;

  private HashMap<String, ClassDef> _classDefMap;

  private HashMap<String, ClassDef> _lowerClassDefMap;

  private boolean _isLazy;

  // crc of the entries
  private long _crc;

  public DefinitionState(QuercusContext quercus)
  {
    _quercus = quercus;

    _isStrict = quercus.isStrict();

    _funMap = new HashMap<String, AbstractFunction>(256, 0.25F);
    _classDefMap = new HashMap<String, ClassDef>(256, 0.25F);

    if (! _isStrict) {
      _lowerFunMap = new HashMap<String, AbstractFunction>(256, 0.25F);

      _lowerClassDefMap = new HashMap<String, ClassDef>(256, 0.25F);
    }
  }

  private DefinitionState(DefinitionState oldState)
  {
    this(oldState._quercus);

    _funMap.putAll(oldState._funMap);

    if (_lowerFunMap != null)
      _lowerFunMap.putAll(oldState._lowerFunMap);

    _classDefMap.putAll(oldState._classDefMap);

    if (_lowerClassDefMap != null)
      _lowerClassDefMap.putAll(oldState._lowerClassDefMap);

    _crc = oldState._crc;
  }

  private DefinitionState(DefinitionState oldState, boolean isLazy)
  {
    _isLazy = true;
    
    _quercus = oldState._quercus;
    _isStrict = oldState._isStrict;

    _funMap = oldState._funMap;
    _lowerFunMap = oldState._lowerFunMap;

    _classDefMap = oldState._classDefMap;
    _lowerClassDefMap = oldState._lowerClassDefMap;
    
    _crc = oldState._crc;
  }

  /**
   * Returns true for strict mode.
   */
  public final boolean isStrict()
  {
    return _isStrict;
  }

  /**
   * Returns the owning PHP engine.
   */
  public QuercusContext getQuercus()
  {
    return _quercus;
  }

  /**
   * returns the crc.
   */
  public long getCrc()
  {
    return _crc;
  }

  /**
   * Returns an array of the defined functions.
   */
  public ArrayValue getDefinedFunctions()
  {
    ArrayValue result = new ArrayValueImpl();

    ArrayValue internal = _quercus.getDefinedFunctions();
    ArrayValue user = new ArrayValueImpl();

    // XXX: i18n
    result.put(new StringBuilderValue("internal"), internal);
    result.put(new StringBuilderValue("user"), user);

    for (String name : _funMap.keySet()) {
      StringValue key = new StringBuilderValue(name);

      if (! internal.contains(key).isset())
        user.put(name);
    }

    return result;
  }

  /**
   * Finds the java reflection method for the function with the given name.
   *
   * @param name the method name
   * @return the found method or null if no method found.
   */
  public AbstractFunction findFunction(String name)
  {
    AbstractFunction fun = _funMap.get(name);

    if (fun == null) {
    }
    else if (fun instanceof UnsetFunction) {
      UnsetFunction unsetFun = (UnsetFunction) fun;

      if (_crc == unsetFun.getCrc())
        return null;
    }
    else {
      return fun;
    }
    
    if (_lowerFunMap != null) {
      fun = _lowerFunMap.get(name.toLowerCase());

      if (fun != null) {
        // copyOnWrite();
        _funMap.put(name, fun);

        return fun;
      }
    }

    fun = findModuleFunction(name);

    if (fun != null) {
      // copyOnWrite();
      _funMap.put(name, fun);

      return fun;
    }
    else {
      // copyOnWrite();
      _funMap.put(name, new UnsetFunction(_crc));
      
      return null;
    }
  }

  /**
   * Finds the java reflection method for the function with the given name.
   *
   * @param name the method name
   * @return the found method or null if no method found.
   */
  private AbstractFunction findModuleFunction(String name)
  {
    AbstractFunction fun = null;

    fun = _quercus.findFunction(name);
    if (fun != null)
      return fun;

    return fun;
  }

  /**
   * Adds a function, e.g. from an include.
   */
  public Value addFunction(String name, AbstractFunction fun)
  {
    AbstractFunction oldFun = findFunction(name);

    if (oldFun != null) {
      throw new QuercusException(L.l("can't redefine function {0}", name));
    }

    copyOnWrite();
    _funMap.put(name, fun);
    _crc = Crc64.generate(_crc, name);

    if (_lowerFunMap != null)
      _lowerFunMap.put(name.toLowerCase(), fun);

    return BooleanValue.TRUE;
  }

  /**
   * Adds a function from a compiled include
   *
   * @param name the function name, must be an intern() string
   * @param lowerName the function name, must be an intern() string
   */
  public Value addFunction(String name, String lowerName, AbstractFunction fun)
  {
    // XXX: skip the old function check since the include for compiled
    // pages is already verified.  Might have a switch here?
    /*
    AbstractFunction oldFun = _lowerFunMap.get(lowerName);

    if (oldFun == null)
      oldFun = _quercus.findLowerFunctionImpl(lowerName);

    if (oldFun != null) {
      throw new QuercusException(L.l("can't redefine function {0}", name));
    }
    */

    copyOnWrite();
    _funMap.put(name, fun);
    _crc = Crc64.generate(_crc, name);

    if (_lowerFunMap != null)
      _lowerFunMap.put(lowerName, fun);

    return BooleanValue.TRUE;
  }

  /**
   * Adds a class, e.g. from an include.
   */
  public void addClassDef(String name, ClassDef cl)
  {
    copyOnWrite();
    _classDefMap.put(name, cl);
    _crc = Crc64.generate(_crc, name);

    if (_lowerClassDefMap != null)
      _lowerClassDefMap.put(name.toLowerCase(), cl);
  }

  /**
   * Adds a class, e.g. from an include.
   */
  public ClassDef findClassDef(String name)
  {
    ClassDef def = _classDefMap.get(name);

    if (def != null)
      return def;

    if (_lowerClassDefMap != null)
      def = _lowerClassDefMap.get(name.toLowerCase());

    return def;
  }

  /**
   * Returns the declared classes.
   *
   * @return an array of the declared classes()
   */
  public Value getDeclaredClasses(Env env)
  {
    ArrayList<String> names = new ArrayList<String>();

    /*
    for (String name : _classMap.keySet()) {
      if (! names.contains(name))
        names.add(name);
    }
    */

    for (String name : _classDefMap.keySet()) {
      if (! names.contains(name))
        names.add(name);
    }

    for (String name : _quercus.getClassMap().keySet()) {
      if (! names.contains(name))
        names.add(name);
    }

    Collections.sort(names);

    ArrayValue array = new ArrayValueImpl();

    for (String name : names) {
      array.put(env.createString(name));
    }

    return array;
  }

  public DefinitionState copy()
  {
    return new DefinitionState(this);
  }

  public DefinitionState copyLazy()
  {
    return new DefinitionState(this, true);
  }

  private void copyOnWrite()
  {
    if (! _isLazy)
      return;
    
    _isLazy = false;

    _funMap = new HashMap<String, AbstractFunction>(_funMap);

    if (_lowerFunMap != null) {
      _lowerFunMap = new HashMap<String, AbstractFunction>(_lowerFunMap);
    }

    _classDefMap = new HashMap<String, ClassDef>(_classDefMap);

    if (_lowerClassDefMap != null) {
      _lowerClassDefMap = new HashMap<String, ClassDef>(_lowerClassDefMap);
    }
  }

  static class ClassKey {
    private final WeakReference<ClassDef> _defRef;
    private final WeakReference<QuercusClass> _parentRef;

    ClassKey(ClassDef def, QuercusClass parent)
    {
      _defRef = new WeakReference<ClassDef>(def);

      if (parent != null)
        _parentRef = new WeakReference<QuercusClass>(parent);
      else
        _parentRef = null;
    }

    public int hashCode()
    {
      int hash = 37;

      ClassDef def = _defRef.get();
      
      QuercusClass parent = null;
      if (_parentRef != null)
        parent = _parentRef.get();

      if (def != null)
        hash = 65521 * hash + def.hashCode();

      if (parent != null)
        hash = 65521 * hash + parent.hashCode();

      return hash;
    }

    public boolean equals(Object o)
    {
      ClassKey key = (ClassKey) o;

      ClassDef aDef = _defRef.get();
      ClassDef bDef = key._defRef.get();

      if (aDef != bDef)
        return false;

      if (_parentRef == key._parentRef)
        return true;
      
      else if (_parentRef != null && key._parentRef != null)
        return _parentRef.get() == key._parentRef.get();

      else
        return false;
    }
  }
}

