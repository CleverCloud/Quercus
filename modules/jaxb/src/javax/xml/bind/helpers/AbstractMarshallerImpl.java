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

package javax.xml.bind.helpers;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;

public abstract class AbstractMarshallerImpl implements Marshaller
{
  private XMLOutputFactory _factory;
  private AttachmentMarshaller _attachmentMarshaller;
  private String _encoding = "UTF-8";
  private ValidationEventHandler _validationEventHandler 
    = _defaultValidationEventHandler;
  private Listener _listener;
  private boolean _formattedOutput = false;
  private boolean _fragment = false;
  private String _noNSSchemaLocation;
  private Schema _schema;
  private String _schemaLocation;
  private HashMap<String,Object> _properties =
    new HashMap<String,Object>();
  private HashMap<Class,XmlAdapter> _adapters =
    new HashMap<Class,XmlAdapter>();

  private static final ValidationEventHandler _defaultValidationEventHandler 
    = new DefaultValidationEventHandler() {
      public boolean handleEvent(ValidationEvent event)
      {
        if (event == null)
          throw new IllegalArgumentException();

        return event.getSeverity() != ValidationEvent.FATAL_ERROR;
      }
    };

  private final HashMap<String,String> _ianaToJavaEncoding =
    new HashMap<String,String>();

  public AbstractMarshallerImpl()
  {
    _ianaToJavaEncoding.put("UTF-8", "UTF8");
    // XXX add more encodings
  }

  public <A extends XmlAdapter> A getAdapter(Class<A> type)
  {
    return (A)_adapters.get(type);
  }

  public AttachmentMarshaller getAttachmentMarshaller()
  {
    return _attachmentMarshaller;
  }

  protected String getEncoding()
  {
    return _encoding;
  }

  public ValidationEventHandler getEventHandler() throws JAXBException
  {
    return _validationEventHandler;
  }

  protected String getJavaEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    if (_ianaToJavaEncoding.containsKey(encoding))
      return _ianaToJavaEncoding.get(encoding);

    throw new UnsupportedEncodingException(encoding);
  }

  public Listener getListener()
  {
    return _listener;
  }

  public Node getNode(Object obj) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  protected String getNoNSSchemaLocation()
  {
    return _noNSSchemaLocation;
  }

  public Object getProperty(String name)
    throws PropertyException
  {
    if (name.equals("jaxb.encoding")) {
      return getEncoding();
    } 
    else if (name.equals("jaxb.formatted.output")) {
      return _formattedOutput;
    } 
    else if (name.equals("jaxb.schemaLocation")) {
      return getSchemaLocation();
    } 
    else if (name.equals("jaxb.noNamespaceSchemaLocation")) {
      return getNoNSSchemaLocation();
    } 
    else if (name.equals("jaxb.fragment")) {
      return _fragment;
    } 
    else if (_properties.containsKey(name)) {
      return _properties.get(name);
    }
    
    throw new PropertyException(name);
  }

  public Schema getSchema()
  {
    return _schema;
  }

  protected String getSchemaLocation()
  {
    return _schemaLocation;
  }

  protected boolean isFormattedOutput()
  {
    return _formattedOutput;
  }

  protected boolean isFragment()
  {
    return _fragment;
  }

  public final void marshal(Object obj, ContentHandler handler)
    throws JAXBException
  {
    SAXResult result = new SAXResult(handler);

    marshal(obj, result);
  }

  public final void marshal(Object obj, Node node) throws JAXBException
  {
    DOMResult result = new DOMResult(node);

    marshal(obj, result);
  }

  private XMLOutputFactory getXMLOutputFactory()
  {
    if (_factory == null) {
      _factory = XMLOutputFactory.newInstance();
      _factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                           Boolean.TRUE);
    }

    return _factory;
  }

  public final void marshal(Object obj, OutputStream os) throws JAXBException
  {
    marshal(obj, new StreamResult(os));
  }

  public final void marshal(Object obj, Writer w) throws JAXBException
  {
    marshal(obj, new StreamResult(w));
  }

  public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter)
  {
    _adapters.put(type, adapter);
  }

  public void setAdapter(XmlAdapter adapter)
  {
    _adapters.put((Class)adapter.getClass(), adapter);
  }

  public void setAttachmentMarshaller(AttachmentMarshaller am)
  {
    _attachmentMarshaller = am;
  }

  protected void setEncoding(String encoding)
  {
    _encoding = encoding;
  }

  public void setEventHandler(ValidationEventHandler handler)
    throws JAXBException
  {
    _validationEventHandler = handler;
  }

  protected void setFormattedOutput(boolean v)
  {
    _formattedOutput = v;
  }

  protected void setFragment(boolean v)
  {
    _fragment = v;
  }

  public void setListener(Listener listener)
  {
    _listener = listener;
  }

  protected void setNoNSSchemaLocation(String location)
  {
    _noNSSchemaLocation = location;
  }

  public void setProperty(String name, Object value)
    throws PropertyException
  {
    if (name.equals("jaxb.encoding")) {
      setEncoding((String)value);
    } else if (name.equals("jaxb.formatted.output")) {
      setFormattedOutput(((Boolean)value).booleanValue());
    } else if (name.equals("jaxb.schemaLocation")) {
      setSchemaLocation((String)value);
    } else if (name.equals("jaxb.noNamespaceSchemaLocation")) {
      setNoNSSchemaLocation((String)value);
    } else if (name.equals("jaxb.fragment")) {
      setFragment(((Boolean)value).booleanValue());
    } else {
      _properties.put(name, value);
    }
  }

  public void setSchema(Schema schema)
  {
    _schema = schema;
  }

  protected void setSchemaLocation(String location)
  {
    _schemaLocation = location;
  }

  public void marshal(Object obj, XMLEventWriter writer) throws JAXBException
  {
    throw new UnsupportedOperationException("subclasses must override this");
  }

  public void marshal(Object obj, XMLStreamWriter writer) throws JAXBException
  {
    throw new UnsupportedOperationException("subclasses must override this");
  }
}

