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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.http;

import com.caucho.util.CharCursor;
import com.caucho.util.FreeList;
import com.caucho.util.HashMapImpl;
import com.caucho.util.StringCharCursor;
import com.caucho.vfs.ByteToChar;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.CharacterIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Form handling.
 */
public class Form {
  private static final Logger log = Logger.getLogger(Form.class.getName());

  private static final FreeList<Form> _freeList = new FreeList<Form>(32);

  private final ByteToChar _converter = ByteToChar.create();


  public static Form allocate()
  {
    Form form = _freeList.allocate();

    if (form == null)
      form = new Form();

    return form;
  }

  public static void free(Form form)
  {
    _freeList.free(form);
  }
  
  /**
   * Parses the values from a query string.
   *
   * @param table the hashtable which will contain the results
   * @param query the query string to evaluate
   * @param javaEncoding the Java name for the charset
   */
  public void parseQueryString(HashMapImpl<String,String[]> table,
                               String query,
                               String javaEncoding,
                               boolean isTop)
    throws IOException
  {
    CharCursor is = new StringCharCursor(query);

    ByteToChar converter = _converter;
    try {
      converter.setEncoding(javaEncoding);
    } catch (UnsupportedEncodingException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    int ch = is.current();
    while (ch != CharacterIterator.DONE) {
      for (; Character.isWhitespace((char) ch) || ch == '&'; ch = is.next()) {
      }

      converter.clear();
      for (; ch != CharacterIterator.DONE && ch != '=' && ch != '&'; ch = is.next())
        readChar(converter, is, ch, isTop);

      String key = converter.getConvertedString();

      converter.clear();
      if (ch == '=')
        ch = is.next();
      for (; ch != CharacterIterator.DONE && ch != '&'; ch = is.next())
        readChar(converter, is, ch, isTop);
      
      String value = converter.getConvertedString();

      if (log.isLoggable(Level.FINE))
        log.fine("query: " + key + "=" + value);
      
      String []oldValue = table.get(key);
      
      if (key == null || key.equals("")) {
      }
      else if (oldValue == null)
        table.put(key, new String[] { value });
      else if (isTop) {
        String []newValue = new String[oldValue.length + 1];
        System.arraycopy(oldValue, 0, newValue, 0, oldValue.length);
        newValue[oldValue.length] = value;
        table.put(key, newValue);
      } else {
        String []newValue = new String[oldValue.length + 1];
        System.arraycopy(oldValue, 0, newValue, 1, oldValue.length);
        newValue[0] = value;
        table.put(key, newValue);
      }
    }
  }

  /**
   * Scans the next character from the input stream, adding it to the
   * converter.
   *
   * @param converter the byte-to-character converter
   * @param is the form's input stream
   * @param ch the next character
   */
  private static void readChar(ByteToChar converter, CharCursor is,
                               int ch, boolean isTop)
    throws IOException
  {
    if (ch == '+') {
      if (isTop)
        converter.addByte(' ');
      else
        converter.addChar(' ');
    }
    else if (ch == '%') {
      int ch1 = is.next();

      if (ch1 == 'u') {
        ch1 = is.next();
        int ch2 = is.next();
        int ch3 = is.next();
        int ch4 = is.next();

        converter.addChar((char) ((toHex(ch1) << 12) +
                                  (toHex(ch2) << 8) + 
                                  (toHex(ch3) << 4) + 
                                  (toHex(ch4))));
      }
      else {
        int ch2 = is.next();
        
        converter.addByte(((toHex(ch1) << 4) + toHex(ch2)));
      }
    }
    else if (isTop)
      converter.addByte((byte) ch);
    else
      converter.addChar((char) ch);
  }

  /**
   * Parses the values from a post data
   *
   * @param table the hashtable which will contain the results
   * @param is an input stream containing the data
   * @param javaEncoding the Java name for the charset
   */
  void parsePostData(HashMapImpl<String,String[]> table, InputStream is,
                     String javaEncoding)
    throws IOException
  {
    ByteToChar converter = _converter;
    try {
      converter.setEncoding(javaEncoding);
    } catch (UnsupportedEncodingException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    int ch = is.read();
    while (ch >= 0) {
      for (; Character.isWhitespace((char) ch) || ch == '&'; ch = is.read()) {
      }

      converter.clear();
      for (;
           ch >= 0 && ch != '=' && ch != '&' &&
             ! Character.isWhitespace((char) ch);
           ch = is.read()) {
        readChar(converter, is, ch);
      }

      String key = converter.getConvertedString();

      for (; Character.isWhitespace((char) ch); ch = is.read()) {
      }
      
      converter.clear(); 
      if (ch == '=') {
        ch = is.read();
        for (; Character.isWhitespace((char) ch); ch = is.read()) {
        }
      }
      
      for (; ch >= 0 && ch != '&'; ch = is.read())
        readChar(converter, is, ch);
      
      String value = converter.getConvertedString();

      /* Could show passwords
      if (log.isLoggable(Level.FINE))
        log.fine("post: " + key + "=" + value);
      */
      
      String []oldValue = table.get(key);
      
      if (key == null || key.equals("")) {
      }
      else if (oldValue == null)
        table.put(key, new String[] { value });
      else {
        String []newValue = new String[oldValue.length + 1];
        System.arraycopy(oldValue, 0, newValue, 0, oldValue.length);
        newValue[oldValue.length] = value;
        table.put(key, newValue);
      }
    }
  }

  /**
   * Scans the next character from the input stream, adding it to the
   * converter.
   *
   * @param converter the byte-to-character converter
   * @param is the form's input stream
   * @param ch the next character
   */
  private static void readChar(ByteToChar converter, InputStream is, int ch)
    throws IOException
  {
    if (ch == '+')
      converter.addByte(' ');
    else if (ch == '%') {
      int ch1 = is.read();

      if (ch1 == 'u') {
        ch1 = is.read();
        int ch2 = is.read();
        int ch3 = is.read();
        int ch4 = is.read();

        converter.addChar((char) ((toHex(ch1) << 12) +
                                  (toHex(ch2) << 8) + 
                                  (toHex(ch3) << 4) + 
                                  (toHex(ch4))));
      }
      else {
        int ch2 = is.read();

        converter.addByte(((toHex(ch1) << 4) + toHex(ch2)));
      }
    }
    else
      converter.addByte(ch);
  }

  /**
   * Converts a hex character to a byte
   */
  private static int toHex(int ch)
  {
    if (ch >= '0' && ch <= '9')
      return ch - '0';
    else if (ch >= 'a' && ch <= 'f')
      return ch - 'a' + 10;
    else if (ch >= 'A' && ch <= 'F')
      return ch - 'A' + 10;
    else
      return -1;
  }
}
