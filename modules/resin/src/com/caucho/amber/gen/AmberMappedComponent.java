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

package com.caucho.amber.gen;

import com.caucho.VersionFactory;
import com.caucho.amber.field.*;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.MappedSuperclassType;
import com.caucho.amber.type.EntityType;
import com.caucho.bytecode.*;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.ClassComponent;
import com.caucho.loader.Environment;
import com.caucho.util.L10N;
import com.caucho.vfs.PersistentDependency;

import javax.persistence.CascadeType;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Generates the Java code for the wrapped object.
 */
abstract public class AmberMappedComponent extends ClassComponent {
  private static final L10N L = new L10N(AmberMappedComponent.class);

  String _baseClassName;
  String _extClassName;

  EntityType _entityType;

  private ArrayList<PersistentDependency> _dependencies
    = new ArrayList<PersistentDependency>();

  public AmberMappedComponent()
  {
  }

  /**
   * Sets the bean info for the generator
   */
  void setRelatedType(EntityType entityType)
  {
    _entityType = entityType;

    _dependencies.addAll(entityType.getDependencies());

    for (int i = 0; i < _dependencies.size(); i++)
      Environment.addDependency(_dependencies.get(i));
  }

  public EntityType getEntityType()
  {
    return _entityType;
  }

  /**
   * Sets the base class name
   */
  public void setBaseClassName(String baseClassName)
  {
    _baseClassName = baseClassName;
  }

  /**
   * Gets the base class name
   */
  public String getBaseClassName()
  {
    return _baseClassName;
  }

  /**
   * Sets the ext class name
   */
  public void setExtClassName(String extClassName)
  {
    _extClassName = extClassName;
  }

  /**
   * Sets the ext class name
   */
  public String getClassName()
  {
    return _extClassName;
  }

  /**
   * Get bean class name.
   */
  public String getBeanClassName()
  {
    // return _entityType.getBeanClass().getName();
    return _baseClassName;
  }

  /**
   * Returns the dependencies.
   */
  public ArrayList<PersistentDependency> getDependencies()
  {
    return _dependencies;
  }

  protected boolean isEntityParent()
  {
    EntityType parentType = getEntityType().getParentType();

      // jpa/0gg0
    return ((parentType != null) && parentType.isEntity());
  }

  /**
   * Starts generation of the Java code
   */
  @Override
  public final void generate(JavaWriter out)
    throws IOException
  {
    try {
      EntityType parentType = getEntityType().getParentType();

      generateHeader(out, isEntityParent());

      generateInit(out);

      HashSet<Object> completedSet = new HashSet<Object>();

      generatePrologue(out, completedSet);

      generateGetCacheEntity(out);

      generateGetEntityType(out);

      if (! isEntityParent())
        generateGetEntityState(out);

      generateIsLoaded(out);
      
      generateIsDirty(out);

      generateMatch(out);

      generateFields(out);

      generateMethods(out);

      generateDetach(out, isEntityParent());

      generateLoad(out, isEntityParent());

      int min = 0;
      if (isEntityParent())
        min = getEntityType().getParentType().getLoadGroupIndex() + 1;
      int max = getEntityType().getLoadGroupIndex();

      for (int i = min; i <= max; i++)
        generateLoadGroup(out, i);

      generateResultSetLoad(out, isEntityParent());

      generateSetQuery(out, isEntityParent());

      generateMerge(out);

      generateSetLoadMask(out);

      generateMakePersistent(out);

      generateCascadePersist(out);

      generateCascadeRemove(out);

      generateCreate(out);

      generateDelete(out);

      generateDeleteForeign(out);

      generateFlush(out);

      generateIncrementVersion(out);

      generateAfterCommit(out, isEntityParent());

      generateAfterRollback(out);

      generateLoadKey(out);

      generateHome(out);

      // printDependList(out, _dependencies);
    } catch (IOException e) {
      throw e;
    }
  }

  /**
   * Generates the class header for the generated code.
   */
  void generateHeader(JavaWriter out,
                      boolean isEntityParent)
    throws IOException
  {
    out.println("/*");
    out.println(" * Generated by Resin Amber");
    out.println(" * " + VersionFactory.getVersion());
    out.println(" */");
    out.print("private static final java.util.logging.Logger __caucho_log = ");
    out.println("java.util.logging.Logger.getLogger(\"" + getBeanClassName() + "\");");

    // jpa/0ge3 if (! isEntityParent) {
    if (_entityType.getParentType() == null) {
      out.println();
      out.println("protected transient com.caucho.amber.type.EntityType __caucho_home;");
      out.println("public transient com.caucho.amber.entity.EntityItem __caucho_cacheItem;");
      out.println("protected transient com.caucho.amber.manager.AmberConnection __caucho_session;");
      out.println("protected transient com.caucho.amber.entity.EntityState __caucho_state = com.caucho.amber.entity.EntityState.TRANSIENT;");

      // XXX: needs to generate load masks for groups in the subclasses,
      // but the group numbering should not always start at zero.

      int loadCount = _entityType.getLoadGroupIndex();
      for (int i = 0; i <= loadCount / 64; i++) {
        out.println("protected transient long __caucho_loadMask_" + i + ";");
      }

      int dirtyCount = _entityType.getDirtyIndex();

      for (int i = 0; i <= dirtyCount / 64; i++) {
        out.println("protected transient long __caucho_dirtyMask_" + i + ";");
        out.println("protected transient long __caucho_updateMask_" + i + ";");
      }

      out.println("protected transient boolean __caucho_inc_version;");
    }
  }

  /**
   * Generates the init generated code.
   */
  void generateInit(JavaWriter out)
    throws IOException
  {
    if (isEntityParent())
      return;

    String className = getClassName();
    int p = className.lastIndexOf('.');
    if (p > 0)
      className = className.substring(p + 1);

    ArrayList<AmberField> fields = _entityType.getFields();

    Class beanClass = _entityType.getBeanClass();
    for (Constructor ctor : beanClass.getDeclaredConstructors()) {
      out.println();
      // XXX: s/b actual access type?
      out.print("public ");

      out.print(className);
      out.print("(");

      Class []args = ctor.getParameterTypes();
      for (int i = 0; i < args.length; i++) {
        if (i != 0)
          out.print(", ");

        out.printClass(args[i]);
        out.print(" a" + i);
      }
      out.println(")");
      out.println("{");
      out.pushDepth();

      out.print("super(");
      for (int i = 0; i < args.length; i++) {
        if (i != 0)
          out.print(", ");

        out.print("a" + i);
      }
      out.println(");");

      // jpa/0l14
      out.println("__caucho_state = com.caucho.amber.entity.EntityState.TRANSIENT;");

      // jpa/0gh2: compound pk and constructor with arguments.
      if (_entityType.getId() instanceof CompositeId) {
        out.println("try {");
        out.println("  __caucho_setPrimaryKey(__caucho_getPrimaryKey());");
        out.println("} catch (Exception e) {");
        out.println("  __caucho_log.fine(\"amber unable to set primary key within argument constructor \" + this.getClass().getName() + \"[PK: unknown]\");");
        out.println("}");
      }

      for (AmberField field : fields) {
        field.generatePostConstructor(out);
      }

      out.popDepth();
      out.println("}");
    }

    Id id = _entityType.getId();

    if (id == null && _entityType.isEntity())
        throw new IllegalStateException(L.l("'{0}' is missing a key.",
                                            _entityType.getName()));

    boolean isAbstract
      = Modifier.isAbstract(_entityType.getBeanClass().getModifiers());

    out.println();
    out.println("public void __caucho_setPrimaryKey(Object key)");
    out.println("{");
    out.pushDepth();

    if (id != null && ! isAbstract)
      id.generateSet(out, "super", id.generateCastFromObject("key"));

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public Object __caucho_getPrimaryKey()");
    out.println("{");
    out.pushDepth();

    out.println("try {");
    out.pushDepth();

    out.print("return ");

    if (id == null || isAbstract)
      out.print("null");
    else
      out.print(id.toObject(id.generateGet("super")));

    out.println(";");

    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  throw new com.caucho.amber.AmberRuntimeException(e);");
    out.println("}");

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public void __caucho_setConnection(com.caucho.amber.manager.AmberConnection aConn)");
    out.println("{");
    out.println("  __caucho_session = aConn;");
    out.println("}");

    out.println();
    out.println("public com.caucho.amber.manager.AmberConnection __caucho_getConnection()");
    out.println("{");
    out.println("  return __caucho_session;");
    out.println("}");

    generateExpire(out);
  }

  /**
   * Generates the expire code.
   */
  void generateExpire(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public void __caucho_expire()");
    out.println("{");
    out.pushDepth();

    generateLogFine(out, " amber expire");

    out.println();
    int loadCount = _entityType.getLoadGroupIndex();
    for (int i = 0; i <= loadCount / 64; i++) {
      out.println("__caucho_loadMask_" + i + " = 0L;");
    }

    _entityType.generateExpire(out);

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the isDirty code.
   */
  void generateIsDirty(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public boolean __caucho_isDirty()");
    out.println("{");
    out.pushDepth();

    int dirtyCount = _entityType.getDirtyIndex();

    for (int i = 0; i <= dirtyCount / 64; i++) {
      out.println("if (__caucho_dirtyMask_" + i + " != 0L)");
      out.println("  return true;");
      out.println();
    }

    out.println("return false;");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the isLoaded code.
   */
  void generateIsLoaded(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public boolean __caucho_isLoaded()");
    out.println("{");
    out.pushDepth();

    out.println("return __caucho_loadMask_0 != 0L;");

    out.popDepth();
    out.println("}");
  }



  /**
   * Generates the match code.
   */
  void generateMatch(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public boolean __caucho_match(Class cl, Object key)");
    out.println("{");
    out.pushDepth();

    /*
      out.println("if (! (" + getBeanClassName() + ".class.isAssignableFrom(cl)))");
      out.println("  return false;");
    */
    out.println("if (" + getBeanClassName() + ".class  != cl)");
    out.println("  return false;");
    out.println("else {");
    out.pushDepth();

    Id id = _entityType.getId();

    // jpa/0gg0
    if (id == null || Modifier.isAbstract(_entityType.getBeanClass().getModifiers())) {
      // jpa/0ge6: MappedSuperclass

      out.println("return true;");
      out.popDepth();
      out.println("}");
      out.popDepth();
      out.println("}");

      return;
    }

    out.println("try {");
    out.pushDepth();

    id.generateMatch(out, id.generateCastFromObject("key"));

    out.popDepth();
    out.println("} catch (ClassCastException e) {");
    out.println("  throw new IllegalArgumentException(\"Primary key type is incorrect: '\"+key.getClass().getName()+\"'\");");
    out.println("}");

    out.popDepth();
    out.println("}");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the prologue.
   */
  void generatePrologue(JavaWriter out, HashSet<Object> completedSet)
    throws IOException
  {
    if (_entityType.getColumns() != null) {
      for (AmberColumn column : _entityType.getColumns())
        column.generatePrologue(out);
    }

    Id id = _entityType.getId();

    boolean isAbstractParent
      = (_entityType.getParentType() == null
         || ! _entityType.getParentType().isEntity());

    // jpa/0m02
    if (id != null) // && isAbstractParent)
      id.generatePrologue(out, completedSet);

    ArrayList<AmberField> fields = _entityType.getFields();

    for (int i = 0; i < fields.size(); i++) {
      AmberField prop = fields.get(i);

      prop.generatePrologue(out, completedSet);
    }
  }

  /**
   * Generates the __caucho_getCacheEntity()
   */
  void generateGetCacheEntity(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public com.caucho.amber.entity.Entity __caucho_getCacheEntity()");
    out.println("{");
    out.pushDepth();

    out.println("if (__caucho_cacheItem != null)");
    out.println("  return __caucho_cacheItem.getEntity();");
    out.println("else");
    out.println("  return null;");

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public com.caucho.amber.entity.EntityItem __caucho_getCacheItem()");
    out.println("{");
    out.pushDepth();

    out.println("return __caucho_cacheItem;");

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public void __caucho_setCacheItem(com.caucho.amber.entity.EntityItem cacheItem)");
    out.println("{");
    out.pushDepth();

    out.println("__caucho_cacheItem = cacheItem;");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the entity type
   */
  void generateGetEntityType(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public com.caucho.amber.type.EntityType __caucho_getEntityType()");
    out.println("{");
    out.pushDepth();

    out.println("return __caucho_home;");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the get entity state
   */
  void generateGetEntityState(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public com.caucho.amber.entity.EntityState __caucho_getEntityState()");
    out.println("{");
    out.pushDepth();

    out.println("return __caucho_state;");

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public void __caucho_setEntityState(com.caucho.amber.entity.EntityState state)");
    out.println("{");
    out.pushDepth();

    out.println("__caucho_state = state;");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the fields.
   */
  void generateFields(JavaWriter out)
    throws IOException
  {
    if (_entityType.getId() != null) {
      for (AmberField key : _entityType.getId().getKeys()) {
        if (_entityType == key.getSourceType()) {
          key.generateSuperGetterMethod(out);
          key.generateSuperSetterMethod(out);
        }
      }
    }

    for (AmberField prop : _entityType.getFields()) {
      if (_entityType == prop.getSourceType()) {
        prop.generateSuperGetterMethod(out);
        prop.generateSuperSetterMethod(out);

        if (! (prop instanceof IdField)) {
          prop.generateGetterMethod(out);
          prop.generateSetterMethod(out);
        }
      }
    }
  }

  /**
   * Generates the stub methods (needed for EJB)
   */
  void generateMethods(JavaWriter out)
    throws IOException
  {
    for (StubMethod method : _entityType.getMethods()) {
      method.generate(out);
    }
  }

  /**
   * Generates the load
   */
  void generateLoad(JavaWriter out,
                    boolean isEntityParent)
    throws IOException
  {
    // commented out: jpa/0l03
    // if (_entityType.getParentType() != null)
    //   return;

    if (! isEntityParent) {
      out.println();
      out.println("public boolean __caucho_makePersistent(com.caucho.amber.manager.AmberConnection aConn, com.caucho.amber.type.EntityType home)");
      out.println("  throws java.sql.SQLException");
      out.println("{");
      out.pushDepth();

      out.println("__caucho_session = aConn;");
      out.println("if (home != null)");
      out.println("  __caucho_home = home;");

      // XXX: makePersistent is called in contexts other than the P_NON_TRANSACTIONAL one, so this setting is inappropriate
      // out.println("__caucho_state = com.caucho.amber.entity.EntityState.P_NON_TRANSACTIONAL;");

      int loadCount = _entityType.getLoadGroupIndex();
      for (int i = 0; i <= loadCount / 64; i++) {
        out.println("__caucho_loadMask_" + i + " = 0L;");
      }

      int dirtyCount = _entityType.getDirtyIndex();
      for (int i = 0; i <= dirtyCount / 64; i++) {
        out.println("__caucho_dirtyMask_" + i + " = 0L;");
        out.println("__caucho_updateMask_" + i + " = 0L;");
      }

      out.println();
      out.println("return true;");

      out.popDepth();
      out.println("}");
    }

    int index = _entityType.getLoadGroupIndex();

    boolean hasLoad = (_entityType.getFields().size() > 0);

    if (! isEntityParent) {
      index = 0;
      hasLoad = hasLoad || (_entityType.getId() != null);
    }

    // jpa/0l20
    if (true || hasLoad || ! isEntityParent) {
      out.println();
      out.println("public void __caucho_retrieve_eager(com.caucho.amber.manager.AmberConnection aConn)");
      out.println("{");
      out.pushDepth();

      generateRetrieveEager(out, _entityType);

      out.popDepth();
      out.println("}");
      
      out.println();
      out.println("public void __caucho_retrieve_self(com.caucho.amber.manager.AmberConnection aConn)");
      out.println("{");
      out.pushDepth();

      generateRetrieveSelf(out, _entityType);

      out.popDepth();
      out.println("}");
    }
  }

  private void generateRetrieveEager(JavaWriter out, EntityType entityType)
    throws IOException
  {
    if (entityType == null || ! entityType.isEntity())
      return;

    int index = entityType.getLoadGroupIndex();

    boolean hasLoad = (entityType.getFields().size() > 0);

    EntityType parentType = entityType.getParentType();
    if (parentType == null || ! parentType.isEntity()) {
      index = 0;
      hasLoad = true;
    }

    generateRetrieveEager(out, parentType);

    if (hasLoad)
      out.println("__caucho_load_" + index + "(aConn);");
  }

  private void generateRetrieveSelf(JavaWriter out, EntityType entityType)
    throws IOException
  {
    if (entityType == null || ! entityType.isEntity())
      return;

    int index = entityType.getLoadGroupIndex();

    boolean hasLoad = (entityType.getFields().size() > 0);

    EntityType parentType = entityType.getParentType();
    
    if (parentType != null && parentType.isEntity()) {
      generateRetrieveSelf(out, parentType);
    }
    else {
      index = 0;
      hasLoad = true;
    }

    if (hasLoad) {
      int group = index / 64;
      long mask = (1L << (index % 64));
      
      out.println("if ((__caucho_loadMask_" + group + " & " + mask + "L) == 0)");
      
      out.println("  __caucho_load_select_" + index + "(aConn);");
    }
  }

  /**
   * Generates the detach
   */
  void generateDetach(JavaWriter out,
                      boolean isEntityParent)
    throws IOException
  {
    if (isEntityParent)
      return;

    out.println();
    out.println("public void __caucho_detach()");
    out.println("{");
    out.pushDepth();

    generateLogFinest(out, " amber detach");

    out.println();
    out.println("__caucho_session = null;");

    for (AmberField field : _entityType.getFields())
      field.generateDetach(out);

    // jpa/0x00
    // out.println("__caucho_home = null;");

    out.println("__caucho_state = com.caucho.amber.entity.EntityState.TRANSIENT;");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the load group.
   */
  void generateLoadGroup(JavaWriter out, int groupIndex)
    throws IOException
  {
    if (_entityType.hasLoadGroup(groupIndex)) {
      new LoadGroupGenerator(_extClassName,
                             _entityType,
                             groupIndex).generate(out);
    }
  }

  /**
   * Generates the load
   */
  void generateResultSetLoad(JavaWriter out,
                             boolean isEntityParent)
    throws IOException
  {
    if (isEntityParent)
      return;

    out.println();
    out.println("public int __caucho_load(com.caucho.amber.manager.AmberConnection aConn, java.sql.ResultSet rs, int index)");
    out.println("  throws java.sql.SQLException");
    out.println("{");
    out.pushDepth();
    
    int index = _entityType.generateLoad(out, "rs", "index", 0, 0);

    out.println("__caucho_loadMask_0 |= 1L;");

    int dirtyCount = _entityType.getDirtyIndex();

    for (int i = 0; i <= dirtyCount / 64; i++) {
      out.println("__caucho_dirtyMask_" + i + " = 0;");

      // ejb/0645
      // out.println("__caucho_updateMask_" + i + " = 0;");
    }
    
    out.println();
    /* jpa/0g43 - XA doesn't have a cache item
    out.println("if (__caucho_cacheItem == null) {");
    // the cache item does not have its state changed
    out.println("}");
    out.println("else ");
    */
    out.println("if (__caucho_state.isTransactional()) {");
    out.println("}");
    out.println("else if (__caucho_session == null");
    out.println("         || ! __caucho_session.isActiveTransaction()) {");
    out.println("  __caucho_state = com.caucho.amber.entity.EntityState.P_NON_TRANSACTIONAL;");
    out.println("  if (__caucho_cacheItem != null)");
    out.println("    __caucho_cacheItem.save((com.caucho.amber.entity.Entity) this);");
    out.println("}");
    out.println("else {");
    out.println("  __caucho_state = com.caucho.amber.entity.EntityState.P_TRANSACTIONAL;");
    out.println("}");

    if (_entityType.getHasLoadCallback()) {
      out.println();
      out.println("__caucho_load_callback();");
    }

    out.println("return " + index + ";");
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the load
   */
  void generateSetQuery(JavaWriter out,
                        boolean isEntityParent)
    throws IOException
  {
    if (isEntityParent)
      return;

    out.println();
    out.println("public void __caucho_setKey(java.sql.PreparedStatement pstmt, int index)");
    out.println("  throws java.sql.SQLException");
    out.println("{");
    out.pushDepth();

    // jpa/0gg0 vs jpa/06c5
    if (! _entityType.isAbstractClass())
      _entityType.generateSet(out, "pstmt", "index", "super");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the increment version
   */
  void generateIncrementVersion(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public void __caucho_increment_version()");
    out.println("{");
    out.pushDepth();

    VersionField version = _entityType.getVersionField();
    
    if (version != null) {
      out.println("if (__caucho_inc_version)");
      out.println("  return;");
      out.println();
      out.println("__caucho_inc_version = true;");

      version.generateIncrementVersion(out);
    }

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the flush
   */
  void generateFlush(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the update
   */
  void generateFlushUpdate(JavaWriter out,
                           boolean isEntityParent)
    throws IOException
  {
    out.println();
    out.println("protected void __caucho_flushUpdate(long mask, com.caucho.amber.type.EntityType home)");
    out.println("  throws java.sql.SQLException");
    out.println("{");
    out.pushDepth();

    if (isEntityParent) {
      out.println("super.__caucho_flushUpdate(mask, home.getParentType());");
    }

    out.println("String sql = home.generateUpdateSQL(mask);");

    out.println("if (sql != null) {");
    out.pushDepth();

    out.println("java.sql.PreparedStatement pstmt = __caucho_session.prepareStatement(sql);");

    out.println("int index = 1;");

    ArrayList<AmberField> fields = _entityType.getFields();
    for (int i = 0; i < fields.size(); i++) {
      AmberField field = fields.get(i);

      field.generateUpdate(out, "mask", "pstmt", "index");
    }

    out.println();
    _entityType.getId().generateSet(out, "pstmt", "index");

    out.println();
    out.println("pstmt.executeUpdate();");

    out.println();
    generateLogFine(out, " amber update");

    // println();
    // println("pstmt.close();");

    out.popDepth();
    out.println("}");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the after-commit
   */
  void generateAfterCommit(JavaWriter out,
                           boolean isEntityParent)
    throws IOException
  {
    // XXX: needs to handle this with subclasses
    // but there is an issue in the enhancer fixup
    // removing the "super." call.
    // if (_entityType.getParentType() != null) {
    //   out.println();
    //   out.println("public void __caucho_super_afterCommit()");
    //   out.println("{");
    //   out.println("  super.__caucho_afterCommit();");
    //   out.println("}");
    // }

    out.println();
    out.println("public void __caucho_afterCommit()");
    out.println("{");
    out.pushDepth();

    // ejb/06c9
    out.println("com.caucho.amber.entity.EntityState state = __caucho_state;");
    out.println();

    out.println("__caucho_state = com.caucho.amber.entity.EntityState.P_NON_TRANSACTIONAL;");

    out.println();
    out.println("if (__caucho_session == null) {");

    int dirtyCount = _entityType.getDirtyIndex();
    for (int i = 0; i <= dirtyCount / 64; i++) {
      out.println("  __caucho_updateMask_" + i + " = 0L;");
    }

    out.println("  return;");
    out.println("}");
    out.println();

    // jpa/0h20, jpa/0l20, jpa/0l43
    out.println("if (__caucho_cacheItem == null)");
    out.println("  return;");
    out.println();

    // ejb/06c9
    out.println("if (state.isDeleting())");
    out.println("  return;");
    out.println();

    // ejb/06--, ejb/0a-- and jpa/0o04
    int group = 0;
    out.print(getClassName() + " item = (" + getClassName() + ")");
    out.println("__caucho_cacheItem.getEntity();");
    
    out.println("Object pk = __caucho_getPrimaryKey();");
    out.println("item.__caucho_setPrimaryKey(pk);");
    
    _entityType.generateCopyLoadObject(out, "item", "super", group);
    out.println("item.__caucho_loadMask_" + group + " |= __caucho_loadMask_" + group + " & 1L;");

    out.println("__caucho_session.getPersistenceUnit().updateCacheItem((com.caucho.amber.type.EntityType) __caucho_home.getRootType(), pk, __caucho_cacheItem);");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the after-rollback
   */
  void generateAfterRollback(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public void __caucho_afterRollback()");
    out.println("{");
    out.pushDepth();

    out.println("__caucho_state = com.caucho.amber.entity.EntityState.P_NON_TRANSACTIONAL;");

    int loadCount = _entityType.getLoadGroupIndex();
    for (int i = 0; i <= loadCount / 64; i++) {
      out.println("__caucho_loadMask_" + i + " = 0L;");
    }

    int dirtyCount = _entityType.getDirtyIndex();
    for (int i = 0; i <= dirtyCount / 64; i++) {
      out.println("__caucho_dirtyMask_" + i + " = 0L;");
    }

    out.popDepth();
    out.println("}");
  }

  String getDebug()
  {
    return "this";
  }

  /**
   * Generates the update
   */
  void generateCreate(JavaWriter out)
    throws IOException
  {
    boolean isAbstract
      = Modifier.isAbstract(_entityType.getBeanClass().getModifiers());
    
    boolean isGeneratedValue = false;

    // jpa/0gg0
    if (_entityType.getId() != null && ! isAbstract) {
      ArrayList<IdField> fields = _entityType.getId().getKeys();
      IdField idField = fields.size() > 0 ? fields.get(0) : null;

      boolean hasReturnGeneratedKeys = false;

      try {
        hasReturnGeneratedKeys = _entityType.getPersistenceUnit().hasReturnGeneratedKeys();
      } catch (Exception e) {
        // Meta-data exception which is acceptable or
        // no data-source configured. The latter will
        // be thrown on web.xml validation. (ejb/06m0)
      }

      if (idField != null && idField.isAutoGenerate())
        isGeneratedValue = true;

      if (! hasReturnGeneratedKeys &&
          idField != null && idField.getType().isAutoIncrement()) {
        out.println();
        out.println("private static com.caucho.amber.field.Generator __caucho_id_gen;");
        out.println("static {");
        out.pushDepth();
        out.println("com.caucho.amber.field.MaxGenerator gen = new com.caucho.amber.field.MaxGenerator();");
        out.print("gen.setColumn(\"");
        out.printJavaString(idField.getColumns().get(0).generateInsertName());
        out.println("\");");
        out.print("gen.setTable(\"");
        out.printJavaString(_entityType.getName());
        out.println("\");");
        out.println("gen.init();");
        out.popDepth();
        out.println("}");
      }
    }

    // jpa/0ga2
    out.println();
    out.println("public boolean __caucho_lazy_create(com.caucho.amber.manager.AmberConnection aConn, com.caucho.amber.type.EntityType home)");
    out.println("  throws java.sql.SQLException");
    out.println("{");
    out.pushDepth();

    int loadCount = 0;

    // jpa/0ge2: MappedSuperclassType
    if ((_entityType.getTable() == null) || (_entityType.getId() == null)) {
      out.println("return false;");

      out.popDepth();
      out.println("}");
    }
    else {
      out.println("if (__caucho_session != null)");
      out.println("  return true;");
      out.println();

      // commented out: jpa/0h25
      // out.println("  throw new com.caucho.amber.AmberException(\"object \" + " + getDebug() + " + \" is already persistent.\");");

      out.println("__caucho_state = com.caucho.amber.entity.EntityState.P_PERSISTING;");

      loadCount = _entityType.getLoadGroupIndex();
      for (int i = 0; i <= loadCount / 64; i++) {
        out.println("__caucho_loadMask_" + i + " = 0L;");

        // XXX: jpa/0l21

        EntityType parentType = _entityType;

        do {
          out.println("__caucho_loadMask_" + i + " |= " + parentType.getCreateLoadMask(i) + ";");
        } while ((parentType = parentType.getParentType()) != null);
      }

      out.println();
      out.println("__caucho_session = aConn;");
      out.println("__caucho_home = home;");

      _entityType.generatePrePersist(out);

      //out.println();
      //out.println("__caucho_home.prePersist(this);");

      out.println();
      
      // jpa/0r20
      for (Method method : _entityType.getPrePersistCallbacks()) {
        out.println(method.getName() + "();");
      }

      if (isGeneratedValue) {
        // jpa/0g50: generated id needs to flush the insert statement at persist() time.
        out.println("__caucho_create(aConn, home);");
      }
      else {
        // jpa/0j5e: persist() is lazy but should cascade to add entities to the context.
        //out.println();
        //out.println("__caucho_cascadePrePersist(aConn);");

        out.println("__caucho_cascadePostPersist(aConn);");
      }

      out.println("__caucho_home.postPersist(this);");

      for (Method method : _entityType.getPostPersistCallbacks()) {
        out.println(method.getName() + "();");
      }

      out.println();
      out.println("return true;");

      out.popDepth();
      out.println("}");
    }

    out.println();
    out.println("public boolean __caucho_create(com.caucho.amber.manager.AmberConnection aConn, com.caucho.amber.type.EntityType home)");
    out.println("  throws java.sql.SQLException");
    out.println("{");
    out.pushDepth();

    // jpa/0ge2: MappedSuperclassType
    if ((_entityType.getTable() == null) || (_entityType.getId() == null)) {
      out.println("return false;");

      out.popDepth();
      out.println("}");

      return;
    }

    out.println("if (__caucho_state != com.caucho.amber.entity.EntityState.P_PERSISTING)");
    out.println("  return false;");

    out.println();
    out.println("__caucho_state = com.caucho.amber.entity.EntityState.P_PERSISTED;");

    out.println();
    out.println("__caucho_cascadePrePersist(aConn);");

    int dirtyCount = _entityType.getDirtyIndex();
    for (int i = 0; i <= dirtyCount / 64; i++) {
      out.println("__caucho_dirtyMask_" + i + " = 0L;");
    }

    AmberTable table = _entityType.getTable();

    String sql = null;

    out.println("String sql;");

    boolean isAutoInsert = false;
    
    if (_entityType.getId() != null
        && ! isAbstract
        && _entityType.getId().isIdentityGenerator()) {
      isAutoInsert = true;
    }
    
    out.println("int index = 1;");

    _entityType.getId().generateCheckCreateKey(out);

    out.println("java.sql.PreparedStatement pstmt;");

    // jpa/0gg0, jpa/0gh0
    if (isAutoInsert) {
      out.println("if (__caucho_home.isIdentityGenerator()) {");
      out.pushDepth();
      
      out.print("sql = \"");
      out.printJavaString(_entityType.generateAutoCreateSQL(table));
      out.println("\";");

      out.println("pstmt = aConn.prepareInsertStatement(sql, true);");
      out.popDepth();
      out.println("} else {");
      out.pushDepth();
    }

    out.print("sql = \"");
    out.printJavaString(_entityType.generateCreateSQL(table));
    out.println("\";");
    
    out.println("pstmt = aConn.prepareInsertStatement(sql, false);");
    
    if (isAutoInsert) {
      out.popDepth();
      out.println("}");
    }

    _entityType.getId().generateSetInsert(out, "pstmt", "index");
    _entityType.generateInsertSet(out, table, "pstmt", "index", "super");

    out.println();
    out.println("pstmt.executeUpdate();");

    out.println();
    _entityType.getId().generateSetGeneratedKeys(out, "pstmt");

    EntityType parentType = _entityType;

    do {
      for (AmberTable subTable : parentType.getSecondaryTables()) {
        sql = parentType.generateCreateSQL(subTable);

        out.println();
        out.print("sql = \"");
        out.printJavaString(sql);
        out.println("\";");

        out.println("pstmt = aConn.prepareStatement(sql);");

        out.println("index = 1;");

        out.println();
        parentType.getId().generateSetInsert(out, "pstmt", "index");

        parentType.generateInsertSet(out, subTable, "pstmt", "index", "super");

        out.println();
        out.println("pstmt.executeUpdate();");

        out.println();
        parentType.getId().generateSetGeneratedKeys(out, "pstmt");
      }
    } while ((parentType = parentType.getParentType()) != null);

    // println("pstmt.close();");

    out.println("__caucho_cacheItem = new com.caucho.amber.entity.CacheableEntityItem(home.getHome(), new " + getClassName() + "());");

    out.println(getClassName() + " cacheEntity = (" + getClassName() + ") __caucho_cacheItem.getEntity();");
    out.println("cacheEntity.__caucho_home = home;");

    Id id = _entityType.getId();

    out.println("Object pk = null;");

    if (! id.isEmbeddedId()) {
      ArrayList<IdField> keys = id.getKeys();

      for (IdField key : keys) {
        String value;

        if (keys.size() == 1)
          value = key.getType().generateCastFromObject("(pk = __caucho_getPrimaryKey())");
        else
          value = key.generateGet("super");

        out.println(key.generateSet("cacheEntity", value) + ";");
      }
    }
    else {
      // jpa/0gh0

      id.generateCopy(out, "cacheEntity", "this");

      // out.println("pk = __caucho_compound_key;");

      // out.println(id.getEmbeddedIdField().generateStatementSet("cacheEntity", "__caucho_compound_key") + ";");
    }

    out.println("try {");
    out.pushDepth();

    // jpa/0o01
    out.println("Object child;");

    // jpa/0l21
    for (int i = 0; i <= loadCount; i++) {
      _entityType.generateCopyLoadObject(out, "cacheEntity", "super", i);
    }

    out.popDepth();
    out.println("} catch (RuntimeException e) {");
    out.println("  throw e;");
    out.println("} catch (Exception e) {");
    out.println("  throw new com.caucho.amber.AmberRuntimeException(e);");
    out.println("}");

    parentType = _entityType;

    // jpa/0l21
    for (int i = 0; i <= loadCount / 64; i++) {
      out.println("cacheEntity.__caucho_loadMask_" + i + " = 0L;");

      do {
        out.println("cacheEntity.__caucho_loadMask_" + i + " |= " + parentType.getCreateLoadMask(i) + ";");
      }
      while ((parentType = parentType.getParentType()) != null);
    }

    out.println();
    out.println("if (pk == null)");
    out.println("  pk = __caucho_getPrimaryKey();");

    // jpa/0i5e, jpa/1641
    // caching entity must only occur after the commit completes to
    // handle rollbacks and also so the cache doesn't have an
    // item in the middle of a transaction
    /*
      out.println();
      out.println("aConn.getPersistenceUnit().putEntity((com.caucho.amber.type.EntityType) __caucho_home.getRootType(),");
      out.println("                                     pk, __caucho_cacheItem);");
    */

    out.println();
    generateLogFine(out, " amber create");

    out.println();
    out.println("return false;");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the delete
   */
  void generateDelete(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the foreign delete
   */
  void generateDeleteForeign(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public void __caucho_invalidate_foreign(String table, Object key)");
    out.println("{");
    out.pushDepth();

    _entityType.generateInvalidateForeign(out);

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the create
   */
  void generateMerge(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public void __caucho_mergeFrom(AmberConnection aConn,");
    out.println("                               Entity sourceEntity)");
    out.println("{");
    out.pushDepth();

    out.println(getClassName() + " source = (" + getClassName() + ") sourceEntity;");

    _entityType.generateMergeFrom(out, "this", "source");

    // XXX: can't be right
    /*
    out.println();
    out.println("try {");
    out.pushDepth();

    // jpa/1622
    // out.println("targetEntity.__caucho_cascadePostPersist(aConn);");

    out.popDepth();
    out.println("} catch (java.sql.SQLException e) {");
    out.println("  throw new com.caucho.amber.AmberRuntimeException(e);");
    out.println("}");
    */

    // jpa/1900
    //generateLogFine(out, " merged");

    out.popDepth();
    out.println("}");
  }

  void generateSetLoadMask(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the copy
   */
  void generateMakePersistent(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public void __caucho_makePersistent(com.caucho.amber.manager.AmberConnection aConn,");
    out.println("                                    com.caucho.amber.entity.EntityItem cacheItem)");
    out.println("{");
    out.pushDepth();

    out.println(_extClassName + " entity = (" + _extClassName + ") cacheItem.getEntity();");

    out.println("__caucho_home = entity.__caucho_home;");
    out.println("if (__caucho_home == null) throw new NullPointerException();");
    out.println("__caucho_cacheItem = cacheItem;");

    // jpa/0ge6: MappedSuperclass
    if (_entityType.getId() != null) {
      _entityType.getId().generateCopy(out, "super", "entity");
    }

    out.println("__caucho_session = aConn;");

    out.println("__caucho_state = com.caucho.amber.entity.EntityState.P_NON_TRANSACTIONAL;");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the cascade persist
   */
  void generateCascadePersist(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public void __caucho_cascadePrePersist(com.caucho.amber.manager.AmberConnection aConn)");
    out.println("  throws java.sql.SQLException");
    out.println("{");
    out.pushDepth();

    // jpa/0i60
    /*
      out.println("if (__caucho_state == com.caucho.amber.entity.EntityState.P_TRANSACTIONAL)");
      out.println("  __caucho_state = com.caucho.amber.entity.EntityState.P_PERSIST;");
    */

    // out.println("if (aConn == null)");
    // out.println("  throw new com.caucho.amber.AmberException(\"Null AmberConnection when object \" + " + getDebug() + " + \" is trying to cascade persist child objects.\");");

    ArrayList<AmberField> fields = _entityType.getFields();

    for (int i = 0; i < fields.size(); i++) {
      AmberField field = fields.get(i);

      if (field.isCascadable()) {
        CascadableField cascadable = (CascadableField) field;

        out.println();

        cascadable.generatePreCascade(out, "aConn", CascadeType.PERSIST);
      }
    }

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public void __caucho_cascadePostPersist(com.caucho.amber.manager.AmberConnection aConn)");
    out.println("  throws java.sql.SQLException");
    out.println("{");
    out.pushDepth();

    for (int i = 0; i < fields.size(); i++) {
      AmberField field = fields.get(i);

      if (field.isCascadable()) {
        CascadableField cascadable = (CascadableField) field;

        out.println();

        cascadable.generatePostCascade(out, "aConn", CascadeType.PERSIST);
      }
    }

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the cascade remove
   */
  void generateCascadeRemove(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public void __caucho_cascadePreRemove(com.caucho.amber.manager.AmberConnection aConn)");
    out.println("  throws java.sql.SQLException");
    out.println("{");
    out.pushDepth();

    ArrayList<AmberField> fields = _entityType.getFields();

    for (int i = 0; i < fields.size(); i++) {
      AmberField field = fields.get(i);

      if (field.isCascadable()) {
        CascadableField cascadable = (CascadableField) field;

        out.println();

        cascadable.generatePreCascade(out, "aConn", CascadeType.REMOVE);
      }
    }

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public void __caucho_cascadePostRemove(com.caucho.amber.manager.AmberConnection aConn)");
    out.println("  throws java.sql.SQLException");
    out.println("{");
    out.pushDepth();

    for (int i = 0; i < fields.size(); i++) {
      AmberField field = fields.get(i);

      if (field.isCascadable()) {
        CascadableField cascadable = (CascadableField) field;

        out.println();

        cascadable.generatePostCascade(out, "aConn", CascadeType.REMOVE);
      }
    }

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the home methods
   */
  void generateHome(JavaWriter out)
    throws IOException
  {
    generateHomeFind(out);

    boolean generateHomeNew = true;

    // jpa/0ge2
    if (_entityType instanceof MappedSuperclassType)
      generateHomeNew = false;

    /* XXX
       if (_entityType instanceof SubEntityType) {
         SubEntityType sub = (SubEntityType) _entityType;

         // jpa/0ge2
         if (! sub.isParentMappedSuperclass()) {
           if (! sub.getParentType().isAbstractClass())
             generateHomeNew = false;
         }
       }
    */

    if (generateHomeNew) {
      generateHomeNew(out);
      generateHomeFindNew(out);
    }
  }

  /**
   * Generates the load key.
   */
  void generateLoadKey(JavaWriter out)
    throws IOException
  {
    out.println();
    out.print("public Object __caucho_load_key(");
    out.print("com.caucho.amber.manager.AmberConnection aConn,");
    out.println("java.sql.ResultSet rs, int index)");
    out.println("  throws java.sql.SQLException");
    out.println("{");
    out.pushDepth();

    boolean isAbstract
      = Modifier.isAbstract(_entityType.getBeanClass().getModifiers());

    // jpa/0gg0
    if (_entityType.getId() == null || isAbstract) {
      // jpa/0ge6: MappedSuperclass

      out.println("return null;");
      out.popDepth();
      out.println("}");

      return;
    }

    out.print("return ");
    int index = _entityType.getId().generateLoadForeign(out, "rs", "index", 0);
    out.println(";");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the home methods
   */
  void generateHomeFind(JavaWriter out)
    throws IOException
  {
    out.println();
    out.print("public com.caucho.amber.entity.EntityItem __caucho_home_find(");
    out.print("com.caucho.amber.manager.AmberConnection aConn,");
    out.print("com.caucho.amber.entity.AmberEntityHome home,");
    out.println("java.sql.ResultSet rs, int index)");
    out.println("  throws java.sql.SQLException");
    out.println("{");
    out.pushDepth();

    boolean isAbstract
      = Modifier.isAbstract(_entityType.getBeanClass().getModifiers());

    if (_entityType.getId() == null || isAbstract) {
      // jpa/0ge6: MappedSuperclass

      out.println("return null;");
      out.popDepth();
      out.println("}");

      return;
    }

    out.print("Object key = ");
    int index = _entityType.getId().generateLoadForeign(out, "rs", "index", 0);
    out.println(";");

    if (_entityType.getDiscriminator() == null) {
      out.println("return aConn.loadCacheItem(home.getJavaClass(), key, home);");
    }
    else {
      out.println("String discriminator = rs.getString(index + " + index + ");");
      out.println();
      out.println("return home.findDiscriminatorEntityItem(aConn, key, discriminator);");
    }

    out.popDepth();
    out.println("}");
  }

  void generateHomeNew(JavaWriter out)
    throws IOException
  {
    out.println();
    out.print("public com.caucho.amber.entity.Entity __caucho_home_new(");
    out.print("com.caucho.amber.entity.AmberEntityHome home");
    out.println(", Object key");
    out.println(", AmberConnection aConn");
    out.println(", EntityItem cacheItem");
    out.println(")");
    out.println("{");
    out.pushDepth();

    if (_entityType.isAbstractClass() || _entityType.getId() == null) {
      out.println("return null;");
      out.popDepth();
      out.println("}");
      return;
    }

    out.println(getClassName() + " entity = new " + getClassName() + "();");

    out.println("entity.__caucho_home = home.getEntityType();");
    out.println("entity.__caucho_setPrimaryKey(key);");
    out.println("entity.__caucho_session = aConn;");
    out.println("entity.__caucho_cacheItem = cacheItem;");

    out.println("return entity;");

    out.popDepth();
    out.println("}");
  }

  void generateHomeFindNew(JavaWriter out)
    throws IOException
  {
    EntityType parentType = _entityType.getParentType();

    // jpa/0ge3
    // jpa/0l32: find(SubBean.class, "2") would try to select the
    // discriminator column from the "sub-table".
    if (isEntityParent())
      return;

    out.println();
    out.print("public com.caucho.amber.entity.Entity __caucho_home_find(");
    out.print("com.caucho.amber.manager.AmberConnection aConn, ");
    out.print("com.caucho.amber.entity.AmberEntityHome home, ");
    out.println("Object key)");
    out.println("{");
    out.pushDepth();

    AmberColumn discriminator = _entityType.getDiscriminator();

    if (_entityType.isAbstractClass()
        || _entityType.getId() == null
        || discriminator == null) {
      out.println("return __caucho_home_new(home, key, aConn, null);");
      out.popDepth();
      out.println("}");
      return;
    }

    String rootTableName = _entityType.getRootTableName();

    if (rootTableName == null)
      rootTableName = "";

    out.println("java.sql.ResultSet rs = null;");
    out.println("java.sql.PreparedStatement pstmt = null;");
    out.println("String sql = null;");
    out.println();
    out.println("try {");
    out.pushDepth();

    String keyType = _entityType.getId().getForeignTypeName();

    String discriminatorVar = "discriminator";

    out.println("String " + discriminatorVar + " = null;");

    generateHomeNewLoading(out, discriminatorVar);

    out.println("com.caucho.amber.entity.Entity entity = home.newDiscriminatorEntity(key, " + discriminatorVar + ");");

    out.println("entity.__caucho_load(aConn, rs, 1);");

    out.println("return entity;");

    out.popDepth();
    out.println("} catch (RuntimeException e) {");
    out.println("  throw e;");
    out.println("} catch (Exception e) {");
    out.println("  throw new com.caucho.amber.AmberRuntimeException(e);");
    out.println("} finally {");
    /*
      out.println("  if (rs != null)");
      out.println("    rs.close();");
      out.println("  if (pstmt != null)");
      out.println("    aConn.closeStatement(sql);");
    */
    out.println("}");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the loading for home_new
   */
  void generateHomeNewLoading(JavaWriter out,
                              String discriminatorVar)
    throws IOException
  {
    out.print("sql = \"select ");

    EntityType parentType = _entityType;

    /* XXX: jpa/0gg3
    // jpa/0l32
    if (_entityType.getDiscriminator() != null) {
      while (parentType.getParentType() != null)
        parentType = parentType.getParentType();
    }
    */

    out.printJavaString(parentType.generateLoadSelect("o"));
    out.print(" from ");

    /*
      if (rootTableName == null)
        out.print(_entityType.getTable().getName());
      else
        out.print(rootTableName);
    */
    out.printJavaString(_entityType.getTable().getName());

    out.print(" o where ");
    // jpa/0s27
    out.printJavaString((parentType.getId().generateMatchArgWhere("o")));
    out.println("\";");

    out.println("pstmt = aConn.prepareStatement(sql);");

    String keyType = _entityType.getId().getForeignTypeName();

    out.println(keyType + " " + "keyValue = (" + keyType + ") key;");

    out.println("int index = 1;");
    _entityType.getId().generateSetKey(out, "pstmt", "index", "keyValue");

    out.println("rs = pstmt.executeQuery();");
    out.println("if (rs.next()) {");
    out.println("  " + discriminatorVar + " = rs.getString(1);"); // XXX:

    out.println("}");
  }

  void generateCallbacks(JavaWriter out,
                         String object,
                         ArrayList<Method> callbacks)
    throws IOException
  {
    if (callbacks.size() > 0) {

      out.println();

      for (Method method : callbacks) {
        out.println(object + "." + method.getName() + "();");
      }
    }
  }

  protected void generateLogFine(JavaWriter out, String msg)
    throws IOException
  {
    out.println("if (__caucho_log.isLoggable(java.util.logging.Level.FINE))");
    out.print("  __caucho_log.fine(");
    out.print("getClass().getName() + \"[\" + __caucho_getPrimaryKey() + \"]\"");
    out.print(" + \"");
    out.printJavaString(msg);
    out.println("\");");
  }

  protected void generateLogFinest(JavaWriter out, String msg)
    throws IOException
  {
    out.println("if (__caucho_log.isLoggable(java.util.logging.Level.FINEST))");
    out.print("  __caucho_log.finest(");
    out.print("getClass().getName() + \"[\" + __caucho_getPrimaryKey() + \"]\"");
    out.print(" + \"");
    out.printJavaString(msg);
    out.println("\");");
  }
}
