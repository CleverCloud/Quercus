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

package com.caucho.xml2;

import com.caucho.util.CharBuffer;
import com.caucho.xml.QName;

import org.xml.sax.Attributes;

/**
 * Implements the SAX Attributes class.
 */
class AttributesImpl implements Attributes {
  private InternQName []_names = new InternQName[32];
  private String []_values = new String[32];
  private int _size;

  /**
   * Clears the attributes.
   */
  void clear()
  {
    _size = 0;
  }

  /**
   * Adds a new attribute name and value.
   */
  void add(InternQName name, String value)
  {
    if (_size == _names.length) {
      InternQName []newNames = new InternQName[2 * _names.length];
      String []newValues = new String[2 * _names.length];
      System.arraycopy(_names, 0, newNames, 0, _names.length);
      System.arraycopy(_values, 0, newValues, 0, _names.length);
      _names = newNames;
      _values = newValues;
    }
    
    _names[_size] = name;
    _values[_size] = value;
    _size++;
  }

  /**
   * Returns the number of attributes.
   */
  public int getLength()
  {
    return _size;
  }    

  /**
   * Returns the indexed name.
   */
  /*
  public QName getName(int i)
  {
    return _names[i];
  }
  */

  /**
   * Returns the name.
   */
  public String getQName(int i)
  {
    return _names[i].getName();
  }    

  /**
   * Returns the namespace URI.
   */
  public String getURI(int i)
  {
    throw new UnsupportedOperationException();
    //return _names[i].getNamespaceURI();
  }    

  /**
   * Returns the local name.
   */
  public String getLocalName(int i)
  {
    return _names[i].getLocalName();
  }    

  /**
   * Returns the value.
   */
  public String getValue(int i)
  {
    return _values[i];
  }    

  /**
   * Returns the value with the given name.
   */
  public String getValue(String qName)
  {
    for (int i = _size - 1; i >= 0; i--) {
      if (qName.equals(_names[i].getName()))
        return _values[i];
    }

    return null;
  }    

  /**
   * Returns the value for hte uri and local name.
   */
  public String getValue(String uri, String localName)
  {
    for (int i = _size - 1; i >= 0; i--) {
      if (uri.equals(_names[i].getNamespaceURI()) &&
          localName.equals(_names[i].getLocalName()))
        return _values[i];
    }

    return null;
  }    

  /**
   * Returns the index of the given name.
   */
  public int getIndex(String qName)
  {
    for (int i = _size - 1; i >= 0; i--) {
      if (qName.equals(_names[i].getName()))
        return i;
    }

    return -1;
  }    

  /**
   * Returns the index of the matching name.
   */
  public int getIndex(String uri, String localName)
  {
    for (int i = _size -1; i >= 0; i--) {
      if (uri.equals(_names[i].getNamespaceURI()) &&
          localName.equals(_names[i].getLocalName()))
        return i;
    }

    return -1;
  }    

  /**
   * Returns the set type.
   */
  public String getType(int i)
  {
    return "CDATA";
  }    

  /**
   * Returns the set type.
   */
  public String getType(String uri, String localName)
  {
    return "CDATA";
  }    

  /**
   * Returns the set type.
   */
  public String getType(String qName)
  {
    return "CDATA";
  }

  /**
   * Returns a printable version.
   */
  public String toString()
  {
    CharBuffer cb = new CharBuffer();
    cb.append("AttributesImpl[");
    for (int i = 0; i < _size; i++) {
      cb.append(" ");
      cb.append(_names[i]);
      cb.append("=\"");
      cb.append(_values[i]);
      cb.append("\"");
    }
    cb.append("]");
    
    return cb.close();
  }
}
