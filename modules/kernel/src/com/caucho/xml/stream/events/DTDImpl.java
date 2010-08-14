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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.DTD;
import java.io.Writer;
import java.util.List;

public class DTDImpl extends XMLEventImpl implements DTD {
  private final String _dtdTypeDecl;
  private final List _entities = null; // XXX
  private final List _notations = null; // XXX
  private final Object _processedDTD = null; // XXX

  public DTDImpl(String dtdTypeDecl)
  {
    _dtdTypeDecl = dtdTypeDecl;
  }

  public String getDocumentTypeDeclaration()
  {
    return _dtdTypeDecl;
  }

  public List getEntities()
  {
    throw new UnsupportedOperationException();
  }

  public List getNotations()
  {
    throw new UnsupportedOperationException();
  }

  public Object getProcessedDTD()
  {
    throw new UnsupportedOperationException();
  }

  public int getEventType()
  {
    return DTD;
  }

  public void writeAsEncodedUnicode(Writer writer) 
    throws XMLStreamException
  {
    // XXX
  }

  public boolean equals(Object o) 
  {
    if (! (o instanceof DTD))
      return false;
    if (o == null)
      return false;
    if (this == o)
      return true;

    DTD dtd = (DTD) o;

    return 
      getDocumentTypeDeclaration().equals(dtd.getDocumentTypeDeclaration()) &&
      getEntities().equals(dtd.getEntities()) &&
      getNotations().equals(dtd.getNotations()) &&
      getProcessedDTD().equals(dtd.getProcessedDTD());
  }
}

