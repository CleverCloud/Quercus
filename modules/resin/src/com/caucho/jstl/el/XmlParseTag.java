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

package com.caucho.jstl.el;

import com.caucho.el.Expr;
import com.caucho.jsp.BodyContentImpl;
import com.caucho.jsp.PageContextImpl;
import com.caucho.util.L10N;
import com.caucho.vfs.*;
import com.caucho.xml.Xml;
import com.caucho.xml.XmlParser;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.Reader;

public class XmlParseTag extends BodyTagSupport {
  private static L10N L = new L10N(XmlParseTag.class);

  private Expr _xml;
  private Expr _systemId;
  private Expr _filter;
  
  private String _var;
  private String _scope;
  
  private String _varDom;
  private String _scopeDom;

  /**
   * Sets the xml
   */
  public void setXml(Expr xml)
  {
    setDoc(xml);
  }

  /**
   * Sets the doc
   */
  public void setDoc(Expr xml)
  {
    _xml = xml;
  }

  /**
   * Sets the system id
   */
  public void setSystemId(Expr systemId)
  {
    _systemId = systemId;
  }

  /**
   * Sets the filter
   */
  public void setFilter(Expr filter)
  {
    _filter = filter;
  }

  /**
   * Sets the variable
   */
  public void setVar(String var)
  {
    _var = var;
  }

  /**
   * Sets the scope
   */
  public void setScope(String scope)
  {
    _scope = scope;
  }

  /**
   * Sets the variable
   */
  public void setVarDom(String var)
  {
    _varDom = var;
  }

  /**
   * Sets the scope
   */
  public void setScopeDom(String scope)
  {
    _scopeDom = scope;
  }

  public int doEndTag() throws JspException
  {
    try {
      PageContextImpl pageContext = (PageContextImpl) this.pageContext;
      BodyContentImpl body = (BodyContentImpl) getBodyContent();

      Reader reader;

      if (_xml != null) {
        Object obj = _xml.evalObject(pageContext.getELContext());

        if (obj instanceof Reader)
          reader = (Reader) obj;
        else if (obj instanceof String)
          reader = Vfs.openString((String) obj).getReader();
        else
          throw new JspException(L.l("'doc' attribute must be a Reader or String at `{0}'",
                                     obj));
      }
      else if (body != null) {
        TempCharReader tempReader = (TempCharReader) body.getReader();
        int ch;

        while (Character.isWhitespace((ch = tempReader.read()))) {
        }

        if (ch >= 0)
          tempReader.unread();
        
        reader = tempReader;
      }
      else
        throw new JspException(L.l("x:parse requires a body"));

      InputSource is = new InputSource(reader);

      XmlParser parser = new Xml();

      Document doc = parser.parseDocument(is);

      reader.close();

      if (_var != null)
        CoreSetTag.setValue(pageContext, _var, _scope, doc);
      else if (_varDom != null)
        CoreSetTag.setValue(pageContext, _varDom, _scopeDom, doc);
      else
        throw new JspException(L.l("x:parse needs either var or varDom"));
    } catch (JspException e) {
      throw e;
    } catch (Exception e) {
      throw new JspException(e);
    }

    return EVAL_PAGE;
  }
}
