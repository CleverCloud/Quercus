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

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import java.io.InputStream;
import java.util.Calendar;

/**
 * Represents a base value
 */
abstract public class BaseValue implements Value {
  private static final L10N L = new L10N(BaseValue.class);
  
  abstract public String getString()
    throws ValueFormatException,
           IllegalStateException,
           RepositoryException;
  
  public InputStream getStream()
    throws IllegalStateException,
           RepositoryException
  {
    throw new IllegalStateException(getClass().getName());
  }
  
  public long getLong()
    throws ValueFormatException,
           IllegalStateException,
           RepositoryException
  {
    return Long.parseLong(getString());
  }
  
  public double getDouble()
    throws ValueFormatException,
           IllegalStateException,
           RepositoryException
  {
    return Double.parseDouble(getString());
  }
  
  public Calendar getDate()
    throws ValueFormatException,
           IllegalStateException,
           RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public boolean getBoolean()
    throws ValueFormatException,
           IllegalStateException,
           RepositoryException
  {
    return ! "false".equals(getString());
  }
  
  public int getType()
  {
    return PropertyType.STRING;
  }
}
