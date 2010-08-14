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
* @author Scott Ferguson
*/

package javax.xml.bind;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.validation.Schema;
import java.io.*;

public interface Marshaller {

  static final String JAXB_ENCODING="jaxb.encoding";

  static final String JAXB_FORMATTED_OUTPUT="jaxb.formatted.output";

  static final String JAXB_FRAGMENT="jaxb.fragment";

  static final String JAXB_NO_NAMESPACE_SCHEMA_LOCATION
    = "jaxb.noNamespaceSchemaLocation";

  static final String JAXB_SCHEMA_LOCATION="jaxb.schemaLocation";

  <A extends XmlAdapter> A getAdapter(Class<A> type);

  AttachmentMarshaller getAttachmentMarshaller();

  ValidationEventHandler getEventHandler() throws JAXBException;

  Listener getListener();

  Node getNode(Object contentTree) throws JAXBException;

  Object getProperty(String name) throws PropertyException;

  Schema getSchema();

  void marshal(Object jaxbElement, ContentHandler handler)
    throws JAXBException;

  void marshal(Object jaxbElement, Node node) throws JAXBException;

  void marshal(Object jaxbElement, OutputStream os)
    throws JAXBException;

  void marshal(Object jaxbElement, Result result) throws JAXBException;

  void marshal(Object jaxbElement, Writer writer) throws JAXBException;

  /**
   * @since JAXB 2.1
   */
  void marshal(Object jaxbElement, File output) throws JAXBException;

  void marshal(Object jaxbElement, XMLEventWriter writer)
    throws JAXBException;

  void marshal(Object jaxbElement, XMLStreamWriter writer)
    throws JAXBException;

  <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter);

  void setAdapter(XmlAdapter adapter);

  void setAttachmentMarshaller(AttachmentMarshaller am);

  void setEventHandler(ValidationEventHandler handler)
    throws JAXBException;

  void setListener(Listener listener);

  void setProperty(String name, Object value) throws PropertyException;

  void setSchema(Schema schema);

  public static abstract class Listener {

    public Listener()
    {
    }

    public void afterMarshal(Object source)
    {
    }

    public void beforeMarshal(Object source)
    {
    }

  }
}

