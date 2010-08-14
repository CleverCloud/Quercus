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

package com.caucho.amber.field;

import com.caucho.amber.expr.AmberExpr;
import com.caucho.amber.expr.PathExpr;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.type.BeanType;
import com.caucho.amber.type.EntityType;
import com.caucho.bytecode.JType;
import com.caucho.bytecode.JTypeWrapper;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration for a bean's property
 */
abstract public class AbstractField implements AmberField {
  private static final L10N L = new L10N(AbstractField.class);
  private static final Logger log
    = Logger.getLogger(AbstractField.class.getName());

  final BeanType _sourceType;

  private String _name;

  private JType _type;

  private Method _getterMethod;
  private Method _setterMethod;

  private boolean _isLazy = true;
  private boolean _isOverride;

  private int _updateIndex;
  private int _loadGroupIndex = -1;

  AbstractField(BeanType sourceType)
  {
    _sourceType = sourceType;
  }

  AbstractField(BeanType sourceType, String name)
    throws ConfigException
  {
    this(sourceType);

    setName(name);
    
    if (log.isLoggable(Level.FINER))
      log.finer(_sourceType + " field " + this);
  }

  /**
   * Sets the name.
   */
  public void setName(String name)
    throws ConfigException
  {
    _name = name;

    ClassLoader loader
      = getSourceType().getPersistenceUnit().getTempClassLoader();
      
    if (! isFieldAccess()) {
      char ch = name.charAt(0);
      if (Character.isLowerCase(ch))
        name = Character.toUpperCase(ch) + name.substring(1);

      String getter = "get" + name;
      String setter = "set" + name;

      _getterMethod = BeanType.getGetter(getBeanClass(), getter);

      if (_getterMethod == null) {
        getter = "is" + name;
        _getterMethod = BeanType.getGetter(getBeanClass(), getter);
      }

      /* jpa/0u21
      if (_getterMethod == null)
        throw new ConfigException(L.l("{0}: {1} has no matching getter.",
                                      getBeanClass().getName(), name));
      */
    
      if (_getterMethod == null) {
        Field field = BeanType.getField(getBeanClass(), _name);

        if (field == null)
          throw new ConfigException(L.l("{0}: {1} has no matching field.",
                                        getBeanClass().getName(), _name));

        _type = JTypeWrapper.create(field.getGenericType(), loader);
      }
      else {
        _type = JTypeWrapper.create(_getterMethod.getGenericReturnType(),
                                    loader);

        _setterMethod = BeanType.getSetter(getBeanClass(), setter);
      }
    }
    else {
      Field field = BeanType.getField(getBeanClass(), name);

      if (field == null)
        throw new ConfigException(L.l("{0}: {1} has no matching field.",
                                      getBeanClass().getName(), name));

      _type = JTypeWrapper.create(field.getGenericType(), loader);
    }

    /*
      if (_setterMethod == null && ! isAbstract())
      throw new ConfigException(L.l("{0}: {1} has no matching setter.",
      getBeanClass().getName(), name));
    */
  }

  /**
   * Returns the field name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the java type.
   */
  protected void setJavaType(JType type)
  {
    _type = type;
  }

  /**
   * Returns the owning entity class.
   */
  public BeanType getSourceType()
  {
    return _sourceType;
  }

  /**
   * Returns the amber manager.
   */
  public AmberPersistenceUnit getPersistenceUnit()
  {
    return getSourceType().getPersistenceUnit();
  }

  /**
   * Returns the bean class.
   */
  public Class getBeanClass()
  {
    return getSourceType().getBeanClass();
  }

  /**
   * Returns the source type as
   * entity or mapped-superclass.
   */
  public EntityType getEntitySourceType()
  {
    return (EntityType) getSourceType();
  }

  /**
   * Returns the table containing the field's columns.
   */
  public AmberTable getTable()
  {
    return getEntitySourceType().getTable();
  }

  /**
   * Returns the column for the field
   */
  public AmberColumn getColumn()
  {
    return null;
  }

  /**
   * Returns the column for the field
   */
  public void setColumn(AmberColumn column)
  {
  }

  /**
   * Returns the property index.
   */
  public int getIndex()
  {
    return _updateIndex;
  }

  /**
   * Set the property index.
   */
  public void setIndex(int index)
  {
    _updateIndex = index;
  }

  /**
   * Returns the property's group index.
   */
  public int getLoadGroupIndex()
  {
    return _loadGroupIndex;
  }

  /**
   * Returns the property's group index.
   */
  protected void setLoadGroupIndex(int index)
  {
    _loadGroupIndex = index;
  }

  /**
   * Returns the load group mask.
   */
  public long getCreateLoadMask(int group)
  {
    int index = getLoadGroupIndex();

    if (64 * group <= index && index < 64 * (group + 1))
      return 1L << (index % 64);
    else
      return 0;
  }

  /**
   * Returns true for a lazy field.
   */
  public boolean isLazy()
  {
    return _isLazy;
  }

  /**
   * Set true for a lazy field.
   */
  public void setLazy(boolean isLazy)
  {
    _isLazy = isLazy;
  }

  /**
   * Returns true for an override
   */
  public boolean isOverride()
  {
    return _isOverride;
  }

  /**
   * Returns true for an override
   */
  public void setOverride(boolean isOverride)
  {
    _isOverride = isOverride;
  }

  /**
   * Returns true for a key
   */
  public boolean isKey()
  {
    return false;
  }

  /**
   * Returns the getter name.
   */
  public String getJavaTypeName()
  {
    return getJavaTypeName(getJavaClass());
  }

  /**
   * Returns the Java code for the type.
   */
  private String getJavaTypeName(Class cl)
  {
    if (cl.isArray())
      return getJavaTypeName(cl.getComponentType()) + "[]";
    else
      return cl.getName();
  }

  /**
   * Returns the field's type
   */
  public JType getJavaType()
  {
    return _type;
  }
  
  /**
   * Returns the field's class
   */
  public Class getJavaClass()
  {
    return getJavaType().getRawType().getJavaClass();
  }

  /**
   * Returns true if values are accessed by the fields.
   */
  public boolean isFieldAccess()
  {
    return getSourceType().isFieldAccess();
  }

  /**
   * Returns true if the methods are abstract.
   */
  public boolean isAbstract()
  {
    // jpa/0u21
    return (_getterMethod != null
            && Modifier.isAbstract(_getterMethod.getModifiers()));
  }

  /**
   * Returns true if the field is cascadable.
   */
  public boolean isCascadable()
  {
    return false;
  }

  /**
   * Returns true if the methods are abstract.
   */
  public boolean isUpdateable()
  {
    return true;
  }

  /**
   * Creates a copy of the field for a parent
   */
  public AmberField override(BeanType table)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Initialize the field.
   */
  public void init()
    throws ConfigException
  {
    if (_loadGroupIndex < 0) {
      if (_isLazy)
        _loadGroupIndex = getEntitySourceType().nextLoadGroupIndex();
      else
        _loadGroupIndex = getEntitySourceType().getDefaultLoadGroupIndex();
    }
  }

  /**
   * Generates the post constructor initialization.
   */
  public void generatePostConstructor(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates any prologue.
   */
  public void generatePrologue(JavaWriter out, HashSet<Object> completedSet)
    throws IOException
  {
    // CMP
    if (isAbstract()) {
      out.println();
      out.print("public ");
      out.print(getJavaTypeName());
      out.print(" " + getFieldName() + ";");
    }
  }

  //
  // getter/setter code generation
  //

  /**
   * Returns the getter method.
   */
  public Method getGetterMethod()
  {
    return _getterMethod;
  }

  /**
   * Returns the setter method.
   */
  public Method getSetterMethod()
  {
    return _setterMethod;
  }

  /**
   * Returns the getter name.
   */
  public String getGetterName()
  {
    if (isFieldAccess())
      return "__caucho_get_" + getName();
    else
      return _getterMethod.getName();
  }

  /**
   * Returns the setter name.
   */
  public String getSetterName()
  {
    if (isFieldAccess())
      return "__caucho_set_" + getName();
    else if (_setterMethod != null)
      return _setterMethod.getName();
    else
      return "set" + getGetterName().substring(3);
  }
  
  /**
   * Returns the actual data.
   */
  public String generateSuperGetter(String objThis)
  {
    if (! getSourceType().isEmbeddable())
      return objThis + ".__caucho_super_get_" + getName() + "()";
    else if (isFieldAccess())
      return objThis + "." + getName();
    else
      return objThis + "." + getGetterMethod().getName() + "()";
  }

  /**
   * Sets the actual data.
   */
  public String generateSuperSetter(String objThis, String value)
  {
    if (! getSourceType().isEmbeddable())
      return objThis + "." + "__caucho_super_set_" + getName() + "(" + value + ")";
    else if (isFieldAccess())
      return objThis + "." + getName() + " = " + value;
    else
      return objThis + "." + getSetterMethod().getName() + "(" + value + ")";
  }

  /**
   * Generates the field getter.
   *
   * @param value the non-null value
   */
  public String generateGet(String objThis)
  {
    if (objThis == null)
      return generateNull();

    if (objThis.equals("super"))
      return generateSuperGetter("this");
    else
      return objThis + "." + getGetterName() + "()";
    
    /*
    else if (! isAbstract())
      return obj + "." + _getterMethod.getName() + "()";
    else if (_getterMethod != null)
      return obj + "." + _getterMethod.getName() + "()";
    else
      return generateSuperGetter(obj);
    */
  }

  /**
   * Generates the field setter.
   *
   * @param value the non-null value
   */
  public String generateSet(String objThis, String value)
  {
    if (objThis.equals("super"))
      return generateSuperSetter("this", value);
    else
      return objThis + "." + getSetterName() + "(" + value + ")";
    /*
    else if (isFieldAccess()) {
      // jpa/0h09
      return obj + "." + getSetterName() + "(" + value + ")";
    }
    else if (_setterMethod != null)
      return obj + "." + _setterMethod.getName() + "(" + value + ")";
    else
      return obj +  ""; // ejb/0gb9
     */
  }

  /**
   * Generates the field getter.
   *
   * @param value the non-null value
   */
  public void generateGet(JavaWriter out, String objThis)
    throws IOException
  {
    out.print(generateGet(objThis));
  }

  /**
   * Generates set code, which goes through the active calls, i.e.
   * not a direct call to the underlying field.
   */
  public void generateSet(JavaWriter out, String obj, String value)
    throws IOException
  {
    out.println(generateSet(obj, value) + ";");
  }

   /**
   * Generates the super getter method implementation
   */
  public void generateSuperGetterMethod(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public final " + getJavaTypeName() + " __caucho_super_get_" + getName() + "()");
    out.println("{");
    out.pushDepth();

    if (isAbstract() || getGetterMethod() == null)
      out.println("return " + getFieldName() + ";");
    else if (this instanceof IdField)
      out.println("return " + getGetterName() + "();");
    else
      out.println("return super." + getGetterName() + "();");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the super setter method implementation
   */
  public void generateSuperSetterMethod(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public final void __caucho_super_set_" + getName() + "(" + getJavaTypeName() + " v)");
    out.println("{");
    out.pushDepth();

    if (isAbstract() || getGetterMethod() == null)
      out.println(getFieldName() + " = v;");
    else if (getSetterMethod() == null) {
    }
    else if (this instanceof IdField)
      out.println(getSetterMethod().getName() + "(v);");
    else
      out.println("super." + getSetterMethod().getName() + "(v);");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the getter method implementation.
   */
  public void generateGetterMethod(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the setter method implementation.
   */
  public void generateSetterMethod(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the detachment code
   */
  public void generateDetach(JavaWriter out)
    throws IOException
  {
  }

  //
  // SQL generation
  //
  
  /**
   * Generates the select clause for an entity load.
   */
  public String generateLoadSelect(AmberTable table, String id)
  {
    return null;
  }

  /**
   * Generates the select clause.
   */
  public String generateSelect(String id)
  {
    return null;
  }

  /**
   * Generates the JPA QL select clause.
   */
  public String generateJavaSelect(String id)
  {
    return null;
  }

  /**
   * Generates the where clause.
   */
  public String generateWhere(String id)
  {
    return null;
  }

  /**
   * Generates the where clause.
   */
  public void generateUpdate(CharBuffer sql)
  {
  }

  /**
   * Generates loading cache
   */
  public void generateUpdate(JavaWriter out, String maskVar, String pstmt,
                             String index)
    throws IOException
  {
    int group = getIndex() / 64;
    long mask = 1L << getIndex() % 64;

    out.println();
    out.println("if ((" + maskVar + "_" + group + " & " + mask + "L) != 0) {");
    out.pushDepth();

    generateStatementSet(out, pstmt, index);

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates loading code
   */
  public boolean hasLoadGroup(int index)
  {
    return index == _loadGroupIndex;
  }

  /**
   * Generates loading code
   */
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
    return index;
  }

  /**
   * Generates loading code after the basic fields.
   */
  public int generatePostLoadSelect(JavaWriter out, int index)
    throws IOException
  {
    return index;
  }

  /**
   * Generates loading cache
   */
  public void generateLoadFromObject(JavaWriter out, String obj)
    throws IOException
  {
    if (getGetterMethod() == null || getSetterMethod() == null)
      return;

    String getter = getGetterName();

    String loadVar = "__caucho_loadMask_" + (getLoadGroupIndex() / 64);
    long loadMask = (1L << getLoadGroupIndex());

    out.println("if ((" + loadVar + " & " + loadMask + "L) != 0)");
    out.print("  ");

    out.println("  " + generateSuperSetter("this", generateGet(obj)) + ";");
  }

  /**
   * Generates loading for a native query
   */
  public int generateLoadNative(JavaWriter out, int index)
    throws IOException
  {
    return index;
  }

  /**
   * Generates loading for a native query
   */
  public void generateNativeColumnNames(ArrayList<String> names)
  {
  }

  /**
   * Generates loading cache
   */
  public void generateUpdateFromObject(JavaWriter out, String obj)
    throws IOException
  {
    out.println(generateSuperSetter("this", generateGet(obj)) + ";");
  }

  /**
   * Returns the null value.
   */
  public String generateNull()
  {
    return "null";
  }

  /**
   * Returns the field name.
   */
  protected String getFieldName()
  {
    return getName();
  }

  /**
   * Generates the insert.
   */
  public final String generateInsert()
  {
    return null;
  }

  /**
   * Generates the insert.
   */
  public void generateInsertColumns(ArrayList<String> columns)
  {
  }

  /**
   * Generates the table create.
   */
  public String generateCreateTableSQL(AmberPersistenceUnit manager)
  {
    return null;
  }

  /**
   * Generates the set clause.
   */
  public void generateStatementSet(JavaWriter out, String pstmt, String index)
    throws IOException
  {
    generateStatementSet(out, pstmt, index, "super");
  }

  /**
   * Generates the set clause for the insert clause.
   */
  public void generateInsertSet(JavaWriter out, String pstmt,
                                String index, String obj)
    throws IOException
  {
    generateStatementSet(out, pstmt, index, obj);
  }

  /**
   * Generates the set clause for the insert clause.
   */
  public void generateUpdateSet(JavaWriter out, String pstmt,
                                String index, String obj)
    throws IOException
  {
    generateStatementSet(out, pstmt, index, obj);
  }

  /**
   * Generates any code needed before a persist occurs
   */
  public void generatePrePersist(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Updates the cached copy.
   */
  public void generateCopyUpdateObject(JavaWriter out,
                                       String dst, String src,
                                       int updateIndex)
    throws IOException
  {
    // commented out: jpa/0l03

    if (getIndex() == updateIndex) {
      String value = generateGet(src);
      out.println(generateSet(dst, value) + ";");
    }
  }

  /**
   * Updates the cached copy.
   */
  public void generateCopyLoadObject(JavaWriter out,
                                     String dst, String src,
                                     int loadIndex)
    throws IOException
  {
    // jpa/0g0l
    if (getLoadGroupIndex() != loadIndex)
      return;

    String value = generateGet(src);

    // jpa/0l43 out.println(generateStatementSet(dst, value) + ";");

    boolean isJPA = getEntitySourceType().getPersistenceUnit().isJPA();

    if (isJPA
        && ! (dst.equals("cacheEntity")
              || dst.equals("super")
              || dst.equals("item"))) {
      // jpa/0j5fn: merge()
      out.println("if (isFullMerge)");
      out.println("  " + generateSet(dst, value) + ";");
      out.println("else");
      out.print("  ");
    }

    if (! dst.equals("super"))
      out.println(generateSuperSetter(dst, value) + ";");
    else
      out.println(generateSuperSetter("this", value) + ";");
  }

  /**
   * Updates the cached copy.
   */
  public void generateMergeFrom(JavaWriter out,
                                String dst, String src)
    throws IOException
  {
    // jpa/0g0l
    //if (getLoadGroupIndex() != loadIndex)
    //  return;

    String value = generateGet(src);

    // jpa/0l43
    out.println(generateSet(dst, value) + ";");
  }

  /**
   * Checks entity-relationships from an object.
   */
  public void generateDumpRelationships(JavaWriter out,
                                        int updateIndex)
    throws IOException
  {
  }

  /**
   * Generates the set clause.
   */
  public void generateStatementSet(JavaWriter out, String pstmt,
                          String index, String obj)
    throws IOException
  {
  }

  /**
   * Converts to an object.
   */
  public String toObject(String value)
  {
    return value;
  }

  /**
   * Links to the target.
   */
  public void link()
  {
  }

  /**
   * Generates the pre-delete code
   */
  public void generatePreDelete(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the delete foreign
   */
  public void generatePostDelete(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the expire code
   */
  public void generateExpire(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates code for foreign entity create/delete
   */
  public void generateInvalidateForeign(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Deletes the children
   */
  public void childDelete(AmberConnection aConn, Serializable primaryKey)
    throws SQLException
  {
  }

  /**
   * Generates code to convert to the type from the object.
   */
  public String generateCastFromObject(String value)
  {
    return value;
  }

  /**
   * Generates code to test the equals.
   */
  public String generateEquals(String leftBase, String value)
  {
    return leftBase + ".equals(" + value + ")";
  }

  /**
   * Creates the expression for the field.
   */
  public AmberExpr createExpr(QueryParser parser, PathExpr parent)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "," + getSourceType() + "]";
  }
}
