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

package com.caucho.quercus.statement;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import java.util.ArrayList;

/**
 * Represents sequence of statements.
 */
public class BlockStatement extends Statement {
  protected Statement []_statements;

  public BlockStatement(Location location, Statement []statements)
  {
    super(location);

    _statements = statements;

    for (Statement stmt : _statements)
      stmt.setParent(this);
  }

  public BlockStatement(Location location, ArrayList<Statement> statementList)
  {
    super(location);

    _statements = new Statement[statementList.size()];
    statementList.toArray(_statements);

    for (Statement stmt : _statements)
      stmt.setParent(this);
  }

  public BlockStatement append(ArrayList<Statement> statementList)
  {
    Statement []statements
      = new Statement[_statements.length + statementList.size()];

    System.arraycopy(_statements, 0, statements, 0, _statements.length);

    for (int i = 0; i < statementList.size(); i++)
      statements[i + _statements.length] = statementList.get(i);

    return new BlockStatement(getLocation(), statements);
  }

  public Statement []getStatements()
  {
    return _statements;
  }

  /**
   * Returns true if the statement can fallthrough.
   */
  public int fallThrough()
  {
    for (int i = 0; i < getStatements().length; i++) {
      Statement stmt = getStatements()[i];

      int fallThrough = stmt.fallThrough();

      if (fallThrough != FALL_THROUGH)
        return fallThrough;
    }

    return FALL_THROUGH;
  }

  public Value execute(Env env)
  {
    for (int i = 0; i < _statements.length; i++) {
      Statement statement = _statements[i];

      Value value = statement.execute(env);

      if (value != null) {
        return value;
      }
    }

    return null;
  }
}

