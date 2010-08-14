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

package com.caucho.jstl.el;

import com.caucho.el.Expr;
import com.caucho.jsp.PageContextImpl;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.el.ELContext;
import javax.servlet.jsp.JspException;
import java.util.Iterator;

public class ForTokensTag extends ForEachTag {
  private static L10N L = new L10N(ForTokensTag.class);
  
  private Expr _delimsExpr;

  /**
   * Sets the delimiter expression.
   */
  public void setDelims(Expr delims)
  {
    _delimsExpr = delims;
  }

  /**
   * Process the tag.
   */
  public int doStartTag()
    throws JspException
  {
    try {
      _iterator = null;
      _index = 0;
      _count = 0;

      PageContextImpl pageContext = (PageContextImpl) this.pageContext;
      ELContext env = pageContext.getELContext();

      if (_beginExpr != null)
        _begin = (int) _beginExpr.evalLong(env);
      else
        _begin = -1;

      if (_endExpr != null)
        _end = (int) _endExpr.evalLong(env);
      else
        _end = Integer.MAX_VALUE;

      if (_stepExpr != null)
        _step = (int) _stepExpr.evalLong(env);
      else
        _step = 0;
    
      String items = _itemsExpr.evalString(env);
      String delims = _delimsExpr.evalString(env);

      _iterator = new TokenIterator(items, delims);

      while (_index < _begin && _iterator.hasNext()) {
        _index++;
        _iterator.next();
      }

      if (_varStatus != null)
        pageContext.setAttribute(_varStatus, this);

      return doAfterBody();
    } catch (Exception e) {
      throw new JspException(e);
    }
  }

  public class TokenIterator implements Iterator {
    private String value;
    private char []delims;
    private int length;
    private int i;
    private CharBuffer cb = new CharBuffer();

    TokenIterator(String value, String delims)
    {
      this.value = value;
      this.delims = delims.toCharArray();
      this.length = value.length();
    }
    
    public boolean hasNext()
    {
      return i < length;
    }
    
    public Object next()
    {
      cb.clear();

      char ch = 0;
      int startDelims = delims.length - 1;
      loop:
      for (; i < length; i++) {
        ch = value.charAt(i);
        
        for (int j = startDelims; j >= 0; j--) {
          if (delims[j] == ch)
            break loop;
        }
        
        cb.append(ch);
      }

      i++;

      return cb.toString();
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
