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

package javax.servlet.jsp.jstl.core;

import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.IterationTag;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;
import java.util.Collection;
import java.util.Map;
import java.util.Iterator;
import java.util.Enumeration;

abstract public class LoopTagSupport extends TagSupport
  implements IterationTag, TryCatchFinally, LoopTag {
  
  protected int begin;
  protected boolean beginSpecified;

  protected ValueExpression deferredExpression;
  
  protected int end = -1;
  protected boolean endSpecified;
  
  protected int step = 1;
  protected boolean stepSpecified;
  
  protected String itemId;
  protected String statusId;

  private Object _initialVar;
  private Object _current;
  private LoopTagStatus _status;
  private Object _oldStatus;
  private int _index;
  private int _count;
  private ValueExpression _mapped;

  /**
   * Sets the var attribute.
   */
  public void setVar(String id)
  {
    this.itemId = id;
  }

  /**
   * Sets the var status attribute.
   */
  public void setVarStatus(String id)
  {
    this.statusId = id;
  }

  /**
   * Checks that the begin property makes sense.
   */
  protected void validateBegin()
    throws JspTagException
  {
    if (begin < 0)
      throw new JspTagException("Invalid loop tag, 'begin' < 0.");
  }

  /**
   * Checks that the end property makes sense.
   */
  protected void validateEnd()
    throws JspTagException
  {
    if (this.end < 0)
      throw new JspTagException("Invalid loop tag, 'end' < 0.");
  }

  /**
   * Checks that the step property makes sense.
   */
  protected void validateStep()
    throws JspTagException
  {
    if (step <= 0)
      throw new JspTagException("Invalid loop tag, 'step' <= 0.");
  }

  /**
   * Called before iteration starts
   */
  protected abstract void prepare()
    throws JspTagException;

  /**
   * Returns the next object for the tag.
   */
  protected abstract Object next()
    throws JspTagException;
  
  /**
   * Returns true if there are more values in the tag.
   */
  protected abstract boolean hasNext()
    throws JspTagException;

  /**
   * Returns the current object.
   */
  public Object getCurrent()
  {
    return _current;
  }

  /**
   * Returns the loop status.
   */
  public LoopTagStatus getLoopStatus()
  {
    return _status;
  }

  private ValueExpression createIndexedExpression(int index)
    throws JspTagException
  {
    Object items = deferredExpression.getValue(this.pageContext.getELContext());

    if (items == null)
      return deferredExpression;
    else if (items instanceof Collection)
      return new IndexedValueExpression(deferredExpression, index);
    else if (items.getClass().isArray())
      return new IndexedValueExpression(deferredExpression, index);
    else if (items instanceof Map)
      return new IteratedValueExpression(
        new IteratedExpression(deferredExpression, null), index);
    else if (items instanceof Iterator)
      return new IteratedValueExpression(
        new IteratedExpression(deferredExpression, null), index);
    else if (items instanceof Enumeration)
      return new IteratedValueExpression(
        new IteratedExpression(deferredExpression, null), index);
    else if (items instanceof String)
      return new IteratedValueExpression(
        new IteratedExpression(deferredExpression, getDelims()), index);
    else
      throw new JspTagException("unknown items value '" + items + "'");
  }

  /**
   * Starts the iteration.
   */
  public int doStartTag()
    throws JspException
  {
    _index = 0;
    _count = 0;

    prepare();

    if (this.itemId != null)
      _initialVar = pageContext.getAttribute(itemId);

    if (getLoopStatus() == null)
      _status = new Status();

    while (_index < begin && hasNext()) {
      _index++;
      next();
    }

    if (hasNext() && (end == -1 || _index <= end)) {
      _count++;

      _current = next();

      if (itemId != null) {
        if (deferredExpression != null) {

          VariableMapper mapper = pageContext.getELContext()
            .getVariableMapper();

          _mapped = mapper.setVariable(itemId, createIndexedExpression(_index));
        }
        else
          pageContext.setAttribute(itemId, _current);
      }

      if (statusId != null) {
        _oldStatus = pageContext.getAttribute(statusId, PageContext.PAGE_SCOPE);

        if (! (_oldStatus instanceof LoopTagStatus))
          _oldStatus = null;

        pageContext.setAttribute(statusId, getLoopStatus());
      }

      return EVAL_BODY_INCLUDE;
    }
    else
      return SKIP_BODY;
  }

  public int doAfterBody() throws JspException
  {
    int stepCount;

    if (! stepSpecified)
      stepCount = 1;
    else
      stepCount = step;

    for (; stepCount > 0; stepCount--) {
      if (! hasNext()) {
        if (this.itemId != null)
          pageContext.setAttribute(itemId, _initialVar);

        return SKIP_BODY;
      }
        
      _index++;
      _current = next();
    }

    _count++;

    if (end == -1 || _index <= end) {
      if (itemId != null) {
        if (deferredExpression != null) {
          VariableMapper mapper = pageContext.getELContext().getVariableMapper();

          mapper.setVariable(itemId, createIndexedExpression(_index));
        }
        else
          pageContext.setAttribute(itemId, _current);
      }

      return EVAL_BODY_AGAIN;
    }
    else {
      return SKIP_BODY;
    }
  }
  
  public void doCatch(Throwable t) throws Throwable
  {
    throw t;
  }
  
  public void doFinally()
  {
    if (itemId != null) {
      if (deferredExpression != null) {
        VariableMapper mapper = pageContext.getELContext().getVariableMapper();

        mapper.setVariable(itemId, _mapped);
      }
      else
        pageContext.setAttribute(itemId, null);
    }
    
    if (statusId != null)
      pageContext.setAttribute(statusId, _oldStatus);

    _mapped = null;

    deferredExpression = null;
  }

  protected String getDelims()
  {
    return ",";
  }


  private class Status implements LoopTagStatus {
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
      return _index;
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
      try {
        if (! hasNext())
          return true;
        else if (endSpecified && step + _index > end)
          return true;
        else
          return false;
      } catch (Exception e) {
        return false;
      }
    }

    /**
     * Returns the begin index.
     */
    public Integer getBegin()
    {
      if (beginSpecified)
        return new Integer(begin);
      else
        return null;
    }
  
    /**
     * Returns the end index.
     */
    public Integer getEnd()
    {
      if (endSpecified)
        return new Integer(end);
      else
        return null;
    }
  
    /**
   * Returns the step index.
   */
    public Integer getStep()
    {
      if (stepSpecified)
        return new Integer(step);
      else
        return null;
    }
  }
}
