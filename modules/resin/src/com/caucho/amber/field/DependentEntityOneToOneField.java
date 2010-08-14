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
import com.caucho.amber.expr.DependentEntityOneToOneExpr;
import com.caucho.amber.expr.PathExpr;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.*;
import com.caucho.amber.type.*;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;

import javax.persistence.CascadeType;

/**
 * Represents the dependent side of a one-to-one bidirectional link.
 */
public class DependentEntityOneToOneField extends CascadableField {
  private static final L10N L = new L10N(DependentEntityOneToOneField.class);
  private static final Logger log
    = Logger.getLogger(DependentEntityOneToOneField.class.getName());

  private ManyToOneField _targetField;
  private long _targetLoadIndex;
  private boolean _isCascadeDelete;

  public DependentEntityOneToOneField(EntityType relatedType,
                                      String name)
    throws ConfigException
  {
    super(relatedType, name, null);
  }

  public DependentEntityOneToOneField(EntityType relatedType,
                                      String name,
                                      CascadeType[] cascadeTypes)
    throws ConfigException
  {
    super(relatedType, name, cascadeTypes);
  }

  /**
   * Sets the target field.
   */
  public void setTargetField(ManyToOneField targetField)
  {
    _targetField = targetField;
  }

  /**
   * Sets the target field.
   */
  public ManyToOneField getTargetField()
  {
    return _targetField;
  }

  /**
   * Gets the target load index.
   */
  public long getTargetLoadIndex()
  {
    return _targetLoadIndex;
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
   * Returns the target type as
   * entity or mapped-superclass.
   */
  public EntityType getEntityTargetType()
  {
    return (EntityType) _targetField.getSourceType();
  }

  /**
   * Returns the target type.
   */
  public AmberType getType()
  {
    return getEntityTargetType();
  }

  /**
   * Returns the foreign type.
   */
  public String getForeignTypeName()
  {
    //return ((KeyColumn) getColumn()).getType().getForeignTypeName();
    return getEntityTargetType().getForeignTypeName();
  }

  /**
   * Sets the column.
   */
  public void setColumn(AmberColumn column)
  {
    throw new IllegalStateException();
  }

  /**
   * Sets the cascade-delete property.
   */
  public void setCascadeDelete(boolean isCascadeDelete)
  {
    _isCascadeDelete = isCascadeDelete;
  }

  /**
   * Returns the cascade-delete property.
   */
  public boolean isCascadeDelete()
  {
    return _isCascadeDelete;
  }

  public void init()
    throws ConfigException
  {
    super.init();

    _targetLoadIndex = getEntitySourceType().nextLoadGroupIndex();
  }

  /**
   * Creates the expression for the field.
   */
  public AmberExpr createExpr(QueryParser parser, PathExpr parent)
  {
    return new DependentEntityOneToOneExpr(parent,
                                           _targetField.getLinkColumns());
  }

  /**
   * Gets the column corresponding to the target field.
   */
  public ForeignColumn getColumn(IdField targetField)
  {
    /*
      EntityColumn entityColumn = (EntityColumn) getColumn();

      ArrayList<ForeignColumn> columns = entityColumn.getColumns();

      Id id = getEntityTargetType().getId();
      ArrayList<IdField> keys = id.getKeys();

      for (int i = 0; i < keys.size(); i++ ){
      if (keys.get(i) == targetField)
      return columns.get(i);
      }
    */

    return null;
  }

  /**
   * Generates the flush check for this child.
   */
  /*
  public boolean generateFlushCheck(JavaWriter out)
    throws IOException
  {
    // ejb/06bi
    if (! getEntitySourceType().getPersistenceUnit().isJPA())
      return false;

    String getter = generateSuperGetter("this");

    out.println("if (" + getter + " != null) {");
    out.pushDepth();
    out.println("com.caucho.amber.entity.EntityState state = ((com.caucho.amber.entity.Entity) " + getter + ").__caucho_getEntityState();");

    // jpa/0s2d
    out.println("if (__caucho_state.isTransactional() && ! state.isManaged())");
    String errorString = ("(\"amber flush: unable to flush " +
                          getEntitySourceType().getName() + "[\" + __caucho_getPrimaryKey() + \"] "+
                          "with non-managed relationship one-to-one to "+
                          getEntityTargetType().getName() + " with state='\" + __caucho_state + \"'\")");

    // jpa/0o37 (tck)
    out.println("  throw new IllegalStateException" + errorString + ";");
    out.popDepth();
    out.println("}");

    return true;
  }
  */

  /**
   * Generates any prologue.
   */
  public void generatePrologue(JavaWriter out, HashSet<Object> completedSet)
    throws IOException
  {
    super.generatePrologue(out, completedSet);

    out.println();

    Id id = getEntityTargetType().getId();

    id.generatePrologue(out, completedSet, getName());
  }

  /**
   * Generates the linking for a join
   */
  public void generateJoin(CharBuffer cb,
                           String sourceTable,
                           String targetTable)
  {
    LinkColumns linkColumns = _targetField.getLinkColumns();

    cb.append(linkColumns.generateJoin(sourceTable, targetTable));
  }

  /**
   * Generates loading code
   */
  @Override
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
    if (isLazy()) {
      out.println(generateSuperSetter("this", "null") + ";");

      String loadVar = "__caucho_loadMask_" + (_targetLoadIndex / 64);
      long loadMask = (1L << _targetLoadIndex);

      out.println(loadVar + " &= ~" + loadMask + "L;");
    }

    return index;
  }

  /**
   * Generates loading code after the basic fields.
   */
  @Override
  public int generatePostLoadSelect(JavaWriter out, int index)
    throws IOException
  {
    if (! isLazy()) {
      out.println(getGetterName() + "();");
    }

    return ++index;
  }

  /**
   * Generates the set property.
   */
  @Override
  public void generateGetterMethod(JavaWriter out)
    throws IOException
  {
    String loadVar = "__caucho_loadMask_" + (_targetLoadIndex / 64);
    long loadMask = 1L << (_targetLoadIndex % 64);

    String index = "_" + (_targetLoadIndex / 64);
    index += "_" + (1L << (_targetLoadIndex % 64));

    String javaType = getJavaTypeName();

    out.println();
    out.println("public " + javaType + " " + getGetterName() + "()");
    out.println("{");
    out.pushDepth();

    // jpa/0h29
    out.println("if (__caucho_session != null");
    out.println("    && ! __caucho_state.isDeleting()");
    out.println("    && (" + loadVar + " & " + loadMask + "L) == 0) {");
    out.pushDepth();
    out.println("__caucho_load_select_" + getLoadGroupIndex() + "(__caucho_session);");
    out.println(loadVar + " |= " + loadMask + "L;");

    generateLoadProperty(out, index, "__caucho_session");

    out.println("return v"+index+";");
    out.popDepth();
    out.println("}");
    out.println("else {");
    out.println("  return " + generateSuperGetter("this") + ";");
    out.println("}");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the set property.
   */
  public void generateLoadProperty(JavaWriter out,
                                   String index,
                                   String session)
    throws IOException
  {
    String javaType = getJavaTypeName();

    out.println(javaType + " v" + index + " = null;");

    out.println("try {");
    out.pushDepth();

    out.print("String sql"+index+" = \"");
    out.print("SELECT o." + getName() +
              " FROM " + getEntitySourceType().getName() + " o" +
              " WHERE ");

    ArrayList<IdField> sourceKeys = getEntitySourceType().getId().getKeys();
    for (int i = 0; i < sourceKeys.size(); i++) {
      if (i != 0)
        out.print(" and ");

      IdField key = sourceKeys.get(i);

      out.print("o." + key.getName() + "=?");
    }
    out.println("\";");

    out.println("com.caucho.amber.AmberQuery query"+index+" = "+session+".prepareQuery(sql"+index+");");

    out.println("int index"+index+" = 1;");

    getEntitySourceType().getId().generateSet(out, "query"+index, "index"+index, "super");

    boolean isJPA = getEntitySourceType().getPersistenceUnit().isJPA();

    if (isJPA) {
      out.println("v"+index+" = (" + javaType + ") query"+index+".getSingleResult();");
    } else {
      // ejb/06hj
      out.println("com.caucho.amber.entity.Entity e = (com.caucho.amber.entity.Entity) query"+index+".getSingleResult();");
      out.println("v"+index+" = (" + javaType + ") __caucho_session.loadProxy(e.__caucho_getEntityType(), e.__caucho_getPrimaryKey());");
    }

    out.popDepth();
    out.println("} catch (java.sql.SQLException e) {");
    out.println("  throw new RuntimeException(e);");
    out.println("}");

    out.println(generateSuperSetter("this", "v" + index) + ";");
  }

  /**
   * Updates the cached copy.
   */
  public void generateCopyUpdateObject(JavaWriter out,
                                       String dst, String src,
                                       int updateIndex)
    throws IOException
  {
    // jpa/0ge4

    /* XXX: should not be necessary to update the cache item from the
       dependent side.

    if (getIndex() == updateIndex) {
      String value = generateGet(src);

      value = "(" + getEntityTargetType().getInstanceClassName() + ") aConn.getEntity((com.caucho.amber.entity.Entity) " + value + ")";

      out.println(generateStatementSet(dst, value) + ";");
    }
    */
  }

  /**
   * Updates the cached copy.
   */
  public void generateMergeFrom(JavaWriter out,
                                      String dst, String src)
    throws IOException
  {
    if (! (getEntityTargetType() instanceof EntityType))
      return;

    String value = generateGet(src);

    out.println("if (" + value + " != null) {");
    out.pushDepth();

    if (! isCascade(CascadeType.MERGE)) {
      value = "(" + getJavaTypeName() + ") aConn.mergeDetachedEntity((com.caucho.amber.entity.Entity) " + value + ")";
    }
    else {
      value = "(" + getJavaTypeName() + ") aConn.recursiveMerge(" +
        value + ")";
    }

    out.println(generateSet(dst, value) + ";");

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
    Id id = getEntityTargetType().getId();

    String keyType = getEntityTargetType().getId().getForeignTypeName();

    out.println();
    out.println("public void " + getSetterName() + "(" + getJavaTypeName() + " v)");
    out.println("{");
    out.pushDepth();

    out.println(generateSuperSetter("this", "v") + ";");
    out.println("if (__caucho_session != null) {");
    out.pushDepth();

    out.println("try {");
    out.pushDepth();

    // XXX: jpa/0h27
    out.println("if (__caucho_state.isPersist())");
    out.println("  __caucho_cascadePrePersist(__caucho_session);");

    out.popDepth();
    out.println("} catch (RuntimeException e) {");
    out.println("  throw e;");
    out.println("} catch (Exception e) {");
    out.println("  throw new com.caucho.amber.AmberRuntimeException(e);");
    out.println("}");
    
    String loadVar = "__caucho_loadMask_" + (_targetLoadIndex / 64);
    long loadMask = 1L << (_targetLoadIndex % 64);
    out.println(loadVar + " |= " + loadMask + "L;");

    /*
    String updateVar = "__caucho_updateMask_" + (_targetLoadIndex / 64);
    long updateMask = (1L << _targetLoadIndex);

    out.println(updateVar + " |= " + updateMask + "L;");
    out.println("__caucho_session.update(this);");
    */
    out.popDepth();
    out.println("}");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the set clause.
   */
  public void generateStatementSet(JavaWriter out, String pstmt, String index)
    throws IOException
  {
  }

  /**
   * Generates loading cache
   */
  public void generateUpdateFromObject(JavaWriter out, String obj)
    throws IOException
  {
  }

  /**
   * Generates code for foreign entity create/delete
   */
  public void generateInvalidateForeign(JavaWriter out)
    throws IOException
  {
    // AmberTable table = getEntityTargetType().getTable();

    AmberPersistenceUnit persistenceUnit = getSourceType().getPersistenceUnit();

    AmberTable table;

    if (persistenceUnit.isJPA()) {
      String className = getJavaType().getName();
      EntityType entity = persistenceUnit.getEntityType(className);

      // jpa/0ge4
      table = entity.getTable();
    }
    else {
      // ejb/0691
      table = getEntityTargetType().getTable();
    }

    out.println("if (\"" + table.getName() + "\".equals(table)) {");
    out.pushDepth();
    String loadVar = "__caucho_loadMask_" + (_targetLoadIndex / 64);
    out.println(loadVar + " = 0;");
    out.popDepth();
    out.println("}");
  }

  public void generatePreCascade(JavaWriter out,
                                 String aConn,
                                 CascadeType cascadeType)
    throws IOException
  {
    if (cascadeType == CascadeType.PERSIST)
      return;

    // jpa/0o33
    generateInternalCascade(out, aConn, cascadeType);
  }

  public void generatePostCascade(JavaWriter out,
                                  String aConn,
                                  CascadeType cascadeType)
    throws IOException
  {
    if (cascadeType != CascadeType.PERSIST)
      return;

    // jpa/0o33
    generateInternalCascade(out, aConn, cascadeType);
  }
}
