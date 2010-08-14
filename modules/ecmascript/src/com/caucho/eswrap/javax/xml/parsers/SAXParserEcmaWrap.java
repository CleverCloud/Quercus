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

package com.caucho.eswrap.javax.xml.parsers;

import com.caucho.es.Call;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;

import org.xml.sax.HandlerBase;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import java.io.IOException;
import java.io.InputStream;

public class SAXParserEcmaWrap {
  public static void parse(SAXParser parser, Call call, int length)
    throws Throwable
  {
    Object obj = call.getArgObject(0, length);
    Object objBase = call.getArgObject(1, length);

    if (objBase instanceof HandlerBase) {
      HandlerBase base = (HandlerBase) objBase;
      if (obj instanceof InputStream)
        parser.parse((InputStream) obj, base);
      else if (obj instanceof Path) {
        Path path = (Path) obj;
        ReadStream is = path.openRead();
        try {
          parser.parse(is, base);
        } finally {
          is.close();
        }
      }
      else if (obj instanceof String) {
        parser.parse((String) obj, base);
      }
      else
        throw new IOException();
    }
    else {
      DefaultHandler base = (DefaultHandler) objBase;
      if (obj instanceof InputStream)
        parser.parse((InputStream) obj, base);
      else if (obj instanceof Path) {
        Path path = (Path) obj;
        ReadStream is = path.openRead();
        try {
          parser.parse(is, base);
        } finally {
          is.close();
        }
      }
      else if (obj instanceof String) {
        parser.parse((String) obj, base);
      }
      else if (obj instanceof InputSource) {
        parser.parse((InputSource) obj, base);
      }
      else
        throw new IOException();
    }
  }
}
