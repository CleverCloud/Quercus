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

package com.caucho.el;

import com.caucho.util.BeanUtil;
import com.caucho.vfs.WriteStream;

import javax.el.ELContext;
import javax.el.ELException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Represents an array reference:
 *
 * <pre>
 * a[b]
 * </pre>
 */
public class ArrayExpr extends Expr
{
  private Expr _left;
  private Expr _right;

  // cached getter method
  private transient Class _lastClass;
  private transient String _lastField;
  private transient Method _lastMethod;

  /**
   * Creates a new array expression.
   *
   * @param left the object expression
   * @param right the index expression.
   */
  public ArrayExpr(Expr left, Expr right)
  {
    _left = left;
    _right = right;
  }

  /**
   * Returns the base expression.
   */
  public Expr getExpr()
  {
    return _left;
  }

  /**
   * Returns the index expression.
   */
  public Expr getIndex()
  {
    return _right;
  }

  /**
   * Creates a method for constant arrays.
   */
  @Override
  public Expr createMethod(Expr []args)
  {
    if (! (_right instanceof StringLiteral))
      return null;

    StringLiteral literal = (StringLiteral) _right;

    return new MethodExpr(_left, literal.getValue(), args);
  }
  
  /**
   * Evaluates the expression as applicable to the provided context, and returns
   * the most general type that can be accepted by the setValue(javax.el.ELContext,
   * java.lang.Object) method.
   *
   * @param env
   * @return
   * @throws PropertyNotFoundException
   * @throws ELException
   */
  @Override
  public Class<?> getType(ELContext env)
  {
    Object aObj = _left.getValue(env);

    if (aObj == null)
      return null;

    Object fieldObj = _right.getValue(env);

    if (fieldObj == null)
      return null;

    return env.getELResolver().getType(env, aObj, fieldObj);
  }
  
  /**
   * Evaluate the expression as an object.
   *
   * @param env the variable environment
   *
   * @return the evaluated object
   */
  @Override
  public Object getValue(ELContext env)
    throws ELException
  {
    Object aObj = _left.getValue(env);

    if (aObj == null)
      return null;

    Object fieldObj = _right.getValue(env);
    if (fieldObj == null)
      return null;

    if (aObj instanceof Map) {
      return ((Map) aObj).get(fieldObj);
    }
    
    if (aObj instanceof List) {
      int ref = (int) toLong(fieldObj, null);

      try {
        List list = (List) aObj;

        if (ref < 0 || list.size() < ref)
          return null;
        else
          return list.get(ref);
      } catch (IndexOutOfBoundsException e) {
      } catch (Exception e) {
        return invocationError(e);
      }
    }

    Class aClass = aObj.getClass();
    
    if (aClass.isArray()) {
      int ref = (int) toLong(fieldObj, null);

      try {
        return Array.get(aObj, ref);
      } catch (IndexOutOfBoundsException e) {
      } catch (Exception e) {
        return error(e, env);
      }
    }

    String fieldName = toString(fieldObj, env);

    Method getMethod = null;
    try {
      synchronized (this) {
        if (_lastClass == aClass && _lastField.equals(fieldName))
          getMethod = _lastMethod;
        else {
          // XXX: the Introspection is a memory hog
          // BeanInfo info = Introspector.getBeanInfo(aClass);
          getMethod = BeanUtil.getGetMethod(aClass, fieldName);
          _lastClass = aClass;
          _lastField = fieldName;
          _lastMethod = getMethod;
        }
      }

      if (getMethod != null)
        return getMethod.invoke(aObj, (Object []) null);
    } catch (Exception e) {
      return invocationError(e);
    }

    try {
      getMethod = aClass.getMethod("get", new Class[] { String.class });

      if (getMethod != null)
        return getMethod.invoke(aObj, new Object[] {fieldName});
    } catch (NoSuchMethodException e) {
      return null;
    } catch (Exception e) {
      return invocationError(e);
    }

    try {
      getMethod = aClass.getMethod("get", new Class[] { Object.class });

      if (getMethod != null)
        return getMethod.invoke(aObj, new Object[] {fieldObj});
    } catch (Exception e) {
      return invocationError(e);
    }

    ELException e = new ELException(L.l("no get method {0} for class {1}",
                                        fieldName, aClass.getName()));

    error(e, env);
    
    return null;
  }

  /**
   * Prints the code to create an LongLiteral.
   *
   * @param os stream to the generated *.java code
   */
  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.el.ArrayExpr(");
    _left.printCreate(os);
    os.print(", ");
    _right.printCreate(os);
    os.print(")");
  }

  /**
   * Returns true for equal strings.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof ArrayExpr))
      return false;

    ArrayExpr expr = (ArrayExpr) o;

    return (_left.equals(expr._left) && _right.equals(expr._right));
  }

  /**
   * Returns a readable representation of the expr.
   */
  public String toString()
  {
    return _left + "[" + _right + "]";
  }
}
