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
 * @author Scott Ferguson
 */

package com.caucho.soap.wsdl;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import static com.caucho.soap.wsdl.WSDLConstants.*;

import com.caucho.java.JavaWriter;

import com.caucho.xml.schema.Type;

/**
 * WSDL operation definition
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="tOperation", namespace=WSDL_NAMESPACE)
public class WSDLOperation extends WSDLNamedExtensibleDocumented {

  @XmlElement(name="input", namespace=WSDL_NAMESPACE)
  private List<WSDLOperationInput> _inputs;

  @XmlElement(name="output", namespace=WSDL_NAMESPACE)
  private List<WSDLOperationOutput> _outputs;

  @XmlElement(name="fault", namespace=WSDL_NAMESPACE)
  private List<WSDLOperationFault> _faults;

  @XmlAttribute(name="parameterOrder")
  private List<String> _parameterOrder;

  @XmlTransient
  private WSDLPortType _portType;

  public List<String> getParameterOrder()
  {
    return _parameterOrder;
  }

  public void addParameterOrder(String param)
  {
    if (_parameterOrder == null)
      _parameterOrder = new ArrayList<String>();

    _parameterOrder.add(param);
  }

  public List<WSDLOperationInput> getInputs()
  {
    if (_inputs == null)
      _inputs = new ArrayList<WSDLOperationInput>();

    return _inputs;
  }

  public List<WSDLOperationOutput> getOutputs()
  {
    if (_outputs == null)
      _outputs = new ArrayList<WSDLOperationOutput>();

    return _outputs;
  }

  public List<WSDLOperationFault> getFaults()
  {
    if (_faults == null)
      _faults = new ArrayList<WSDLOperationFault>();

    return _faults;
  }

  public void setPortType(WSDLPortType portType)
  {
    _portType = portType;
  }

  public WSDLPortType getPortType()
  {
    return _portType;
  }

  public void generateJava(JavaWriter out)
    throws IOException
  {
    WSDLDefinitions wsdl = _portType.getDefinitions();

    out.print("public ");

    if (_outputs == null || _outputs.size() == 0)
      out.print("void ");
    else {
      WSDLOperationOutput output = _outputs.get(0);
      WSDLMessage message = output.getMessage();
      List<WSDLPart> parts = message.getParts();

      if (parts.size() == 0)
        out.print("void ");
      else {
        Type type = parts.get(0).getType();
        type.setEmit(false);

        out.print(type.getJavaType(0));
        out.print(" ");
      }

      //XXX more output parts?
    }

    out.print(getName());
    out.print("(");

    if (_inputs != null && _inputs.size() > 0) {
      WSDLOperationInput input = _inputs.get(0);
      WSDLMessage message = input.getMessage();
      List<WSDLPart> parts = message.getParts();

      for (int i = 0; i < parts.size(); i++) {
        Type type = parts.get(i).getType();
        type.setEmit(false);

        int j = 0;

        for (String name = type.getArgumentName(j);
             name != null;
             name = type.getArgumentName(j)) {
          if (j > 0) 
            out.print(", ");

          out.print("@WebParam(name=\"");
          out.print(name);
          out.print("\") ");

          String javaType = type.getJavaType(j);
          out.print(javaType);
          out.print(" arg" + j);
          j++;
        }
      }

      //XXX more input parts?
    }

    out.print(")");

    if (_faults != null && _faults.size() > 0) {
      out.println();
      out.pushDepth();
      out.print("throws ");

      WSDLOperationFault fault = _faults.get(0);
      WSDLMessage message = fault.getMessage();
      List<WSDLPart> parts = message.getParts();

      for (int i = 0; i < parts.size(); i++) {
        Type type = parts.get(i).getType();
        type.setEmitFaultWrapper(true);

        if (i > 0)
          out.print(", ");

        out.print(type.getFaultWrapperClassname());
      }

      out.popDepth();

      //XXX more fault parts?
    }

    out.println(";");
  }
}
