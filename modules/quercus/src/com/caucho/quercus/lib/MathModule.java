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

package com.caucho.quercus.lib;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;

import java.util.Iterator;
import java.math.BigInteger;

/**
 * PHP math routines.
 */
public class MathModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(MathModule.class);

  public static final double M_PI = Math.PI;
  public static final double M_E = Math.E;

  public static final long RAND_MAX = Integer.MAX_VALUE;

  public static final double M_LOG2E = log2(Math.E);
  public static final double M_LOG10E = Math.log10(Math.E);
  public static final double M_LN2 = Math.log(2);
  public static final double M_LN10 = Math.log(10);
  public static final double M_PI_2 = Math.PI / 2;
  public static final double M_PI_4 = Math.PI / 4;
  public static final double M_1_PI = 1 / Math.PI;
  public static final double M_2_PI = 2 / Math.PI;
  public static final double M_SQRTPI = Math.sqrt(Math.PI);
  public static final double M_2_SQRTPI = 2 / Math.sqrt(Math.PI);
  public static final double M_SQRT2 = Math.sqrt(2);
  public static final double M_SQRT3 = Math.sqrt(3);
  public static final double M_SQRT1_2 = 1 / Math.sqrt(2);
  public static final double M_LNPI = Math.log(Math.PI);
  public static final double M_EULER = 0.57721566490153286061;

  private static double log2(double v)
  {
    return Math.log(v) / Math.log(2);
  }

  public static Value abs(Value value)
  {
    return value.abs();
  }

  public static double acos(double value)
  {
    return Math.acos(value);
  }

  public static double acosh(Env env, double value)
  {
    return Math.log(value + Math.sqrt(value * value - 1));
  }

  public static Value asin(Value value)
  {
    return new DoubleValue(Math.asin(value.toDouble()));
  }

  public static double asinh(double value)
  {
    return Math.log(value + Math.sqrt(value * value + 1));
  }

  public static double atan2(double yV, double xV)
  {
    return Math.atan2(yV, xV);
  }

  public static double atan(double value)
  {
    return Math.atan(value);
  }

  public static double atanh(double value)
  {
    return 0.5 * Math.log((1 + value) / (1 - value));
  }

  /**
   * Convert a number between arbitrary bases
   *
   * @param number A string represeantion of an binary number.
   * @param fromBase The base of the number parameter.
   * @param toBase The base of convert to.
   * @return the number as a Value, either a LongValue or a DoubleValue.
   */
  public static Value base_convert(Env env,
                                   StringValue str,
                                   int fromBase,
                                   int toBase)
  {
    if (fromBase < 2 || fromBase > 36) {
      env.warning(L.l("invalid `{0}' ({1})", "from base", fromBase));
      return BooleanValue.FALSE;
    }

    if (toBase < 2 || toBase > 36) {
      env.warning(L.l("invalid `{0}' ({1})", "to base", toBase));
      return BooleanValue.FALSE;
    }
    
    Number num = baseToInt(env, str, fromBase);
    
    if (num instanceof BigInteger)
      return intToBase(env, (BigInteger) num, toBase);
    else
      return intToBase(env, num.longValue(), toBase);
  }
  
  private static Number baseToInt(Env env, StringValue str, int base)
  {
    long result = 0L;

    boolean isLong = true;
    
    int len = str.length();
    
    for (int i = 0; i < len; i++) {
      int ch = str.charAt(i);
      
      int d;
      
      if ('0' <= ch && ch <= '9')
        d = ch - '0';
      else if ('a' <= ch && ch <= 'z')
        d = ch - 'a' + 10;
      else if ('A' <= ch && ch <= 'Z')
        d = ch - 'A' + 10;
      else
        continue;
      
      if (base <= d)
        continue;
      
      if (result * base + d < result) {
        isLong = false;
        break;
      }
      
      result = result * base + d;
    }
    
    if (isLong)
      return Long.valueOf(result);
    else
      return new BigInteger(str.toString(), base);
  }
  
  private static StringValue intToBase(Env env, long num, int base)
  {
    if (num == 0)
      return env.createString((char) '0');
    
    // ignore sign
    if (num < 0)
      num = num ^ Long.MAX_VALUE + 1;
    
    int bufLen = 64;
    char []buffer = new char[bufLen];
    
    int i = bufLen;
    while (num != 0 && i > 0) {
      int d = (int) (num % base);

      if (d < 10)
        buffer[--i] = (char) (d + '0');
      else
        buffer[--i] = (char) (d + 'a' - 10);

      num = num / base;
    }
    
    for (int j = i; j < bufLen; j++) {
      buffer[j - i] = buffer[j];
    }

    return env.createString(buffer, bufLen - i);
  }
  
  private static StringValue intToBase(Env env, BigInteger num, int base)
  {
    BigInteger toBaseBig = BigInteger.valueOf(base);
    BigInteger zero = BigInteger.valueOf(0);
    
    StringValue sb = env.createStringBuilder();
    
    do {
      BigInteger []resultArray = num.divideAndRemainder(toBaseBig);
      
      num = resultArray[0];
      int d = resultArray[1].intValue();
      
      if (d < 10)
        sb.append((char) (d + '0'));
      else
        sb.append((char) (d + 'a' - 10));
      
    } while (num.compareTo(zero) != 0);

    StringValue toReturn = env.createStringBuilder();
    
    int len = sb.length();
    for (int i = len - 1; i >= 0; i--) {
      toReturn.append(sb.charAt(i));
    }

    return toReturn;
  }

  /**
   * Returns the decimal equivalent of the binary number represented by the
   * binary string argument.
   *
   * @param bin A string representation of an binary number.
   * @return the decimal equivalent of the binary number
   */
  public static Value bindec(Env env, StringValue bin)
  {
     Number num = baseToInt(env, bin, 2);
     
     if (num instanceof Long)
       return LongValue.create(num.longValue());
     else
       return env.wrapJava(num);
  }

  public static double ceil(double value)
  {
    return Math.ceil(value);
  }

  public static double cos(double value)
  {
    return Math.cos(value);
  }

  public static double cosh(double value)
  {
    return Math.cosh(value);
  }

  /**
   * Returns a binary representation of a number.
   * @param value the number
   */
  public static StringValue decbin(Env env, long value)
  {
    return intToBase(env, value, 2);
    
    /*
    value = value & 037777777777L;
    
    if (value == 0)
      return env.createString("0");
    
    char []buffer = new char[32];

    int i = 32;
    while (value != 0 && i >= 0) {
      int d = (int) (value & 0x01);
      
      value = value >>> 1;
      
      buffer[--i] = (char) (d + '0');
    }

    for (int j = i; j < 32; j++) {
      buffer[j - i] = buffer[j];
    }

    return env.createString(buffer, 32 - i);
    */
  }

  /**
   * Returns a hexadecimal representation of a number.
   * @param value the number
   */
  public static StringValue dechex(Env env, long value)
  {
    return intToBase(env, value, 16);
    
    /*
    value = value & 037777777777L;
    
    if (value == 0)
      return env.createString("0");
    
    char []buffer = new char[16];

    int i = 16;
    while (value != 0) {
      int d = (int) (value & 0xf);
      
      value = value >>> 4;
      
      if (d < 10)
        buffer[--i] = (char) (d + '0');
      else
        buffer[--i] = (char) (d + 'a' - 10);
    }

    for (int j = i; j < 16; j++) {
      buffer[j - i] = buffer[j];
    }

    return env.createString(buffer, 16 - i);
    */
  }

  /**
   * Returns an octal representation of a number.
   * @param value the number
   */
  public static StringValue decoct(Env env, long value)
  {
    return intToBase(env, value, 8);
    
    /*
    value = value & 037777777777L;
    
    if (value == 0)
      return env.createString("0");
    
    char []buffer = new char[11];

    int i = 11;
    while (value != 0 && i > 0) {
      int d = (int) (value & 0x7);
      
      value = value >>> 3;
      
      buffer[--i] = (char) (d + '0');
    }

    for (int j = i; j < 11; j++) {
      buffer[j - i] = buffer[j];
    }

    return env.createString(buffer, 11 - i);
    */
  }

  public static double deg2rad(double value)
  {
    return value * Math.PI / 180;
  }

  public static Value exp(Value value)
  {
    return new DoubleValue(Math.exp(value.toDouble()));
  }

  public static Value expm1(Value value)
  {
    return new DoubleValue(Math.expm1(value.toDouble()));
  }

  public static Value floor(Value value)
  {
    return new DoubleValue(Math.floor(value.toDouble()));
  }

  public static double fmod(double xV, double yV)
  {
    return Math.IEEEremainder(xV, yV);
  }

  public static Value hexdec(Env env, StringValue s)
  {
    Number num = baseToInt(env, s, 16);
    
    if (num instanceof Long)
      return LongValue.create(num.longValue());
    else
      return env.wrapJava(num);
  }

  public static double hypot(double a, double b)
  {
    return Math.hypot(a, b);
  }

  public static boolean is_finite(Value value)
  {
    if (value instanceof LongValue)
      return true;
    else if (value instanceof DoubleValue) {
      double v = value.toDouble();

      return ! Double.isInfinite(v);
    }
    else
      return false;
  }

  public static Value is_infinite(Value value)
  {
    if (value instanceof LongValue)
      return BooleanValue.FALSE;
    else if (value instanceof DoubleValue) {
      double v = value.toDouble();

      return Double.isInfinite(v) ? BooleanValue.TRUE : BooleanValue.FALSE;
    }
    else
      return BooleanValue.FALSE;
  }

  public static Value is_nan(Value value)
  {
    if (value instanceof LongValue)
      return BooleanValue.FALSE;
    else if (value instanceof DoubleValue) {
      double v = value.toDouble();

      return Double.isNaN(v) ? BooleanValue.TRUE : BooleanValue.FALSE;
    }
    else
      return BooleanValue.FALSE;
  }

  public static double log(double value)
  {
    return Math.log(value);
  }

  public static double log10(double value)
  {
    return Math.log10(value);
  }

  public static double log1p(double value)
  {
    return Math.log1p(value);
  }

  public static Value getrandmax()
  {
    return mt_getrandmax();
  }

  public static Value max(Env env, Value []args)
  {
    if (args.length == 1 && args[0] instanceof ArrayValue) {
      Value array = args[0];
      Value max = NullValue.NULL;
      double maxValue = Double.MIN_VALUE;

      Iterator<Value> iter = array.getValueIterator(env);

      while (iter.hasNext()) {
        Value value = iter.next();

        double dValue = value.toDouble();

        if (maxValue < dValue) {
          maxValue = dValue;
          max = value;
        }
      }

      return max;
    }
    else {
      double maxValue = - Double.MAX_VALUE;
      Value max = NullValue.NULL;

      for (int i = 0; i < args.length; i++) {
        double value = args[i].toDouble();

        if (maxValue < value) {
          maxValue = value;
          max = args[i];
        }
      }

      return max;
    }
  }

  public static Value min(Env env, Value []args)
  {
    if (args.length == 1 && args[0] instanceof ArrayValue) {
      Value array = args[0];
      Value min = NullValue.NULL;
      double minValue = Double.MAX_VALUE;

      Iterator<Value> iter = array.getValueIterator(env);

      while (iter.hasNext()) {
        Value value = iter.next();

        double dValue = value.toDouble();

        if (dValue < minValue) {
          minValue = dValue;
          min = value;
        }
      }

      return min;
    }
    else {
      double minValue = Double.MAX_VALUE;
      Value min = NullValue.NULL;

      for (int i = 0; i < args.length; i++) {
        double value = args[i].toDouble();

        if (value < minValue) {
          minValue = value;
          min = args[i];
        }
      }

      return min;
    }
  }

  public static Value mt_getrandmax()
  {
    return new LongValue(RAND_MAX);
  }

  public static long mt_rand(@Optional("0") long min,
                             @Optional("RAND_MAX") long max)
  {
    long range = max - min + 1;

    if (range <= 0)
      return min;

    long value = RandomUtil.getRandomLong();
    if (value < 0)
      value = - value;

    return min + value % range;
  }

  public static Value mt_srand(@Optional long seed)
  {
    return NullValue.NULL;
  }
  
  public static double lcg_value()
  {
    return RandomUtil.nextDouble();
  }

  /**
   *  Returns the decimal equivalent of the octal number represented by the
   *  octal_string argument.
   *
   * @param oct A string represeantion of an octal number.
   * @return the decimal equivalent of the octal number
   */
  public static Value octdec(Env env, StringValue oct)
  {
    Number num = baseToInt(env, oct, 8);
    
    if (num instanceof Long)
      return LongValue.create(num.longValue());
    else
      return env.wrapJava(num);
  }

  public static double pi()
  {
    return M_PI;
  }

  /*
   * XXX: may return an integer if it can be represented
   *      by a system-dependent integer
   */
  public static double pow(double base, double exp)
  {
    return Math.pow(base, exp);
  }

  public static double rad2deg(double value)
  {
    return 180 * value / Math.PI;
  }

  public static long rand(@Optional int min,
                          @Optional("RAND_MAX") int max)
  {
    return mt_rand(min, max);
  }

  public static double round(double value, @Optional int precision)
  {
    if (precision > 0) {
      double exp = Math.pow(10, precision);

      return Math.round(value * exp) / exp;
    }
    else
      return Math.round(value);
  }

  public static double sin(double value)
  {
    return Math.sin(value);
  }

  public static Value sinh(Value value)
  {
    return new DoubleValue(Math.sinh(value.toDouble()));
  }

  public static double sqrt(double value)
  {
    return Math.sqrt(value);
  }

  public static Value srand(@Optional long seed)
  {
    return NullValue.NULL;
  }

  public static double tan(double value)
  {
    return Math.tan(value);
  }

  public static double tanh(double value)
  {
    return Math.tanh(value);
  }
}
