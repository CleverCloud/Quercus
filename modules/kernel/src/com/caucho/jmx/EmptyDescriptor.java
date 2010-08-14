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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */

package com.caucho.jmx;

import javax.management.Descriptor;
import javax.management.RuntimeOperationsException;

public class EmptyDescriptor
  implements Descriptor
{
  public static final Descriptor EMPTY_DESCRIPTOR =  new EmptyDescriptor();

  private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  private EmptyDescriptor()
  {
  }

  public Object getFieldValue(String fieldName)
    throws RuntimeOperationsException
  {
    return null;
  }

  public void setField(String fieldName, Object fieldValue)
    throws RuntimeOperationsException
  {
    throw new RuntimeOperationsException(new UnsupportedOperationException());
  }

  public String[] getFields()
  {
    return EMPTY_STRING_ARRAY;
  }

  public String[] getFieldNames()
  {
    return EMPTY_STRING_ARRAY;
  }

  public Object[] getFieldValues(String[] fieldNames)
  {
    return EMPTY_OBJECT_ARRAY;
  }

  public void removeField(String fieldName)
  {
    throw new RuntimeOperationsException(new UnsupportedOperationException());
  }

  public void setFields(String[] fieldNames, Object[] fieldValues)
    throws RuntimeOperationsException
  {
    throw new RuntimeOperationsException(new UnsupportedOperationException());
  }

  public Object clone()
    throws RuntimeOperationsException
  {
    return this;
  }

  public boolean isValid()
    throws RuntimeOperationsException
  {
    return true;
  }
}
