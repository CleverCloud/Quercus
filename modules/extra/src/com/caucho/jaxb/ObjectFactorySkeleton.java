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

package com.caucho.jaxb;

import java.lang.reflect.*;
import java.util.*;
import javax.xml.bind.*;
import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import com.caucho.jaxb.skeleton.*;

public class ObjectFactorySkeleton {
  private JAXBContextImpl _context;
  private Object _objectFactory;
  //private HashMap<QName,Skeleton> _roots = new HashMap<QName,Skeleton>();
  private HashMap<QName,Method> _roots = new HashMap<QName,Method>();
  private HashMap<Class,ClassSkeleton> _classSkeletons 
    = new HashMap<Class,ClassSkeleton>();

  public ObjectFactorySkeleton(JAXBContextImpl context, 
                               Class objectFactoryClass)
    throws JAXBException
  {
    _context = context;

    try {
      _objectFactory = objectFactoryClass.newInstance();
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }

    String namespace = null;
    Package pkg = objectFactoryClass.getPackage();

    if (pkg.isAnnotationPresent(XmlSchema.class)) {
      XmlSchema schema = (XmlSchema) pkg.getAnnotation(XmlSchema.class);

      if (! "".equals(schema.namespace()))
        namespace = schema.namespace();
    }

    Method[] methods = objectFactoryClass.getMethods();

    for (Method method : methods) {
      if (method.getName().startsWith("create")) {
        XmlElementDecl decl = method.getAnnotation(XmlElementDecl.class);
        Class cl = method.getReturnType();

        if (cl.equals(JAXBElement.class)) {
          ParameterizedType type = 
            (ParameterizedType) method.getGenericReturnType();
          cl = (Class) type.getActualTypeArguments()[0];
        }

        if (decl != null) {
          String localName = decl.name();

          if (! "##default".equals(decl.namespace()))
            namespace = decl.namespace();

          QName root = null;

          if (namespace == null)
            root = new QName(localName);
          else
            root = new QName(localName, namespace);

          _roots.put(root, method);
        }
        else {
          if (! _context.hasSkeleton(cl)) {
            ClassSkeleton skeleton = new ClassSkeleton(_context, cl);

            _classSkeletons.put(cl, skeleton);
          }
        }
      }
      else if (method.getName().equals("newInstance")) {
        // XXX
      }
      else if (method.getName().equals("getProperty")) {
        // XXX
      }
      else if (method.getName().equals("setProperty")) {
        // XXX
      }
    }
  }
}
