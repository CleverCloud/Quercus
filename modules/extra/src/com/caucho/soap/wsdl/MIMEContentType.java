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

/**
 * MIME content type.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="content", 
                namespace="http://schemas.xmlsoap.org/wsdl/mime/")
public class MIMEContentType extends WSDLExtensibilityElement {
  @XmlAttribute(name="type")
  private String _type;

  @XmlAttribute(name="part")
  private String _part;

  /**
   * Sets the type.
   */
  public void setType(String type)
  {
    _type = type;
  }

  public String getType()
  {
    return _type;
  }
  
  /**
   * Sets the part.
   */
  public void setPart(String part)
  {
    _part = part;
  }

  public String getPart()
  {
    return _part;
  }
}
