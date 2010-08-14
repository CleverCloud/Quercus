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

package com.caucho.xmpp;

import java.io.*;
import java.util.logging.*;
import javax.xml.stream.*;
import javax.xml.namespace.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.xml.stream.*;

/**
 * Marshals from an xmpp request to and from a serialized class
 */
public class XmppStreamReaderImpl extends XMLStreamReaderImpl
  implements XmppStreamReader
{
  private static final L10N L = new L10N(XmppStreamReaderImpl.class);
  private static final Logger log
    = Logger.getLogger(XmppStreamReaderImpl.class.getName());
  
  private XmppMarshalFactory _marshalFactory;
  
  XmppStreamReaderImpl(ReadStream is, XmppMarshalFactory factory)
    throws XMLStreamException
  {
    super(is);

    _marshalFactory = factory;
  }

  public Serializable readValue()
    throws IOException, XMLStreamException
  {
    QName name = getName();

    Serializable query = null;

    XmppMarshal marshal = _marshalFactory.getUnserialize(name);

    if (marshal != null)
      return marshal.fromXml(this);
    else
      return readAsXmlString();
  }

  public String readAsXmlString()
    throws IOException, XMLStreamException
  {
    StringBuilder sb = new StringBuilder();
    int depth = 0;

    while (true) {
      if (XMLStreamReader.START_ELEMENT == getEventType()) {
        depth++;

        String prefix = getPrefix();

        sb.append("<");

        if (! "".equals(prefix)) {
          sb.append(prefix);
          sb.append(":");
        }

        sb.append(getLocalName());

        if (getNamespaceURI() != null) {
          if ("".equals(prefix))
            sb.append(" xmlns");
          else
            sb.append(" xmlns:").append(prefix);

          sb.append("=\"");
          sb.append(getNamespaceURI()).append("\"");
        }

        for (int i = 0; i < getAttributeCount(); i++) {
          sb.append(" ");
          sb.append(getAttributeLocalName(i));
          sb.append("=\"");
          sb.append(getAttributeValue(i));
          sb.append("\"");
        }
        sb.append(">");

        log.finest(this + " " + sb);
      }
      else if (XMLStreamReader.END_ELEMENT == getEventType()) {
        depth--;

        sb.append("</");

        String prefix = getPrefix();
        if (! "".equals(prefix))
          sb.append(prefix).append(":");

        sb.append(getLocalName());
        sb.append(">");

        if (depth == 0)
          return sb.toString();
      }
      else if (XMLStreamReader.CHARACTERS == getEventType()) {
        sb.append(getText());
      }
      else {
        log.finer(this + " tag=" + getEventType());

        return sb.toString();
      }

      if (next() < 0) {
        log.finer(this + " unexpected end of file");

        return sb.toString();
      }
    }
  }
}
