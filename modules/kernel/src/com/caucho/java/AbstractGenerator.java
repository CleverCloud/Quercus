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

package com.caucho.java;

import com.caucho.java.gen.DependencyComponent;
import com.caucho.java.gen.GenClass;
import com.caucho.loader.SimpleLoader;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.IOExceptionWrapper;
import com.caucho.vfs.Path;
import com.caucho.vfs.PersistentDependency;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Generates the Java code for the wrapped object.
 */
public abstract class AbstractGenerator {
  // The full name of the generated class
  private String _fullClassName;
  // The package of the generated class
  private String _packageName;
  // The class name of the generated class
  private String _className;
  
  // Parent class loader
  private ClassLoader _parentLoader;
  // class loader
  private ClassLoader _loader;
  
  // Write stream for generating the code
  private WriteStream _os;
  // The java writer
  protected JavaWriter _out;
  
  // The search path
  private Path _searchPath;

  // The work directory
  private Path _workPath;

  private GenClass _genClass;

  private String _initMethod = "_caucho_init";
  private String _isModifiedMethod = "_caucho_is_modified";
  
  /**
   * Sets the search path.
   */
  public void setSearchPath(Path path)
  {
    _searchPath = path;
  }

  public Path getSearchPath()
  {
    return _searchPath;
  }

  /**
   * Sets the full generated class.
   */
  public void setFullClassName(String fullClassName)
  {
    CharBuffer cb = CharBuffer.allocate();
    for (int i = 0; i < fullClassName.length(); i++) {
      char ch = fullClassName.charAt(i);

      if (ch == '.' || ch == '/')
        cb.append('.');
      else if (Character.isJavaIdentifierPart(ch))
        cb.append(ch);
      else
        cb.append('_');
    }
    
    _fullClassName = cb.close();

    int p = _fullClassName.lastIndexOf('.');
    if (p > 0) {
      _packageName = _fullClassName.substring(0, p);
      _className = _fullClassName.substring(p + 1);
    }
    else {
      _packageName = "";
      _className = _fullClassName;
    }
  }
  
  /**
   * Returns the full class name
   */
  public String getFullClassName()
  {
    if (_genClass != null)
      return _genClass.getFullClassName();
    else
      return _fullClassName;
  }
  
  /**
   * Returns the generated package name
   */
  public String getPackageName()
  {
    if (_genClass != null)
      return _genClass.getPackageName();
    else
      return _packageName;
  }
  
  /**
   * Returns the generated class name
   */
  public String getClassName()
  {
    if (_genClass != null)
      return _genClass.getClassName();
    else
      return _className;
  }

  /**
   * Sets the java class.
   */
  public void setGenClass(GenClass genClass)
  {
    _genClass = genClass;
  }

  /**
   * Gets the java class.
   */
  public GenClass getGenClass()
  {
    return _genClass;
  }
  
  /**
   * Sets the parent class loader.
   *
   * @param loader parent class loader
   */
  public void setParentLoader(ClassLoader loader)
  {
    _parentLoader = loader;
  }
  
  /**
   * Sets the class loader.
   *
   * @param loader parent class loader
   */
  public void setLoader(ClassLoader loader)
  {
    _loader = loader;
  }

  /**
   * Sets the parent class loader.
   *
   * @return the parent class loader
   */
  public ClassLoader getParentLoader()
  {
    if (_parentLoader == null)
      _parentLoader = Thread.currentThread().getContextClassLoader();
    
    return _parentLoader;
  }

  /**
   * Sets the work path for the generated class.
   */
  public void setClassDir(Path workPath)
  {
    _workPath = workPath;
    Thread.dumpStack();
  }
  
  /**
   * Returns the class dir for the generated class.
   */
  public Path getClassDir()
  {
    if (_workPath == null)
      return WorkDir.getLocalWorkDir();
    else
      return _workPath;
  }

  /**
   * Try to preload the class.
   *
   * @return true if the preloaded class is still valid.
   */
  public Class<?> preload()
    throws IOException
  {
    return loadClass(true);
  }

  /**
   * Call to generate the java source.
   */
  public void generate()
    throws Exception
  {
    String className = getFullClassName();
    String javaPathName = className.replace('.', '/') + ".java";
    String classPathName = className.replace('.', '/') + ".class";

    Path javaPath = getClassDir().lookup(javaPathName);
    Path classPath = getClassDir().lookup(classPathName);
    
    try {
      classPath.remove();
    } catch (IOException e) {
    }

    javaPath.getParent().mkdirs();

    _os = javaPath.openWrite();
    _out = new JavaWriter(_os);

    if (_genClass != null)
      _genClass.generate(_out);
    else
      generateJava();

    _os.close();
  }

  /**
   * Compiles the Java code
   */
  public Class compile()
    throws Exception
  {
    compileJava();
    
    return loadClass(false);
  }
  
  /**
   * Starts generation of the Java code
   */
  public void generateJava()
    throws Exception
  {
  }

  /**
   * Generates the class dependency code.
   *
   * @param depends list of Paths representing dependencies
   */
  protected void printDependList(ArrayList<PersistentDependency> depends)
    throws IOException
  {
    DependencyComponent depend = new DependencyComponent();
    depend.setSearchPath(_searchPath);

    for (int i = 0; i < depends.size(); i++)
      depend.addDependency(depends.get(i));

    depend.generate(getOut());
  }

  /**
   * Prints a method header.
   *
   * @param method the method to print
   */
  public void printMethodHeader(Method method)
    throws IOException
  {
    printMethodHeader(method.getName(), method.getParameterTypes(),
                      method.getReturnType(), method.getExceptionTypes());
  }

  /**
   * Prints a method header.
   *
   * @param name the method name to print
   * @param method the method to print
   */
  public void printMethodHeader(String name, Method method)
    throws IOException
  {
    printMethodHeader(name, method.getParameterTypes(),
                      method.getReturnType(), method.getExceptionTypes());
  }
  
  /**
   * Prints a method header.
   *
   * @param methodName the method name to print
   * @param param the method argument classes
   * @param returnType the return type of the method
   * @param exn array of exceptions thrown by the method
   */
  public void printMethodHeader(String methodName, Class []parameters,
                                Class returnType, Class []exn)
    throws IOException
  {
    println();
    print("public ");
    printClass(returnType);
    print(" ");
    print(methodName);
    print("(");

    for (int i = 0; i < parameters.length; i++) {
      if (i != 0)
        print(", ");

      printClass(parameters[i]);
      print(" a" + i);
    }
    println(")");

    if (exn != null && exn.length > 0) {
      print("  throws ");
      printClass(exn[0]);
      
      for (int i = 1; i < exn.length; i++) {
        print(", ");
        printClass(exn[i]);
      }
      println();
    }
  }
  
  /**
   * Prints the Java represention of the class
   */
  public void printClass(Class cl)
    throws IOException
  {
    if (! cl.isArray())
      print(cl.getName().replace('$',  '.'));
    else {
      printClass(cl.getComponentType());
      print("[]");
    }
  }
  
  /**
   * Compiles the class.
   */
  public void compileJava()
    throws IOException, ClassNotFoundException
  {
    JavaCompiler compiler = getCompiler();
    
    compiler.compile(getFullClassName().replace('.', '/') + ".java", null);
  }

  public JavaCompiler getCompiler()
  {
    JavaCompiler compiler = JavaCompiler.create(getParentLoader());

    compiler.setClassLoader(getParentLoader());
    compiler.setClassDir(getClassDir());

    return compiler;
  }

  /**
   * Loads the generated class.  If any class dependencies have
   * changed, return null.
   */
  public Class loadClass(boolean preload)
    throws IOException
  {
    return loadClass(getFullClassName(), preload);
  }

  /**
   * Loads the generated class.  If any class dependencies have
   * changed, return null.
   */
  public Class loadClass(String fullClassName, boolean preload)
    throws IOException
  {
    try {
      ClassLoader loader;

      if (! preload && _loader != null)
        loader = _loader;
      else {
        loader = SimpleLoader.create(getParentLoader(),
                                     getClassDir(),
                                     fullClassName);
      }

      Class cl = CauchoSystem.loadClass(fullClassName, false, loader);


      if (cl == null)
        return null;
      if (! preload)
        return cl;

      Method method = cl.getMethod(_initMethod, new Class[] { Path.class });
      method.invoke(null, new Object[] { getSearchPath() });

      method = cl.getMethod(_isModifiedMethod, new Class[0]);
      Boolean value = (Boolean) method.invoke(null, new Object[] {});

      if (value.booleanValue())
        return null;

      if (_loader != null)
        loader = _loader;
      else {
        loader = SimpleLoader.create(getParentLoader(),
                                     getClassDir(),
                                     fullClassName);
      }

      return CauchoSystem.loadClass(fullClassName, false, loader);
    } catch (Throwable e) {
      if (! preload)
        throw new IOExceptionWrapper(e);
      else
        return null;
    }
  }

  /**
   * Returns the java writer.
   */
  public JavaWriter getOut()
  {
    return _out;
  }

  /**
   * Pushes an indentation depth.
   */
  public void pushDepth()
    throws IOException
  {
    _out.pushDepth();
  }

  /**
   * Pops an indentation depth.
   */
  public void popDepth()
    throws IOException
  {
    _out.popDepth();
  }

  /**
   * Prints a character
   */
  public void print(int ch)
    throws IOException
  {
    _out.print(ch);
  }

  /**
   * Prints a character
   */
  public void print(char ch)
    throws IOException
  {
    _out.print(ch);
  }

  /**
   * Prints a string
   */
  public void print(String s)
    throws IOException
  {
    _out.print(s);
  }

  /**
   * Prints a string
   */
  public void printStr(String s)
    throws IOException
  {
    int len = s.length();
    for (int i = 0; i < len; i++) {
      char ch = s.charAt(i);

      switch (ch) {
      case '\\':
        _out.print("\\\\");
        break;
      case '\n':
        _out.print("\\n");
        break;
      case '\r':
        _out.print("\\r");
        break;
      case '"':
        _out.print("\\\"");
        break;
      default:
        _out.print(ch);
      }
    }
  }

  /**
   * Prints a new line.
   */
  public void println()
    throws IOException
  {
    _out.println();
  }

  /**
   * Prints a string with a new line
   */
  public void println(String s)
    throws IOException
  {
    _out.println(s);
  }
}
