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
public class ESArrayWrapper {
  public static ESJavaWrapper wrapper(Global g, Class cl)
  {
    Class baseClass = cl.getComponentType();
    String name = baseClass.getName();

    if (name.equals("boolean"))
      return new BooleanArray(g);
    else if (name.equals("byte"))
      return new ByteArray(g);
    else if (name.equals("short"))
      return new ShortArray(g);
    else if (name.equals("char"))
      return new CharArray(g);
    else if (name.equals("int"))
      return new IntArray(g);
    else if (name.equals("long"))
      return new LongArray(g);
    else if (name.equals("float"))
      return new FloatArray(g);
    else if (name.equals("double"))
      return new DoubleArray(g);
    else
      return new ObjArray(g);
  }

  static class ObjArray extends ESArrayWrap {
    protected ObjArray() {}
    protected ESObject dup()
    {
      ObjArray dup = new ObjArray();
      dup.value = value;
      return dup;
    }

    protected int length() { return ((Object []) value).length; }

    public ESBase getProperty(int i)
      throws Throwable
    {
      Object []array = (Object []) value;

      if (i >= 0 && i < array.length)
        return Global.getGlobalProto().wrap(array[i]);
      else
        return esEmpty;
    }

    public void setProperty(int i, ESBase value) throws ESException
    {
      Object []array = (Object []) this.value;

      if (i >= 0 && i < array.length) {
        array[i] = value.toJavaObject();
      }
    }

    public ESBase delete(int i) throws ESException
    {
      Object []array = (Object []) this.value;

      if (i >= 0 && i < array.length) {
        array[i] = null;
        return ESBoolean.TRUE;
      }

      return ESBoolean.FALSE;
    }

    ObjArray(Global g)
    {
      super(g.arrayProto, null);
    }
  }

  static class BooleanArray extends ESArrayWrap {
    protected BooleanArray() {}
    protected ESObject dup()
    {
      BooleanArray dup = new BooleanArray();
      dup.value = value;
      return dup;
    }

    protected int length() { return ((boolean []) value).length; }

    public ESBase getProperty(int i)
    {
      boolean []array = (boolean []) value;

      if (i >= 0 && i < array.length)
        return ESBoolean.create(array[i]);
      else
        return esEmpty;
    }

    public void setProperty(int i, ESBase value) throws ESException
    {
      boolean []array = (boolean []) this.value;

      if (i >= 0 && i < array.length) {
        array[i] = value.toBoolean();
      }
    }

    public ESBase delete(int i) throws ESException
    {
      boolean []array = (boolean []) this.value;

      if (i >= 0 && i < array.length) {
        array[i] = false;
        return ESBoolean.TRUE;
      }

      return ESBoolean.FALSE;
    }

    BooleanArray(Global g)
    {
      super(g.arrayProto, null);
    }
  }

  static class ByteArray extends ESArrayWrap {
    protected ByteArray() {}
    protected ESObject dup()
    {
      ByteArray dup = new ByteArray();
      dup.value = value;
      return dup;
    }

    protected int length() { return ((byte []) value).length; }

    public ESBase getProperty(int i)
    {
      byte []array = (byte []) value;

      if (i >= 0 && i < array.length)
        return ESNumber.create(array[i]);
      else
        return esEmpty;
    }

    public void setProperty(int i, ESBase value) throws Throwable
    {
      byte []array = (byte []) this.value;

      if (i >= 0 && i < array.length) {
        array[i] = (byte) value.toInt32();
      }
    }

    public ESBase delete(int i) throws ESException
    {
      byte []array = (byte []) this.value;

      if (i >= 0 && i < array.length) {
        array[i] = 0;
        return ESBoolean.TRUE;
      }

      return ESBoolean.FALSE;
    }

    ByteArray(Global g)
    {
      super(g.arrayProto, null);
    }
  }

  static class ShortArray extends ESArrayWrap {
    protected ShortArray() {}
    protected ESObject dup()
    {
      ShortArray dup = new ShortArray();
      dup.value = value;
      return dup;
    }

    protected int length() { return ((short []) value).length; }

    public ESBase getProperty(int i)
    {
      short []array = (short []) value;

      if (i >= 0 && i < array.length)
        return ESNumber.create(array[i]);
      else
        return esEmpty;
    }

    public void setProperty(int i, ESBase value) throws Throwable
    {
      short []array = (short []) this.value;

      if (i >= 0 && i < array.length) {
        array[i] = (short) value.toInt32();
      }
    }

    public ESBase delete(int i) throws ESException
    {
      short []array = (short []) this.value;

      if (i >= 0 && i < array.length) {
        array[i] = 0;
        return ESBoolean.TRUE;
      }

      return ESBoolean.FALSE;
    }

    ShortArray(Global g)
    {
      super(g.arrayProto, null);
    }
  }

  static class CharArray extends ESArrayWrap {
    protected CharArray() {}
    protected ESObject dup()
    {
      CharArray dup = new CharArray();
      dup.value = value;
      return dup;
    }

    protected int length() { return ((char []) value).length; }

    public ESBase getProperty(int i)
    {
      char []array = (char []) value;

      if (i >= 0 && i < array.length)
        return ESString.create("" + array[i]);
      else
        return esEmpty;
    }

    public void setProperty(int i, ESBase value) throws Throwable
    {
      char []array = (char []) this.value;

      if (i >= 0 && i < array.length) {
        ESString str = value.toStr();

        array[i] = str.length() > 0 ? str.charAt(0) : 0;
      }
    }

    public ESBase delete(int i) throws ESException
    {
      char []array = (char []) this.value;

      if (i >= 0 && i < array.length) {
        array[i] = 0;
        return ESBoolean.TRUE;
      }

      return ESBoolean.FALSE;
    }

    CharArray(Global g)
    {
      super(g.arrayProto, null);
    }
  }

  static class IntArray extends ESArrayWrap {
    protected IntArray() {}
    protected ESObject dup()
    {
      IntArray dup = new IntArray();
      dup.value = value;
      return dup;
    }

    protected int length() { return ((int []) value).length; }

    public ESBase getProperty(int i)
    {
      int []array = (int []) value;

      if (i >= 0 && i < array.length)
        return ESNumber.create(array[i]);
      else
        return esEmpty;
    }

    public void setProperty(int i, ESBase value) throws Throwable
    {
      int []array = (int []) this.value;

      if (i >= 0 && i < array.length) {
        array[i] = (int) value.toInt32();
      }
    }

    public ESBase delete(int i) throws ESException
    {
      int []array = (int []) this.value;

      if (i >= 0 && i < array.length) {
        array[i] = 0;
        return ESBoolean.TRUE;
      }

      return ESBoolean.FALSE;
    }

    IntArray(Global g)
    {
      super(g.arrayProto, null);
    }
  }

  static class LongArray extends ESArrayWrap {
    protected LongArray() {}
    protected ESObject dup()
    {
      LongArray dup = new LongArray();
      dup.value = value;
      return dup;
    }

    protected int length() { return ((long []) value).length; }

    public ESBase getProperty(int i)
    {
      long []array = (long []) value;

      if (i >= 0 && i < array.length)
        return ESNumber.create(array[i]);
      else
        return esEmpty;
    }

    public void setProperty(int i, ESBase value) throws Throwable
    {
      long []array = (long []) this.value;

      if (i >= 0 && i < array.length) {
        array[i] = (long) value.toNum();
      }
    }

    public ESBase delete(int i) throws ESException
    {
      long []array = (long []) this.value;

      if (i >= 0 && i < array.length) {
        array[i] = 0;
        return ESBoolean.TRUE;
      }

      return ESBoolean.FALSE;
    }

    LongArray(Global g)
    {
      super(g.arrayProto, null);
    }
  }

  static class FloatArray extends ESArrayWrap {
    protected FloatArray() {}
    protected ESObject dup()
    {
      FloatArray dup = new FloatArray();
      dup.value = value;
      return dup;
    }

    protected int length() { return ((float []) value).length; }

    public ESBase getProperty(int i)
    {
      float []array = (float []) value;

      if (i >= 0 && i < array.length)
        return ESNumber.create(array[i]);
      else
        return esEmpty;
    }

    public void setProperty(int i, ESBase value) throws Throwable
    {
      float []array = (float []) this.value;

      if (i >= 0 && i < array.length) {
        array[i] = (float) value.toNum();
      }
    }

    public ESBase delete(int i) throws ESException
    {
      float []array = (float []) this.value;

      if (i >= 0 && i < array.length) {
        array[i] = 0;
        return ESBoolean.TRUE;
      }

      return ESBoolean.FALSE;
    }

    FloatArray(Global g)
    {
      super(g.arrayProto, null);
    }
  }

  static class DoubleArray extends ESArrayWrap {
    protected DoubleArray() {}
    protected ESObject dup()
    {
      DoubleArray dup = new DoubleArray();
      dup.value = value;
      return dup;
    }

    protected int length() { return ((double []) value).length; }

    public ESBase getProperty(int i)
    {
      double []array = (double []) value;

      if (i >= 0 && i < array.length)
        return ESNumber.create(array[i]);
      else
        return esEmpty;
    }

    public void setProperty(int i, ESBase value) throws Throwable
    {
      double []array = (double []) this.value;

      if (i >= 0 && i < array.length) {
        array[i] = (double) value.toNum();
      }
    }

    public ESBase delete(int i) throws ESException
    {
      double []array = (double []) this.value;

      if (i >= 0 && i < array.length) {
        array[i] = 0;
        return ESBoolean.TRUE;
      }

      return ESBoolean.FALSE;
    }

    DoubleArray(Global g)
    {
      super(g.arrayProto, null);
    }
  }
}
