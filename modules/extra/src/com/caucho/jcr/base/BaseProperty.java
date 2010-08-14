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

package com.caucho.jcr.base;

import com.caucho.util.L10N;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;
import java.io.InputStream;
import java.util.Calendar;

/**
 * Represents a property item in the repository.
 */
public class BaseProperty extends BaseItem implements Property {
  private static final L10N L = new L10N(BaseProperty.class);

  private ValueFactory _factory = BaseValueFactory.FACTORY;
  
  private Value _value; // set to some null

  protected BaseProperty(Value value)
  {
    _value = value;
  }
  
  /**
   * Sets the property value.
   */
  public void setValue(Value value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException
  {
    _value = value;
  }
  
  /**
   * Sets the property value to a value array.
   */
  public void setValue(Value[] values)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Sets the property value to a string.
   */
  public void setValue(String value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException
  {
    setValue(_factory.createValue(value));
  }
  
  /**
   * Sets the property value to a string array.
   */
  public void setValue(String[] values)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets the property value to a binary chunk.
   */
  public void setValue(InputStream value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException
  {
    setValue(_factory.createValue(value));
  }
  
  /**
   * Sets the property value to a long.
   */
  public void setValue(long value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException
  {
    setValue(_factory.createValue(value));
  }
  
  /**
   * Sets the property value to a double.
   */
  public void setValue(double value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException
  {
    setValue(_factory.createValue(value));
  }
  
  /**
   * Sets the property value to a date.
   */
  public void setValue(Calendar value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException
  {
    setValue(_factory.createValue(value));
  }
  
  /**
   * Sets the property value to a boolean.
   */
  public void setValue(boolean value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException
  {
    setValue(_factory.createValue(value));
  }
  
  /**
   * Sets the property value to a node reference.
   */
  public void setValue(Node value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException
  {
    setValue(_factory.createValue(value));
  }
  
  /**
   * Returns the property value.
   */
  public Value getValue()
    throws ValueFormatException,
           RepositoryException
  {
    return _value;
  }
  
  /**
   * Returns the property value as a value array.
   */
  public Value[] getValues()
    throws ValueFormatException,
           RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Returns the property value as a string.
   */
  public String getString()
    throws ValueFormatException,
           RepositoryException
  {
    return getValue().getString();
  }
  
  /**
   * Returns the property value as a binary stream.
   */
  public InputStream getStream()
    throws ValueFormatException,
           RepositoryException
  {
    return getValue().getStream();
  }
  
  /**
   * Returns the property value as a long.
   */
  public long getLong()
    throws ValueFormatException,
           RepositoryException
  {
    return getValue().getLong();
  }
  
  /**
   * Returns the property value as a double.
   */
  public double getDouble()
    throws ValueFormatException,
           RepositoryException
  {
    return getValue().getDouble();
  }
  
  /**
   * Returns the property value as a date.
   */
  public Calendar getDate()
    throws ValueFormatException,
           RepositoryException
  {
    return getValue().getDate();
  }
  
  /**
   * Returns the property value as a boolean.
   */
  public boolean getBoolean()
    throws ValueFormatException,
           RepositoryException
  {
    return getValue().getBoolean();
  }
  
  /**
   * Returns the property value as a node reference.
   */
  public Node getNode()
    throws ValueFormatException,
           RepositoryException
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns the size of the property.
   */
  public long getLength()
    throws ValueFormatException,
           RepositoryException
  {
    return 0;
  }
  
  /**
   * Returns the size of all the properties.
   */
  public long[] getLengths()
    throws ValueFormatException,
           RepositoryException
  {
    return new long[0];
  }
  
  /**
   * Returns the property's definition.
   */
  public PropertyDefinition getDefinition()
    throws RepositoryException
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns the property's base type.
   */
  public int getType()
    throws RepositoryException
  {
    return getDefinition().getRequiredType();
  }
}
