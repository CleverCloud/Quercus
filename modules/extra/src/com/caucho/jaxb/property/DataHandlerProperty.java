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

package com.caucho.jaxb.property;

import java.security.*;
import java.lang.reflect.*;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.activation.DataContentHandler;
import javax.activation.DataHandler;
import javax.activation.CommandInfo;
import javax.activation.CommandMap;

import javax.mail.util.ByteArrayDataSource;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.caucho.jaxb.JAXBUtil;
import com.caucho.jaxb.BinderImpl;

import com.caucho.util.Attachment;
import com.caucho.util.Base64;
import com.caucho.util.L10N;

/**
 * DataHandler property.
 *
 * Note that DataHandler fields/properties are not affected by XmlMimeType
 * annotations.
 */
public class DataHandlerProperty extends CDataProperty 
                                 implements AttachmentProperty
{
  public static final QName SCHEMA_TYPE = 
    new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "base64Binary", "xsd");

  private static final L10N L = new L10N(DataHandlerProperty.class);
  private static final Logger log = 
    Logger.getLogger(DataHandlerProperty.class.getName());

  public static final DataHandlerProperty PROPERTY = new DataHandlerProperty();
  private static final String DEFAULT_DATA_HANDLER_MIME_TYPE 
    = "application/octet-stream";

  protected Object read(String in) 
    throws IOException, JAXBException
  {
    byte[] buffer = Base64.decodeToByteArray(in);

    ByteArrayDataSource bads = 
      new ByteArrayDataSource(buffer, DEFAULT_DATA_HANDLER_MIME_TYPE);

    return new DataHandler(bads);
  }

  public String write(Object value)
    throws IOException, JAXBException
  {
    if (value != null) {
      DataHandler handler = (DataHandler) value;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      handler.writeTo(baos);

      return Base64.encodeFromByteArray(baos.toByteArray());
    }

    return "";
  }

  public QName getSchemaType()
  {
    return SCHEMA_TYPE;
  }

  public String getMimeType(Object obj)
  {
    return ((DataHandler) obj).getContentType();
  }

  public void writeAsAttachment(Object obj, OutputStream out)
    throws IOException
  {
    DataHandler handler = (DataHandler) obj;
    handler.setCommandMap(com.caucho.xml.saaj.SAAJCommandMap.COMMAND_MAP);

    handler.writeTo(out);
  }

  public Object readFromAttachment(Attachment attachment)
    throws IOException
  {
    String mimeType = attachment.getHeaderValue("Content-Type");

    if (mimeType == null) {
      mimeType = DEFAULT_DATA_HANDLER_MIME_TYPE;

      if (log.isLoggable(Level.FINER)) {
        log.finer("No Content-Type specified for DataHandler attachment, " + 
                  "assuming " + DEFAULT_DATA_HANDLER_MIME_TYPE);
      }
    }

    ByteArrayDataSource source = 
      new ByteArrayDataSource(attachment.getRawContents(), mimeType);

    return new DataHandler(source);
  }
}
