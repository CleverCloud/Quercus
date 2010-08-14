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

package com.caucho.jsp;

import com.caucho.util.L10N;
import com.caucho.bytecode.JavaClass;

import java.util.logging.Logger;

/**
 * Stores analyzed information about a tag.
 */
public class AnalyzedTag {
  private static final Logger log
    = Logger.getLogger(AnalyzedTag.class.getName());
  static final L10N L = new L10N(AnalyzedTag.class);

  private AnalyzedTag _parent;
  private JavaClass _javaClass;

  private boolean _isBodyTag;
  
  private boolean _doStart;
  private boolean _startReturnsSkip;
  private boolean _startReturnsInclude;
  private boolean _startReturnsBuffered;
  
  private boolean _doEnd;
  private boolean _endReturnsSkip;
  private boolean _endReturnsEval;

  private boolean _doAfter;
  private boolean _afterReturnsAgain;
  
  private boolean _doInit;

  private boolean _doCatch;
  private boolean _doFinally;

  private boolean _hasInjection;

  public AnalyzedTag getParent()
  {
    return _parent;
  }

  public void setParent(AnalyzedTag parent)
  {
    _parent = parent;
  }

  /**
   * Set true for a body tag.
   */
  public void setBodyTag(boolean isBodyTag)
  {
    _isBodyTag = isBodyTag;
  }

  /**
   * Set true for a body tag.
   */
  public boolean isBodyTag()
  {
    return _isBodyTag;
  }

  /**
   * Set true if the tag implements doStart.
   */
  public boolean getDoStart()
  {
    return _doStart;
  }

  /**
   * Set true if the tag implements doStart.
   */
  public void setDoStart(boolean doStart)
  {
    _doStart = doStart;
  }

  /**
   * Set true if the doStart can return SKIP_BODY
   */
  public boolean getStartReturnsSkip()
  {
    return _startReturnsSkip;
  }

  /**
   * Set true if the doStart can return SKIP_BODY
   */
  public void setStartReturnsSkip(boolean skip)
  {
    _startReturnsSkip = skip;
  }

  /**
   * Set true if the doStart can return INCLUDE_BODY
   */
  public boolean getStartReturnsInclude()
  {
    return _startReturnsInclude;
  }

  /**
   * Set true if the doStart can return INCLUDE_BODY
   */
  public void setStartReturnsInclude(boolean include)
  {
    _startReturnsInclude = include;
  }

  /**
   * Set true if the doStart can return INCLUDE_BODY_BUFFERED
   */
  public boolean getStartReturnsBuffered()
  {
    return _isBodyTag && _startReturnsBuffered;
  }

  /**
   * Set true if the doStart can return INCLUDE_BODY_BUFFERED
   */
  public boolean getStartReturnsBufferedAsParent()
  {
    return _startReturnsBuffered;
  }

  /**
   * Set true if the doStart can return INCLUDE_BODY_BUFFERED
   */
  public void setStartReturnsBuffered(boolean buffered)
  {
    _startReturnsBuffered = buffered;
  }

  /**
   * Set true if the tag implements doEndTag.
   */
  public boolean getDoEnd()
  {
    if (_doEnd)
      return true;

    int count = 0;
    count += (getEndReturnsSkip() ? 1 : 0);
    count += (getEndReturnsEval() ? 1 : 0);

    return count > 1;
  }

  /**
   * Set true if the tag implements doEndTag.
   */
  public void setDoEnd(boolean doEnd)
  {
    _doEnd = doEnd;
  }

  /**
   * Set true if the doEndTag can return SKIP_PAGE
   */
  public boolean getEndReturnsSkip()
  {
    return _endReturnsSkip;
  }

  /**
   * Set true if the doEndTag can return SKIP_PAGE
   */
  public void setEndReturnsSkip(boolean skip)
  {
    _endReturnsSkip = skip;
  }

  /**
   * Set true if the doEndTag can return EVAL_PAGE
   */
  public boolean getEndReturnsEval()
  {
    return _endReturnsEval;
  }

  /**
   * Set true if the doEndTag can return EVAL_PAGE
   */
  public void setEndReturnsEval(boolean eval)
  {
    _endReturnsEval = eval;
  }

  /**
   * Set true if the tag implements doAfterBody
   */
  public boolean getDoAfter()
  {
    return _doAfter;
  }

  /**
   * Set true if the tag implements doAfterBody
   */
  public void setDoAfter(boolean doAfter)
  {
    _doAfter = doAfter;
  }

  /**
   * Set true if the doAfterBody can return EVAL_BODY_AGAIN
   */
  public boolean getAfterReturnsAgain()
  {
    return _afterReturnsAgain;
  }

  /**
   * Set true if the doAfterBody can return EVAL_BODY_AGAIN
   */
  public void setAfterReturnsAgain(boolean again)
  {
    _afterReturnsAgain = again;
  }

  /**
   * Set true if the tag implements doInitBody.
   */
  public boolean getDoInit()
  {
    return _doInit;
  }

  /**
   * Set true if the tag implements doInitBody.
   */
  public void setDoInit(boolean doInit)
  {
    _doInit = doInit;
  }

  /**
   * Set true if the tag implements doCatch
   */
  public boolean getDoCatch()
  {
    return _doCatch;
  }

  /**
   * Set true if the tag implements doCatch
   */
  public void setDoCatch(boolean doCatch)
  {
    _doCatch = doCatch;
  }

  /**
   * Set true if the tag implements doFinally
   */
  public boolean getDoFinally()
  {
    return _doFinally;
  }

  /**
   * Set true if the tag implements doFinally
   */
  public void setDoFinally(boolean doFinally)
  {
    _doFinally = doFinally;
  }

  /**
   * True if the tag has a @Resource.
   */
  public boolean getHasInjection()
  {
    return _hasInjection;
  }

  /**
   * True if the tag has a @Resource.
   */
  public void setHasInjection(boolean hasInjection)
  {
    _hasInjection = hasInjection;
  }

  public JavaClass getJavaClass()
  {
    return _javaClass;
  }

  public void setJavaClass(JavaClass javaClass)
  {
    _javaClass = javaClass;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _javaClass + "]";
  }
}
