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

package com.caucho.soap.jaxws;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import javax.xml.ws.*;
import javax.xml.ws.handler.*;

import com.caucho.util.L10N;

public class LogicalMessageImpl implements LogicalMessage
{
  private static Transformer _transformer;

  private Source _source;

  public Source getPayload()
  {
    return _source;
  }

  public void setPayload(Source source)
    throws WebServiceException
  {
    try {
      // The payload needs to be stable, so transform stream-based 
      // sources to DOM
      if ((source instanceof StreamSource) || (source instanceof SAXSource)) {
        DOMResult dom = new DOMResult();
        getTransformer().transform(source, dom);

        _source = new DOMSource(dom.getNode());
      }
      else 
        _source = source;
    }
    catch (Exception e) {
      throw new WebServiceException(e);
    }
  }

  public void setPayload(Object payload, JAXBContext context)
    throws WebServiceException
  {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      Marshaller marshaller = context.createMarshaller();
      marshaller.marshal(payload, baos);

      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      _source = new StreamSource(bais);
    }
    catch (Exception e) {
      throw new WebServiceException(e);
    }
  }

  public Object getPayload(JAXBContext context)
    throws WebServiceException
  {
    try {
      Unmarshaller unmarshaller = context.createUnmarshaller();
      return unmarshaller.unmarshal(_source);
    }
    catch (Exception e) {
      throw new WebServiceException(e);
    }
  }

  private static Transformer getTransformer()
    throws TransformerException
  {
    if (_transformer == null) {
      TransformerFactory factory = TransformerFactory.newInstance();
      _transformer = factory.newTransformer();
    }

    return _transformer;
  }
}
