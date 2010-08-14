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
import javax.xml.stream.events.Characters;
import java.io.IOException;
import java.io.Writer;

public class CharactersImpl extends XMLEventImpl implements Characters {
  private final String _data;
  private final boolean _isCData;
  private final boolean _isIgnorableWhiteSpace;
  private final boolean _isWhiteSpace;

  public CharactersImpl(String data, boolean isCData, 
                        boolean isIgnorableWhiteSpace, boolean isWhiteSpace)
  {
    _data = data;
    _isCData = isCData;
    _isIgnorableWhiteSpace = isIgnorableWhiteSpace;
    _isWhiteSpace = isWhiteSpace;
  }

  public String getData()
  {
    return _data;
  }

  public boolean isCData()
  {
    return _isCData;
  }

  public boolean isIgnorableWhiteSpace()
  {
    return _isIgnorableWhiteSpace;
  }

  public boolean isWhiteSpace()
  {
    return _isWhiteSpace;
  }

  public int getEventType()
  {
    if (_isCData)
      return CDATA;
    else if (_isWhiteSpace)
      return SPACE;
    else if (_isIgnorableWhiteSpace)
      return SPACE;

    return CHARACTERS;
  }

  public void writeAsEncodedUnicode(Writer writer) 
    throws XMLStreamException
  {
    try {
      writer.write(_data);
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public String toString()
  {
    return "Characters[" + _data + "]";
  }

  public boolean equals(Object o) 
  {
    if (! (o instanceof Characters))
      return false;
    if (o == null)
      return false;
    if (this == o)
      return true;

    Characters characters = (Characters) o;

    return getData().equals(characters.getData()) &&
           isCData() == characters.isCData() &&
           isIgnorableWhiteSpace() == characters.isIgnorableWhiteSpace() &&
           isWhiteSpace() == characters.isWhiteSpace();
  }
}

