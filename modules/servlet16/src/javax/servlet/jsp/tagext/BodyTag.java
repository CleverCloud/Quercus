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
 *
 * $Id: BodyTag.java,v 1.2 2004/09/29 00:12:48 cvs Exp $
 */

package javax.servlet.jsp.tagext;

import javax.servlet.jsp.JspException;

/**
 * BodyTag lets tags access the generated tag contents and it allows
 * tag looping.  For example, a SQL tag may use the enclosed SQL to
 * update a table.  A BodyTag must explicitly write the contents the
 * enclosing stream.
 *
 * <pre><code>
 * if (tag.doStartTag() == EVAL_BODY_TAG) {
 *   out = pageContext.pushBody();
 *   tag.setBodyContent(out);
 *   tag.doInitBody();
 *   do {
 *     ...
 *   } while (tag.doAfterBody() == EVAL_BODY_AGAIN);
 *   out = pageContent.popBody();
 * }
 * if (tag.doEndTag() == SKIP_PAGE)
 *   return;
 * </code><pre>
 */
public interface BodyTag extends IterationTag {
  /**
   * Constant returned by doStartTag to evaluate a tag body.
   *
   * @deprecated
   */
  public final static int EVAL_BODY_TAG = 2;
  /**
   * Constant returned by doStartTag to evaluate a tag body.
   */
  public final static int EVAL_BODY_BUFFERED = 2;
  
  /**
   * Sets the BodyContent stream.  A tag calls setBodyContent before calling
   * doInitBody()
   *
   * @param out The body content for tag and its contents.
   */
  public void setBodyContent(BodyContent out);
  /**
   * Tags call doInitBody before processing the tag body.  doInitBody is
   * called after setBodyContent. It is called only once for each tag, even
   * if the tag loops.
   *
   * <p>empty tags and tags returning SKIP_BODY do not call
   * doInitBody and doAfterBody.
   *
   * <pre><code>
   * if (tag.doStartTag() == EVAL_BODY_TAG) {
   *   out = pageContext.pushBody();
   *   tag.setBodyContent(out);
   *   tag.doInitBody();
   *   ...
   * }
   * </code></pre>
   */
  public void doInitBody() throws JspException;
  /**
   * Tags call doAfterBody after processing the tag body.  Tags writing
   * to the output stream must write the body to the enclosing JspWriter.
   * Tags can loop by returning EVAL_PAGE and stop by returning SKIP_PAGE.
   *
   * <p>empty tags and tags returning SKIP_BODY do not call
   * doInitBody and doAfterBody.
   *
   * <p>Here's an example of a tag that copies its contents
   * to the output (assuming setBodyContent sets bodyOut):
   * <pre><code>
   * public int doAfterBody() throws JspException
   * {
   *   try {
   *     bodyOut.writeOut(bodyOut.getEnclosingWriter());
   *   } catch (IOException e) {
   *     throw JspException(String.valueOf(e));
   *   }
   *   
   *   return SKIP_PAGE;
   * }
   * </code></pre>
   *
   * @return EVAL_PAGE to repeat the tag and SKIP_PAGE to stop.
   */
  public int doAfterBody() throws JspException;
}
