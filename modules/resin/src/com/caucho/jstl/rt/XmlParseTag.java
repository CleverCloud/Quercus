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

package com.caucho.jstl.rt;

import com.caucho.jsp.BodyContentImpl;
import com.caucho.jsp.PageContextImpl;
import com.caucho.util.L10N;
import com.caucho.vfs.TempCharReader;
import com.caucho.vfs.Vfs;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import java.io.Reader;

public class XmlParseTag extends BodyTagSupport {
  private static L10N L = new L10N(XmlParseTag.class);

  private Object _xml;
  private String _systemId;
  private Object _filter;

  private String _var;
  private String _scope;
  
  private String _varDom;
  private String _scopeDom;

  /**
   * Sets the xml
   */
  public void setXml(Object xml)
  {
    _xml = xml;
  }

  /**
   * Sets the xml
   */
  public void setDoc(Object xml)
  {
    setXml(xml);
  }

  /**
   * Sets the system id
   */
  public void setSystemId(String systemId)
  {
    _systemId = systemId;
  }

  /**
   * Sets the filter
   */
  public void setFilter(Object filter)
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

      XMLReader xmlReader = null;
      Reader reader = null;
      InputSource is;

      if (_xml != null) {
        Object obj = _xml;

        if (obj instanceof Reader) {
          reader = (Reader) obj;

          is = new InputSource(reader);
        }
        else if (obj instanceof String) {
          reader = Vfs.openString((String) obj).getReader();

          is = new InputSource(reader);
        } else if (obj instanceof XMLReader) {
          xmlReader = (XMLReader) obj;

          is = new InputSource();
        }
        else
          throw new JspException(L.l("xml must be java.io.Reader, String or org.xml.sax.XMLReader at `{0}'",
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

        is = new InputSource(reader);
      }
      /*
      else if (_filter != null) {
        is = null;
      }
      */
      else {
        throw new JspException(L.l("No XML document supplied via a doc attribute or inside the body of <x:parse> tag."));
      }

      if (_systemId != null && is != null)
        is.setSystemId(_systemId);

      XMLFilter filter = (XMLFilter) _filter;

      if (xmlReader == null) {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        
        // jsp/1m14
        parserFactory.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
        parserFactory.setFeature("http://xml.org/sax/features/namespaces", true);
        
        xmlReader = parserFactory.newSAXParser().getXMLReader();
      }

      // jsp/1g05
      if (_filter != null && _var == null && _varDom == null) {
        filter.setParent(xmlReader);
        
        filter.parse(is);
      } else {
        DocumentBuilderFactory domFactory
          = DocumentBuilderFactory.newInstance();
        
        domFactory.setNamespaceAware(true);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        
        Document doc = builder.newDocument();
        
        SAXTransformerFactory saxFactory
          = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        
        //saxFactory.setAttribute("http://xml.org/sax/features/namespace-prefixes", false);
        // saxFactory.setAttribute("http://xml.org/sax/features/namespaces", false);
        //saxFactory.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
        // xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
        
        TransformerHandler handler
          = saxFactory.newTransformerHandler();
        
        handler.setResult(new DOMResult(doc));

        if (filter != null) {
          filter.setParent(xmlReader);
          filter.setContentHandler(handler);

          filter.parse(is);
        } else {
          xmlReader.setContentHandler(handler);

          xmlReader.parse(is);
        }

        if (_var == null && _varDom == null)
          throw new JspException(L.l("x:parse needs either var or varDom"));

        if (_var != null)
          CoreSetTag.setValue(pageContext, _var, _scope, doc);

        if (_varDom != null)
          CoreSetTag.setValue(pageContext, _varDom, _scopeDom, doc);

        if (reader != null)
          reader.close();
      }
    } catch (JspException e) {
      throw e;
    } catch (Exception e) {
      throw new JspException(e);
    }

    return EVAL_PAGE;
  }
}
