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
import com.caucho.jsp.ResinJspWriter;
import com.caucho.jstl.NameValueTag;
import com.caucho.server.http.CauchoResponse;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class CoreImportTag extends BodyTagSupport implements NameValueTag {
  private static L10N L = new L10N(CoreImportTag.class);
  
  private String _url;
  private String _context;

  private CharBuffer _query = new CharBuffer();

  private String _charEncoding;

  private String _var;
  private String _scope;
  
  private String _varReader;

  private Reader _reader;
  private Reader _oldReader;

  /**
   * Sets the URL to be imported.
   */
  public void setURL(String url)
  {
    _url = url;
  }
  
  /**
   * Sets the external context for the import.
   */
  public void setContext(String context)
  {
    _context = context;
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
  public void setCharEncoding(String charEncoding)
  {
    _charEncoding = charEncoding;
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
    _query.clear();
    
    JspWriter jspWriter = null;
    
    try {
      if (_varReader != null) {
        jspWriter = pageContext.pushBody();
        
        BodyContentImpl body = (BodyContentImpl) pageContext.getOut();

        handleBody(body);
        
        _reader = body.getReader();

        pageContext.setAttribute(_varReader, _reader);

        return EVAL_BODY_INCLUDE;
      }
      else
        return EVAL_BODY_BUFFERED;
    } catch (JspException e) {
      throw e;
    } catch (ServletException e) {
      throw new JspException(e);
    } catch (IOException e) {
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
    if (_varReader != null) {
      this.pageContext.removeAttribute(_varReader);
    }
    else {
      try {
        JspWriter jspWriter = pageContext.pushBody();
          
        BodyContentImpl body = (BodyContentImpl) pageContext.getOut();

        handleBody(body);

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
      } catch (ServletException e) {
        Throwable e1 = e;
        
        if (e1.getCause() != null)
          e1 = e1.getCause();
        
        throw new JspException(e1.getMessage(), e1);
      } catch (Exception e) {
        throw new JspException(e.getMessage(), e);
      }
    }
    
    return EVAL_PAGE;
  }

  private String getCharEncoding()
  {
    if (_charEncoding != null && !"".equals(_charEncoding))
      return _charEncoding;

    String charEncoding = _charEncoding;

    if (_charEncoding == null || "".equals(_charEncoding))
      charEncoding = null;

    if (charEncoding == null) {
      //XXX performance?
      WebApp webApp = WebApp.getCurrent();

      if (webApp.getJsp() != null)
        charEncoding = webApp.getJsp().getPageEncoding();

      if (charEncoding == null)
        charEncoding = webApp.getCharacterEncoding();
    }

    return charEncoding;
  }

  private void handleBody(BodyContentImpl body)
    throws JspException, ServletException, IOException
  {
    String url = _url;

    if (url == null || url.equals(""))
      throw new JspTagException(L.l("URL may not be null for `{0}'",
                                    _url));

    int p;
    if (_query == null || _query.getLength() == 0) {
    }
    else if ((p = url.indexOf('?')) > 0) {
      // jsp/1cip
      url = url + '&' + _query; 
      //url = url.substring(0, p) + '?' + _query + '&' + url.substring(p + 1);
    }
    else
      url = url + '?' + _query;

    JspWriter out = body;
    if (out instanceof ResinJspWriter)
      ((ResinJspWriter) out).flushBuffer();
    else {
      // jsp/1ei0
      // out.flush();
    }

    if (_context != null) {
      String context = _context;

      if (! url.startsWith("/"))
        throw new JspException(L.l("URL `{0}' must start with `/' with context `{0}'", url, context));
        
      if (context != null && context.startsWith("/")) {
        ServletContext app = pageContext.getServletContext().getContext(context);

        RequestDispatcher disp = app.getRequestDispatcher(url);

        if (disp == null)
          throw new JspException(L.l("URL `{0}' does not map to any servlet",
                                     url));

        CauchoResponse response = (CauchoResponse) pageContext.getResponse();

        String charEncoding = getCharEncoding();

        if (charEncoding != null)
          response.getResponseStream().setEncoding(charEncoding);

        final ServletRequest request = pageContext.getRequest();

        disp.include(request, response);

/*
        final Integer statusCode
          = (Integer) request.getAttribute("com.caucho.dispatch.response.statusCode");


        if (statusCode != null) {
          final int status = statusCode.intValue();

          if (status < 200 || status > 299) {
            String message = L.l(
              "c:import status code {0} recieved while serving {1}",
              statusCode,
              context + url);

            throw new JspException(message);
          }
        }
*/
        //jsp/1jif
        int status = response.getStatus();

        if (status < 200 || status > 299) {
          String message = L.l(
            "c:import status code {0} recieved while serving {1}",
            status,
            context + url);

          throw new JspException(message);
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
      CauchoResponse response = (CauchoResponse) pageContext.getResponse();

      String charEncoding = getCharEncoding();

      if (charEncoding != null)
        response.getResponseStream().setEncoding(charEncoding);

      RequestDispatcher disp = request.getRequestDispatcher(url);

      JstlImportResponseWrapper wrapper = new JstlImportResponseWrapper(response);
      disp.include(request, wrapper);

      //jsp/1jie
      int status = wrapper.getStatus();

      if (status < 200 || status > 299) {
        String message = L.l(
          "c:import status code {0} recieved while serving {1}",
          status,
          url);

        throw new JspException(message);
      } else {
        response.setStatus(status);
      }

    }
    else
      handleExternalBody(url);
  }

  private void handleExternalBody(String url)
    throws JspException, ServletException, IOException
  {
    URL netURL = new URL(url);

    URLConnection conn = netURL.openConnection();

    if (conn instanceof HttpURLConnection)
      ((HttpURLConnection) conn).setFollowRedirects(true);

    InputStream is = conn.getInputStream();
    try {
      ReadStream in = Vfs.openRead(is);
      String encoding = conn.getContentEncoding();
      String contentType = conn.getContentType();

      if (_charEncoding != null) {
        encoding = _charEncoding;
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

  static class JstlImportResponseWrapper extends HttpServletResponseWrapper {
    private int _status;

    JstlImportResponseWrapper(HttpServletResponse response)
    {
      super(response);
      _status = response.getStatus();
    }

    @Override
    public void setStatus(int sc)
    {
      _status = sc;
    }

    @Override
    public int getStatus()
    {
      return _status;
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + getResponse() + "]";
    }
  }
}

