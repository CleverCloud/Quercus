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
import javax.servlet.jsp.PageContext;

/**
 * Tags are Java classes implementing JSP Tag extensions.  The class must
 * have a null argument public constructor and implement the tag attribute
 * as setXXX methods, following the Beans spec.
 *
 * <pre><code>
 * MyTag tag = new MyTag();
 *
 * tag.setPageContext(page);
 * tag.setParent(...);
 * tag.setFoo(...);
 * tag.setBar(...);
 * if (tag.doStartTag() == EVAL_BODY_INCLUDE) {
 *   ...
 * }
 * if (tag.doEndTag() == SKIP_PAGE)
 *   return;
 * ...
 * tag.setParent(...);
 * tag.setFoo(...);
 * if (tag.doStartTag() == EVAL_BODY_INCLUDE) {
 *   ...
 * }
 * if (tag.doEndTag() == SKIP_PAGE)
 *   return;
 * ...
 * tag.release();
 * </code><pre>
 */
public interface Tag extends JspTag {
  public final static int SKIP_BODY = 0;
  public final static int EVAL_BODY_INCLUDE = 1;
 
  public final static int SKIP_PAGE = 5;
  public final static int EVAL_PAGE = 6;

  /**
   * Sets the page context of this page.
   */
  public void setPageContext(PageContext page);
  /**
   * Sets the containing tag.
   */
  public void setParent(Tag t);
  /**
   * Returns the containing tag.
   */
  public Tag getParent();
  /**
   * Callback to handle the start of a tag.
   *
   * <p>doStartTag can assume <code>setPageContext</code>,
   * <code>setParent</code>, and all tag attribute properties have
   * been called.
   *
   * @return SKIP_BODY to ignore the body and EVAL_BODY_INCLUDE
   * to evaluate the body.
   */
  public int doStartTag() throws JspException;
  /**
   * Callback to handle the end of a tag.
   * @return SKIP_PAGE to skip the rest of the page and
   * EVAL_PAGE to continue with the rest of the page.
   */
  public int doEndTag() throws JspException;
  /**
   * Cleans up the tag at the end of the page.  The same tag instance
   * might be reused for multiple tags in the page.
   */
  public void release();
}
