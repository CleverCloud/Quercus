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
import javax.xml.stream.events.Namespace;

public class NamespaceImpl extends AttributeImpl implements Namespace {
  private final String _namespaceURI;
  private final String _prefix;

  public NamespaceImpl(String namespaceURI, String prefix)
  {
    super("".equals(prefix) || prefix == null 
            ? new QName("http://www.w3.org/2000/xmlns/", "xmlns")
            : new QName("http://www.w3.org/2000/xmlns/", prefix, "xmlns"),
          namespaceURI);
    _namespaceURI = namespaceURI;
    _prefix = prefix;
  }

  public String getNamespaceURI()
  {
    return _namespaceURI;
  }

  public String getPrefix()
  {
    return _prefix;
  }

  public boolean isDefaultNamespaceDeclaration()
  {
    return _prefix == null || "".equals(_prefix);
  }

  public int getEventType()
  {
    return NAMESPACE;
  }

  public String toString()
  {
    return "xmlns:" + _prefix + "=" + _namespaceURI;
  }

  public boolean equals(Object o) 
  {
    if (! (o instanceof Namespace))
      return false;
    if (o == null)
      return false;
    if (this == o)
      return true;

    Namespace namespace = (Namespace) o;
    
    return getNamespaceURI().equals(namespace.getNamespaceURI()) &&
           getPrefix().equals(namespace.getPrefix());
  }
}
