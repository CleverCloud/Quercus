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

import java.awt.Image;
import java.awt.image.*;
import javax.imageio.*;
import javax.imageio.stream.*;

import javax.xml.XMLConstants;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.HashMap;
import java.util.Iterator;

import com.caucho.util.Attachment;
import com.caucho.util.Base64;
import com.caucho.util.L10N;

/**
 * an Image Property
 */
public class ImageProperty extends CDataProperty 
                           implements AttachmentProperty 
{
  public static final QName SCHEMA_TYPE = 
    new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "base64Binary", "xsd");

  private static final L10N L = new L10N(ImageProperty.class);
  private static final String DEFAULT_IMAGE_MIME_TYPE = "image/png";
  private static final HashMap<String,ImageProperty> _mimeTypePropertyMap
    = new HashMap<String,ImageProperty>();

  private String _mimeType;
  private ImageReader _imageReader;
  private ImageWriter _imageWriter;

  public static ImageProperty getImageProperty(String mimeType)
    throws JAXBException
  {
    ImageProperty property = _mimeTypePropertyMap.get(mimeType);

    if (property == null) {
      if (mimeType == null) {
        property = new ImageProperty(DEFAULT_IMAGE_MIME_TYPE);
        _mimeTypePropertyMap.put(DEFAULT_IMAGE_MIME_TYPE, property);
      }
      else
        property = new ImageProperty(mimeType);

      _mimeTypePropertyMap.put(mimeType, property);
    }

    return property;
  }

  private ImageProperty(String mimeType)
    throws JAXBException
  {
    int slash = mimeType.indexOf('/');

    if (slash < 0)
      throw new JAXBException(L.l("Unrecognized MIME type : {0}", mimeType));

    String formatName = mimeType.substring(slash + 1);

    Iterator writers = ImageIO.getImageWritersByFormatName(formatName);

    if (! writers.hasNext())
      throw new JAXBException(L.l("Unrecognized MIME type : {0}", mimeType));

    _mimeType = mimeType;
    _imageWriter = (ImageWriter) writers.next();
    _imageReader = ImageIO.getImageReader(_imageWriter);

    if (_imageReader == null)
      throw new JAXBException(L.l("Unrecognized MIME type : {0}", mimeType));
  }

  public String write(Object in)
    throws IOException
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(baos);

    _imageWriter.setOutput(out);
    _imageWriter.write((RenderedImage) in);

    out.flush();

    return Base64.encodeFromByteArray(baos.toByteArray());
  }

  protected Object read(String in)
    throws IOException
  {
    byte[] buffer = Base64.decodeToByteArray(in);
    ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
    MemoryCacheImageInputStream is = new MemoryCacheImageInputStream(bais);

    _imageReader.setInput(is);

    return _imageReader.read(0);
  }

  public QName getSchemaType()
  {
    return SCHEMA_TYPE;
  }

  public String getMimeType(Object obj)
  {
    return _mimeType;
  }

  public void writeAsAttachment(Object obj, OutputStream out)
    throws IOException
  {
    MemoryCacheImageOutputStream iout = new MemoryCacheImageOutputStream(out);

    _imageWriter.setOutput(iout);
    _imageWriter.write((RenderedImage) obj);

    iout.flush();
  }

  public Object readFromAttachment(Attachment attachment)
    throws IOException
  {
    InputStream is = new ByteArrayInputStream(attachment.getRawContents());
    MemoryCacheImageInputStream iis = new MemoryCacheImageInputStream(is);

    _imageReader.setInput(iis);

    return _imageReader.read(0);
  }
}
