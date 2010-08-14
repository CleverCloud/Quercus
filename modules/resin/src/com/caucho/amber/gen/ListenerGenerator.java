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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.gen;

import com.caucho.amber.type.ListenerType;
import com.caucho.java.AbstractGenerator;
import com.caucho.util.L10N;

import java.io.IOException;

//import com.caucho.amber.field.Field;
//import com.caucho.amber.field.FieldType;

/**
 * Generates the Java code for the wrapped object.
 */
public class ListenerGenerator extends AbstractGenerator {
  static final L10N L = new L10N(ListenerGenerator.class);

  private String _baseClassName;
  private String _extClassName;
  private ListenerType _listenerType;

  /**
   * Sets the bean info for the generator
   */
  public void setListenerType(ListenerType listenerType)
  {
    _listenerType = listenerType;
  }

  /**
   * Sets the base class name
   */
  public void setBaseClassName(String baseClassName)
  {
    _baseClassName = baseClassName;
  }

  /**
   * Gets the base class name
   */
  public String getBaseClassName()
  {
    return _baseClassName;
  }

  /**
   * Sets the ext class name
   */
  public void setExtClassName(String extClassName)
  {
    _extClassName = extClassName;

    setFullClassName(_extClassName);
  }

  /**
   * Get bean class name.
   */
  public String getBeanClassName()
  {
    return _baseClassName;
  }


  /**
   * Starts generation of the Java code
   */
  public void generateJava()
    throws IOException
  {
  }
}
