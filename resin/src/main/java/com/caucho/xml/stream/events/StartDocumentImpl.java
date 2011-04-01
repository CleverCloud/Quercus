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
import javax.xml.stream.events.StartDocument;
import java.io.IOException;
import java.io.Writer;

public class StartDocumentImpl extends XMLEventImpl implements StartDocument {
  private final boolean _encodingSet;
  private final String _characterEncodingScheme;
  private final String _systemId;
  private final String _version;
  private final boolean _isStandalone;
  private final boolean _standaloneSet;

  public StartDocumentImpl()
  {
    this(false, null, null, "1.0", false, false);
  }

  public StartDocumentImpl(boolean encodingSet, String characterEncodingScheme,
                           String systemId, String version, 
                           boolean isStandalone, boolean standaloneSet)
  {
    _encodingSet = encodingSet;
    _characterEncodingScheme = characterEncodingScheme;
    _systemId = systemId;
    _version = version;
    _isStandalone = isStandalone;
    _standaloneSet = standaloneSet;
  }

  public boolean encodingSet()
  {
    return _encodingSet;
  }

  public String getCharacterEncodingScheme()
  {
    return _characterEncodingScheme;
  }

  public String getSystemId()
  {
    return _systemId;
  }

  public String getVersion()
  {
    return _version;
  }

  public boolean isStandalone()
  {
    return _isStandalone;
  }

  public boolean standaloneSet()
  {
    return _standaloneSet;
  }

  public int getEventType()
  {
    return START_DOCUMENT;
  }

  public void writeAsEncodedUnicode(Writer writer) 
    throws XMLStreamException
  {
    try {
      writer.write("<?xml version=\"" + _version + "\"");

      if (_encodingSet)
        writer.write(" encoding=\"" + _characterEncodingScheme + "\"");

      if (_standaloneSet)
        writer.write(" standalone=\"" + _standaloneSet + "\"");

      writer.write("?>");

      // XXX system id?
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append("<?xml version=\"" + _version + "\"");

    if (_encodingSet)
      sb.append(" encoding=\"" + _characterEncodingScheme + "\"");

    if (_standaloneSet)
      sb.append(" standalone=\"" + _standaloneSet + "\"");

    sb.append("?>");

    return sb.toString();
  }

  public boolean equals(Object o) 
  {
    if (! (o instanceof StartDocument))
      return false;
    if (o == null)
      return false;
    if (this == o)
      return true;

    StartDocument start = (StartDocument) o;

    if (getCharacterEncodingScheme() != null) {
      if (! getCharacterEncodingScheme().equals
            (start.getCharacterEncodingScheme()))
        return false;
    }
    else if (start.getCharacterEncodingScheme() != null)
      return false;

    if (getSystemId() != null) {
      if (! getSystemId().equals(start.getSystemId()))
        return false;
    }
    else if (start.getSystemId() != null)
      return false;

    if (getVersion() != null) {
      if (! getVersion().equals(start.getVersion()))
        return false;
    }
    else if (start.getVersion() != null)
      return false;

    return encodingSet() == start.encodingSet() &&
           isStandalone() == start.isStandalone() &&
           standaloneSet() == start.standaloneSet();
  }
}

