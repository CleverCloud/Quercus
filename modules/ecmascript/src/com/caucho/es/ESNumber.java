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

import com.caucho.vfs.VfsWriteObject;
import com.caucho.vfs.WriteStream;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Implementation class for JavaScript numbers.  Essentially, these are
 * equivalent to Java doubles.
 */
public class ESNumber extends ESBase implements VfsWriteObject, Externalizable {
  public static ESNumber ZERO = new ESNumber(0.0);
  public static ESNumber ONE = new ESNumber(1.0);
  public static ESNumber NaN = new ESNumber(0.0/0.0);
  static ESNumber ints[];
  static {
    ints = new ESNumber[128];
    for (int i = 0; i < ints.length; i++)
      ints[i] = new ESNumber(i);
  }

  private double value;

  /**
   * Null-arg constructor for serialization.
   */
  public ESNumber()
  {
    prototype = esNull;
  }

  /**
   * Create a new object based on a prototype
   */
  private ESNumber(double value)
  {
    prototype = esNull;
    this.value = value;
  }

  public static ESNumber create(double value)
  {
    try {
      // Can't use 0 because of -0
      if (value >= 128 || value <= 0)
        return new ESNumber(value);
    
      int intValue = (int) value;
      if (intValue == value)
        return ints[intValue];
      else
        return new ESNumber(value);
    } catch (Exception e) {
      return new ESNumber(value);
    }
  }

  /**
   * Any non-zero number is true.
   *
   * XXX: NaN and inf?
   */
  public boolean toBoolean()
  {
    return ! Double.isNaN(value) && value != 0.0;
  }

  public boolean isNum()
  {
    return true;
  }

  public double toNum()
  {
    return value;
  }

  public ESObject toObject() throws ESException
  {
    return new ESWrapper("Number", Global.getGlobalProto().numProto, this);
  }

  public Object toJavaObject()
  {
    return new Double(value);
  }

  public ESBase typeof() throws ESException
  {
    return ESString.create("number");
  }

  public Class getJavaType()
  {
    if ((int) value == value)
      return int.class;
    else
      return double.class;
  }

  public ESBase getProperty(ESString key) throws Throwable
  {
    return Global.getGlobalProto().numProto.getProperty(key);
  }

  public ESString toStr()
  {
    int intValue = (int) value;

    if (intValue == value)
      return ESString.create(intValue);
    else
      return ESString.create(toString());
  }
  
  /**
   * Returns the string representation of the number.
   *
   * Notes: the spec says
   *   1) -0 should be printed at 0.
   *   2) 20 decimal digit integers should be printed as integers.
   *      This is insane since the double can only almost a 16 digit decimal.
   *   3) The exponent should be lower case.
   */
  public String toString()
  {
    int intValue = (int) value;

    if (intValue == value)
      return String.valueOf(intValue);
    else if ((long) value == value)
      return String.valueOf((long) value);
    else if (Double.isNaN(value))
      return "NaN";
    else if (Double.isInfinite(value))
      return (value < 0 ? "-Infinity" : "Infinity");

    return String.valueOf(value).toLowerCase();
  }

  public void print(WriteStream os) throws IOException
  {
    int intValue = (int) value;

    if (intValue == value)
      os.print(intValue);
    else if ((long) value == value)
      os.print((long) value);
    else if (Double.isNaN(value))
      os.print("NaN");
    else if (Double.isInfinite(value))
      os.print(value < 0 ? "-Infinity" : "Infinity");
    else
      os.print(value);
  }

  public int hashCode() 
  { 
    long bits = Double.doubleToLongBits(value);

    return (int) bits + 65517 * (int) (bits >> 32);
  }

  public boolean equals(Object b) 
  { 
    return (b instanceof ESNumber) && value == ((ESNumber) b).value;
  }

  public boolean ecmaEquals(ESBase b) throws Throwable
  {
    return b != esNull && value == b.toNum();
  }

  public boolean lessThan(ESBase b, boolean neg) throws Throwable
  {
    double db = b.toNum();

    if (Double.isNaN(value) || Double.isNaN(db))
      return false;
    else
      return (value < db) != neg;
  }

  public ESBase plus(ESBase b) throws Throwable
  { 
    if (b instanceof ESNumber)
      return create(value + ((ESNumber) b).value);
    else {
      ESBase primB = b.toPrimitive(NONE);

      if (primB instanceof ESString)
        return ESString.create(toString() + primB.toString());
      else
        return create(value + primB.toNum());
    }
  }

  /**
   * Save the external representation.
   */
  public void writeExternal(ObjectOutput os)
    throws IOException
  {
    os.writeDouble(value);
  }

  /**
   * Restore the external representation.
   */
  public void readExternal(ObjectInput is)
    throws IOException
  {
    value = is.readDouble();
  }
}
