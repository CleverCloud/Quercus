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

import com.caucho.jstl.NameValueTag;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class CoreUrlTag extends TagSupport implements NameValueTag {
  private static L10N L = new L10N(CoreUrlTag.class);

  private static String []_shortEncoding;
  
  private String _value;
  private String _context;

  private String _var;
  private String _scope;

  private CharBuffer _url;

  /**
   * Sets the URL to be imported.
   */
  public void setValue(String value)
  {
    _value = value;
  }
  
  /**
   * Sets the external context for the import.
   */
  public void setContext(String context)
  {
    _context = context;
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
   * Adds a parameter.
   */
  public void addParam(String name, String value)
  {
    String encoding = this.pageContext.getResponse().getCharacterEncoding();
    
    addParam(_url, name, value, encoding);
  }

  public int doStartTag() throws JspException
  {
    String value = _value;
    String context = _context;
    
    _url = normalizeURL(pageContext, value, context);

    return EVAL_BODY_INCLUDE;
  }
      
  public int doEndTag() throws JspException
  {
    String value = encodeURL(pageContext, _url);

    try {
      if (_var == null) {
        JspWriter out = pageContext.getOut();

        out.print(value);
      }
      else
        CoreSetTag.setValue(pageContext, _var, _scope, value);
    } catch (IOException e) {
      throw new JspException(e);
    }

    return EVAL_PAGE;
  }

  public static CharBuffer normalizeURL(PageContext pageContext,
                                        String url, String context)
    throws JspException
  {
    if (context != null
        && context.length() != 0
        && ! context.startsWith("/"))
      throw new JspException(L.l("URL context '{0}' must start with '/'",
                                 context));
    
    if (context != null && (url == null || ! url.startsWith("/")))
      throw new JspException(L.l("URL '{0}' must start with '/'",
                                 url));
    
    CharBuffer value = new CharBuffer();

    if (url == null)
      url = "";

    int slash = url.indexOf('/');
    int colon = url.indexOf(':');

    if (colon > 0 && colon < slash) {
      value.append(url);
    }
    else if (slash == 0) {
      HttpServletRequest request;
      request = (HttpServletRequest) pageContext.getRequest();

      if (context != null) {
        if (context.length() > 1)
          value.append(context);

        value.append(url);
      }
      else {
        value.append(request.getContextPath());
        value.append(url);
      }
    }
    else {
      if (context != null) {
        value.append(context);
        value.append(url);
      }
      else
        value.append(url);
    }

    return value;
  }

  public static CharBuffer addParam(CharBuffer url,
                                    String name, String value,
                                    String encoding)
  {
    if (url.indexOf('?') < 0)
      url.append('?');
    else
      url.append("&");

    addEncodedString(url, name, encoding);
    url.append('=');
    addEncodedString(url, value, encoding);

    return url;
  }


  /**
   * Adds a parameter to the string, with the given encoding.
   */
  public static CharBuffer addEncodedString(CharBuffer cb,
                                            String value,
                                            String encoding)
  {
    if (encoding == null || encoding.equalsIgnoreCase("iso-8859-1"))
      return addEncodedLatin1(cb, value);
    else if (encoding.equalsIgnoreCase("utf8") ||
             encoding.equalsIgnoreCase("utf-8"))
      return addEncodedUTF8(cb, value);
    else {
      try {
        cb.append(URLEncoder.encode(value, encoding));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
      
      return cb;
    }
  }

  /**
   * Adds a parameter to the string, encoding with latin-1 when necessary.
   */
  public static CharBuffer addEncodedLatin1(CharBuffer cb, String value)
  {
    int len = value.length();

    for (int i = 0; i < len; i++) {
      char ch = value.charAt(i);

      if (ch < 0x80)
        cb.append(_shortEncoding[ch]);
      else {
        cb.append('%');
        cb.append(hex(ch >> 4));
        cb.append(hex(ch & 0xf));
      }
    }

    return cb;
  }

  /**
   * Adds a parameter to the string, encoding with UTF8 when necessary.
   */
  public static CharBuffer addEncodedUTF8(CharBuffer cb, String value)
  {
    int len = value.length();

    for (int i = 0; i < len; i++) {
      char ch = value.charAt(i);

      if (ch < 0x80)
        cb.append(_shortEncoding[ch]);
      else if (ch < 0x800) {
        // UTF-8

        int d1 = 0xc0 + ((ch >> 6) & 0x1f);
        int d2 = 0x80 + (ch & 0x3f);
                         
        cb.append('%');
        cb.append(hex(d1 >> 4));
        cb.append(hex(d1 & 0xf));
        cb.append('%');
        cb.append(hex(d2 >> 4));
        cb.append(hex(d2 & 0xf));
      }
      else {
        // UTF-8

        int d1 = 0xe0 + ((ch >> 12) & 0xf);
        int d2 = 0x80 + ((ch >> 6) & 0x3f);
        int d3 = 0x80 + (ch & 0x3f);
                         
        cb.append('%');
        cb.append(hex(d1 >> 4));
        cb.append(hex(d1 & 0xf));
        cb.append('%');
        cb.append(hex(d2 >> 4));
        cb.append(hex(d2 & 0xf));
        cb.append('%');
        cb.append(hex(d3 >> 4));
        cb.append(hex(d3 & 0xf));
      }
    }

    return cb;
  }

  public static String encodeURL(PageContext pageContext, CharBuffer url)
  {
    String value = url.toString();

    int colon = value.indexOf(':');
    int slash = value.indexOf('/');

    if (colon < slash && slash > 0)
      return value;
    else
      return ((HttpServletResponse) pageContext.getResponse()).encodeURL(value);

    /*
    if (value.startsWith("/"))
      return ((HttpServletResponse) pageContext.getResponse()).encodeURL(value);
    else
      return value;
    */
  }


  private static char hex(int d)
  {
    d = d & 0xf;
    
    if (d < 10)
      return (char) ('0' + d);
    else
      return (char) ('A' + d - 10);
  }
      
  static {
    _shortEncoding = new String[128];

    for (int i = 0; i < _shortEncoding.length; i++)
      _shortEncoding[i] = "%" + hex(i / 16) + hex(i);

    for (int i = 'a'; i <= 'z'; i++)
      _shortEncoding[i] = String.valueOf((char) i);
    
    for (int i = 'A'; i <= 'Z'; i++)
      _shortEncoding[i] = String.valueOf((char) i);
    
    for (int i = '0'; i <= '9'; i++)
      _shortEncoding[i] = String.valueOf((char) i);

    _shortEncoding[' '] = "+";
    _shortEncoding['-'] = "-";
    _shortEncoding['_'] = "_";
    _shortEncoding['.'] = ".";
    _shortEncoding['!'] = "!";
    _shortEncoding['~'] = "~";
    _shortEncoding['\''] = "\'";
  }
}
