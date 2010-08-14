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

package com.caucho.java.gen;

import com.caucho.bytecode.JMethod;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;
import java.lang.reflect.*;

/**
 * Basic class generation.
 */
public class BaseClass extends ClassComponent {
  private static final L10N L = new L10N(BaseClass.class);

  private String _className;
  private String _superClassName;

  private boolean _isStatic;
  private String _visibility = "public";
  
  private ArrayList<String> _interfaceNames
    = new ArrayList<String>();

  private ArrayList<ClassComponent> _components
    = new ArrayList<ClassComponent>();

  private DependencyComponent _dependencyComponent;

  /**
   * Creates the base class
   */
  public BaseClass()
  {
  }

  /**
   * Creates the base class
   */
  public BaseClass(String className)
  {
    _className = className;
  }

  /**
   * Creates the base class
   */
  public BaseClass(String className, String superClassName)
  {
    _className = className;
    _superClassName = superClassName;
  }

  /**
   * Sets the class name.
   */
  public void setClassName(String className)
  {
    _className = className;
  }

  /**
   * Gets the class name.
   */
  public String getClassName()
  {
    return _className;
  }

  /**
   * Sets the superclass name.
   */
  public void setSuperClassName(String superClassName)
  {
    _superClassName = superClassName;
  }

  /**
   * Adds an interface.
   */
  public void addInterfaceName(String name)
  {
    if (! _interfaceNames.contains(name))
      _interfaceNames.add(name);
  }

  /**
   * Sets the class static property.
   */
  public void setStatic(boolean isStatic)
  {
    _isStatic = isStatic;
  }

  /**
   * Sets the class visibility property.
   */
  public void setVisibility(String visibility)
  {
    _visibility = visibility;
  }

  /**
   * Adds a method
   */
  public void addMethod(BaseMethod method)
  {
    addComponent(method);
  }

  /**
   * Creates the dependency component.
   */
  public DependencyComponent addDependencyComponent()
  {
    if (_dependencyComponent == null) {
      _dependencyComponent = new DependencyComponent();
      addComponent(_dependencyComponent);
    }

    return _dependencyComponent;
  }

  /**
   * Finds a method
   */
  public BaseMethod findMethod(Method method)
  {
    for (ClassComponent component : _components) {
      if (component instanceof BaseMethod) {
        BaseMethod baseMethod = (BaseMethod) component;

        if (baseMethod.getMethod().equals(method))
          return baseMethod;
      }
    }

    return null;
  }

  /**
   * Creates a method
   */
  public BaseMethod createMethod(Method method)
  {
    BaseMethod baseMethod = findMethod(method);

    if (baseMethod == null) {
      baseMethod = new BaseMethod(method, method);

      _components.add(baseMethod);
    }

    return baseMethod;
  }

  /**
   * Adds a class component.
   */
  public void addComponent(ClassComponent component)
  {
    _components.add(component);
  }
  
  /**
   * Generates the code for the class.
   *
   * @param out the writer to the output stream.
   */
  public void generate(JavaWriter out)
    throws IOException
  {
    if (_visibility != null && ! _visibility.equals(""))
      out.print(_visibility + " ");

    if (_isStatic)
      out.print("static ");

    out.print("class " + _className);
    
    if (_superClassName != null)
      out.print(" extends " + _superClassName);

    if (_interfaceNames.size() > 0) {
      out.print(" implements ");
      
      for (int i = 0; i < _interfaceNames.size(); i++) {
        if (i != 0)
          out.print(", ");

        out.print(_interfaceNames.get(i));
      }
    }

    out.println(" {");
    out.pushDepth();

    generateClassContent(out);
    
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the class content.
   */
  protected void generateClassContent(JavaWriter out)
    throws IOException
  {
    generateComponents(out);
  }

  /**
   * Generates the class components.
   */
  protected void generateComponents(JavaWriter out)
    throws IOException
  {
    for (int i = 0; i < _components.size(); i++) {
      if (i != 0)
        out.println();

      _components.get(i).generate(out);
    }
  }
}
