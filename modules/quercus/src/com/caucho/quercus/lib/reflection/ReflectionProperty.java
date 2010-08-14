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

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.program.ClassDef;
import com.caucho.util.L10N;

public class ReflectionProperty
  implements Reflector
{
  private static final L10N L = new L10N(ReflectionProperty.class);
  
  public static final int IS_STATIC = 1;
  public static final int IS_PUBLIC = 256;
  public static final int IS_PROTECTED = 512;
  public static final int IS_PRIVATE = 1024;

  private Property _prop;
  
  protected ReflectionProperty(Property prop)
  {
    _prop = prop;
  }
  
  protected ReflectionProperty(Env env, QuercusClass cls, StringValue nameV)
  {
    _prop = Property.create(env, cls, nameV);
  }
  
  protected static ReflectionProperty create(Env env,
                                             QuercusClass cls,
                                             StringValue propName,
                                             boolean isStatic)
  {
    Property prop;
    
    if (isStatic)
      prop = new StaticProperty(cls, propName);
    else
      prop = new Property(cls, propName);
    
    return new ReflectionProperty(prop);
  }
  
  final private void __clone()
  {
  }
  
  public static ReflectionProperty __construct(Env env,
                                               String clsName,
                                               StringValue propName)
  {
    QuercusClass cls = env.findClass(clsName);

    if (cls == null) {
      throw new ReflectionException(L.l("Cannot find class '{0}'", clsName));
    }
    
    return new ReflectionProperty(env, cls, propName);
  }

  public static String export(Env env,
                              Value cls,
                              String name,
                              @Optional boolean isReturn)
  {
    return null;
  }
  
  public StringValue getName()
  {
    return _prop.getName();
  }
  
  public boolean isPublic()
  {
    return false;
  }
  
  public boolean isPrivate()
  {
    return false;
  }
  
  public boolean isProtected()
  {
    return false;
  }
  
  public boolean isStatic()
  {
    return _prop.isStatic();
  }
  
  /*
   * XXX: no documentation whatsoever
   */
  public boolean isDefault()
  {
    return true;
  }
  
  public int getModifiers()
  {
    return -1;
  }
  
  public Value getValue(Env env, @Optional ObjectValue obj)
  {
    return _prop.getValue(env, obj);
  }
  
  public void setValue(Env env, ObjectValue obj, Value value)
  {
    _prop.setValue(env, obj, value);
  }
  
  public ReflectionClass getDeclaringClass(Env env)
  {
    return _prop.getDeclaringClass(env);
  }
  
  @ReturnNullAsFalse
  public String getDocComment(Env env)
  {
    return _prop.getComment(env);
  }
  
  public String toString()
  {
    return "ReflectionProperty[" + _prop.toString() + "]";
  }

  static class Property
  {
    final QuercusClass _cls;
    final StringValue _nameV;
    
    QuercusClass _declaringClass;
    
    public static Property create(Env env, QuercusClass cls, StringValue nameV)
    { 
      if (cls.getClassField(nameV) != null)
        return new Property(cls, nameV);
      else if (cls.getStaticFieldValue(env, nameV) != null)
        return new StaticProperty(cls, nameV);
      else
        throw new ReflectionException(L.l("Property {0}->${1} does not exist",
                                          cls.getName(), nameV));
    }
    
    protected Property(QuercusClass cls, StringValue nameV)
    {
      _cls = cls;
      _nameV = nameV;
    }
    
    public boolean isStatic()
    {
      return false;
    }
    
    public final StringValue getName()
    {
      return _nameV;
    }
    
    public Value getValue(Env env, ObjectValue obj)
    {
      return obj.getField(env, _nameV);
    }
    
    public void setValue(Env env, ObjectValue obj, Value value)
    {
      obj.putField(env, _nameV, value);
    }
    
    public final ReflectionClass getDeclaringClass(Env env)
    {
      QuercusClass cls = getDeclaringClass(env, _cls);
      
      if (cls != null)
        return new ReflectionClass(cls);
      else
        return null;
    }
    
    protected final QuercusClass getDeclaringClass(Env env, QuercusClass cls)
    {
      if (_declaringClass == null)
        _declaringClass = getDeclaringClassImpl(env, cls);
      
      return _declaringClass;
    }
    
    protected QuercusClass getDeclaringClassImpl(Env env, QuercusClass cls)
    {
      if (cls == null)
        return null;
      
      QuercusClass refClass = getDeclaringClassImpl(env, cls.getParent());

      if (refClass != null)
        return refClass;
      else if (cls.getClassField(_nameV) != null)
        return cls;

      return null;
    }
    
    public String getComment(Env env)
    {
      QuercusClass cls = getDeclaringClass(env, _cls);
      
      ClassDef def = cls.getClassDef();

      return def.getFieldComment(_nameV);
    }
    
    public String toString()
    {
      if (_cls.getName() != null)
        return _cls.getName() + "->" + _nameV;
      else
        return _nameV.toString();
    }
  }
  
  static class StaticProperty extends Property
  {
    private StringValue _name;
    
    public StaticProperty(QuercusClass cls, StringValue nameV)
    {
      super(cls, nameV);
      
      _name = nameV;
    }
    
    @Override
    public boolean isStatic()
    {
      return true;
    }
    
    @Override
    public Value getValue(Env env, ObjectValue obj)
    {
      return _cls.getStaticFieldValue(env, _name);
    }
    
    @Override
    public void setValue(Env env, ObjectValue obj, Value value)
    {
      _cls.getStaticFieldVar(env, _name).set(value);
    }
    
    @Override
    protected QuercusClass getDeclaringClassImpl(Env env, QuercusClass cls)
    {
      if (cls == null)
        return null;
      
      QuercusClass refClass = getDeclaringClassImpl(env, cls.getParent());

      if (refClass != null)
        return refClass;
      else if (cls.getStaticField(env, _name) != null)
        return cls;

      return null;
    }
    
    public String getComment(Env env)
    {
      QuercusClass cls = getDeclaringClass(env, _cls);
      
      ClassDef def = cls.getClassDef();

      return def.getStaticFieldComment(_name.toString());
    }
    
    public String toString()
    {
      if (_cls.getName() != null)
        return _cls.getName() + "::" + _name;
      else
        return _name.toString();
    }
  }
}
