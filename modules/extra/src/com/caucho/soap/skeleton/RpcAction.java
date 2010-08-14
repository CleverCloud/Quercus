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

package com.caucho.soap.skeleton;

import com.caucho.jaxb.JAXBContextImpl;

import com.caucho.soap.wsdl.MIMEContentType;
import com.caucho.soap.wsdl.MIMEMultipartRelated;
import com.caucho.soap.wsdl.MIMEPart;
import com.caucho.soap.wsdl.WSDLBindingOperationMessage;
import com.caucho.soap.wsdl.WSDLDefinitions;

import com.caucho.util.L10N;

import javax.jws.WebParam;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.WebServiceException;
import java.lang.reflect.Method;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Behaves like a DocumentWrappedAction most of the time, but also allows
 * attachments.
 */
public class RpcAction extends DocumentWrappedAction {
  private final static Logger log = Logger.getLogger(RpcAction.class.getName());
  public static final L10N L = new L10N(RpcAction.class);

  public RpcAction(Method method, Method eiMethod,
                   JAXBContextImpl jaxbContext, 
                   String targetNamespace,
                   WSDLDefinitions wsdl,
                   Marshaller marshaller,
                   Unmarshaller unmarshaller)
    throws JAXBException, WebServiceException
  {
    super(method, eiMethod, 
          jaxbContext, targetNamespace, wsdl, 
          marshaller, unmarshaller);
  }
  
  protected String attachmentContentType(WebParam webParam)
  {
    if (_bindingOperation == null)
      return null;

    WSDLBindingOperationMessage message = null;

    if (webParam.mode() == WebParam.Mode.IN ||
        webParam.mode() == WebParam.Mode.INOUT) {
      String contentType = attachmentContentType(_bindingOperation.getInput(), 
                                                 webParam.partName());

      if (contentType != null)
        return contentType;
    }

    if (webParam.mode() == WebParam.Mode.OUT ||
        webParam.mode() == WebParam.Mode.INOUT) {
      String contentType = attachmentContentType(_bindingOperation.getOutput(),
                                                 webParam.partName());

      if (contentType != null)
        return contentType;
    }

    return null;
  }

  private String attachmentContentType(WSDLBindingOperationMessage message,
                                       String partName)
  {
    for (Object o : _bindingOperation.getInput().getAny()) {
      if (o instanceof MIMEMultipartRelated) {
        MIMEMultipartRelated related = (MIMEMultipartRelated) o;

        for (MIMEPart part : related.getParts()) {

          for (Object subpart : part.getAny()) {
            if (subpart instanceof MIMEContentType) {
              MIMEContentType content = (MIMEContentType) subpart;

              if (partName.equals(content.getPart()))
                return content.getType();
            }
          }

        }
      }
    }

    return null;
  }
}
