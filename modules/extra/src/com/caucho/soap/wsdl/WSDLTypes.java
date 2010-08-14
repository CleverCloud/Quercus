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
 * @author Emil Ong
 */

package com.caucho.soap.wsdl;

import java.io.*;

import java.util.List;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import javax.xml.namespace.QName;

import static com.caucho.soap.wsdl.WSDLConstants.*;

import com.caucho.xml.schema.Schema;
import com.caucho.xml.schema.Type;

/**
 * WSDL types
 */
@XmlType(name="types", namespace=WSDL_NAMESPACE)
public class WSDLTypes extends WSDLExtensibleDocumented 
                       implements WSDLDefinition
{
  public void writeJAXBClasses(File outputDirectory, String pkg)
    throws IOException
  {
    List<Object> any = getAny();

    if (any == null)
      return;

    for (int i = 0; i < any.size(); i++) { 
      if (any.get(i) instanceof Schema) {
        Schema schema = (Schema) any.get(i);
        schema.writeJAXBClasses(outputDirectory, pkg);
      }
    }
  }

  public void resolveImports(Unmarshaller u)
    throws JAXBException
  {
    List<Object> any = getAny();

    if (any == null)
      return;

    for (int i = 0; i < any.size(); i++) { 
      if (any.get(i) instanceof Schema) {
        Schema schema = (Schema) any.get(i);
        schema.resolveImports(u); 
      }
    }
  }

  public Type getType(QName typeName)
  {
    List<Object> any = getAny();

    if (any != null) {
      for (int i = 0; i < any.size(); i++) { 
        if (any.get(i) instanceof Schema) {
          Schema schema = (Schema) any.get(i);

          Type type = schema.getType(typeName);

          if (type != null)
            return type;
        }
      }
    }

    return null;
  }
}
