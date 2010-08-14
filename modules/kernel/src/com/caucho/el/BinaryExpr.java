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

package com.caucho.el;

import com.caucho.vfs.WriteStream;

import javax.el.ELContext;
import javax.el.ELException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Represents a binary numeric operation
 */
public class BinaryExpr extends Expr {
  private int _op;
  private Expr _left;
  private Expr _right;

  /**
   * Creates the binary expression.
   *
   * @param op the binary operation's lexical code
   * @param left the left subexpression
   * @param right the right subexpression
   */
  private BinaryExpr(int op, Expr left, Expr right)
  {
    _op = op;
    _left = left;
    _right = right;
  }

  public static Expr create(int op, Expr left, Expr right)
  {
    switch (op) {
    case ADD:
      return new AddExpr(left, right);
    case SUB:
      return new SubExpr(left, right);
    case MUL:
      return new MulExpr(left, right);
    case DIV:
      return new DivExpr(left, right);
    case MOD:
      return new ModExpr(left, right);

    default:
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Returns true if this is a constant expression.
   */
  @Override
  public boolean isConstant()
  {
    return _left.isConstant() && _right.isConstant();
  }
  
  /**
   * Evaluate the expression as an object.
   *
   * @param env the variable environment
   *
   * @return the result as an object
   */
  @Override
  public Object getValue(ELContext env)
    throws ELException
  {
    Object aObj = _left.getValue(env);
    Object bObj = _right.getValue(env);

    if (aObj instanceof BigDecimal || bObj instanceof BigDecimal) {
      BigDecimal a = toBigDecimal(aObj, env);
      BigDecimal b = toBigDecimal(bObj, env);
      
      switch (_op) {
      case ADD:
        return a.add(b);
      case SUB:
        return a.subtract(b);
      case MUL:
        return a.multiply(b);
      case DIV:
        return a.divide(b, BigDecimal.ROUND_HALF_UP);
      case MOD:
        {
          double da = toDouble(aObj, env);
          double db = toDouble(bObj, env);

          return new Double(da % db);
        }
      default:
        throw new IllegalStateException();
      }
    }
    else if (aObj instanceof BigInteger || bObj instanceof BigInteger) {
      BigInteger a = toBigInteger(aObj, env);
      BigInteger b = toBigInteger(bObj, env);
      
      switch (_op) {
      case ADD:
        return a.add(b);
      case SUB:
        return a.subtract(b);
      case MUL:
        return a.multiply(b);
      case DIV:
        {
          BigDecimal da = toBigDecimal(aObj, env);
          BigDecimal db = toBigDecimal(bObj, env);

          return da.divide(db, BigDecimal.ROUND_HALF_UP);
        }
      case MOD:
        {
          if (aObj instanceof Float ||
              aObj instanceof Double ||
              bObj instanceof Float ||
              bObj instanceof Double) {
            double da = toDouble(aObj, env);
            double db = toDouble(bObj, env);

            return new Double(da % db);
          }
          else
            return a.remainder(b);
        }
      default:
        throw new IllegalStateException();
      }
    }
      
    else if (isDouble(aObj) || isDouble(bObj)) {
      double a = toDouble(aObj, env);
      double b = toDouble(bObj, env);
      double dValue = 0;

      switch (_op) {
      case ADD:
        dValue =  a + b;
        break;
      case SUB:
        dValue = a - b;
        break;
      case MUL:
        dValue = a * b;
        break;
      case DIV:
        dValue = a / b;
        break;
      case MOD:
        dValue = a % b;
        break;
      }
      
      return Double.isNaN(dValue) ? new Double(0) : new Double(dValue);
    }
    
    if (aObj == null && bObj == null)
      return new Integer(0);

    if (bObj instanceof Double || bObj instanceof Float) {
      double a = toDouble(aObj, env);
      double b = ((Number) bObj).doubleValue();
      double dValue = 0;

      switch (_op) {
      case ADD:
        dValue = a + b;
        break;
      case SUB:
        dValue = a - b;
        break;
      case MUL:
        dValue = a * b;
        break;
      case DIV:
        dValue = a / b;
        break;
        
      case MOD:
        dValue = a % b;
        break;
      }
      
      return Double.isNaN(dValue) ? new Double(0) : new Double(dValue);
    }
    else if (aObj instanceof Number) {
      long a = ((Number) aObj).longValue();
      long b = toLong(bObj, env);

      switch (_op) {
      case ADD: return new Long(a + b);
      case SUB: return new Long(a - b);
      case MUL: return new Long(a * b);
      case DIV:
        double dValue = (double) a / (double) b;
        
        return Double.isNaN(dValue) ? new Double(0) : new Double(dValue);
          
      case MOD: return new Long(a % b);
      }
    }
    else if (bObj instanceof Number) {
      long a = toLong(aObj, env);
      long b = ((Number) bObj).longValue();

      switch (_op) {
      case ADD: return new Long(a + b);
      case SUB: return new Long(a - b);
      case MUL: return new Long(a * b);
      case DIV:
        double dValue = (double) a / (double) b;
        
        return Double.isNaN(dValue) ? new Double(0) : new Double(dValue);
          
      case MOD: return new Long(a % b);
      }
    }

    if (isDoubleString(aObj) || isDoubleString(bObj)) {
      double a = toDouble(aObj, env);
      double b = toDouble(bObj, env);
    
      switch (_op) {
      case ADD: return new Double(a + b);
      case SUB: return new Double(a - b);
      case MUL: return new Double(a * b);
      case DIV:
        double dValue = (double) a / (double) b;
        
        return Double.isNaN(dValue) ? new Double(0) : new Double(dValue);
          
      case MOD: return new Double(a % b);
      }
    }
    else {
      long a = toLong(aObj, env);
      long b = toLong(bObj, env);

      switch (_op) {
      case ADD: return new Long(a + b);
      case SUB: return new Long(a - b);
      case MUL: return new Long(a * b);
      case DIV:
        double dValue = (double) a / (double) b;
        
        return Double.isNaN(dValue) ? new Double(0) : new Double(dValue);
        
      case MOD: return new Long(a % b);
      }
    }

    return null;
  }

  /**
   * Evaluate the expression as a long
   *
   * @param env the variable environment
   *
   * @return the result as an long
   */
  @Override
  public long evalLong(ELContext env)
    throws ELException
  {
    long a = _left.evalLong(env);
    long b = _right.evalLong(env);

    switch (_op) {
    case ADD: return a + b;
    case SUB: return a - b;
    case MUL: return a * b;
    case DIV: return (long) ((double) a / (double) b);
    case MOD: return a % b;
    }

    ELException e = new ELException(L.l("error evaluating add {0} and {1}",
                                        String.valueOf(a),
                                        String.valueOf(b)));

    error(e, env);

    return 0;
  }
  
  /**
   * Evaluate the expression as a double
   *
   * @param env the variable environment
   *
   * @return the result as an double
   */
  @Override
  public double evalDouble(ELContext env)
    throws ELException
  {
    double a = _left.evalDouble(env);
    double b = _right.evalDouble(env);

    switch (_op) {
    case ADD: return a + b;
    case SUB: return a - b;
    case MUL: return a * b;
    case DIV: return a / b;
    case MOD: return a % b;
    }

    ELException e = new ELException(L.l("error evaluating add {0} and {1}",
                                        String.valueOf(a),
                                        String.valueOf(b)));

    error(e, env);

    return 0;
  }

  /**
   * Prints the Java code to recreate an LongLiteral.
   *
   * @param os the output stream to the *.java file
   */
  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.el.BinaryExpr(");
    os.print(_op + ", ");
    _left.printCreate(os);
    os.print(", ");
    _right.printCreate(os);
    os.print(")");
  }

  /**
   * Returns true for equal strings.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof BinaryExpr))
      return false;

    BinaryExpr expr = (BinaryExpr) o;

    return (_op == expr._op &&
            _left.equals(expr._left) &&
            _right.equals(expr._right));
  }
  
  /**
   * Returns a readable representation of the expr.
   */
  public String toString()
  {
    String op;

    switch (_op) {
    case ADD:
      op = " + ";
      break;
    case SUB:
      op = " - ";
      break;
    case MUL:
      op = " * ";
      break;
    case DIV:
      op = " / ";
      break;
    case MOD:
      op = " % ";
      break;
    default:
      op = " unknown(" + _op + ") ";
      break;
    }
        
    return "(" + _left + op + _right + ")";
  }
}
