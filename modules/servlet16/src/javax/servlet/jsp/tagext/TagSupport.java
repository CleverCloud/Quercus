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
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Convenient support class for creating tags which don't analyze their
 * bodies.  Normally, tags will extend TagSupport instead of implementing
 * Tag directly.
 */
public class TagSupport implements IterationTag, Serializable {
  /**
   * The value of the "id" attribute for the tag, if specified.
   */
  protected String id;
  /**
   * The owning PageContext.
   */
  protected PageContext pageContext;
  private Tag parent;
  private Hashtable values;

  /**
   * Tags need to implement a zero-arg constructor.
   */
  public TagSupport()
  {
  }

  /**
   * Processed at the beginning of the tag.
   *
   * <p>The default behavior returns SKIP_BODY to skip the tag's body.
   */
  public int doStartTag() throws JspException
  {
    return SKIP_BODY;
  }

  /**
   * Processed to check if the tag should loop.  The default behavior
   * returns SKIP_BODY so it does not loop.
   *
   * @return EVAL_BODY_AGAIN to repeat the body or SKIP_BODY to stop.
   */
  public int doAfterBody() throws JspException
  {
    return SKIP_BODY;
  }

  /**
   * Processed at the end of the tag.  The default behavior continues
   * with the rest of the JSP.
   *
   * @return EVAL_PAGE to continue the page SKIP_PAGE to stop.
   */
  public int doEndTag() throws JspException
  {
    return EVAL_PAGE;
  }

  /**
   * Sets the id attribute.
   */
  public void setId(String id)
  {
    this.id = id;
  }
  
  /**
   * Sets the id attribute.
   */
  public String getId()
  {
    return id;
  }

  /**
   * Stores the page context for the JSP page.
   */
  public void setPageContext(PageContext page)
  {
    this.pageContext = page;
  }

  /**
   * If the tag is contained in another tag, this sets the parent.
   *
   * @param parent the tag to be used as a parent. 
   */
  public void setParent(Tag parent)
  {
    this.parent = parent;
    if (values != null)
      values.clear();
  }

  /**
   * Returns the tag's parent.
   */
  public Tag getParent()
  {
    return this.parent;
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
  public static final Tag findAncestorWithClass(Tag tag, Class cl)
  {
    if (tag == null || cl == null)
      return null;

    tag = tag.getParent();
    for (; tag != null; tag = tag.getParent()) {
      if (cl.isAssignableFrom(tag.getClass()))
        return tag;
    }

    return tag;
  }

  /**
   * Returns an attribute from the tag.
   */
  public Object getValue(String name)
  {
    if (values == null)
      return null;
    else
      return values.get(name);
  }

  /**
   * Enumerates the tag's attributes.
   */
  public Enumeration<String> getValues()
  {
    if (values == null)
      values = new Hashtable();

    return values.keys();
  }

  /**
   * Removes a value from the tag.
   */
  public void removeValue(String name)
  {
    if (values == null)
      return;

    values.remove(name);
  }

  /**
   * Sets the value for a tag attribute.
   *
   * @param name the name of the attribute.
   * @param value the new value for the attribute.
   */
  public void setValue(String name, Object value)
  {
    if (values == null)
      values = new Hashtable();

    values.put(name, value);
  }

  /**
   * Cleans the tag after it completes.
   */
  public void release()
  {
    parent = null;
    if (values != null)
      values.clear();
  }
}


