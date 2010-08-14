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
import org.xml.sax.InputSource;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

public interface Unmarshaller {

  <A extends XmlAdapter> A getAdapter(Class<A> type);

  AttachmentUnmarshaller getAttachmentUnmarshaller();

  ValidationEventHandler getEventHandler() throws JAXBException;

  Listener getListener();

  Object getProperty(String name) throws PropertyException;

  Schema getSchema();

  UnmarshallerHandler getUnmarshallerHandler();

  boolean isValidating() throws JAXBException;

  <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter);

  void setAdapter(XmlAdapter adapter);

  void setAttachmentUnmarshaller(AttachmentUnmarshaller au);

  void setEventHandler(ValidationEventHandler handler)
    throws JAXBException;

  void setListener(Listener listener);

  void setProperty(String name, Object value) throws PropertyException;

  void setSchema(Schema schema);

  void setValidating(boolean validating) throws JAXBException;

  Object unmarshal(File f) throws JAXBException;

  Object unmarshal(InputSource source) throws JAXBException;

  Object unmarshal(InputStream is) throws JAXBException;

  Object unmarshal(Node node) throws JAXBException;

  <T> JAXBElement<T> unmarshal(Node node, Class<T> declaredType)
      throws JAXBException;

  Object unmarshal(Reader reader) throws JAXBException;

  Object unmarshal(Source source) throws JAXBException;

  <T> JAXBElement<T> unmarshal(Source node, Class<T> declaredType)
      throws JAXBException;

  Object unmarshal(URL url) throws JAXBException;

  Object unmarshal(XMLEventReader reader) throws JAXBException;

  <T> JAXBElement<T> unmarshal(XMLEventReader xmlEventReader,
                                        Class<T> declaredType)
      throws JAXBException;

  Object unmarshal(XMLStreamReader reader) throws JAXBException;

  <T> JAXBElement<T> unmarshal(XMLStreamReader xmlStreamReader,
                                        Class<T> declaredType)
      throws JAXBException;

  public static abstract class Listener {

    public Listener()
    {
    }

    public void afterUnmarshal(Object target, Object parent)
    {
    }


    public void beforeUnmarshal(Object target, Object parent)
    {
    }

  }
}

