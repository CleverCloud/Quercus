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

package com.caucho.servlets.ssi;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Represents a SSI string expression
 */
public class EqExpr extends SSIExpr {
  private final SSIExpr _left;
  private final SSIExpr _right;

  EqExpr(SSIExpr left, SSIExpr right)
  {
    _left = left;
    _right = right;
  }

  /**
   * Evaluate as a string.
   */
  @Override
  public String evalString(HttpServletRequest request,
                           HttpServletResponse response)
  {
    return String.valueOf(evalBoolean(request, response));
  }

  /**
   * Evaluate as a string.
   */
  @Override
  public boolean evalBoolean(HttpServletRequest request,
                           HttpServletResponse response)
  {
    String leftValue = _left.evalString(request, response);
    String rightValue = _right.evalString(request, response);

    return leftValue.equals(rightValue);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _left + "," + _right + "]";
  }
}
