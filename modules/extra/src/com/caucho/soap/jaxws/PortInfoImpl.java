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

package com.caucho.soap.jaxws;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.PortInfo;

public class PortInfoImpl implements PortInfo {
  private final String _bindingId;
  private final QName _portName;
  private final QName _serviceName;
  private String _endpointAddress;

  public PortInfoImpl(String bindingId, QName portName, QName serviceName)
  {
    this(bindingId, portName, serviceName, null);
  }

  public PortInfoImpl(String bindingId, QName portName, QName serviceName, 
                      String endpointAddress)
  {
    _bindingId = bindingId;
    _portName = portName;
    _serviceName = serviceName;
    _endpointAddress = endpointAddress;
  }

  public String getBindingID()
  {
    return _bindingId;
  }

  public QName getPortName()
  {
    return _portName;
  }

  public QName getServiceName()
  {
    return _serviceName;
  }

  public String getEndpointAddress()
  {
    return _endpointAddress;
  }

  public void setEndpointAddress(String endpointAddress)
  {
    _endpointAddress = endpointAddress;
  }
}
