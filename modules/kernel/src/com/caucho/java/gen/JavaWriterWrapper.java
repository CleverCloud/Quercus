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

package com.caucho.java.gen;

import com.caucho.bytecode.JClass;
import com.caucho.java.JavaWriter;
import com.caucho.java.LineMap;
import com.caucho.vfs.WriteStream;

import java.io.IOException;

/**
 * Wrapper
 */
public class JavaWriterWrapper extends JavaWriter {
  // Write stream for generating the code
  private JavaWriter _writer;

  public JavaWriterWrapper(JavaWriter writer)
  {
    super(null);
    
    _writer = writer;
  }

  /**
   * Returns the underlying stream.
   */
  public WriteStream getWriteStream()
  {
    return _writer.getWriteStream();
  }

  /**
   * Returns the destination line.
   */
  public int getDestLine()
  {
    return _writer.getDestLine();
  }

  /**
   * Sets the line map
   */
  public void setLineMap(LineMap lineMap)
  {
    _writer.setLineMap(lineMap);
  }

  /**
   * Gets the line map
   */
  public LineMap getLineMap()
  {
    return _writer.getLineMap();
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
    _writer.setLocation(filename, line);
  }

  /**
   * Generates a unique id.
   */
  public int generateId()
  {
    return _writer.generateId();
  }

  /**
   * Prints a Java escaped string
   */
  public void printJavaString(String s)
    throws IOException
  {
    _writer.printJavaString(s);
  }

  /**
   * Prints a Java escaped string surrounded by ", or null if the string is null.
   */
  public void printQuotedJavaString(String s)
    throws IOException
  {
    if (s == null)
      _writer.print("null");
    else {
      _writer.print("\"");
      _writer.printJavaString(s);
      _writer.print("\"");
    }
  }

  /**
   * Prints a Java escaped string
   */
  public void printJavaChar(char ch)
    throws IOException
  {
    _writer.printJavaChar(ch);
  }

  /**
   * Pushes an indentation depth.
   */
  public void pushDepth()
    throws IOException
  {
    _writer.pushDepth();
  }

  /**
   * Pops an indentation depth.
   */
  public void popDepth()
    throws IOException
  {
    _writer.popDepth();
  }

  /**
   * Prints a string
   */
  public void print(String s)
    throws IOException
  {
    _writer.print(s);
  }

  /**
   * Prints a character.
   */
  public void print(char ch)
    throws IOException
  {
    _writer.print(ch);
  }

  /**
   * Prints a boolean.
   */
  public void print(boolean b)
    throws IOException
  {
    _writer.print(b);
  }

  /**
   * Prints an integer.
   */
  public void print(int i)
    throws IOException
  {
    _writer.print(i);
  }

  /**
   * Prints an long
   */
  public void print(long l)
    throws IOException
  {
    _writer.print(l);
  }

  /**
   * Prints an object.
   */
  public void print(Object o)
    throws IOException
  {
    _writer.print(o);
  }

  /**
   * Prints a string with a new line
   */
  public void println(String s)
    throws IOException
  {
    _writer.println(s);
  }

  /**
   * Prints a boolean with a new line
   */
  public void println(boolean v)
    throws IOException
  {
    _writer.println(v);
  }

  /**
   * Prints a character.
   */
  public void println(char ch)
    throws IOException
  {
    _writer.println(ch);
  }

  /**
   * Prints an integer with a new line
   */
  public void println(int v)
    throws IOException
  {
    _writer.println(v);
  }

  /**
   * Prints an long with a new line
   */
  public void println(long v)
    throws IOException
  {
    _writer.println(v);
  }

  /**
   * Prints an object with a new line
   */
  public void println(Object v)
    throws IOException
  {
    _writer.println(v);
  }

  /**
   * Prints a newline
   */
  public void println()
    throws IOException
  {
    _writer.println();
  }
  
  /**
   * Prints the Java represention of the class
   */
  public void printClass(Class cl)
    throws IOException
  {
    _writer.printClass(cl);
  }
  
  /**
   * Converts a java primitive type to a Java object.
   *
   * @param value the java expression to be converted
   * @param javaType the type of the converted expression.
   */
  public void printJavaTypeToObject(String value, Class javaType)
    throws IOException
  {
    _writer.printJavaTypeToObject(value, javaType);
  }
  
  /**
   * Converts a java primitive type to a Java object.
   *
   * @param value the java expression to be converted
   * @param javaType the type of the converted expression.
   */
  public void printJavaTypeToObject(String value, JClass javaType)
    throws IOException
  {
    _writer.printJavaTypeToObject(value, javaType);
  }

  /**
   * Prints the indentation at the beginning of a line.
   */
  public void printIndent()
    throws IOException
  {
    _writer.printIndent();
  }

  /**
   * Returns the error message with proper line number.
   */
  public String errorMessage(String message)
  {
    return _writer.errorMessage(message);
  }
}
