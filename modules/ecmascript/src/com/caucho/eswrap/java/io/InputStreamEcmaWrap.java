/*
 * Copyright (c) 1998-199 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 *
 * $Id: InputStreamEcmaWrap.java,v 1.1.1.1 2004/09/11 05:14:19 cvs Exp $
 */

package com.caucho.eswrap.java.io;

import com.caucho.util.CharBuffer;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamEcmaWrap {
  public static int readByte(InputStream is)
  throws IOException
  {
    return is.read();
  }

  public static String read(InputStream is)
  throws IOException
  {
    int ch = is.read();

    if (ch == -1)
      return null;

    return String.valueOf((char) ch);
  }

  public static String read(InputStream is, int length)
  throws IOException
  {
    CharBuffer bb = new CharBuffer();

    for (; length > 0; length--) {
      int ch = is.read();

      if (ch == -1)
        break;

      bb.append((char) ch);
    }

    if (bb.length() == 0)
      return null;

    return bb.toString();
  }

  public static String readln(InputStream is)
  throws IOException
  {
    CharBuffer bb = new CharBuffer();

    boolean hasCr = false;
    boolean hasData = false;
    while (true) {
      int ch = is.read();

      if (ch == -1)
        break;
      else if (ch == '\n') {
        hasData = true;
        break;
      }
      else if (hasCr) {
        hasData = true;
        break;
      }

      if (ch == '\r')
        hasCr = true;
      else
        bb.append((char) ch);
    }

    if (bb.length() == 0 && ! hasData)
      return null;

    return bb.toString();
  }
}

