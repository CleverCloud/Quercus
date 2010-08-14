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

public class JAXBRestProxy extends RestProxy {
  private static final Logger log = Logger.getLogger(RestProxy.class.getName());

  private JAXBContext _context;
  private Marshaller _marshaller;
  private Unmarshaller _unmarshaller;

  public JAXBRestProxy(Class api, String url)
    throws JAXBException
  {
    super(api, url);

    ArrayList<Class> classList = new ArrayList<Class>();
    JAXBUtil.introspectClass(_api, classList);

    _context = JAXBContext.newInstance(classList.toArray(new Class[0]));
    init();
  }

  public JAXBRestProxy(Class api, String url, Class[] jaxbClasses)
    throws JAXBException
  {
    super(api, url);

    _context = JAXBContext.newInstance(jaxbClasses);
    init();
  }

  public JAXBRestProxy(Class api, String url, String jaxbPackages)
    throws JAXBException
  {
    super(api, url);

    _context = JAXBContext.newInstance(jaxbPackages);
    init();
  }

  private void init()
    throws JAXBException
  {
    _marshaller = _context.createMarshaller();
    _marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    _unmarshaller = _context.createUnmarshaller();
  }

  protected void writePostData(OutputStream out, ArrayList<Object> postValues)
    throws IOException, RestException
  {
    try {
      for (Object postValue : postValues)
        _marshaller.marshal(postValue, out);
    }
    catch (JAXBException e) {
      throw new RestException(e);
    }
  }

  protected Object readResponse(InputStream in)
    throws IOException, RestException
  {
    try {
      return _unmarshaller.unmarshal(in);
    }
    catch (JAXBException e) {
      throw new RestException(e);
    }
  }
}
