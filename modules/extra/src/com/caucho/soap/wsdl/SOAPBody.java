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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.List;

/**
 * SOAP body definition
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="body", namespace="http://schemas.xmlsoap.org/wsdl/soap/")
public class SOAPBody extends WSDLExtensibilityElement {
  @XmlAttribute(name="parts")
  private List<String> _parts;

  @XmlAttribute(name="encodingStyle")
  private List<String> _encodingStyle;

  @XmlAttribute(name="namespace")
  private String _namespace;

  @XmlAttribute(name="use")
  private SOAPUseChoice _use;

  // XXX Added to accommodate WSDL given in TCK, but not in schema!
  @XmlAttribute(name="message")
  private QName _message;

  public void addPart(String part)
  {
    if (_parts == null)
      _parts = new ArrayList<String>();

    _parts.add(part);
  }

  public List<String> getParts()
  {
    if (_parts == null)
      _parts = new ArrayList<String>();

    return _parts;
  }

  public void addEncodingStyle(String encodingStyle)
  {
    if (_encodingStyle == null)
      _encodingStyle = new ArrayList<String>();

    _encodingStyle.add(encodingStyle);
  }

  public List<String> getEncodingStyle()
  {
    if (_encodingStyle == null)
      _encodingStyle = new ArrayList<String>();

    return _encodingStyle;
  }

  public void setNamespace(String namespace)
  {
    _namespace = namespace;
  }

  public String getNamespace()
  {
    return _namespace;
  }

  public void setUse(SOAPUseChoice use)
  {
    _use = use;
  }

  public SOAPUseChoice getUse()
  {
    return _use;
  }
}
