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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jsp.java;

import com.caucho.java.JavaWriter;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.WriteStream;

import java.io.IOException;

/**
 * Writing class for generated Java code.
 */
public class JspJavaWriter extends JavaWriter {
  // The JSP generator
  private JavaJspGenerator _gen;

  private String _filename;
  private int _line = -1;
  
  // Current text node.
  private CharBuffer _cb = CharBuffer.allocate();

  public JspJavaWriter(WriteStream os, JavaJspGenerator gen)
  {
    super(os);

    _gen = gen;
  }

  /**
   * Adds text to the current buffer.
   */
  public void addText(String text)
    throws IOException
  {
    if (_filename != null && _cb.length() == 0) {
      super.setLocation(_filename, _line);
    }
    
    _cb.append(text);
  }

  /**
   * Sets the source filename and line.
   *
   * @param filename the filename of the source file.
   * @param line the line of the source file.
   */
  public void setLocation(String filename, int line)
    throws IOException
  {
    _filename = filename;
    _line = line;
  }

  /**
   * Flushes text.
   */

  /**
   * Generates the code for the static text
   *
   * @param out the output writer for the generated java.
   */
  protected void flushText()
    throws IOException
  {
    String filename = _filename;
    int line = _line;
    _filename = null;
    
    if (_cb.length() > 0) {
      int length = _cb.length();
      _cb.clear();
      generateText(_cb.getBuffer(), 0, length);
    }

    if (filename != null)
      super.setLocation(filename, line);
  }

  /**
   * Generates text from a string.
   *
   * @param out the output writer for the generated java.
   * @param string the text to generate.
   * @param offset the offset into the text.
   * @param length the length of the text.
   */
  private void generateText(char []text, int offset, int length)
    throws IOException
  {
    if (length > 32000) {
      generateText(text, offset, 16 * 1024);
      generateText(text, offset + 16 * 1024, length - 16 * 1024);
      return;
    }

    if (length == 1) {
      int ch = text[offset];
      
      print("out.write('");
      switch (ch) {
      case '\\':
        print("\\\\");
        break;
      case '\'':
        print("\\'");
        break;
      case '\n':
        print("\\n");
        break;
      case '\r':
        print("\\r");
        break;
      default:
        print((char) ch);
        break;
      }

      println("');");
    }
    else {
      int index = _gen.addString(new String(text, offset, length));
    
      print("out.write(_jsp_string" + index + ", 0, ");
      println("_jsp_string" + index + ".length);");
    }
  }

  /**
   * Prints a Java escaped string
   */
  public void printJavaString(String s)
    throws IOException
  {
    flushText();

    super.printJavaString(s);
  }

  /**
   * Pushes an indentation depth.
   */
  public void pushDepth()
    throws IOException
  {
    flushText();

    super.pushDepth();
  }

  /**
   * Pops an indentation depth.
   */
  public void popDepth()
    throws IOException
  {
    flushText();

    super.popDepth();
  }

  /**
   * Prints a string
   */
  public void print(String s)
    throws IOException
  {
    flushText();

    super.print(s);
  }

  /**
   * Prints a character.
   */
  public void print(char ch)
    throws IOException
  {
    flushText();

    super.print(ch);
  }

  /**
   * Prints a boolean.
   */
  public void print(boolean b)
    throws IOException
  {
    flushText();

    super.print(b);
  }

  /**
   * Prints an integer.
   */
  public void print(int i)
    throws IOException
  {
    flushText();

    super.print(i);
  }

  /**
   * Prints an long
   */
  public void print(long l)
    throws IOException
  {
    flushText();

    super.print(l);
  }

  /**
   * Prints an object.
   */
  public void print(Object o)
    throws IOException
  {
    flushText();

    super.print(o);
  }

  /**
   * Prints a string with a new line
   */
  public void println(String s)
    throws IOException
  {
    flushText();

    super.println(s);
  }

  /**
   * Prints a boolean with a new line
   */
  public void println(boolean v)
    throws IOException
  {
    flushText();

    super.println(v);
  }

  /**
   * Prints a character.
   */
  public void println(char ch)
    throws IOException
  {
    flushText();

    super.println(ch);
  }

  /**
   * Prints an integer with a new line
   */
  public void println(int v)
    throws IOException
  {
    flushText();

    super.println(v);
  }

  /**
   * Prints an long with a new line
   */
  public void println(long v)
    throws IOException
  {
    flushText();

    super.println(v);
  }

  /**
   * Prints an object with a new line
   */
  public void println(Object v)
    throws IOException
  {
    flushText();

    super.println(v);
  }

  /**
   * Prints a newline
   */
  public void println()
    throws IOException
  {
    flushText();

    super.println();
  }
}
