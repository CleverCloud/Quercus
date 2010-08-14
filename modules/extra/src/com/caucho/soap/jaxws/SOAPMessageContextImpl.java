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

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.xml.bind.JAXBContext;

import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.transform.Source;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import com.caucho.util.L10N;

import com.caucho.xml.saaj.MessageFactoryImpl;

public class SOAPMessageContextImpl extends AbstractMessageContextImpl
                                    implements SOAPMessageContext 
{
  private SOAPMessage _message;
  private static MessageFactory _messageFactory;

  public Object[] getHeaders(QName header, 
                             JAXBContext context, 
                             boolean allRoles)
    throws WebServiceException
  {
    throw new UnsupportedOperationException();
  }

  public Set<String> getRoles()
  {
    throw new UnsupportedOperationException();
  }

  public SOAPMessage getMessage()
  {
    return _message;
  }

  public void setMessage(SOAPMessage message)
    throws WebServiceException
  {
    _message = message;
  }

  public void setMessage(Source source)
    throws WebServiceException
  {
    try {
      if (_message == null)
        _message = getMessageFactory().createMessage();

      _message.getSOAPPart().setContent(source);
    }
    catch (SOAPException e) {
      throw new WebServiceException(e);
    }
  }
  
  MessageFactory getMessageFactory()
    throws SOAPException
  {
    if (_messageFactory == null) {
      _messageFactory = new MessageFactoryImpl();
    }

    return _messageFactory;
  }
}
