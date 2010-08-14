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

package com.caucho.es;

import java.util.Random;

/**
 * JavaScript object
 */
class NativeMath extends Native {
  static final int ABS = 1;
  static final int ACOS = 2;
  static final int ASIN = 3;
  static final int ATAN = 4;
  static final int ATAN2 = 5;
  static final int CEIL = 6;
  static final int COS = 7;
  static final int EXP = 8;
  static final int FLOOR = 9;
  static final int LOG = 10;
  static final int MAX = 11;
  static final int MIN = 12;
  static final int POW = 13;
  static final int RANDOM = 14;
  static final int ROUND = 15;
  static final int SET_SEED = 16;
  static final int SIN = 17;
  static final int SQRT = 18;
  static final int TAN = 19;

  Random random; // XXX: s/b in global?

  /**
   * Create a new object based on a prototype
   */
  private NativeMath(String name, int n, int len)
  {
    super(name, len);

    this.n = n;
  }
  
  /**
   * Creates the native Object object
   */
  static ESObject create(Global resin)
  {
    ESObject math = new ESObject("Math", null);

    put(math, "abs", ABS, 1);
    put(math, "asin", ASIN, 1);
    put(math, "acos", ACOS, 1);
    put(math, "atan", ATAN, 1);
    put(math, "atan2", ATAN2, 2);
    put(math, "ceil", CEIL, 1);
    put(math, "cos", COS, 1);
    put(math, "exp", EXP, 1);
    put(math, "floor", FLOOR, 1);
    put(math, "log", LOG, 1);
    put(math, "max", MAX, 2);
    put(math, "min", MIN, 2);
    put(math, "pow", POW, 2);
    put(math, "round", ROUND, 1);
    put(math, "sin", SIN, 1);
    put(math, "sqrt", SQRT, 1);
    put(math, "tan", TAN, 1);
    put(math, "random", RANDOM, 0);
    put(math, "setSeed", SET_SEED, 1);

    int flags = DONT_ENUM|DONT_DELETE|READ_ONLY;
    math.put("E", ESNumber.create(Math.E), flags);
    math.put("LN10", ESNumber.create(Math.log(10.0)), flags);
    math.put("LN2", ESNumber.create(Math.log(2.0)), flags);
    math.put("LOG2E", ESNumber.create(1.0 / Math.log(2.0)), flags);
    math.put("LOG10E", ESNumber.create(1.0 / Math.log(10.0)), flags);
    math.put("PI", ESNumber.create(Math.PI), flags);
    math.put("SQRT1_2", ESNumber.create(Math.sqrt(0.5)), flags);
    math.put("SQRT2", ESNumber.create(Math.sqrt(2.0)), flags);
    // XXX: potential problems.
    Random random = new Random();
    try {
      ((NativeMath) math.getProperty("random")).random = random;
      ((NativeMath) math.getProperty("setSeed")).random = random;
    } catch (Throwable e) {
    }

    math.setClean();

    return math;
  }

  private static void put(ESObject obj, String name, int n, int length)
  {
    obj.put(name, new NativeMath(name, n, length), DONT_ENUM);
  }

  public ESBase call(Call eval, int length) throws Throwable
  {
    double arg;
    double value;

    switch (n) {
    case ABS:
      if (length == 0)
        return esUndefined;
      arg = eval.getArg(0).toNum();
      return ESNumber.create(arg == 0 ? 0 : (arg < 0 ? -arg : arg));

    case ACOS:
      if (length == 0)
        return esUndefined;
      return ESNumber.create(Math.acos(eval.getArg(0).toNum()));

    case ASIN:
      if (length == 0)
        return esUndefined;
      return ESNumber.create(Math.asin(eval.getArg(0).toNum()));

    case ATAN:
      if (length == 0)
        return esUndefined;
      return ESNumber.create(Math.atan(eval.getArg(0).toNum()));

    case ATAN2:
      if (length < 2)
        return esUndefined;
      return ESNumber.create(Math.atan2(eval.getArg(0).toNum(),
                                        eval.getArg(1).toNum()));

    case CEIL:
      if (length == 0)
        return esUndefined;
      return ESNumber.create(Math.ceil(eval.getArg(0).toNum()));

    case COS:
      if (length == 0)
        return esUndefined;
      return ESNumber.create(Math.cos(eval.getArg(0).toNum()));

    case EXP:
      if (length == 0)
        return esUndefined;
      return ESNumber.create(Math.exp(eval.getArg(0).toNum()));

    case FLOOR:
      if (length == 0)
        return esUndefined;
      return ESNumber.create(Math.floor(eval.getArg(0).toNum()));

    case LOG:
      if (length == 0)
        return esUndefined;
      return ESNumber.create(Math.log(eval.getArg(0).toNum()));

    case MAX:
      arg = -1.0/0.0;
      for (int i = 0; i < length; i++) {
        double v = eval.getArg(i).toNum();

        arg = Math.max(v, arg);
      }
      return ESNumber.create(arg);

    case MIN:
      arg = 1.0/0.0;
      for (int i = 0; i < length; i++) {
        double v = eval.getArg(i).toNum();

        arg = Math.min(v, arg);
      }
      return ESNumber.create(arg);

    case POW:
      if (length < 2)
        return esUndefined;
      return ESNumber.create(Math.pow(eval.getArg(0).toNum(),
                                      eval.getArg(1).toNum()));

    case RANDOM:
      {
        if (length > 1) {
          int n = eval.getArg(0).toInt32();
          if (n > 0)
            return ESNumber.create((int) (random.nextDouble() * n));
        } else {
          return ESNumber.create(random.nextDouble());
        }
      }

    case ROUND:
      if (length == 0)
        return esUndefined;

      arg = eval.getArg(0).toNum();
      if (arg >= -0.5 && arg < 0.5)
        return ESNumber.create(Math.rint(arg));
      else
        return ESNumber.create(Math.floor(arg + 0.5));

    case SET_SEED:
      if (length != 0)
        random.setSeed((long) eval.getArg(0).toNum());
      return esUndefined;

    case SIN:
      if (length == 0)
        return esUndefined;
      return ESNumber.create(Math.sin(eval.getArg(0).toNum()));

    case SQRT:
      if (length == 0)
        return esUndefined;
      return ESNumber.create(Math.sqrt(eval.getArg(0).toNum()));

    case TAN:
      if (length == 0)
        return esUndefined;
      return ESNumber.create(Math.tan(eval.getArg(0).toNum()));

    default:
      throw new ESException("Undefined math function");
    }
  }
}
