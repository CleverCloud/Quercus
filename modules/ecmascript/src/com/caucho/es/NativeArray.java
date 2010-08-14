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

/**
 * JavaScript object
 */
class NativeArray extends Native {
  static ESId LENGTH = ESId.intern("length");
  static final int NEW = 1;
  static final int JOIN = NEW + 1;
  static final int TO_STRING = JOIN + 1;
  static final int REVERSE = TO_STRING + 1;
  static final int SORT = REVERSE + 1;

  // js1.2
  static final int CONCAT = SORT + 1;
  static final int POP = CONCAT + 1;
  static final int PUSH = POP + 1;
  static final int SHIFT = PUSH + 1;
  static final int UNSHIFT = SHIFT + 1;
  static final int SLICE = UNSHIFT + 1;
  static final int SPLICE = SLICE + 1;

  /**
   * Create a new object based on a prototype
   */
  private NativeArray(String name, int n, int len)
  {
    super(name, len);

    this.n = n;
  }

  /**
   * Creates the native Array object
   */
  static ESObject create(Global resin)
  {
    Native nativeArray = new NativeArray("Array", NEW, 1);
    ESArray proto = new ESArray();
    proto.prototype = resin.objProto;
    NativeWrapper array = new NativeWrapper(resin, nativeArray,
                                            proto, ESThunk.ARRAY_THUNK);
    resin.arrayProto = proto;

    put(proto, "join", JOIN, 1);
    put(proto, "toString", TO_STRING, 0);
    put(proto, "reverse", REVERSE, 0);
    put(proto, "sort", SORT, 0);

    // js1.2
    put(proto, "concat", CONCAT, 0);
    put(proto, "pop", POP, 0);
    put(proto, "push", PUSH, 0);
    put(proto, "shift", SHIFT, 0);
    put(proto, "unshift", UNSHIFT, 0);
    put(proto, "slice", SLICE, 2);
    put(proto, "splice", SPLICE, 0);

    proto.setClean();
    array.setClean();

    return array;
  }

  private static void put(ESObject obj, String name, int n, int len)
  {
    ESId id = ESId.intern(name);

    obj.put(id, new NativeArray(name, n, len), DONT_ENUM);
  }


  public ESBase call(Call eval, int length) throws Throwable
  {
    switch (n) {
    case NEW:
      return create(eval, length);

    case JOIN:
      if (length == 0)
        return toString(eval, length);
      else
        return join(eval, length);

    case TO_STRING:
      return toString(eval, length);

    case REVERSE:
      return reverse(eval, length);

    case SORT:
      return sort(eval, length);

    case CONCAT:
      return concat(eval, length);

    case POP:
      return pop(eval, length);

    case PUSH:
      return push(eval, length);

    case SHIFT:
      return shift(eval, length);

    case UNSHIFT:
      return unshift(eval, length);

    case SLICE:
      return slice(eval, length);

    case SPLICE:
      return splice(eval, length);

    default:
      throw new ESException("Unknown object function");
    }
  }

  ESBase create(Call eval, int length) throws Throwable
  {
    ESObject obj = Global.getGlobalProto().createArray();

    if (length == 0)
      return obj;
    if (length == 1) {
      ESBase arg = eval.getArg(0);

      if (arg instanceof ESNumber)
        obj.setProperty(LENGTH, ESNumber.create(arg.toInt32()));
      else
        obj.setProperty(0, arg);

      return obj;
    }

    for (int i = 0; i < length; i++)
      obj.setProperty(i, eval.getArg(i));

    return obj;
  }

  static ESBase join(ESObject array, String separator) throws Throwable
  {
    if (array.mark != 0) {
      return ESString.create("...");
    }
    array.mark = -1;

    try {
      int len = array.getProperty(LENGTH).toInt32();
      StringBuffer sbuf = new StringBuffer();

      for (int i = 0; i < len; i++) {
        if (i != 0)
          sbuf.append(separator);

        ESBase value = array.hasProperty(i);

        if (value != null && value != esNull && value != esUndefined)
          sbuf.append(value.toString());
      }

      return ESString.create(sbuf.toString());
    } finally {
      array.mark = 0;
    }
  }

  ESBase join(Call eval, int length) throws Throwable
  {
    String separator = length == 0 ? "," : eval.getArg(0).toString();

    ESObject array = eval.getArg(-1).toObject();

    return join(array, separator);
  }

  static ESBase toString(ESObject array) throws Throwable
  {
    return join(array, ",");
  }

  // XXX: different for non-js1.2
  ESBase toString(Call eval, int length) throws Throwable
  {
    ESObject array = eval.getArg(-1).toObject();

    return toString(array);
  }

  ESBase reverse(Call eval, int length) throws Throwable
  {
    ESObject array = eval.getArg(-1).toObject();

    int len = (int) array.getProperty(LENGTH).toInt32();
    for (int k = 0; k < len / 2; k++) {
      int firstIndex = k;
      int secondIndex = len - k - 1;

      ESBase first = array.hasProperty(firstIndex);
      ESBase second = array.hasProperty(secondIndex);

      if (first == null)
        array.delete(secondIndex);
      else
        array.setProperty(secondIndex, first);

      if (second == null)
        array.delete(firstIndex);
      else
        array.setProperty(firstIndex, second);
    }

    return array;
  }

  ESBase sort(Call eval, int length) throws Throwable
  {
    ESObject array = eval.getArg(-1).toObject();

    ESBase cmp = length == 0 ? null : eval.getArg(0);

    int len = (int) array.getProperty(LENGTH).toInt32();
    ESBase []values = new ESBase[len];

    for (int i = 0; i < len; i++)
      values[i] = array.getProperty("" + i);

    qsort(values, 0, len, cmp);

    for (int i = 0; i < len; i++) {
      if (values[i] == esUndefined)
        array.delete("" + i);
      else
        array.setProperty("" + i, values[i]);
    }

    return array;
  }

  private void qsort(ESBase []array, int offset, int length, ESBase cmp)
    throws Throwable
  {
    if (length == 2) {
      if (compare(cmp, array[offset], array[offset + 1]) > 0) {
        ESBase temp = array[offset];
        array[offset] = array[offset + 1];
        array[offset + 1] = temp;
      }
    } else if (length > 2) {
      int keyIndex = offset + length / 2;
      ESBase key = array[keyIndex];
      int keys = 0;
      int tail = 0;
      int val;

      if ((val = compare(cmp, array[offset], key)) > 0) {
        key = array[offset];
        array[offset] = array[keyIndex];
        array[keyIndex] = key;
      } else if (val == 0)
        keys++;

      if ((val = compare(cmp, key, array[offset + length - 1])) > 0) {
        key = array[offset + length - 1];
        array[offset + length - 1] = array[keyIndex];
        array[keyIndex] = key;
        keys = 0;
        tail = 1;

        if ((val = compare(cmp, array[offset], key)) > 0) {
          key = array[offset];
          array[offset] = array[keyIndex];
          array[keyIndex] = key;
        } else if (val == 0)
          keys++;
      } else if (val < 0)
        tail = 1;

      int i;
      if (keyIndex == offset + 1) {
        i = 2 + tail;
        keys++;
      }
      else
        i = 1 + tail;

      for (; i < length; i++) {
        int index = offset + i - tail;

        if (array[index] == key) {
          keys++;
          continue;
        }

        int cmpResult = compare(cmp, key, array[index]);
        if (cmpResult > 0 && keys != 0) {
          ESBase temp = array[index];
          array[index] = array[index - keys];
          array[index - keys] = temp;
        } else if (cmpResult < 0) {
          ESBase temp = array[offset + length - tail - 1];
          array[offset + length - tail - 1] = array[index];
          array[index] = temp;
          tail += 1;
        } else if (cmpResult == 0)
          keys++;
      }

      if (length - tail - keys > 1)
        qsort(array, offset, length - tail - keys, cmp);
      if (tail > 1)
        qsort(array, offset + length - tail, tail, cmp);
    }
  }

  private int compare(ESBase cmp, ESBase a, ESBase b)
    throws Throwable
  {
    if (a == b)
      return 0;
    else if (a == esUndefined)
      return 1;
    else if (b == esUndefined)
      return -1;
    else if (a == esNull)
      return 1;
    else if (b == esNull)
      return -1;
    else if (cmp != null) {
      // Call eval = new Call(ESGlobal.getGlobalProto(), false);
      Global resin = Global.getGlobalProto();
      Call eval = resin.getCall();

      eval.stack[0] = esNull;
      eval.stack[1] = a;
      eval.stack[2] = b;
      eval.top = 1;

      int result = cmp.call(eval, 2).toInt32();

      resin.freeCall(eval);

      return result;
    }
    else {
      String sa = a.toString();
      String sb = b.toString();

      return sa.compareTo(sb);
    }
  }

  ESBase concat(Call eval, int length) throws Throwable
  {
    ESArray array = Global.getGlobalProto().createArray();

    int k = 0;
    for (int i = -1; i < length; i++) {
      ESBase arg = eval.getArg(i);

      if (arg == esNull || arg == esUndefined || arg == esEmpty)
        continue;

      ESBase arglen = arg.hasProperty(LENGTH);

      if (arglen == null) {
        array.setProperty(k++, arg);
        continue;
      }

      int len = (int) arglen.toInt32();

      if (len < 0) {
        array.setProperty(k++, arg);
        continue;
      }

      for (int j = 0; j < len; j++) {
        ESBase obj = arg.hasProperty(j);

        if (obj != null)
          array.setProperty(k, obj);
        k++;
      }
    }
    array.setProperty(LENGTH, ESNumber.create(k));

    return array;
  }

  ESBase pop(Call eval, int length) throws Throwable
  {
    ESObject obj = eval.getArg(-1).toObject();

    ESBase lenObj = obj.hasProperty(LENGTH);
    int len;
    if (lenObj == null || (len = lenObj.toInt32()) <= 0)
      return esUndefined;

    ESBase value = obj.getProperty(len - 1);

    obj.setProperty(LENGTH, ESNumber.create(len - 1));

    return value;
  }

  ESBase push(Call eval, int length) throws Throwable
  {
    ESObject obj = eval.getArg(-1).toObject();

    ESBase lenObj = obj.getProperty(LENGTH);
    int len = lenObj.toInt32();
    if (len < 0)
      len = 0;

    for (int i = 0; i < length; i++)
      obj.setProperty(len + i, eval.getArg(i));

    ESNumber newLen = ESNumber.create(len + length);
    obj.setProperty(LENGTH, newLen);

    return newLen;
  }

  ESBase shift(Call eval, int length) throws Throwable
  {
    ESObject obj = eval.getArg(-1).toObject();

    ESBase lenObj = obj.hasProperty(LENGTH);
    int len;
    if (lenObj == null || (len = (int) lenObj.toInt32()) <= 0)
      return esUndefined;

    ESBase value = obj.getProperty(0);

    for (int i = 1; i < len; i++) {
      ESBase temp = obj.hasProperty(i);
      if (temp == null)
        obj.delete(ESString.create(i - 1));
      else
        obj.setProperty(i - 1, temp);
    }

    obj.setProperty(LENGTH, ESNumber.create(len - 1));

    return value;
  }

  ESBase unshift(Call eval, int length) throws Throwable
  {
    ESObject obj = eval.getArg(-1).toObject();

    ESBase lenObj = obj.getProperty(LENGTH);
    int len = lenObj.toInt32();
    if (len < 0)
      len = 0;

    if (length == 0)
      return ESNumber.create(0);

    for (int i = len - 1; i >= 0; i--) {
      ESBase value = obj.getProperty(i);

      if (value == null)
        obj.delete(ESString.create(length + i));
      else
        obj.setProperty(length + i, value);
    }

    for (int i = 0; i < length; i++) {
      ESBase value = eval.getArg(i);

      if (value == null)
        obj.delete(ESString.create(i));
      else
        obj.setProperty(i, value);
    }

    ESNumber numLen = ESNumber.create(len + length);
    obj.setProperty(LENGTH, numLen);

    return numLen;
  }

  ESBase slice(Call eval, int length) throws Throwable
  {
    ESObject obj = eval.getArg(-1).toObject();

    ESBase lenObj = obj.getProperty(LENGTH);
    int len = lenObj.toInt32();

    ESArray array = Global.getGlobalProto().createArray();
    if (len <= 0)
      return array;

    int start = 0;
    if (length > 0)
      start = eval.getArg(0).toInt32();
    if (start < 0)
      start += len;
    if (start < 0)
      start = 0;
    if (start > len)
      return array;

    int end = len;
    if (length > 1)
      end = eval.getArg(1).toInt32();
    if (end < 0)
      end += len;
    if (end < 0)
      return array;
    if (end > len)
      end = len;

    if (start >= end)
      return array;

    for (int i = 0; i < end - start; i++) {
      ESBase value = obj.hasProperty(start + i);

      if (value != null)
        array.setProperty(i, value);
    }

    array.setProperty(LENGTH, ESNumber.create(end - start));

    return array;
  }

  ESBase splice(Call eval, int length) throws Throwable
  {
    if (length < 2)
      return esUndefined;

    ESObject obj = eval.getArg(-1).toObject();

    int index = eval.getArg(0).toInt32();
    int count = eval.getArg(1).toInt32();
    boolean single = count == 1;

    ESBase lenObj = obj.getProperty(LENGTH);
    int len = lenObj.toInt32();

    if (index < 0)
      index += len;
    if (index < 0)
      index = 0;

    if (count < 0)
      count = 0;
    if (index + count > len)
      count = len - index;

    ESBase value;

    if (count < 1)
      value = esUndefined;
    else {
      value = Global.getGlobalProto().createArray();

      for (int i = 0; i < count; i++)
        value.setProperty(i, obj.getProperty(index + i));
    }

    int delta = length - 2 - count;
    if (delta < 0) {
      for (int i = 0; i < len - count; i++) {
        ESBase temp = obj.getProperty(i + index + count);
        if (temp == null)
          obj.delete(ESString.create(i + index + count + delta));
        else
          obj.setProperty(i + index + count + delta, temp);
      }
    } else if (delta > 0) {
      for (int i = len - count - 1; i >= 0; i--) {
        ESBase temp = obj.getProperty(i + index + count);
        if (temp == null)
          obj.delete(ESString.create(i + index + count + delta));
        else
          obj.setProperty(i + index + count + delta, temp);
      }
    }

    for (int i = 0; i < length - 2; i++)
      obj.setProperty(i + index, eval.getArg(i + 2));

    obj.setProperty(LENGTH, ESNumber.create(len - count + length - 2));

    return value;
  }

}
