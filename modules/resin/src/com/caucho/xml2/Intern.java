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

import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempCharBuffer;
import com.caucho.vfs.Vfs;
import com.caucho.xml.ExtendedLocator;
import com.caucho.xml.QName;
import com.caucho.xml.XmlChar;

import org.xml.sax.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;

/**
 * Interning names
 */
public class Intern {
  private static final int SIZE = 203;
  
  private final InternQName []_entries = new InternQName[SIZE];

  InternQName add(char []buffer, int offset, int length, int colon)
  {
    int hash = 0;

    for (int i = length - 1; i >= 0; i--) {
      hash = 37 * hash + buffer[offset + i];
    }

    int bucket = (hash & 0x7fffffff) % SIZE;

    InternQName qName;
    
    for (qName = _entries[bucket];
         qName != null;
         qName = qName._next) {
      if (qName.match(buffer, offset, length))
        return qName;
    }

    qName = new InternQName(_entries[bucket], buffer, offset, length, colon);
    _entries[bucket] = qName;

    return qName;
  }
}
