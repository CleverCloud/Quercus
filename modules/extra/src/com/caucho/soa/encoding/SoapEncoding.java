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

package com.caucho.soa.encoding;

import com.caucho.soap.reflect.WebServiceIntrospector;
import com.caucho.soap.skeleton.DirectSkeleton;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * Invokes a service based on a Hessian-encoded request.
 */
public class SoapEncoding implements ServiceEncoding {
  private static final Logger log =
    Logger.getLogger(SoapEncoding.class.getName());

  protected final XMLOutputFactory _xmlOutputFactory 
    = XMLOutputFactory.newInstance();
  protected final XMLInputFactory _xmlInputFactory
    = XMLInputFactory.newInstance();

  private Object _object;
  private Class _class;
  private DirectSkeleton _skeleton;
  private String _wsdlLocation;

  public void setService(Object service)
  {
    _object = service;

    if (_class == null)
      _class = service.getClass();
  }

  public void setSeparateSchema(boolean separateSchema)
    throws JAXBException, IOException
  {
    getSkeleton().setSeparateSchema(separateSchema);
  }

  public void setInterface(Class cl)
  {
    _class = cl;
  }

  @PostConstruct
  public void init()
  {
    _xmlOutputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, 
                                  Boolean.TRUE);
  }

  public void invoke(InputStream is, OutputStream os)
    throws Throwable
  {
  }

  public void invoke(HttpServletRequest request, HttpServletResponse response)
    throws Throwable
  {
    getSkeleton().invoke(_object, request, response);
  }    

  public void dumpWSDL(OutputStream out)
    throws IOException, XMLStreamException, JAXBException
  {
    getSkeleton().dumpWSDL(out);
  }

  private DirectSkeleton getSkeleton()
    throws JAXBException, IOException
  {
    if (_skeleton == null) {
      _skeleton = 
        WebServiceIntrospector.introspect(_class, _wsdlLocation, null);
    }

    return _skeleton;
  }
}
