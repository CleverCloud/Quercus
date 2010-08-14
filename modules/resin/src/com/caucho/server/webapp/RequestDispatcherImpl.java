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

package com.caucho.server.webapp;

import com.caucho.server.dispatch.Invocation;
import com.caucho.server.http.AbstractHttpResponse;
import com.caucho.server.http.AbstractResponseStream;
import com.caucho.server.http.CauchoRequest;
import com.caucho.server.http.CauchoResponse;
import com.caucho.server.http.HttpServletRequestImpl;
import com.caucho.server.http.HttpServletResponseImpl;
import com.caucho.util.L10N;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Logger;

public class RequestDispatcherImpl implements RequestDispatcher {
  private final static Logger log
    = Logger.getLogger(RequestDispatcherImpl.class.getName());

  private static final L10N L = new L10N(RequestDispatcherImpl.class);

  static final int MAX_DEPTH = 64;

  private static final String REQUEST_URI
    = "javax.servlet.include.request_uri";
  private static final String CONTEXT_PATH
    = "javax.servlet.include.context_path";
  private static final String SERVLET_PATH
    = "javax.servlet.include.servlet_path";
  private static final String PATH_INFO
    = "javax.servlet.include.path_info";
  private static final String QUERY_STRING
    = "javax.servlet.include.query_string";

  private static final String FWD_REQUEST_URI =
    "javax.servlet.forward.request_uri";
  private static final String FWD_CONTEXT_PATH =
    "javax.servlet.forward.context_path";
  private static final String FWD_SERVLET_PATH =
    "javax.servlet.forward.servlet_path";
  private static final String FWD_PATH_INFO =
    "javax.servlet.forward.path_info";
  private static final String FWD_QUERY_STRING =
    "javax.servlet.forward.query_string";

  // WebApp the request dispatcher was called from
  private WebApp _webApp;
  private Invocation _includeInvocation;
  private Invocation _forwardInvocation;
  private Invocation _errorInvocation;
  private Invocation _dispatchInvocation;
  private boolean _isLogin;

  RequestDispatcherImpl(Invocation includeInvocation,
                        Invocation forwardInvocation,
                        Invocation errorInvocation,
                        Invocation dispatchInvocation,
                        WebApp webApp)
  {
    _includeInvocation = includeInvocation;
    _forwardInvocation = forwardInvocation;
    _errorInvocation = errorInvocation;
    _dispatchInvocation = dispatchInvocation;

    _webApp = webApp;
  }

  public void setLogin(boolean isLogin)
  {
    _isLogin = isLogin;
  }

  public boolean isModified()
  {
    return _includeInvocation.isModified();
  }
  
  public Invocation getAsyncInvocation()
  {
    return _dispatchInvocation;
  }

  @Override
  public void forward(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    forward(request, response,
            null, _forwardInvocation, DispatcherType.FORWARD);
  }

  public void dispatchResume(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    dispatchResume((HttpServletRequest) request, (HttpServletResponse) response,
                  _forwardInvocation);
  }

  public void error(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    forward(request, response, "error", _errorInvocation, DispatcherType.ERROR);
  }

  public void dispatch(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    forward(request, response,
            "error", _dispatchInvocation, DispatcherType.REQUEST);
  }

  /**
   * Forwards the request to the servlet named by the request dispatcher.
   *
   * @param topRequest the servlet request.
   * @param topResponse the servlet response.
   * @param method special to tell if from error.
   */
  public void forward(ServletRequest topRequest, ServletResponse topResponse,
                      String method, Invocation invocation,
                      DispatcherType type)
    throws ServletException, IOException
  {
    CauchoResponse cauchoRes = null;

    boolean allowForward = _webApp.isAllowForwardAfterFlush();

    if (topResponse instanceof CauchoResponse) {
      cauchoRes = (CauchoResponse) topResponse;

      cauchoRes.setForwardEnclosed(! allowForward);
    }

    // jsp/15m8
    if (topResponse.isCommitted() && method == null && ! allowForward) {
      IllegalStateException exn;
      exn = new IllegalStateException(L.l("forward() not allowed after buffer has committed."));

      if (cauchoRes == null || ! cauchoRes.hasError()) {
        if (cauchoRes != null)
          cauchoRes.setHasError(true);
        throw exn;
      }

      _webApp.log(exn.getMessage(), exn);

      return;
    } else if ("error".equals(method) || (method == null)) {
      // server/10yg
      
      // } else if ("error".equals(method) || (method == null && ! allowForward)) {
      
      topResponse.resetBuffer();

      if (cauchoRes != null) {
        // server/10yh
        // ServletResponse resp = cauchoRes.getResponse();
        ServletResponse resp = cauchoRes;

        while (resp != null) {
          if (allowForward && resp instanceof IncludeResponse) {
            // server/10yh
            break;
          }
          else if (resp instanceof CauchoResponse) {
            CauchoResponse cr = (CauchoResponse) resp;
            cr.resetBuffer();
            resp = cr.getResponse();
          } else {
            resp.resetBuffer();

            resp = null;
          }
        }
      }
    }

    HttpServletRequest parentReq;
    ServletRequestWrapper reqWrapper = null;
    if (topRequest instanceof ServletRequestWrapper) {
      // reqWrapper = (ServletRequestWrapper) req;

      ServletRequest request = topRequest; // reqWrapper.getRequest();

      while (request instanceof ServletRequestWrapper) {
        reqWrapper = (ServletRequestWrapper) request;
      
        request = ((ServletRequestWrapper) request).getRequest();
      }

      parentReq = (HttpServletRequest) request;
    } else if (topRequest instanceof HttpServletRequest) {
      parentReq = (HttpServletRequest) topRequest;
    } else {
      throw new IllegalStateException(L.l(
        "expected instance of ServletRequest at `{0}'", topRequest));
    }

    HttpServletResponse parentRes;
    ServletResponseWrapper resWrapper = null;

    if (topResponse instanceof ServletResponseWrapper) {
      ServletResponse response = topResponse;

      while (response instanceof ServletResponseWrapper) {
        resWrapper = (ServletResponseWrapper) response;

        response = ((ServletResponseWrapper) response).getResponse();
      }

      parentRes = (HttpServletResponse) response;
    } else if (topResponse instanceof HttpServletResponse) {
      parentRes = (HttpServletResponse) topResponse;
    } else {
      throw new IllegalStateException(L.l(
        "expected instance of ServletResponse at `{0}'", topResponse));
    }

    ForwardRequest subRequest;

    if (_isLogin)
      subRequest = new LoginRequest(parentReq, parentRes, invocation);
    else if (type == DispatcherType.ERROR)
      subRequest = new ErrorRequest(parentReq, parentRes, invocation);
    else
      subRequest = new ForwardRequest(parentReq, parentRes, invocation);

    // server/10ye
    if (subRequest.getRequestDepth(0) > MAX_DEPTH)
      throw new ServletException(L.l("too many servlet forwards `{0}'", parentReq.getServletPath()));

    ForwardResponse subResponse = subRequest.getResponse();

    if (reqWrapper != null) {
      reqWrapper.setRequest(subRequest);
    }
    else {
      topRequest = subRequest;
    }

    if (resWrapper != null) {
      resWrapper.setResponse(subResponse);
    }
    else {
      topResponse = subResponse;
    }
    
    boolean isValid = false;

    subRequest.startRequest();

    try {
      invocation.service(topRequest, topResponse);

      isValid = true;
    } finally {
      if (reqWrapper != null)
        reqWrapper.setRequest(parentReq);
      
      if (resWrapper != null)
        resWrapper.setResponse(parentRes);

      subRequest.finishRequest(isValid);
      
      // server/106r, ioc/0310
      if (isValid) {
        finishResponse(topResponse);
      }
    }
  }

  private void finishResponse(ServletResponse res)
    throws ServletException, IOException
  {
    if (_webApp.isAllowForwardAfterFlush()) {
      //
    } else {
      if (res instanceof CauchoResponse) {
        CauchoResponse cauchoResponse = (CauchoResponse) res;
        cauchoResponse.close();

        ServletResponse resp = cauchoResponse.getResponse();

        while(resp != null) {
          if (resp instanceof CauchoResponse) {
            CauchoResponse cr = (CauchoResponse)resp;
            cr.close();
            resp = cr.getResponse();
          } else {
            resp = null;
          }
        }
      } else {
        try {
          OutputStream os = res.getOutputStream();
          os.close();
        } catch (Exception e) {
        }

        try {
          PrintWriter out = res.getWriter();
          out.close();
        } catch (Exception e) {
        }
      }
    }
  }
  
  public void include(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    include(request, response, null);
  }

  /**
   * Include a request into the current page.
   */
  public void include(ServletRequest topRequest, ServletResponse topResponse,
                      String method)
    throws ServletException, IOException
  {
    Invocation invocation = _includeInvocation;

    HttpServletRequest parentReq;
    ServletRequestWrapper reqWrapper = null;

    if (topRequest instanceof ServletRequestWrapper) {
      ServletRequest request = topRequest;
      
      while (request instanceof ServletRequestWrapper) {
        reqWrapper = (ServletRequestWrapper) request;
        
        request = ((ServletRequestWrapper) request).getRequest();
      }

      parentReq = (HttpServletRequest) request;
    } else if (topRequest instanceof HttpServletRequest) {
      parentReq = (HttpServletRequest) topRequest;
    } else {
      throw new IllegalStateException(L.l(
        "expected instance of ServletRequestWrapper at `{0}'", topResponse));
    }

    HttpServletResponse parentRes;
    ServletResponseWrapper resWrapper = null;

    if (topResponse instanceof ServletResponseWrapper) {
      ServletResponse response = topResponse;
      
      while (response instanceof ServletResponseWrapper) {
        resWrapper = (ServletResponseWrapper) response;
        
        response = ((ServletResponseWrapper) response).getResponse();
      }

      parentRes = (HttpServletResponse) response;
    } else if (topResponse instanceof HttpServletResponse) {
      parentRes = (HttpServletResponse) topResponse;
    } else {
      throw new IllegalStateException(L.l(
        "expected instance of ServletResponse at `{0}'", topResponse));
    }

    IncludeRequest subRequest
      = new IncludeRequest(parentReq, parentRes, invocation);
    
    // server/10yf, jsp/15di
    if (subRequest.getRequestDepth(0) > MAX_DEPTH)
      throw new ServletException(L.l("too many servlet includes `{0}'", parentReq.getServletPath()));

    IncludeResponse subResponse = subRequest.getResponse();

    if (reqWrapper != null) {
      reqWrapper.setRequest(subRequest);
    }
    else {
      topRequest = subRequest;
    }

    if (resWrapper != null) {
      resWrapper.setResponse(subResponse);
    }
    else {
      topResponse = subResponse;
    }
    
    // jsp/15lf, jsp/17eg - XXX: integrated with ResponseStream?
    // res.flushBuffer();

    subRequest.startRequest();

    try {
      invocation.service(topRequest, topResponse);
    } finally {
      if (reqWrapper != null)
        reqWrapper.setRequest(parentReq);
      
      if (resWrapper != null)
        resWrapper.setResponse(parentRes);
      
      subRequest.finishRequest();
    }
  }

  /**
   * Dispatch the async resume request to the servlet
   * named by the request dispatcher.
   *
   * @param request the servlet request.
   * @param response the servlet response.
   * @param invocation current invocation
   */
  public void dispatchResume(HttpServletRequest request,
                             HttpServletResponse response,
                             Invocation invocation)
    throws ServletException, IOException
  {
    HttpServletRequestWrapper parentRequest = null;
    HttpServletRequestImpl bottomRequest = null;
    HttpServletResponseImpl bottomResponse = null;
    
    HttpServletRequest req = request;
    while (req != null && req instanceof HttpServletRequestWrapper) {
      parentRequest = (HttpServletRequestWrapper) req;
      
      req = (HttpServletRequest) parentRequest.getRequest();
    }
    
    if (! (req instanceof HttpServletRequestImpl)) {
      throw new IllegalStateException(L.l("Wrapped async requests must use HttpServletRequestWrapper around the original request"));
    }
    
    bottomRequest = (HttpServletRequestImpl) req;
    
    HttpServletResponse res = response;
    while (res != null && res instanceof HttpServletResponseWrapper) {
      HttpServletResponseWrapper parentResponse
        = (HttpServletResponseWrapper) res;
      
      res = (HttpServletResponse) parentResponse.getResponse();
    }
    
    if (! (res instanceof HttpServletResponseImpl)) {
      throw new IllegalStateException(L.l("Wrapped async requests must use HttpServletRequestWrapper around the original request"));
    }
    
    bottomResponse = (HttpServletResponseImpl) res;
    
    AsyncRequest asyncRequest
      = new AsyncRequest(bottomRequest, bottomResponse, invocation);
    
    if (parentRequest != null) {
      parentRequest.setRequest(asyncRequest);
    }
    else {
      request = asyncRequest;
    }

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      invocation.service(request, response);
    } finally {
      // subRequest.finishRequest();

      thread.setContextClassLoader(oldLoader);
    }
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _dispatchInvocation.getRawURI() + "]");
  }
}
