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

import com.caucho.java.JavaWriter;

import com.caucho.jaxb.JAXBUtil;

import com.caucho.vfs.*;

/**
 * JAXB annotated Schema data structure.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="complexType", namespace=W3C_XML_SCHEMA_NS_URI)
public class ComplexType extends Type {
  @XmlAttribute(name="name")
  private String _name;

  @XmlElements({
      @XmlElement(name="sequence", 
                  namespace=W3C_XML_SCHEMA_NS_URI,
                  type=com.caucho.xml.schema.Sequence.class)})
  private List<Object> _contents;

  @XmlTransient
  private Schema _schema;

  @XmlTransient
  private String _className;

  @XmlTransient
  /** If set to false, generateJava does nothing. */
  private boolean _emit = true;

  @XmlTransient
  /** If set to true, generateJava also generates a wrapper Exception class. */
  private boolean _emitFaultWrapper = false;

  @XmlTransient
  public Schema getSchema()
  {
    return _schema;
  }

  public void setSchema(Schema schema)
  {
    _schema = schema;

    if (_contents != null) {
      for (int i = 0; i < _contents.size(); i++) { 
        Sequence sequence = (Sequence) _contents.get(i);
        sequence.setSchema(_schema);
      }
    }
  }

  public String getName()
  {
    return _name;
  }

  public List<Object> getContents()
  {
    if (_contents == null)
      _contents = new ArrayList<Object>();

    return _contents;
  }

  public String getJavaType(int index)
  {
    if (_contents == null)
      return "void";

    if (_contents.size() == 0)
      return "void";

    if (_contents.size() != 1)
      return null;

    if (_contents.get(0) instanceof Sequence) {
      Sequence sequence = (Sequence) _contents.get(0);
      List<Object> sequenceContents = sequence.getContents();

      int j = 0; 

      for (int i = 0; i < sequenceContents.size(); i++) {
        Object o = sequenceContents.get(i);

        if (o instanceof Element) {
          Element element = (Element) o;

          if (index == j) {
            String className = element.getClassname();

            if (className != null)
              return className;

            Type type = _schema.getType(element.getType());

            if (type != null)
              return type.getClassname();

            return null;
          }

          j++;
        }
      }
    }

    return null;
  }

  public String getArgumentName(int index)
  {
    if (_contents == null)
      return null;

    if (_contents.size() == 0)
      return null;

    if (_contents.size() != 1)
      return null;

    if (_contents.get(0) instanceof Sequence) {
      Sequence sequence = (Sequence) _contents.get(0);
      List<Object> sequenceContents = sequence.getContents();

      int j = 0; 

      for (int i = 0; i < sequenceContents.size(); i++) {
        Object o = sequenceContents.get(i);

        if (o instanceof Element) {
          Element element = (Element) o;

          if (index == j)
            return element.getName();

          j++;
        }
      }
    }

    return null;
  }

  public void writeJava(File outputDirectory, String pkg)
    throws IOException
  {
    if (! _emit)
      return;

    File dir = new File(outputDirectory, pkg.replace(".", File.separator));

    dir.mkdirs();

    File output = new File(dir, getClassname() + ".java");
    WriteStream os = null;

    try {
      os = Vfs.openWrite(output.toString());
      JavaWriter out = new JavaWriter(os);

      out.println("package " + pkg + ";");
      out.println();
      out.println("import java.math.BigDecimal;");
      out.println("import java.math.BigInteger;");
      out.println("import java.util.List;");
      out.println("import javax.xml.bind.annotation.*;");
      out.println();

      out.print("@XmlType(name=\"" + getName() + "\"");
      if (_schema != null && _schema.getTargetNamespace() != null)
        out.print(", namespace=\"" + _schema.getTargetNamespace() + "\"");
      out.println(")");

      out.println("public class " + getClassname() + " {");

      out.pushDepth();
      
      if (_contents != null) {
        for (int i = 0; i < _contents.size(); i++) {
          if (_contents.get(i) instanceof Sequence) {
            Sequence sequence = (Sequence) _contents.get(i);

            for (Object o : sequence.getContents()) {
              if (o instanceof Element) {
                ((Element) o).generateJavaField(out);
                out.println();
              }
            }
          }
        }
      }

      out.popDepth();

      out.println("}");
    }
    finally {
      if (os != null)
        os.close();
    }

    if (_emitFaultWrapper) {
      os = null;
      output = new File(dir, getFaultWrapperClassname() + ".java");

      try {
        os = Vfs.openWrite(output.toString());
        JavaWriter out = new JavaWriter(os);

        out.println("package " + pkg + ";");
        out.println();
        out.println("import java.math.BigDecimal;");
        out.println("import java.math.BigInteger;");
        out.println("import java.util.List;");
        out.println();
        out.println("public class " + getFaultWrapperClassname());
        out.pushDepth();

        out.println("extends Exception {");
        out.println();
        out.println("private " + getClassname() + " _faultInfo;");
        out.println();

        out.print("public " + getFaultWrapperClassname() + "(String message, ");
        out.println(getClassname() + " faultInfo)");
        out.println("{");
        out.pushDepth();

        out.println("super(message);");
        out.println("_faultInfo = faultInfo;");

        out.popDepth();
        out.println("}");
        out.println();

        out.print("public " + getFaultWrapperClassname() + "(String message, ");
        out.println(getClassname() + " faultInfo, Throwable cause)");
        out.println("{");
        out.pushDepth();

        out.println("super(message, cause);");
        out.println("_faultInfo = faultInfo;");

        out.popDepth();
        out.println("}");
        out.println();

        out.println("public " + getClassname() + " getFaultInfo()");
        out.println("{");
        out.pushDepth();

        out.println("return _faultInfo;");

        out.popDepth();
        out.println("}");

        out.popDepth();
        out.println("}");
      }
      finally {
        if (os != null)
          os.close();
      }
    }
  }

  public String getClassname()
  {
    if (_className == null) {
      _className = Character.toUpperCase(getName().charAt(0)) +
                   (getName().length() > 1 ? getName().substring(1) : "");
    }
      
    return _className;
  }

  public String getFaultWrapperClassname()
  {
    return getClassname() + "_Exception";
  }

  public void setEmit(boolean emit)
  {
    _emit = emit;
  }

  public void setEmitFaultWrapper(boolean emitFaultWrapper)
  {
    _emitFaultWrapper = emitFaultWrapper;
  }

  public String toString()
  {
    return "ComplexType[" + _name + "]";
  }
}
