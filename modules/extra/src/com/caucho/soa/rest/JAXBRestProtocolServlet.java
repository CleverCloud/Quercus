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
public class JAXBRestProtocolServlet extends RestProtocolServlet
{
  private String _jaxbPackages;
  private ArrayList<Class> _jaxbClasses = new ArrayList<Class>();
  private Marshaller _marshaller;
  private Unmarshaller _unmarshaller;
  private JAXBContext _context = null;

  public JAXBRestProtocolServlet()
  {
    super();
  }
  
  public void addJaxbPackage(String packageName)
  {
    if (_jaxbPackages != null)
      _jaxbPackages = _jaxbPackages + ";" + packageName;
    else
      _jaxbPackages = packageName;
  }

  public void addJaxbClass(Class cl)
  {
    _jaxbClasses.add(cl);
  }

  public void init()
    throws ServletException
  {
    super.init();

    try {
      Class cl = _service.getClass();

      if (cl.isAnnotationPresent(WebService.class)) {
        WebService webService
          = (WebService) cl.getAnnotation(WebService.class);

        String endpoint = webService.endpointInterface();

        if (endpoint != null && ! "".equals(endpoint))
          cl = CauchoSystem.loadClass(webService.endpointInterface());
      }

      ArrayList<Class> jaxbClasses = _jaxbClasses;

      for (Method method : cl.getMethods()) {
        if (method.getDeclaringClass().equals(Object.class))
          continue;

        int modifiers = method.getModifiers();

        // Allow abstract for interfaces
        if (Modifier.isStatic(modifiers)
            || Modifier.isFinal(modifiers)
            || ! Modifier.isPublic(modifiers))
          continue;

        if (_context == null)
          JAXBUtil.introspectMethod(method, jaxbClasses);
      }

      if (_context != null) {
      }
      else if (_jaxbPackages != null) {
        _context = JAXBContext.newInstance(_jaxbPackages);
      }
      else {
        Class[] classes = jaxbClasses.toArray(new Class[jaxbClasses.size()]);
        _context = JAXBContext.newInstance(classes);
      }
      
      _marshaller = _context.createMarshaller();
      _marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      _unmarshaller = _context.createUnmarshaller();
    }
    catch (Exception e) {
      throw new ServletException(e);
    }
  }

  protected Object readPostData(InputStream in)
    throws IOException, RestException
  {
    try {
      return _unmarshaller.unmarshal(in);
    }
    catch (JAXBException e) {
      throw new RestException(e);
    }
  }

  protected void writeResponse(OutputStream out, Object result)
    throws IOException, RestException
  {
    WriteStream ws = Vfs.openWrite(out);

    try {
      XMLStreamWriterImpl writer = new XMLStreamWriterImpl(ws);
      
      _marshaller.marshal(result, writer);
    } 
    catch (JAXBException e) {
      throw new RuntimeException(e);
    }
    finally {
      ws.close();
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
