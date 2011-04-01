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

import com.caucho.config.types.Period;
import com.caucho.util.BeanUtil;
import com.caucho.util.IntMap;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.MethodInfo;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.el.ValueExpression;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.JspException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract implementation class for an expression.
 */
public abstract class Expr extends ValueExpression {
  protected static final Logger log
    = Logger.getLogger(Expr.class.getName());
  protected static final L10N L = new L10N(Expr.class);

  private static final BigDecimal BIG_DECIMAL_ZERO = new BigDecimal("0");

  private static final BigInteger BIG_INTEGER_ZERO = new BigInteger("0");
  
  private static final HashMap<Class<?>,CoerceType> _coerceMap
    = new HashMap<Class<?>,CoerceType>();

  // lexeme codes
  final static int ADD = 1;
  final static int SUB = ADD + 1;
  final static int MUL = SUB + 1;
  final static int DIV = MUL + 1;
  final static int MOD = DIV + 1;
  final static int EQ = MOD + 1;
  final static int NE = EQ + 1;
  final static int LT = NE + 1;
  final static int LE = LT + 1;
  final static int GT = LE + 1;
  final static int GE = GT + 1;
  final static int AND = GE + 1;
  final static int OR = AND + 1;
  
  final static int NOT = OR + 1;
  final static int MINUS = NOT + 1;
  final static int EMPTY = MINUS + 1;

  final static int OBJECT = 0;
  final static int BOOLEAN = OBJECT + 1;
  final static int BYTE = BOOLEAN + 1;
  final static int SHORT = BYTE + 1;
  final static int INT = SHORT + 1;
  final static int LONG = INT + 1;
  final static int FLOAT = LONG + 1;
  final static int DOUBLE = FLOAT + 1;
  
  final static int BOOLEAN_OBJ = DOUBLE + 1;
  final static int BYTE_OBJ = BOOLEAN_OBJ + 1;
  final static int SHORT_OBJ = BYTE_OBJ + 1;
  final static int INT_OBJ = SHORT_OBJ + 1;
  final static int LONG_OBJ = INT_OBJ + 1;
  final static int FLOAT_OBJ = LONG_OBJ + 1;
  final static int DOUBLE_OBJ = FLOAT_OBJ + 1;
  
  final static int STRING = DOUBLE_OBJ + 1;

  final static IntMap _typeMap = new IntMap();

  /**
   * Returns true if the expression is constant.
   */
  public boolean isConstant()
  {
    return false;
  }

  /**
   * Returns true if the expression is read-only.
   */
  public boolean isReadOnly(ELContext env)
  {
    return true;
  }

  /**
   * Returns true if the expression is literal text
   */
  public boolean isLiteralText()
  {
    return false;
  }

  /**
   * Creates a field reference using this expression as the base object.
   *
   * @param field the expression for the field.
   */
  public Expr createField(Expr field)
  {
    return new ArrayResolverExpr(this, field);
  }

  /**
   * Creates a field reference using this expression as the base object.
   *
   * @param field the string reference for the field.
   */
  public Expr createField(String field)
  {
    return createField(new StringLiteral(field));
  }

  /**
   * Creates a method call using this as the <code>obj.method</code>
   * expression
   *
   * @param args the arguments for the method
   */
  public Expr createMethod(Expr []args)
  {
    return new FunctionExpr(this, args);
  }

  /**
   * Evaluates the expression, returning an object.
   *
   * @param env the variable environment
   *
   * @return the value of the expression as an object
   */
  abstract public Object getValue(ELContext env)
    throws ELException;

  /**
   * Evaluates the expression, returning an object.
   *
   * @param env the variable environment
   *
   * @return the value of the expression as an object
   */
  public MethodInfo getMethodInfo(ELContext env,
                                  Class<?> returnType,
                                  Class<?> []argTypes)
    throws ELException
  {
    throw new ELException(L.l("'{0}' is an illegal method expression." + getClass(),
                              toString()));
  }

  /**
   * Evaluates the expression, returning an object.
   *
   * @param env the variable environment
   *
   * @return the value of the expression as an object
   */
  public Object invoke(ELContext env, Class<?> []argTypes, Object []args)
    throws ELException
  {
    throw new ELException(L.l("'{0}' is an illegal method expression." + getClass(),
                              toString()));
  }

  /**
   * Evaluates the expression, returning an object.
   *
   * @param env the variable environment
   *
   * @return the value of the expression as an object
   */
  public final Object evalObject(ELContext env)
    throws ELException
  {
    return getValue(env);
  }

  /**
   * Evaluate the expression, knowing the value should be a boolean.
   *
   * @param env the variable environment
   *
   * @return the value of the expression as a boolean
   */
  public boolean evalBoolean(ELContext env)
    throws ELException
  {
    return toBoolean(getValue(env), env);
  }

  /**
   * Evaluate the expression, knowing the value should be a double.
   *
   * @param env the variable environment
   *
   * @return the value of the expression as a double
   */
  public double evalDouble(ELContext env)
    throws ELException
  {
    return toDouble(getValue(env), env);
  }

  /**
   * Evaluate the expression, knowing the value should be a long
   *
   * @param env the variable environment
   *
   * @return the value of the expression as a double
   */
  public long evalLong(ELContext env)
    throws ELException
  {
    return toLong(getValue(env), env);
  }

  /**
   * Evaluate the expression, knowing the value should be a string
   *
   * @param env the variable environment
   *
   * @return the value of the expression as a string
   */
  public String evalString(ELContext env)
    throws ELException
  {
    return toString(getValue(env), env);
  }

  /**
   * Evaluate the expression, knowing the value should be a string
   *
   * @param env the variable environment
   *
   * @return the value of the expression as a string
   */
  public String evalStringWithNull(ELContext env)
    throws ELException
  {
    return toStringWithNull(getValue(env), env);
  }

  /**
   * Evaluate the expression, knowing the value should be a string
   *
   * @param env the variable environment
   *
   * @return the value of the expression as a string
   */
  public char evalCharacter(ELContext env)
    throws ELException
  {
    return toCharacter(getValue(env), env);
  }

  /**
   * Evaluate the expression, knowing the value should be a period
   *
   * @param env the variable environment
   *
   * @return the value of the expression as a period
   */
  public long evalPeriod(ELContext env)
    throws ELException
  {
    try {
      Object obj = getValue(env);

      if (obj instanceof Number)
        return 1000L * ((Number) obj).longValue();
      else
        return Period.toPeriod(toString(obj, env));
    } catch (Exception e) {
      throw new ELException(e.getMessage());
    }
                        
  }

  /**
   * Evaluate the expression, knowing the value should be a BigInteger.
   *
   * @param env the variable environment
   *
   * @return the value of the expression as a BigInteger
   */
  public BigInteger evalBigInteger(ELContext env)
    throws ELException
  {
    return toBigInteger(getValue(env), env);
  }

  /**
   * Evaluate the expression, knowing the value should be a BigDecimal.
   *
   * @param env the variable environment
   *
   * @return the value of the expression as a BigDecimal
   */
  public BigDecimal evalBigDecimal(ELContext env)
    throws ELException
  {
    return toBigDecimal(getValue(env), env);
  }

  /**
   * Evaluates the expression, setting an object.
   *
   * @param env the variable environment
   *
   * @return the value of the expression as an object
   */
  @Override
  public void setValue(ELContext env, Object value)
    throws PropertyNotFoundException,
           PropertyNotWritableException,
           ELException
  {
    throw new PropertyNotWritableException(getClass().getName() + ": " + toString());
  }

  /**
   * Evaluates directly to the output.  The method returns true
   * if the default value should be printed instead.
   *
   * @param out the output writer
   * @param env the variable environment
   * @param escapeXml if true, escape reserved XML
   *
   * @return true if the object is null, otherwise false
   */
  public boolean print(WriteStream out,
                       ELContext env,
                       boolean escapeXml)
    throws IOException, ELException
  {
    Object obj = getValue(env);

    if (obj == null)
      return true;
    else if (escapeXml) {
      toStreamEscaped(out, obj);
      return false;
    }
    else {
      toStream(out, obj);
      return false;
    }
  }

  /**
   * Evaluates directly to the output.  The method returns true
   * if the default value should be printed instead.
   *
   * @param out the output writer
   * @param env the variable environment
   * @param escapeXml if true, escape reserved XML
   *
   * @return true if the object is null, otherwise false
   */
  public boolean print(JspWriter out,
                       ELContext env,
                       boolean escapeXml)
    throws IOException, ELException
  {
    //try {
    
    Object obj = getValue(env);


    if (obj == null)
      return true;
    else if (escapeXml) {
      toStreamEscaped(out, obj);
      return false;
    }
    else {
      toStream(out, obj);
      return false;
    }
    
      /*
    } catch (ELException e) {
      // jsp/3253
      
      log.log(Level.WARNING, e.toString(), e);
      return false;
    }
      */
  }

  /**
   * Generates the code to regenerate the expression.
   *
   * @param os the stream to the *.java page
   */
  public void printCreate(WriteStream os)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  //
  // EL methods
  //

  @Override
  public String getExpressionString()
  {
    return toString();
  }

  @Override
  public Class<?> getExpectedType()
  {
    return Object.class;
  }

  @Override
  public Class<?> getType(ELContext context)
    throws PropertyNotFoundException,
           ELException
  {
    Object value = getValue(context);

    if (value == null)
      return null;
    else
      return value.getClass();
  }

  //
  // Static convenience methods
  //

  /**
   * Returns true for a double or double-equivalent.
   */
  public static boolean isDouble(Object o)
  {
    if (o instanceof Double)
      return true;
    else if (o instanceof Float)
      return true;
    else if (! (o instanceof String))
      return false;
    else {
      String s = (String) o;
      int len = s.length();

      for (int i = 0; i < len; i++) {
        char ch = s.charAt(i);

        if (ch == '.' || ch == 'e' || ch == 'E')
          return true;
      }

      return false;
    }
  }

  /**
   * Converts some unknown value to a string.
   *
   * @param value the value to be converted.
   *
   * @return the string-converted value.
   */
  public static String toStringWithNull(Object value, ELContext env)
  {
    if (value == null || value instanceof String)
      return (String) value;
    else
      return value.toString();
  }

  /**
   * Converts some unknown value to a string.
   *
   * @param value the value to be converted.
   *
   * @return the string-converted value.
   */
  public static String toString(Object value, ELContext env)
  {
    if (value == null)
      return "";
    else if (value instanceof String)
      return (String) value;
    else
      return value.toString();
  }

  /**
   * Converts some unknown value to a string.
   *
   * @param value the value to be converted.
   *
   * @return the string-converted value.
   */
  public static String toString(long value, ELContext env)
  {
    return String.valueOf(value);
  }

  /**
   * Converts some unknown value to a string.
   *
   * @param value the value to be converted.
   *
   * @return the string-converted value.
   */
  public static String toString(double value, ELContext env)
  {
    return String.valueOf(value);
  }

  /**
   * Converts some unknown value to a string.
   *
   * @param value the value to be converted.
   *
   * @return the string-converted value.
   */
  public static String toString(boolean value, ELContext env)
  {
    return String.valueOf(value);
  }

  /**
   * Converts some unknown value to a string.
   *
   * @param value the value to be converted.
   *
   * @return the string-converted value.
   */
  public static String toString(char value, ELContext env)
  {
    return String.valueOf(value);
  }

  /**
   * Converts some unknown value to a string.
   *
   * @param value the value to be converted.
   *
   * @return the string-converted value.
   */
  public static char toCharacter(Object value, ELContext env)
    throws ELException
  {
    if (value == null)
      return (char) 0;
    else if (value instanceof Character) {
      return ((Character) value).charValue();
    }
    else if (value instanceof String) {
      String s = (String) value;

      if (s == null || s.length() == 0)
        return (char) 0;
      else
        return s.charAt(0);
    }
    else if (value instanceof Number) {
      Number number = (Number) value;

      return (char) number.intValue();
    }
    else if (value instanceof Boolean) {
      ELException e = new ELException(L.l("can't convert {0} to character.",
                                          value.getClass().getName()));

      throw e;
      /*
      error(e, env);

      return (char) 0;
      */
    }
    else
      return (char) toLong(value, env);
  }

  /**
   * Converts some unknown value to a boolean.
   *
   * @param value the value to be converted.
   *
   * @return the boolean-converted value.
   */
  public static boolean toBoolean(Object value, ELContext env)
    throws ELException
  {
    if (value == null || value.equals(""))
      return false;
    else if (value instanceof Boolean)
      return ((Boolean) value).booleanValue();
    else if (value instanceof String)
      return value.equals("true") || value.equals("yes");
    else {
      ELException e = new ELException(L.l("can't convert {0} to boolean.",
                                          value.getClass().getName()));

      // jsp/18s1
      throw e;
      /*
      error(e, env);

      return false;
      */
    }
  }

  /**
   * Converts some unknown value to a double.
   *
   * @param value the value to be converted.
   *
   * @return the double-converted value.
   */
  public static double toDouble(Object value, ELContext env)
    throws ELException
  {
    try {
      if (value == null)
        return 0;
      else if (value instanceof Number) {
        double dValue = ((Number) value).doubleValue();

        if (Double.isNaN(dValue))
          return 0;
        else
          return dValue;
      }
      else if (value.equals(""))
        return 0;
      else if (value instanceof String) {
        double dValue = Double.parseDouble((String) value);

        if (Double.isNaN(dValue))
          return 0;
        else
          return dValue;
      }
      else if (value instanceof Character) {
        // jsp/18s7
        return ((Character) value).charValue();
      }
      else {
        ELException e = new ELException(L.l("can't convert {0} to double.",
                                            value.getClass().getName()));

        // error(e, env);

        // return 0;

        throw e;
      }
    } catch (NumberFormatException e) {
      throw new ELException(L.l("can't convert '{0}' to double.", value));
    }
  }

  /**
   * Converts some unknown value to a big decimal
   *
   * @param value the value to be converted.
   *
   * @return the BigDecimal-converted value.
   */
  public static BigDecimal toBigDecimal(Object value, ELContext env)
    throws ELException
  {
    if (value == null)
      return BIG_DECIMAL_ZERO;
    else if  (value instanceof BigDecimal)
      return (BigDecimal) value;
    else if (value instanceof Number) {
      double dValue = ((Number) value).doubleValue();

      return new BigDecimal(dValue);
    }
    else if (value.equals(""))
      return BIG_DECIMAL_ZERO;
    else if (value instanceof String) {
      return new BigDecimal((String) value);
    }
    else if (value instanceof Character) {
      return new BigDecimal(((Character) value).charValue());
    }
    else {
      ELException e = new ELException(L.l("can't convert {0} to BigDecimal.",
                                          value.getClass().getName()));
      
      error(e, env);

      return BIG_DECIMAL_ZERO;
    }
  }

  /**
   * Converts some unknown value to a big integer
   *
   * @param value the value to be converted.
   *
   * @return the BigInteger-converted value.
   */
  public static BigInteger toBigInteger(Object value, ELContext env)
    throws ELException
  {
    if (value == null)
      return BIG_INTEGER_ZERO;
    else if  (value instanceof BigInteger)
      return (BigInteger) value;
    else if (value instanceof Number) {
      // return new BigInteger(value.toString());
      return new BigDecimal(value.toString()).toBigInteger();
    }
    else if (value.equals(""))
      return BIG_INTEGER_ZERO;
    else if (value instanceof String) {
      return new BigInteger((String) value);
    }
    else if (value instanceof Character) {
      return new BigInteger(String.valueOf((int) ((Character) value).charValue()));
    }
    else {
      ELException e = new ELException(L.l("can't convert {0} to BigInteger.",
                                          value.getClass().getName()));
      
      error(e, env);

      return BIG_INTEGER_ZERO;
    }
  }

  /**
   * Converts some unknown value to a long.
   *
   * @param value the value to be converted.
   *
   * @return the long-converted value.
   */
  public static long toLong(Object value, ELContext env)
    throws ELException
  {
    if (value == null)
      return 0;
    else if (value instanceof Number)
      return ((Number) value).longValue();
    else if (value.equals(""))
      return 0;
    else if (value instanceof String) {
      try {
	return Long.parseLong((String) value);
      } catch (Exception e) {
	throw new ELException(e);
      }
    }
    else if (value instanceof Character) {
      // jsp/18s6
      return ((Character) value).charValue();
    }
    else {
      ELException e = new ELException(L.l("can't convert {0} to long.",
                                          value.getClass().getName()));
      
      // error(e, env);

      // return 0;

      throw e;
    }
  }

  /**
   * Write to the stream.
   *
   * @param out the output stream
   * @param value the value to be written.
   *
   * @return true for null
   */
  public static boolean toStream(JspWriter out,
                                 Object value,
                                 boolean isEscaped)
    throws IOException
  {
    if (value == null)
      return true;
    
    if (isEscaped)
      toStreamEscaped(out, value);
    else
      toStream(out, value);

    return false;
  }

  /**
   * Write to the stream.
   *
   * @param out the output stream
   * @param value the value to be written.
   */
  public static void toStream(WriteStream out, Object value)
    throws IOException
  {
    if (value == null)
      return;
    else if (value instanceof String)
      out.print((String) value);
    else if (value instanceof Reader) {
      out.writeStream((Reader) value);
    }
    else
      out.print(value.toString());
  }

  /**
   * Write to the stream.
   *
   * @param out the output stream
   * @param value the value to be written.
   */
  public static void toStream(JspWriter out, Object value)
    throws IOException
  {
    if (value == null)
      return;
    else if (value instanceof String)
      out.print((String) value);
    else if (value instanceof Reader) {
      Reader reader = (Reader) value;
      int ch;

      while ((ch = reader.read()) > 0) {
        out.print((char) ch);
      }
    }
    else
      out.print(value.toString());
  }

  /**
   * Write to the *.java stream escaping Java reserved characters.
   *
   * @param os the output stream to the *.java code.
   *
   * @param string the value to be converted.
   */
  public static void printEscapedString(WriteStream os, String string)
    throws IOException
  {
    int length = string.length();
    for (int i = 0; i < length; i++) {
      char ch = string.charAt(i);

      switch (ch) {
      case '\\':
        os.print("\\\\");
        break;
      case '\n':
        os.print("\\n");
        break;
      case '\r':
        os.print("\\r");
        break;
      case '\"':
        os.print("\\\"");
        break;
      default:
        os.print((char) ch);
        break;
      }
    }
  }

  /**
   * Write to the stream.
   *
   * @param out the output stream
   * @param value the value to be written.
   */
  public static void toStreamEscaped(Writer out, Object value)
    throws IOException
  {
    if (value == null)
      return;
    else if (value instanceof Reader) {
      toStreamEscaped(out, (Reader) value);
      return;
    }

    String string = value.toString();
    int length = string.length();
    for (int i = 0; i < length; i++) {
      int ch = string.charAt(i);

      switch (ch) {
      case '<':
        out.write("&lt;");
        break;
      case '>':
        out.write("&gt;");
        break;
      case '&':
        out.write("&amp;");
        break;
      case '\'':
        out.write("&#039;");
        break;
      case '"':
        out.write("&#034;");
        break;
      default:
        out.write((char) ch);
        break;
      }
    }
  }

  /**
   * Write to the stream escaping XML reserved characters.
   *
   * @param out the output stream.
   * @param value the value to be converted.
   */
  public static void toStreamEscaped(WriteStream out, Object value)
    throws IOException
  {
    if (value == null)
      return;

    String string = value.toString();
    int length = string.length();
    for (int i = 0; i < length; i++) {
      int ch = string.charAt(i);

      switch (ch) {
      case '<':
        out.print("&lt;");
        break;
      case '>':
        out.print("&gt;");
        break;
      case '&':
        out.print("&amp;");
        break;
      case '\'':
        out.print("&#039;");
        break;
      case '"':
        out.print("&#034;");
        break;
      default:
        out.print((char) ch);
        break;
      }
    }
  }

  /**
   * Write to the stream escaping XML reserved characters.
   *
   * @param out the output stream.
   * @param in the value to be converted.
   */
  public static void toStreamEscaped(Writer out, Reader in)
    throws IOException
  {
    if (in == null)
      return;
    
    int ch;
    while ((ch = in.read()) >= 0) {
      switch (ch) {
      case '<':
        out.write("&lt;");
        break;
      case '>':
        out.write("&gt;");
        break;
      case '&':
        out.write("&amp;");
        break;
      case '\'':
        out.write("&#039;");
        break;
      case '"':
        out.write("&#034;");
        break;
      default:
        out.write((char) ch);
        break;
      }
    }
  }

  public static Object toEnum(Object obj, Class<? extends Enum> enumType)
  {
    if (obj == null)
      return null;

    Class objClass = obj.getClass();

    if (objClass.equals(enumType))
      return obj;

    if (obj.getClass().equals(String.class) && "".equals(obj))
      return null;

    try {
      return Enum.valueOf(enumType, obj.toString());
    } catch (IllegalArgumentException e) {
      throw new ELException(L.l("Unable convert '{0}' to '{1}'",
                                obj.toString(),
                                enumType.getName()));
    }
  }
  
  /**
   * Write to the *.java stream escaping Java reserved characters.
   *
   * @param os the output stream to the *.java code.
   *
   * @param is the value to be converted.
   */
  public static void printEscaped(WriteStream os, ReadStream is)
    throws IOException
  {
    int ch;
    
    while ((ch = is.readChar()) >= 0) {
      switch (ch) {
      case '\\':
        os.print("\\\\");
        break;
      case '\n':
        os.print("\\n");
        break;
      case '\r':
        os.print("\\r");
        break;
      case '\"':
        os.print("\\\"");
        break;
      default:
        os.print((char) ch);
        break;
      }
    }
  }

  // XXX: does this belong in JSP?
  public static void setProperty(Object target, String property, Object value)
    throws ELException, JspException
  {
    if (target instanceof Map) {
      Map<String,Object> map = (Map) target;
      
      if (value != null)
        map.put(property, value);
      else
        map.remove(property);
    }
    else if (target != null) {
      Method method = null;

      try {
        method = BeanUtil.getSetMethod(target.getClass(), property);
      } catch (Exception e) {
        throw new JspException(e);
      }

      if (method == null)
        throw new JspException(L.l("can't find property `{0}' in `{1}'",
                                  property, target.getClass()));

      Class<?> type = method.getParameterTypes()[0];

      try {
        int code = _typeMap.get(type);

        switch (code) {
        case BOOLEAN:
          value = toBoolean(value, null) ? Boolean.TRUE : Boolean.FALSE;
          break;

        case BYTE:
          value = new Byte((byte) toLong(value, null));
          break;

        case SHORT:
          value = new Short((short) toLong(value, null));
          break;

        case INT:
          value = new Integer((int) toLong(value, null));
          break;

        case LONG:
          value = new Long((long) toLong(value, null));
          break;

        case FLOAT:
          value = new Float((float) toDouble(value, null));
          break;

        case DOUBLE:
          value = new Double((double) toDouble(value, null));
          break;

        case BOOLEAN_OBJ:
          if (value != null)
            value = toBoolean(value, null) ? Boolean.TRUE : Boolean.FALSE;
          break;

        case BYTE_OBJ:
          if (value != null)
            value = new Byte((byte) toLong(value, null));
          break;

        case SHORT_OBJ:
          if (value != null)
            value = new Short((short) toLong(value, null));
          break;

        case INT_OBJ:
          if (value != null)
            value = new Integer((int) toLong(value, null));
          break;

        case LONG_OBJ:
          if (value != null)
            value = new Long((long) toLong(value, null));
          break;

        case FLOAT_OBJ:
          if (value != null)
            value = new Float((float) toDouble(value, null));
          break;

        case DOUBLE_OBJ:
          if (value != null)
            value = new Double((double) toDouble(value, null));
          break;

        case STRING:
          if (value != null)
            value = String.valueOf(value);
          break;

        default:
          break;
        }

        method.invoke(target, new Object[] { value });
      } catch (Exception e) {
        throw new JspException(e);
      }
    }
    else {
      // jsp/1c2l and JSTL TCK for exception type
      throw new javax.servlet.jsp.JspException(L.l("null is an invalid c:set target value."));
    }
  }

  protected static boolean isDoubleString(Object obj)
  {
    if (! (obj instanceof String))
      return false;

    String s = (String) obj;

    int len = s.length();
    for (int i = 0; i < len; i++) {
      char ch = s.charAt(i);

      if (ch == '.' || ch == 'e' || ch == 'E')
        return true;
    }

    return false;
  }
  
  public static Object coerceToType(Object obj, Class<?> targetType)
    throws ELException
  {
    if (targetType == null)
      return obj;

    CoerceType type = _coerceMap.get(targetType);

    if (type != null) {
      switch (type) {
      case BOOLEAN:
        return Expr.toBoolean(obj, null) ? Boolean.TRUE : Boolean.FALSE;
      case CHARACTER:
        return Expr.toCharacter(obj, null);
      case BYTE:
        return new Byte((byte) Expr.toLong(obj, null));
      case SHORT:
        return new Short((short) Expr.toLong(obj, null));
      case INTEGER:
        return new Integer((int) Expr.toLong(obj, null));
      case LONG:
        return new Long(Expr.toLong(obj, null));
      case FLOAT:
        return new Float((float) Expr.toDouble(obj, null));
      case DOUBLE:
        return new Double(Expr.toDouble(obj, null));
      case STRING:
        if (obj == null)
          return "";
        else
          return obj.toString();
      case BIG_DECIMAL:
        return Expr.toBigDecimal(obj, null);
      case BIG_INTEGER:
        return Expr.toBigInteger(obj, null);
      case OBJECT:
        return obj;
      case VOID:
        return null;
      }
    } else if (targetType.isEnum()) {
      return Expr.toEnum(obj, (Class<? extends Enum>) targetType);
    } else if (obj == null) {
      return null;
    } else if (targetType.isAssignableFrom(obj.getClass())) {
      return obj;
    }

    throw new ELException(L.l("unable to convert {0} to type {1}",
                              obj,
                              targetType));
  }

  /**
   * Returns an error object
   */
  public static Object error(Throwable e, ELContext env)
    throws ELException
  {
    if (env == null) {
      // jsp/1b56
      throw new ELException(e);
    }
    else if (env instanceof ExprEnv && ((ExprEnv) env).isIgnoreException()) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
    else if (e instanceof RuntimeException)
      throw (RuntimeException) e;
    else
      throw new ELException(e);
  }

  public int hashCode()
  {
    return toString().hashCode();
  }
  
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof Expr))
      return false;

    return toString().equals(o.toString());
  }

  abstract public String toString();

  /**
   * Returns an error object
   */
  public static Object invocationError(Throwable e)
    throws ELException
  {
    if (e instanceof InvocationTargetException && e.getCause() != null)
      e = e.getCause();

    if (e instanceof RuntimeException)
      throw (RuntimeException) e;
    else if (e instanceof Error)
      throw (Error) e;
    else
      throw new ELException(e);
  }

  private enum CoerceType {
    BOOLEAN,
    CHARACTER,
    STRING,
    INTEGER,
    DOUBLE,
    LONG,
    FLOAT,
    SHORT,
    BYTE,
    BIG_INTEGER,
    BIG_DECIMAL,
    VOID,
    OBJECT
  };

  static {
    _coerceMap.put(boolean.class, CoerceType.BOOLEAN);
    _coerceMap.put(Boolean.class, CoerceType.BOOLEAN);
    
    _coerceMap.put(byte.class, CoerceType.BYTE);
    _coerceMap.put(Byte.class, CoerceType.BYTE);
    
    _coerceMap.put(short.class, CoerceType.SHORT);
    _coerceMap.put(Short.class, CoerceType.SHORT);
    
    _coerceMap.put(int.class, CoerceType.INTEGER);
    _coerceMap.put(Integer.class, CoerceType.INTEGER);
    
    _coerceMap.put(long.class, CoerceType.LONG);
    _coerceMap.put(Long.class, CoerceType.LONG);
    
    _coerceMap.put(float.class, CoerceType.FLOAT);
    _coerceMap.put(Float.class, CoerceType.FLOAT);
    
    _coerceMap.put(double.class, CoerceType.DOUBLE);
    _coerceMap.put(Double.class, CoerceType.DOUBLE);
    
    _coerceMap.put(char.class, CoerceType.CHARACTER);
    _coerceMap.put(Character.class, CoerceType.CHARACTER);
    
    _coerceMap.put(String.class, CoerceType.STRING);
    
    _coerceMap.put(BigDecimal.class, CoerceType.BIG_DECIMAL);
    _coerceMap.put(BigInteger.class, CoerceType.BIG_INTEGER);
    
    _coerceMap.put(void.class, CoerceType.VOID);
    _coerceMap.put(Object.class, CoerceType.OBJECT);
  }

  static {
    _typeMap.put(boolean.class, BOOLEAN);
    _typeMap.put(byte.class, BYTE);
    _typeMap.put(short.class, SHORT);
    _typeMap.put(int.class, INT);
    _typeMap.put(long.class, LONG);
    _typeMap.put(float.class, FLOAT);
    _typeMap.put(double.class, DOUBLE);
    
    _typeMap.put(Boolean.class, BOOLEAN_OBJ);
    _typeMap.put(Byte.class, BYTE_OBJ);
    _typeMap.put(Short.class, SHORT_OBJ);
    _typeMap.put(Integer.class, INT_OBJ);
    _typeMap.put(Long.class, LONG_OBJ);
    _typeMap.put(Float.class, FLOAT_OBJ);
    _typeMap.put(Double.class, DOUBLE_OBJ);
    
    _typeMap.put(String.class, STRING);
  }
}
