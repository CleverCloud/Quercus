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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class XmlListCollectionProperty extends CDataProperty {
  private final CDataProperty _componentProperty;
  private final Constructor _collectionConstructor;

  public XmlListCollectionProperty(CDataProperty componentProperty, 
                                   Class collectionType)
    throws JAXBException
  {
    _componentProperty = componentProperty;

    try {
      if (collectionType.isInterface() || 
          Modifier.isAbstract(collectionType.getModifiers())) {

        if (List.class.isAssignableFrom(collectionType)) 
          _collectionConstructor = ArrayList.class.getConstructor();

        else if (Set.class.isAssignableFrom(collectionType))
          _collectionConstructor = LinkedHashSet.class.getConstructor();

        else if (Queue.class.isAssignableFrom(collectionType))
          _collectionConstructor = LinkedList.class.getConstructor();

        else if (Collection.class.isAssignableFrom(collectionType))
          _collectionConstructor = ArrayList.class.getConstructor();

        else
          _collectionConstructor = null;
      }
      else
        _collectionConstructor = collectionType.getConstructor();
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }

    if (_collectionConstructor == null)
      throw new JAXBException(L.l("Cannot instantiate interface or abstract class: {0}", collectionType));
  }

  public String write(Object in)
    throws IOException, JAXBException
  {
    if (in == null)
      return "";

    StringBuilder sb = new StringBuilder();

    for (Object o : (Collection) in) {
      sb.append(_componentProperty.write(o));
      sb.append(' ');
    }

    if (sb.length() > 0)
      sb.deleteCharAt(sb.length() - 1);

    return sb.toString();
  }

  protected Object read(String in)
    throws IOException, JAXBException
  {
    String[] tokens = in.split("\\s+");

    try {
      Collection collection = (Collection) _collectionConstructor.newInstance();

      for (int i = 0; i < tokens.length; i++)
        collection.add(_componentProperty.read(tokens[i]));

      return collection;
    }
    catch (JAXBException e) {
      throw e;
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }
  }

  public QName getSchemaType()
  {
    return _componentProperty.getSchemaType();
  }
}
