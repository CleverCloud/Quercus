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
* @author Scott Ferguson
*/

package com.caucho.xml.stream.events;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

public class EndElementImpl extends XMLEventImpl implements EndElement {
  private final QName _name;
  private final Iterator _namespaces;

  public EndElementImpl(QName name, Iterator namespaces)
  {
    _name = name;
    _namespaces = namespaces;
  }

  public EndElementImpl(QName name)
  {
    this(name, null);
  }

  public QName getName()
  {
    return _name;
  }

  public Iterator getNamespaces()
  {
    return _namespaces;
  }

  public int getEventType()
  {
    return END_ELEMENT;
  }

  public void writeAsEncodedUnicode(Writer writer) 
    throws XMLStreamException
  {
    try {
      writer.write("</" + _name + ">");
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public boolean equals(Object o) 
  {
    if (! (o instanceof EndElement))
      return false;
    if (o == null)
      return false;
    if (this == o)
      return true;

    EndElement end = (EndElement) o;
    
    // XXX Check namespaces

    return getName().equals(end.getName());
  }
}

