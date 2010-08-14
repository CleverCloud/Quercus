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

import com.caucho.amber.field.*;
import com.caucho.amber.type.*;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

/**
 * Generates the Java code for the wrapped object.
 */
public class EntityComponent extends AmberMappedComponent {
  private static final L10N L = new L10N(EntityComponent.class);

  public EntityComponent()
  {
  }

  /**
   * Gets the entity type.
   */
  @Override
  public EntityType getEntityType()
  {
    return (EntityType) _entityType;
  }

  /**
   * Sets the bean info for the generator
   */
  public void setEntityType(EntityType entityType)
  {
    setRelatedType(entityType);
  }

  /**
   * Generates the delete
   */
  @Override
  void generateDelete(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public void __caucho_delete()");
    out.println("{");
    out.pushDepth();

    out.println("if (com.caucho.amber.entity.EntityState.P_DELETING.ordinal() <= __caucho_state.ordinal())");
    out.println("  return;");
    out.println();

    out.println("if (__caucho_home != null)");
    out.println("  __caucho_home.preRemove(this);");
    out.println();

    generateCallbacks(out, "this", _entityType.getPreRemoveCallbacks());

    _entityType.generatePreDelete(out);

    out.println("__caucho_state = com.caucho.amber.entity.EntityState.P_DELETING;");

    out.println("if (__caucho_session != null) {");
    out.pushDepth();
    out.println("__caucho_session.update((com.caucho.amber.entity.Entity) this);");
    out.println("__caucho_home.getTable().beforeEntityDelete(__caucho_session, (com.caucho.amber.entity.Entity) this);");
    out.println("__caucho_state = com.caucho.amber.entity.EntityState.P_DELETED;");
    _entityType.generatePostDelete(out);
    out.popDepth();
    out.println("}");
    out.println("else");
    out.println("  __caucho_state = com.caucho.amber.entity.EntityState.P_DELETED;");

    out.popDepth();
    out.println("}");

    Id id = _entityType.getId();

    out.println();
    out.println("private void __caucho_delete_int()");
    out.println("  throws java.sql.SQLException");
    out.println("{");
    out.pushDepth();

    // jpa/0ge2: MappedSuperclassType
    if ((_entityType.getTable() == null) || (id == null)) {
      out.println("return;");

      out.popDepth();
      out.println("}");

      return;
    }

    out.println("java.sql.PreparedStatement pstmt = null;");
    out.println("String sql = null;");
    out.println();

    out.println("try {");
    out.pushDepth();

    out.print("__caucho_home.delete(__caucho_session, ");
    out.print(id.toObject(id.generateGet("this")));
    out.println(");");

    out.println("__caucho_session.removeEntity((com.caucho.amber.entity.Entity) this);");

    String table = _entityType.getTable().getName();
    String where = _entityType.getId().generateMatchArgWhere(null);

    String sql = ("delete from " + table + " where " + where);

    out.print("sql = \"");
    out.printJavaString(sql);
    out.println("\";");

    out.println();
    out.println("pstmt = __caucho_session.prepareStatement(sql);");

    out.println("int index = 1;");
    id.generateSet(out, "pstmt", "index", "this");

    out.println();
    out.println("pstmt.executeUpdate();");

    out.println("__caucho_home.postRemove(this);");

    generateCallbacks(out, "this", _entityType.getPostRemoveCallbacks());

    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  if (pstmt != null)");
    out.println("    __caucho_session.closeStatement(sql);");
    out.println();
    out.println("  if (e instanceof java.sql.SQLException)");
    out.println("    throw (java.sql.SQLException) e;");
    out.println();
    out.println("  if (e instanceof RuntimeException)");
    out.println("    throw (RuntimeException) e;");
    out.println();
    out.println("  throw new com.caucho.amber.AmberRuntimeException(e);");
    out.println("}");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the flush
   */
  @Override
  void generateFlush(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("protected void __caucho_flush_callback()");
    out.println("  throws java.sql.SQLException");
    out.println("{");
    out.println("}");

    out.println();
    out.println("public boolean __caucho_flush()");
    out.println("  throws java.sql.SQLException");
    out.println("{");
    out.pushDepth();

    boolean isAbstract
      = Modifier.isAbstract(_entityType.getBeanClass().getModifiers());

    if (_entityType.getId() == null || isAbstract) {
      // jpa/0ge6: MappedSuperclass

      out.println("return false;");
      out.popDepth();
      out.println("}");

      return;
    }

    out.println("if (__caucho_session == null)");
    out.println("  return false;");
    out.println();

    ArrayList<AmberField> fields = _entityType.getFields();

    for (int i = 0; i < fields.size(); i++) {
      AmberField field = fields.get(i);

      if (field.isCascadable()) {
        CascadableField cascadable = (CascadableField) field;

        cascadable.generateFlushCheck(out);

        out.println();
      }
    }

    out.println();
    out.println("if (__caucho_state == com.caucho.amber.entity.EntityState.P_DELETED) {");
    out.println("  __caucho_delete_int();");
    out.println("  return true;");
    out.println("}");
    out.println("else if (__caucho_state == com.caucho.amber.entity.EntityState.P_PERSISTING) {");
    // jpa/0ga2
    out.println("  __caucho_create(__caucho_session, __caucho_home);");
    out.println("}");
    out.println("else if (__caucho_state == com.caucho.amber.entity.EntityState.P_PERSISTED) {");
    out.println("  __caucho_cascadePrePersist(__caucho_session);");
    out.println("}");
    out.println();

    out.println("boolean isDirty = false;");

    int dirtyCount = _entityType.getDirtyIndex();

    for (int i = 0; i <= dirtyCount / 64; i++) {
      out.println("long mask_" + i + " = __caucho_dirtyMask_" + i + ";");
      out.println("__caucho_dirtyMask_" + i + " = 0L;");
      out.println("__caucho_updateMask_" + i + " |= mask_" + i + ";");

      out.println();
      out.println("if (mask_" + i + " != 0L)");
      out.println("  isDirty = true;");
    }

    out.println();

    // if (version == null)
    out.println("if (isDirty) {");
    out.pushDepth();

    // ejb/0605
    out.println();
    out.println("__caucho_flush_callback();");

    // else {
    // jpa/0x02
    //  out.println("if (! (isDirty || " + version.generateIsNull() + "))");
    // }
    // out.println("  return true;");

    // jpa/0r10
    out.println("__caucho_home.preUpdate(this);");

    // jpa/0r10
    generateCallbacks(out, "this", _entityType.getPreUpdateCallbacks());

    out.println("com.caucho.util.CharBuffer cb = new com.caucho.util.CharBuffer();");

    out.println("__caucho_home.generateUpdateSQLPrefix(cb);");

    out.println("boolean isFirst = true;");

    VersionField version = _entityType.getVersionField();

    for (int i = 0; i <= dirtyCount / 64; i++) {
      // jpa/0x02 is a negative test.
      if (i != 0 || version == null) {
        out.println("if (mask_" + i + " != 0L)");
        out.print("  ");
      }

      out.println("isFirst = __caucho_home.generateUpdateSQLComponent(cb, " + i + ", mask_" + i + ", isFirst);");
    }
    out.println("__caucho_home.generateUpdateSQLSuffix(cb);");

    out.println();
    out.println("java.sql.PreparedStatement pstmt = null;");
    out.println("String sql = cb.toString();");
    out.println();
    out.println("try {");
    out.pushDepth();

    out.println("pstmt = __caucho_session.prepareStatement(sql);");

    out.println("int index = 1;");

    for (int i = 0; i < fields.size(); i++) {
      AmberField field = fields.get(i);

      field.generateUpdate(out, "mask", "pstmt", "index");
    }

    out.println();
    _entityType.getId().generateStatementSet(out, "pstmt", "index");

    if (version != null) {
      out.println();
      version.generateStatementSet(out, "pstmt", "index");
    }

    out.println();
    out.println("int updateCount = pstmt.executeUpdate();");
    out.println();

    if (version != null) {
      out.println("if (updateCount == 0) {");
      out.println("  throw new javax.persistence.OptimisticLockException((com.caucho.amber.entity.Entity) this);");
      out.println("} else {");
      out.pushDepth();
      String value = version.generateGet("super");
      AmberType type = version.getColumn().getType();
      out.println(version.generateSuperSetter("this", type.generateIncrementVersion(value)) + ";");
      out.popDepth();
      out.println("}");
      out.println();
    }

    out.println("__caucho_home.postUpdate(this);");

    generateCallbacks(out, "this", _entityType.getPostUpdateCallbacks());

    out.println();
    generateLogFine(out, " amber update");

    out.println();
    out.println("__caucho_inc_version = false;");
    out.println();

    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  if (pstmt != null)");
    out.println("    __caucho_session.closeStatement(sql);");
    out.println();
    out.println("  if (e instanceof java.sql.SQLException)");
    out.println("    throw (java.sql.SQLException) e;");
    out.println();
    out.println("  if (e instanceof RuntimeException)");
    out.println("    throw (RuntimeException) e;");
    out.println();
    out.println("  throw new com.caucho.amber.AmberRuntimeException(e);");
    out.println("}");

    out.popDepth();
    out.println("}");

    out.println("if (__caucho_state == com.caucho.amber.entity.EntityState.P_PERSISTED) {");
    out.println("  __caucho_cascadePostPersist(__caucho_session);");
    out.println("}");

    out.println();
    out.println("return false;");

    out.popDepth();
    out.println("}");
  }
}
