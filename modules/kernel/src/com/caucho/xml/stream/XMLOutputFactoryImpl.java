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

package com.caucho.xml.stream;
import com.caucho.util.L10N;
import com.caucho.vfs.Vfs;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class XMLOutputFactoryImpl extends XMLOutputFactory {
  private static final L10N L = new L10N(XMLOutputFactoryImpl.class);

  private boolean _repair = false;

  public XMLOutputFactoryImpl()
  {
  }


  //
  // Event writer
  //

  public XMLEventWriter createXMLEventWriter(OutputStream stream)
    throws XMLStreamException
  {
    return new XMLEventWriterImpl(createXMLStreamWriter(stream));
  }

  public XMLEventWriter createXMLEventWriter(OutputStream stream,
                                             String encoding)
    throws XMLStreamException
  {
    return new XMLEventWriterImpl(createXMLStreamWriter(stream, encoding));
  }

  /**
   *  This method is optional.
   */
  public XMLEventWriter createXMLEventWriter(Result result)
    throws XMLStreamException
  {
    return new XMLEventWriterImpl(createXMLStreamWriter(result));
  }

  public XMLEventWriter createXMLEventWriter(Writer stream)
    throws XMLStreamException
  {
    return new XMLEventWriterImpl(createXMLStreamWriter(stream));
  }

  // 
  // Stream writer
  //

  public XMLStreamWriter createXMLStreamWriter(OutputStream stream)
    throws XMLStreamException
  {
    return new XMLStreamWriterImpl(Vfs.openWrite(stream), _repair);
  }

  public XMLStreamWriter createXMLStreamWriter(OutputStream stream,
                                               String encoding)
    throws XMLStreamException
  {
    try {
      OutputStreamWriter osw = new OutputStreamWriter(stream, encoding);
      return new XMLStreamWriterImpl(Vfs.openWrite(osw), _repair);
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  /**
   *  This method is optional.
   */
  public XMLStreamWriter createXMLStreamWriter(Result result)
    throws XMLStreamException
  {
    if (result instanceof DOMResult) {
      return new DOMResultXMLStreamWriterImpl((DOMResult) result, _repair);
    }
    else if (result instanceof SAXResult) {
      return new SAXResultXMLStreamWriterImpl((SAXResult) result);
    }
    else if (result instanceof StreamResult) {
      Writer writer = ((StreamResult) result).getWriter();

      if (writer != null) 
        return createXMLStreamWriter(writer);

      OutputStream os = ((StreamResult) result).getOutputStream();

      if (os != null)
        return createXMLStreamWriter(os);

      throw new XMLStreamException("StreamResult has no output stream or writer");
    }

    throw new UnsupportedOperationException(L.l("Results of type {0} are not supported", result.getClass().getName()));
  }

  public XMLStreamWriter createXMLStreamWriter(Writer stream)
    throws XMLStreamException
  {
    return new XMLStreamWriterImpl(Vfs.openWrite(stream), _repair);
  }

  public Object getProperty(String name)
    throws IllegalArgumentException
  {
    throw new IllegalArgumentException("property \""+name+"\" not supported");
  }

  public boolean isPropertySupported(String name)
  {
    return false;
  }

  public void setProperty(String name, Object value)
    throws IllegalArgumentException
  {
    if (IS_REPAIRING_NAMESPACES.equals(name)) {
      if (value instanceof Boolean)
        _repair = ((Boolean) value).booleanValue();
      else 
        throw new IllegalArgumentException("value of " + name + " must be Boolean, not " + value);
    }
    else
      throw new IllegalArgumentException("property \""+name+"\" not supported");
  }
}
