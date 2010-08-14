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
 * @author Adam Megacz
 */

package com.caucho.jaxb.property;

import javax.xml.XMLConstants;
import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.caucho.util.Attachment;

// XXX hexBinary
/**
 * a ByteArray Property
 */
public class ByteArrayProperty extends CDataProperty 
                               implements AttachmentProperty
{
  public static final QName SCHEMA_TYPE = 
    new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "base64Binary", "xsd");

  public static final ByteArrayProperty PROPERTY = new ByteArrayProperty();

  public String write(Object in)
  {
    return DatatypeConverter.printBase64Binary((byte[]) in);
  }

  protected Object read(String in)
  {
    return DatatypeConverter.parseBase64Binary(in);
  }

  public QName getSchemaType()
  {
    return SCHEMA_TYPE;
  }

  public String getMimeType(Object obj)
  {
    return "application/octet-stream";
  }

  public void writeAsAttachment(Object obj, OutputStream out)
    throws IOException
  {
    byte[] buffer = (byte []) obj;

    out.write(buffer);

    out.flush();
  }

  public Object readFromAttachment(Attachment attachment)
    throws IOException
  {
    return attachment.getRawContents();
  }
}
