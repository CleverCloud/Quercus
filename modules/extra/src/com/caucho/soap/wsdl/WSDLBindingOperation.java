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

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import static com.caucho.soap.wsdl.WSDLConstants.*;

import com.caucho.java.JavaWriter;

/**
 * WSDL operation definition
 */
@XmlType(name="tBindingOperation", namespace=WSDL_NAMESPACE)
public class WSDLBindingOperation extends WSDLNamedExtensibleDocumented {
  @XmlElement(name="input", namespace=WSDL_NAMESPACE)
  private WSDLBindingOperationMessage _input;

  @XmlElement(name="output", namespace=WSDL_NAMESPACE)
  private WSDLBindingOperationMessage _output;

  @XmlElement(name="fault", namespace=WSDL_NAMESPACE)
  private List<WSDLBindingOperationFault> _faults;

  @XmlTransient
  private WSDLOperation _operation;

  public void setInput(WSDLBindingOperationMessage input)
  {
    _input = input;
  }

  /**
   * Returns the input.
   */
  public WSDLBindingOperationMessage getInput()
  {
    return _input;
  }

  public void setOutput(WSDLBindingOperationMessage output)
  {
    _output = output;
  }

  /**
   * Returns the output.
   */
  public WSDLBindingOperationMessage getOutput()
  {
    return _output;
  }

  public void addFault(WSDLBindingOperationFault fault)
  {
    if (_faults == null)
      _faults = new ArrayList<WSDLBindingOperationFault>();

    _faults.add(fault);
  }

  public List<WSDLBindingOperationFault> getFaults()
  {
    return _faults;
  }

  public void setOperation(WSDLOperation operation)
  {
    _operation = operation;
  }

  public WSDLOperation getOperation()
  {
    return _operation;
  }

  public void generateJava(JavaWriter out)
    throws IOException
  {
    out.println("@WebMethod");
    //out.println("@WebResult");
    getOperation().generateJava(out);
  }
}
