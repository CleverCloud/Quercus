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

import org.xml.sax.Attributes;
import javax.xml.namespace.QName;

class QAttributes implements Attributes {
  QName []names = new QName[32];
  String []values = new String[32];
  int size;

  void clear()
  {
    size = 0;
  }

  void add(QName name, String value)
  {
    if (size == names.length) {
      QName []newNames = new QName[2 * names.length];
      String []newValues = new String[2 * names.length];
      System.arraycopy(names, 0, newNames, 0, names.length);
      System.arraycopy(values, 0, newValues, 0, names.length);
      names = newNames;
      values = newValues;
    }
    
    names[size] = name;
    values[size] = value;
    size++;
  }

  public int getLength()
  {
    return size;
  }    

  public QName getName(int i)
  {
    return names[i];
  }    

  public String getQName(int i)
  {
    String prefix = names[i].getPrefix();

    if (prefix != null && prefix.length() > 0)
      return prefix + ":" + names[i].getLocalPart();
    else
      return names[i].getLocalPart();
  }    

  public String getURI(int i)
  {
    String uri = names[i].getNamespaceURI();

    if (uri != null)
      return uri;
    else
      return ""; 
  }    

  public String getLocalName(int i)
  {
    String name = names[i].getLocalPart();

    if (name != null)
      return name;
    else
      return ""; 
  }    

  public String getValue(int i)
  {
    return values[i];
  }    

  public String getValue(String qName)
  {
    for (int i = 0; i < size; i++) {
      if (qName.equals(names[i].getLocalPart()))
        return values[i];
    }

    return null;
  }    

  public String getValue(String uri, String localName)
  {
    for (int i = 0; i < size; i++) {
      String testURI = names[i].getNamespaceURI();

      if (testURI == null)
        testURI = "";
      
      if (uri.equals(testURI) && localName.equals(names[i].getLocalPart()))
        return values[i];
    }

    return null;
  }    

  public int getIndex(String qName)
  {
    for (int i = 0; i < size; i++) {
      if (qName.equals(names[i].getLocalPart()))
        return i;
    }

    return -1;
  }    

  public int getIndex(String uri, String localName)
  {
    for (int i = 0; i < size; i++) {
      if (uri.equals(names[i].getNamespaceURI()) &&
          localName.equals(names[i].getLocalPart()))
        return i;
    }

    return -1;
  }    

  public String getType(int i)
  {
    return "CDATA";
  }    

  public String getType(String uri, String localName)
  {
    return "CDATA";
  }    

  public String getType(String qName)
  {
    return "CDATA";
  }

  public String toString()
  {
    StringBuilder cb = new StringBuilder();
    cb.append("QAttributes[");
    for (int i = 0; i < size; i++) {
      cb.append(" ");
      cb.append(names[i]);
      cb.append("=\"");
      cb.append(values[i]);
      cb.append("\"");
    }
    cb.append("]");
    
    return cb.toString();
  }
}
