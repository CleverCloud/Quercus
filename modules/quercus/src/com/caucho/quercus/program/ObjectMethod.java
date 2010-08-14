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

package com.caucho.quercus.program;

import com.caucho.quercus.Location;
import com.caucho.quercus.expr.ExprFactory;
import com.caucho.quercus.statement.Statement;
import com.caucho.util.L10N;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Represents sequence of statements.
 */
public class ObjectMethod extends Function {
  private static final Logger log = Logger.getLogger(
    ObjectMethod.class.getName());
  private static final L10N L = new L10N(ObjectMethod.class);

  private ClassDef _quercusClass;

  ObjectMethod(Location location,
               InterpretedClassDef quercusClass,
               String name,
               FunctionInfo info,
               Arg []args,
               Statement []statements)
  {
    super(location, name, info, args, statements);

    _quercusClass = quercusClass;
  }

  public ObjectMethod(ExprFactory exprFactory,
                      Location location,
                      InterpretedClassDef quercusClass,
                      String name,
                      FunctionInfo info,
                      Arg []argList,
                      Statement []statementList)
  {
    super(exprFactory, location, name, info, argList, statementList);
    _quercusClass = quercusClass;
  }

  @Override
  public String getDeclaringClassName()
  {
    return _quercusClass.getName();
  }
  
  @Override
  public ClassDef getDeclaringClass()
  {
    return _quercusClass;
  }

  @Override
  public boolean isObjectMethod()
  {
    return true;
  }
}

