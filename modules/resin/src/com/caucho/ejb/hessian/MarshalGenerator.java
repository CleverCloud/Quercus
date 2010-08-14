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

package com.caucho.ejb.hessian;

import com.caucho.java.AbstractGenerator;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.util.CharBuffer;
import com.caucho.util.IntMap;
import com.caucho.vfs.Path;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Base class for generating code to marshal and unmarshal sml-rpc calls.
 */
abstract class MarshalGenerator extends AbstractGenerator {
  // Classes which can be safely passed by reference because they're
  // immutable and part of the JDK
  static IntMap immutableClasses;
  
  protected static Class readerClass;
  protected static Class inputStreamClass;
  protected static Class remoteClass;
  protected static Class nodeClass;
  
  static {
    readerClass = java.io.Reader.class;
    inputStreamClass = java.io.InputStream.class;
    remoteClass = java.rmi.Remote.class;
    nodeClass = org.w3c.dom.Node.class;
  }
  
  protected Class _cl;
  String objClass;
  String fullName;
  String pkg;
  String className;

  protected int unique;
  
  protected ArrayList marshallClasses;
  protected ArrayList unmarshallClasses;
  
  protected ArrayList marshallArrays;
  protected ArrayList unmarshallArrays;

  /**
   * Initialize the generated classname.
   *
   * @param beanClass the bean which needs the stub/skeleton
   *
   * @return the path to the file to be generated
   */
  Path initClassNames(Class beanClass, String suffix)
    throws Exception
  {
    ClassLoader parentLoader = getParentLoader();
    
    if (parentLoader instanceof DynamicClassLoader) {
      DynamicClassLoader dcl = (DynamicClassLoader) parentLoader;

      dcl.make();
    }

    Path workPath = getClassDir();

    _cl = beanClass;

    fullName = _cl.getName() + suffix;

    objClass = _cl.getName();
    int p = objClass.lastIndexOf('.');
    if (p > 0)
      objClass = objClass.substring(p + 1);
    
    p = fullName.lastIndexOf('.');
    if (p > 0) {
      pkg = fullName.substring(0, p);
      className = fullName.substring(p + 1);
    }
    else
      className = fullName;

    Path path = workPath.lookup(fullName.replace('.', '/') + ".java");
    path.getParent().mkdirs();

    return path;
  }

  /**
   * Creates a unique mangled method name based on the method name and
   * the method parameters.
   *
   * @param name the base method name
   * @param method the method to mangle
   * @param isFull if true, mangle the full classname
   *
   * @return a mangled string.
   */
  protected String mangleMethodName(String name, Method method, boolean isFull)
  {
    CharBuffer cb = CharBuffer.allocate();
    
    cb.append(name);
    
    Class []params = method.getParameterTypes();
    for (int i = 0; i < params.length; i++) {
      cb.append('_');
      mangleClass(cb, params[i], isFull);
    }

    return cb.close();
  }

  /**
   * Creates a unique mangled method name based on the method name and
   * the method parameters.
   *
   * @param name the base method name
   * @param method the method to mangle
   * @param isFull if true, mangle the full classname
   *
   * @return a mangled string.
   */
  protected String mangleMethodName(String name, Class []param, boolean isFull)
  {
    CharBuffer cb = CharBuffer.allocate();
    
    cb.append(name);
    
    for (int i = 0; i < param.length; i++) {
      cb.append('_');
      mangleClass(cb, param[i], isFull);
    }

    return cb.close();
  }

  /**
   * Mangles a classname.
   */
  private void mangleClass(CharBuffer cb, Class cl, boolean isFull)
  {
    String name = cl.getName();

    if (name.equals("boolean"))
      cb.append("boolean");
    else if (name.equals("int") ||
             name.equals("short") ||
             name.equals("byte"))
      cb.append("int");
    else if (name.equals("long"))
      cb.append("long");
    else if (name.equals("double") || name.equals("float"))
      cb.append("double");
    else if (name.equals("java.lang.String") ||
             name.equals("com.caucho.util.CharBuffer") ||
             name.equals("char") ||
             name.equals("java.io.Reader"))
      cb.append("string");
    else if (name.equals("java.util.Date") ||
             name.equals("com.caucho.util.QDate"))
      cb.append("date");
    else if (inputStreamClass.isAssignableFrom(cl) ||
             name.equals("[B"))
      cb.append("binary");
    else if (cl.isArray()) {
      cb.append("[");
      mangleClass(cb, cl.getComponentType(), isFull);
    }
    else if (name.equals("org.w3c.dom.Node") ||
             name.equals("org.w3c.dom.Element") ||
             name.equals("org.w3c.dom.Document"))
      cb.append("xml");
    else if (isFull)
      cb.append(name);
    else {
      int p = name.lastIndexOf('.');
      if (p > 0)
        cb.append(name.substring(p + 1));
      else
        cb.append(name);
    }
  }

  /**
   * Prints a method declaration with a given name.
   *
   * @param name the name to use for the generated method
   * @param method the method to override
   */
  protected void printMethodDeclaration(String name, Method method)
    throws IOException
  {
    Class ret = method.getReturnType();
    Class []params = method.getParameterTypes();

    Class []exns = method.getExceptionTypes();

    println();
    print("public ");
    printClass(ret);
    print(" " + name + "(");
    for (int i = 0; i < params.length; i++) {
      if (i != 0)
        print(", ");
      printClass(params[i]);
      print(" _arg" + i);
    }
    println(")");
    if (exns.length > 0)
      print("  throws ");
    for (int i = 0; i < exns.length; i++) {
      if (i != 0)
        print(", ");
      printClass(exns[i]);
    }
    if (exns.length > 0)
      println();
  }

  /**
   * Unmarshal the reading of a single variable, knowing the result
   * type.
   *
   * @param var the generated java variable which will receive the data.
   * @param cl the class of the target variable.
   */
  protected void printUnmarshalType(Class cl)
    throws IOException
  {
    String name = cl.getName();

    if (cl.equals(boolean.class)) {
      println("in.readBoolean();");
    }
    else if (cl.equals(int.class)) {
      println("in.readInt();");
    }
    else if (cl.equals(short.class) ||
             cl.equals(char.class) ||
             cl.equals(byte.class)) {
      println("(" + name + ") in.readInt();");
    }
    else if (cl.equals(long.class)) {
      println("in.readLong();");
    }
    else if (cl.equals(double.class)) {
      println("in.readDouble();");
    }
    else if (cl.equals(float.class)) {
      println("(" + name + ") in.readDouble();");
    }
    else if (cl.equals(String.class))
      println("in.readString();");
    else if (cl.equals(java.util.Date.class)) {
      println("new java.util.Date(in.readUTCDate());");
    }
    else if (cl.equals(byte[].class)) {
      println("in.readBytes();");
    }
    else if (org.w3c.dom.Node.class.isAssignableFrom(cl)) {
      println("in.readNode();");
    }
    else if (cl.equals(java.lang.Object.class)) {
      println("in.readObject();");
    }
    else {
      print("(");
      printClass(cl);
      print(") in.readObject(");
      printClass(cl);
      println(".class);");
    }
  }

  protected void printMarshalType(Class cl, String var)
    throws IOException
  {
    String name = cl.getName();

    if (cl.equals(void.class)) {
      println("out.writeNull();");
    }
    else if (cl.equals(boolean.class)) {
      println("out.writeBoolean(" + var + ");");
    }
    else if (cl.equals(int.class) ||
             cl.equals(short.class) ||
             cl.equals(char.class) ||
             cl.equals(byte.class)) {
      println("out.writeInt(" + var + ");");
    }
    else if (cl.equals(long.class)) {
      println("out.writeLong(" + var + ");");
    }
    else if (cl.equals(double.class) || cl.equals(float.class)) {
      println("out.writeDouble(" + var + ");");
    }
    else if (cl.equals(String.class)) {
      println("out.writeString(" + var + ");");
    }
    else if (cl.equals(java.util.Date.class)) {
      println("out.writeUTCDate(" + var + " == null ? 0 : " + var + ".getTime());");
    }
    else if (org.w3c.dom.Node.class.isAssignableFrom(cl)) {
      println("out.writeXml(" + var + ");");
    }
    else if (cl.equals(byte[].class)) {
      println("out.writeBytes(" + var + ");");
    }
    else {
      println("out.writeObject(" + var + ");");
    }
  }

  /**
   * Returns true if the class needs serialization.
   *
   * @param cl the class to test.
   *
   * @return true if the class needs serialization
   */
  boolean needsSerialization(Class cl)
  {
    if (cl.isPrimitive())
      return false;
    else
      return immutableClasses.get(cl) < 0;
  }

  public void printClass(Class cl)
    throws IOException
  {
    if (! cl.isArray())
      print(cl.getName());
    else {
      printClass(cl.getComponentType());
      print("[]");
    }
  }

  protected void printNewArray(Class cl)
    throws IOException
  {
    if (! cl.isArray()) {
      print(cl.getName());
      print("[length]");
    }
    else {
      printNewArray(cl.getComponentType());
      print("[]");
    }
  }

  static {
    immutableClasses = new IntMap();
    immutableClasses.put(String.class, 1);
    immutableClasses.put(Byte.class, 1);
    immutableClasses.put(Character.class, 1);
    immutableClasses.put(Short.class, 1);
    immutableClasses.put(Integer.class, 1);
    immutableClasses.put(Long.class, 1);
    immutableClasses.put(Float.class, 1);
    immutableClasses.put(Double.class, 1);
    immutableClasses.put(Class.class, 1);
  }
}
