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
 * @author Rodrigo Westrupp
 */

package com.caucho.quercus.lib.db;

import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.Array;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Quercus Oracle OCI-Collection object oriented API.
 */
public class OracleOciCollection {
  private static final Logger log = Logger.getLogger(
      OracleOciCollection.class.getName());
  private static final L10N L = new L10N(OracleOciCollection.class);

  // The Oracle array descriptor
  private Object _arrayDescriptor;

  // The Oracle collection
  private Array _collection;

  // The cached Java collection
  private ArrayList _javaCollection;

  // The Oracle JDBC connection
  private Connection _jdbcConn;

  // Cache class oracle.sql.ARRAY
  private static Class classOracleARRAY;

  static {
    try {
      classOracleARRAY = Class.forName("oracle.sql.ARRAY");
    } catch (Exception e) {
      L.l("Unable to load ARRAY class oracle.sql.ARRAY.");
    }
  }

  /**
   * Constructor for OracleOciCollection
   */
  OracleOciCollection(Connection jdbcConn,
                      Object arrayDescriptor)
  {
    _jdbcConn = jdbcConn;

    _arrayDescriptor = arrayDescriptor;

    _collection = null;

    _javaCollection = new ArrayList();
  }

  /**
   * Appends element to the collection
   *
   * @param value can be a string or a number
   */
  public boolean append(Env env,
                        Value value)
  {
    try {

      _javaCollection.add(value.toJavaObject());

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Assigns a value to the collection from another existing collection
   */
  public boolean assign(Env env,
                        OracleOciCollection fromCollection)
  {
    try {

      _javaCollection.addAll(fromCollection.getJavaCollection());

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Assigns a value to the element of the collection
   *
   * @param index 1-based index
   * @param value can be a string or a number
   */
  public boolean assignElem(Env env,
                            int index,
                            Value value)
  {
    try {

      if ((index < 1) || (index > _javaCollection.size())) {
        return false;
      }

      _javaCollection.set(index - 1, value.toJavaObject());

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Frees the resources associated with the collection object
   */
  public boolean free(Env env)
  {
    try {

      _collection = null;

      _javaCollection = null;

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Returns the underlying Oracle collection
   */
  protected Array getCollection()
  {
    try {

      // oracle.sql.ARRAY array = new oracle.sql.ARRAY(
      // arrayDesc, jdbcConn, arrayValues);

      Class clArrayDescriptor = Class.forName("oracle.sql.ArrayDescriptor");

      Constructor constructor = classOracleARRAY.getDeclaredConstructor(
          new Class[]
        {clArrayDescriptor, Connection.class, Object.class});

      Object []elements = _javaCollection.toArray();

      _collection = (Array) constructor.newInstance(new Object[]
        {_arrayDescriptor, _jdbcConn, elements});

      if (_collection != null) {
        // Optimization
        Method setAutoBuffering
          = classOracleARRAY.getDeclaredMethod("setAutoBuffering",
                                               new Class[] {Boolean.TYPE});
        setAutoBuffering.invoke(_collection, new Object[] {true});
      }

      return _collection;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Returns value of the element by index (1-based)
   */
  public Value getElem(Env env,
                       int index)
  {
    try {

      if ((index < 1) || (index > _javaCollection.size())) {
        return BooleanValue.FALSE;
      }

      return env.wrapJava(_javaCollection.get(index - 1));

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the underlying Java collection
   */
  protected ArrayList getJavaCollection()
  {
    return _javaCollection;
  }

  /**
   * Returns the maximum number of elements in the collection
   * If the returned value is 0, then the number of elements
   * is not limited. Returns FALSE in case of error.
   */
  @ReturnNullAsFalse
  public LongValue max(Env env)
  {
    try {

      return LongValue.create(0);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Returns size of the collection
   */
  @ReturnNullAsFalse
  public LongValue size(Env env)
  {
    try {

      return LongValue.create(_javaCollection.size());

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Trims num elements from the end of the collection
   */
  public boolean trim(Env env,
                      int num)
  {
    try {

      if (num < 0) {
        return false;
      }

      if (num == 0) {
        return true;
      }

      int length = _javaCollection.size();

      if (num > length) {
        num = length;
      }

      int i = length - num;

      _javaCollection.subList(i, length).clear();

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  public String toString() {
    return "OracleOciCollection()";
  }
}
