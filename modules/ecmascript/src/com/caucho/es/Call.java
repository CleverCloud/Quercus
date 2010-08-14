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

import java.util.Enumeration;
import java.util.Iterator;

/**
 * Implementation class representing a call context.
 */
public final class Call extends ESBase {
  static ESId ARGUMENTS = ESId.intern("arguments");
  static ESId ARRAY = ESId.intern("Array");

  Call caller;
  ESObject callThis;
  int callLength;
  ESBase callee;

  public ESBase []stack;
  int top;

  public ESGlobal global;
  ESBase []scope;
  int scopeLength;

  public ESBase []values;

  ESObject aux;

  Call child; // just a cache

  /**
   * Create a new context object
   */
  Call()
  {
    prototype = esBase;

    stack = new ESBase[64];
    scope = new ESBase[16];
    values = new ESBase[64];
    top = 0;
  }

  void clear()
  {
    aux = null;
    child = null;
    top = 0;
  }

  public Call getCall()
  {
    Call child = this.child;

    if (child == null)
      child = this.child = new Call();

    child.caller = this;

    child.global = global;

    return child;
  }

  Global getGlobalProto() { return Global.getGlobalProto(); }

  public ESBase wrap(Object o) throws Throwable { return Global.wrap(o); }
  
  public ESBase wrap(long n) throws Throwable
  {
    return ESNumber.create(n);
  }
  
  public ESBase wrapClass(Class cl) throws Throwable
  {
    return Global.getGlobalProto().classWrap(cl);
  }

  // public ESGlobal getGlobal() { return Global.getGlobal().global; }

  final ESBase getArg(int i)
  {
    return stack[top + i];
  }

  public final ESBase getArg(int i, int len) 
  { 
    return i < len ? stack[top + i] : esUndefined; 
  }

  public final int getArgInt32(int i, int len) throws Throwable
  {
    return i < len ? stack[top + i].toInt32() : 0;
  }

  public final double getArgNum(int i, int len) throws Throwable
  {
    return i < len ? stack[top + i].toNum() : (0.0/0.0);
  }

  public final String getArgString(int i, int len) throws Throwable
  {
    return i < len ? stack[top + i].toJavaString() : null;
  }

  public final Object getArgObject(int i, int len) throws Throwable
  {
    return i < len ? stack[top + i].toJavaObject() : null;
  }

  public ESObject createObject()
  {
    return new ESObject("Object", getGlobalProto().objProto);
  }

  public ESBase createDate(long time)
  {
    return new ESDate(time, getGlobalProto().dateProto);
  }

  public String printf(int length)
    throws Throwable
  {
    return Printf.sprintf(this, length);
  }

  void setArg(int i, ESBase obj)
  {
    stack[top + i] = obj;
  }

  public final ESObject getThis() throws Throwable
  {
    return stack[top - 1].toObject();
  }
  
  public final Object getThisWrapper() throws Throwable
  { 
    return stack[top - 1].toJavaObject();
  }
  
  void setThis(ESBase obj)
  {
    stack[top - 1] = obj;
  }

  public ESGlobal getGlobal()
  {
    return global;
  }

  public final ESObject getCallThis() throws Throwable
  { 
    ESBase obj = caller.stack[caller.top - 1];

    return obj.toObject();
  }

  ESBase getContext()
  {
    return scope[scopeLength - 1];
  }

  ESBase getFunctionContext()
  {
    if (caller == null || caller.scopeLength == 0)
      return global;
    else
      return caller.scope[caller.scopeLength - 1];
  }

  public void pushScope(ESBase value)
  {
    scope[scopeLength++] = value;
  }

  public void popScope()
  {
    scopeLength--;
  }

  public ESObject getEval()
  {
    return (ESObject) scope[scopeLength - 1];
  }
  
  public ESObject createArg(ESId []args, int length)
    throws Throwable
  {
    ESObject arg = ESArguments.create(args, this, length);

    scope[scopeLength++] = arg;

    return arg;
  }

  public void setProperty(ESString name, ESBase value) throws Throwable
  {
    //setProperty(name, value);
  }

  public ESBase delete(ESString key) throws Throwable
  {
    return aux == null ? ESBoolean.FALSE : aux.delete(key); 
  } 

  public ESBase findScopeProperty(ESString id) throws Throwable
  {
    for (int i = scopeLength - 1; i > 0; i--) {
      if (scope[i].getProperty(id) != esEmpty)
        return scope[i];
    }

    return global;
  }

  public ESBase scopeTypeof(ESString id) throws Throwable
  {
    for (int i = scopeLength - 1; i >= 0; i--) {
      ESBase value;
      if ((value = scope[i].getProperty(id)) != esEmpty)
        return value.typeof();
    }

    return esEmpty.typeof();
  }

  public static ESBase setProperty(ESBase base, ESString field, ESBase value)
    throws Throwable
  {
    base.setProperty(field, value);
    
    return value;
  }

  public static ESBase doVoid(ESBase value)
  {
    return ESBase.esUndefined;
  }

  public ESBase array(ESBase value)
    throws Throwable
  {
    ESBase array = call(global, ARRAY, 0);
    array.setProperty(0, value);
    return array;
  }

  public static ESBase comma(ESBase left, ESBase right)
  {
    return right;
  }

  public static ESBase _first(ESBase left, ESBase right)
    throws Throwable
  {
    // This is only used for postfix
    if (! (left instanceof ESNumber))
      return ESNumber.create(left.toNum());
    else
      return left;
  }

  public static double _first(double left, double right)
    throws Throwable
  {
    return left;
  }

  /**
   * Returns the first value in a tuple.  Used
   *
   * @param left the first value
   * @param right the right value
   *
   * @return the first value.
   */
  public static int _first(int left, int right)
    throws Throwable
  {
    return left;
  }

  public static double _pre(ESBase expr, ESString field, int inc)
    throws Throwable
  {
    double oldVal = expr.getProperty(field).toNum();
    ESNumber newVal = ESNumber.create(oldVal + inc);

    expr.setProperty(field, newVal);
    
    return oldVal + inc;
  }

  public static double _post(ESBase expr, ESString field, int inc)
    throws Throwable
  {
    double oldVal = expr.getProperty(field).toNum();
    ESNumber newVal = ESNumber.create(oldVal + inc);

    expr.setProperty(field, newVal);
    
    return oldVal;
  }

  public double _pre(ESString field, int inc)
    throws Throwable
  {
    double oldVal = getScopeProperty(field).toNum();
    ESNumber newVal = ESNumber.create(oldVal + inc);

    setScopeProperty(field, newVal);
    
    return oldVal + inc;
  }

  public double _post(ESString field, int inc)
    throws Throwable
  {
    double oldVal = getScopeProperty(field).toNum();
    ESNumber newVal = ESNumber.create(oldVal + inc);

    setScopeProperty(field, newVal);
    
    return oldVal;
  }

  public ESBase setGlobalProperty(ESString id, ESBase value) throws Throwable
  {
    global.setProperty(id, value);
    
    return value;
  }

  /**
   * Returns the global variable of the id, throwing an exception if
   * the it's undefined.
   */
  public ESBase getGlobalVariable(ESString id) throws Throwable
  {
    ESBase value = global.getProperty(id);
    if (value == ESBase.esEmpty)
      throw new ESUndefinedException("undefined variable `" + id + "'");

    return value;
  }

  public ESBase getScopeProperty(ESString id) throws Throwable
  {
    for (int i = scopeLength - 1; i >= 0; i--) {
      ESBase value;
      if ((value = scope[i].getProperty(id)) != esEmpty) {
        return value;
      }
    }

    throw new ESUndefinedException("undefined variable `" + id + "'");
  }

  public void fillScope()
  {
    if (callee instanceof ESClosure) {
      ESClosure closure = (ESClosure) callee;

      if (closure.scopeLength == 0) {
        scope[0] = caller.global;
        scopeLength = 1;
        return;
      }
      for (int i = 0; i < closure.scopeLength; i++) {
        scope[i] = closure.scope[i];
      }
      scopeLength = closure.scopeLength;
    }
    else {
      scope[0] = caller.global;
      scopeLength = 1;
    }
  }

  public ESBase hasScopeProperty(ESString id) throws Throwable
  {
    for (int i = scopeLength - 1; i >= 0; i--) {
      ESBase value;
      if ((value = scope[i].getProperty(id)) != esEmpty)
        return value;
    }

    return esEmpty;
  }

  public ESBase setScopeProperty(ESString id, ESBase value) throws Throwable
  {
    if (value == esEmpty)
      value = esUndefined;

    for (int i = scopeLength - 1; i > 0; i--) {
      if (scope[i].getProperty(id) != esEmpty) {
        scope[i].setProperty(id, value);
        return value;
      }
    }

    global.setProperty(id, value);

    return value;
  }

  public ESBase deleteScopeProperty(ESString id) throws Throwable
  {
    for (int i = scopeLength - 1; i > 0; i--) {
      if (scope[i].getProperty(id) != esEmpty)
        return scope[i].delete(id);
    }

    return global.delete(id);
  }
  /*
  public ESBase startCallScopeProperty(ESString id, int i) throws Throwable
  {
    top = i + 1; 

    for (int j = scopeLength - 1; j >= 0; j--) {
      ESBase value; 

      if ((value = scope[j].getProperty(id)) != esEmpty) {
        stack[i] = global;
        return value;
      }
    }

    throw new ESUndefinedException("undefined call `" + id + "'");
  }
  */

  public int arg(int i, ESBase arg)
  {
    stack[i + 1] = arg;
    
    return 1;
  }

  public ESBase callScope(ESString id, int i) throws Throwable
  {
    top = i + 1;

    int scopeLength = caller.scopeLength;
    ESBase []scope = caller.scope;
    for (int j = scopeLength - 1; j >= 0; j--) {
      ESBase value; 

      if ((value = scope[j].getProperty(id)) != esEmpty) {
        callee = value;
        stack[i] = scope[j];
        return value.call(this, 0);
      }
    }

    throw new ESUndefinedException("undefined call `" + id + "'");
  }

  public ESBase callScope(ESString id, int i, ESBase a) throws Throwable
  {
    top = i + 1;

    int scopeLength = caller.scopeLength;
    ESBase []scope = caller.scope;
    for (int j = scopeLength - 1; j >= 0; j--) {
      ESBase value; 

      if ((value = scope[j].getProperty(id)) != esEmpty) {
        callee = value;
        stack[i] = scope[j];
        stack[i + 1] = a;
        return value.call(this, 1);
      }
    }

    throw new ESUndefinedException("undefined call `" + id + "'");
  }

  public ESBase callScope(ESString id, int i, ESBase a, ESBase b) 
    throws Throwable
  {
    top = i + 1; 

    int scopeLength = caller.scopeLength;
    ESBase []scope = caller.scope;
    for (int j = scopeLength - 1; j >= 0; j--) {
      ESBase value; 

      if ((value = scope[j].getProperty(id)) != esEmpty) {
        callee = value;
        stack[i] = scope[j];
        stack[i + 1] = a;
        stack[i + 2] = b;
        return value.call(this, 2);
      }
    }

    throw new ESUndefinedException("undefined call `" + id + "'");
  }

  public ESBase callScope(ESString id, int i, ESBase a, ESBase b, ESBase c,
                          int length)
    throws Throwable
  {
    top = i + 1; 

    int scopeLength = caller.scopeLength;
    ESBase []scope = caller.scope;
    for (int j = scopeLength - 1; j >= 0; j--) {
      ESBase value;
      if ((value = scope[j].getProperty(id)) != esEmpty) {
        callee = value;
        stack[i] = scope[j];
        stack[i + 1] = a;
        stack[i + 2] = b;
        stack[i + 3] = c;
        return value.call(this, length);
      }
    }

    throw new ESUndefinedException("undefined call `" + id + "'");
  }

  public ESBase call(ESBase base, ESString name, int i) 
    throws Throwable
  {
    top = i + 1; 

    stack[i] = base;
    
    return base.call(this, 0, name);
  }

  public ESBase call(ESBase base, ESString name, int i, ESBase a) 
    throws Throwable
  {
    top = i + 1;

    stack[i] = base;
    stack[i + 1] = a;

    return base.call(this, 1, name);
  }

  public ESBase call(ESBase base, ESString name, int i, ESBase a, ESBase b) 
    throws Throwable
  {
    top = i + 1; 

    stack[i] = base;
    stack[i + 1] = a;
    stack[i + 2] = b;

    return base.call(this, 2, name);
  }

  public ESBase call(ESBase base, ESString name, int i, 
                     ESBase a, ESBase b, ESBase c, int length)
    throws Throwable
  {
    top = i + 1; 

    stack[i] = base;
    stack[i + 1] = a;
    stack[i + 2] = b;
    stack[i + 3] = c;

    return base.call(this, length, name);
  }

  public ESBase call(ESBase base, int i) 
    throws Throwable
  {
    top = i + 1; 

    stack[i] = global;
    callee = base;
    
    return base.call(this, 0);
  }

  public ESBase call(ESBase base, int i, ESBase a) 
    throws Throwable
  {
    top = i + 1;

    stack[i + 1] = a;
    stack[i] = global;
    callee = base;
    return base.call(this, 1);
  }

  public ESBase call(ESBase base, int i, ESBase a, ESBase b) 
    throws Throwable
  {
    top = i + 1; 

    stack[i] = base;
    stack[i + 1] = a;
    stack[i + 2] = b;
    stack[i] = global;
    callee = base;
    return base.call(this, 2);
  }

  public ESBase call(ESBase base, int i,
                     ESBase a, ESBase b, ESBase c, int length)
    throws Throwable
  {
    top = i + 1; 

    stack[i] = base;
    stack[i + 1] = a;
    stack[i + 2] = b;
    stack[i + 3] = c;
    stack[i] = global;
    callee = base;
    return base.call(this, length);
  }

  public ESBase newScope(ESString id, int i) throws Throwable
  {
    top = i + 1;

    int scopeLength = caller.scopeLength;
    ESBase []scope = caller.scope;
    for (int j = scopeLength - 1; j >= 0; j--) {
      ESBase value; 

      if ((value = scope[j].getProperty(id)) != esEmpty) {
        callee = value;
        stack[i] = global;
        return value.construct(this, 0);
      }
    }

    throw new ESUndefinedException("undefined constructor `" + id + "'");
  }

  public ESBase newScope(ESString id, int i, ESBase a) throws Throwable
  {
    top = i + 1;

    int scopeLength = caller.scopeLength;
    ESBase []scope = caller.scope;
    for (int j = scopeLength - 1; j >= 0; j--) {
      ESBase value; 

      if ((value = scope[j].getProperty(id)) != esEmpty) {
        callee = value;
        stack[i] = global;
        stack[i + 1] = a;
        return value.construct(this, 1);
      }
    }

    throw new ESUndefinedException("undefined constructor `" + id + "'");
  }

  public ESBase newScope(ESString id, int i, ESBase a, ESBase b)
    throws Throwable
  {
    top = i + 1;

    int scopeLength = caller.scopeLength;
    ESBase []scope = caller.scope;
    for (int j = scopeLength - 1; j >= 0; j--) {
      ESBase value; 

      if ((value = scope[j].getProperty(id)) != esEmpty) {
        callee = value;
        stack[i] = global;
        stack[i + 1] = a;
        stack[i + 2] = b;
        return value.construct(this, 2);
      }
    }

    throw new ESUndefinedException("undefined constructor `" + id + "'");
  }

  public ESBase newScope(ESString id, int i, ESBase a, ESBase b, ESBase c,
                          int length)
    throws Throwable
  {
    top = i + 1; 

    int scopeLength = caller.scopeLength;
    ESBase []scope = caller.scope;
    for (int j = scopeLength - 1; j >= 0; j--) {
      ESBase value; 

      if ((value = scope[j].getProperty(id)) != esEmpty) {
        callee = value;
        stack[i] = global;
        stack[i + 1] = a;
        stack[i + 2] = b;
        stack[i + 3] = c;
        return value.construct(this, length);
      }
    }

    throw new ESUndefinedException("undefined constructor `" + id + "'");
  }

  public ESBase doNew(ESBase base, ESString name, int i) 
    throws Throwable
  {
    top = i + 1; 

    ESBase obj = base.getProperty(name);
    stack[i] = base;
    callee = obj;
    if (obj != esEmpty)
      return obj.construct(this, 0);
    else
      throw new ESUndefinedException("undefined constructor `" + name + "'");
  }

  public ESBase doNew(ESBase base, ESString name, int i, ESBase a) 
    throws Throwable
  {
    top = i + 1;

    stack[i] = base;
    stack[i + 1] = a;

    ESBase obj = base.getProperty(name);
    callee = obj;
    if (obj != esEmpty)
      return obj.construct(this, 1);
    else
      throw new ESUndefinedException("undefined constructor `" + name + "'");
  }

  public ESBase doNew(ESBase base, ESString name, int i, ESBase a, ESBase b) 
    throws Throwable
  {
    top = i + 1; 

    stack[i] = base;
    stack[i + 1] = a;
    stack[i + 2] = b;

    ESBase obj = base.getProperty(name);
    callee = obj;
    if (obj != esEmpty)
      return obj.construct(this, 2);
    else
      throw new ESUndefinedException("undefined constructor `" + name + "'");
  }

  public ESBase doNew(ESBase base, ESString name, int i, 
                      ESBase a, ESBase b, ESBase c, int length)
    throws Throwable
  {
    top = i + 1; 

    stack[i] = base;
    stack[i + 1] = a;
    stack[i + 2] = b;
    stack[i + 3] = c;

    ESBase obj = base.getProperty(name);
    callee = obj;
    if (obj != esEmpty)
      return obj.construct(this, length);
    else
      throw new ESUndefinedException("undefined constructor `" + name + "'");
  }

  public ESBase doNew(ESBase base, int i) 
    throws Throwable
  {
    top = i + 1; 

    stack[i] = global;
    callee = base;
    return base.construct(this, 0);
  }

  public ESBase doNew(ESBase base, int i, ESBase a) 
    throws Throwable
  {
    top = i + 1;

    stack[i] = global;
    stack[i + 1] = a;
    callee = base;
    
    return base.construct(this, 1);
  }

  public ESBase doNew(ESBase base, int i, ESBase a, ESBase b) 
    throws Throwable
  {
    top = i + 1; 

    stack[i] = global;
    stack[i + 1] = a;
    stack[i + 2] = b;
    callee = base;
    
    return base.construct(this, 2);
  }

  public ESBase doNew(ESBase base, int i, 
                      ESBase a, ESBase b, ESBase c, int length)
    throws Throwable
  {
    top = i + 1; 

    stack[i] = global;
    stack[i + 1] = a;
    stack[i + 2] = b;
    stack[i + 3] = c;
    callee = base;
    
    return base.construct(this, length);
  }

  public void free()
  {
    clear();
    
    for (int i = stack.length - 1; i >= 0; i--)
      stack[i] = null;
    for (int i = scope.length - 1; i >= 0; i--)
      scope[i] = null;
    for (int i = values.length - 1; i >= 0; i--)
      values[i] = null;
    global = null;
  }

  public static Iterator toESIterator(Iterator i)
  {
    return new ESIterator(i);
  }

  public static Iterator toESIterator(Enumeration e)
  {
    return new ESEnumIterator(e);
  }

  public static boolean matchException(ESBase test, Exception e)
  {
    String testString;
    try {
      testString = test.toStr().toString();
    } catch (Throwable foo) {
      testString = "undefined";
    }

    Class eClass = e.getClass();
    String dotted = "." + test;

    for (; eClass != null; eClass = eClass.getSuperclass()) {
      String eString = eClass.getName();

      if (testString.equals(eString) || eString.endsWith(dotted))
        return true;
    }

    return false;
  }

  static class ESIterator implements Iterator {
    Global resin;
    Iterator i;

    public boolean hasNext() 
    {
      return i != null && i.hasNext();
    }

    public Object next()
    {
      Object value = i == null ? null : i.next();

      Object result;
      try {
        if (value == null)
          result = ESBase.esNull;
        else
          result = resin.objectWrap(value);
      } catch (Throwable e) {
        result = ESBase.esNull;
      }

      return result;
    }

    public void remove() { throw new RuntimeException(); }

    ESIterator(Iterator i)
    {
      this.i = i;
      this.resin = Global.getGlobalProto();
    }
  }

  static class ESEnumIterator implements Iterator {
    Global resin;
    Enumeration e;

    public boolean hasNext() 
    {
      return e != null && e.hasMoreElements();
    }

    public Object next()
    {
      Object value = e != null ? e.nextElement() : null;;

      try {
        if (value == null)
          return ESBase.esNull;
        else
          return resin.objectWrap(value);
      } catch (Throwable e) {
        return ESBase.esNull;
      }
    }

    public void remove() { throw new RuntimeException(); }

    ESEnumIterator(Enumeration e)
    {
      this.e = e;
      this.resin = Global.getGlobalProto();
    }
  }
}
