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

package com.caucho.xml.stream;

import static javax.xml.XMLConstants.*;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import static javax.xml.stream.XMLStreamConstants.*;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.io.IOException;

import java.util.Iterator;

import com.caucho.util.L10N;

/**
 * Utility class to do namespace repairs on XMLStreamWriters that don't have
 * repair enabled.  Used by JAXB.
 **/
public class StaxUtil {
  public static final L10N L = new L10N(StaxUtil.class);

  public static QName resolveStringToQName(String string, XMLStreamReader in)
    throws XMLStreamException
  {
    return resolveStringToQName(string, in.getNamespaceContext());
  }

  public static QName resolveStringToQName(String string, 
                                           NamespaceContext context)
    throws XMLStreamException
  {
    int colon = string.indexOf(':');

    if (colon < 0)
      return new QName(string);

    if (colon + 1 >= string.length())
      throw new XMLStreamException(L.l("Invalid qualified name: {0}", string));

    String prefix = string.substring(0, colon);
    String localName = string.substring(colon + 1);
    String namespace = context.getNamespaceURI(prefix);

    if (NULL_NS_URI.equals(namespace))
      throw new XMLStreamException(L.l("No namespace for qualifed name: {0}", string));

    return new QName(namespace, localName, prefix);
  }

  /**
   * Ensures that a given namespace exists within the namespace context
   * given.
   **/
  public static void repairNamespace(XMLStreamWriter out, String namespace)
    throws XMLStreamException
  {
    try {
      Object repairing = 
        out.getProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES);

      if (Boolean.TRUE.equals(repairing))
        return;
    }
    catch (IllegalArgumentException e) {
    }
    catch (NullPointerException e) {
    }

    getNamespacePrefix(out, namespace);
  }

  /**
   * Ensures that a given namespace exists within the namespace context
   * given and returns the prefix.
   **/
  public static String getNamespacePrefix(XMLStreamWriter out, String namespace)
    throws XMLStreamException
  {
    NamespaceContext context = out.getNamespaceContext();
    String prefix = context.getPrefix(namespace);

    if (prefix != null)
      return prefix;

    if (context instanceof NamespaceWriterContext) {
      NamespaceWriterContext writerContext = (NamespaceWriterContext) context;

      boolean repair = writerContext.getRepair();

      writerContext.setRepair(true);
      prefix = writerContext.declare(namespace);
      writerContext.setRepair(repair);

      return prefix;
    }
    else {
      // not one of ours... 
      // we have to search for a unique prefix...
      int i = 0;
      String unique = "ns" + i;

      while (true) {
        boolean found = false;
        Iterator iterator = context.getPrefixes(namespace);

        while (iterator.hasNext()) {
          prefix = iterator.next().toString();

          if (prefix.equals(unique)) {
            i++;
            unique = "ns" + i;
            found = true;
            break;
          }
        }

        if (! found)
          break;
      }

      out.writeNamespace(unique, namespace);

      return unique;
    }
  }  
  
  /**
   * Ensures that a given prefix->namespace mapping exists within the 
   * namespace context given.
   **/
  public static void repairNamespace(XMLStreamWriter out, 
                                     String prefix, String namespace)
    throws XMLStreamException
  {
    try {
      Object repairing = 
        out.getProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES);

      if (Boolean.TRUE.equals(repairing))
        return;
    }
    catch (IllegalArgumentException e) {
    }
    catch (NullPointerException e) {
    }

    NamespaceContext context = out.getNamespaceContext();

    String oldPrefix = context.getPrefix(namespace);

    if (! prefix.equals(oldPrefix))
      out.writeNamespace(prefix, namespace);
  }

  public static XMLStreamWriter toRepairingXMLStreamWriter(XMLStreamWriter out)
  {
    try {
      Object repairing = 
        out.getProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES);

      if (Boolean.TRUE.equals(repairing))
        return out;
    }
    catch (IllegalArgumentException e) {
    }
    catch (NullPointerException e) {
    }

    if (out instanceof XMLStreamWriterImpl) {
      ((XMLStreamWriterImpl) out).setRepair(true);

      return out;
    }
    else {
      return new XMLStreamWriterRepairingWrapper(out);
    }
  }

  /**
   * Copys all the events from the input to the output, without going past
   * an unmatch end element.
   *
   * E.g.: if the input is at the start of <y>, it will only read just past </y>
   *
   * <x>
   *   <y>
   *     <z/>
   *   </y>
   * </x>
   *
   **/
  public static void copyReaderToWriter(XMLStreamReader in, XMLStreamWriter out)
    throws XMLStreamException
  {
    int depth = 0;

    do {
      switch (in.getEventType()) {
        case ATTRIBUTE:
          break;

        case CDATA:
          out.writeCData(in.getText());
          break;

        case CHARACTERS:
          out.writeCharacters(in.getText());
          break;

        case COMMENT:
          out.writeComment(in.getText());
          break;

        case DTD:
          out.writeDTD(in.getText());
          break;

        case END_DOCUMENT:
          out.writeEndDocument();
          break;

        case END_ELEMENT:
          depth--;

          if (depth < 0) {
            out.flush();

            return;
          }

          out.writeEndElement();
          break;

        case ENTITY_REFERENCE:
          out.writeEntityRef(in.getText());
          break;

        case NAMESPACE:
          break;

        case PROCESSING_INSTRUCTION:
          out.writeProcessingInstruction(in.getPITarget(), in.getPIData());
          break;

        case SPACE:
          out.writeCharacters(in.getText());
          break;

        case START_DOCUMENT:
          out.writeStartDocument(in.getEncoding(), in.getVersion());
          break;

        case START_ELEMENT:
          QName qname = in.getName();

          if (qname.getNamespaceURI() == null ||
              "".equals(qname.getNamespaceURI()))
            out.writeStartElement(qname.getLocalPart());
          else if (qname.getPrefix() == null || "".equals(qname.getPrefix()))
            out.writeStartElement(qname.getNamespaceURI(), 
                                  qname.getLocalPart());
          else
            out.writeStartElement(qname.getPrefix(), 
                                  qname.getLocalPart(),
                                  qname.getNamespaceURI());

          for (int i = 0; i < in.getAttributeCount(); i++) {
            qname = in.getAttributeName(i);
            String value = in.getAttributeValue(i);

            if (qname.getNamespaceURI() == null ||
                "".equals(qname.getNamespaceURI()))
              out.writeAttribute(qname.getLocalPart(), value);
            else if (qname.getPrefix() == null || "".equals(qname.getPrefix()))
              out.writeAttribute(qname.getNamespaceURI(), 
                                 qname.getLocalPart(),
                                 value);
            else
              out.writeAttribute(qname.getPrefix(), 
                                 qname.getNamespaceURI(),
                                 qname.getLocalPart(),
                                 value);
          }

          for (int i = 0; i < in.getNamespaceCount(); i++)
            out.writeNamespace(in.getNamespacePrefix(i), in.getNamespaceURI(i));

          depth++;
          break;
      }

      if (in.hasNext())
        in.next();
      else
        break;
    } 
    while (depth >= 0);

    out.flush();
  }

  public static String constantToString(int constant) 
    throws XMLStreamException
  {
    switch(constant) {
      case ATTRIBUTE: return "ATTRIBUTE";
      case CDATA: return "CDATA";
      case CHARACTERS: return "CHARACTERS";
      case COMMENT: return "COMMENT";
      case DTD: return "DTD";
      case END_DOCUMENT: return "END_DOCUMENT";
      case END_ELEMENT: return "END_ELEMENT";
      case ENTITY_DECLARATION: return "ENTITY_DECLARATION";
      case ENTITY_REFERENCE: return "ENTITY_REFERENCE";
      case NAMESPACE: return "NAMESPACE";
      case NOTATION_DECLARATION: return "NOTATION_DECLARATION";
      case PROCESSING_INSTRUCTION: return "PROCESSING_INSTRUCTION";
      case SPACE: return "SPACE";
      case START_DOCUMENT: return "START_DOCUMENT";
      case START_ELEMENT: return "START_ELEMENT";
      case -1:
        throw new XMLStreamException(L.l("Unexpected end of stream"));
      default:
        throw new RuntimeException(L.l("constantToString({0}) unknown", constant));
    }
  }

  public static String printStreamState(XMLStreamReader in)
    throws XMLStreamException
  {
    StringBuilder sb = new StringBuilder(constantToString(in.getEventType()));

    if (in.getEventType() == in.START_ELEMENT ||
        in.getEventType() == in.END_ELEMENT) {
      sb.append(": ");
      sb.append(in.getName());
    }
    else if (in.getEventType() == in.CHARACTERS) {
      sb.append(": ");
      sb.append(in.getText());
    }

    return sb.toString();
  }

  /**
   * Converts a QName to a String using the context of a XMLStreamWriter.
   * Intended for writing QNames as attributes or text in a XMLStreamWriter
   * that's passed in.
   **/
  public static String qnameToString(XMLStreamWriter out, QName qname)
    throws XMLStreamException
  {
    if (qname.getNamespaceURI() == null || "".equals(qname.getNamespaceURI()))
      return qname.getLocalPart();

    NamespaceContext context = out.getNamespaceContext();
    String prefix = context.getPrefix(qname.getNamespaceURI());

    if (prefix == null) {
      if (qname.getPrefix() != null && ! "".equals(qname.getPrefix())) {
        out.writeNamespace(qname.getPrefix(), qname.getNamespaceURI());

        return qname.getPrefix() + ':' + qname.getLocalPart();
      }

      prefix = getNamespacePrefix(out, qname.getNamespaceURI());

      // XXX this shouldn't be necessary
      out.writeNamespace(prefix, qname.getNamespaceURI());
    }

    return prefix + ':' + qname.getLocalPart();
  }

  public static void writeStartElement(XMLStreamWriter out, QName name)
    throws XMLStreamException
  {
    if (name == null)
      return;

    if (name.getPrefix() != null && ! "".equals(name.getPrefix()))
      out.writeStartElement(name.getPrefix(), 
                            name.getLocalPart(), 
                            name.getNamespaceURI());
    else if (name.getNamespaceURI() != null && 
             ! "".equals(name.getNamespaceURI()))
      out.writeStartElement(name.getNamespaceURI(), name.getLocalPart());
    else
      out.writeStartElement(name.getLocalPart());
  }

  public static void writeEndElement(XMLStreamWriter out, QName name)
    throws XMLStreamException
  {
    if (name == null)
      return;

    out.writeEndElement();
  }

  public static void writeAttribute(XMLStreamWriter out, 
                                    QName name, String value)
    throws XMLStreamException
  {
    if (name.getNamespaceURI() == null || "".equals(name.getNamespaceURI()))
      out.writeAttribute(name.getLocalPart(), value);

    else if (name.getPrefix() == null || "".equals(name.getPrefix()))
      out.writeAttribute(name.getNamespaceURI(), name.getLocalPart(), value);
    
    else
      out.writeAttribute(name.getPrefix(), 
                         name.getNamespaceURI(), 
                         name.getLocalPart(), 
                         value);
  }
}
