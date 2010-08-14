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

package com.caucho.jstl.rt;

import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.jsp.JspUtil;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.jstl.core.LoopTagSupport;
import javax.el.ELContext;
import javax.el.ValueExpression;
import java.util.Iterator;

public class CoreForTokensTag extends LoopTagSupport {
  private static L10N L = new L10N(CoreForTokensTag.class);

  protected String _items;
  protected String _delims;
  protected Iterator _iterator;

  /**
   * Sets the collection expression.
   */
  public void setItems(String items)
  {
    _items = items;
  }

  /**
   * Sets the delimiters expression.
   */
  public void setDelims(String delims)
  {
    _delims = delims;
  }

  @Override
  protected String getDelims()
  {
    return _delims;
  }

  protected ValueExpression createIndexedExpression(int index)
    throws JspTagException
  {
    return CoreForEachTag.getExpr(deferredExpression,
                                  index,
                                  _items,
                                  _delims);
  }

  /**
   * Sets the beginning value
   */
  public void setBegin(int begin)
  {
    this.begin = begin;
    this.beginSpecified = true;
  }

  /**
   * Sets the ending value
   */
  public void setEnd(int end)
  {
    this.end = end;
    this.endSpecified = true;
  }

  /**
   * Sets the step value
   */
  public void setStep(int step)
  {
    this.step = step;
    this.stepSpecified = true;
  }

  /**
   * Prepares the iterator.
   */
  public void prepare()
    throws JspTagException
  {
        // jsp/1ce6
    if (_items != null && _items.contains("#{")) {
      ELContext elContext = pageContext.getELContext();

      deferredExpression
        = JspUtil.createValueExpression(elContext, String.class, _items);

      _items = (String) deferredExpression.getValue(elContext);
    }

    _iterator = new TokenIterator(_items, _delims);
  }

  /**
   * Returns true if there are more items.
   */
  public boolean hasNext()
  {
    return _iterator.hasNext();
  }

  /**
   * Returns the next item
   */
  public Object next()
  {
    return _iterator.next();
  }

  public static class TokenIterator implements Iterator {
    private String _value;
    private char []_delims;
    private int _length;
    private int _i;
    private CharBuffer _cb = new CharBuffer();

    TokenIterator(String value, String delims)
    {
      if (value == null)
        value = "";
      _value = value;
      
      if (delims != null)
        _delims = delims.toCharArray();
      else
        _delims = new char[0];

      _length = value.length();

      char ch;
      final int startDelims = _delims.length - 1;

      for (_i = 0; _i < _length; _i++) {
        ch = _value.charAt(_i);

        boolean isDelim = false;
        for (int j = startDelims; j >= 0; j--) {
          if (_delims[j] == ch) {
            isDelim = true;

            break;
          }
        }

        if (! isDelim)
          break;
      }
    }
    
    public boolean hasNext()
    {
      return _i < _length;
    }
    
    public Object next()
    {
      _cb.clear();

      char ch;
      final int startDelims = _delims.length - 1;
      loop:
      for (; _i < _length; _i++) {
        ch = _value.charAt(_i);

        for (int j = startDelims; j >= 0; j--) {
          if (_delims[j] == ch)
            break loop;
        }
        
        _cb.append(ch);
      }
      
      for (_i++; _i < _length; _i++) {
        ch = _value.charAt(_i);

        boolean hasDelim = false;
        for (int j = startDelims; j >= 0; j--) {
          if (_delims[j] == ch) {
            hasDelim = true;
            break;
          }
        }

        if (! hasDelim)
          return _cb.toString();
      }

      return _cb.toString();
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
