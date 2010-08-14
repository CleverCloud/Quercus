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

package javax.xml.stream;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.transform.Source;
import java.io.InputStream;
import java.io.Reader;

public abstract class XMLInputFactory {

  public static final String ALLOCATOR =
    "javax.xml.stream.allocator";

  public static final String IS_COALESCING =
    "javax.xml.stream.isCoalescing";

  public static final String IS_NAMESPACE_AWARE =
    "javax.xml.stream.isNamespaceAware";

  public static final String IS_REPLACING_ENTITY_REFERENCES =
    "javax.xml.stream.isReplacingEntityReferences";

  public static final String IS_SUPPORTING_EXTERNAL_ENTITIES =
    "javax.xml.stream.isSupportingExternalEntities";

  public static final String IS_VALIDATING =
    "javax.xml.stream.isValidating";

  public static final String REPORTER =
    "javax.xml.stream.reporter";

  public static final String RESOLVER =
    "javax.xml.stream.resolver";

  public static final String SUPPORT_DTD =
    "javax.xml.stream.supportDTD";

  protected XMLInputFactory()
  {
  }

  public abstract XMLEventReader 
    createFilteredReader(XMLEventReader reader, EventFilter filter)
    throws XMLStreamException;


  public abstract XMLStreamReader 
    createFilteredReader(XMLStreamReader reader, StreamFilter filter)
    throws XMLStreamException;

  public abstract XMLEventReader 
    createXMLEventReader(InputStream stream)
    throws XMLStreamException;

  public abstract XMLEventReader 
    createXMLEventReader(InputStream stream, String encoding)
    throws XMLStreamException;

  public abstract XMLEventReader 
    createXMLEventReader(Reader reader)
    throws XMLStreamException;

  public abstract XMLEventReader 
    createXMLEventReader(Source source)
    throws XMLStreamException;

  public abstract XMLEventReader 
    createXMLEventReader(String systemId, InputStream stream)
    throws XMLStreamException;

  public abstract XMLEventReader 
    createXMLEventReader(String systemId, Reader reader)
    throws XMLStreamException;

  public abstract XMLEventReader 
    createXMLEventReader(XMLStreamReader reader)
    throws XMLStreamException;

  public abstract XMLStreamReader 
    createXMLStreamReader(InputStream stream)
    throws XMLStreamException;

  public abstract XMLStreamReader 
    createXMLStreamReader(InputStream stream, String encoding)
    throws XMLStreamException;

  public abstract XMLStreamReader 
    createXMLStreamReader(Reader reader)
    throws XMLStreamException;

  public abstract XMLStreamReader 
    createXMLStreamReader(Source source)
    throws XMLStreamException;

  public abstract XMLStreamReader 
    createXMLStreamReader(String systemId, InputStream stream)
    throws XMLStreamException;

  public abstract XMLStreamReader 
    createXMLStreamReader(String systemId, Reader reader)
    throws XMLStreamException;

  public abstract XMLEventAllocator getEventAllocator();

  public abstract Object getProperty(String name)
    throws IllegalArgumentException;

  public abstract XMLReporter getXMLReporter();

  public abstract XMLResolver getXMLResolver();

  public abstract boolean isPropertySupported(String name);

  public static XMLInputFactory newInstance() throws FactoryConfigurationError
  {
    XMLInputFactory ret =
      newInstance("javax.xml.stream.XMLInputFactory",
                  Thread.currentThread().getContextClassLoader());

    if (ret!=null)
      return ret;

    throw new FactoryConfigurationError("No factory defined");
  }

  public static XMLInputFactory newInstance(String factoryId,
                                            ClassLoader classLoader)
    throws FactoryConfigurationError
  {
    return (XMLInputFactory)FactoryLoader
      .getFactoryLoader(factoryId).newInstance(classLoader);
  }

  public abstract void setEventAllocator(XMLEventAllocator allocator);

  public abstract void setProperty(String name, Object value)
    throws IllegalArgumentException;

  public abstract void setXMLReporter(XMLReporter reporter);

  public abstract void setXMLResolver(XMLResolver resolver);

}

