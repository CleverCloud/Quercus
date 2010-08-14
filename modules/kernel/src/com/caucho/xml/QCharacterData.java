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
 * @author Scott Ferguson
 */

package com.caucho.xml;

import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;

import java.io.IOException;

abstract class QCharacterData extends QAbstractNode implements CharacterData {
  protected String _data;
  protected boolean _whitespaceOnly;

  /**
   * Creates new character data with initial data.
   */
  QCharacterData()
  {
  }

  /**
   * Creates new character data with initial data.
   */
  QCharacterData(String data)
  {
    _data = data;
  }

  /**
   * Returns the node value.  For QCharacterData, this is the text value.
   */
  public String getNodeValue()
  {
    return _data;
  }

  /**
   * Sets the node value.  For QCharacterData, this is the text value.
   */
  public void setNodeValue(String data)
  {
    _data = data;
  }

  /**
   * Returns the node value.  For QCharacterData, this is the text value.
   */
  public String getData()
  {
    return _data;
  }

  /**
   * Sets the node value.  For QCharacterData, this is the text value.
   */
  public void setData(String data)
  {
    _data = data;
  }

  /**
   * Returns the length of the text data.
   */
  public int getLength()
  {
    return _data.length();
  }

  public String substringData(int start, int count)
    throws DOMException
  { 
    if (start + count >= _data.length())
      return _data.substring(start);
    else
      return _data.substring(start, start + count);
  }

  public String substringData(int start)
    throws DOMException
  { 
    return _data.substring(start);
  }

  public void appendData(String arg)
    throws DOMException
  { 
    _data = _data + arg;
  }

  public void insertData(int offset, String arg)
    throws DOMException
  { 
    _data = _data.substring(0, offset) + arg + _data.substring(offset);
  }

  public void deleteData(int offset, int count)
    throws DOMException
  { 
    if (_data.length() <= offset + count)
      _data = _data.substring(0, offset);
    else
      _data = _data.substring(0, offset) + _data.substring(offset + count);
  }

  public void deleteData(int offset)
    throws DOMException
  { 
    _data = _data.substring(0, offset);
  }

  public void replaceData(int offset, int count, String arg)
    throws DOMException
  { 
    if (_data.length() <= offset + count)
      _data = _data.substring(0, offset) + arg;
    else
      _data = _data.substring(0, offset) + arg + _data.substring(offset + count);
  }

  public boolean hasContent() 
  {
    for (int i = 0; i < _data.length(); i++)
      if (! Character.isWhitespace(_data.charAt(i)))
        return true;

    return false;
  }

  public boolean isElementContentWhitespace() 
  {
    for (int i = 0; i < _data.length(); i++)
      if (! Character.isWhitespace(_data.charAt(i)))
        return false;

    return true;
  }

  public void print(XmlPrinter os) throws IOException
  {
    os.text(getData());
  }
}
