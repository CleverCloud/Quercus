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

package com.caucho.xml.stream.events;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import java.io.IOException;
import java.io.Writer;

public class AttributeImpl extends XMLEventImpl implements Attribute {
  private final QName _name;
  private final String _value;
  private final boolean _specified;
  private final String _dtdType;

  public AttributeImpl(QName name, String value)
  {
    this(name, value, true);
  }

  public AttributeImpl(QName name, String value, boolean specified)
  {
    this(name, value, specified, "CDATA");
  }

  public AttributeImpl(QName name, String value, boolean specified, 
                       String dtdType)
  {
    _name = name;
    _value = value;
    _specified = specified;
    _dtdType = dtdType;
  }

  public String getDTDType()
  {
    return _dtdType;
  }

  public QName getName()
  {
    return _name;
  }

  public String getValue()
  {
    return _value;
  }

  public boolean isSpecified()
  {
    return _specified;
  }

  public int getEventType()
  {
    return ATTRIBUTE;
  }

  public void writeAsEncodedUnicode(Writer writer) 
    throws XMLStreamException
  {
    try {
      writer.write(_name + "=\"" + _value + "\"");
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public String toString()
  {
    return _name + "=\"" + _value + "\"";
  }

  public boolean equals(Object o) 
  {
    if (! (o instanceof Attribute))
      return false;
    if (o == null)
      return false;
    if (this == o)
      return true;

    Attribute attr = (Attribute) o;

    return getName().equals(attr.getName()) &&
           getDTDType().equals(attr.getDTDType()) &&
           getValue().equals(attr.getValue()) &&
           isSpecified() == attr.isSpecified();
  }
}

