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

package com.caucho.quercus.program;

import com.caucho.quercus.env.*;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.Location;
import com.caucho.util.L10N;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a Quercus class definition
 */
abstract public class ClassDef {
  private final static L10N L = new L10N(ClassDef.class);

  private final Location _location;
  private final String _name;
  private final String _parentName;

  private String []_ifaceList;

  protected ClassDef(Location location,
                     String name,
                     String parentName,
                     String []ifaceList)
  {
    _location = location;
    _name = name;
    _parentName = parentName;
    _ifaceList = ifaceList;
  }

  /**
   * Returns the location for where the
   * class was defined, null if it is unknown.
   */
  public Location getLocation()
  {
    return _location;
  }

  /**
   * Returns the name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the parent name.
   */
  public String getParentName()
  {
    return _parentName;
  }
  
  /*
   * Returns the name of the extension that this class is part of.
   */
  public String getExtension()
  {
    return null;
  }

  protected void addInterface(String iface)
  {
    for (int i = 0; i < _ifaceList.length; i++)
      if (_ifaceList[i].equals(iface))
        return;

    String[] ifaceList = new String[_ifaceList.length + 1];

    System.arraycopy(_ifaceList, 0, ifaceList, 0, _ifaceList.length);
    ifaceList[ifaceList.length - 1] = iface;

    _ifaceList = ifaceList;
  }

  /**
   * forces a load of any lazy ClassDef
   */
  public ClassDef loadClassDef()
  {
    return this;
  }
  
  public AbstractFunction getCall()
  {
    return null;
  }

  public void init()
  {
  }
  
  public void init(QuercusClass cl)
  {
  }
  
  /**
   * Returns the interfaces.
   */
  public String []getInterfaces()
  {
    return _ifaceList;
  }

  /**
   * Adds the interfaces to the set
   */
  public void addInterfaces(HashSet<String> interfaceSet)
  {
    interfaceSet.add(getName().toLowerCase());
    
    for (String name : getInterfaces()) {
      interfaceSet.add(name.toLowerCase());
    }
  }

  /**
   * Return true for an abstract class.
   */
  public boolean isAbstract()
  {
    return false;
  }

  /**
   * Return true for an interface class.
   */
  public boolean isInterface()
  {
    return false;
  }
  
  /*
   * Returns true for a final class.
   */
  public boolean isFinal()
  {
    return false;
  }
  
  /*
   * Returns true if the class has private/protected methods.
   */
  public boolean hasNonPublicMethods()
  {
    return false;
  }
  
  /**
   * Initialize the quercus class.
   */
  public void initClass(QuercusClass cl)
  {
  }

  /**
   * Creates a new instance.
   */
  public ObjectValue newInstance(Env env, QuercusClass qcl)
  {
    if (isAbstract()) {
      throw env.createErrorException(
        L.l("abstract class '{0}' cannot be instantiated.", getName()));
    }
    else if (isInterface()) {
      throw env.createErrorException(
        L.l("interface '{0}' cannot be instantiated.", getName()));
    }
    
    return new ObjectExtValue(qcl);
  }
  
  /*
   * Creates a new object.
   */
  public ObjectValue createObject(Env env, QuercusClass cls)
  {
    if (isAbstract()) {
      throw env.createErrorException(
        L.l("abstract class '{0}' cannot be instantiated.", getName()));
    }
    else if (isInterface()) {
      throw env.createErrorException(
        L.l("interface '{0}' cannot be instantiated.", getName()));
    }
    
    return new ObjectExtValue(cls);
  }

  /**
   * Creates a new instance.
   */
  public Value callNew(Env env, Expr []args)
  {
    return null;
  }

  /**
   * Creates a new instance.
   */
  public Value callNew(Env env, Value []args)
  {
    return null;
  }

  /**
   * Returns value for instanceof.
   */
  public boolean isA(String name)
  {
    if (_name.equalsIgnoreCase(name))
      return true;

    for (int i = 0; i < _ifaceList.length; i++) {
      if (_ifaceList[i].equalsIgnoreCase(name))
        return true;
    }

    return false;
  }

  /**
   * Returns the constructor
   */
  abstract public AbstractFunction findConstructor();

  /**
   * Finds the matching constant
   */
  public Expr findConstant(String name)
  {
    return null;
  }
  
  /**
   * Returns the documentation for this class.
   */
  public String getComment()
  {
    return null;
  }
  
  /**
   * Returns the comment for the specified field.
   */
  public String getFieldComment(StringValue name)
  {
    return null;
  }
  
  /**
   * Returns the comment for the specified static field.
   */
  public String getStaticFieldComment(String name)
  {
    return null;
  }

  public String toString()
  {
    return getClass().getSimpleName()
           + "@"
           + System.identityHashCode(this)
           + "[" + _name + "]";
  }

  public Set<Map.Entry<StringValue, FieldEntry>> fieldSet()
  {
    return null;
  }
  
  public Set<Map.Entry<String, StaticFieldEntry>> staticFieldSet()
  {
    return null;
  }
  
  public Set<Map.Entry<String, AbstractFunction>> functionSet()
  {
    return null;
  }


  public static class FieldEntry {
    private final Expr _value;
    private final FieldVisibility _visibility;
    private final String _comment;

    public FieldEntry(Expr value, FieldVisibility visibility)
    {
      _value = value;
      _visibility = visibility;
      _comment = null;
    }
    
    public FieldEntry(Expr value, FieldVisibility visibility, String comment)
    {
      _value = value;
      _visibility = visibility;
      _comment = comment;
    }

    public Expr getValue()
    {
      return _value;
    }

    public FieldVisibility getVisibility()
    {
      return _visibility;
    }
    
    public String getComment()
    {
      return _comment;
    }
  }
  
  public static class StaticFieldEntry {
    private final Expr _value;
    private final String _comment;

    public StaticFieldEntry(Expr value)
    {
      _value = value;
      _comment = null;
    }
    
    public StaticFieldEntry(Expr value, String comment)
    {
      _value = value;
      _comment = comment;
    }

    public Expr getValue()
    {
      return _value;
    }
    
    public String getComment()
    {
      return _comment;
    }
  }
}

