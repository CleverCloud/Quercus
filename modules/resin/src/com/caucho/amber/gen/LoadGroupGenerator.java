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

import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.*;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.ClassComponent;
import com.caucho.util.L10N;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Generates the Java code for the wrapped object.
 */
public class LoadGroupGenerator extends ClassComponent {
  private static final L10N L = new L10N(LoadGroupGenerator.class);

  private String _extClassName;
  private EntityType _entityType;
  private int _index;

  public LoadGroupGenerator(String extClassName,
                            EntityType entityType,
                            int index)
  {
    _extClassName = extClassName;
    _entityType = entityType;
    _index = index;
  }

  /**
   * Generates the load group.
   */
  @Override
  public void generate(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("protected void __caucho_load_" + _index +  "(com.caucho.amber.manager.AmberConnection aConn)");
    out.println("{");
    out.pushDepth();

    int group = _index / 64;
    long mask = (1L << (_index % 64));

    out.println("boolean isLoaded = (__caucho_loadMask_" + group
                + " & " + mask + "L) != 0;");
    
    // jpa/0ge2: MappedSuperclassType
    if (_entityType.getTable() != null) {

      int min = 0;

      if (_entityType.getParentType() == null)
        min = _index;

      // XXX: need to do another check for a long hierarchy and/or many-to-one
      // if ((_entityType.getParentType() != null) &&
      //     (_index = _entityType.getParentType().getLoadGroupIndex() + 1)) {
      //   min = _entityType.getParentType().getLoadGroupIndex();
      // }

      int max = _index;

      generateTransactionChecks(out, group, mask, min, max);

      if (min <= max) {
        out.println("else {");
        out.pushDepth();
      }

      for (int i = min; i <= max; i++) {
        // jpa/0l48: inheritance optimization.
        out.println("if ((__caucho_loadMask_" + group + " & " + (1L << (i % 64)) + "L) == 0)");
        out.println("  __caucho_load_select_" + i + "(aConn);");
      }

      if (min <= max) {
        out.popDepth();
        out.println("}");
      }

      out.println();

      _entityType.generatePostLoadSelect(out, 1, _index);

      // jpa/0o09
      // needs to be after load to prevent loop if toString() expects data
      out.println();
      out.println("if (__caucho_log.isLoggable(java.util.logging.Level.FINER))");
      out.println("  __caucho_log.finer(getClass().getSimpleName() + \"[\" + __caucho_getPrimaryKey() + \"] amber load-" + _index + "\");");


      out.println();
      out.println("if (! isLoaded) {");
      out.pushDepth();

      // ejb/06j2, ejb/0690
      if (_entityType.getHasLoadCallback() && _index == 0) {
        out.println();
        out.println("__caucho_load_callback();");
      }

      // ejb/069a, jpa/0r00 vs. jpa/0r01
      out.println();
      out.println("if (__caucho_home != null)");
      out.println("  __caucho_home.postLoad(this);");

      // jpa/0r01: @PostLoad, with transaction.
      // Within a transaction the entity is not copied
      // directly from cache, so we need to invoke the
      // callbacks after load. For jpa/0r00, see AmberMappedComponent.
      generateCallbacks(out, _entityType.getPostLoadCallbacks());

      out.popDepth();
      out.println("}");
    }

    out.popDepth();
    out.println("}");

    if (_index == 0 && _entityType.getHasLoadCallback()) {
      out.println();
      out.println("protected void __caucho_load_callback() {}");
    }

    generateLoadSelect(out, group, mask);

    if (_index == 0)
      generateLoadNative(out);
  }

  private void generateLoadNative(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public void __caucho_load_native(java.sql.ResultSet rs, String []columnNames)");
    out.println("  throws java.sql.SQLException");
    out.println("{");
    out.pushDepth();

    _entityType.generateLoadNative(out);

    out.println("__caucho_loadMask_0 |= 1L;");
    
    out.popDepth();
    out.println("}");
  }

  private void generateTransactionChecks(JavaWriter out,
                                         int group, long mask,
                                         int min, int max)
    throws IOException
  {
    // non-read-only entities must be reread in a transaction
    if (! _entityType.isReadOnly()) {
      // jpa/1800
      out.println("if (aConn.isInTransaction()) {");
      out.pushDepth();

      // deleted objects are not reloaded
      out.println("if (__caucho_state.isDeleting()) {");
      out.println("  return;");
      out.println("}");

      // from non-transactional to transactional
      out.println("else if (__caucho_state.isNonTransactional()) {");
      out.pushDepth();

      out.println("__caucho_state = com.caucho.amber.entity.EntityState.P_TRANSACTIONAL;");

      // XXX: ejb/0d01 (create issue?)
      // jpa/0g0k: see __caucho_load_select
      // out.println("    aConn.makeTransactional(this);");
      // out.println("    if ((state > 0) && ((__caucho_loadMask_" + group + " & " + mask + "L) != 0))");
      // out.println("      return;");
      out.println();

      /* XXX: jpa/0o09
        int loadCount = _entityType.getLoadGroupIndex();
        for (int i = 0; i <= loadCount / 64; i++) {
          out.println("    __caucho_loadMask_" + i + " = 0;");
        }
      */

      int dirtyCount = _entityType.getDirtyIndex();
      for (int i = 0; i <= dirtyCount / 64; i++) {
        out.println("__caucho_dirtyMask_" + i + " = 0;");
      }

      out.popDepth();
      out.println("}");
      // ejb/0d01 - already loaded in the transaction
      /*
        out.println("else if ((__caucho_loadMask_" + group + " & " + mask + "L) != 0)");
        out.println("  return;");
      */

      for (int i = min; i <= max; i++) {
        // jpa/0l48: inheritance optimization.
        out.println();
        out.println("if ((__caucho_loadMask_" + group + " & " + (1L << (i % 64)) + "L) == 0)");
        out.println("  __caucho_load_select_" + i + "(aConn);");
      }

      out.popDepth();
      out.println("}");
      out.print("else ");
    }

    out.println("if ((__caucho_loadMask_" + group + " & " + mask + "L) != 0) {");
    out.println("}");

    // XXX: the load doesn't cover other load groups
    out.println("else if (__caucho_cacheItem != null) {");
    out.pushDepth();
    out.println(_extClassName + " item = (" + _extClassName + ") __caucho_cacheItem.getEntity();");

    out.println("item.__caucho_load_select_" + _index + "(aConn);");

    // ejb/06--, ejb/0a-- and jpa/0o04
    _entityType.generateCopyLoadObject(out, "super", "item", _index);

    // out.println("__caucho_loadMask_" + group + " |= " + mask + "L;");
    //out.println("__caucho_loadMask_" + group + " |= item.__caucho_loadMask_" + group + ";"); // mask + "L;");

    out.println("__caucho_loadMask_" + group + " |= item.__caucho_loadMask_" + group + " & " + mask + "L;"); // mask + "L;");

    out.popDepth();
    out.println("}");
  }

  private void generateLoadSelect(JavaWriter out, int group, long mask)
    throws IOException
  {
    // jpa/0l40
    if ((_index == 0) && (_entityType.getDiscriminator() != null)) {
      out.println();
      out.println("String __caucho_discriminator;");
    }

    out.println();
    out.println("protected void __caucho_load_select_" + _index +  "(com.caucho.amber.manager.AmberConnection aConn)");
    out.println("{");
    out.pushDepth();

    if (_entityType.getTable() == null) {
      out.popDepth();
      out.println("}");

      return;
    }

    out.println("if ((__caucho_loadMask_" + group + " & " + mask + "L) != 0)");
    out.println("  return;");

    AmberTable table = _entityType.getTable();

    String from = null;
    String select = null;
    String where = null;

    String subSelect = null;
    AmberTable mainTable = null;
    String tableName = null;

    select = _entityType.generateLoadSelect(table, "o", _index);

    if (select != null) {
      from = table.getName() + " o";
      where = _entityType.getId().generateMatchArgWhere("o");
      mainTable = table;
      tableName = "o";
    }

    ArrayList<AmberTable> subTables = _entityType.getSecondaryTables();

    for (int i = 0; i < subTables.size(); i++) {
      AmberTable subTable = subTables.get(i);

      subSelect = _entityType.generateLoadSelect(subTable, "o" + i, _index);

      if (subSelect == null)
        continue;

      if (select != null)
        select = select + ", " + subSelect;
      else
        select = subSelect;

      if (from != null)
        from = from + ", " + subTable.getName() + " o" + i;
      else
        from = subTable.getName() + " o" + i;

      if (where != null) {
        LinkColumns link = subTable.getDependentIdLink();

        where = where + " and " + link.generateJoin("o" + i, "o");
      }
      else
        throw new IllegalStateException();
    }

    if (select == null) {
      if (_index > 0) {
        // XXX: jpa/0o00

        out.println("return;");

        out.popDepth();
        out.println("}");

        return;
      }

      select = "1";
    }

    if (where == null) {
      from = table.getName() + " o";

      where = _entityType.getId().generateMatchArgWhere("o");
    }

    String sql = "select " + select + " from " + from + " where " + where;

    out.println();
    out.println("java.sql.ResultSet rs = null;");
    
    out.println();
    out.println("try {");
    out.pushDepth();

    // jpa/0o05
    //out.println("com.caucho.amber.entity.Entity contextEntity = aConn.getEntity(this);");

    out.println();
    out.print("String sql = \"");
    out.printJavaString(sql);
    out.println("\";");

    out.println();
    out.println("java.sql.PreparedStatement pstmt = aConn.prepareStatement(sql);");

    out.println("int index = 1;");
    _entityType.getId().generateSet(out, "pstmt", "index", "super");

    out.println();
    out.println("rs = pstmt.executeQuery();");

    out.println("if (rs.next()) {");
    out.pushDepth();

    // jpa/0l40
    if ((_index == 0) && (_entityType.getDiscriminator() != null)) {
      out.println();
      out.println("__caucho_discriminator = rs.getString(1);");
    }

    // jpa/0gg3
    _entityType.generateLoad(out, "rs", "", 1, _index);
    out.println("__caucho_loadMask_" + group + " |= " + mask + "L;");

    out.popDepth();
    out.println("}");
    out.println("else {");

    String errorString = ("(\"amber load: no matching object " +
                          _entityType.getName() + "[\" + __caucho_getPrimaryKey() + \"]\")");

    out.println("  throw new com.caucho.amber.AmberObjectNotFoundException(" + errorString + ");");
    out.println("}");

    out.popDepth();
    out.println("} catch (RuntimeException e) {");
    out.println("  throw e;");
    out.println("} catch (Exception e) {");
    out.println("  throw new com.caucho.amber.AmberRuntimeException(e);");
    out.println("} finally {");
    out.println("  aConn.close(rs);");
    out.println("}");

    out.popDepth();
    out.println("}");
  }

  private void generateCallbacks(JavaWriter out, ArrayList<Method> callbacks)
    throws IOException
  {
    if (callbacks.size() == 0)
      return;

    out.println();
    for (Method method : callbacks) {
      out.println(method.getName() + "();");
    }
  }
}
