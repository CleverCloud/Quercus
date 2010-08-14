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

import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.xml.soap.*;
import static javax.xml.soap.SOAPConstants.*;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.soap.SOAPBinding;

import com.caucho.util.L10N;

public class SOAPBindingImpl extends AbstractBinding implements SOAPBinding {
  private final static L10N L = new L10N(SOAPBindingImpl.class);

  private boolean _mtom;
  private Set<String> _roles;

  private final MessageFactory _messageFactory;
  private final SOAPFactory _soapFactory;

  public SOAPBindingImpl(String bindingId)
    throws SOAPException
  {
    if (bindingId.equals(SOAP11HTTP_BINDING)) {
      _messageFactory = MessageFactory.newInstance(SOAP_1_1_PROTOCOL);
      _soapFactory = SOAPFactory.newInstance(SOAP_1_1_PROTOCOL);
      _mtom = false;
    }
    else if (bindingId.equals(SOAP11HTTP_MTOM_BINDING)) {
      _messageFactory = MessageFactory.newInstance(SOAP_1_1_PROTOCOL);
      _soapFactory = SOAPFactory.newInstance(SOAP_1_1_PROTOCOL);
      _mtom = true;
    }
    else if (bindingId.equals(SOAP12HTTP_BINDING)) {
      _messageFactory = MessageFactory.newInstance(SOAP_1_2_PROTOCOL);
      _soapFactory = SOAPFactory.newInstance(SOAP_1_2_PROTOCOL);
      _mtom = false;
    }
    else if (bindingId.equals(SOAP12HTTP_MTOM_BINDING)) {
      _messageFactory = MessageFactory.newInstance(SOAP_1_2_PROTOCOL);
      _soapFactory = SOAPFactory.newInstance(SOAP_1_2_PROTOCOL);
      _mtom = true;
    }
    else
      throw new SOAPException(L.l("Unknown SOAP binding: {0}", bindingId));
  }

  public MessageFactory getMessageFactory()
  {
    return _messageFactory;
  }

  public SOAPFactory getSOAPFactory()
  {
    return _soapFactory;
  }

  public boolean isMTOMEnabled()
  {
    return _mtom;
  }

  public void setMTOMEnabled(boolean mtom)
  {
    _mtom = mtom;
  }

  public Set<String> getRoles()
  {
    return _roles;
  }

  public void setRoles(Set<String> roles)
  {
    _roles = roles;
  }
}

