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

package com.caucho.xml.schema;

import java.io.*;
import java.util.*;
import static javax.xml.XMLConstants.*;
import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;

import com.caucho.java.JavaWriter;

import com.caucho.jaxb.JAXBUtil;

/**
 * JAXB annotated Schema data structure.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="element", namespace=W3C_XML_SCHEMA_NS_URI)
public class Element {
  @XmlAttribute(name="name")
  private String _name;

  @XmlAttribute(name="type")
  private QName _type;

  @XmlAttribute(name="minOccurs")
  private Integer _minOccurs;

  @XmlAttribute(name="maxOccurs")
  private String _maxOccurs;

  @XmlTransient
  private String _className;

  @XmlTransient
  private Schema _schema;

  @XmlTransient
  public Schema getSchema()
  {
    return _schema;
  }

  public void setSchema(Schema schema)
  {
    _schema = schema;
  }

  public String getName()
  {
    return _name;
  }

  public QName getType()
  {
    return _type;
  }

  public Integer getMinOccurs()
  {
    return _minOccurs;
  }

  public String getMaxOccurs()
  {
    return _maxOccurs;
  }

  public void generateJavaField(JavaWriter out)
    throws IOException
  {
    out.println("@XmlTransient");
    out.print("public ");
    out.print(getClassname());
    out.print(" _");
    out.print(getName());
    out.println(";");
    out.println();

    out.println("@XmlElement(name=\"" + getName() + "\")");
    out.print("public ");
    out.print(getClassname());

    if ("Boolean".equals(getClassname()) || "boolean".equals(getClassname()))
      out.print(" is");
    else
      out.print(" get");

    out.print(JAXBUtil.xmlNameToClassName(getName()));
    out.println("()");
    out.println("{");
    out.pushDepth();
    out.println("return _" + getName() + ";");
    out.popDepth();
    out.println("}");
    out.println();

    out.print("public void set");
    out.print(JAXBUtil.xmlNameToClassName(getName()));
    out.print("(");
    out.print(getClassname());
    out.print(" ");
    out.print(getName());
    out.println(")");
    out.println("{");
    out.pushDepth();
    out.println("_" + getName() + " = " + getName() + ";");
    out.popDepth();
    out.println("}");
    out.println();
  }

  public String getClassname()
  {
    if (_className == null) {
      Class cl = JAXBUtil.getClassForDatatype(getType());
      String name = null;

      if (cl != null) {
        name = cl.getName();
      }
      else if (_schema != null) {
        Type type = _schema.getType(getType());

        if (type != null)
          name = type.getClassname();
      }
      else {
        return null;
      }

      if ("unbounded".equals(getMaxOccurs())) {
        if (cl != null && cl.isPrimitive())
          name = JAXBUtil.primitiveToWrapperName(cl);

        _className = "List<" + name + ">";
      }
      else if (getMinOccurs() != null && getMinOccurs() == 0 && 
               cl != null && cl.isPrimitive()) {
        _className = JAXBUtil.primitiveToWrapperName(cl);
      }
      else if (cl != null && cl.isArray()) {
        if (cl.getComponentType().equals(Byte.class))
          _className = "byte[]";
        else
          _className = cl.getComponentType().getName() + "[]";
      }
      else
        _className = name;
    }

    return _className;
  }
}
