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

package com.caucho.jstl.el;

import com.caucho.el.Expr;
import com.caucho.jsp.BodyContentImpl;
import com.caucho.jsp.PageContextImpl;
import com.caucho.jstl.NameValueTag;
import com.caucho.server.http.CauchoResponse;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.FlushBuffer;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;

import javax.el.ELContext;
import javax.el.ELException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class ImportTag extends BodyTagSupport implements NameValueTag {
  private static L10N L = new L10N(ImportTag.class);
  
  private Expr _urlExpr;
  private Expr _contextExpr;

  private CharBuffer _query = new CharBuffer();

  private Expr _charEncodingExpr;

  private String _var;
  private String _scope;
  
  private String _varReader;

  private Reader _reader;

  /**
   * Sets the URL to be imported.
   */
  public void setURL(Expr url)
  {
    _urlExpr = url;
  }
  
  /**
   * Sets the external context for the import.
   */
  public void setContext(Expr context)
  {
    _contextExpr = context;
  }

  /**
   * Adds a parameter.
   */
  public void addParam(String name, String value)
  {
    if (name == null)
      return;

    if (value == null)
      value = "";
    
    if (_query.length() != 0)
      _query.append('&');

    _query.append(name);
    _query.append('=');
    int len = value.length();
    for (int i = 0; i < len; i++) {
      char ch = value.charAt(i);

      switch (ch) {
      case '&':
        _query.append("%26");
        break;

      case '%':
        _query.append("%25");
        break;

      case '+':
        _query.append("%2b");
        break;

      case '=':
        _query.append("%3d");
        break;

      default:
        _query.append(ch);
        break;
      }
    }
  }
  
  /**
   * Sets the external character encoding for the import.
   */
  public void setCharEncoding(Expr charEncoding)
  {
    _charEncodingExpr = charEncoding;
  }

  /**
   * Sets the variable for the import.
   */
  public void setVar(String var)
  {
    _var = var;
  }
  
  /**
   * Sets the scope for the result variable for the output.
   */
  public void setScope(String scope)
  {
    _scope = scope;
  }
  
  /**
   * Sets the variable for the import.
   */
  public void setVarReader(String varReader)
  {
    _varReader = varReader;
  }

  /**
   * Process the tag.
   */
  public int doStartTag()
    throws JspException
  {
    JspWriter jspWriter = null;

    _query.clear();

    PageContextImpl pageContext = (PageContextImpl) this.pageContext;
    
    try {
      if (_varReader != null) {
        jspWriter = pageContext.pushBody();
        
        handleBody();

        BodyContentImpl body = (BodyContentImpl) pageContext.getOut();

        _reader = body.getReader();

        pageContext.setAttribute(_varReader, _reader);

        return EVAL_BODY_INCLUDE;
      }
      else
        return EVAL_BODY_BUFFERED;
    } catch (JspException e) {
      throw e;
    } catch (Exception e) {
      throw new JspException(e);
    } finally {
      if (jspWriter != null)
        pageContext.popBody();
    }
  }
  
  /**
   * Process the end tag
   */
  public int doEndTag()
    throws JspException
  {
    if (_varReader == null) {
      try {
        JspWriter jspWriter = pageContext.pushBody();
          
        handleBody();

        BodyContentImpl body = (BodyContentImpl) pageContext.getOut();

        if (_var != null) {
          String value = body.getString();

          pageContext.popBody();

          CoreSetTag.setValue(pageContext, _var, _scope, value);
        }
        else {
          body.writeOut(body.getEnclosingWriter());

          pageContext.popBody();
        }
      } catch (JspException e) {
        throw e;
      } catch (Exception e) {
        throw new JspException(e);
      }
    }
    
    return EVAL_PAGE;
  }

  private void handleBody()
    throws JspException, ServletException, IOException, ELException
  {
    PageContextImpl pageContext = (PageContextImpl) this.pageContext;
    ELContext env = pageContext.getELContext();
    
    String url = _urlExpr.evalString(env);

    if (url == null || url.equals(""))
      throw new JspTagException(L.l("URL may not be null for `{0}'",
                                    _urlExpr));

    if (_query == null || _query.getLength() == 0) {
    }
    else if (url.indexOf('?') > 0)
      url = url + '&' + _query;
    else
      url = url + '?' + _query;

    JspWriter out = pageContext.getOut();
    if (out instanceof FlushBuffer)
      ((FlushBuffer) out).flushBuffer();
    else
      out.flush();

    if (_contextExpr != null) {
      String context = _contextExpr.evalString(env);

      if (! url.startsWith("/"))
        throw new JspException(L.l("URL `{0}' must start with `/' with context `{0}'", url, context));
        
      if (context != null && context.startsWith("/")) {
        ServletContext app = pageContext.getServletContext().getContext(context);

        try {
          RequestDispatcher disp = app.getRequestDispatcher(url);

          if (disp == null)
            throw new JspException(L.l("URL `{0}' does not map to any servlet",
                                       url));

          CauchoResponse response = (CauchoResponse) pageContext.getResponse();
          response.getResponseStream().setEncoding(null);

          disp.include(pageContext.getRequest(), response);
        } catch (FileNotFoundException e) {
          throw new JspException(L.l("`{0}' is an unknown file or servlet.",
                                     url));
        }
      }
      else
        handleExternalBody(context + url);
      
      return;
    }

    int colon = url.indexOf(':');
    int slash = url.indexOf('/');
    if (slash == 0 || colon < 0 || slash < 0 || slash < colon) {
      ServletRequest request = pageContext.getRequest();

      try {
        RequestDispatcher disp = request.getRequestDispatcher(url);

        if (disp == null)
          throw new JspException(L.l("URL `{0}' does not map to any servlet",
                                     url));

        CauchoResponse response = (CauchoResponse) pageContext.getResponse();
        response.getResponseStream().setEncoding(null);

        disp.include(pageContext.getRequest(), response);
      } catch (FileNotFoundException e) {
        throw new JspException(L.l("URL `{0}' is an unknown file or servlet.",
                                   url));
      }
    }
    else
      handleExternalBody(url);
  }

  private void handleExternalBody(String url)
    throws JspException, ServletException, IOException, ELException
  {
    PageContextImpl pageContext = (PageContextImpl) this.pageContext;
    ELContext env = pageContext.getELContext();
    
    URL netURL = new URL(url);

    URLConnection conn = netURL.openConnection();

    if (conn instanceof HttpURLConnection)
      ((HttpURLConnection) conn).setFollowRedirects(true);

    InputStream is = conn.getInputStream();
    try {
      ReadStream in = Vfs.openRead(is);
      String encoding = conn.getContentEncoding();
      String contentType = conn.getContentType();

      if (_charEncodingExpr != null) {
        encoding = _charEncodingExpr.evalString(env);
        if (encoding != null && ! encoding.equals(""))
          in.setEncoding(encoding);
      }
      else if (encoding != null)
        in.setEncoding(encoding);
      else if (contentType != null) {
        int p = contentType.indexOf("charset=");
        if (p > 0) {
          CharBuffer cb = new CharBuffer();
          for (int i = p + 8; i < contentType.length(); i++) {
            int ch = contentType.charAt(i);
            if (ch == '"' || ch == '\'') {
            }
            else if (ch >= 'a' && ch <= 'z')
              cb.append((char) ch);
            else if (ch >= 'A' && ch <= 'Z')
              cb.append((char) ch);
            else if (ch >= '0' && ch <= '9')
              cb.append((char) ch);
            else if (ch == '-' || ch == '_')
              cb.append((char) ch);
            else
              break;
          }
          encoding = cb.toString();

          in.setEncoding(encoding);
        }
      }
      
      JspWriter out = pageContext.getOut();

      int ch;
      while ((ch = in.readChar()) >= 0)
        out.print((char) ch);
    } finally {
      is.close();
    }
  }
}
