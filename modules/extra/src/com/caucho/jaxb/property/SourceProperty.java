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
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import com.caucho.util.Attachment;
import com.caucho.util.Base64;

/**
 * a Source Property
 */
public class SourceProperty extends CDataProperty
                            implements AttachmentProperty
{
  public static final QName SCHEMA_TYPE = 
    new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "base64Binary", "xsd");

  public static final SourceProperty PROPERTY = new SourceProperty();

  private final Transformer _transformer;

  private SourceProperty()
  {
    try {
      TransformerFactory factory = TransformerFactory.newInstance();
      _transformer = factory.newTransformer();
    }
    catch (TransformerException e) {
      throw new RuntimeException(e);
    }
  }

  public String write(Object in)
    throws IOException, JAXBException
  {
    Source src = (Source) in;

    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();

      _transformer.transform(src, new StreamResult(out));

      return Base64.encodeFromByteArray(out.toByteArray());
    }
    catch (TransformerException e) {
      if (src instanceof StreamSource) {
        StreamSource ss = (StreamSource) src;

        InputStream is = ss.getInputStream();

        if (is != null) {
          ByteArrayOutputStream out = new ByteArrayOutputStream();

          for (int ch = is.read(); ch >= 0; ch = is.read())
            out.write(ch);

          return Base64.encodeFromByteArray(out.toByteArray());
        }

        Reader reader = ss.getReader();

        if (reader != null) {
          CharArrayWriter out = new CharArrayWriter();

          for (int ch = reader.read(); ch >= 0; ch = reader.read())
            out.write(ch);

          return Base64.encode(new String(out.toCharArray()));
        }
      }

      throw new JAXBException(e);
    }
  }

  protected Object read(String in)
  {
    byte[] bytes = Base64.decodeToByteArray(in);
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

    return new StreamSource(bais);
  }

  public QName getSchemaType()
  {
    return SCHEMA_TYPE;
  }

  public String getMimeType(Object obj)
  {
    return "text/xml";
  }

  public void writeAsAttachment(Object obj, OutputStream out)
    throws IOException
  {
    try {
      if (obj instanceof StreamSource) {
        StreamSource source = (StreamSource) obj;
        InputSource inputSource = null;

        InputStream is = source.getInputStream();
        Reader reader = source.getReader();
        String systemId = source.getSystemId();

        if (is != null)
          inputSource = new InputSource(is);
       
        else if (reader != null)
          inputSource = new InputSource(reader);
        
        else if (systemId != null)
          inputSource = new InputSource(systemId);

        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setEntityResolver(_entityResolver);

        SAXSource saxSource = new SAXSource(xmlReader, inputSource);
        _transformer.transform(saxSource, new StreamResult(out));
      }
      else
        _transformer.transform((Source) obj, new StreamResult(out));
    }
    catch (TransformerException e) {
      IOException ioe = new IOException();
      ioe.initCause(e);

      throw ioe;
    }
    catch (SAXException saxe) {
      IOException ioe = new IOException();
      ioe.initCause(saxe);

      throw ioe;
    }
  }

  public Object readFromAttachment(Attachment attachment)
    throws IOException
  {
    byte[] contents = attachment.getRawContents();

    // try to give a DOMSource, but if that doesn't work, 
    // just give a simple StreamSource
    try {
      DOMResult result = new DOMResult();
      InputStream is = new ByteArrayInputStream(contents);

      _transformer.transform(new StreamSource(is), result);

      return new DOMSource(result.getNode());
    }
    catch (TransformerException e) {
      InputStream is = new ByteArrayInputStream(contents);

      return new StreamSource(is);
    }
  }

  private static final EntityResolver _entityResolver = new EntityResolver() {
    public InputSource resolveEntity(String publicId, String systemId)
    {
      return new InputSource(new StringReader(""));
    }
  };
}
