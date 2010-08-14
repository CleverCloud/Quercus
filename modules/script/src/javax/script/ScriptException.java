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

package javax.script;

/**
 * Represents a script exception.
 */
public class ScriptException extends Exception {
  protected String fileName;
  protected int lineNumber = -1;
  protected int columnNumber = -1;

  /**
   * Creates a ScriptException.
   */
  public ScriptException(String message)
  {
    super(message);
  }

  /**
   * Creates a ScriptException.
   */
  public ScriptException(Exception e)
  {
    super(e);
  }

  /**
   * Creates a ScriptException.
   */
  public ScriptException(String message, String fileName, int lineNumber)
  {
    super(message);

    this.fileName = fileName;
    this.lineNumber = lineNumber;
  }

  /**
   * Creates a ScriptException.
   */
  public ScriptException(String message, String fileName,
                         int lineNumber, int columnNumber)
  {
    super(message);

    this.fileName = fileName;
    this.lineNumber = lineNumber;
    this.columnNumber = columnNumber;
  }

  /**
   * Returns the file name.
   */
  public String getFileName()
  {
    return this.fileName;
  }

  /**
   * Returns the line number.
   *
   * @return the line number or -1 if none is available.
   */
  public int getLineNumber()
  {
    return this.lineNumber;
  }
  
  /**
   * Returns the column number.
   *
   * @return the column number or -1 if none is available.
   */
  public int getColumnNumber()
  {
    return this.columnNumber;
  }
}

