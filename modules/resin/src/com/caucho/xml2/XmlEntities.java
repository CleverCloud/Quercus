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

import com.caucho.util.IntMap;

import java.io.IOException;

/**
 * Standard XML entities.
 */
class XmlEntities extends Entities {
  private static Entities _xmlEntities = new XmlEntities();
  private IntMap _entities;

  static Entities create()
  {
    return _xmlEntities;
  }

  protected XmlEntities()
  {
    _entities = new IntMap();
    _entities.put("lt", '<');
    _entities.put("gt", '>');
    _entities.put("amp", '&');
    _entities.put("quot", '"');
    _entities.put("apos", '\'');
  }

  int getEntity(String entity)
  {
    return _entities.get(entity);
  }

  /**
   * Print some text, replacing entities.
   */
  void printText(XmlPrinter os,
                 char []text, int offset, int length,
                 boolean attr)
    throws IOException
  {
    for (int i = 0; i < length; i++) {
      char ch = text[offset + i];

      switch (ch) {
      case '\t': 
        os.print(ch);
        break;
        
      case '\n':
      case '\r': 
        if (! attr)
          os.print(ch);
        else {
          os.print("&#");
          os.print((int) ch);
          os.print(";");
        }
        break;

      case '<':
          os.print("&lt;");
          /*
        if (! attr)
          os.print("&lt;");
        else
          os.print("<");
          */
        break;

      case '>':
          os.print("&gt;");
          /*
        if (! attr)
          os.print("&gt;");
        else
          os.print("<");
          */
        break;
        
      case '&':
        os.print("&amp;");
        break;
        
      case '"':
        if (attr)
          os.print("&quot;");
        else
          os.print('"');
        break;

      default:
        if (ch >= 0x20 && ch < 0x7f || XmlChar.isChar(ch)) {
          try {
            os.print(ch);
          } catch (IOException e) {
            os.print("&#");
            os.print((int) ch);
            os.print(';');
          }
        }
        else {
          os.print("&#");
          os.print((int) ch);
          os.print(';');
        }
      }
    }
  }
}
