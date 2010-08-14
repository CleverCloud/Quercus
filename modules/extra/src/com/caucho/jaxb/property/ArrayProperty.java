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
 * @author Adam Megacz
 */

package com.caucho.jaxb.property;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.events.*;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

import com.caucho.util.L10N;

/**
 * a property for serializing/deserializing arrays
 */
public abstract class ArrayProperty extends IterableProperty {
  private static final L10N L = new L10N(CDataProperty.class);

  protected ArrayProperty(Property componentProperty)
  {
    _componentProperty = componentProperty;
  }

  public static ArrayProperty createArrayProperty(Property componentProperty,
                                                  Class type)
    throws JAXBException
  {
    if (! type.isPrimitive())
      return new ObjectArrayProperty(componentProperty, type);

    if (Double.TYPE.equals(type))
      return DoubleArrayProperty.PROPERTY;

    if (Float.TYPE.equals(type))
      return FloatArrayProperty.PROPERTY;

    if (Integer.TYPE.equals(type))
      return IntegerArrayProperty.PROPERTY;

    if (Long.TYPE.equals(type))
      return LongArrayProperty.PROPERTY;

    if (Boolean.TYPE.equals(type))
      return BooleanArrayProperty.PROPERTY;

    if (Character.TYPE.equals(type))
      return CharacterArrayProperty.PROPERTY;

    if (Short.TYPE.equals(type))
      return ShortArrayProperty.PROPERTY;

    /* XXX
    if (Byte.TYPE.equals(type))
      return ByteArrayProperty.PROPERTY; */

    throw new JAXBException(L.l("{0} is neither primitive, nor non-primitive!",
                                type));
  }

  public QName getSchemaType()
  {
    return _componentProperty.getSchemaType();
  }

  public String getMaxOccurs()
  {
    return "unbounded";
  }

  protected boolean isPrimitiveType()
  {
    return false;
  }

  public boolean isXmlPrimitiveType()
  {
    return _componentProperty.isXmlPrimitiveType();
  }

  public boolean isNullable()
  {
    return true;
  }
}


