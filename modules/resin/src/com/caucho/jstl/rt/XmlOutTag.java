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

package com.caucho.jstl.rt;

import com.caucho.jsp.PageContextImpl;
import com.caucho.xpath.Env;
import com.caucho.xpath.Expr;
import com.caucho.xpath.XPath;

import org.w3c.dom.Node;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
import javax.el.ELException;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

public class XmlOutTag extends TagSupport {
  private static final Logger log 
    = Logger.getLogger(XmlOutTag.class.getName());


  // private Expr _select;
  private Expr _select;
  private boolean _escapeXml = true;

  /**
   * Sets the JSP-EL expression value.
   * @throws XPathExpressionException 
   */
  public void setSelect(Expr select)
  {
    _select = select;
  }

  /**
   * Sets true if XML should be escaped.
   */
  public void setEscapeXml(boolean escapeXml)
  {
    _escapeXml = escapeXml;
  }

  /**
   * Process the tag.
   */
  public int doStartTag()
    throws JspException
  {
    try {
      PageContextImpl pageContext = (PageContextImpl) this.pageContext;

      JspWriter out = pageContext.getOut();

      Node node = pageContext.getNodeEnv();

      Env env = XPath.createEnv();
      env.setVarEnv(pageContext.getVarEnv());

      String value = _select.evalString(node, env);

      env.free();

      if (_escapeXml)
        com.caucho.el.Expr.toStreamEscaped(out, value);
      else
        out.print(value);
    }
    catch (Exception e) {
      throw new JspException(e);
    }

    return SKIP_BODY;
  }
}
