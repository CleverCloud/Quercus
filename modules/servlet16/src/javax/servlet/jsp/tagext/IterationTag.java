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

package javax.servlet.jsp.tagext;

import javax.servlet.jsp.JspException;

/**
 * IterationTag lets tags loop over the body.
 *
 * <p>The generated code looks something like:
 *
 * <pre><code>
 * if (tag.doStartTag() == EVAL_BODY_INCLUDE) {
 *   do {
 *     ...
 *   } while (tag.doAfterBody() == EVAL_BODY_AGAIN);
 * }
 * if (tag.doEndTag() == SKIP_PAGE)
 *   return;
 * </code><pre>
 */
public interface IterationTag extends Tag {
  /**
   * Constant to reiterate the body.
   */
  public final static int EVAL_BODY_AGAIN = 2;
  /**
   * Tags call doAfterBody after processing the tag body.  By returning
   * EVAL_BODY_AGAIN, an iterator tag can repeat evaluation of the tag body.
   *
   * <p>empty tags and tags returning SKIP_BODY do not call
   * doAfterBody.
   *
   * @return EVAL_BODY_AGAIN to repeat the tag and SKIP_PAGE to stop.
   */
  public int doAfterBody() throws JspException;
}
