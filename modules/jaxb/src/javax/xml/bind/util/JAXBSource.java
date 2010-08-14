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

package javax.xml.bind.util;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/** Make the result of a JAXB marshalling look like a SAX source */
public class JAXBSource extends SAXSource {

  public JAXBSource(JAXBContext context, Object contentObject)
    throws JAXBException
  {
    super(createInputSource(context.createMarshaller(), contentObject));

    try {
      setXMLReader(XMLReaderFactory.createXMLReader());
    }
    catch (SAXException e) {
      throw new JAXBException(e);
    }
  }

  public JAXBSource(Marshaller marshaller, Object contentObject)
    throws JAXBException
  {
    super(createInputSource(marshaller, contentObject));

    try {
      setXMLReader(XMLReaderFactory.createXMLReader());
    }
    catch (SAXException e) {
      throw new JAXBException(e);
    }
  }

  private static InputSource createInputSource(Marshaller marshaller,
                                               Object contentObject)
    throws JAXBException
  {
    // current implementation is very inefficient, but correct

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    marshaller.marshal(contentObject, baos);

    return new InputSource(new ByteArrayInputStream(baos.toByteArray()));
  }
}

