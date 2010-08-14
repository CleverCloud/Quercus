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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import javax.xml.bind.Unmarshaller;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import static com.caucho.soap.wsdl.WSDLConstants.*;

/**
 * WSDL PortType definition
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="portType", namespace=WSDL_NAMESPACE)
public class WSDLPortType extends WSDLNamedExtensibleAttributeDocumented 
                          implements WSDLDefinition
{
  @XmlElement(name="operation", namespace=WSDL_NAMESPACE)
  private List<WSDLOperation> _operations;

  @XmlTransient
  private WSDLDefinitions _definitions;

  private final Map<String,WSDLOperation> _operationMap 
    = new HashMap<String,WSDLOperation>();

  /**
   * Adds an operation.
   */
  public void addOperation(WSDLOperation operation)
  {
    if (_operations == null)
      _operations = new ArrayList<WSDLOperation>();

    _operations.add(operation);
  }

  /**
   * Returns the operations.
   */
  public List<WSDLOperation> getOperations()
  {
    if (_operations == null)
      _operations = new ArrayList<WSDLOperation>();

    return _operations;
  }

  public void afterUnmarshal(Unmarshaller u, Object o)
  {
    if (_operations != null) {
      for (WSDLOperation operation : _operations)
        _operationMap.put(operation.getName(), operation);
    }
  }

  public WSDLOperation getOperation(String name)
  {
    return _operationMap.get(name);
  }

  public WSDLDefinitions getDefinitions()
  {
    return _definitions;
  }

  public void setDefinitions(WSDLDefinitions definitions)
  {
    _definitions = definitions;
  }
}
