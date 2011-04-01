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

package com.caucho.config.bytecode;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import com.caucho.bytecode.CodeWriterAttribute;
import com.caucho.bytecode.ConstantPool;
import com.caucho.bytecode.JavaClass;
import com.caucho.bytecode.JavaClassLoader;
import com.caucho.bytecode.JavaField;
import com.caucho.bytecode.JavaMethod;
import com.caucho.config.ConfigException;
import com.caucho.config.inject.HandleAware;
import com.caucho.inject.Module;
import com.caucho.loader.ProxyClassLoader;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * interceptor generation
 */
@Module
public class SerializationAdapter<X> {
  private final Class<X> _cl;
  
  private Class<X> _proxyClass;

  private SerializationAdapter(Class<X> cl)
  {
    _cl = cl;
  }

  public static <X> Class<X> gen(Class<X> cl)
  {
    if (Modifier.isFinal(cl.getModifiers()))
      return cl;
    if (HandleAware.class.isAssignableFrom(cl))
      return cl;

    SerializationAdapter<X> gen = new SerializationAdapter<X>(cl);

    Class<X> proxyClass = gen.generateProxy();

    return proxyClass;
  }

  public static void setHandle(Object obj, Object handle)
  {
    if (obj instanceof HandleAware) {
      ((HandleAware) obj).setSerializationHandle(handle);
    }
    else {
      try {
        Class cl = obj.getClass();

        for (Field field : cl.getDeclaredFields()) {
          if (field.getName().equals("__caucho_handle")) {
            field.setAccessible(true);
            field.set(obj, handle);
          }
        }
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }
  }

  private Class generateProxy()
  {
    try {
      JavaClassLoader jLoader = new JavaClassLoader(_cl.getClassLoader());
      
      JavaClass jClass = new JavaClass(jLoader);
      jClass.setAccessFlags(Modifier.PUBLIC);
      ConstantPool cp = jClass.getConstantPool();

      jClass.setWrite(true);
      
      jClass.setMajor(49);
      jClass.setMinor(0);

      String superClassName = _cl.getName().replace('.', '/');
      String thisClassName = superClassName + "$BeanProxy";

      jClass.setSuperClass(superClassName);
      jClass.setThisClass(thisClassName);

      jClass.addInterface("java/io/Serializable");
      jClass.addInterface("com/caucho/config/inject/HandleAware");

      generateConstructors(jClass, superClassName);
      
      generateWriteReplace(jClass);
      generateSetHandle(jClass);

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      WriteStream out = Vfs.openWrite(bos);

      jClass.write(out);
    
      out.close();

      byte []buffer = bos.toByteArray();

      if (false) {
        String userName = System.getProperty("user.name");
        
        out = Vfs.lookup("file:/tmp/" + userName + "/qa/temp.class").openWrite();
        out.write(buffer, 0, buffer.length);
        out.close();
      }
      
      String cleanName = thisClassName.replace('/', '.');
      _proxyClass = (Class<X>) new ProxyClassLoader().loadClass(cleanName, buffer);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return _proxyClass;
  }

  private void generateConstructors(JavaClass jClass, String superClassName)
  {
    for (Constructor baseCtor : _cl.getDeclaredConstructors()) {
      if (Modifier.isPrivate(baseCtor.getModifiers()))
        continue;

      generateConstructor(jClass, superClassName, baseCtor);
    }
  }

  public static void generateConstructor(JavaClass jClass,
                                         String superClassName,
                                         Constructor baseCtor)
  {
    Class []types = baseCtor.getParameterTypes();

    StringBuilder sb = new StringBuilder();
    createDescriptor(sb, types);
    sb.append("V");

    String descriptor = sb.toString();
      
    JavaMethod ctor = jClass.createMethod("<init>", descriptor);
      
    ctor.setAccessFlags(Modifier.PUBLIC);
      
    CodeWriterAttribute code = ctor.createCodeWriter();
    code.setMaxLocals(5 + 2 * types.length);
    code.setMaxStack(5 + 2 * types.length);

    code.pushObjectVar(0);

    marshal(code, types);

    code.invokespecial(superClassName, "<init>", descriptor, 1, 0);
    code.addReturn();
    code.close();
  }

  private void generateWriteReplace(JavaClass jClass)
  {
    JavaField jField
      = jClass.createField("__caucho_handle", "Ljava/lang/Object;");
    jField.setAccessFlags(Modifier.PRIVATE);
    
    JavaMethod jMethod
      = jClass.createMethod("writeReplace", "()Ljava/lang/Object;");
    
    jMethod.setAccessFlags(Modifier.PRIVATE);
      
    CodeWriterAttribute code = jMethod.createCodeWriter();
    code.setMaxLocals(5);
    code.setMaxStack(5);

    code.pushObjectVar(0);
    code.getField(jClass.getThisClass(), "__caucho_handle",
                   "Ljava/lang/Object;");

    code.addObjectReturn();
    
    code.close();
  }

  private void generateSetHandle(JavaClass jClass)
  {
    /*
    JavaField jField
      = jClass.createField("__caucho_handle", "Ljava/lang/Object;");
    jField.setAccessFlags(Modifier.PRIVATE);
    */
    
    JavaMethod jMethod
      = jClass.createMethod("setSerializationHandle", "(Ljava/lang/Object;)V");
    
    jMethod.setAccessFlags(Modifier.PUBLIC);
      
    CodeWriterAttribute code = jMethod.createCodeWriter();
    code.setMaxLocals(5);
    code.setMaxStack(5);

    code.pushObjectVar(0);
    code.pushObjectVar(1);
    code.putField(jClass.getThisClass(), "__caucho_handle",
                  "Ljava/lang/Object;");

    code.addReturn();
    
    code.close();
  }

  public static void marshal(CodeWriterAttribute code, Class []param)
  {
    int stack = 1;
    int index = 1;
    
    for (int i = 0; i < param.length; i++) {
      Class type = param[i];
      
      if (boolean.class.equals(type)
          || byte.class.equals(type)
          || short.class.equals(type)
          || int.class.equals(type)) {
        code.pushIntVar(index);
        index += 1;
        stack += 1;
      }
      else if (long.class.equals(type)) {
        code.pushLongVar(index);
        index += 2;
        stack += 2;
      }
      else if (float.class.equals(type)) {
        code.pushFloatVar(index);
        index += 1;
        stack += 1;
      }
      else if (double.class.equals(type)) {
        code.pushDoubleVar(index);
        index += 2;
        stack += 2;
      }
      else {
        code.pushObjectVar(index);
        index += 1;
        stack += 1;
      }
    }
  }

  private int parameterCount(Class []parameters)
  {
    int count = 0;

    for (Class param : parameters) {
      if (long.class.equals(param) || double.class.equals(param))
        count += 2;
      else
        count += 1;
    }

    return count;
  }

  public static void createDescriptor(StringBuilder sb, Class []params)
  {
    sb.append("(");
    
    for (Class param : params) {
      sb.append(createDescriptor(param));
    }
    
    sb.append(")");
  }

  public static String createDescriptor(Class cl)
  {
    if (cl.isArray())
      return "[" + createDescriptor(cl.getComponentType());

    String primValue = _prim.get(cl);

    if (primValue != null)
      return primValue;

    return "L" + cl.getName().replace('.', '/') + ";";
  }

  private static HashMap<Class,String> _prim = new HashMap<Class,String>();
  private static HashMap<Class,String> _boxClass = new HashMap<Class,String>();

  static {
    _prim.put(boolean.class, "Z");
    _prim.put(byte.class, "B");
    _prim.put(char.class, "C");
    _prim.put(short.class, "S");
    _prim.put(int.class, "I");
    _prim.put(long.class, "J");
    _prim.put(float.class, "F");
    _prim.put(double.class, "D");
    _prim.put(void.class, "V");
    
    _boxClass.put(boolean.class, "java/lang/Boolean");
    _boxClass.put(byte.class, "java/lang/Byte");
    _boxClass.put(char.class, "java/lang/Character");
    _boxClass.put(short.class, "java/lang/Short");
    _boxClass.put(int.class, "java/lang/Integer");
    _boxClass.put(long.class, "java/lang/Long");
    _boxClass.put(float.class, "java/lang/Float");
    _boxClass.put(double.class, "java/lang/Double");
  }
}
