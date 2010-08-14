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
import com.caucho.quercus.env.BreakValue;
import com.caucho.quercus.env.ContinueValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.Expr;

import java.util.ArrayList;

/**
 * Represents a switch statement.
 */
public class SwitchStatement extends Statement {
  protected final Expr _value;

  protected final Expr[][] _cases;
  protected final BlockStatement[] _blocks;

  protected final Statement _defaultBlock;
  protected final String _label;

  public SwitchStatement(Location location,
                         Expr value,
                         ArrayList<Expr[]> caseList,
                         ArrayList<BlockStatement> blockList,
                         Statement defaultBlock,
                         String label)
  {
    super(location);

    _value = value;

    _cases = new Expr[caseList.size()][];
    caseList.toArray(_cases);

    _blocks = new BlockStatement[blockList.size()];
    blockList.toArray(_blocks);

    _defaultBlock = defaultBlock;
    
    for (int i = 0; i < _blocks.length; i++) {
      _blocks[i].setParent(this);
    }
    
    if (_defaultBlock != null)
      _defaultBlock.setParent(this);
    
    _label = label;
  }

  /**
   * Executes the 'switch' statement, returning any value.
   */
  public Value execute(Env env)
  {
    try {
      Value testValue = _value.eval(env);

      int len = _cases.length;

      for (int i = 0; i < len; i++) {
        Expr []values = _cases[i];

        for (int j = 0; j < values.length; j++) {
          Value caseValue = values[j].eval(env);

          if (testValue.eq(caseValue)) {
            Value retValue = _blocks[i].execute(env);

            if (retValue instanceof BreakValue) {
              BreakValue breakValue = (BreakValue) retValue;
              
              int target = breakValue.getTarget();
              
              if (target > 1)
                return new BreakValue(target - 1);
              else
                return null;
            }
            else if (retValue instanceof ContinueValue) {
              ContinueValue conValue = (ContinueValue) retValue;
              
              int target = conValue.getTarget();
              
              if (target > 1)
                return new ContinueValue(target - 1);
              else
                return null;
            }
            else
              return retValue;
          }
        }
      }

      if (_defaultBlock != null) {
        Value retValue = _defaultBlock.execute(env);

        if (retValue instanceof BreakValue)
          return null;
        else
          return retValue;
      }

    }
    catch (RuntimeException e) {
      rethrow(e, RuntimeException.class);
    }

    return null;
  }

  /**
   * Returns true if control can go past the statement.
   */
  public int fallThrough()
  {
    return FALL_THROUGH;
    /* php/367t, php/367u
    if (_defaultBlock == null)
      return FALL_THROUGH;

    int fallThrough = _defaultBlock.fallThrough();

    for (int i = 0; i < _blocks.length; i++) {
      fallThrough &= _blocks[i].fallThrough();
    }

    if (fallThrough == BREAK_FALL_THROUGH)
      return 0;
    else
      return fallThrough;
    */
  }
}

