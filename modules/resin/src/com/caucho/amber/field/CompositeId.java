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

import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.type.BeanType;
import com.caucho.amber.type.EmbeddableType;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.AmberType;
import com.caucho.java.JavaWriter;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;


/**
 * Configuration for a bean's field
 */
public class CompositeId extends Id {
  private static final L10N L = new L10N(CompositeId.class);
  private static final Logger log
    = Logger.getLogger(CompositeId.class.getName());

  private Class _tKeyClass;

  public CompositeId(EntityType ownerType,
                     ArrayList<IdField> keys)
  {
    super(ownerType, keys);
  }

  protected CompositeId(EntityType ownerType)
  {
    super(ownerType);
  }

  /**
   * Sets the foreign key type.
   */
  public void setKeyClass(Class keyClass)
  {
    _tKeyClass = keyClass;

    getOwnerType().addDependency(keyClass);
  }

  /**
   * Returns the foreign type.
   */
  @Override
  public String getForeignTypeName()
  {
    if (_tKeyClass != null)
      return _tKeyClass.getName();
    else if (isEmbeddedId())
      return getEmbeddedIdField().getJavaTypeName();
    else
      return getOwnerType().getName();
  }

  /**
   * Returns the foreign type.
   */
  public String getForeignMakeKeyName()
  {
    return getOwnerType().getName().replace('.', '_').replace('/', '_');
  }

  /**
   * Generates any prologue.
   */
  @Override
  public void generatePrologue(JavaWriter out, HashSet<Object> completedSet)
    throws IOException
  {
    super.generatePrologue(out, completedSet);

    generatePrologue(out, completedSet, getForeignMakeKeyName());
  }

  /**
   * Generates any prologue.
   */
  @Override
  public void generatePrologue(JavaWriter out,
                               HashSet<Object> completedSet,
                               String name)
    throws IOException
  {
    // jpa/0u21
    out.println();
    out.println("private transient " + getForeignTypeName() + " __caucho_compound_key = new " + getForeignTypeName() + "();");
      
    generatePrologueMake(out, completedSet);
    generatePrologueLoad(out, completedSet);
  }

  /**
   * Generates any prologue.
   */
  public void generatePrologueMake(JavaWriter out,
                                   HashSet<Object> completedSet)
    throws IOException
  {
    String makeName = "__caucho_make_key_" + getForeignMakeKeyName();

    if (completedSet.contains(makeName))
      return;

    completedSet.add(makeName);

    out.println();
    out.print("private static ");
    out.print(getForeignTypeName() + " " + makeName);
    out.print("(");

    if (! isEmbeddedId()) {
      ArrayList<IdField> keys = getKeys();
      for (int i = 0; i < keys.size(); i++) {
        if (i != 0)
          out.print(", ");

        IdField key = keys.get(i);

        out.print(key.getJavaTypeName() + " a" + i);
      }
    }
    else {
      EmbeddableType embeddable = (EmbeddableType) getEmbeddedIdField().getType();

      ArrayList<AmberField> fields = embeddable.getFields();
      for (int i = 0; i < fields.size(); i++) {
        if (i != 0)
          out.print(", ");

        AmberField field = fields.get(i);

        out.print(field.getJavaTypeName() + " a" + i);
      }
    }

    out.println(")");
    out.println("{");
    out.pushDepth();

    out.println();
    out.println(getForeignTypeName() + " key = new " + getForeignTypeName() + "();");

    if (getOwnerType().getPersistenceUnit().isJPA() && ! isEmbeddedId()) {
      String args = "";

      ArrayList<IdField> keys = getKeys();

      for (int i = 0; i < keys.size(); i++) {
        KeyPropertyField key = (KeyPropertyField) keys.get(i);

        String name = key.getName();

        char ch = Character.toUpperCase(name.charAt(0));
        if (name.length() == 1)
          name = "get" + ch;
        else
          name = "get" + ch + key.getName().substring(1);

        Method method = BeanType.getGetter(_tKeyClass, name);

        if (key.isKeyField() || (method != null)) {
          out.println(key.generateSetKeyProperty("key", "a" + i) + ";");
        }
        else {
          // Arg. constructor jpa/0u21
          if (i != 0)
            args += ", ";

          args += " a" + i;

          out.println("if (a" + i + " == null)");
          out.println("  return new " + getForeignTypeName() + "();");

          if (i + 1 == keys.size())
            out.print("key = new " + getForeignTypeName() + "(" + args + ");");
        }
      }

    }
    else {
      ArrayList fields;

      if (getEmbeddedIdField() == null) {
        // ejb/06x2
        fields = getKeys();
      }
      else {
        EmbeddableType embeddable
          = (EmbeddableType) getEmbeddedIdField().getType();

        fields = embeddable.getFields();
      }

      for (int i = 0; i < fields.size(); i++) {
        AmberField field = (AmberField) fields.get(i);

        if (getOwnerType().isFieldAccess())
          out.println(field.generateSet("key", "a" + i) + ";");
        else {
          String setter = field.getName();

          if (getOwnerType().getPersistenceUnit().isJPA()) {
            setter = "set" + Character.toUpperCase(setter.charAt(0)) +
              (setter.length() == 1 ? "" : setter.substring(1));

            out.println("key." + setter + "(a" + i + ");");
          }
          else // XXX: ejb/06x2, ejb/06if
            out.println("key." + setter + " = a" + i + ";");
        }
      }
    }

    out.println("return key;");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates any prologue.
   */
  public void generatePrologueLoad(JavaWriter out,
                                   HashSet<Object> completedSet)
    throws IOException
  {
    String loadName = "__caucho_load_key_" + getForeignMakeKeyName();

    if (completedSet.contains(loadName))
      return;

    completedSet.add(loadName);

    out.println();
    out.print("private static ");
    out.print(getForeignTypeName() + " " + loadName);
    out.println("(com.caucho.amber.manager.AmberConnection aConn, java.sql.ResultSet rs, int index)");
    out.println("  throws java.sql.SQLException");

    out.println("{");
    out.pushDepth();

    int index = 0;
    ArrayList<IdField> keys = getKeys();
    for (int i = 0; i < keys.size(); i++) {
      IdField key = keys.get(i);

      String javaType = key.getJavaTypeName();
      out.print(javaType + " a" + i + " = (" + javaType + ") ");
      index = key.getType().generateLoad(out, "rs", "index", index);
      out.println(";");

      out.println("if (rs.wasNull())");
      out.println("  return null;");
    }

    out.print(getForeignTypeName() + " key = new " + getForeignTypeName() + "(");

    if (isEmbeddedId() || ! getOwnerType().getPersistenceUnit().isJPA()) {
      out.println(");");

      // ejb/06x2
      for (int i = 0; i < keys.size(); i++) {
        out.println(keys.get(i).generateSetKeyProperty("key", "a" + i) + ";");
      }
    }
    else {
      for (int i = 0; i < keys.size(); i++) {
        KeyPropertyField key = (KeyPropertyField) keys.get(i);

        String name = key.getName();

        char ch = Character.toUpperCase(name.charAt(0));
        if (name.length() == 1)
          name = "get" + ch;
        else
          name = "get" + ch + key.getName().substring(1);

        Method method = BeanType.getGetter(_tKeyClass, name);

        if (key.isKeyField() || (method != null)) {
          if (i == 0)
            out.println(");");

          out.println(key.generateSetKeyProperty("key", "a" + i) + ";");
        }
        else {
          // Arg. constructor jpa/0u21
          if (i != 0)
            out.print(", ");

          out.print(" a" + i);

          if (i + 1 == keys.size())
            out.println(");");
        }
      }
    }

    out.println("return key;");

    out.popDepth();
    out.println("}");
  }

  /**
   * Returns the foreign type.
   */
  @Override
  public int generateLoadForeign(JavaWriter out, String rs,
                                 String indexVar, int index)
    throws IOException
  {
    return generateLoadForeign(out, rs, indexVar, index,
                               getForeignTypeName().replace('.', '_'));
  }

  /**
   * Returns the foreign type.
   */
  @Override
  public int generateLoadForeign(JavaWriter out, String rs,
                                 String indexVar, int index,
                                 String name)
    throws IOException
  {
    out.print("__caucho_load_key_" + getForeignMakeKeyName());
    out.print("(aConn, " + rs + ", " + indexVar + " + " + index + ")");

    ArrayList<IdField> keys = getKeys();

    index += keys.size();

    return index;
  }

  /**
   * Generates the select clause.
   */
  @Override
  public String generateSelect(String id)
  {
    ArrayList<IdField> keys = getKeys();

    CharBuffer cb = CharBuffer.allocate();

    for (int i = 0; i < keys.size(); i++) {
      if (i != 0)
        cb.append(", ");

      cb.append(keys.get(i).generateSelect(id));
    }

    return cb.close();
  }

  /**
   * Generates the JPA QL select clause.
   */
  @Override
  public String generateJavaSelect(String id)
  {
    ArrayList<IdField> keys = getKeys();

    CharBuffer cb = CharBuffer.allocate();

    for (int i = 0; i < keys.size(); i++) {
      if (i != 0)
        cb.append(", ");

      cb.append(keys.get(i).generateJavaSelect(id));
    }

    return cb.close();
  }

  /**
   * Generates the select clause.
   */
  @Override
  public String generateLoadSelect(String id)
  {
    return null;
  }

  /**
   * Returns the key for the value
   */
  @Override
  public String generateGet(String value)
  {
    CharBuffer cb = CharBuffer.allocate();

    cb.append("__caucho_make_key_" + getForeignMakeKeyName());
    cb.append("(");

    ArrayList<IdField> keys = getKeys();

    for (int i = 0; i < keys.size(); i++) {
      if (i != 0)
        cb.append(", ");

      cb.append(keys.get(i).generateGet(value));
    }

    cb.append(")");

    return cb.close();
  }

  /**
   * Generates loading cache
   */
  @Override
  public void generateSet(JavaWriter out, String objThis, String value)
    throws IOException
  {
    out.println("if (" + value + " != null) {");
    out.pushDepth();

    AmberPersistenceUnit persistenceUnit
      = getOwnerType().getPersistenceUnit();

    // ejb/06ie
    if (persistenceUnit.isJPA() && ! isEmbeddedId()) {

      // jpa/0u21

      EmbeddableType embeddable
        = persistenceUnit.getEmbeddable(_tKeyClass.getName());

      // jpa/0u21 ArrayList<IdField> keys = getKeys();
      ArrayList<AmberField> keys = embeddable.getFields();

      for (int i = 0; i < keys.size(); i++) {
        PropertyField key = (PropertyField) keys.get(i);

        String getter = "__caucho_get_field(" + i + ")";

        String subValue
          = "((com.caucho.amber.entity.Embeddable) key)." + getter;

        out.println("Object field" + i + " = " + subValue + ";");

        out.println("if (field" + i + " == null)");
        out.println("  return;");

        KeyPropertyField prop = null;

        AmberColumn column = key.getColumn();

        // jpa/0j55
        if (true || column == null) {
          ArrayList<IdField> fields = getKeys();
          for (int j = 0; j < fields.size(); j++) {
            IdField id = fields.get(j);
            if (id.getName().equals(key.getName()))
              if (id instanceof KeyPropertyField)
                prop = (KeyPropertyField) id;
          }
        }

        if (prop != null)
          key = prop;

        AmberType columnType = key.getColumn().getType();

        value = columnType.generateCastFromObject("field" + i);

        key.generateSet(out, objThis, value);
      }

      // jpa/0u21
      // out.println("__caucho_compound_key  = (" + getForeignTypeName() + ") " + value + ";");

      /*
      for (int i = 0; i < keys.size(); i++) {
        IdField key = keys.get(i);

        key.generateStatementSet(out, key.generateGetKeyProperty(obj + "_key"));
      }
      */
    }
    else {
      out.println(getForeignTypeName() + " " + value + "_key = (" + getForeignTypeName() + ") " + value + ";");

      if (getEmbeddedIdField() == null) {
        // ejb/06ie

        ArrayList<IdField> keys = getKeys();

        for (int i = 0; i < keys.size(); i++) {
          IdField key = keys.get(i);

          key.generateSet(out, objThis, key.generateGetKeyProperty(objThis + "_key"));
        }
      }
      else
        getEmbeddedIdField().generateSet(out, objThis, value + "_key");
    }

    out.popDepth();
    out.println("}");
  }

  /**
   * Returns the key for the value
   */
  public String generateGetProxyProperty(String value)
  {
    CharBuffer cb = CharBuffer.allocate();

    cb.append("__caucho_make_key_" + getForeignMakeKeyName());
    cb.append("(");

    ArrayList<IdField> keys = getKeys();

    for (int i = 0; i < keys.size(); i++) {
      if (i != 0)
        cb.append(", ");

      cb.append(keys.get(i).generateGetProxyProperty(value));
    }

    cb.append(")");

    return cb.close();
  }

  /**
   * Generates loading cache
   */
  @Override
  public void generateLoadFromObject(JavaWriter out, String obj)
    throws IOException
  {
    ArrayList<IdField> keys = getKeys();

    for (int i = 0; i < keys.size(); i++) {
      keys.get(i).generateLoadFromObject(out, obj);
    }
  }

  /**
   * Generates loading cache
   */
  @Override
  public void generateUpdateFromObject(JavaWriter out, String obj)
    throws IOException
  {
    ArrayList<IdField> keys = getKeys();

    for (int i = 0; i < keys.size(); i++) {
      keys.get(i).generateUpdateFromObject(out, obj);
    }
  }

  /**
   * Generates the where clause.
   */
  public String generateWhere(String id)
  {
    ArrayList<IdField> keys = getKeys();

    CharBuffer cb = CharBuffer.allocate();

    for (int i = 0; i < keys.size(); i++) {
      if (i != 0)
        cb.append(" and ");

      cb.append(keys.get(i).generateWhere(id));
    }

    return cb.close();
  }

  /**
   * Generates the where clause.
   */
  @Override
  public String generateCreateTableSQL(AmberPersistenceUnit manager)
  {
    return null;
  }

  /**
   * Generates the set clause.
   */
  @Override
  public void generateSetKey(JavaWriter out, String pstmt,
                             String obj, String index)
    throws IOException
  {
    generateSet(out, pstmt, obj, index);
  }

  /**
   * Generates code to convert to the type from the object.
   */
  @Override
  public String generateCastFromObject(String value)
  {
    return value;
  }

  /**
   * Generates code for a match.
   */
  @Override
  public void generateMatch(JavaWriter out, String key)
    throws IOException
  {
    out.println("return __caucho_getPrimaryKey().equals(" + key + ");");
  }

  /**
   * Generates code to test the equals.
   */
  @Override
  public String generateEquals(String leftBase, String value)
  {
    return leftBase + ".equals(" + value + ")";
  }
  /**
   * Generates the set clause.
   */
  @Override
  public void generateCheckCreateKey(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the set clause.
   */
  /*
    public void generateSetGeneratedKeys(JavaWriter out, String pstmt)
    throws IOException
    {
    }
  */

  /**
   * Generates code to convert to the object.
   */
  @Override
  public String toObject(String value)
  {
    return value;
  }
}
