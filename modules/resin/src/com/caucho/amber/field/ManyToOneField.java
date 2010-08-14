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

import com.caucho.amber.AmberRuntimeException;
import com.caucho.amber.cfg.*;
import com.caucho.amber.expr.AmberExpr;
import com.caucho.amber.expr.ManyToOneExpr;
import com.caucho.amber.expr.PathExpr;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.*;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.persistence.CascadeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;
import javax.persistence.JoinColumn;

/**
 * Represents a many-to-one link pointing to an entity.
 */
public class ManyToOneField extends CascadableField {
  private static final L10N L = new L10N(ManyToOneField.class);
  private static final Logger log
    = Logger.getLogger(ManyToOneField.class.getName());

  private LinkColumns _linkColumns;

  private EntityType _targetType;

  private int _targetLoadIndex;

  private DependentEntityOneToOneField _targetField;
  private AmberField _aliasField;

  private boolean _isInsert = true;
  private boolean _isUpdate = true;

  private boolean _isSourceCascadeDelete;
  private boolean _isTargetCascadeDelete;

  private boolean _isManyToOne;

  private JoinColumn _joinColumnsAnn[];
  private HashMap<String, JoinColumnConfig> _joinColumnMap = null;

  public ManyToOneField(EntityType relatedType,
                              String name,
                              CascadeType[] cascadeType,
                              boolean isManyToOne)
    throws ConfigException
  {
    super(relatedType, name, cascadeType);

    _isManyToOne = isManyToOne;
  }

  public ManyToOneField(EntityType relatedType,
                              String name,
                              CascadeType[] cascadeType)
    throws ConfigException
  {
    super(relatedType, name, cascadeType);
  }

  public ManyToOneField(EntityType relatedType,
                              String name)
    throws ConfigException
  {
    this(relatedType, name, null);
  }

  public ManyToOneField(EntityType relatedType)
  {
    super(relatedType);
  }

  /**
   * Sets the target type.
   */
  public void setType(AmberType targetType)
  {
    if (! (targetType instanceof EntityType))
      throw new AmberRuntimeException(L.l("many-to-one requires an entity target at '{0}'",
                                          targetType));

    _targetType = (EntityType) targetType;
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
   * Returns the target type as
   * entity or mapped-superclass.
   */
  public EntityType getEntityTargetType()
  {
    return _targetType;
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
   * Returns true if it is annotated as many-to-one.
   */
  public boolean isAnnotatedManyToOne()
  {
    return _isManyToOne;
  }

  /**
   * Set true if deletes cascade to the target.
   */
  public void setTargetCascadeDelete(boolean isCascadeDelete)
  {
    _isTargetCascadeDelete = isCascadeDelete;
  }

  /**
   * Set true if deletes cascade to the source.
   */
  public void setSourceCascadeDelete(boolean isCascadeDelete)
  {
    _isSourceCascadeDelete = isCascadeDelete;
  }

  /**
   * Set true if deletes cascade to the target.
   */
  public boolean isTargetCascadeDelete()
  {
    return _isTargetCascadeDelete;
  }

  /**
   * Set true if deletes cascade to the source.
   */
  public boolean isSourceCascadeDelete()
  {
    return _isSourceCascadeDelete;
  }

  /**
   * Sets the join column annotations.
   */
  public void setJoinColumns(JoinColumn joinColumnsAnn[])
  {
    _joinColumnsAnn = joinColumnsAnn;
  }

  /**
   * Gets the join column annotations.
   */
  public Object[] getJoinColumns()
  {
    return _joinColumnsAnn;
  }

  /**
   * Sets the join column map.
   */
  public void setJoinColumnMap(HashMap<String, JoinColumnConfig> joinColumnMap)
  {
    _joinColumnMap = joinColumnMap;
  }

  /**
   * Gets the join column map.
   */
  public HashMap<String, JoinColumnConfig> getJoinColumnMap()
  {
    return _joinColumnMap;
  }

  /**
   * Sets the join columns.
   */
  public void setLinkColumns(LinkColumns linkColumns)
  {
    _linkColumns = linkColumns;
  }

  /**
   * Gets the columns.
   */
  public LinkColumns getLinkColumns()
  {
    return _linkColumns;
  }

  /**
   * Sets the target field.
   */
  public void setTargetField(DependentEntityOneToOneField field)
  {
    _targetField = field;
  }

  /**
   * Sets any alias field.
   */
  public void setAliasField(AmberField alias)
  {
    _aliasField = alias;
  }

  /**
   * Creates a copy of the field for a parent
   */
  @Override
  public AmberField override(BeanType type)
  {
    ManyToOneField field
      = new ManyToOneField((EntityType) getSourceType(), getName(),
                                 getCascadeType(), _isManyToOne);

    field.setOverride(true);
    field.setLazy(isLazy());
    /*
    field.setInsert(_isInsert);
    field.setUpdate(_isUpdate);
    */
    
    return field;
  }

  /**
   * Initializes the field.
   */
  @Override
  public void init()
    throws ConfigException
  {
    init(getRelatedType());
  }

  /**
   * Initializes the field.
   */
  public void init(EntityType relatedType)
    throws ConfigException
  {
    boolean isJPA = relatedType.getPersistenceUnit().isJPA();

    int loadGroupIndex = getEntitySourceType().getDefaultLoadGroupIndex();
    super.setLoadGroupIndex(loadGroupIndex);

    // jpa/0l40 vs. ejb/0602
    if (isJPA)
      _targetLoadIndex = loadGroupIndex;
    else
      _targetLoadIndex = relatedType.nextLoadGroupIndex();

    AmberTable sourceTable = relatedType.getTable();

    if (sourceTable == null || ! isJPA) {
      // jpa/0ge3, ejb/0602
      super.init();
      return;
    }

    // jpa/0j67
    setSourceCascadeDelete(isCascade(CascadeType.REMOVE));

    int n = 0;

    if (_joinColumnMap != null)
      n = _joinColumnMap.size();

    ArrayList<ForeignColumn> foreignColumns = new ArrayList<ForeignColumn>();

    EntityType parentType = _targetType;

    ArrayList<AmberColumn> targetIdColumns = _targetType.getId().getColumns();

    while (targetIdColumns.size() == 0) {
      parentType = parentType.getParentType();

      if (parentType == null)
        break;

      targetIdColumns = parentType.getId().getColumns();
    }

    for (AmberColumn keyColumn : targetIdColumns) {
      String columnName;

      columnName = getName() + '_' + keyColumn.getName();

      boolean nullable = true;
      boolean unique = false;

      if (n > 0) {
        JoinColumnConfig joinColumn;

        if (n == 1) {
          joinColumn = (JoinColumnConfig) _joinColumnMap.values().toArray()[0];
        } else
          joinColumn = _joinColumnMap.get(keyColumn.getName());

        if (joinColumn != null) {
          // jpa/0h0d
          if (! "".equals(joinColumn.getName()))
            columnName = joinColumn.getName();

          nullable = joinColumn.isNullable();
          unique = joinColumn.isUnique();
        }
      }
      else {
        JoinColumn joinAnn
          = BaseConfigIntrospector.getJoinColumn(_joinColumnsAnn,
                                                 keyColumn.getName());

        if (joinAnn != null) {
          columnName = joinAnn.name();

          nullable = joinAnn.nullable();
          unique = joinAnn.unique();
        }
      }

      ForeignColumn foreignColumn;

      foreignColumn = sourceTable.createForeignColumn(columnName, keyColumn);

      foreignColumn.setNotNull(! nullable);
      foreignColumn.setUnique(unique);

      foreignColumns.add(foreignColumn);
    }

    LinkColumns linkColumns = new LinkColumns(sourceTable,
                                              _targetType.getTable(),
                                              foreignColumns);

    setLinkColumns(linkColumns);

    super.init();

    Id id = getEntityTargetType().getId();
    ArrayList<AmberColumn> keys = id.getColumns();

    if (_linkColumns == null) {
      ArrayList<ForeignColumn> columns = new ArrayList<ForeignColumn>();

      for (int i = 0; i < keys.size(); i++) {
        AmberColumn key = keys.get(i);

        String name;

        if (keys.size() == 1)
          name = getName();
        else
          name = getName() + "_" + key.getName();

        columns.add(sourceTable.createForeignColumn(name, key));
      }

      _linkColumns = new LinkColumns(relatedType.getTable(),
                                     _targetType.getTable(),
                                     columns);
    }

    if (relatedType.getId() != null) {
      // resolve any alias
      for (AmberField field : relatedType.getId().getKeys()) {
        for (ForeignColumn column : _linkColumns.getColumns()) {
          if (field.getColumn() != null
              && field.getColumn().getName().equals(column.getName())) {
            _aliasField = field;
          }
        }
      }
    }

    _targetLoadIndex = relatedType.getLoadGroupIndex(); // nextLoadGroupIndex();

    _linkColumns.setTargetCascadeDelete(isTargetCascadeDelete());
    _linkColumns.setSourceCascadeDelete(isSourceCascadeDelete());
  }

  /**
   * Generates the post constructor initialization.
   */
  @Override
  public void generatePostConstructor(JavaWriter out)
    throws IOException
  {
    if (_aliasField == null) {
      out.println(getSetterName() + "(" + generateSuperGetter("this") + ");");
    }
  }

  /**
   * Creates the expression for the field.
   */
  @Override
  public AmberExpr createExpr(QueryParser parser, PathExpr parent)
  {
    return new ManyToOneExpr(parent, _linkColumns);
  }

  /**
   * Gets the column corresponding to the target field.
   */
  public ForeignColumn getColumn(AmberColumn targetColumn)
  {
    return _linkColumns.getSourceColumn(targetColumn);
  }

  /**
   * Generates the insert.
   */
  @Override
  public void generateInsertColumns(ArrayList<String> columns)
  {
    if (_isInsert && _aliasField == null)
      _linkColumns.generateInsert(columns);
  }

  /**
   * Generates the select clause.
   */
  @Override
  public String generateLoadSelect(AmberTable table, String id)
  {
    if (_aliasField != null)
      return null;

    if (_linkColumns == null) {
      // jpa/0ge3
      return null;
    }

    if (_linkColumns.getSourceTable() != table)
      return null;
    else
      return _linkColumns.generateSelectSQL(id);
  }

  /**
   * Generates the select clause.
   */
  @Override
  public String generateSelect(String id)
  {
    if (_aliasField != null)
      return null;

    return _linkColumns.generateSelectSQL(id);
  }

  /**
   * Generates the update set clause
   */
  @Override
  public void generateUpdate(CharBuffer sql)
  {
    if (_aliasField != null)
      return;

    if (_isUpdate) {
      sql.append(_linkColumns.generateUpdateSQL());
    }
  }

  /**
   * Generates any prologue.
   */
  @Override
  public void generatePrologue(JavaWriter out, HashSet<Object> completedSet)
    throws IOException
  {
    super.generatePrologue(out, completedSet);

    out.println();

    Id id = getEntityTargetType().getId();

    out.println("protected transient " + id.getForeignTypeName() + " __caucho_field_" + getName() + ";");

    if (_aliasField == null) {
      id.generatePrologue(out, completedSet, getName());
    }
  }

  /**
   * Generates the linking for a join
   */
  public void generateJoin(CharBuffer cb,
                           String sourceTable,
                           String targetTable)
  {
    cb.append(_linkColumns.generateJoin(sourceTable, targetTable));
  }

  /**
   * Generates loading code
   */
  @Override
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
    if (_aliasField != null)
      return index;

    out.print("__caucho_field_" + getName() + " = ");

    index = getEntityTargetType().getId().generateLoadForeign(out, rs,
                                                              indexVar, index,
                                                              getName());

    out.println(";");

    /*
    // ejb/0a06
    String proxy = "aConn.loadProxy(\"" + getEntityTargetType().getName() + "\", __caucho_field_" + getName() + ")";

    proxy = "(" + getEntityTargetType().getProxyClass().getName() + ") " + proxy;

    out.println(generateSuperSetterMethod(proxy) + ";");
    */

    // commented out jpa/0l40
    // out.println(generateSuperSetterMethod("null") + ";");

    int group = _targetLoadIndex / 64;
    long mask = (1L << (_targetLoadIndex % 64));

    //out.println("__caucho_loadMask_" + group + " &= ~" + mask + "L;");

    return index;
  }

  /* XXX: moved to generatePostLoadSelect()
   * Generates loading code
   *
  public int generateLoadEager(JavaWriter out, String rs,
                               String indexVar, int index)
    throws IOException
  {
  }
  */

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
   * Generates the get property.
   */
  @Override
  public void generateGetterMethod(JavaWriter out)
    throws IOException
  {
    // jpa/0h07, jpa/0h08
    // jpa/0o03, jpa/0o05, jpa/0o09
    // jpa/0s2d, jpa/1810

    String javaType = getJavaTypeName();

    out.println();
    out.println("public " + javaType + " " + getGetterName() + "()");
    out.println("{");
    out.pushDepth();

    int keyLoadIndex = getLoadGroupIndex();
    int entityLoadIndex = _targetLoadIndex;

    int group = entityLoadIndex / 64;
    long mask = (1L << (entityLoadIndex % 64));
    String loadVar = "__caucho_loadMask_" + group;

    // jpa/0h29
    out.println("if (__caucho_session == null || __caucho_state.isDeleting()) {");

    out.println("  return " + generateSuperGetter("this") + ";");
    out.println("}");

    /* XXX: jpa/0h04
    if (isLazy())
      out.println(" && (" + loadVar + " & " + mask + "L) == 0) {");
    else {
      // jpa/0o03
      out.println(") {");
    }
    */

    out.println();

    String index = "_" + group;
    index += "_" + mask;

    if (_aliasField == null) {
      // XXX: possibly bypassing of caching
      out.println("if ((" + loadVar + " & " + mask + "L) == 0)");
      out.println("  __caucho_load_select_" + getLoadGroupIndex() + "(__caucho_session);");
    }

    out.println(loadVar + " |= " + mask + "L;");

    String varName = generateLoadProperty(out, index, "__caucho_session");

    out.println("return " + varName + ";");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the set property.
   */
  public String generateLoadProperty(JavaWriter out,
                                     String index,
                                     String session)
    throws IOException
  {
    boolean isJPA = getRelatedType().getPersistenceUnit().isJPA();

    String targetTypeExt = _targetType.getInstanceClassName();

    String otherKey;

    if (_aliasField == null)
      otherKey = "__caucho_field_" + getName();
    else
      otherKey = _aliasField.generateGet("super");

    String proxyType = getEntityTargetType().getProxyClass().getName();
    boolean isProxy = ! isJPA;

    String varName = "v" + index;
    String proxyVarName;

    if (isProxy)
      proxyVarName = "p" + index;
    else
      proxyVarName = varName;

    if (isProxy)
      out.println(proxyType + " " + proxyVarName + " = null;");

    out.println(targetTypeExt + " " + varName + " = null;");

    out.println();

    Id id = getEntityTargetType().getId();

    // jpa/0s2d
    String nullTest = otherKey + " != null";

    /* XXX
    if (id instanceof CompositeId) {
    }
    else {
      KeyPropertyField key = (KeyPropertyField) id.getKeys().get(0);
      nullTest = key.getColumn().getType().generateIsNotNull(otherKey);
    }
    */

    // jpa/0h27
    out.println("if (" + nullTest + ") {");
    out.pushDepth();

    long targetGroup = 0;
    long targetMask = 0;

    // jpa/0s2e as a negative test.
    if (_targetField != null) {
      // jpa/0l42
      long targetLoadIndex = _targetField.getTargetLoadIndex();
      targetGroup = targetLoadIndex / 64;
      targetMask = (1L << (targetLoadIndex % 64));
    }

    out.println(varName + " = (" + targetTypeExt + ") "
                + session + ".loadEntity("
                + targetTypeExt + ".class, "
                + otherKey + ", " + ! isLazy() + ");");

    /*
    // jpa/0j67
    out.println("if (" + varName + " != null && " + varName + " != " + generateSuperGetter("this") + ") {");
    out.pushDepth();

    // ejb/069a
    if (isJPA && _targetField != null) {
      out.println(_targetField.generateSet(varName, "this") + ";");
      out.println(varName + ".__caucho_retrieve_eager(" + session + ");");
    }

    // generateSetTargetLoadMask(out, varName);
    

    out.popDepth();
    out.println("}");
    */

    // ejb/06h0, jpa/0o03
    if (isAbstract() && (isLazy() || ! isJPA)) {
      String proxy = session + ".loadProxy(\"" + getEntityTargetType().getName() + "\", __caucho_field_" + getName() + ")";

      // jpa/0o09
      if (isJPA)
        proxyType = targetTypeExt;

      proxy = proxyVarName + " = (" + proxyType + ") " + proxy + ";";

      out.println(proxy);
    }
    else {
      /*
      // jpa/0h24
      out.println("if (! " + varName + ".__caucho_getEntityState().isManaged()) {");
      out.pushDepth();

      long targetMask = 0;
      long targetGroup = 0;

      if (_targetField != null) {
        long targetLoadIndex = _targetField.getTargetLoadIndex();
        targetGroup = targetLoadIndex / 64;
        targetMask = (1L << (targetLoadIndex % 64));
      }

      // jpa/0o03, jpa/0ge4
      out.println(session + ".loadFromHome(" + targetTypeExt + ".class.getName(), " + otherKey + ", " + targetMask + ", " + targetGroup + ");");
      out.popDepth();
      out.println("}");
      */
    }

    out.popDepth();
    out.println("}");

    out.println(generateSuperSetter("this", proxyVarName) + ";");

    return proxyVarName;
  }

  /**
   * Generates the set property.
   */
  @Override
  public void generateSetterMethod(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public void " + getSetterName() + "(" + getJavaTypeName() + " v)");
    out.println("{");
    out.pushDepth();

    out.println("if (__caucho_session == null) {");
    out.println("  " + generateSuperSetter("this", "v") + ";");
    out.println("  return;");
    out.println("}");

    String targetClassName = getEntityTargetType().getInstanceClassName();
    
    // ejb/06gc - updates with EJB 2.0

    Id id = getEntityTargetType().getId();
    String var = "__caucho_field_" + getName();

    String keyType = getEntityTargetType().getId().getForeignTypeName();

    int group = getLoadGroupIndex() / 64;
    long loadMask = (1L << (getLoadGroupIndex() % 64));
    String loadVar = "__caucho_loadMask_" + group;

    if (_aliasField == null) {
      out.println();
      out.println("if ((" + loadVar + " & " + loadMask + "L) == 0) {");
      // ejb/0602
      out.println("  __caucho_load_select_" + group + "(__caucho_session);");
      //out.println();
      // jpa/0j5f
      //out.println("  if (__caucho_session.isActiveTransaction())");
      //out.println("    __caucho_session.makeTransactional((com.caucho.amber.entity.Entity) this);");
      out.println("}");
      
      out.println();
      out.println(generateSuperSetter("this", "v") + ";");
      
      out.println();
      out.println("if (v == null) {");
      out.println("  if (" + var + " == null)");
      out.println("    return;");
      out.println();
      out.println("  " + var + " = null;");
      out.println("} else {");
      out.pushDepth();
      out.println(targetClassName + " newV = (" + targetClassName + ") v;");

      out.print(keyType + " key = ");

      EntityType targetType = getEntityTargetType();

      if (targetType.isEJBProxy(getJavaTypeName())) {
        // To handle EJB local objects.
        out.print(id.generateGetProxyKey("v"));
      }
      else {
        out.print(id.toObject(id.generateGet("newV")));
      }

      out.println(";");

      out.println();
      out.println("if (key.equals(" + var + "))");
      out.println("  return;");

      out.println();
      out.println(var + " = key;");

      out.popDepth();
      out.println("}");

      out.println();

      int entityGroup = _targetLoadIndex / 64;
      long entityLoadMask = (1L << (_targetLoadIndex % 64));
      String entityLoadVar = "__caucho_loadMask_" + group;

      out.println(entityLoadVar + " |= " + entityLoadMask + "L;");

      String dirtyVar = "__caucho_dirtyMask_" + (getIndex() / 64);
      long dirtyMask = (1L << (getIndex() % 64));

      // jpa/0o42: merge()
      out.println(dirtyVar + " |= " + dirtyMask + "L;");

      /*
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
      */

      out.println("__caucho_session.update((com.caucho.amber.entity.Entity) this);");

      out.println("__caucho_session.addCompletion(__caucho_home.createManyToOneCompletion(\"" + getName() + "\", (com.caucho.amber.entity.Entity) this, v));");

      out.println();
    }
    else {
      out.println("throw new IllegalStateException(\"aliased field cannot be set\");");
    }

    out.popDepth();
    out.println("}");
  }

  void generateSetTargetLoadMask(JavaWriter out, String varName)
    throws IOException
  {
    // jpa/0o0-, jpa/0ge4
    boolean isJPA = getRelatedType().getPersistenceUnit().isJPA();

    if (_targetField != null && isJPA) {
      long targetLoadIndex = _targetField.getTargetLoadIndex();
      long targetGroup = targetLoadIndex / 64;
      long targetMask = (1L << (targetLoadIndex % 64));

      //out.println(varName + ".__caucho_setLoadMask(" + varName + ".__caucho_getLoadMask(" + targetGroup + ") | " + targetMask + "L, " + targetGroup + ");");

      varName = "((" + _targetType.getInstanceClassName() + ") " + varName + ")";

      // jpa/0ge4
      String thisContextEntity = "(" + _targetField.getJavaTypeName() /* getSourceType().getInstanceClassName() */ + ") contextEntity";

      // jpa/0o05
      out.println(_targetField.generateSuperSetter(varName, thisContextEntity) + ";");
    }
  }

  /**
   * Updates the cached copy.
   */
  @Override
  public void generateCopyUpdateObject(JavaWriter out,
                                       String dst, String src,
                                       int updateIndex)
    throws IOException
  {
    // jpa/0s29, jpa/0s2j
    if (getIndex() != updateIndex)
      return;

    // order matters: ejb/06gc
    String var = "__caucho_field_" + getName();
    out.println(generateAccessor(dst, var) + " = " + generateAccessor(src, var) + ";");

    /* The cache update is handled copying only the key.
    // ejb/0627
    if (getRelatedType().getPersistenceUnit().isJPA()) {
      // jpa/0h0a

      String value = generateGet(src);

      value = "(" + _targetType.getInstanceClassName() + ") aConn.getEntity((com.caucho.amber.entity.Entity) " + value + ")";

      out.println(generateStatementSet(dst, value) + ";");
    }
    */
  }

  /**
   * Updates the cached copy.
   */
  @Override
  public void generateCopyLoadObject(JavaWriter out,
                                     String dst, String src,
                                     int updateIndex)
    throws IOException
  {
    if (getLoadGroupIndex() != updateIndex)
      return;

    String var = "__caucho_field_" + getName();

    boolean isJPA = getEntitySourceType().getPersistenceUnit().isJPA();

    // order matters: jpa/0h08, jpa/0h09

    // jpa/0h20, jpa/0o08, ejb/06--, ejb/0a-- and jpa/0o04
    // ejb/0628 vs. jpa/0h0a
    if (isJPA &&
        ! (dst.equals("cacheEntity")
           || dst.equals("super")
           || dst.equals("item"))) {
      String value = generateGet(src);

      out.println("// " + dst);

      if (_targetType instanceof EntityType) {
        String targetTypeExt = getEntityTargetType().getInstanceClassName();

        // jpa/0s2e
        out.println("if (isFullMerge)");
        out.println("  child = " + value + ";");
        out.println("else {");
        out.pushDepth();

        // jpa/0h0a: gets the cache object to copy from.
        out.println("child = aConn.getCacheEntity(" + targetTypeExt + ".class, " + var + ");");

        // jpa/0o36: the cache item is only available after commit.
        out.println();
        out.println("if (child == null && " + value + " != null)");
        out.println("  child = ((com.caucho.amber.entity.Entity) " + value + ").__caucho_getCacheEntity();");

        out.popDepth();
        out.println("}");
      }
      else {
        // XXX: jpa/0l14
        out.println("child = null;");
      }

      out.println("if (child != null) {");
      out.pushDepth();

      String targetTypeExt = getEntityTargetType().getInstanceClassName();

      out.println("if (isFullMerge) {");
      out.pushDepth();

      // jpa/0j5f
      out.println("child = aConn.load(child.getClass(), ((com.caucho.amber.entity.Entity) child).__caucho_getPrimaryKey(), true);");

      out.popDepth();
      out.println("} else {");
      out.pushDepth();

      // jpa/0l42
      out.println("com.caucho.amber.entity.Entity newChild = aConn.addNewEntity(child.getClass(), ((com.caucho.amber.entity.Entity) child).__caucho_getPrimaryKey());");

      out.println("if (newChild == null) {");
      out.pushDepth();

      value = "aConn.getEntity((com.caucho.amber.entity.Entity) child)";

      out.println("newChild = " + value + ";");

      out.popDepth();
      out.println("} else {");
      out.pushDepth();

      // jpa/0h13
      out.println("((com.caucho.amber.entity.Entity) child).__caucho_copyTo(newChild, aConn, (com.caucho.amber.entity.EntityItem) null);");

      out.popDepth();
      out.println("}");

      out.println("child = newChild;");

      out.popDepth();
      out.println("}");

      out.popDepth();
      out.println("}");

      value = "(" + targetTypeExt + ") child";

      // XXX: jpa/0l43
      out.println("if (isFullMerge)");
      out.println("  " + generateSet(dst, value) + ";");
    }

    // jpa/0o05
    // if (getLoadGroupIndex() == updateIndex) {

    // order matters: ejb/06gc
    out.println(generateAccessor(dst, var) + " = " + generateAccessor(src, var) + ";");

    // jpa/0o08, jpa/0o04
    if (! dst.equals("cacheEntity")) {
      // jpa/0o05, jpa/0h20
      if (! (dst.equals("super") || dst.equals("item"))) { // || isLazy())) {
        String targetObject;

        if (isJPA) {
          // jpa/0h0a

          String targetTypeExt = getEntityTargetType().getInstanceClassName();

          targetObject = "(" + targetTypeExt + ") child";
        }
        else
          targetObject = generateSuperGetter("this");

        String objThis = "((" + getRelatedType().getInstanceClassName() + ") " + dst + ")";

        out.println(generateSuperSetter(objThis, targetObject) + ";");
      }
    }

    // jpa/0o05
    // }

    // commented out: jpa/0s29
    // if (_targetLoadIndex == updateIndex) { // ejb/0h20

    // }

    /*
      if (_targetLoadIndex == updateIndex) {
      // ejb/0a06
      String value = generateGet(src);
      out.println(generateStatementSet(dst, value) + ";");
      }
    */
  }

  /**
   * Updates the cached copy.
   */
  @Override
  public void generateMergeFrom(JavaWriter out, String dst, String src)
    throws IOException
  {
    if (! (getEntityTargetType() instanceof EntityType))
      return;

    String value = generateGet(src);

    out.println("if (" + value + " != null) {");
    out.pushDepth();

    if (isCascade(CascadeType.MERGE)) {
      value = ("(" + getJavaTypeName() + ") aConn.recursiveMerge("
               + value + ")");
    }
    else {
      // jpa/0h08
      value = "(" + getJavaTypeName() + ") aConn.mergeDetachedEntity((com.caucho.amber.entity.Entity) " + value + ")";
    }

    out.println(generateSet(dst, value) + ";");

    out.popDepth();
    out.println("}");
  }

  private String generateAccessor(String src, String var)
  {
    if (src.equals("super"))
      return var;
    else
      return "((" + getRelatedType().getInstanceClassName() + ") " + src + ")." + var;
  }

  /**
   * Generates the flush check for this child.
   */
  /*
  @Override
  public boolean generateFlushCheck(JavaWriter out)
    throws IOException
  {
    // ejb/06bi
    if (! getRelatedType().getPersistenceUnit().isJPA())
      return false;

    String getter = generateSuperGetter("this");

    String dirtyVar = "__caucho_dirtyMask_" + (getIndex() / 64);
    long dirtyMask = (1L << (getIndex() % 64));

    out.println("if ((" + getter + " != null) && (__caucho_state.isPersist() || (" + dirtyVar + " & " + dirtyMask + "L) != 0L)) {");
    out.pushDepth();

    String relatedEntity = "((com.caucho.amber.entity.Entity) " + getter + ")";
    out.println("com.caucho.amber.entity.EntityState otherState = " + relatedEntity + ".__caucho_getEntityState();");

    // jpa/0j5e as a negative test.
    out.println("if (" + relatedEntity + ".__caucho_getConnection() == null) {");
    out.pushDepth();

    // jpa/0j5c as a positive test.
    out.println("if (__caucho_state.isTransactional() && ! otherState.isManaged())");

    String errorString = ("(\"amber flush: unable to flush " +
                          getRelatedType().getName() + "[\" + __caucho_getPrimaryKey() + \"] "+
                          "with non-managed dependent relationship many-to-one to "+
                          getEntityTargetType().getName() +
                          ". Current entity state: \" + __caucho_state + \" " +
                          "and parent entity state: \" + otherState)");

    out.println("  throw new IllegalStateException" + errorString + ";");

    out.popDepth();
    out.println("}");

    out.popDepth();
    out.println("}");

    return true;
  }
  */

  /**
   * Generates the set clause.
   */
  @Override
  public void generateStatementSet(JavaWriter out, String pstmt,
                          String index, String source)
    throws IOException
  {
    if (_aliasField != null)
      return;

    if (source == null) {
      throw new NullPointerException();
    }

    String var = "__caucho_field_" + getName();

    if (! source.equals("this") && ! source.equals("super"))
      var = source + "." + var;

    if (! (isAbstract() && getRelatedType().getPersistenceUnit().isJPA())) {
      // jpa/1004, ejb/06bi
      out.println("if (" + var + " != null) {");
    }
    else if (isCascade(CascadeType.PERSIST)) {
      // jpa/0h09
      out.println("if (" + var + " != null) {");
    } else {
      // jpa/0j58: avoids breaking FK constraints.

      // The "one" end in the many-to-one relationship.
      String amberVar = getFieldName();

      out.println("com.caucho.amber.entity.EntityState " + amberVar + "_state = (" + var + " == null) ? ");
      out.println("com.caucho.amber.entity.EntityState.TRANSIENT : ");
      out.println("((com.caucho.amber.entity.Entity) " + amberVar + ").");
      out.println("__caucho_getEntityState();");

      out.println("if (" + amberVar + "_state.isTransactional()) {");
    }

    out.pushDepth();

    Id id = getEntityTargetType().getId();
    ArrayList<IdField> keys = id.getKeys();

    if (keys.size() == 1) {
      IdField key = keys.get(0);

      key.getType().generateSet(out, pstmt, index, key.getType().generateCastFromObject(var));
    }
    else {
      for (int i = 0; i < keys.size(); i++) {
        IdField key = keys.get(i);

        key.getType().generateSet(out, pstmt, index, key.generateGetKeyProperty(var));
      }
    }

    out.popDepth();
    out.println("} else {");
    out.pushDepth();

    for (int i = 0; i < keys.size(); i++) {
      IdField key = keys.get(i);

      key.getType().generateSetNull(out, pstmt, index);
    }

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates loading cache
   */
  @Override
  public void generateUpdateFromObject(JavaWriter out, String obj)
    throws IOException
  {
    String var = "__caucho_field_" + getName();

    out.println(var + " = " + obj + "." + var + ";");
  }

  /**
   * Updates the cached copy.
   */
  @Override
  public void generatePrePersist(JavaWriter out)
    throws IOException
  {
    EntityType targetType = getEntityTargetType();

    if (! (targetType instanceof EntityType))
      return;
    
    String fieldVar = "__caucho_field_" + getName();
    
    String className = targetType.getInstanceClassName();
    String var = "v_" + out.generateId();

    out.println();
    out.println(className + " " + var
                + " = (" + className + ") "
                + generateSuperGetter("this") + ";");

    Id id = targetType.getId();
    
    out.println("if (" + var + " != null) {");
    out.pushDepth();

    if (isCascade(CascadeType.PERSIST)) {
      out.println(var + " = (" + className + ") aConn.persistFromCascade(" + var + ");");
      out.println(var + ".__caucho_flush();");
    }

    out.print(fieldVar + " = ");
    out.print(id.toObject(id.generateGet(var)));
    out.println(";");
      
    String loadVar = "__caucho_loadMask_" + (_targetLoadIndex / 64);
    long loadMask = (_targetLoadIndex % 64L);

    out.println(loadVar + " |= " + loadMask + "L;");

    out.println(generateSuperSetter("this", var) + ";");
    
    out.popDepth();
    out.println("} else {");
    out.println("  " + fieldVar + " = null;");
    out.println("}");
  }

  /**
   * Generates code for foreign entity create/delete
   */
  @Override
  public void generateInvalidateForeign(JavaWriter out)
    throws IOException
  {
    out.println("if (\"" + _targetType.getTable().getName() + "\".equals(table)) {");
    out.pushDepth();

    String loadVar = "__caucho_loadMask_" + (_targetLoadIndex / 64);

    out.println(loadVar + " = 0L;");
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates any pre-delete code
   */
  @Override
  public void generatePreDelete(JavaWriter out)
    throws IOException
  {
    if (! isTargetCascadeDelete())
      return;

    String var = "caucho_field_" + getName();

    out.println(getJavaTypeName() + " " + var + " = " + getGetterName() + "();");
  }

  /**
   * Generates any pre-delete code
   */
  @Override
  public void generatePostDelete(JavaWriter out)
    throws IOException
  {
    if (! isTargetCascadeDelete())
      return;

    String var = "caucho_field_" + getName();

    out.println("if (" + var + " != null) {");
    out.println("  try {");
    // out.println("    __caucho_session.delete(" + var + ");");
    out.println("    " + var + ".remove();");
    out.println("  } catch (Exception e) {");
    out.println("    throw com.caucho.amber.AmberRuntimeException.create(e);");
    out.println("  }");
    out.println("}");
  }
}
