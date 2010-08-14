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

import com.caucho.server.util.CauchoSystem;
import com.caucho.soa.servlet.ProtocolServlet;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.stream.XMLStreamWriterImpl;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A binding for REST services.
 */
public abstract class RestProtocolServlet extends GenericServlet
  implements ProtocolServlet
{
  private static final Logger log = 
    Logger.getLogger(RestProtocolServlet.class.getName());

  public static final String DELETE = "DELETE";
  public static final String GET = "GET";
  public static final String HEAD = "HEAD";
  public static final String POST = "POST";
  public static final String PUT = "PUT";

  private Map<String,Map<String,Method>> _methods 
    = new HashMap<String,Map<String,Method>>();
  
  private HashMap<String,Method> _defaultMethods 
    = new HashMap<String,Method>();

  protected Object _service;

  public RestProtocolServlet()
  {
  }
  
  public void setService(Object service)
  {
    _service = service;
  }

  public void init()
    throws ServletException
  {
    try {
      Class cl = _service.getClass();

      if (cl.isAnnotationPresent(WebService.class)) {
        WebService webService
          = (WebService) cl.getAnnotation(WebService.class);

        String endpoint = webService.endpointInterface();

        if (endpoint != null && ! "".equals(endpoint))
          cl = CauchoSystem.loadClass(webService.endpointInterface());
      }

      _methods.put(DELETE, new HashMap<String,Method>());
      _methods.put(GET, new HashMap<String,Method>());
      _methods.put(HEAD, new HashMap<String,Method>());
      _methods.put(POST, new HashMap<String,Method>());
      _methods.put(PUT, new HashMap<String,Method>());

      for (Method method : cl.getMethods()) {
        if (method.getDeclaringClass().equals(Object.class))
          continue;

        int modifiers = method.getModifiers();

        // Allow abstract for interfaces
        if (Modifier.isStatic(modifiers)
            || Modifier.isFinal(modifiers)
            || ! Modifier.isPublic(modifiers))
          continue;

        String methodName = method.getName();

        if (method.isAnnotationPresent(WebMethod.class)) {
          WebMethod webMethod = 
            (WebMethod) method.getAnnotation(WebMethod.class);

          if (! "".equals(webMethod.operationName()))
            methodName = webMethod.operationName();
        }

        if (method.isAnnotationPresent(RestMethod.class)) {
          RestMethod restMethod = 
            (RestMethod) method.getAnnotation(RestMethod.class);

          if (! "".equals(restMethod.operationName()))
            methodName = restMethod.operationName();
        }

        boolean hasHTTPMethod = false;

        if (method.isAnnotationPresent(Delete.class)) {
          if (_methods.get(DELETE).containsKey(methodName)) {
            throw new UnsupportedOperationException("Overloaded method: " + 
                method.getName());
          }

          _methods.get(DELETE).put(methodName, method);

          hasHTTPMethod = true;
        }

        if (method.isAnnotationPresent(Get.class)) {
          if (_methods.get(GET).containsKey(methodName)) {
            throw new UnsupportedOperationException("Overloaded method: " + 
                method.getName());
          }

          _methods.get(GET).put(methodName, method);

          hasHTTPMethod = true;
        }

        if (method.isAnnotationPresent(Post.class)) {
          if (_methods.get(POST).containsKey(methodName)) {
            throw new UnsupportedOperationException("Overloaded method: " + 
                method.getName());
          }

          _methods.get(POST).put(methodName, method);

          hasHTTPMethod = true;
        }

        if (method.isAnnotationPresent(Put.class)) {
          if (_methods.get(PUT).containsKey(methodName)) {
            throw new UnsupportedOperationException("Overloaded method: " + 
                method.getName());
          }

          _methods.get(PUT).put(methodName, method);

          hasHTTPMethod = true;
        }

        if (method.isAnnotationPresent(Head.class)) {
          if (_methods.get(HEAD).containsKey(methodName)) {
            throw new UnsupportedOperationException("Overloaded method: " + 
                method.getName());
          }

          _methods.get(HEAD).put(methodName, method);

          hasHTTPMethod = true;
        }

        if (! hasHTTPMethod) {
          if (_defaultMethods.containsKey(methodName)) {
            throw new UnsupportedOperationException("Overloaded method: " + 
                method.getName());
          }

          _defaultMethods.put(methodName, method);
        }
      }
    }
    catch (Exception e) {
      throw new ServletException(e);
    }
  }

  public void service(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    
    Map<String,String> queryArguments = new HashMap<String,String>();

    if (req.getQueryString() != null)
      queryToMap(req.getQueryString(), queryArguments);

    String[] pathArguments = null;

    if (req.getPathInfo() != null) {
      String pathInfo = req.getPathInfo();

      // remove the initial and final slashes
      int startPos = 0;
      int endPos = pathInfo.length();

      if (pathInfo.length() > 0 && pathInfo.charAt(0) == '/')
        startPos = 1;

      if (pathInfo.length() > startPos && 
          pathInfo.charAt(pathInfo.length() - 1) == '/')
        endPos = pathInfo.length() - 1;

      pathInfo = pathInfo.substring(startPos, endPos);

      pathArguments = pathInfo.split("/");

      if (pathArguments.length == 1 && pathArguments[0].length() == 0)
        pathArguments = new String[0];
    }
    else
      pathArguments = new String[0];

    try {
      invoke(_service, req.getMethod(), pathArguments, queryArguments,
             req, req.getInputStream(), res.getOutputStream());
    } 
    catch (NoSuchMethodException e) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
    catch (Throwable e) {
      throw new ServletException(e);
    }
  }

  private static void queryToMap(String query, 
                                 Map<String,String> queryArguments)
  {
    String[] entries = query.split("&");

    for (String entry : entries) {
      if (entry.indexOf("=") < 0)
        continue;

      String[] nameValue = entry.split("=", 2);

      queryArguments.put(nameValue[0], nameValue[1]);
    }
  }

  private void invoke(Object object,
                      String httpMethod,
                      String[] pathArguments,
                      Map<String,String> queryArguments,
                      HttpServletRequest req,
                      InputStream postData,
                      OutputStream out)
    throws Throwable
  {
    int pathIndex = 0;
    boolean pathMethod = false;

    // Two special approaches: path and query
    //
    // Path takes the first part of the path as the method name
    //
    // Query checks for /?method=myMethod in the query part
    //
    // Query overrides path since it's more explicit
    
    String methodName = queryArguments.get("method");

    if ((methodName == null) && (pathArguments.length > 0)) {
      methodName = pathArguments[0];

      if (methodName != null)
        pathMethod = true;
    }

    // First, look by http method and method name
    // This may hit the default method since methodName can be null
    Method method = _methods.get(httpMethod).get(methodName);

    // next, check for a default method, ignoring http method
    if (method == null)
      method = _defaultMethods.get(methodName);

    // finally, check for a completely default method
    if (method == null) {
      method = _defaultMethods.get(null);

      pathMethod = false;
    }

    if (method == null)
      throw new NoSuchMethodException(methodName);

    if (pathMethod)
      pathIndex = 1;

    // Construct the arguments for the invocation
    ArrayList arguments = new ArrayList();

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
          {
            String arg = null;

            if (pathIndex < pathArguments.length)
              arg = pathArguments[pathIndex++];

            arguments.add(stringToType(parameterTypes[i], arg));
            // XXX var args
          }
          break;
        case QUERY:
          arguments.add(stringToType(parameterTypes[i], 
                                     queryArguments.get(key)));
          break;
        case POST: 
          arguments.add(readPostData(postData));
          break;
        case HEADER:
          arguments.add(stringToType(parameterTypes[i], req.getHeader(key)));
          break;
      }
    }

    Object result = method.invoke(object, arguments.toArray());
    
    if (result != null)
      writeResponse(out, result);
  }

  protected abstract Object readPostData(InputStream in)
    throws IOException, RestException;

  protected abstract void writeResponse(OutputStream out, Object obj)
    throws IOException, RestException;

  private static Object stringToType(Class type, String arg)
    throws Throwable
  {
    if (arg == null) {
      return null;
    } 
    else if (type.equals(boolean.class)) {
      return new Boolean(arg);
    } 
    else if (type.equals(Boolean.class)) {
      return new Boolean(arg);
    } 
    else if (type.equals(byte.class)) {
      return new Byte(arg);
    } 
    else if (type.equals(Byte.class)) {
      return new Byte(arg);
    } 
    else if (type.equals(char.class)) {
      if (arg.length() != 1) {
        throw new IllegalArgumentException("Cannot convert String to type " +
                                           type.getName());
      }

      return new Character(arg.charAt(0));
    } 
    else if (type.equals(Character.class)) {
      if (arg.length() != 1) {
        throw new IllegalArgumentException("Cannot convert String to type " +
                                           type.getName());
      }

      return new Character(arg.charAt(0));
    } 
    else if (type.equals(double.class)) {
      return new Double(arg);
    } 
    else if (type.equals(Double.class)) {
      return new Double(arg);
    } 
    else if (type.equals(float.class)) {
      return new Float(arg);
    } 
    else if (type.equals(Float.class)) {
      return new Float(arg);
    } 
    else if (type.equals(int.class)) {
      return new Integer(arg);
    } 
    else if (type.equals(Integer.class)) {
      return new Integer(arg);
    } 
    else if (type.equals(long.class)) {
      return new Long(arg);
    } 
    else if (type.equals(Long.class)) {
      return new Long(arg);
    } 
    else if (type.equals(short.class)) {
      return new Short(arg);
    } 
    else if (type.equals(Short.class)) {
      return new Short(arg);
    } 
    else if (type.equals(String.class)) {
      return arg;
    }
    else 
      throw new IllegalArgumentException("Cannot convert String to type " +
                                         type.getName());
  }
}
