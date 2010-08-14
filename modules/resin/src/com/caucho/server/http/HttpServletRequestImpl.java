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

package com.caucho.server.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.charset.Charset;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.MultipartConfigElement;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import com.caucho.config.scope.ScopeRemoveListener;
import com.caucho.i18n.CharacterEncoding;
import com.caucho.network.listen.SocketLink;
import com.caucho.network.listen.SocketLinkDuplexController;
import com.caucho.network.listen.SocketLinkDuplexListener;
import com.caucho.security.AbstractLogin;
import com.caucho.security.Login;
import com.caucho.server.cluster.Server;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.session.SessionManager;
import com.caucho.server.webapp.WebApp;
import com.caucho.servlet.WebSocketContext;
import com.caucho.servlet.WebSocketListener;
import com.caucho.servlet.WebSocketServletRequest;
import com.caucho.util.CharBuffer;
import com.caucho.util.CharSegment;
import com.caucho.util.HashMapImpl;
import com.caucho.util.L10N;
import com.caucho.util.NullEnumeration;
import com.caucho.vfs.Encoding;
import com.caucho.vfs.FilePath;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * User facade for http requests.
 */
public final class HttpServletRequestImpl extends AbstractCauchoRequest
  implements CauchoRequest, WebSocketServletRequest
{
  private static final Logger log
    = Logger.getLogger(HttpServletRequestImpl.class.getName());

  private static final L10N L = new L10N(HttpServletRequestImpl.class);

  private static final String CHAR_ENCODING = "resin.form.character.encoding";
  private static final String FORM_LOCALE = "resin.form.local";
  private static final String CAUCHO_CHAR_ENCODING = "caucho.form.character.encoding";

  private static final Charset UTF8 = Charset.forName("UTF-8");

  private AbstractHttpRequest _request;

  private final HttpServletResponseImpl _response;

  private Boolean _isSecure;

  private Invocation _invocation;

  // form
  private HashMapImpl<String,String[]> _filledForm;
  private List<Part> _parts;

  // session/cookies
  private Cookie []_cookiesIn;

  private boolean _varyCookies;   // True if the page depends on cookies
  private boolean _hasCookie;

  private boolean _isSessionIdFromCookie;

  // security
  private String _runAs;
  private boolean _isLoginRequested;

  // input stream management
  private boolean _hasReader;
  private boolean _hasInputStream;

  // servlet attributes
  private HashMapImpl<String,Object> _attributes;

  // proxy caching
  private boolean _isSyntheticCacheHeader;

  // comet
  private long _asyncTimeout = 10000;
  private AsyncContextImpl _asyncContext;

  private ArrayList<Path> _closeOnExit;

  /**
   * Create a new Request.  Because the actual initialization occurs with
   * the start() method, this just allocates statics.
   *
   * @param request
   */
  public HttpServletRequestImpl(AbstractHttpRequest request)
  {
    _request = request;

    _response = new HttpServletResponseImpl(this,
                                            request.getAbstractHttpResponse());
  }

  public HttpServletResponseImpl getResponse()
  {
    return _response;
  }

  //
  // ServletRequest methods
  //

  /**
   * Returns the prococol, e.g. "HTTP/1.1"
   */
  @Override
  public String getProtocol()
  {
    return _request.getProtocol();
  }

  /**
   * Returns the request scheme, e.g. "http"
   */
  @Override
  public String getScheme()
  {
    String scheme = _request.getScheme();

    // server/12j2
    if (isSecure() && "http".equals(scheme))
      return "https";
    else
      return scheme;
  }

  /**
   * Returns the server name handling the request.  When using virtual hosts,
   * this returns the virtual host name, e.g. "vhost1.caucho.com".
   *
   * This call returns the host name as the client sees it, which means that
   * if ipchains, load balancing, or proxying is involved this call returns the
   * correct call for forming urls, but may not contain the host that Resin is
   * actually listening on.
   */
  @Override
  public String getServerName()
  {
    return _request.getServerName();
  }

  /**
   * Returns the server port used by the client, e.g. 80.
   *
   * This call returns the port number as the client sees it, which means that
   * if ipchains, load balancing, or proxying is involved this call returns the
   * correct call for forming urls, but may not return the actual port that
   * Resin is listening on.
   *
   * This call should not be used to test for an ssl connection
   * (getServerPort() == 443), {@link #isSecure()} is provided for
   * that purpose.
   */
  @Override
  public int getServerPort()
  {
    return _request.getServerPort();
  }

  /**
   * Returns the IP address of the remote host, i.e. the client browser.
   */
  @Override
  public String getRemoteAddr()
  {
    return _request.getRemoteAddr();
  }

  /**
   * Returns the DNS hostname of the remote host, i.e. the client browser.
   */
  @Override
  public String getRemoteHost()
  {
    return _request.getRemoteHost();
  }

  /**
   * Returns the port of the remote host, i.e. the client browser.
   *
   * @since 2.4
   */
  @Override
  public int getRemotePort()
  {
    return _request.getRemotePort();
  }

  /**
   * This call returns the ip of the host actually used to connect to the Resin
   * server,  which means that if ipchains, load balancing, or proxying is
   * involved this call <i>does not</i> return the correct host for
   * forming urls.
   *
   * @since 2.4
   */
  @Override
  public String getLocalAddr()
  {
    return _request.getLocalHost();
  }

  /**
   * Returns the IP address of the local host, i.e. the server.
   *
   * This call returns the name of the host actually used to connect to the
   * Resin server,  which means that if ipchains, load balancing, or proxying
   * is involved this call <i>does not</i> return the correct host for
   * forming urls.
   *
   * @since 2.4
   */
  @Override
  public String getLocalName()
  {
    return _request.getLocalHost();
  }

  /**
   * Returns the port of the local host.
   *
   * This call returns the port number actually used to connect to the Resin
   * server,  which means that if ipchains, load balancing, or proxying is
   * involved this call <i>does not</i> return the correct port for
   * forming urls.
   *
   * This call should not be used to test for an ssl connection
   * (getServerPort() == 443), {@link #isSecure()} is provided for that purpose.
   *
   * @since 2.4
   */
  @Override
  public int getLocalPort()
  {
    return _request.getLocalPort();
  }

  /**
   * Overrides the character encoding specified in the request.
   * <code>setCharacterEncoding</code> must be called before calling
   * <code>getReader</code> or reading any parameters.
   */
  @Override
  public void setCharacterEncoding(String encoding)
    throws java.io.UnsupportedEncodingException
  {
    _request.setCharacterEncoding(encoding);
  }

  /**
   * Returns an InputStream to retrieve POST data from the request.
   * The stream will automatically end when the end of the POST data
   * is complete.
   */
  @Override
  public ServletInputStream getInputStream()
    throws IOException
  {
    if (_hasReader)
      throw new IllegalStateException(L.l("getInputStream() can't be called after getReader()"));

    _hasInputStream = true;

    return _request.getInputStream();
  }

  /**
   * Returns a reader to read POSTed data.  Character encoding is
   * based on the request data and is the same as
   * <code>getCharacterEncoding()</code>
   */
  @Override
  public BufferedReader getReader()
    throws IOException, IllegalStateException
  {
    if (_hasInputStream)
      throw new IllegalStateException(L.l("getReader() can't be called after getInputStream()"));

    _hasReader = true;

    return _request.getReader();
  }

  /**
   * Returns the character encoding of the POSTed data.
   */
  @Override
  public String getCharacterEncoding()
  {
    return _request.getCharacterEncoding();
  }

  /**
   * Returns the content length of the data.  This value may differ from
   * the actual length of the data.  Newer browsers
   * supporting HTTP/1.1 may use "chunked" encoding which does
   * not make the content length available.
   *
   * <p>The upshot is, rely on the input stream to end when the data
   * completes.
   */
  @Override
  public int getContentLength()
  {
    return _request.getContentLength();
  }

  /**
   * Returns the request's mime-type.
   */
  @Override
  public String getContentType()
  {
    return _request.getContentType();
  }

  /**
   * Returns the request's preferred locale, based on the Accept-Language
   * header.  If unspecified, returns the server's default locale.
   */
  @Override
  public Locale getLocale()
  {
    return _request.getLocale();
  }

  /**
   * Returns an enumeration of all locales acceptable by the client.
   */
  @Override
  public Enumeration<Locale> getLocales()
  {
    return _request.getLocales();
  }

  /**
   * Returns true if the connection is secure, e.g. it uses SSL.
   */
  @Override
  public boolean isSecure()
  {
    if (_isSecure != null)
      return _isSecure;

    AbstractHttpRequest request = _request;

    if (request != null)
      return request.isSecure();
    else
      return false;
  }

  //
  // request attributes
  //

  /**
   * Returns the value of the named request attribute.
   *
   * @param name the attribute name.
   *
   * @return the attribute value.
   */
  @Override
  public Object getAttribute(String name)
  {
    HashMapImpl<String,Object> attributes = _attributes;

    if (attributes != null)
      return attributes.get(name);
    else if (isSecure()) {
      _attributes = new HashMapImpl<String,Object>();
      attributes = _attributes;
      _request.initAttributes(this);

      return attributes.get(name);
    }
    else
      return null;
  }

  /**
   * Returns an enumeration of the request attribute names.
   */
  @Override
  public Enumeration<String> getAttributeNames()
  {
    HashMapImpl<String,Object> attributes = _attributes;

    if (attributes != null) {
      return Collections.enumeration(attributes.keySet());
    }
    else if (isSecure()) {
      _attributes = new HashMapImpl<String,Object>();
      attributes = _attributes;
      _request.initAttributes(this);

      return Collections.enumeration(attributes.keySet());
    }
    else
      return NullEnumeration.create();
  }

  /**
   * Sets the value of the named request attribute.
   *
   * @param name the attribute name.
   * @param value the new attribute value.
   */
  @Override
  public void setAttribute(String name, Object value)
  {
    HashMapImpl<String,Object> attributes = _attributes;

    if (value != null) {
      if (attributes == null) {
        attributes = new HashMapImpl<String,Object>();
        _attributes = attributes;
        _request.initAttributes(this);
      }

      Object oldValue = attributes.put(name, value);

      WebApp webApp = getWebApp();

      if (webApp != null) {
        for (ServletRequestAttributeListener listener
               : webApp.getRequestAttributeListeners()) {
          ServletRequestAttributeEvent event;

          if (oldValue != null) {
            event = new ServletRequestAttributeEvent(webApp, this,
                                                     name, oldValue);

            listener.attributeReplaced(event);
          }
          else {
            event = new ServletRequestAttributeEvent(webApp, this,
                                                     name, value);

            listener.attributeAdded(event);
          }
        }
      }
    }
    else
      removeAttribute(name);
  }

  /**
   * Removes the value of the named request attribute.
   *
   * @param name the attribute name.
   */
  @Override
  public void removeAttribute(String name)
  {
    HashMapImpl<String,Object> attributes = _attributes;

    if (attributes == null)
      return;

    Object oldValue = attributes.remove(name);

    WebApp webApp = getWebApp();

    for (ServletRequestAttributeListener listener
           : webApp.getRequestAttributeListeners()) {
      ServletRequestAttributeEvent event;

      event = new ServletRequestAttributeEvent(webApp, this,
                                               name, oldValue);

      listener.attributeRemoved(event);
    }

    if (oldValue instanceof ScopeRemoveListener) {
      ((ScopeRemoveListener) oldValue).removeEvent(this, name);
    }
  }

  //
  // request dispatching
  //

  /**
   * Returns a request dispatcher for later inclusion or forwarding.  This
   * is the servlet API equivalent to SSI includes.  <code>uri</code>
   * is relative to the request URI.  Absolute URIs are relative to
   * the application prefix (<code>getContextPath()</code>).
   *
   * <p>If <code>getRequestURI()</code> is /myapp/dir/test.jsp and the
   * <code>uri</code> is "inc.jsp", the resulting page is
   * /myapp/dir/inc.jsp.

   * <code><pre>
   *   RequestDispatcher disp;
   *   disp = getRequestDispatcher("inc.jsp?a=b");
   *   disp.include(request, response);
   * </pre></code>
   *
   * @param path path relative to <code>getRequestURI()</code>
   * (including query string) for the included file.
   * @return RequestDispatcher for later inclusion or forwarding.
   */
  @Override
  public RequestDispatcher getRequestDispatcher(String path)
  {
    if (path == null || path.length() == 0)
      return null;
    else if (path.charAt(0) == '/')
      return getWebApp().getRequestDispatcher(path);
    else {
      CharBuffer cb = new CharBuffer();

      WebApp webApp = getWebApp();

      String servletPath = getPageServletPath();
      if (servletPath != null)
        cb.append(servletPath);
      String pathInfo = getPagePathInfo();
      if (pathInfo != null)
        cb.append(pathInfo);

      int p = cb.lastIndexOf('/');
      if (p >= 0)
        cb.setLength(p);
      cb.append('/');
      cb.append(path);

      if (webApp != null)
        return webApp.getRequestDispatcher(cb.toString());

      return null;
    }
  }

  /**
   * Returns the servlet context for the request
   *
   * @since Servlet 3.0
   */
  @Override
  public ServletContext getServletContext()
  {
    Invocation invocation = _invocation;

    if (invocation != null)
      return invocation.getWebApp();
    else
      return null;
  }

  /**
   * Returns the servlet response for the request
   *
   * @since Servlet 3.0
   */
  @Override
  public ServletResponse getServletResponse()
  {
    return _response;
  }

  //
  // HttpServletRequest APIs
  //

  /**
   * Returns the HTTP method, e.g. "GET" or "POST"
   *
   * <p/>Equivalent to CGI's <code>REQUEST_METHOD</code>
   */
  @Override
  public String getMethod()
  {
    return _request.getMethod();
  }

  /**
   * Returns the URI for the request
   */
  @Override
  public String getRequestURI()
  {
    if (_invocation != null)
      return _invocation.getRawURI();
    else
      return "";
  }

  /**
   * Returns the URI for the page.  getPageURI and getRequestURI differ
   * for included files.  getPageURI gets the URI for the included page.
   * getRequestURI returns the original URI.
   */
  public String getPageURI()
  {
    return _invocation.getRawURI();
  }

  /**
   * Returns the context part of the uri.  The context part is the part
   * that maps to an webApp.
   */
  public String getContextPath()
  {
    if (_invocation != null)
      return _invocation.getContextPath();
    else
      return "";
  }

  /**
   * Returns the context part of the uri.  For included files, this will
   * return the included context-path.
   */
  public String getPageContextPath()
  {
    return getContextPath();
  }

  /**
   * Returns the portion of the uri mapped to the servlet for the original
   * request.
   */
  public String getServletPath()
  {
    if (_invocation != null)
      return _invocation.getServletPath();
    else
      return "";
  }

  /**
   * Returns the portion of the uri mapped to the servlet for the current
   * page.
   */
  public String getPageServletPath()
  {
    if (_invocation != null)
      return _invocation.getServletPath();
    else
      return "";
  }

  /**
   * Returns the portion of the uri after the servlet path for the original
   * request.
   */
  public String getPathInfo()
  {
    if (_invocation != null)
      return _invocation.getPathInfo();
    else
      return null;
  }

  /**
   * Returns the portion of the uri after the servlet path for the current
   * page.
   */
  public String getPagePathInfo()
  {
    if (_invocation != null)
      return _invocation.getPathInfo();
    else
      return null;
  }

  /**
   * Returns the URL for the request
   */
  @Override
  public StringBuffer getRequestURL()
  {
    StringBuffer sb = new StringBuffer();

    sb.append(getScheme());
    sb.append("://");

    sb.append(getServerName());
    int port = getServerPort();

    if (port > 0
        && port != 80
        && port != 443) {
      sb.append(":");
      sb.append(port);
    }

    sb.append(getRequestURI());

    return sb;
  }

  /**
   * @deprecated As of JSDK 2.1
   */
  @Override
  public String getRealPath(String path)
  {
    if (path == null)
      return null;
    if (path.length() > 0 && path.charAt(0) == '/')
      return _invocation.getWebApp().getRealPath(path);

    String uri = getPageURI();
    String context = getPageContextPath();
    if (context != null)
      uri = uri.substring(context.length());

    int p = uri.lastIndexOf('/');
    if (p >= 0)
      path = uri.substring(0, p + 1) + path;

    return _invocation.getWebApp().getRealPath(path);
  }

  /**
   * Returns the real path of pathInfo.
   */
  @Override
  public String getPathTranslated()
  {
    String pathInfo = getPathInfo();

    if (pathInfo == null)
      return null;
    else
      return getRealPath(pathInfo);
  }

  /**
   * Returns the current page's query string.
   */
  @Override
  public String getQueryString()
  {
    if (_invocation != null)
      return _invocation.getQueryString();
    else
      return null;
  }

  /**
   * Returns the current page's query string.
   */
  public String getPageQueryString()
  {
    return getQueryString();
  }

  //
  // header management
  //

  /**
   * Returns the first value for a request header.
   *
   * <p/>Corresponds to CGI's <code>HTTP_*</code>
   *
   * <code><pre>
   * String userAgent = request.getHeader("User-Agent");
   * </pre></code>
   *
   * @param name the header name
   * @return the header value
   */
  @Override
  public String getHeader(String name)
  {
    return _request.getHeader(name);
  }

  /**
   * Returns all the values for a request header.  In some rare cases,
   * like cookies, browsers may return multiple headers.
   *
   * @param name the header name
   * @return an enumeration of the header values.
   */
  @Override
  public Enumeration<String> getHeaders(String name)
  {
    return _request.getHeaders(name);
  }

  /**
   * Returns an enumeration of all headers sent by the client.
   */
  @Override
  public Enumeration<String> getHeaderNames()
  {
    return _request.getHeaderNames();
  }

  /**
   * Converts a header value to an integer.
   *
   * @param name the header name
   * @return the header value converted to an integer
   */
  @Override
  public int getIntHeader(String name)
  {
    return _request.getIntHeader(name);
  }

  /**
   * Converts a date header to milliseconds since the epoch.
   *
   * <pre><code>
   * long mod = request.getDateHeader("If-Modified-Since");
   * </code></pre>
   *
   * @param name the header name
   * @return the header value converted to an date
   */
  @Override
  public long getDateHeader(String name)
  {
    return _request.getDateHeader(name);
  }

  //
  // parameter/form
  //

  /**
   * Returns an enumeration of the form names.
   */
  @Override
  public Enumeration<String> getParameterNames()
  {
    if (_filledForm == null)
      _filledForm = parseQuery();

    return Collections.enumeration(_filledForm.keySet());
  }

  /**
   * Returns a map of the form.
   */
  @Override
  public Map<String,String[]> getParameterMap()
  {
    if (_filledForm == null)
      _filledForm = parseQuery();

    return Collections.unmodifiableMap(_filledForm);
  }

  /**
   * Returns the form's values for the given name.
   *
   * @param name key in the form
   * @return value matching the key
   */
  @Override
  public String []getParameterValues(String name)
  {
    if (_filledForm == null)
      _filledForm = parseQuery();

    return (String []) _filledForm.get(name);
  }

  /**
   * Returns the form primary value for the given name.
   */
  @Override
  public String getParameter(String name)
  {
    String []values = getParameterValues(name);

    if (values != null && values.length > 0)
      return values[0];
    else
      return null;
  }

  /**
   * @since Servlet 3.0
   */
  @Override
  public Collection<Part> getParts()
    throws IOException, ServletException
  {
    MultipartConfigElement multipartConfig
      = _invocation.getMultipartConfig();
    
    if (multipartConfig == null)
      throw new ServletException(L.l("multipart-form is disabled; check @MultipartConfig annotation on `{0}'.", _invocation.getServletName()));
    
    /*
    if (! getWebApp().doMultipartForm())
      throw new ServletException("multipart-form is disabled; check <multipart-form> configuration tag.");
      */

    if (! getContentType().startsWith("multipart/form-data"))
      throw new ServletException("Content-Type must be of 'multipart/form-data'.");

    if (_filledForm == null)
      _filledForm = parseQuery();

    return _parts;
  }

  Part createPart(String name, Map<String, List<String>> headers)
  {
    return new PartImpl(name, headers);
  }

  /**
   * @since Servlet 3.0
   */
  @Override
  public Part getPart(String name)
    throws IOException, ServletException
  {
    for (Part part : getParts()) {
      if (name.equals(part.getName()))
        return part;
    }

    return null;
  }

  /**
   * Parses the query, either from the GET or the post.
   *
   * <p/>The character encoding is somewhat tricky.  If it's a post, then
   * assume the encoded form uses the same encoding as
   * getCharacterEncoding().
   *
   * <p/>If the request doesn't provide the encoding, use the
   * character-encoding parameter from the webApp.
   *
   * <p/>Otherwise use the default system encoding.
   */
  private HashMapImpl<String,String[]> parseQuery()
  {
    HashMapImpl<String,String[]> form = _request.getForm();

    try {
      String query = getQueryString();
      CharSegment contentType = _request.getContentTypeBuffer();

      if (query == null && contentType == null)
        return form;

      Form formParser = _request.getFormParser();
      long contentLength = _request.getLongContentLength();

      String charEncoding = getCharacterEncoding();
      if (charEncoding == null) {
        charEncoding = (String) getAttribute(CAUCHO_CHAR_ENCODING);
        if (charEncoding == null)
          charEncoding = (String) getAttribute(CHAR_ENCODING);
        if (charEncoding == null) {
          Locale locale = (Locale) getAttribute(FORM_LOCALE);
          if (locale != null)
            charEncoding = Encoding.getMimeName(locale);
        }
      }

      if (query != null) {
        String queryEncoding = charEncoding;

        if (queryEncoding == null && getServer() != null)
          queryEncoding = getServer().getURLCharacterEncoding();

        if (queryEncoding == null)
          queryEncoding = CharacterEncoding.getLocalEncoding();

        String javaEncoding = Encoding.getJavaName(queryEncoding);

        formParser.parseQueryString(form, query, javaEncoding, true);
      }

      if (charEncoding == null)
        charEncoding = CharacterEncoding.getLocalEncoding();

      String javaEncoding = Encoding.getJavaName(charEncoding);

      MultipartConfigElement multipartConfig
        = _invocation.getMultipartConfig();

      if (contentType == null || ! "POST".equalsIgnoreCase(getMethod())) {
      }

      else if (contentType.startsWith("application/x-www-form-urlencoded")) {
        formParser.parsePostData(form, getInputStream(), javaEncoding);
      }

      else if ((getWebApp().doMultipartForm() || multipartConfig != null)
               && contentType.startsWith("multipart/form-data")) {
        int length = contentType.length();
        int i = contentType.indexOf("boundary=");

        if (i < 0)
          return form;

        long formUploadMax = getWebApp().getFormUploadMax();

        Object uploadMax = getAttribute("caucho.multipart.form.upload-max");
        if (uploadMax instanceof Number)
          formUploadMax = ((Number) uploadMax).longValue();

        // XXX: should this be an error?
        if (formUploadMax >= 0 && formUploadMax < contentLength) {
          setAttribute("caucho.multipart.form.error",
                       L.l("Multipart form upload of '{0}' bytes was too large.",
                           String.valueOf(contentLength)));
          setAttribute("caucho.multipart.form.error.size",
                       new Long(contentLength));

          return form;
        }

        long fileUploadMax = -1;

        if (multipartConfig != null) {
          formUploadMax = multipartConfig.getMaxRequestSize();
          fileUploadMax = multipartConfig.getMaxFileSize();
        }

        if (multipartConfig != null
            && formUploadMax > 0
            && formUploadMax < contentLength)
          throw new IllegalStateException(L.l(
            "multipart form data request's Content-Length '{0}' is greater then configured in @MultipartConfig.maxRequestSize value: '{1}'",
            contentLength,
            formUploadMax));

        i += "boundary=".length();
        char ch = contentType.charAt(i);
        CharBuffer boundary = new CharBuffer();
        if (ch == '\'') {
          for (i++; i < length && contentType.charAt(i) != '\''; i++)
            boundary.append(contentType.charAt(i));
        }
        else if (ch == '\"') {
          for (i++; i < length && contentType.charAt(i) != '\"'; i++)
            boundary.append(contentType.charAt(i));
        }
        else {
          for (;
               i < length && (ch = contentType.charAt(i)) != ' ' &&
                 ch != ';';
               i++) {
            boundary.append(ch);
          }
        }

        _parts = new ArrayList<Part>();

        try {
          MultipartForm.parsePostData(form,
                                      _parts,
                                      getStream(false), boundary.toString(),
                                      this,
                                      javaEncoding,
                                      formUploadMax,
                                      fileUploadMax);
        } catch (IOException e) {
          log.log(Level.FINE, e.toString(), e);
          setAttribute("caucho.multipart.form.error", e.getMessage());
        }
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return form;
  }

  //
  // session/cookie management
  //

  /**
   * Returns an array of all cookies sent by the client.
   */
  @Override
  public Cookie []getCookies()
  {
    if (_cookiesIn == null) {
      _cookiesIn = _request.getCookies();

      SessionManager sessionManager = getSessionManager();
      String sessionCookieName = getSessionCookie(sessionManager);

      for (int i = 0; i < _cookiesIn.length; i++) {
        Cookie cookie = _cookiesIn[i];

        if (cookie.getName().equals(sessionCookieName)
          && sessionManager.isSecure()) {
          cookie.setSecure(true);
          break;
        }
      }

      /*
      // The page varies depending on the presense of any cookies
      setVaryCookie(null);

      // If any cookies actually exist, the page is not anonymous
      if (_cookiesIn != null && _cookiesIn.length > 0)
        setHasCookie();
      */
    }

    if (_cookiesIn == null || _cookiesIn.length == 0)
      return null;
    else
      return _cookiesIn;
  }

  /**
   * Returns the named cookie from the browser
   */
  @Override
  public Cookie getCookie(String name)
  {
    /*
    // The page varies depending on the presense of any cookies
    setVaryCookie(name);
    */

    return findCookie(name);
  }

  private Cookie findCookie(String name)
  {
    Cookie []cookies = getCookies();

    if (cookies == null)
      return null;

    int length = cookies.length;
    for (int i = 0; i < length; i++) {
      Cookie cookie = cookies[i];

      if (cookie.getName().equals(name)) {
        setHasCookie();
        return cookie;
      }
    }

    return null;
  }

  /**
   * Returns the session id in the HTTP request.  The cookie has
   * priority over the URL.  Because the webApp might be using
   * the cookie to change the page contents, the caching sets
   * vary: JSESSIONID.
   */
  @Override
  public String getRequestedSessionId()
  {
    SessionManager manager = getSessionManager();

    if (manager != null && manager.enableSessionCookies()) {
      setVaryCookie(getSessionCookie(manager));

      String id = findSessionIdFromCookie();

      if (id != null) {
        _isSessionIdFromCookie = true;
        setHasCookie();
        return id;
      }
    }

    String id = findSessionIdFromUrl();
    if (id != null) {
      return id;
    }

    if (manager != null && manager.enableSessionCookies())
      return null;
    else
      return _request.findSessionIdFromConnection();
  }

  /**
   * Returns the session id in the HTTP request cookies.
   * Because the webApp might use the cookie to change
   * the page contents, the caching sets vary: JSESSIONID.
   */
  protected String findSessionIdFromCookie()
  {
    SessionManager manager = getSessionManager();

    if (manager == null || ! manager.enableSessionCookies())
      return null;

    Cookie cookie = getCookie(getSessionCookie(manager));

    if (cookie != null) {
      _isSessionIdFromCookie = true;
      return cookie.getValue();
    }
    else
      return null;
  }

  @Override
  public boolean isSessionIdFromCookie()
  {
    return _isSessionIdFromCookie;
  }

  @Override
  public String getSessionId()
  {
    String sessionId = getResponse().getSessionId();

    if (sessionId != null)
      return sessionId;
    else
      return getRequestedSessionId();
  }

  @Override
  public void setSessionId(String sessionId)
  {
    getResponse().setSessionId(sessionId);
  }

  /**
   * Returns the session id in the HTTP request from the url.
   */
  private String findSessionIdFromUrl()
  {
    // server/1319
    // setVaryCookie(getSessionCookie(manager));

    String id = _invocation != null ? _invocation.getSessionId() : null;
    if (id != null)
      setHasCookie();

    return id;
  }

  /**
   * Returns true if the current sessionId came from a cookie.
   */
  @Override
  public boolean isRequestedSessionIdFromCookie()
  {
    return findSessionIdFromCookie() != null;
  }

  /**
   * Returns true if the current sessionId came from the url.
   */
  @Override
  public boolean isRequestedSessionIdFromURL()
  {
    return findSessionIdFromUrl() != null;
  }

  /**
   * @deprecated
   */
  @Override
  public boolean isRequestedSessionIdFromUrl()
  {
    return isRequestedSessionIdFromURL();
  }

  /**
   * Returns the session id in the HTTP request.  The cookie has
   * priority over the URL.  Because the webApp might be using
   * the cookie to change the page contents, the caching sets
   * vary: JSESSIONID.
   */
  public String getRequestedSessionIdNoVary()
  {
    boolean varyCookies = _varyCookies;
    boolean hasCookie = _hasCookie;
    boolean privateCache = _response.getPrivateCache();

    String id = getRequestedSessionId();

    _varyCookies = varyCookies;
    _hasCookie = hasCookie;
    _response.setPrivateOrResinCache(privateCache);

    return id;
  }

  //
  // security
  //

  @Override
  protected String getRunAs()
  {
    return _runAs;
  }

  /**
   * Gets the authorization type
   */
  public String getAuthType()
  {
    Object login = getAttribute(AbstractLogin.LOGIN_NAME);

    if (login instanceof X509Certificate)
      return HttpServletRequest.CLIENT_CERT_AUTH;

    WebApp app = getWebApp();

    if (app != null && app.getLogin() != null && getUserPrincipal() != null)
      return app.getLogin().getAuthType();
    else
      return null;
  }

  /**
   * Returns the login for the request.
   */
  protected Login getLogin()
  {
    WebApp webApp = getWebApp();

    if (webApp != null)
      return webApp.getLogin();
    else
      return null;
  }
  /**
   * Returns true if any authentication is requested
   */
  @Override
  public boolean isLoginRequested()
  {
    return _isLoginRequested;
  }

  @Override
  public void requestLogin()
  {
    _isLoginRequested = true;
  }

  /**
   * @since Servlet 3.0
   */
  /*
  @Override
  public void login(String username, String password)
    throws ServletException
  {
    WebApp webApp = getWebApp();

    Authenticator auth = webApp.getConfiguredAuthenticator();

    if (auth == null)
      throw new ServletException(L.l("No authentication mechanism is configured for '{0}'", getWebApp()));

    // server/1aj0
    Login login = webApp.getLogin();

    if (login == null)
      throw new ServletException(L.l("No login mechanism is configured for '{0}'", getWebApp()));

    if (! login.isPasswordBased())
      throw new ServletException(L.l("Authentication mechanism '{0}' does not support password authentication", login));

    removeAttribute(Login.LOGIN_USER);
    removeAttribute(Login.LOGIN_PASSWORD);

    Principal principal = login.getUserPrincipal(this);

    if (principal != null)
      throw new ServletException(L.l("UserPrincipal object has already been established"));

    setAttribute(Login.LOGIN_USER, username);
    setAttribute(Login.LOGIN_PASSWORD, password);

    try {
      login.login(this, getResponse(), false);
    }
    finally {
      removeAttribute(Login.LOGIN_USER);
      removeAttribute(Login.LOGIN_PASSWORD);
    }

    principal = login.getUserPrincipal(this);

    if (principal == null)
      throw new ServletException("can't authenticate a user");
  }
  */

  /**
   * Authenticate the user.
   */
  /*
  @Override
  public boolean login(boolean isFail)
  {
    try {
      WebApp webApp = getWebApp();

      if (webApp == null) {
        if (log.isLoggable(Level.FINE))
          log.finer("authentication failed, no web-app found");

        _response.sendError(HttpServletResponse.SC_FORBIDDEN);

        return false;
      }

      // If the authenticator can find the user, return it.
      Login login = webApp.getLogin();

      if (login != null) {
        Principal user = login.login(this, getResponse(), isFail);

        return user != null;
      }
      else if (isFail) {
        if (log.isLoggable(Level.FINE))
          log.finer("authentication failed, no login module found for "
                    + webApp);

        _response.sendError(HttpServletResponse.SC_FORBIDDEN);

        return false;
      }
      else {
        // if a non-failure, then missing login is fine

        return false;
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }
  */

  /**
   * Gets the remote user from the authorization type
   */
  public String getRemoteUser()
  {
    Principal principal = getUserPrincipal();

    if (principal != null)
      return principal.getName();
    else
      return null;
  }

  /**
   * Internal logging return to get the remote user.  If the request already
   * knows the user, get it, otherwise just return null.
   */
  public String getRemoteUser(boolean create)
  {
    /*
    if (getSession(false) == null)
      return null;
    */

    Principal user = (Principal) getAttribute(AbstractLogin.LOGIN_NAME);

    if (user == null && create)
      user = getUserPrincipal();

    if (user != null)
      return user.getName();
    else
      return null;
  }

  /**
   * Logs out the principal.
   */
  public void logout()
  {
    Login login = getLogin();

    if (login != null) {
      login.logout(getUserPrincipal(), this, getResponse());
    }
  }

  /**
   * Clear the principal from the request object.
   */
  public void logoutUserPrincipal()
  {
    // XXX:
    /*
    if (_session != null)
      _session.logout();
    */
  }

  /**
   * Sets the overriding role.
   */
  public String runAs(String role)
  {
    String oldRunAs = _runAs;

    _runAs = role;

    return oldRunAs;
  }

  public void setSecure(boolean isSecure)
  {
    // server/12ds
    _isSecure = isSecure;
  }

  //
  // deprecated
  //

  public ReadStream getStream()
    throws IOException
  {
    return _request.getStream();
  }

  public ReadStream getStream(boolean isFlush)
    throws IOException
  {
    return _request.getStream(isFlush);
  }

  public int getRequestDepth(int depth)
  {
    return depth;
  }

  public void setHeader(String key, String value)
  {
    _request.setHeader(key, value);
  }

  public void setSyntheticCacheHeader(boolean isTop)
  {
    _isSyntheticCacheHeader = isTop;
  }

  public boolean isSyntheticCacheHeader()
  {
    return _isSyntheticCacheHeader;
  }

  /**
   * Called if the page depends on a cookie.  If the cookie is null, then
   * the page depends on all cookies.
   *
   * @param cookie the cookie the page depends on.
   */
  public void setVaryCookie(String cookie)
  {
    _varyCookies = true;

    // XXX: server/1315 vs 2671
    // _response.setPrivateOrResinCache(true);
  }

  /**
   * Returns true if the page depends on cookies.
   */
  public boolean getVaryCookies()
  {
    return _varyCookies;
  }

  /**
   * Set when the page actually has a cookie.
   */
  public void setHasCookie()
  {
    _hasCookie = true;

    // XXX: 1171 vs 1240
    // _response.setPrivateOrResinCache(true);
  }

  /**
   * True if this page uses cookies.
   */
  public boolean getHasCookie()
  {
    if (_hasCookie)
      return true;
    else if (_invocation != null)
      return _invocation.getSessionId() != null;
    else
      return false;
  }

  public boolean isTop()
  {
    return true;
  }

  public boolean isComet()
  {
    return _request.isCometActive();
  }

  /**
   * Adds a file to be removed at the end.
   */
  public void addCloseOnExit(Path path)
  {
    if (_closeOnExit == null)
      _closeOnExit = new ArrayList<Path>();

    _closeOnExit.add(path);
  }

  public boolean isDuplex()
  {
    return _request.isDuplex();
  }

  public void killKeepalive()
  {
    _request.killKeepalive();
  }

  public boolean isKeepaliveAllowed()
  {
    return _request.isKeepaliveAllowed();
  }

  public boolean isClientDisconnect()
  {
    return _request.isClientDisconnect();
  }

  public void clientDisconnect()
  {
    _request.clientDisconnect();
  }

  public SocketLink getConnection()
  {
    return _request.getConnection();
  }

  //
  // HttpServletRequestImpl methods
  //

  public AbstractHttpRequest getAbstractHttpRequest()
  {
    return _request;
  }

  public boolean isSuspend()
  {
    return _request.isSuspend();
  }

  public boolean hasRequest()
  {
    return _request.hasRequest();
  }

  public void setInvocation(Invocation invocation)
  {
    _invocation = invocation;
  }

  public Invocation getInvocation()
  {
    return _invocation;
  }

  public long getStartTime()
  {
    return _request.getStartTime();
  }

  public void finishInvocation()
  {
    AsyncContextImpl asyncContext = _asyncContext;

    if (asyncContext != null)
      asyncContext.onComplete();

    _request.finishInvocation();
  }

  //
  // servlet 3.0 async support
  //

  /**
   * Returns true if the request is in async.
   *
   * @since Servlet 3.0
   */
  @Override
  public boolean isAsyncStarted()
  {
    AbstractHttpRequest request = _request;
    
    return request != null && request.isCometActive();
  }

  /**
   * Returns true if the request supports async
   *
   * @since Servlet 3.0
   */
  @Override
  public boolean isAsyncSupported()
  {
    Invocation invocation = _invocation;

    if (invocation != null)
      return invocation.isAsyncSupported();
    else
      return false;
  }

  /**
   * Starts an async mode
   *
   * @since Servlet 3.0
   */
  @Override
  public AsyncContext startAsync()
  {
    return startAsync(this, _response);
  }

  /**
   * Starts an async mode
   *
   * @since Servlet 3.0
   */
  @Override
  public AsyncContext startAsync(ServletRequest request,
                                 ServletResponse response)
  {

    if (! isAsyncSupported())
      throw new IllegalStateException(L.l("The servlet '{0}' at '{1}' does not support async because the servlet or one of the filters does not support asynchronous mode.  The servlet should be annotated with a @WebServlet(asyncSupported=true) annotation or have a <async-supported> tag in the web.xml.",
                                          getServletName(), getServletPath()));

    if (_request.isCometActive()) {
      throw new IllegalStateException(L.l("startAsync may not be called twice on the same dispatch."));
    }

    boolean isOriginal = (request == this && response == _response);

    _asyncContext = new AsyncContextImpl(_request, request, response, isOriginal);

    if (_asyncTimeout > 0)
      _asyncContext.setTimeout(_asyncTimeout);

    return _asyncContext;
  }

  /**
   * Returns the async context for the request
   *
   * @since Servlet 3.0
   */
  @Override
  public AsyncContextImpl getAsyncContext()
  {
    if (_asyncContext != null)
      return _asyncContext;
    else
      throw new IllegalStateException(L.l("getAsyncContext() must be called after asyncStarted() has started a new AsyncContext."));
  }

  //
  // WebSocket
  //

  public WebSocketContext startWebSocket(WebSocketListener listener)
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " upgrade HTTP to WebSocket " + listener);

    String connection = getHeader("Connection");
    String upgrade = getHeader("Upgrade");

    if (! "WebSocket".equals(upgrade)) {
      throw new IllegalStateException(L.l("HTTP Upgrade header '{0}' must be 'WebSocket', because the WebSocket protocol requires an Upgrade: WebSocket header.",
                                          upgrade));
    }

    if (! "Upgrade".equalsIgnoreCase(connection)) {
      throw new IllegalStateException(L.l("HTTP Connection header '{0}' must be 'Upgrade', because the WebSocket protocol requires a Connection: Upgrade header.",
                                          connection));
    }

    String origin = getHeader("Origin");

    if (origin == null) {
      throw new IllegalStateException(L.l("HTTP Origin header is required, because the WebSocket protocol requires an Origin header."));
    }

    _response.setStatus(101, "Web Socket Protocol Handshake");
    _response.setHeader("Upgrade", "WebSocket");

    _response.setContentLength(0);

    StringBuilder sb = new StringBuilder();
    if (isSecure())
      sb.append("wss://");
    else
      sb.append("ws://");
    sb.append(getServerName());

    if (! isSecure() && getServerPort() != 80
        || isSecure() && getServerPort() != 443) {
      sb.append(":");
      sb.append(getServerPort());
    }

    sb.append(getContextPath());
    if (getServletPath() != null)
      sb.append(getServletPath());

    String url = sb.toString();

    _response.setHeader("WebSocket-Location", url);
    _response.setHeader("WebSocket-Origin", origin.toLowerCase());

    String protocol = getHeader("WebSocket-Protocol");

    if (protocol != null)
      _response.setHeader("WebSocket-Protocol", protocol);

    WebSocketContextImpl duplex
      = new WebSocketContextImpl(this, _response, listener);

    SocketLinkDuplexController controller = _request.startDuplex(duplex);
    duplex.setController(controller);

    try {
      duplex.onStart();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return duplex;
  }

  int getAvailable()
    throws IOException
  {
    return _request.getAvailable();
  }

  public DispatcherType getDispatcherType()
  {
    return DispatcherType.REQUEST;
  }

  @Override
  protected void finishRequest()
    throws IOException
  {
    AsyncContextImpl comet = _asyncContext;
    _asyncContext = null;

    /* server/1ld5
    if (comet != null) {
      comet.onComplete();
    }
    */

    super.finishRequest();

    // ioc/0a10
    cleanup();

    if (_closeOnExit != null) {
      for (int i = _closeOnExit.size() - 1; i >= 0; i--) {
        Path path = _closeOnExit.get(i);

        try {
          path.remove();
        } catch (Throwable e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }

    _request = null;
  }

  public void cleanup()
  {
    HashMapImpl<String,Object> attributes = _attributes;

    if (attributes != null) {
      for (Map.Entry<String,Object> entry : attributes.entrySet()) {
        Object value = entry.getValue();

        if (value instanceof ScopeRemoveListener) {
          ((ScopeRemoveListener) value).removeEvent(this, entry.getKey());
        }
      }
    }
  }

  //
  // XXX: unsorted
  //

  /**
   * Returns the servlet name.
   */
  public String getServletName()
  {
    if (_invocation != null) {
      return _invocation.getServletName();
    }
    else
      return null;
  }

  public final Server getServer()
  {
    return _request.getServer();
  }

  /**
   * Returns the invocation's webApp.
   */
  public final WebApp getWebApp()
  {
    if (_invocation != null)
      return _invocation.getWebApp();
    else
      return null;
  }

  public boolean isClosed()
  {
    return _request == null;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _request + "]";
  }

  public class PartImpl implements Part {
    private String _name;
    private Map<String, List<String>> _headers;
    private Object _value;
    private Path _newPath;

    private PartImpl(String name, Map<String, List<String>> headers)
    {
      _name = name;
      _headers = headers;
    }

    public void delete()
      throws IOException
    {
      if (_newPath != null)
        _newPath.remove();

      Object value = getValue();

      if (! (value instanceof FilePath))
        throw new IOException(L.l("Part.delete() is not applicable to part '{0}':'{1}'", _name, value));

      ((FilePath)value).remove();
    }

    public String getContentType()
    {
      String[] value = _filledForm.get(_name + ".content-type");

      if (value != null && value.length > 0)
        return value[0];

      return null;
    }

    public String getHeader(String name)
    {
      List<String> values = _headers.get(name);

      if (values != null && values.size() > 0)
        return values.get(0);

      return null;
    }

    public Collection<String> getHeaderNames()
    {
      return _headers.keySet();
    }

    public Collection<String> getHeaders(String name)
    {
      return _headers.get(name);
    }

    public InputStream getInputStream()
      throws IOException
    {
      Object value = getValue();

      if (value instanceof FilePath)
        return ((FilePath) value).openRead();

      ByteArrayInputStream is
        = new ByteArrayInputStream(value.toString().getBytes(UTF8));

      return is;
    }


    public String getName()
    {
      return _name;
    }

    public long getSize()
    {
      Object value = getValue();

      if (value instanceof FilePath) {
        return ((Path) value).getLength();
      }
      else if (value instanceof String) {
        return -1;
      }
      else if (value == null) {
        return -1;
      }
      else {
        log.finest(L.l("Part.getSize() is not applicable to part'{0}':'{1}'",
                       _name, value));

        return -1;
      }
    }

    @Override
    public void write(String fileName)
      throws IOException
    {
      if (_newPath != null)
        throw new IOException(L.l(
          "Contents of part '{0}' has already been written to '{1}'",
          _name,
          _newPath));

      Path path;

      Object value = getValue();

      if (! (value instanceof FilePath))
        throw new IOException(L.l(
          "Part.write() is not applicable to part '{0}':'{1}'",
          _name,
          value));
      else
        path = (Path) value;

      MultipartConfigElement mc = _invocation.getMultipartConfig();
      String location = mc.getLocation().replace('\\', '/');
      fileName = fileName.replace('\\', '/');

      String file;

      if (location.charAt(location.length() -1) != '/' && fileName.charAt(fileName.length() -1) != '/')
        file = location + '/' + fileName;
      else
        file = location + fileName;

      _newPath = Vfs.lookup(file);

      if (_newPath.exists())
        throw new IOException(L.l("File '{0}' already exists.", _newPath));

      Path parent = _newPath.getParent();

      if (! parent.exists())
        if (! parent.mkdirs())
          throw new IOException(L.l("Unable to create path '{0}'. Check permissions.", parent));

      if (! path.renameTo(_newPath)) {
        WriteStream out = null;

        try {
          out = _newPath.openWrite();

          path.writeToStream(out);

          out.flush();

          out.close();
        } catch (IOException e) {
          log.log(Level.SEVERE, L.l("Cannot write contents of '{0}' to '{1}'", path, _newPath), e);

          throw e;
        } finally {
          if (out != null)
            out.close();
        }
      }
    }

    public Object getValue()
    {
      if (_value != null)
        return _value;

      String []values = _filledForm.get(_name + ".file");

      if (values != null && values.length > 0) {
        _value = Vfs.lookup(values[0]);
      } else {
        values = _filledForm.get(_name);

        if (values != null && values.length > 0)
          _value = values[0];
      }

      return _value;
    }
  }

  static class WebSocketContextImpl
    implements WebSocketContext, SocketLinkDuplexListener
  {
    private final HttpServletRequestImpl _request;
    private final WebSocketListener _listener;

    private SocketLinkDuplexController _controller;

    WebSocketContextImpl(HttpServletRequestImpl request,
                         HttpServletResponseImpl response,
                         WebSocketListener listener)
    {
      _request = request;
      _listener = listener;
    }

    public void setController(SocketLinkDuplexController controller)
    {
      _controller = controller;
    }

    public void setTimeout(long timeout)
    {
      _controller.setIdleTimeMax(timeout);
    }

    public long getTimeout()
    {
      return _controller.getIdleTimeMax();
    }

    public InputStream getInputStream()
      throws IOException
    {
      return _controller.getReadStream();
    }

    public OutputStream getOutputStream()
      throws IOException
    {
      return _controller.getWriteStream();
    }

    public void complete()
    {
      _controller.complete();
    }

    void onStart()
      throws IOException
    {
      _listener.onStart(this);
    }

    public void onRead(SocketLinkDuplexController duplex)
      throws IOException
    {
      do {
        _listener.onRead(this);
      } while (_request.getAvailable() > 0);
    }

    public void onComplete(SocketLinkDuplexController duplex)
      throws IOException
    {
      _listener.onComplete(this);
    }

    public void onTimeout(SocketLinkDuplexController duplex)
      throws IOException
    {
      _listener.onTimeout(this);
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _listener + "]";
    }

    /* (non-Javadoc)
     * @see com.caucho.network.listen.SocketLinkDuplexListener#onStart(com.caucho.network.listen.SocketLinkDuplexController)
     */
    @Override
    public void onStart(SocketLinkDuplexController context) throws IOException
    {
      // TODO Auto-generated method stub
      
    }
  }
}
