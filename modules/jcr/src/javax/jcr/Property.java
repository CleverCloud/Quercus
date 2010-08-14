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
package javax.jcr;

import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;
import java.io.InputStream;
import java.util.Calendar;

/**
 * Represents a node's property.
 */
public interface Property extends Item {
  /**
   * Sets the property value.
   */
  public void setValue(Value value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Sets the property value to a value array.
   */
  public void setValue(Value[] values)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Sets the property value to a string.
   */
  public void setValue(String value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Sets the property value to a string array.
   */
  public void setValue(String[] values)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Sets the property value to a binary chunk.
   */
  public void setValue(InputStream value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Sets the property value to a long.
   */
  public void setValue(long value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Sets the property value to a double.
   */
  public void setValue(double value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Sets the property value to a date.
   */
  public void setValue(Calendar value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Sets the property value to a boolean.
   */
  public void setValue(boolean value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Sets the property value to a node reference.
   */
  public void setValue(Node value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Returns the property value.
   */
  public Value getValue()
    throws ValueFormatException,
           RepositoryException;
  
  /**
   * Returns the property value as a value array.
   */
  public Value[] getValues()
    throws ValueFormatException,
           RepositoryException;
  
  /**
   * Returns the property value as a string.
   */
  public String getString()
    throws ValueFormatException,
           RepositoryException;
  
  /**
   * Returns the property value as a binary stream.
   */
  public InputStream getStream()
    throws ValueFormatException,
           RepositoryException;
  
  /**
   * Returns the property value as a long.
   */
  public long getLong()
    throws ValueFormatException,
           RepositoryException;
  
  /**
   * Returns the property value as a double.
   */
  public double getDouble()
    throws ValueFormatException,
           RepositoryException;
  
  /**
   * Returns the property value as a date.
   */
  public Calendar getDate()
    throws ValueFormatException,
           RepositoryException;
  
  /**
   * Returns the property value as a boolean.
   */
  public boolean getBoolean()
    throws ValueFormatException,
           RepositoryException;
  
  /**
   * Returns the property value as a node reference.
   */
  public Node getNode()
    throws ValueFormatException,
           RepositoryException;
  
  /**
   * Returns the size of the property.
   */
  public long getLength()
    throws ValueFormatException,
           RepositoryException;
  
  /**
   * Returns the size of all the properties.
   */
  public long[] getLengths()
    throws ValueFormatException,
           RepositoryException;
  
  /**
   * Returns the property's definition.
   */
  public PropertyDefinition getDefinition()
    throws RepositoryException;
  
  /**
   * Returns the property's base type.
   */
  public int getType()
    throws RepositoryException;
}
