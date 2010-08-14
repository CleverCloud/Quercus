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
 * @author Emil Ong
 */

package com.caucho.soa.rest;

import com.caucho.jaxb.JAXBUtil;
import com.caucho.util.URLUtil;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public abstract class RestProxy implements InvocationHandler {
  private static final Logger log = Logger.getLogger(RestProxy.class.getName());

  private RestEncoding _defaultRestEncoding = RestEncoding.QUERY;
  private String _url;
  protected Class _api;

  public RestProxy(Class api, String url)
  {
    _api = api;
    _url = url;

    if (_api.isAnnotationPresent(RestService.class)) {
      RestService restService = 
        (RestService) _api.getAnnotation(RestService.class);

      _defaultRestEncoding = restService.encoding();
    }
  }

  public Object invoke(Object proxy, Method method, Object[] args)
    throws Throwable
  {
    String httpMethod = "GET";

    if (method.isAnnotationPresent(Delete.class))
      httpMethod = "DELETE";

    if (method.isAnnotationPresent(Get.class))
      httpMethod = "GET";

    if (method.isAnnotationPresent(Post.class))
      httpMethod = "POST";

    if (method.isAnnotationPresent(Put.class))
      httpMethod = "PUT";

    if (method.isAnnotationPresent(Head.class))
      httpMethod = "HEAD";

    // Check annotations
    String methodName = method.getName();
    RestEncoding restEncoding = _defaultRestEncoding;

    if (method.isAnnotationPresent(WebMethod.class)) {
      WebMethod webMethod = (WebMethod) method.getAnnotation(WebMethod.class);

      if (webMethod.operationName().length() > 0)
        methodName = webMethod.operationName();
    }

    if (method.isAnnotationPresent(RestMethod.class)) {
      RestMethod restMethod = 
        (RestMethod) method.getAnnotation(RestMethod.class);

      if (restMethod.operationName().length() > 0)
        methodName = restMethod.operationName();

      if (restMethod.encoding() != RestEncoding.UNSET)
        restEncoding = restMethod.encoding();
    }

    // Build the url

    StringBuilder urlBuilder = new StringBuilder(_url);
    StringBuilder queryBuilder = new StringBuilder();
    
    switch (restEncoding) {
      case PATH:
        if (! _url.endsWith("/"))
          urlBuilder.append("/");

        urlBuilder.append(methodName);
        urlBuilder.append("/");
        break;
      case QUERY:
        queryBuilder.append("method=");
        queryBuilder.append(methodName);
        break;
    }

    ArrayList<Object> postValues = new ArrayList<Object>();
    HashMap<String,String> headers = new HashMap<String,String>();

    if (args != null) {
      Class[] parameterTypes = method.getParameterTypes();
      Annotation[][] annotations = method.getParameterAnnotations();

      for (int i = 0; i < parameterTypes.length; i++) {
        RestParam.Source source = RestParam.Source.QUERY;
        String key = "arg" + i;

        for (int j = 0; j < annotations[i].length; j++) {
          if (annotations[i][j].annotationType().equals(RestParam.class)) {
            RestParam restParam = (RestParam) annotations[i][j];
            source = restParam.source();
          }
          else if (annotations[i][j].annotationType().equals(WebParam.class)) {
            WebParam webParam = (WebParam) annotations[i][j];

            if (! "".equals(webParam.name()))
              key = webParam.name();
          }
        }

        switch (source) {
          case PATH:
            urlBuilder.append(URLUtil.encodeURL(args[i].toString()));
            urlBuilder.append('/');
            break;
          case QUERY:
            if (queryBuilder.length() > 0)
              queryBuilder.append('&');

            queryBuilder.append(URLUtil.encodeURL(key));
            queryBuilder.append('=');
            queryBuilder.append(URLUtil.encodeURL(args[i].toString()));
            break;
          case POST: 
            postValues.add(args[i]);
            break;
          case HEADER:
            headers.put(key, args[i].toString());
            break;
        }
      }
    }

    if (queryBuilder.length() > 0) {
      urlBuilder.append('?');
      urlBuilder.append(queryBuilder);
    }

    URL url = new URL(urlBuilder.toString());
    URLConnection connection = url.openConnection();

    if (connection instanceof HttpURLConnection) {
      HttpURLConnection httpConnection = (HttpURLConnection) connection;

      try {
        httpConnection.setRequestMethod(httpMethod);
        httpConnection.setDoInput(true);

        if (postValues.size() > 0) {
          httpConnection.setDoOutput(true);

          OutputStream out = httpConnection.getOutputStream();

          writePostData(out, postValues);

          out.flush();
        }

        int code = httpConnection.getResponseCode();

        if (code == 200) {
          if (method.getReturnType() == null)
            return null;

          return readResponse(httpConnection.getInputStream());
        }
        else {
          log.finer("request failed: " + httpConnection.getResponseMessage());

          throw new RestException(httpConnection.getResponseMessage());
        }
      }
      finally {
        httpConnection.disconnect();
      }
    }
    else
      throw new RestException();
  }

  protected abstract void writePostData(OutputStream out, 
                                        ArrayList<Object> postValues)
    throws IOException, RestException;

  protected abstract Object readResponse(InputStream in)
    throws IOException, RestException;
}
