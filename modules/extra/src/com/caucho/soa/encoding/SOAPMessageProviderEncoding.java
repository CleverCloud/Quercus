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
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import static javax.xml.soap.SOAPConstants.*;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.BindingType;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.soap.SOAPBinding;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * Invokes a service Provider.
 */
public class SOAPMessageProviderEncoding extends ProviderEncoding {
  private static final L10N L = new L10N(ProviderEncoding.class);

  private final MessageFactory _factory;

  protected SOAPMessageProviderEncoding(Object service)
    throws ConfigurationException
  {
    super(service);

    if (_mode != Service.Mode.MESSAGE)
      throw new ConfigurationException(L.l("{0} implements Provider<SOAPMessage> must have @ServiceMode annotation with value Service.Mode == MESSAGE", _class.getName()));

    try {
      BindingType bindingType 
        = (BindingType) _class.getAnnotation(BindingType.class);

      if (bindingType != null && 
          bindingType.value().equals(SOAPBinding.SOAP12HTTP_BINDING))
        _factory = MessageFactory.newInstance(SOAP_1_2_PROTOCOL);
      else
        _factory = MessageFactory.newInstance(SOAP_1_1_PROTOCOL);
    }
    catch (SOAPException e) {
      throw new ConfigurationException(e);
    }
  }

  public void invoke(InputStream is, OutputStream os)
    throws Throwable
  {
    SOAPMessage request = _factory.createMessage(new MimeHeaders(), is);
    SOAPMessage response = (SOAPMessage) _provider.invoke(request);

    response.writeTo(os);
    os.flush();
  }
}

