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

import com.caucho.jsp.PageContextImpl;
import com.caucho.util.L10N;
import com.caucho.xpath.Env;
import com.caucho.xpath.Expr;
import com.caucho.xpath.XPath;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.IterationTag;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.jstl.core.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Tag representing a "for each" condition.
 */
public class XmlForEachTag extends TagSupport implements IterationTag {
  private static L10N L = new L10N(XmlIfTag.class);
  
  private Expr _select;
  private String _var;
  private String _varStatus;
  private Node _oldEnv;

  private int _begin;
  private int _end = Integer.MAX_VALUE / 2;
  private int _step = 1;

  private int _count;

  private Iterator _iterator;
  private Object _current;

  /**
   * Sets the XPath select value.
   */
  public void setSelect(Expr select)
  {
    _select = select;
  }

  /**
   * Sets the variable which should contain the result of the test.
   */
  public void setVar(String var)
  {
    _var = var;
  }

  /**
   * Sets the variable which should contain the result of the test.
   */
  public void setVarStatus(String varStatus)
  {
    _varStatus = varStatus;
  }

  /**
   * Sets the begin value.
   */
  public void setBegin(int begin)
  {
    _begin = begin;
  }

  /**
   * Sets the end value.
   */
  public void setEnd(int end)
  {
    _end = end;
  }

  /**
   * Sets the step value.
   */
  public void setStep(int step)
  {
    _step = step;
  }

  /**
   * Process the tag.
   */
  public int doStartTag()
    throws JspException
  {
    try {
      PageContextImpl pageContext = (PageContextImpl) this.pageContext;
      Env env = XPath.createEnv();
      env.setVarEnv(pageContext.getVarEnv());
      
      _oldEnv = pageContext.getNodeEnv();
      Object obj = _select.evalObject(_oldEnv, env);

      env.free();

      if (obj == null)
        return SKIP_BODY;

      if (obj instanceof Iterator)
        _iterator = (Iterator) obj;
      else if (obj instanceof Collection)
        _iterator = ((Collection) obj).iterator();
      else if (obj instanceof NodeList) {
        NodeList nodeList = (NodeList) obj;
        ArrayList<Object> list = new ArrayList<Object>();

        for (int i = 0;  i < nodeList.getLength(); i++)
          list.add(nodeList.item(i));

        _iterator = list.iterator();
      }
      else {
        ArrayList<Object> list = new ArrayList<Object>();
        list.add(obj);

        _iterator = list.iterator();
      }

      _count = 0;

      while (_count < _begin && _iterator.hasNext()) {
        _iterator.next();
        _count++;
      }

      if (_end < _count || ! _iterator.hasNext())
        return SKIP_BODY;

      _current = _iterator.next();
      Object value = _current;
      _count++;

      if (_var != null)
        pageContext.setAttribute(_var, value);

      if (_varStatus != null)
        pageContext.setAttribute(_varStatus, new Status());

      if (value instanceof Node)
        pageContext.setNodeEnv((Node) value);

      return EVAL_BODY_INCLUDE;
    } catch (Exception e) {
      throw new JspException(e);
    }
  }

  /**
   * Process the loop.
   */
  public int doAfterBody()
    throws JspException
  {
    PageContextImpl pageContext = (PageContextImpl) this.pageContext;

    int step = _step;
    
    Object value = null;
    
    do {
      if (_end < _count || ! _iterator.hasNext()) {
        pageContext.setNodeEnv(_oldEnv);
        
        if (_var != null)
          pageContext.removeAttribute(_var);
        
        if (_varStatus != null)
          pageContext.removeAttribute(_varStatus);
        
        return SKIP_BODY;
      }

      value = _iterator.next();
      _count++;
    } while (--step > 0);
    
    _current = value;

    if (_var != null)
      pageContext.setAttribute(_var, value);
    if (value instanceof Node)
      pageContext.setNodeEnv((Node) value);

    return EVAL_BODY_AGAIN;
  }

  class Status implements LoopTagStatus {
    public Object getCurrent()
    {
      return _current;
    }
    
    public int getIndex()
    {
      return _count;
    }
    
    public int getCount()
    {
      return _count;
    }
    
    @Override
    public boolean isFirst()
    {
      return _count == _begin + 1;
    }
    
    @Override
    public boolean isLast()
    {
      return _count == _end || ! _iterator.hasNext();
    }
    
    public Integer getBegin()
    {
      return _begin;
    }
    
    public Integer getEnd()
    {
      return _end;
    }
    
    public Integer getStep()
    {
      return _step;
    }
    
    public String toString()
    {
      return "XmlForEachTag$Status[]";
    }
  }
}
