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

import javax.el.ELContext;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.JspTag;
import java.io.Writer;
import java.util.logging.Logger;

/**
 * Abstract implementation for the fragment support.
 */
abstract public class JspFragmentSupport extends JspFragment {
  private static final Logger log
    = Logger.getLogger(JspFragmentSupport.class.getName());

  // the parent page context
  protected JspContext _jsp_parentContext;

  // the page context
  protected PageContextImpl pageContext;

  // the EL context
  protected ELContext _jsp_env;
  
  // the parent tag
  protected JspTag _jsp_parent_tag;
  
  // the tag body
  protected JspFragment _jspBody;

  protected com.caucho.jsp.PageManager _jsp_pageManager;

  /**
   * Returns the context.
   */
  public JspContext getJspContext()
  {
    return this.pageContext;
  }
  
  public JspFragment getJspBody()
  {
    // jsp/102b
    return _jspBody;
  }

  /**
   * The public JspFragment interface.
   *
   * @param out optional location for the output
   * @param vars optional map to replace the page context
   */
  public void invoke(Writer out)
    throws JspException
  {
    JspWriter oldOut = null;

    if (out != null)
      oldOut = pageContext.pushBody(out);

    try {
      _jsp_invoke(pageContext.getOut());
    } catch (RuntimeException e) {
      throw e;
    } catch (Error e) {
      throw e;
    } catch (JspException e) {
      throw e;
    } catch (Throwable e) {
      throw new JspException(e);
    } finally {
      if (oldOut != null)
        pageContext.setWriter(oldOut);
    }
  }
  
  /**
   * Implementations will generate code for this.
   */
  abstract protected void _jsp_invoke(JspWriter out)
    throws Throwable;

}
