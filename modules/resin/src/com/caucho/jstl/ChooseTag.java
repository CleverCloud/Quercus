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

package com.caucho.jstl;

import com.caucho.util.L10N;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Tag representing an "choose" condition.
 */
public class ChooseTag extends TagSupport {
  private static L10N L = new L10N(ChooseTag.class);

  private boolean isMatch;

  /**
   * Return true if a match has already occurred.
   */
  public boolean isMatch()
  {
    return isMatch;
  }

  /**
   * Set true if a match has already occurred.
   */
  public void setMatch()
  {
    isMatch = true;
  }

  /**
   * Process the tag.
   */
  public int doStartTag()
    throws JspException
  {
    isMatch = false;

    return EVAL_BODY_INCLUDE;
  }
}
