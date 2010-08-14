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

package com.caucho.jaxb.skeleton;

import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.jaxb.accessor.Accessor;
import com.caucho.jaxb.accessor.JAXBElementAccessor;
import com.caucho.jaxb.mapping.Namer;
import com.caucho.jaxb.mapping.XmlMapping;
import com.caucho.jaxb.mapping.XmlValueMapping;
import com.caucho.jaxb.mapping.JAXBElementMapping;
import com.caucho.util.L10N;

import javax.xml.bind.annotation.*;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.io.IOException;

import java.lang.annotation.*;
import java.lang.reflect.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

/**
 *
 * Wrapper skeleton when a JAXBElement<X> is specified but no explicit
 * JAXBElementSkeleton<X> is registered.  This class is intended to be
 * instantiated once per JAXBContext.
 *
 **/
public class DynamicJAXBElementSkeleton extends ClassSkeleton<Object> {
  // We must extend ClassSkeleton<Object> rather than ClassSkeleton
  // or else javac complains about overridden methods.
  private static final Logger log 
    = Logger.getLogger(DynamicJAXBElementSkeleton.class.getName());
  private static final L10N L = new L10N(DynamicJAXBElementSkeleton.class);
  private static final Object[] SINGLE_NULL_ARG = new Object[] {null};

  private final JAXBElementAccessor _accessor;

  public DynamicJAXBElementSkeleton(JAXBContextImpl context)
    throws JAXBException
  {
    super(context);

    _value = new JAXBElementMapping(context);
    _accessor = (JAXBElementAccessor) _value.getAccessor();
  }

  public void write(Marshaller m, XMLStreamWriter out,
                    Object obj, Namer namer, 
                    ArrayList<XmlMapping> attributes)
    throws IOException, XMLStreamException, JAXBException
  {
    if (! (obj instanceof JAXBElement))
      throw new IllegalArgumentException(L.l("Object must be a JAXBElement"));

    JAXBElement element = (JAXBElement) obj;

    _accessor.setType(element.getDeclaredType());

    JAXBElementMapping mapping = (JAXBElementMapping) _value;
    mapping.setQName(element.getName());
    mapping.setProperty(_context.createProperty(element.getDeclaredType()));

    super.write(m, out, obj, namer, attributes);
  }

  public Object newInstance()
    throws JAXBException
  {
    // This skeleton should not be used for reading
    throw new IllegalStateException();
  }

  public String toString()
  {
    return "DynamicJAXBElementSkeleton[]";
  }
}
