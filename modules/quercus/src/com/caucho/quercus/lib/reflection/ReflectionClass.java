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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.reflection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.ClassField;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.MethodMap;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.QuercusLanguageException;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Var;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.program.ClassDef;
import com.caucho.util.L10N;

public class ReflectionClass
  implements Reflector
{
  private static final L10N L = new L10N(ReflectionClass.class);
  
  public static int IS_IMPLICIT_ABSTRACT = 16;
  public static int IS_EXPLICIT_ABSTRACT = 32;
  public static int IS_FINAL = 64;
  
  public String _name;
  private QuercusClass _cls;
  
  protected ReflectionClass(QuercusClass cls)
  {
    _cls = cls;
    _name = cls.getName();
  }
  
  protected ReflectionClass(Env env, String name)
  {
    _cls = env.findClass(name);
    _name = name;
  }
  
  protected QuercusClass getQuercusClass()
  {
    return _cls;
  }
  
  final private ReflectionClass __clone()
  {
    return new ReflectionClass(_cls);
  }
  
  public static ReflectionClass __construct(Env env, Value obj)
  {
    QuercusClass cls;
    
    if (obj.isObject())
      cls = ((ObjectValue) obj.toValue()).getQuercusClass();
    else
      cls = env.findClass(obj.toString());
    
    if (cls == null)
      throw new ReflectionException(L.l("class '{0}' doesn't exist", obj));

    return new ReflectionClass(cls);
  }
  
  public static String export(Env env,
                              Value cls,
                              @Optional boolean isReturn)
  {
    return null;
  }
  
  public String getName()
  {
    return _name;
  }
  
  public boolean isInternal()
  {
    throw new UnimplementedException("ReflectionClass->isInternal()");
  }
  
  public boolean isUserDefined()
  {
    throw new UnimplementedException("ReflectionClass->isUserDefined()");
  }
  
  public boolean isInstantiable()
  {
    return ! _cls.isInterface();
  }
  
  public boolean hasConstant(String name)
  {
    return _cls.hasConstant(name);
  }

  public String getFileName()
  {
    return _cls.getClassDef().getLocation().getFileName();
  }
  
  public int getStartLine()
  {
    return _cls.getClassDef().getLocation().getLineNumber();
  }
  
  public int getEndLine()
  {
    // TODO
    return _cls.getClassDef().getLocation().getLineNumber();
  }
  
  @ReturnNullAsFalse
  public String getDocComment()
  {
    ClassDef def = _cls.getClassDef();
    
    return def.getComment();
  }
  
  public ReflectionMethod getConstructor()
  {
    AbstractFunction cons = _cls.getConstructor();
    
    if (cons != null)
      return new ReflectionMethod(_name, cons);
    else
      return null;
  }
  
  public boolean hasMethod(StringValue name)
  {
    MethodMap<AbstractFunction> map = _cls.getMethodMap();
    
    return map.containsKey(name);
  }
  
  public ReflectionMethod getMethod(Env env, StringValue name)
  {
    AbstractFunction fun = _cls.findFunction(name);
    
    if (fun == null)
      throw new QuercusLanguageException(
        env.createException("ReflectionException",
                            L.l(
                                "method {0}::{1}() does not exist",
                                _name,
                                name)));
    
    return new ReflectionMethod(_name, _cls.getFunction(name));
    
    /*
    MethodMap<AbstractFunction> map = _cls.getMethodMap();
    
    return new ReflectionMethod(_name, map.get(name));
    */
  }
  
  public ArrayValue getMethods(Env env)
  {
    ArrayValue array = new ArrayValueImpl();
    
    MethodMap<AbstractFunction> map = _cls.getMethodMap();
    
    for (AbstractFunction method : map.values()) {
      array.put(env.wrapJava(new ReflectionMethod(_cls.getName(), method)));
    }
    
    return array;
  }
  
  public boolean hasProperty(StringValue name)
  {
    return _cls.getClassField(name) != null;
  }
  
  public ReflectionProperty getProperty(Env env, StringValue name)
  {
    return new ReflectionProperty(env, _cls, name);
  }
  
  public ArrayValue getProperties(Env env)
  {
    ArrayValue array = new ArrayValueImpl();
    
    HashMap<StringValue,ClassField> fieldMap = _cls.getClassFields();
    
    for (ClassField field : fieldMap.values()) {
      if (field.isPublic()) {
        ReflectionProperty prop
          = ReflectionProperty.create(env, _cls, field.getName(), false);
      
        array.put(env.wrapJava(prop));
      }
    }
    
    ArrayList<StringValue> staticFieldList = _cls.getStaticFieldNames();
    
    for (StringValue fieldName : staticFieldList) {
      ReflectionProperty prop
        = ReflectionProperty.create(env, _cls, fieldName, true);
      
      array.put(env.wrapJava(prop));
    }
    
    return array;
  }
  
  public ArrayValue getConstants(Env env)
  {
    ArrayValue array = new ArrayValueImpl();
    
    HashMap<String, Value> _constMap = _cls.getConstantMap(env);
    
    for (Map.Entry<String, Value> entry : _constMap.entrySet()) {
      Value name = env.createString(entry.getKey());
      
      array.put(name, entry.getValue());
    }

    return array;
  }
  
  public Value getConstant(Env env, String name)
  {
    if (hasConstant(name))
      return _cls.getConstant(env, name);
    else
      return BooleanValue.FALSE;
  }
  
  public ArrayValue getInterfaces(Env env)
  {
    ArrayValue array = new ArrayValueImpl();
    
    findInterfaces(env, array, _cls);
    
    return array;
  }

  private void findInterfaces(Env env, ArrayValue array, QuercusClass cls)
  {
    if (cls.isInterface()) {
      array.put(StringValue.create(cls.getName()),
                env.wrapJava(new ReflectionClass(cls)));
    }
    else {
      ClassDef []defList = cls.getClassDefList();
      
      for (int i = 0; i < defList.length; i++) {
        findInterfaces(env, array, defList[i]);
      }
    }
  }
  
  private void findInterfaces(Env env, ArrayValue array, ClassDef def)
  {
    String name = def.getName();
    
    if (def.isInterface()) {
      addInterface(env, array, name);
    }
    else {
      String []defList = def.getInterfaces();
      
      for (int i = 0; i < defList.length; i++) {
        QuercusClass cls = env.findClass(defList[i]);
        
        findInterfaces(env, array, cls);
      }
    }
  }
  
  private void addInterface(Env env, ArrayValue array, String name)
  {
    QuercusClass cls = env.findClass(name);

    array.put(StringValue.create(name),
              env.wrapJava(new ReflectionClass(cls)));
  }
  
  public boolean isInterface()
  {
    return _cls.isInterface();
  }
  
  public boolean isAbstract()
  {
    return _cls.isAbstract();
  }
  
  public boolean isFinal()
  {
    return _cls.isFinal();
  }
  
  public int getModifiers()
  {
    int flag = 0;
    
    if (isFinal())
      flag |= IS_FINAL;
    
    return flag;
  }
  
  public boolean isInstance(ObjectValue obj)
  {
    return obj.getQuercusClass().getName().equals(_name);
  }
  
  public Value newInstance(Env env, @Optional Value []args)
  {
    return _cls.callNew(env, args);
  }
  
  public Value newInstanceArgs(Env env, @Optional ArrayValue args)
  {
    if (args == null)
      return _cls.callNew(env, new Value []{});
    else
      return _cls.callNew(env, args.getValueArray(env));
  }
  
  @ReturnNullAsFalse
  public ReflectionClass getParentClass()
  {
    QuercusClass parent = _cls.getParent();
    
    if (parent == null)
      return null;
    else
      return new ReflectionClass(parent);
  }
  
  public boolean isSubclassOf(Env env, Object obj)
  {
    String clsName;
    
    if (obj instanceof ReflectionClass)
      clsName = ((ReflectionClass) obj).getName();
    else
      clsName = obj.toString();

    // php/520p
    if (_cls.getName().equals(clsName))
      return false;
    
    return _cls.isA(clsName);
  }
  
  public ArrayValue getStaticProperties(Env env)
  {
    ArrayValue array = new ArrayValueImpl();
    
    getStaticFields(env, array, _cls);
    
    return array;
  }
  
  private void getStaticFields(Env env, ArrayValue array, QuercusClass cls)
  {
    if (cls == null)
      return;
    
    for (StringValue name : cls.getStaticFieldNames()) {
      Value field = cls.getStaticFieldValue(env, name);
      array.put(name, field.toValue());
    }
    
    getStaticFields(env, array, cls.getParent());
  }
  
  public Value getStaticPropertyValue(Env env,
                                      StringValue name,
                                      @Optional Value defaultV)
  {
    Value field = _cls.getStaticField(env, name);
    
    if (field == null) {
      if (! defaultV.isDefault())
        return defaultV;
      else
        throw new QuercusLanguageException(
            env.createException(
                "ReflectionException",
                L.l(
                    "Class '{0}' does not have a property named '{1}'",
                    _name, 
                    name)));
    }

    return field;
  }
  
  public void setStaticPropertyValue(Env env, StringValue name, Value value)
  {
    _cls.getStaticFieldVar(env, name).set(value);
  }
  
  public ArrayValue getDefaultProperties(Env env)
  {
    ArrayValue array = new ArrayValueImpl();
    
    getStaticFields(env, array, _cls);
    
    HashMap<StringValue, ClassField> fieldMap = _cls.getClassFields();
    for (Map.Entry<StringValue, ClassField> entry : fieldMap.entrySet()) {
      Expr initExpr = entry.getValue().getInitValue();
      
      array.put(entry.getKey(), initExpr.eval(env));
    }
    
    return array;
  }
  
  public boolean isIterateable()
  {
    return _cls.getTraversableDelegate() != null;
  }
  
  public boolean implementsInterface(Env env, String name)
  {
    return _cls.implementsInterface(env, name);
  }
  
  public ReflectionExtension getExtension(Env env)
  {
    String extName = getExtensionName();
    
    if (extName != null)
      return new ReflectionExtension(env, extName);
    else
      return null;
  }
  
  public String getExtensionName()
  {
    return _cls.getExtension();
  }
  
  public String toString()
  {
    return "ReflectionClass[" + _name + "]";
  }
}
