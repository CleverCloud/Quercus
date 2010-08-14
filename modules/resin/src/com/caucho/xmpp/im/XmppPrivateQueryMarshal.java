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

package com.caucho.xmpp.im;

import com.caucho.vfs.*;
import com.caucho.xmpp.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.xml.stream.*;

/**
 * private data storage
 *
 * XEP-0049: http://www.xmpp.org/extensions/xep-0049.html
 *
 * <code><pre>
 * namespace = "jabber:iq:private"
 *
 * element query {
 *   other?
 * }
 * </pre></code>
 *
 * element group {
 *   string
 * }
 * </pre></code>
 */
public class XmppPrivateQueryMarshal extends AbstractXmppMarshal {
  private static final Logger log
    = Logger.getLogger(XmppPrivateQueryMarshal.class.getName());

  /**
   * Returns the namespace uri for the XMPP stanza value
   */
  public String getNamespaceURI()
  {
    return "jabber:iq:private";
  }

  /**
   * Returns the local name for the XMPP stanza value
   */
  public String getLocalName()
  {
    return "query";
  }

  /**
   * Returns the java classname of the object
   */
  public String getClassName()
  {
    return PrivateQuery.class.getName();
  }
  
  /**
   * Serializes the object to XML
   */
  public void toXml(XmppStreamWriter out, Serializable object)
    throws IOException, XMLStreamException
  {
    PrivateQuery query = (PrivateQuery) object;

    out.writeStartElement("", getLocalName(), getNamespaceURI());
    out.writeNamespace("", getNamespaceURI());

    if (query.getData() != null)
      out.writeValue(query.getData());
    
    out.writeEndElement(); // </query>
  }
  
  /**
   * Deserializes the object from XML
   */
  public Serializable fromXml(XmppStreamReader in)
    throws IOException, XMLStreamException
  {
    boolean isFinest = log.isLoggable(Level.FINEST);
    int tag = in.nextTag();

    PrivateQuery query = null;
    
    while (tag > 0) {
      if (isFinest)
        debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
        if (query != null)
          return query;
        else
          return new PrivateQuery();
      }
      else if (XMLStreamReader.START_ELEMENT == tag) {
        String name = in.getLocalName();
        String uri = in.getNamespaceURI();

        String data = in.readAsXmlString();

        query = new PrivateQuery(name, uri, data);
      }

      tag = in.next();
    }

    return null;
  }
}
