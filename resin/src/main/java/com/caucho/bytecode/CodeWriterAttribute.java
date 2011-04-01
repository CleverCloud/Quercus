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

package com.caucho.bytecode;

import java.io.*;

/**
 * Code generator attribute.
 */
public class CodeWriterAttribute extends CodeAttribute {
  private int _stack;
  private ByteArrayOutputStream _bos;

  public CodeWriterAttribute(JavaClass jClass)
  {
    setJavaClass(jClass);
    
    addUTF8("Code");

    _bos = new ByteArrayOutputStream();
  }

  public void cast(String className)
  {
    int index = addClass(className);

    write(CodeVisitor.CHECKCAST);
    write(index >> 8);
    write(index);
  }

  public void getField(String className, String fieldName, String sig)
  {
    int index = addFieldRef(className, fieldName, sig);

    write(CodeVisitor.GETFIELD);
    write(index >> 8);
    write(index);
  }

  public void putField(String className, String fieldName, String sig)
  {
    int index = addFieldRef(className, fieldName, sig);

    write(CodeVisitor.PUTFIELD);
    write(index >> 8);
    write(index);
  }

  public void getStatic(String className, String fieldName, String sig)
  {
    int index = addFieldRef(className, fieldName, sig);

    write(CodeVisitor.GETSTATIC);
    write(index >> 8);
    write(index);
  }

  public void putStatic(String className, String fieldName, String sig)
  {
    int index = addFieldRef(className, fieldName, sig);

    write(CodeVisitor.PUTSTATIC);
    write(index >> 8);
    write(index);
  }

  public void getArrayObject()
  {
    write(CodeVisitor.AALOAD);
  }

  public void setArrayObject()
  {
    write(CodeVisitor.AASTORE);
  }

  public void pushObjectVar(int index)
  {
    _stack++;
      
    if (index <= 3) {
      write(CodeVisitor.ALOAD_0 + index);
    }
    else {
      write(CodeVisitor.ALOAD);
      write(index);
    }
  }

  public void pushIntVar(int index)
  {
    _stack++;
      
    if (index <= 3) {
      write(CodeVisitor.ILOAD_0 + index);
    }
    else {
      write(CodeVisitor.ILOAD);
      write(index);
    }
  }

  public void pushLongVar(int index)
  {
    _stack += 2;
      
    if (index <= 3) {
      write(CodeVisitor.LLOAD_0 + index);
    }
    else {
      write(CodeVisitor.LLOAD);
      write(index);
    }
  }

  public void pushFloatVar(int index)
  {
    _stack += 1;
      
    if (index <= 3) {
      write(CodeVisitor.FLOAD_0 + index);
    }
    else {
      write(CodeVisitor.FLOAD);
      write(index);
    }
  }

  public void pushDoubleVar(int index)
  {
    _stack += 2;
      
    if (index <= 3) {
      write(CodeVisitor.DLOAD_0 + index);
    }
    else {
      write(CodeVisitor.DLOAD);
      write(index);
    }
  }

  public void pushNull()
  {
    _stack += 1;
      
    write(CodeVisitor.ACONST_NULL);
  }

  public void pushInt(int value)
  {
    _stack += 1;
      
    write(CodeVisitor.SIPUSH);
    write(value >> 8);
    write(value);
  }

  public void invoke(String className,
                     String methodName,
                     String signature,
                     int argStack,
                     int returnStack)
  {
    _stack += returnStack - argStack;

    int index = addMethodRef(className, methodName, signature);
    
    write(CodeVisitor.INVOKEVIRTUAL);
    write(index >> 8);
    write(index);
  }

  public void invokeInterface(String className,
                              String methodName,
                              String signature,
                              int argStack,
                              int returnStack)
  {
    _stack += returnStack - argStack;

    int index = addInterfaceMethodRef(className, methodName, signature);
    
    write(CodeVisitor.INVOKEINTERFACE);
    write(index >> 8);
    write(index);
    write(argStack);
    write(0);
  }

  public void newInstance(String className)
  {
    _stack += 1;

    int index = addClass(className);
    
    write(CodeVisitor.NEW);
    write(index >> 8);
    write(index);
  }

  public void newObjectArray(String className)
  {
    _stack += 1;

    int index = addClass(className);
    
    write(CodeVisitor.ANEWARRAY);
    write(index >> 8);
    write(index);
  }

  public void dup()
  {
    _stack += 1;
    
    write(CodeVisitor.DUP);
  }

  public void invokespecial(String className,
                            String methodName,
                            String signature,
                            int argStack,
                            int returnStack)
  {
    _stack += returnStack - argStack;

    int index = addMethodRef(className, methodName, signature);
    
    write(CodeVisitor.INVOKESPECIAL);
    write(index >> 8);
    write(index);
  }

  public void invokestatic(String className,
                           String methodName,
                           String signature,
                           int argStack,
                           int returnStack)
  {
    _stack += returnStack - argStack;

    int index = addMethodRef(className, methodName, signature);
    
    write(CodeVisitor.INVOKESTATIC);
    write(index >> 8);
    write(index);
  }
  
  public void addThrow()
  {
    write(CodeVisitor.ATHROW);
  }
  
  public void addReturn()
  {
    write(CodeVisitor.RETURN);
  }
  
  public void addIntReturn()
  {
    write(CodeVisitor.IRETURN);
  }

  public void addLongReturn()
  {
    write(CodeVisitor.LRETURN);
  }

  public void addFloatReturn()
  {
    write(CodeVisitor.FRETURN);
  }

  public void addDoubleReturn()
  {
    write(CodeVisitor.DRETURN);
  }

  public void addObjectReturn()
  {
    write(CodeVisitor.ARETURN);
  }

  public int addFieldRef(String className, String fieldName, String sig)
  {
    FieldRefConstant ref
      = getConstantPool().addFieldRef(className, fieldName, sig);

    return ref.getIndex();
  }

  public int addMethodRef(String className, String methodName, String sig)
  {
    MethodRefConstant ref
      = getConstantPool().addMethodRef(className, methodName, sig);

    return ref.getIndex();
  }

  public int addInterfaceMethodRef(String className, String methodName, String sig)
  {
    InterfaceMethodRefConstant ref
      = getConstantPool().addInterfaceRef(className, methodName, sig);

    return ref.getIndex();
  }

  public int addUTF8(String code)
  {
    Utf8Constant value = getConstantPool().addUTF8(code);

    return value.getIndex();
  }

  public int addClass(String className)
  {
    ClassConstant value = getConstantPool().addClass(className);

    return value.getIndex();
  }

  public ConstantPool getConstantPool()
  {
    return getJavaClass().getConstantPool();
  }

  private void write(int v)
  {
    _bos.write(v);
  }

  public void close()
  {
    if (_bos != null) {
      setCode(_bos.toByteArray());
      _bos = null;
    }
  }
}
