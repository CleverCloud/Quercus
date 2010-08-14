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
import com.caucho.amber.expr.ManyToOneExpr;
import com.caucho.amber.expr.OneToManyExpr;
import com.caucho.amber.expr.PathExpr;
import com.caucho.amber.expr.ElementCollectionExpr;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.AmberType;
import com.caucho.amber.type.ElementType;
import com.caucho.amber.type.EntityType;
import com.caucho.bytecode.JType;
import com.caucho.bytecode.JTypeWrapper;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.persistence.CascadeType;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Configuration for a bean's field
 */
public class ElementCollectionField extends AssociationField {
  private static final L10N L = new L10N(ElementCollectionField.class);
  private static final Logger log
    = Logger.getLogger(ElementCollectionField.class.getName());

  private String _mapKey;

  private AmberType _targetType;

  private AmberTable _associationTable;

  private LinkColumns _sourceLink;

  private ArrayList<String> _orderByFields;
  private ArrayList<Boolean> _orderByAscending;

  private ElementType _elementType;

  public ElementCollectionField(EntityType sourceType,
                                String name)
    throws ConfigException
  {
    super(sourceType, name, null);

    _elementType = new ElementType(sourceType.getPersistenceUnit(), this);
  }

  /**
   * Gets the map key.
   */
  public String getMapKey()
  {
    return _mapKey;
  }

  /**
   * Sets the map key.
   */
  public void setMapKey(String mapKey)
  {
    _mapKey = mapKey;
  }

  /**
   * Sets the target type.
   */
  @Override
  public void setType(AmberType targetType)
  {
    _targetType = targetType;

    super.setType(targetType);
  }

  /**
   * Returns the source type as
   * entity or mapped-superclass.
   */
  public EntityType getRelatedType()
  {
    return (EntityType) getSourceType();
  }

  /**
   * Returns the type argument for the target
   */
  public AmberType getTargetType()
  {
    return _targetType;
  }

  /**
   * Returns the association table
   */
  public AmberTable getAssociationTable()
  {
    return _associationTable;
  }

  /**
   * Sets the association table
   */
  public void setAssociationTable(AmberTable table)
  {
    _associationTable = table;
  }

  /**
   * Adds a column from the association table to the source side.
   */
  public void setSourceLink(LinkColumns link)
  {
    _sourceLink = link;
  }

  /**
   * Returns the source link.
   */
  public LinkColumns getSourceLink()
  {
    return _sourceLink;
  }

  /**
   * Sets the order by.
   */
  public void setOrderBy(ArrayList<String> orderByFields,
                         ArrayList<Boolean> orderByAscending)
  {
    _orderByFields = orderByFields;
    _orderByAscending = orderByAscending;
  }

  /**
   * Initializes the field.
   */
  @Override
  public void init()
    throws ConfigException
  {
  }

  /**
   * Generates the set clause.
   */
  @Override
  public void generateStatementSet(JavaWriter out, String pstmt,
                                   String obj, String index)
    throws IOException
  {
  }

  /**
   * Generates the select clause.
   */
  @Override
  public String generateLoadSelect(String id)
  {
    return id + "." + getName();
  }

  /**
   * Generates loading code after the basic fields.
   */
  @Override
  public int generatePostLoadSelect(JavaWriter out, int index)
    throws IOException
  {
    if (! isLazy()) {
      // jpa/1a02: the size is to force the actual load
      out.println(getGetterName() + "().size();");
    }

    return ++index;
  }

  /**
   * Creates the expression for the field.
   */
  @Override
  public AmberExpr createExpr(QueryParser parser, PathExpr parent)
  {
    return new ElementCollectionExpr(parser, parent, _sourceLink,
                                     _elementType);
  }

  /**
   * Updates from the cached copy.
   */
  @Override
  public void generateCopyLoadObject(JavaWriter out,
                                     String dst, String src,
                                     int loadIndex)
    throws IOException
  {
    // jpa/0s2j
    if (dst.equals("item"))
      return;

    String var = "_caucho_field_" + getGetterName();

    // order matters: jpa/0s2k
    String value = var; // generateGet(src);
    out.println(generateSet(dst, value) + ";");

    out.println(generateAccessor(dst, var)
                + " = " + generateAccessor(src, var) + ";");

    if (! dst.equals("super")) { // || isLazy())) {
      String oThis = "((" + getRelatedType().getInstanceClassName() + ") " + dst + ")";
      out.println(generateSuperSetter(oThis, generateSuperGetter("this")) + ";");
    }
  }

  /**
   * Updates the cached copy.
   */
  @Override
  public void generateMergeFrom(JavaWriter out,
                                      String dst, String src)
    throws IOException
  {
    // jpa/0s2k
    int updateIndex = 0;
    generateCopyLoadObject(out, dst, src, updateIndex);

    out.println();
  }

  /**
   * Generates the set property.
   */
  @Override
  public void generateGetterMethod(JavaWriter out)
    throws IOException
  {
    String var = "_caucho_field_" + getGetterName();

    boolean isSet = getJavaType().isAssignableTo(Set.class);
    boolean isMap = false;
    if (!isSet) {
      isMap = getJavaType().isAssignableTo(Map.class);
    }

    JType type = getJavaType();
    JType []paramArgs = type.getActualTypeArguments();
    JType param = paramArgs.length > 0 ? paramArgs[0] : null;
    JType param2 = paramArgs.length > 1 ? paramArgs[1] : null;

    // jpa/0l44
    out.print("protected ");

    String collectionImpl;

    if (isSet)
      collectionImpl = "com.caucho.amber.collection.SetImpl";
    else if (isMap)
      collectionImpl = "com.caucho.amber.collection.MapImpl";
    else
      collectionImpl = "com.caucho.amber.collection.CollectionImpl";

    out.print(collectionImpl);

    if (param != null) {
      out.print('<' + param.getPrintName());
      
      if (isMap) {
        if (param2 != null) {
          out.print(", " + param2.getPrintName());
        }
      }

      out.print(">");
    }

    out.print(" " + var + ";");

    out.println();
    out.println("public " + getJavaTypeName() + " " + getGetterName() + "()");
    out.println("{");
    out.pushDepth();

    out.println("if (" + var + " != null) {");
    out.pushDepth();

    out.println("if (__caucho_state.isPersist()) {");
    out.pushDepth();

    out.println(var + ".setSession(__caucho_session);");

    out.println("return " + var + ";");

    out.popDepth();
    out.println("}");
    out.println();

    // jpa/1622
    out.println("if (" + var + ".getSession() != null");
    out.println("    && " + var + ".getSession() == __caucho_session)");
    out.println("  return " + var + ";");

    out.popDepth();
    out.println("}");

    out.println();
    out.println("com.caucho.amber.AmberQuery query = null;");

    out.println();
    out.println("try {");
    out.pushDepth();

    out.println("if (__caucho_session == null) {");
    out.pushDepth();

    String newEmptyCollection = "new " + collectionImpl;

    if (param != null) {
      newEmptyCollection += "<" + param.getPrintName();
      if (isMap) {
        newEmptyCollection += ", ";
        newEmptyCollection += param2.getPrintName();
      }
      newEmptyCollection += ">";
    }

    newEmptyCollection += "(query";
    if (isMap) {
      newEmptyCollection += "," + getTargetType().getJavaTypeName();
      newEmptyCollection += ".class.getDeclaredMethod(\"get";
      String getterMapKey = getMapKey();
      getterMapKey = Character.toUpperCase(getterMapKey.charAt(0)) + getterMapKey.substring(1);
      newEmptyCollection += getterMapKey; // "getId");
      newEmptyCollection += "\", (Class []) null)";
    }
    newEmptyCollection += ")";

    // jpa/0s2k, jpa/1622
    out.println("if (" + var + " == null)");
    out.println("  " + var + " = " + newEmptyCollection + ";");

    // if (! isAbstract())
    out.println();
    out.println("return " + var + ";");

    out.popDepth();
    out.println("}");

    out.println();
    out.print("String sql=\"");

    out.print("SELECT c");
    out.print(" FROM " + getSourceType().getName() + " o,");
    out.print("  IN(o." + getName() + ") c");
    out.print(" WHERE ");
    out.print(getRelatedType().getId().generateRawWhere("o"));

    if (_orderByFields != null) {
      out.print(" ORDER BY ");

      for (int i = 0; i < _orderByFields.size(); i++) {
        if (i != 0)
          out.print(", ");

        out.print("c." + _orderByFields.get(i));
        if (Boolean.FALSE.equals(_orderByAscending.get(i)))
          out.print(" DESC");
      }
    }

    out.println("\";");
    out.println("query = __caucho_session.prepareQuery(sql);");

    out.println("int index = 1;");
    getRelatedType().getId().generateSet(out, "query", "index", "this");

    // Ex: _caucho_getChildren = new com.caucho.amber.collection.CollectionImpl
    out.print(var);
    out.print(" = " + newEmptyCollection + ";");

    /*
      out.pushDepth();

      generateAdd(out);
      generateRemove(out);
      generateClear(out);
      // generateSize(out);

      out.popDepth();
      out.println("};");
    */

    out.println();

    // jpa/0i5g
    /*
    out.print(var + "_added = ");
    out.println("new java.util.HashSet<" + getTargetType().getJavaTypeName() + ">();");

    if (isMap)
      out.print(var + "_added.addAll(" + var + ".values());");
    else
      out.println(var + "_added.addAll(" + var + ");");
    */

    out.println();
    out.println("return " + var + ";");

    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  throw com.caucho.amber.AmberRuntimeException.create(e);");
    out.println("}");

    out.popDepth();
    out.println("}");

    generateAmberAdd(out);
    generateAmberRemove(out);
    generateAmberRemoveTargetAll(out);
  }


  /**
   * Generates the set property.
   */
  private void generateAdd(JavaWriter out)
    throws IOException
  {
    JType type = getJavaType();
    JType []paramArgs = type.getActualTypeArguments();
    String gType = paramArgs.length > 0 ? paramArgs[0].getPrintName() : "Object";

    out.println("public boolean add(" + gType + " o)");
    out.println("{");
    out.pushDepth();

    String ownerType = getRelatedType().getInstanceClassName();

    out.println("if (! (o instanceof " + ownerType + "))");
    out.println("  throw new java.lang.IllegalArgumentException((o == null ? \"null\" : o.getClass().getName()) + \" must be a " + ownerType + "\");");

    out.println(ownerType + " bean = (" + ownerType + ") o;");

    // XXX: makePersistent

    /*
      ArrayList<Column> keyColumns = getKeyColumns();
      for (int i = 0; i < keyColumns.size(); i++) {
      Column column = keyColumns.get(i);
      AbstractProperty prop = column.getProperty();


      if (prop != null) {
      out.println("bean." + prop.getSetterName() + "(" + ownerType + "__ResinExt.this);");
      }
      }
    */

    out.println("return true;");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the set property.
   */
  private void generateRemove(JavaWriter out)
    throws IOException
  {
    JType type = getJavaType();
    JType []paramArgs = type.getActualTypeArguments();
    String gType = paramArgs.length > 0 ? paramArgs[0].getPrintName() : "Object";

    out.println("public boolean remove(" + gType + " o)");
    out.println("{");
    out.pushDepth();

    String ownerType = getSourceType().getInstanceClassName();

    out.println("if (! (o instanceof " + ownerType + "))");
    out.println("  throw new java.lang.IllegalArgumentException((o == null ? \"null\" : o.getClass().getName()) + \" must be a " + ownerType + "\");");

    out.println(ownerType + " bean = (" + ownerType + ") o;");

    // XXX: makePersistent

    /*
      ArrayList<Column> keyColumns = getKeyColumns();
      for (int i = 0; i < keyColumns.size(); i++) {
      Column column = keyColumns.get(i);
      AbstractProperty prop = column.getProperty();

      if (prop != null) {
      out.println("bean." + prop.getSetterName() + "(null);");
      }
      }
    */

    out.println("return true;");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the clear method.
   */
  private void generateClear(JavaWriter out)
    throws IOException
  {
    out.println("public void clear()");
    out.println("{");
    out.pushDepth();

    out.println("if (__caucho_session != null) {");
    out.pushDepth();

    out.println("try {");
    out.pushDepth();

    out.println("__caucho_session.flushNoChecks();");

    out.print("String sql=\"");

    out.print("UPDATE ");
    out.print(getSourceType().getName());
    out.print(" SET ");
    /*
      ArrayList<Column> columns = getKeyColumns();
      for (int i = 0; i < columns.size(); i++) {
      if (i != 0)
      out.print(", ");

      out.print(columns.get(i).getName());
      out.print("=null");
      }
    */

    out.print(" WHERE ");

    /*
      for (int i = 0; i < columns.size(); i++) {
      if (i != 0)
      out.print(" and ");

      out.print(columns.get(i).getName());
      out.print("=?");
      }
    */

    out.println("\";");
    out.println("com.caucho.amber.AmberQuery query;");
    out.println("query = __caucho_session.prepareQuery(sql);");

    String ownerType = getSourceType().getInstanceClassName();

    out.println("int index = 1;");
    getRelatedType().getId().generateSet(out, "query", "index", ownerType + ".this");

    out.println("query.executeUpdate();");

    out.println("super.clear();");

    out.popDepth();
    out.println("} catch (java.sql.SQLException e) {");
    out.println("  throw com.caucho.amber.AmberRuntimeException.create(e);");
    out.println("}");

    out.popDepth();
    out.println("} else {");
    out.println("  super.clear();");
    out.println("}");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the size method.
   */
  private void generateSize(JavaWriter out)
    throws IOException
  {
    out.println("public int size()");
    out.println("{");
    out.pushDepth();

    out.println("if (__caucho_session == null || isValid())");
    out.println("  return super.size();");

    out.println("try {");
    out.pushDepth();

    out.println("__caucho_session.flushNoChecks();");

    out.print("String sql=\"");

    out.print("SELECT count(*) FROM ");
    out.print(getSourceType().getName());
    out.print(" AS o ");

    out.print(" WHERE ");

    /*
      ArrayList<Column> columns = getKeyColumns();
      for (int i = 0; i < columns.size(); i++) {
      if (i != 0)
      out.print(" and ");

      out.print("o." + columns.get(i).getName());
      out.print("=?");
      }
    */

    out.println("\";");
    out.println("com.caucho.amber.AmberQuery query;");
    out.println("query = __caucho_session.prepareQuery(sql);");

    out.println("int index = 1;");

    // ejb/06h0
    getRelatedType().getId().generateSet(out, "query", getSourceType().getInstanceClassName() + ".this", "index"); // "__ResinExt.this", "index");

    out.println("java.sql.ResultSet rs = query.executeQuery();");

    out.println("if (rs.next())");
    out.println("  return rs.getInt(1);");
    out.println("else");
    out.println("  return 0;");

    out.popDepth();
    out.println("} catch (java.sql.SQLException e) {");
    out.println("  throw com.caucho.amber.AmberRuntimeException.create(e);");
    out.println("}");

    out.popDepth();
    out.println("}");
  }


  /**
   * Generates the (post) cascade operation from
   * parent to this child. This field will only
   * be cascaded first if the operation can be
   * performed with no risk to break FK constraints.
   */
  @Override
  public void generatePostCascade(JavaWriter out,
                                  String aConn,
                                  CascadeType cascadeType)
    throws IOException
  {
    if (cascadeType != CascadeType.PERSIST
        && cascadeType != CascadeType.REMOVE)
      return;

    if (isCascade(cascadeType)) {
      out.println("if (__caucho_state.ordinal() <= com.caucho.amber.entity.EntityState.P_TRANSACTIONAL.ordinal()) {");
      out.pushDepth();

      String amberCascade = "__amber_" + getGetterName();

      if (cascadeType == CascadeType.PERSIST)
        amberCascade += "_add";
      else
        amberCascade += "_remove";

      String getter = "_caucho_field_" + getGetterName(); // generateSuperGetterMethod();

      out.println("if (" + getter + " != null) {");
      out.pushDepth();

      if (cascadeType == CascadeType.PERSIST) {
        // XXX: jpa/0i5c
        // For now, needs to flush the persist() with many-to-many
        // to avoid breaking FK constraints from join tables.
        out.println("if (__caucho_state == com.caucho.amber.entity.EntityState.P_PERSISTING)");
        out.println("  __caucho_create(__caucho_session, __caucho_home);");
      }

      out.println();
      out.println("for (Object o : " + getter + ") {");
      out.pushDepth();

      if (cascadeType == CascadeType.PERSIST) {
        // jpa/0i60
        out.println("((com.caucho.amber.entity.Entity) o).__caucho_flush();");

        // jpa/1622
        out.println(amberCascade + "(aConn, o);");
      }
      else
        out.println(amberCascade + "(o);");

      out.popDepth();
      out.println("}");

      out.popDepth();
      out.println("}");

      out.popDepth();
      out.println("}");
    }
  }

  /**
   * Generates the set property.
   */
  public void generateAmberAdd(JavaWriter out)
    throws IOException
  {
    // commented out: jpa/0s2d
    // String targetType = getTargetType().getProxyClass().getName();

    String targetType = getTargetType().getJavaTypeName();

    out.println();
    out.println("public boolean" +
                " __amber_" + getGetterName() + "_add(com.caucho.amber.manager.AmberConnection aConn, Object o)");
    out.println("{");
    out.pushDepth();

    out.println("if (! (o instanceof " + targetType + "))");
    out.println("  return false;");

    out.println();
    out.println(targetType + " v = (" + targetType + ") o;");

    // jpa/0i5g
    /*
    String varAdded = "_caucho_field_" + getGetterName() + "_added";
    out.println();
    out.println("if (" + varAdded + " == null)");
    out.println("  " + varAdded + " = new java.util.HashSet<" + getTargetType().getJavaTypeName() + ">();");
    out.println("else if (" + varAdded + ".contains(v))");
    out.println("  return false;");
    out.println();
    out.println(varAdded + ".add(v);");
    */

    out.println();
    out.println("if (aConn == null)");
    out.println("  return false;");

    out.println();
    out.print("String sql = \"INSERT INTO ");
    out.print(_associationTable.getName() + " (");

    out.print(_sourceLink.generateSelectSQL(null));

    /*
    out.print(", ");

    out.print(_targetLink.generateSelectSQL(null));
    */

    out.print(") VALUES (");

    int count = (getRelatedType().getId().getKeyCount());

    for (int i = 0; i < count; i++) {
      if (i != 0)
        out.print(", ");

      out.print("?");
    }
    out.println(")\";");

    out.println();
    out.println("try {");
    out.pushDepth();

    out.println("java.sql.PreparedStatement pstmt = aConn.prepareInsertStatement(sql, false);");

    out.println("int index = 1;");
    getRelatedType().getId().generateSet(out, "pstmt", "index", "this");
    //getTargetType().getId().generateSet(out, "pstmt", "index", "v");

    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  __caucho_log.log(java.util.logging.Level.FINE, e.toString(), e);");
    out.println("}");

    out.println("return false;");
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the remove property.
   */
  public void generateAmberRemove(JavaWriter out)
    throws IOException
  {
    // commented out: jpa/0s2d
    // String targetType = getTargetType().getProxyClass().getName();

    String targetType = getTargetType().getJavaTypeName();

    out.println();
    out.println("public boolean" +
                " __amber_" + getGetterName() + "_remove(Object o)");
    out.println("{");
    out.pushDepth();

    out.println("if (! (o instanceof " + targetType + "))");
    out.println("  return false;");

    out.println();
    out.println(targetType + " v = (" + targetType + ") o;");
    out.println();
    out.println("if (__caucho_session == null)");
    out.println("  return false;");

    out.println();
    out.print("String sql = \"DELETE FROM ");
    out.print(_associationTable.getName() + " WHERE ");

    out.print(_sourceLink.generateMatchArgSQL(null));

    out.println("\";");

    out.println();
    out.println("try {");
    out.pushDepth();

    out.println("java.sql.PreparedStatement pstmt = __caucho_session.prepareStatement(sql);");

    out.println("int index = 1;");
    getRelatedType().getId().generateSet(out, "pstmt", "index", "this");
    // getTargetType().getId().generateSet(out, "pstmt", "index", "v");

    /*
    out.println("if (pstmt.executeUpdate() == 1) {");
    out.pushDepth();
    out.println("__caucho_session.addCompletion(new com.caucho.amber.entity.TableInvalidateCompletion(\"" + _targetLink.getSourceTable().getName() + "\"));");
    out.println("return true;");
    out.popDepth();
    out.println("}");
    */

    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  __caucho_log.log(java.util.logging.Level.FINE, e.toString(), e);");
    out.println("}");

    out.println("return false;");
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the remove property.
   */
  public void generateAmberRemoveTargetAll(JavaWriter out)
    throws IOException
  {
    // commented out: jpa/0s2d
    // String targetType = getTargetType().getProxyClass().getName();

    String targetType = getTargetType().getJavaTypeName();

    out.println();
    out.println("public boolean" +
                " __amber_" + getGetterName() + "_remove_target(Object o)");
    out.println("{");
    out.pushDepth();

    out.println("if (! (o instanceof " + targetType + "))");
    out.println("  return false;");

    out.println();
    out.println(targetType + " v = (" + targetType + ") o;");
    out.println();
    out.println("if (__caucho_session == null)");
    out.println("  return false;");

    out.println();
    out.print("String sql = \"DELETE FROM ");
    out.print(_associationTable.getName() + " WHERE ");

    // XXX: out.print(_targetLink.generateMatchArgSQL(null));

    out.println("\";");

    out.println();
    out.println("try {");
    out.pushDepth();

    out.println("java.sql.PreparedStatement pstmt = __caucho_session.prepareStatement(sql);");

    out.println("int index = 1;");
    // getTargetType().getId().generateSet(out, "pstmt", "index", "v");

    out.println("if (pstmt.executeUpdate() == 1)");
    out.println("  return true;");

    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  __caucho_log.log(java.util.logging.Level.FINE, e.toString(), e);");
    out.println("}");

    out.println("return false;");
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the set property.
   */
  @Override
  public void generateSetterMethod(JavaWriter out)
    throws IOException
  {
    // commented out: jpa/0s2i
    // JMethod setter = getSetterMethod();
    //
    // if (setter == null)
    //   return;
    //
    // JType type = getGetterMethod().getGenericReturnType();

    JType type;

    ClassLoader loader
      = getSourceType().getPersistenceUnit().getTempClassLoader();
    
    if (! getSourceType().isFieldAccess()) {
      type = JTypeWrapper.create(getGetterMethod().getGenericReturnType(),
                                 loader);
    }
    else {
      Field field = EntityType.getField(getBeanClass(), getName());
      
      type = JTypeWrapper.create(field.getGenericType(),
                                 loader);
    }

    out.println();
    // commented out: jpa/0s2i
    // out.print("public void " + setter.getName() + "(");
    out.print("public void " + getSetterName() + "(");
    out.println(type.getPrintName() + " value)");
    out.println("{");
    out.pushDepth();

    out.println("if (" + generateSuperGetter("this") + " == value)");
    out.println("  return;");
    out.println();

    //
    // jpa/0s2j needs to generate the following snippet:
    //
    // _caucho___caucho_get_xAnnualReviews
    //   = new com.caucho.amber.collection.CollectionImpl<qa.XAnnualReview>(__caucho_session, null);
    // _caucho___caucho_get_xAnnualReviews.addAll(0, value);
    //
    //
    // jpa/0s2j:

    out.println("try {");
    out.pushDepth();

    String var = "_caucho_field_" + getGetterName();

    out.print(var + " = new ");

    type = getJavaType();

    boolean isSet = type.isAssignableTo(Set.class);
    boolean isMap = false;
    if (!isSet) {
      isMap = type.isAssignableTo(Map.class);
    }

    JType []paramArgs = type.getActualTypeArguments();
    JType param = paramArgs.length > 0 ? paramArgs[0] : null;
    JType param2 = paramArgs.length > 1 ? paramArgs[1] : null;

    String collectionImpl;

    if (isSet)
      collectionImpl = "com.caucho.amber.collection.SetImpl";
    else if (isMap)
      collectionImpl = "com.caucho.amber.collection.MapImpl";
    else
      collectionImpl = "com.caucho.amber.collection.CollectionImpl";

    out.print(collectionImpl);

    if (param != null) {
      out.print("<");
      out.print(param.getPrintName());
      if (isMap) {
        if (param2 != null) {
          out.print(", ");
          out.print(param2.getPrintName());
        }
      }
      out.print(">");
    }

    out.print("(__caucho_session, null");
    if (isMap) {
      out.print(", ");
      out.print(getTargetType().getJavaTypeName());
      out.print(".class.getDeclaredMethod(\"get");
      String getterMapKey = getMapKey();
      getterMapKey = Character.toUpperCase(getterMapKey.charAt(0)) + getterMapKey.substring(1);
      out.print(getterMapKey); // "getId");
      out.print("\")");
    }
    out.println(");");

    out.print(var + ".");

    if (isMap) {
      out.println("putAll(value);");
    }
    else {
      out.println("addAll(0, value);");
    }

    out.popDepth();
    out.println("} catch(Exception e) {");
    out.println("  throw com.caucho.amber.AmberRuntimeException.create(e);");
    out.println("}");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates code for foreign entity create/delete
   */
  @Override
  public void generateInvalidateForeign(JavaWriter out)
    throws IOException
  {
    out.println("if (\"" + _sourceLink.getSourceTable().getName() + "\".equals(table)) {");
    out.pushDepth();

    generateExpire(out);

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates code for the object expire
   */
  @Override
  public void generateExpire(JavaWriter out)
    throws IOException
  {
    String var = "_caucho_field_" + getGetterName();

    out.println("if (" + var + " != null)");
    out.println("  " + var + ".update();");
  }

  /**
   * Generates the detach property.
   */
  @Override
  public void generateDetach(JavaWriter out)
    throws IOException
  {
    String var = "_caucho_field_" + getGetterName();

    out.println("if (" + var + " != null)");
    out.println("  " + var + ".detach();");
  }

  private String generateAccessor(String src, String var)
  {
    if (src.equals("super"))
      return var;
    else
      return "((" + getRelatedType().getInstanceClassName() + ") " + src + ")." + var;
  }
}
