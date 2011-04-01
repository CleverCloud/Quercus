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

package com.caucho.config.xml;

import com.caucho.config.ConfigException;
import com.caucho.config.annotation.NonEL;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ExprProgram;
import com.caucho.config.type.ConfigType;
import com.caucho.config.type.ExprType;
import com.caucho.config.type.TypeFactory;
import com.caucho.el.Expr;
import com.caucho.xml.QName;

/**
 * new arguments for an XML CanDI bean.
 * 
 * <code><pre>
 * &lt;mypkg:MyBean>
 *   &lt;new>single-argument&lt;/new>
 * &lt;/mypkg:MyBean>
 * </pre></code>
 * 
 * <code><pre>
 * &lt;mypkg:MyBean>
 *   &lt;new>
 *     &lt;value>arg1&lt;/value>
 *     &lt;mypkg:SubBean>arg2&lt;/mypkg:SubBean>
 *   &lt;/new>
 * &lt;/mypkg:MyBean>
 * </pre></code>
 */
@NonEL
public class XmlBeanNewConfig<T> {
  private XmlBeanConfig<T> _bean;
  
  public XmlBeanNewConfig(XmlBeanConfig<T> bean)
  {
    _bean = bean;
  }
  
  public void addText(ConfigProgram program)
  {
    _bean.addArg(program);
  }
  
  public void addArg(ConfigProgram program)
  {
    _bean.addArg(program);
  }

  public void addContentProgram(ConfigProgram program)
  {
    addArg(program);
  }

  public void addValue(ConfigProgram program)
  {
    addArg(program);
  }
}
