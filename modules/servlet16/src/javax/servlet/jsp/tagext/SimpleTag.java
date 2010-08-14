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
 * $Id: SimpleTag.java,v 1.2 2004/09/29 00:12:48 cvs Exp $
 */

package javax.servlet.jsp.tagext;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import java.io.IOException;

/**
 * Tags are Java classes implementing JSP Tag extensions.  The simple tag
 * is intended for tags that support fragments.
 */
public interface SimpleTag extends JspTag {
   /**
    * Returns the parent tag.
    */
   public JspTag getParent();
   
   /**
    * Sets the parent tag.
    */
   public void setParent(JspTag parent);
   
   /**
    * Sets the JSP fragment for the body.
    */
   public void setJspBody(JspFragment jspBody);
   
   /**
    * Sets the JSP context for the tag.
    */
   public void setJspContext(JspContext pc);
   
   /**
    * Callback to handle the tag.
    *
    * <p>doTag can assume <code>setPageContext</code>,
    * <code>setParent</code>, and all tag attribute properties have
    * been called.
    *
    * @return SKIP_PAGE to skip the page or EVAL_PAGE to continue.
    */
  public void doTag() throws JspException, IOException;
}
