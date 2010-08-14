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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EntityDeclaration;
import java.io.Writer;

public class EntityDeclarationImpl extends XMLEventImpl 
  implements EntityDeclaration
{
  private final String _baseURI;
  private final String _name;
  private final String _notationName;
  private final String _publicId;
  private final String _replacementText;
  private final String _systemId;

  public EntityDeclarationImpl(String baseURI, String name, String notationName,
                               String publicId, String replacementText,
                               String systemId)
  {
    _baseURI = baseURI;
    _name = name;
    _notationName = notationName;
    _publicId = publicId;
    _replacementText = replacementText;
    _systemId = systemId;
  }

  public String getBaseURI()
  {
    return _baseURI;
  }

  public String getName()
  {
    return _name;
  }

  public String getNotationName()
  {
    return _notationName;
  }

  public String getPublicId()
  {
    return _publicId;
  }

  public String getReplacementText()
  {
    return _replacementText;
  }

  public String getSystemId()
  {
    return _systemId;
  }

  public int getEventType()
  {
    return ENTITY_DECLARATION;
  }

  public void writeAsEncodedUnicode(Writer writer) 
    throws XMLStreamException
  {
    // XXX
  }

  public boolean equals(Object o) 
  {
    if (! (o instanceof EntityDeclaration))
      return false;
    if (o == null)
      return false;
    if (this == o)
      return true;

    EntityDeclaration entity = (EntityDeclaration) o;
    
    return getBaseURI().equals(entity.getBaseURI()) &&
           getName().equals(entity.getName()) &&
           getNotationName().equals(entity.getNotationName()) &&
           getPublicId().equals(entity.getPublicId()) &&
           getReplacementText().equals(entity.getReplacementText()) &&
           getSystemId().equals(entity.getSystemId());
  }
}

