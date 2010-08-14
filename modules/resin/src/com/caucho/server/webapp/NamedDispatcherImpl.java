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

package com.caucho.server.webapp;

import com.caucho.server.http.CauchoResponse;
import com.caucho.server.http.RequestAdapter;
import com.caucho.util.L10N;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

class NamedDispatcherImpl implements RequestDispatcher {

  private static final L10N L = new L10N(NamedDispatcherImpl.class);

  private WebApp _webApp;
  
  private FilterChain _includeFilterChain;  
  private FilterChain _forwardFilterChain;  
  
  private String _queryString;

  NamedDispatcherImpl(FilterChain includeFilterChain,
                      FilterChain forwardFilterChain,
                      String queryString, WebApp webApp)
  {
    _includeFilterChain = includeFilterChain;
    _forwardFilterChain = forwardFilterChain;
    _queryString = queryString;
    _webApp = webApp;
  }

  public void include(ServletRequest topRequest, ServletResponse topResponse)
    throws IOException, ServletException
  {
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

    RequestAdapter subRequest = RequestAdapter.create();
    subRequest.init(parentReq, parentRes, _webApp);

    DispatchResponse subResponse = new DispatchResponse(parentRes);
    subResponse.init(parentRes);

    if (reqWrapper != null) {
      reqWrapper.setRequest(subRequest);
    } else {
      topRequest = subRequest;
    }

    if (resWrapper != null) {
      resWrapper.setResponse(subResponse);
    } else {
      topResponse = subResponse;
    }

    try {
      _includeFilterChain.doFilter(topRequest, topResponse);
    } finally {
      subResponse.finish();
      RequestAdapter.free(subRequest);

      if (reqWrapper != null)
        reqWrapper.setRequest(parentReq);

      if (resWrapper != null)
        resWrapper.setResponse(parentRes);
    }

    //_includeFilterChain.doFilter(req, new DispatchResponse(res));

    //AbstractResponseStream s = res.getResponseStream();
    // s.setDisableClose(true);

    /* XXX:
    DispatchResponse subResponse = DispatchResponse.createDispatch();
    // XXX: subResponse.init(req);
    subResponse.setNextResponse(res);
    // subResponse.init(req, s);
    subResponse.startRequest(null);
    
    try {
      _includeFilterChain.doFilter(req, subResponse);
    } finally {
      subResponse.finishInvocation();
      subResponse.finishRequest();
    }

    if (reqAdapt != null)
      RequestAdapter.free(reqAdapt);

    DispatchResponse.free(subResponse);
    */
  }

  /**
   * Forward the request to the named servlet.
   */
  public void forward(ServletRequest req, ServletResponse res)
    throws ServletException, IOException
  {
    res.resetBuffer();
    
    res.setContentLength(-1);

    _forwardFilterChain.doFilter(req, res);

    // this is not in a finally block so we can return a real error message
    // if it's not handled.
    // server/1328, server/125i
    if (res instanceof CauchoResponse) {
      CauchoResponse cRes = (CauchoResponse) res;
      
      cRes.close();
    }
    else {
        try {
          OutputStream os = res.getOutputStream();
          if (os != null)
            os.close();
        } catch (IllegalStateException e) {
        }

        try {
          PrintWriter out = res.getWriter();
          if (out != null)
            out.close();
        } catch (IllegalStateException e1) {
        }

    }

    /*
    ServletResponse ptr = res;
    while (ptr instanceof HttpServletResponseWrapper) {
      ptr = ((HttpServletResponseWrapper) ptr).getResponse();

      if (ptr instanceof AbstractHttpResponse)
        ((AbstractHttpResponse) ptr).finish();
    }
    */

    /*
    if (res instanceof AbstractHttpResponse)
      ((AbstractHttpResponse) res).finish(true);

    if (res instanceof Response)
      ((Response) res).finish(true);
    */
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _includeFilterChain + "]";
  }
}

