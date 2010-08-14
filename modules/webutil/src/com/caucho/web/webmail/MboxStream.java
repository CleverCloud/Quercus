/*
 * Copyright (c) 1998-2001 Caucho Technology -- all rights reserved
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

package com.caucho.web.webmail;

import com.caucho.util.ByteBuffer;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.StreamImpl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

/*
 * A stream for reading mbox files.
 *
 * <p/>Each call to openRead() will return the next part and return null
 * when complete.
 *
 * <pre><code>
 * MboxStream mbox = new MboxStream(rawIs);
 * ReadStream is;
 *
 * while ((is = multi.openRead()) != null) {
 *   // read from is as a normal stream
 * }
 */
public class MboxStream extends StreamImpl {
  private byte []boundaryBuffer = "From ".getBytes();
  private int boundaryLength = 5;
  
  private ByteBuffer peekBuffer = new ByteBuffer();
  private byte []peek;
  private int peekOffset;
  private int peekLength;

  private byte []dummyBuffer = new byte[32];
  
  private ReadStream is;
  private ReadStream readStream;
  private boolean isPartDone;
  private boolean isDone;
  private HashMap headers = new HashMap();
  private CharBuffer line = new CharBuffer();

  private String defaultEncoding;

  public MboxStream()
    throws IOException
  {
  }

  public MboxStream(ReadStream is)
    throws IOException
  {
    this();

    init(is);
  }

  /**
   * Returns the default encoding.
   */
  public String getEncoding()
  {
    return defaultEncoding;
  }

  /**
   * Sets the default encoding.
   */
  public void setEncoding(String encoding)
  {
    this.defaultEncoding = encoding;
  }

  /**
   * Initialize the multipart stream with a given boundary.  The boundary
   * passed to <code>init</code> will have "--" prefixed.
   *
   * @param is the underlying stream
   * @param headerBoundary the multipart/mime boundary.
   */
  public void init(ReadStream is)
    throws IOException
  {
    this.is = is;
    
    peekBuffer.setLength(boundaryLength + 5);
    peek = peekBuffer.getBuffer();
    peekOffset = 0;
    peekLength = 0;
    peek[peekLength++] = (byte) '\n';

    isPartDone = false;
    isDone = false;
    
    while (read(dummyBuffer, 0, dummyBuffer.length) >= 0) {
    }
    
    isPartDone = true;
  }

  /**
   * Opens the next message of the mbox stream for reading.  Returns
   * null when the last message is read.
   */
  public ReadStream openRead()
    throws IOException
  {
    if (isDone)
      return null;
    else if (readStream == null)
      readStream = new ReadStream(this, null);
    else if (! isPartDone) {
      int len;
      while ((len = read(dummyBuffer, 0, dummyBuffer.length)) >= 0) {
      }

      if (isDone)
        return null;
    }
    
    readStream.init(this, null);

    isPartDone = false;
    
    if (scanHeaders()) {
      String contentType = (String) getAttribute("content-type");
      
      String charset = getAttributePart(contentType, "charset");
      
      if (charset != null)
        readStream.setEncoding(charset);
      else if (defaultEncoding != null)
        readStream.setEncoding(defaultEncoding);
      
      return readStream;
    }
    else {
      isDone = true;
      readStream.close();
      return null;
    }
  }

  /**
   * Returns a read attribute from the multipart mime.
   */
  public Object getAttribute(String key)
  {
    return headers.get(key.toLowerCase());
  }

  /**
   * Returns the headers from the mime.
   */
  public Iterator getAttributeNames()
  {
    return headers.keySet().iterator();
  }

  /**
   * Scans the mime headers.  The mime headers are in standard mail/http
   * header format: "key: value".
   */
  private boolean scanHeaders()
    throws IOException
  {
    int ch = read() ;

    headers.clear();
    while (ch > 0 && ch != '\n' && ch != '\r') {
      line.clear();

      line.append((char) ch);
      for (ch = read();
           ch >= 0 && ch != '\n' && ch != '\r';
           ch = read()) {
        line.append((char) ch);
      }

      if (ch == '\r') {
        if ((ch = read()) == '\n')
          ch = read();
      } else if (ch == '\n')
        ch = read();

      int i = 0;
      for (; i < line.length() && line.charAt(i) != ':'; i++) {
      }

      String key = null;
      String value = null;
      if (i < line.length()) {
        key = line.substring(0, i).trim().toLowerCase();
        value = line.substring(i + 1).trim();

        headers.put(key, value);
      }
    }
    
    if (ch == '\r') {
      if ((ch = read()) != '\n') {
        peek[0] = (byte) ch;
        peekOffset = 0;
        peekLength = 1;
      }
    }

    return true;
  }
  
  public boolean canRead()
  {
    return true;
  }

  /**
   * Reads from the multipart mime buffer.
   */
  public int read(byte []buffer, int offset, int length) throws IOException
  {
    int b = -1;

    if (isPartDone)
      return -1;

    int i = 0;
    while (i < length && (b = read()) >= 0) {
      boolean hasCr = false;

      if (b == '\r') {
        hasCr = true;
        b = read();

        // XXX: Macintosh?
        if (b != '\n') {
          buffer[offset + i++] = (byte) '\r';
          peek[0] = (byte) b;
          peekOffset = 0;
          peekLength = 1;
          continue;
        }
      }
      else if (b != '\n') {
        buffer[offset + i++] = (byte) b;
        continue;
      }

      int j;
      for (j = 0;
           j < boundaryLength && (b = read()) >= 0 && boundaryBuffer[j] == b;
           j++) {
      }

      if (j == boundaryLength) {
        isPartDone = true;

        while ((b = read()) >= 0 && b != '\r' && b != '\n') {
        }
        
        return 1;
      }

      peekLength = 0;
      if (hasCr && i + 1 < length) {
        buffer[offset + i++] = (byte) '\r';
        buffer[offset + i++] = (byte) '\n';
      }
      else if (hasCr) {
        buffer[offset + i++] = (byte) '\r';
        peek[peekLength++] = (byte) '\n';
      }
      else {
        buffer[offset + i++] = (byte) '\n';
      }

      int k = 0;
      while (k < j && i + 1 < length)
        buffer[offset + i++] = boundaryBuffer[k++];

      while (k < j)
        peek[peekLength++] = boundaryBuffer[k++];

      peek[peekLength++] = (byte) b;
      peekOffset = 0;
    }

    if (i <= 0) {
      isPartDone = true;
      if (b < 0)
        isDone = true;
      return -1;
    }
    else {
      return i;
    }
  }

  /**
   * Read the next byte from the peek or from the underlying stream.
   */
  private int read()
    throws IOException
  {
    if (peekOffset < peekLength)
      return peek[peekOffset++] & 0xff;
    else
      return is.read();
  }

  private static String getAttributePart(String attr, String name)
  {
    if (attr == null)
      return null;
    
    int length = attr.length();
    int i = attr.indexOf(name);
    if (i < 0)
      return null;

    for (i += name.length(); i < length && attr.charAt(i) != '='; i++) {
    }
    
    for (i++; i < length && attr.charAt(i) == ' '; i++) {
    }

    CharBuffer value = CharBuffer.allocate();
    if (i < length && attr.charAt(i) == '\'') {
      for (i++; i < length && attr.charAt(i) != '\''; i++)
        value.append(attr.charAt(i));
    }
    else if (i < length && attr.charAt(i) == '"') {
      for (i++; i < length && attr.charAt(i) != '"'; i++)
        value.append(attr.charAt(i));
    }
    else if (i < length) {
      char ch;
      for (; i < length && (ch = attr.charAt(i)) != ' ' && ch != ';'; i++)
        value.append(ch);
    }

    return value.close();
  }
}
