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

package com.caucho.amber.type;

import com.caucho.amber.field.*;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.table.AmberTable;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.make.ClassDependency;
import com.caucho.util.L10N;
import com.caucho.vfs.PersistentDependency;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Represents a stateful type:
 * embeddable, entity or mapped-superclass.
 */
abstract public class BeanType extends AbstractEnhancedType
{
  private static final Logger log = Logger.getLogger(BeanType.class.getName());
  private static final L10N L = new L10N(BeanType.class);

  private boolean _isFieldAccess;

  // fields declared on this class
  private ArrayList<AmberField> _selfFields = new ArrayList<AmberField>();

  private volatile boolean _isConfigured;

  private ArrayList<PersistentDependency> _dependencies
    = new ArrayList<PersistentDependency>();

  private HashMap<String,String> _completionFields
    = new HashMap<String,String>();

  private AmberColumn _discriminator;

  public BeanType(AmberPersistenceUnit amberPersistenceUnit)
  {
    super(amberPersistenceUnit);
  }

  public boolean isEntity()
  {
    return false;
  }

  /**
   * Set true for field-access.
   */
  public void setFieldAccess(boolean isFieldAccess)
  {
    _isFieldAccess = isFieldAccess;
  }

  /**
   * Set true for field-access.
   */
  public boolean isFieldAccess()
  {
    return _isFieldAccess;
  }

  /**
   * Returns true for an embeddable
   */
  public boolean isEmbeddable()
  {
    return false;
  }

  /**
   * Returns the discriminator.
   */
  public AmberColumn getDiscriminator()
  {
    return _discriminator;
  }

  /**
   * Sets the discriminator.
   */
  public void setDiscriminator(AmberColumn discriminator)
  {
    _discriminator = discriminator;
  }

  /**
   * Returns the java type.
   */
  @Override
  public String getJavaTypeName()
  {
    return getInstanceClassName();
  }

  /**
   * Adds a new field.
   */
  public void addField(AmberField field)
  {
    _selfFields.add(field);
    Collections.sort(_selfFields, new AmberFieldCompare());
  }

  /**
   * Returns the fields declared on this instance
   */
  public ArrayList<AmberField> getSelfFields()
  {
    return _selfFields;
  }

  /**
   * Returns the fields.
   */
  public ArrayList<AmberField> getFields()
  {
    return _selfFields;
  }

  /**
   * Returns the field with a given name.
   */
  public AmberField getField(String name)
  {
    for (AmberField field : getFields()) {
      if (field.getName().equals(name))
        return field;
    }

    return null;
  }

  /**
   * Sets the bean class.
   */
  @Override
  public void setBeanClass(Class beanClass)
  {
    super.setBeanClass(beanClass);

    addDependency(_tBeanClass);
  }

  /**
   * Adds a dependency.
   */
  public void addDependency(Class cl)
  {
    addDependency(new ClassDependency(cl));
  }

  /**
   * Adds a dependency.
   */
  public void addDependency(PersistentDependency depend)
  {
    if (! _dependencies.contains(depend))
      _dependencies.add(depend);
  }

  /**
   * Gets the dependency.
   */
  public ArrayList<PersistentDependency> getDependencies()
  {
    return _dependencies;
  }

  /**
   * Adds a new completion field.
   */
  public void addCompletionField(String name)
  {
    _completionFields.put(name, name);
  }

  /**
   * Returns true if and only if it has the completion field.
   */
  public boolean containsCompletionField(String completionField)
  {
    return _completionFields.containsKey(completionField);
  }

  /**
   * Remove all completion fields.
   */
  public void removeAllCompletionFields()
  {
    _completionFields.clear();
  }

  /**
   * Set true if configured.
   */
  public boolean startConfigure()
  {
    synchronized (this) {
      if (_isConfigured)
        return false;

      _isConfigured = true;

      return true;
    }
  }

  /**
   * Initialize the type.
   */
  @Override
  public void init()
    throws ConfigException
  {
    // jpa/0l14, jpa/0ge3
    for (AmberField field : _selfFields) {
      // ejb/0602
      if (getPersistenceUnit().isJPA()) {
        if (field instanceof ManyToOneField)
          ((ManyToOneField) field).init((EntityType) this);
      }
    }
  }

  /**
   * Converts the value.
   */
  @Override
  public String generateCastFromObject(String value)
  {
    return "((" + getInstanceClassName() + ") " + value + ")";
  }

  /**
   * Generates the select clause for a load.
   */
  public void generateLoadSelect(StringBuilder sb,
                                 AmberTable table,
                                 String id,
                                 int loadGroup)
  {
    // jpa/0l14, jpa/0ge3
    for (AmberField field : getFields()) {
      // jpa/0gg3
      if (field.getLoadGroupIndex() == loadGroup) {
        String propSelect = field.generateLoadSelect(table, id);

        if (propSelect != null) {
          if (sb.length() > 0)
            sb.append(", ");

          sb.append(propSelect);
        }
      }
    }
  }

  /**
   * Generates a string to load the field.
   */
  public int generateLoad(JavaWriter out,
                          String rs,
                          String indexVar,
                          int index,
                          int loadGroupIndex)
    throws IOException
  {
    if (getDiscriminator() != null) {
      EntityType parent = null;

      if (this instanceof EntityType)
        parent = ((EntityType) this).getParentType();

      boolean isAbstractParent = getPersistenceUnit().isJPA()
        && (parent == null 
            || Modifier.isAbstract(parent.getBeanClass().getModifiers()));

      if (loadGroupIndex == 0 || isAbstractParent)
        index++;
    }

    for (AmberField field : getFields()) {
      // jpa/0gg3
      if (field.getLoadGroupIndex() == loadGroupIndex)
        index = field.generateLoad(out, rs, indexVar, index);
    }

    return index;
  }

  /**
   * Generates the select clause for a load.
   */
  abstract public String generateLoadSelect(AmberTable table, String id);

  /**
   * Generates the select clause for a load.
   */
  public String generateLoadSelect(AmberTable table,
                                   String id,
                                   int loadGroup)
  {
    StringBuilder sb = new StringBuilder();
    
    generateLoadSelect(sb, table, id, loadGroup);

    if (sb.length() > 0)
      return sb.toString();
    else
      return null;
  }

  /**
   * Returns the load mask generated on create.
   */
  public void generatePrePersist(JavaWriter out)
    throws IOException
  {
    for (AmberField field : getFields()) {
      field.generatePrePersist(out);
    }
  }

  /**
   * Generates the foreign delete
   */
  public void generateInvalidateForeign(JavaWriter out)
    throws IOException
  {
    for (AmberField field : getFields()) {
      field.generateInvalidateForeign(out);
    }
  }

  /**
   * Generates any expiration code.
   */
  public void generateExpire(JavaWriter out)
    throws IOException
  {
    for (AmberField field : getFields()) {
      field.generateExpire(out);
    }
  }

  /**
   * Gets a matching getter.
   */
  public Method getGetter(String name)
  {
    return getGetter(_tBeanClass, name);
  }

  /**
   * Gets a matching getter.
   */
  public static Method getGetter(Class cl, String name)
  {
    Method []methods = cl.getMethods();

    for (int i = 0; i < methods.length; i++) {
      Class []param = methods[i].getParameterTypes();
      String methodName = methods[i].getName();

      if (name.equals(methodName) && param.length == 0)
        return methods[i];
    }

    cl = cl.getSuperclass();

    if (cl != null)
      return getGetter(cl, name);
    else
      return null;
  }

  /**
   * Gets a matching getter.
   */
  public static Field getField(Class cl, String name)
  {
    Field []fields = cl.getDeclaredFields();

    for (int i = 0; i < fields.length; i++) {
      if (name.equals(fields[i].getName()))
        return fields[i];
    }

    cl = cl.getSuperclass();

    if (cl != null)
      return getField(cl, name);
    else
      return null;
  }

  /**
   * Gets a matching getter.
   */
  public static Method getSetter(Class cl, String name)
  {
    Method []methods = cl.getMethods();

    for (int i = 0; i < methods.length; i++) {
      Class []param = methods[i].getParameterTypes();
      String methodName = methods[i].getName();

      if (name.equals(methodName) && param.length == 1)
        return methods[i];
    }

    cl = cl.getSuperclass();

    if (cl != null)
      return getSetter(cl, name);
    else
      return null;
  }

  /**
   * Returns the load mask generated on create.
   */
  public long getCreateLoadMask(int group)
  {
    long mask = 0;

    for (AmberField field : getFields()) {
      mask |= field.getCreateLoadMask(group);
    }

    return mask;
  }
}
