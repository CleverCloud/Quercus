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

package com.caucho.jsp;

import javax.servlet.jsp.jstl.core.LoopTag;
import javax.servlet.jsp.jstl.core.LoopTagStatus;
import javax.servlet.jsp.tagext.JspTag;
import javax.servlet.jsp.tagext.SimpleTag;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagAdapter;
import javax.servlet.jsp.tagext.TagSupport;

public class IntegerLoopSupportTag extends TagSupport
  implements LoopTag, LoopTagStatus {
  private JspTag _parent;
  
  private int _begin;
  private int _end;
  private int _step;

  private boolean _beginSpecified = false;
  private boolean _endSpecified = false;
  private boolean _stepSpecified = false;
  
  private int _index;
  private int _count;

  /**
   * Sets the parent as a JspTag
   */
  public void setParent(JspTag parent)
  {
    _parent = parent;
  }

  /**
   * Sets the parent as a JspTag
   */
  public void setParent(Tag parent)
  {
    _parent = parent;
  }

  /**
   * Returns the parent.
   */
  public Tag getParent()
  {
    if (_parent == null || _parent instanceof Tag)
      return (Tag) _parent;
    else
      return new TagAdapter((SimpleTag) _parent);
  }
  
  /**
   * Sets the initial values.
   */
  public void init(int begin,
                   int end,
                   int step,
                   boolean beginSpecified,
                   boolean endSpecified,
                   boolean stepSpecified)
  {
    _begin = begin;
    _end = end;
    _step = step;

    _beginSpecified = beginSpecified;
    _endSpecified = endSpecified;
    _stepSpecified = stepSpecified;

    _count = 0;
  }
  
  /**
   * Sets the current value.
   */
  public void setCurrent(int current)
  {
    _index = current;
    _count++;
  }
  
  /**
   * Returns the current value.
   */
  public Object getCurrent()
  {
    return new Integer(_index);
  }
  
  /**
   * Returns the loop status
   */
  public LoopTagStatus getLoopStatus()
  {
    return this;
  }

  public int getIndex()
  {
    return _index;
  }
  
  public int getCount()
  {
    return _count;
  }
  
  public boolean isFirst()
  {
    return _count == 1;
  }
  
  public boolean isLast()
  {
    return _end < _index + _step;
  }
  
  public Integer getBegin()
  {
    if (! _beginSpecified)
      return null;

    return new Integer(_begin);
  }
  
  public Integer getEnd()
  {
    if (! _endSpecified)
      return null;

    return new Integer(_end);
  }
  
  public Integer getStep()
  {
    if (! _stepSpecified)
      return null;

    return new Integer(_step);
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _begin + "," + _end + "]";
  }
}
