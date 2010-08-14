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

import com.caucho.config.ConfigurationException;
import com.caucho.soap.jaxws.JAXWSUtil;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.xml.soap.SOAPConstants.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingType;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.soap.SOAPBinding;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.logging.Logger;

/**
 * Invokes a service Provider.
 */
public class SourceProviderEncoding extends ProviderEncoding {
  public static final L10N L = new L10N(SourceProviderEncoding.class);
  private static final Logger log =
    Logger.getLogger(SourceProviderEncoding.class.getName());

  private String _soapNamespace;

  protected SourceProviderEncoding(Object service)
    throws ConfigurationException
  {
    super(service);

    BindingType bindingType 
      = (BindingType) _class.getAnnotation(BindingType.class);

    if (bindingType != null) {
      if (bindingType.value().equals(SOAPBinding.SOAP11HTTP_BINDING))
        _soapNamespace = URI_NS_SOAP_1_1_ENVELOPE;
      else if (bindingType.value().equals(SOAPBinding.SOAP12HTTP_BINDING))
        _soapNamespace = URI_NS_SOAP_1_2_ENVELOPE;
    }
  }

  public void invoke(InputStream is, OutputStream os)
    throws Throwable
  {
    if (_mode == Service.Mode.PAYLOAD) {
      // Put the payload into a StreamSource
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      JAXWSUtil.extractSOAPBody(is, baos);
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

      // invoke the provider
      Source ret = (Source) _provider.invoke(new StreamSource(bais));

      // Wrap and send the response
      OutputStreamWriter writer = new OutputStreamWriter(os);
      JAXWSUtil.writeStartSOAPEnvelope(writer, _soapNamespace);

      _transformer.transform(ret, new StreamResult(os));
      os.flush();

      JAXWSUtil.writeEndSOAPEnvelope(writer);
    }
    else {
      Source ret = (Source) _provider.invoke(new StreamSource(is));

      _transformer.transform(ret, new StreamResult(os));

      os.flush();
    }
  }
}
