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

package com.caucho.es.parser;

import com.caucho.es.ESBase;
import com.caucho.es.ESBoolean;
import com.caucho.es.ESNumber;
import com.caucho.es.ESString;

import java.io.IOException;

/**
 * Represents a java literal.
 */
class LiteralExpr extends Expr {
  private ESBase value;

  LiteralExpr(Block block, ESBase value)
  {
    super(block);
    
    if (value == null)
      value = ESBase.esNull;
    
    this.value = value;
    
    if (value instanceof ESNumber) {
      double dv = 0;

      try {
        dv = value.toNum();
      } catch (Throwable e) {
      }
      
      if ((double) ((int) dv) == dv)
        type = TYPE_INTEGER;
      else if ((double) ((long) dv) == dv)
        type = TYPE_LONG;
      else
        type = TYPE_NUMBER;
    }
    else if (value instanceof ESBoolean)
      type = TYPE_BOOLEAN;
    else if (value instanceof ESString)
      type = TYPE_STRING;
    else
      type = TYPE_ES;

    if (this.value == null)
      throw new RuntimeException();
  }

  /**
   * Return the literal value.
   */
  ESBase getLiteral()
  {
    return value;
  }

  boolean isSimple()
  {
    return true;
  }

  void printInt32Impl() throws IOException
  {
    try {
      cl.print(value.toInt32());
    } catch (Throwable e) {
    }
  }

  void printNumImpl() throws IOException
  {
    try {
      double v = value.toNum();

      if (Double.isInfinite(v))
        cl.print("Double.POSITIVE_INFINITY");
      else if (Double.isInfinite(-v))
        cl.print("Double.NEGATIVE_INFINITY");
      else if (Double.isNaN(v))
        cl.print("Double.NaN");
      else if ((long) v == v)
        cl.print("(" + (long) v + "L)");
      else
        cl.print("(" + v + "D)");
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  void printBooleanImpl() throws IOException
  {
    cl.print(value.toBoolean());
  }

  /**
   * Prints the literal as a string.
   */
  void printStringImpl() throws IOException
  {
    try {
      cl.print("\"");
      String s = value.toStr().toString(); 
      for (int i = 0; i < s.length(); i++) {
        char ch = s.charAt(i);
        switch (ch) {
        case '"':
          cl.print("\\\"");
          break;
        case '\\':
          cl.print("\\\\");
          break;
        case '\n':
          cl.print("\\n");
          break;
        case '\r':
          cl.print("\\r");
          break;
        default:
          cl.print(ch);
          break;
        }
      }
      cl.print("\"");
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  void printStr() throws IOException
  {
    try {
      printLiteral(value.toStr());
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  void print() throws IOException
  {
    printLiteral(value);
  }

  void printImpl() throws IOException
  {
    printLiteral(value);
  }

  public String toString()
  {
    return "[LiteralExpr " + value.toString() + "]";
  }
}
