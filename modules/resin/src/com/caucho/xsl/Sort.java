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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.xsl;

import com.caucho.xpath.Env;
import com.caucho.xpath.Expr;
import com.caucho.xpath.XPath;
import com.caucho.xpath.XPathException;
import com.caucho.xpath.XPathParseException;

import org.w3c.dom.Node;

import java.util.Comparator;

public class Sort {
  public final static int NO_CASE_ORDER = 0;
  public final static int UPPER_FIRST = 1;
  public final static int LOWER_FIRST = 2;
  
  private Expr _expr;
  private boolean _isText;
  private Expr _isAscending;
  private Expr _caseOrder;
  
  private Expr _lang;

  Sort(Expr expr, Expr isAscending, boolean isText)
  {
    _expr = expr;
    _isAscending = isAscending;
    _isText = isText;
  }

  Sort(Expr expr, Expr isAscending, Expr lang)
  {
    _expr = expr;
    _isAscending = isAscending;
    _isText = true;
    
    _lang = lang;
  }

  public Sort(String expr, String isAscending, boolean isText)
    throws XPathParseException
  {
    _expr = XPath.parseExpr(expr);
    if (isAscending != null)
      _isAscending = XPath.parseExpr(isAscending);
    _isText = isText;
  }

  public Sort(String expr, String isAscending, String lang)
    throws XPathParseException
  {
    _expr = XPath.parseExpr(expr);
    
    if (isAscending != null)
      _isAscending = XPath.parseExpr(isAscending);

    _lang = XPath.parseExpr(lang);
    _isText = true;
  }

  public Sort(String expr, String isAscending, String lang, String caseOrder)
    throws XPathParseException
  {
    _expr = XPath.parseExpr(expr);
    
    if (isAscending != null)
      _isAscending = XPath.parseExpr(isAscending);

    if (lang != null)
      _lang = XPath.parseExpr(lang);
    
    _isText = true;

    if (caseOrder != null)
      _caseOrder = XPath.parseExpr(caseOrder);
  }

  public static Sort create(Expr expr, Expr isAscending, boolean isText)
  {
    return new Sort(expr, isAscending, isText);
  }

  public static Sort create(Expr expr, Expr isAscending, Expr lang)
  {
    return new Sort(expr, isAscending, lang);
  }

  public Expr getExpr()
  {
    return _expr;
  }

  public Expr getAscending()
  {
    return _isAscending;
  }

  /**
   * Sets the case order.
   */
  public void setCaseOrder(Expr caseOrder)
  {
    _caseOrder = caseOrder;
  }

  /**
   * Gets the case order.
   */
  public Expr getCaseOrder()
  {
    return _caseOrder;
  }

  public boolean isText()
  {
    return _isText;
  }

  public Expr getLang()
  {
    return _lang;
  }

  Object sortValue(Node child, Env env)
    throws XPathException
  {
    if (_isText)
      return _expr.evalString(child, env);
    else
      return new Double(_expr.evalNumber(child, env));
  }

  int cmp(Object a, Object b, Comparator comparator,
          boolean isAscending, int caseOrder)
  {
    if (comparator != null) {
      if (a == b)
        return 0;
      else if (a == null)
        return isAscending ? -1 : 1;
      else if (b == null)
        return isAscending ? 1 : -1;
      
      int cmp = comparator.compare(a, b);
      
      return isAscending ? cmp : -cmp;
    }
    else if (! _isText) {
      double da = ((Double) a).doubleValue();
      double db = ((Double) b).doubleValue();

      if (da == db)
        return 0;
      else if (da < db)
        return isAscending ? -1 : 1;
      else if (db < da)
        return isAscending ? 1 : -1;
      else if (! Double.isNaN(da))
        return isAscending ? -1 : 1;
      else if (! Double.isNaN(db))
        return isAscending ? 1 : -1;
      else
        return 0;
    }
    else {
      String va = (String) a;
      String vb = (String) b;

      if (va == vb)
        return 0;
      else if (va == null)
        return isAscending ? -1 : 1;
      else if (vb == null)
        return isAscending ? 1 : -1;

      int cmp = 0;

      if (caseOrder == NO_CASE_ORDER)
        cmp = va.compareTo(vb);
      else if (caseOrder == UPPER_FIRST) {
        int len = va.length();
        if (vb.length() < len)
          len = vb.length();

        int i = 0;
        for (; i < len; i++) {
          char chA = va.charAt(i);
          char chB = vb.charAt(i);

          if (chA == chB)
            continue;
          else if (Character.isUpperCase(chA) &&
                   Character.isLowerCase(chB)) {
            cmp = -1;
            break;
          }
          else if (Character.isLowerCase(chA) &&
                   Character.isUpperCase(chB)) {
            cmp = 1;
            break;
          }
          else {
            cmp = chA - chB;
            break;
          }
        }

        if (i < len) {
        }
        else if (va.length() == vb.length())
          cmp = 0;
        else if (va.length() < vb.length())
          cmp = -1;
        else
          cmp = 1;
      }
      else if (caseOrder == LOWER_FIRST) {
        int len = va.length();
        if (vb.length() < len)
          len = vb.length();

        int i = 0;
        for (; i < len; i++) {
          char chA = va.charAt(i);
          char chB = vb.charAt(i);

          if (chA == chB)
            continue;
          else if (Character.isUpperCase(chA) &&
                   Character.isLowerCase(chB)) {
            cmp = 1;
            break;
          }
          else if (Character.isLowerCase(chA) &&
                   Character.isUpperCase(chB)) {
            cmp = -1;
            break;
          }
          else {
            cmp = chA - chB;
            break;
          }
        }

        if (i < len) {
        }
        else if (va.length() == vb.length())
          cmp = 0;
        else if (va.length() < vb.length())
          cmp = -1;
        else
          cmp = 1;
      }

      return isAscending ? cmp : -cmp;
    }
  }
}
