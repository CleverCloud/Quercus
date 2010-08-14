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

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import java.io.IOException;

/**
 * Support for SimpleTag.
 */
public class SimpleTagSupport implements SimpleTag {
  private JspTag _parent;
  private JspFragment _jspBody;
  private JspContext _jspContext;

  /**
   * Create a new SimpleTagSupport.
   */
  public SimpleTagSupport()
  {
  }

  /**
   * returns the parent
   */
  public JspTag getParent()
  {
    return _parent;
  }

  /**
   * Sets the parent
   */
  public void setParent(JspTag parent)
  {
    _parent = parent;
  }

  /**
   * returns the body fragment.
   */
  protected JspFragment getJspBody()
  {
    return _jspBody;
  }

  /**
   * Sets the body fragment.
   */
  public void setJspBody(JspFragment body)
  {
    _jspBody = body;
  }

  /**
   * returns the context
   */
  protected JspContext getJspContext()
  {
    return _jspContext;
  }

  /**
   * Sets the context
   */
  public void setJspContext(JspContext context)
  {
    _jspContext = context;
  }

  /**
   * Does nothing.
   */
  public void doTag()
    throws IOException, JspException
  {
  }

  /**
   * Finds an ancestor of a tag matching the class.  The search is strict,
   * i.e. only parents will be searched, not the tag itself.
   *
   * @param tag child tag to start searching.
   * @param cl the class that the tag should implement.
   *
   * @return the matching tag or null.
   */
  public static final JspTag findAncestorWithClass(JspTag tag, Class<?> cl)
  {
    if (tag == null || cl == null)
      return null;

    while (tag != null) {
      if (tag instanceof Tag)
        tag = ((Tag) tag).getParent();
      else if (tag instanceof SimpleTag)
        tag = ((SimpleTag) tag).getParent();

      if (tag == null)
        return null;
      else if (tag instanceof TagAdapter) {
        TagAdapter adapter = (TagAdapter) tag;

        if (cl.isAssignableFrom(adapter.getAdaptee().getClass()))
          return adapter.getAdaptee();
      }
      else if (cl.isAssignableFrom(tag.getClass()))
        return tag;
    }

    return null;
  }
}
