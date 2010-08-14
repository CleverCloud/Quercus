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
import com.caucho.util.EnumIterator;
import com.caucho.util.L10N;
import com.caucho.util.NullIterator;

import javax.el.ELContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.jstl.core.LoopTag;
import javax.servlet.jsp.jstl.core.LoopTagStatus;
import javax.servlet.jsp.tagext.IterationTag;
import javax.servlet.jsp.tagext.TagSupport;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

public class ForEachTag extends TagSupport
  implements IterationTag, LoopTag, LoopTagStatus {
  private static L10N L = new L10N(ForEachTag.class);
  
  protected Expr _itemsExpr;
  
  protected Expr _beginExpr;
  protected Expr _endExpr;
  protected Expr _stepExpr;
  
  protected String _var;
  protected String _varStatus;

  // runtime values
  protected Iterator _iterator;
  
  protected int _begin;
  protected int _end;
  protected int _step;

  protected Object _initialVar;
  protected Object _current;
  protected int _index;
  protected int _count;

  /**
   * Sets the collection expression.
   */
  public void setItems(Expr items)
  {
    _itemsExpr = items;
  }

  /**
   * Sets the beginning index.
   */
  public void setBegin(Expr begin)
  {
    _beginExpr = begin;
  }

  /**
   * Sets the ending index.
   */
  public void setEnd(Expr end)
  {
    _endExpr = end;
  }

  /**
   * Sets the step index.
   */
  public void setStep(Expr step)
  {
    _stepExpr = step;
  }

  /**
   * Sets the iteration variable.
   */
  public void setVar(String var)
  {
    _var = var;
  }

  /**
   * Sets the status variable.
   */
  public void setVarStatus(String var)
  {
    _varStatus = var;
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
    
      Object items = null;

      if (_itemsExpr != null) {
        items = _itemsExpr.evalObject(env);

        _iterator = getIterator(items);

        while (_index < _begin && _iterator.hasNext()) {
          _index++;
          _iterator.next();
        }
      }
      else if (_beginExpr == null)
        throw new JspException(L.l("c:forEach must specify `items' or `begin'"));
      else if (_endExpr == null)
        throw new JspException(L.l("c:forEach must specify `items' or `begin'"));
      else {
        _iterator = new RangeIterator(_begin, _end);
        _end = -1;
      }

      if (_varStatus != null)
        pageContext.setAttribute(_varStatus, this);

      if (_var != null)
        _initialVar = pageContext.getAttribute(_var);

      return doAfterBody();
    } catch (Exception e) {
      throw new JspException(e);
    }
  }

  public int doAfterBody() throws JspException
  {
    if (_iterator == null) {
      if (_var != null)
        pageContext.setAttribute(_var, _initialVar);

      return SKIP_BODY;
    }
    else if (_iterator.hasNext()) {
      int stepCount;

      if (_step <= 0 || _count == 0)
        stepCount = 1;
      else
        stepCount = _step;

      for (; stepCount > 0; stepCount--) {
        if (! _iterator.hasNext()) {
          if (_var != null)
            pageContext.setAttribute(_var, _initialVar);

          return SKIP_BODY;
        }
        
        _index++;
        _current = _iterator.next();
      }

      _count++;

      if (_var != null)
        pageContext.setAttribute(_var, _current);

      if (_index - 1 <= _end || _end < 0)
        return EVAL_BODY_AGAIN;
      else {
        if (_var != null)
          pageContext.setAttribute(_var, _initialVar);

        return SKIP_BODY;
      }
    }
    else {
      if (_var != null)
        pageContext.setAttribute(_var, _initialVar);

      return SKIP_BODY;
    }
  }

  // LoopTag

  /**
   * Returns the status.
   */
  public LoopTagStatus getLoopStatus()
  {
    return this;
  }
    

  // LoopTagStatus

  /**
   * Returns the current object.
   */
  public Object getCurrent()
  {
    return _current;
  }

  /**
   * Returns the index.
   */
  public int getIndex()
  {
    return _index - 1;
  }

  /**
   * Returns the number of objects returned.
   */
  public int getCount()
  {
    return _count;
  }

  /**
   * Returns true if this is the first item.
   */
  public boolean isFirst()
  {
    return _count == 1;
  }

  /**
   * Returns true if this is the last item.
   */
  public boolean isLast()
  {
    if (_iterator == null || ! _iterator.hasNext())
      return true;
    else if (_step > 0 && _step + _index > _end)
      return true;
    else
      return false;
  }

  /**
   * Returns the begin index.
   */
  public Integer getBegin()
  {
    if (_beginExpr != null && _itemsExpr != null)
      return new Integer(_begin);
    else
      return null;
  }
  
  /**
   * Returns the end index.
   */
  public Integer getEnd()
  {
    if (_endExpr != null && _itemsExpr != null)
      return new Integer(_end);
    else
      return null;
  }
  
  /**
   * Returns the step index.
   */
  public Integer getStep()
  {
    if (_stepExpr != null)
      return new Integer(_step);
    else
      return null;
  }

  public static Iterator getIterator(Object items)
    throws JspTagException
  {
    if (items == null)
      return NullIterator.create();
    else if (items instanceof Map)
      return ((Map) items).entrySet().iterator();
    else if (items instanceof Collection)
      return ((Collection) items).iterator();
    else if (items.getClass().isArray())
      return new ArrayIterator(items);
    else if (items instanceof Iterator)
      return (Iterator) items;
    else if (items instanceof Enumeration)
      return new EnumIterator((Enumeration) items);
    else if (items instanceof String)
      return new StringIterator((String) items);
    else
      throw new JspTagException(L.l("unknown items value `{0}'", items));
  }

  public static class ArrayIterator implements Iterator {
    private Object _array;
    private int _index;
    private int _length;

    ArrayIterator(Object array)
    {
      _array = array;
      _length = Array.getLength(array);
    }
    
    public boolean hasNext()
    {
      return _index < _length;
    }
    
    public Object next()
    {
      if (_index < _length)
        return Array.get(_array, _index++);
      else
        return null;
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  public static class StringIterator implements Iterator {
    private String _value;
    private int _length;
    private int _i;

    StringIterator(String value)
    {
      _value = value;
      _length = value.length();

      for (; _i < _length; _i++)
        if (_value.charAt(_i) != ',')
          break;
    }
    
    public boolean hasNext()
    {
      return _i < _length;
    }
    
    public Object next()
    {
      char ch;
      int begin = _i;
      int tail = -1;

      for (; _i < _length; _i++) {
        ch =_value.charAt(_i);

        if (ch == ',') {
          if (tail == -1)
            tail = _i;
        } else {
          if (tail != -1)
            break;
        }
      }

      if (tail == -1)
        tail = _length;

      String value = _value.substring(begin, tail);

      return value;
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  public static class RangeIterator implements Iterator {
    private int _end;
    private int _i;

    RangeIterator(int begin, int end)
    {
      _i = begin;
      _end = end;
    }
    
    public boolean hasNext()
    {
      return _i <= _end;
    }
    
    public Object next()
    {
      if (_i <= _end)
        return new Integer(_i++);
      else
        return null;
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
