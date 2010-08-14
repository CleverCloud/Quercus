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

package com.caucho.soap.reflect;

import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.jaxb.JAXBUtil;
import com.caucho.soap.jaxws.JAXWSUtil;
import com.caucho.soap.skeleton.DirectSkeleton;
import com.caucho.soap.skeleton.AbstractAction;
import com.caucho.soap.wsdl.WSDLDefinitions;
import com.caucho.util.L10N;

import org.w3c.dom.Node;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import javax.xml.ws.WebServiceException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Introspects a web service
 */
public class WebServiceIntrospector {
  private static final Logger log 
    = Logger.getLogger(WebServiceIntrospector.class.getName());
  private static XMLOutputFactory _outputFactory = null;

  public static final L10N L = new L10N(WebServiceIntrospector.class);
  /**
   * Introspects the class
   */
  public static DirectSkeleton introspect(Class type)
    throws JAXBException, WebServiceException
  {
    // matches RI stub for the WSDL location
    return introspect(type, "REPLACE_WITH_ACTUAL_URL", null);
  }

  /**
   * Introspects the class
   */
  public static DirectSkeleton introspect(Class type, 
                                          String wsdlLocation,
                                          WSDLDefinitions wsdl)
    throws JAXBException, WebServiceException
  {
    // server/4221 vs soap/0301
    /*
    if (! type.isAnnotationPresent(WebService.class))
      throw new RuntimeException(L.l("{0}: needs a @WebService annotation.",
                                     type.getName()));
    */
 
    boolean isInterface = type.isInterface();

    WebService webService = (WebService) type.getAnnotation(WebService.class);

    // Get all the classes that JAXB needs, then generate schema for them
    HashSet<Class> jaxbClasses = new HashSet<Class>();
    JAXBUtil.introspectClass(type, jaxbClasses);
    Class[] jaxbClassArray = new Class[jaxbClasses.size()];
    jaxbClasses.toArray(jaxbClassArray);

    Class api = JAXWSUtil.getEndpointInterface(type);
    String namespace = JAXWSUtil.getTargetNamespace(type, api);

    Map<String,Object> properties = new HashMap<String,Object>();
    properties.put(JAXBContextImpl.TARGET_NAMESPACE, namespace);

    JAXBContextImpl jaxbContext = 
      new JAXBContextImpl(jaxbClassArray, properties);
    Marshaller marshaller = jaxbContext.createMarshaller();
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

    DirectSkeleton skel = 
      new DirectSkeleton(type, api, jaxbContext, wsdlLocation, namespace, wsdl);

    Method[] methods = type.getMethods();

    boolean excludeIfNoWebMethod = false;

    for (int i = 0; i < methods.length; i++) {
      WebMethod webMethod = methods[i].getAnnotation(WebMethod.class);

      if (webMethod != null && ! webMethod.exclude()) {
        excludeIfNoWebMethod = true;
        break;
      }
    }

    for (int i = 0; i < methods.length; i++) {
      if ((methods[i].getModifiers() & Modifier.PUBLIC) == 0)
        continue;

      WebMethod webMethod = methods[i].getAnnotation(WebMethod.class);

      if (api != type) {
        /* XXX: needed for demo
        if (webMethod != null)
          throw new WebServiceException(L.l("Cannot use WebMethod with WebService.endpointInterface: {0}", type));
        */

        try {
          api.getDeclaredMethod(methods[i].getName(), 
                                methods[i].getParameterTypes());
        }
        catch (NoSuchMethodException e) {
          // ignore methods not declared in the endpoint interface
          continue;
        }
      }

      if (excludeIfNoWebMethod && webMethod == null)
        continue;

      if (webService == null && webMethod == null && ! isInterface)
        continue;

      if (webMethod == null && methods[i].getDeclaringClass() != type)
        continue;

      if (webMethod != null && webMethod.exclude())
        continue;


      AbstractAction action = 
        AbstractAction.createAction(methods[i], jaxbContext, namespace, wsdl,
                                    marshaller, unmarshaller);

      skel.addAction(methods[i], action);
    }

    return skel;
  }

  private static XMLStreamWriter getStreamWriter(DOMResult result)
    throws XMLStreamException
  {
    if (_outputFactory == null)
      _outputFactory = XMLOutputFactory.newInstance();

    return _outputFactory.createXMLStreamWriter(result);
  }
}
