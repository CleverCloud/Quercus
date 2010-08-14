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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

import static com.caucho.soap.wsdl.WSDLConstants.*;

/**
 * WSDL binding definition
 */
@XmlType(name="tBinding", namespace=WSDL_NAMESPACE)
public class WSDLBinding extends WSDLNamedExtensibleDocumented
                         implements WSDLDefinition {
  @XmlElement(name="operation", namespace=WSDL_NAMESPACE)
  protected List<WSDLBindingOperation> _operations;

  @XmlAttribute(required=true, name="type")
  protected QName _type;

  @XmlTransient
  private WSDLPortType _portType;

  @XmlTransient
  public void setPortType(WSDLPortType portType)
  {
    _portType = portType;
  }

  public WSDLPortType getPortType()
  {
    return _portType;
  }

  /**
   * Sets the binding type.
   */
  public void setType(QName type)
  {
    _type = type;
  }

  public QName getType()
  {
    return _type;
  }

  /**
   * Adds an operation.
   */
  public void addOperation(WSDLBindingOperation operation)
  {
    if (_operations == null)
      _operations = new ArrayList<WSDLBindingOperation>();

    _operations.add(operation);
  }

  public List<WSDLBindingOperation> getOperations()
  {
    return _operations;
  }

  public String toString()
  {
    return "WSDLBinding[" + getName() + "]";
  }
}
