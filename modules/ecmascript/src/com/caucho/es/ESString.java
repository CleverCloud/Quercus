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

import com.caucho.util.CharBuffer;
import com.caucho.util.IntMap;

import java.text.CharacterIterator;

/**
 * Implementation class for JavaScript strings.
 */
public class ESString extends ESBase {
  static ESId NULL;
  static ESId LENGTH;
  private static ESId ints[];

  protected String string;
  protected int hashCode;

  /**
   * Create a new object based on a prototype
   */
  protected ESString(String string)
  {
    if (ints == null) {
      ints = new ESId[128];
      NULL = ESId.intern("");
      LENGTH = ESId.intern("length");

      for (int i = 0; i < ints.length; i++)
        ints[i] = ESId.intern(String.valueOf(i));
    }

    prototype = esNull;
    this.string = string;
  }

  public static ESString create(String string)
  {
    return string == null ? NULL : new ESString(string);
  }

  public static ESBase toStr(String string)
  {
    return string == null ? esNull : new ESString(string);
  }

  public static ESString create(int i)
  {
    if (i >= 0 && i < ints.length)
      return ints[i];

    return new ESString(String.valueOf(i));
  }

  public static ESString createFromCharCode(char c)
  {
    return new ESString(String.valueOf((char) c));
  }

  public static ESString create(CharBuffer cb)
  {
    return new ESString(cb.toString());
  }

  /**
   * Create a new string from a java object.
   */
  public static ESString create(Object obj)
  {
    return new ESString(String.valueOf(obj));
  }

  public ESBase typeof() throws ESException
  {
    return ESString.create("string");
  }

  public Class getJavaType()
  {
    return String.class;
  }

  public double toNum()
  {
    return parseFloat(this, true);
  }

  public ESString toStr()
  {
    return this;
  }

  public boolean isString()
  {
    return true;
  }

  public ESString toSource(IntMap map, boolean isLoopPass)
  {
    if (isLoopPass)
      return null;

    return ESString.create("\"" + string + "\"");
  }

  public ESObject toObject()
  {
    ESObject obj = new ESWrapper("String", Global.getGlobalProto().stringProto, this);

    obj.put(LENGTH, ESNumber.create(this.string.length()),
            DONT_ENUM|DONT_DELETE|READ_ONLY);

    return obj;
  }

  public Object toJavaObject()
  {
    return string;
  }

  public ESBase getProperty(ESString key) throws Throwable
  {
    if (key.equals(LENGTH))
      return ESNumber.create(this.string.length());
    else
      return Global.getGlobalProto().stringProto.getProperty(key);
  }

  public boolean ecmaEquals(ESBase b) throws Throwable
  {
    if (b == esNull)
      return false;
    else if (b instanceof ESString)
      return string.equals(((ESString) b).string);
    else if (b instanceof ESObject) {
      ESBase pb = b.toPrimitive(NONE);
      if (pb instanceof ESString)
        return equals(pb);
      else
        return toNum() == pb.toNum();
    } else {
      return toNum() == b.toNum();
    }
  }

  public ESBase plus(ESBase b) throws Throwable
  { 
    ESBase prim = b.toPrimitive(NONE);

    return ESString.create(string + prim.toStr().toString());
  }

  char charAt(int i) { return string.charAt(i); }

  public char carefulCharAt(int i) 
  { 
    if (i >= string.length())
      return CharacterIterator.DONE;
    else
      return string.charAt(i); 
  }

  int length() { return string.length(); }

  boolean regionMatches(int i, String test, int j, int len) 
  { 
    return string.regionMatches(i, test, j, len);
  }

  public int hashCode() 
  { 
    int hash = hashCode;

    if (hash != 0)
      return hash;

    hash = 1021;
    int len = string.length();

    for (int i = 0; i < len; i++)
      hash = 65521 * hash + (string.charAt(i) + 1) * 251;

    hashCode = hash;

    return hash;
  }

  public boolean equals(Object a) 
  { 
    if (this == a)
      return true;
    else if (a instanceof ESString)
      return string.equals(((ESString) a).string);
    else
      return false;
  }

  int compareTo(ESString b)
  { 
    return string.compareTo(b.string);
  }

  int indexOf(ESString a, int pos) { return string.indexOf(a.string, pos); }
  int lastIndexOf(ESString a, int pos) 
  { 
    return string.lastIndexOf(a.string, pos); 
  }

  ESString substring(int begin, int end)
  { 
    return ESString.create(string.substring(begin, end));
  }

  ESString substring(int begin)
  { 
    return substring(begin, string.length());
  }

  ESString toLowerCase()
  { 
    return ESString.create(string.toLowerCase());
  }

  ESString toUpperCase()
  { 
    return ESString.create(string.toUpperCase());
  }

  ESBoolean contains(ESBase b) throws Throwable
  { 
    ESString sb = b.toStr();
    if (sb.string.length() == 0)
      return ESBoolean.TRUE;

    int len = string.length();
    for (int i = 0; i <= len; i++) {
      if (string.regionMatches(i, sb.string, 0, sb.length()))
        return ESBoolean.TRUE;
    } 

    return ESBoolean.FALSE;
  }

  ESBoolean startsWith(ESBase b) throws Throwable
  { 
    ESString sb = b.toStr();

    return ESBoolean.create(string.startsWith(sb.string));
  }

  ESBoolean endsWith(ESBase b) throws Throwable
  { 
    ESString sb = b.toStr();

    return ESBoolean.create(string.endsWith(sb.string));
  }

  public boolean toBoolean()
  {
    return string.length() != 0;
  }

  static boolean isWhitespace(int ch)
  {
    switch (ch) {
    case ' ': case '\t': case '\n': case '\r': case 0x0b: case '\f':
      return true;

    default:
      return false;
    }
  }

  static double checkTail(double value, ESString string, int i)
  {
    for (; i < string.length(); i++) {
      if (! isWhitespace(string.charAt(i))) {
        return 0.0/0.0;
      }
    }

    return value;
  }

  static double parseFloat(ESString string, boolean hexOkay)
  {
    int len = string.length();

    int i = 0;
    int ch = 0;
    for (; i < len && isWhitespace(string.charAt(i)); i++) {
    }

    if (i >= len)
      return hexOkay ? 0 : 0.0/0.0; 

    int radix = 10;
    if (hexOkay && i + 1 < len && (ch = string.charAt(i)) == '0' &&
        ((ch = string.charAt(i + 1)) == 'x' || ch == 'X')) {
      i += 2;
      radix = 16;
    }

    int sign = 1;
    if (radix == 10 && i < len && (ch = string.charAt(i)) == '+') {
      i++;
    } else if (radix == 10 && i < len && ch == '-') {
      sign = -1;
      i++;
    }

    if (radix == 10 && string.regionMatches(i, "Infinity", 0, 8)) {
      if (hexOkay)
        return checkTail(sign * (1.0/0.0), string, i + 8);
      else
        return sign * (1.0/0.0);
    }
    double value = 0.0;
    boolean hasDigit = false;
    for (; i < len; i++) {
      ch = string.charAt(i);

      if ('0' <= ch && ch <= '9') {
        value = radix * value + string.charAt(i) - '0';
        hasDigit = true;
      } else if (radix > 10 && ('a' <= ch && ch <= 'a' + radix - 11)) {
        value = radix * value + string.charAt(i) - 'a' + 10;
        hasDigit = true;
      } else if (radix > 10 && ('A' <= ch && ch <= 'A' + radix - 11)) {
        value = radix * value + string.charAt(i) - 'A' + 10;
        hasDigit = true;
      } else
        break;
    }

    if (radix == 16 && ! hasDigit)
      return 0.0/0.0;
    else if (radix == 16)
      return checkTail(value, string, i);

    int expt = 0;
    if (i < len && string.charAt(i) == '.') {
      i++;

      int power = 1;
      for (; i < len && (ch = string.charAt(i)) >= '0' && ch <= '9'; i++) {
        if (ch == '0')
          power++;
        else {
          value = value * Math.pow(10, power) + ch - '0';
          expt -= power;
          power = 1;
        }
        hasDigit = true;
      }
    }

    if (! hasDigit)
      return 0.0/0.0;

    if (i < len && ((ch = string.charAt(i)) == 'e' || ch == 'E')) {
      i++;
      int exptSign = 1;
      if (i < len && (ch = string.charAt(i)) == '+')
        i++;
      else if (i < len && ch == '-') {
        exptSign = -1;
        i++;
      }

      hasDigit = false;

      int newExpt = 0;
      for (; i < len && (ch = string.charAt(i)) >= '0' && ch <= '9'; i++) {
        newExpt = 10 * newExpt + ch - '0';
        hasDigit = true;
      }

      if (! hasDigit)
        return 0.0/0.0;

      expt += exptSign * newExpt;
    }

    if (expt < 0)
      value = sign * value / Math.pow(10.0, -expt);
    else
      value = sign * value * Math.pow(10.0, expt);

    if (hexOkay)
      return checkTail(value, string, i);
    else
      return value;
  }

  static double parseFloat(ESString string)
  {
    return parseFloat(string, false);
  }

  /**
   * Returns this as a string.
   */
  public String toString()
  {
    return string;
  }
}
