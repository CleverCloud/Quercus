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
 * @author Emil Ong
 */

package com.caucho.jaxb.property;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.io.IOException;

import java.lang.reflect.Array;

import java.util.Collection;

public class XmlListArrayProperty<T> extends CDataProperty {
  private final CDataProperty _componentProperty;
  private final Class<T> _type;

  protected XmlListArrayProperty(CDataProperty componentProperty)
  {
    _componentProperty = componentProperty;
    _type = null;
  }

  private XmlListArrayProperty(CDataProperty componentProperty, Class<T> type)
  {
    _componentProperty = componentProperty;
    _type = type;
  }

  public static XmlListArrayProperty 
    createXmlListArrayProperty(CDataProperty componentProperty, Class type)
    throws JAXBException
  {
    if (boolean.class.equals(type))
      return new XmlListBooleanArrayProperty();
    else if (byte.class.equals(type))
      throw new JAXBException(L.l("@XmlList applied to byte[] valued fields or properties"));
    else if (char.class.equals(type))
      return new XmlListCharacterArrayProperty();
    else if (double.class.equals(type))
      return new XmlListDoubleArrayProperty();
    else if (float.class.equals(type))
      return new XmlListFloatArrayProperty();
    else if (int.class.equals(type))
      return new XmlListIntegerArrayProperty();
    else if (long.class.equals(type))
      return new XmlListLongArrayProperty();
    else if (short.class.equals(type))
      return new XmlListShortArrayProperty();
    else
      return new XmlListArrayProperty(componentProperty, type);
  }

  public String write(Object in)
    throws IOException, JAXBException
  {
    if (in == null)
      return "";

    StringBuilder sb = new StringBuilder();

    int length = Array.getLength(in);

    for (int i = 0; i < length; i++) {
      sb.append(_componentProperty.write(Array.get(in, i)));

      if (i != length - 1)
        sb.append(' ');
    }

    return sb.toString();
  }

  protected Object read(String in)
    throws IOException, JAXBException
  {
    String[] tokens = in.split("\\s+");

    T[] array = (T[]) Array.newInstance(_type, tokens.length);

    for (int i = 0; i < tokens.length; i++)
      array[i] = (T) _componentProperty.read(tokens[i]);

    return array;
  }

  public QName getSchemaType()
  {
    return _componentProperty.getSchemaType();
  }
}
