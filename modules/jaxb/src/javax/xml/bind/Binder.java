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
import javax.xml.validation.Schema;

public abstract class Binder<XmlNode> {

  public Binder()
  {
  }

  public abstract ValidationEventHandler getEventHandler() throws JAXBException;

  public abstract Object getJAXBNode(XmlNode xmlNode);

  public abstract Object getProperty(String name) throws PropertyException;

  public abstract Schema getSchema();

  public abstract XmlNode getXMLNode(Object jaxbObject);

  public abstract void marshal(Object jaxbObject, XmlNode xmlNode)
    throws JAXBException;

  public abstract void setEventHandler(ValidationEventHandler handler)
    throws JAXBException;

  public abstract void setProperty(String name, Object value)
    throws PropertyException;

  public abstract void setSchema(Schema schema);

  public abstract Object unmarshal(XmlNode xmlNode)
    throws JAXBException;

  public abstract <T> JAXBElement<T> unmarshal(XmlNode node, 
                                               Class<T> declaredType)
    throws JAXBException;

  public abstract Object updateJAXB(XmlNode xmlNode) throws JAXBException;

  public abstract XmlNode updateXML(Object jaxbObject) throws JAXBException;

  public abstract XmlNode updateXML(Object jaxbObject, XmlNode xmlNode)
      throws JAXBException;

}

