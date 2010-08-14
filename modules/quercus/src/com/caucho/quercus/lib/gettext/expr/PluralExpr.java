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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.gettext.expr;

import com.caucho.quercus.env.StringValue;

/**
 * Represents a gettext plural expression.
 */
public class PluralExpr
{
  private PluralExprParser _parser;

  private Expr _npluralsExpr;
  private Expr _pluralExpr;

  private PluralExpr(CharSequence expr)
  {
    _parser = new PluralExprParser(expr);
  }

  private void init()
  {
    if (_parser != null) {
      _npluralsExpr = _parser.getNpluralsExpr();
      _pluralExpr = _parser.getPluralExpr();
      _parser = null;
    }
  }

  /**
   * Returns a PluralExpr from the metadata.
   *
   * @param metadata contains the plural expression
   * @return PluralExpr
   */
  public static PluralExpr getPluralExpr(StringValue metaData)
  {
    String pluralForms = "Plural-Forms:";
    int i = metaData.indexOf(pluralForms);

    if (i < 0)
      return new PluralExpr("nplurals=2; plural=n!=1");

    i += pluralForms.length();
    int j = metaData.indexOf('\n', i);

    if (j < 0)
      return new PluralExpr(metaData.substring(i));
    else
      return new PluralExpr(metaData.substring(i, j));
  }

  /**
   * Returns evaluated plural expression
   *
   * @param expr
   * @param quantity number of items
   */
  public static int eval(CharSequence expr, int quantity)
  {
    return new PluralExpr(expr).eval(quantity);
  }

  /**
   * Evaluates this plural expression.
   */
  public int eval(int quantity)
  {
    init();

    return validate(quantity);
  }

  /**
   * Returns a valid plural form index.
   */
  private int validate(int quantity)
  {
    int pluralForm;
    int numOfPlurals;

    if (_pluralExpr == null)
      pluralForm = -1;
    else
      pluralForm = _pluralExpr.eval(quantity);

    if (_npluralsExpr == null)
      numOfPlurals = -1;
    else
      numOfPlurals = _npluralsExpr.eval(quantity);

    if (numOfPlurals < 1 || pluralForm < 0)
    {
      if (quantity == 1)
        return 0;
      else
        return 1;
    }

    // pluralForm is a 0-based index
    if (pluralForm >= numOfPlurals)
      return 0;

    return pluralForm;
  }
}
