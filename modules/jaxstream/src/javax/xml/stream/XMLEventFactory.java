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

package javax.xml.stream;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.events.*;
import java.util.Iterator;

public abstract class XMLEventFactory {

  protected XMLEventFactory()
  {
  }

  public abstract Attribute createAttribute(QName name, String value);

  public abstract Attribute createAttribute(String localName, String value);

  public abstract Attribute createAttribute(String prefix, String namespaceURI,
                                            String localName, String value);

  public abstract Characters createCData(String content);

  public abstract Characters createCharacters(String content);

  public abstract Comment createComment(String text);

  public abstract DTD createDTD(String dtd);

  public abstract EndDocument createEndDocument();

  public abstract EndElement createEndElement(QName name, Iterator namespaces);

  public abstract EndElement createEndElement(String prefix,
                                              String namespaceUri,
                                              String localName);

  public abstract EndElement createEndElement(String prefix,
                                              String namespaceUri,
                                              String localName,
                                              Iterator namespaces);

  public abstract EntityReference
    createEntityReference(String name, EntityDeclaration declaration);

  public abstract Characters createIgnorableSpace(String content);

  public abstract Namespace createNamespace(String namespaceURI);

  public abstract Namespace createNamespace(String prefix, String namespaceUri);

  public abstract ProcessingInstruction
    createProcessingInstruction(String target, String data);

  public abstract Characters createSpace(String content);

  public abstract StartDocument createStartDocument();

  public abstract StartDocument createStartDocument(String encoding);

  public abstract StartDocument createStartDocument(String encoding,
                                                    String version);

  public abstract StartDocument createStartDocument(String encoding,
                                                    String version,
                                                    boolean standalone);

  public abstract StartElement createStartElement(QName name,
                                                  Iterator attributes,
                                                  Iterator namespaces);

  public abstract StartElement createStartElement(String prefix,
                                                  String namespaceUri,
                                                  String localName);

  public abstract StartElement createStartElement(String prefix,
                                                  String namespaceUri,
                                                  String localName,
                                                  Iterator attributes,
                                                  Iterator namespaces);

  public abstract StartElement createStartElement(String prefix,
                                                  String namespaceUri,
                                                  String localName,
                                                  Iterator attributes,
                                                  Iterator namespaces,
                                                  NamespaceContext context);

  public static XMLEventFactory newInstance() throws FactoryConfigurationError
  {
    return newInstance("javax.xml.stream.XMLEventFactory",
                       Thread.currentThread().getContextClassLoader());
  }

  public static XMLEventFactory newInstance(String factoryId,
                                            ClassLoader classLoader)
    throws FactoryConfigurationError
  {
    return (XMLEventFactory)FactoryLoader
      .getFactoryLoader(factoryId).newInstance(classLoader);
  }

  public abstract void setLocation(Location location);

}

