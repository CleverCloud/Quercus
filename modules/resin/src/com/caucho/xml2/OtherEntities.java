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

import java.io.IOException;

/**
 * Encodings other than ascii and latin-1 try to write the characters in
 * the given encoding.  If the translation fails, then use a character
 * encoding.
 */
class OtherEntities extends HtmlEntities {
  private static Entities _html40;
  private static Entities _html32;

  static Entities create(double version)
  {
    if (version == 0 || version >= 4.0) {
      if (_html40 == null)
        _html40 = new OtherEntities(4.0);
      
      return _html40;
    }
    else {
      if (_html32 == null)
        _html32 = new OtherEntities(3.2);
      
      return _html32;
    }
  }

  protected OtherEntities(double version)
  {
    super(version);
  }

  void printText(XmlPrinter os,
                 char []text, int offset, int length,
                 boolean attr)
    throws IOException
  {
    for (int i = 0; i < length; i++) {
      char ch = text[i + offset];

      // ASCII codes use the standard escapes
      if (ch == '&') {
        if (i + 1 < length && text[i + 1] == '{')
          os.print('&');
        else if (attr)
          os.print(_attrLatin1[ch]);
        else
          os.print(_latin1[ch]);
      }
      else if (ch == '"') {
        if (attr)
          os.print("&quot;");
        else
          os.print('"');
      }
      else if (ch == '<') {
        if (attr)
          os.print('<');
        else
          os.print("&lt;");
      }
      else if (ch == '>') {
        if (attr)
          os.print('>');
        else
          os.print("&gt;");
      }
      else if (ch < 161)
        os.print(_latin1[ch]);
      else {
        try {
          os.print(ch);
        } catch (IOException e) {
          char []value = getSparseEntity(ch);
          if (value != null) {
            os.print(value);
          } else {
            os.print("&#");
            os.print((int) ch);
            os.print(";");
          }
        }
      }
    }
  }
}
