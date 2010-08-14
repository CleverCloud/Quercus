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

package com.caucho.xmpp.pubsub;

import com.caucho.vfs.*;
import com.caucho.xmpp.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.xml.stream.*;

/**
 * pubsub query
 *
 * XEP-0060: http://www.xmpp.org/extensions/xep-0060.html
 *
 * <code><pre>
 * namespace = http://jabber.org/protocol/pubsub
 *
 * element pubsub {
 *   (create, configure?)
 *   | (subscribe?, options?)
 *   | affiliations
 *   | items
 *   | publish
 *   | retract
 *   | subscription
 *   | subscriptions
 *   | unsubscribe
 * }
 *
 * element options {
 *   attribute jid,
 *   attribute node?,
 *   attribute subid?,
 *
 *   x{jabber:x:data}*
 * }
 *
 * element subscribe {
 *   attribute jid,
 *   attribute node?
 * }
 * </pre></code>
 */
public class XmppPubSubSubscribeQueryMarshal extends AbstractXmppMarshal {
  private static final Logger log
    = Logger.getLogger(XmppPubSubSubscribeQueryMarshal.class.getName());
  private static final boolean _isFinest = log.isLoggable(Level.FINEST);

  /**
   * Returns the namespace uri for the XMPP stanza value
   */
  public String getNamespaceURI()
  {
    return "http://jabber.org/protocol/pubsub";
  }

  /**
   * Returns the local name for the XMPP stanza value
   */
  public String getLocalName()
  {
    return null; // this class doesn't deserialize
  }

  /**
   * Returns the java classname of the object
   */
  public String getClassName()
  {
    return PubSubSubscribeQuery.class.getName();
  }
  
  /**
   * Serializes the object to XML
   */
  @Override
  public void toXml(XmppStreamWriter out, Serializable object)
    throws IOException, XMLStreamException
  {
    PubSubSubscribeQuery subscribe = (PubSubSubscribeQuery) object;

    out.writeStartElement("", "pubsub", getNamespaceURI());
    out.writeNamespace("", getNamespaceURI());

    out.writeStartElement("subscribe");

    if (subscribe.getJid() != null)
      out.writeAttribute("jid", subscribe.getJid());

    if (subscribe.getNode() != null)
      out.writeAttribute("node", subscribe.getNode());

    out.writeEndElement(); // </subscribe>
    
    PubSubOptions options = subscribe.getOptions();

    if (options != null) {
      out.writeStartElement("options");

      if (options.getJid() != null)
        out.writeAttribute("jid", options.getJid());

      if (options.getNode() != null)
        out.writeAttribute("node", options.getNode());

      if (options.getSubid() != null)
        out.writeAttribute("subid", options.getSubid());

      out.writeEndElement(); // </options>
    }
    
    out.writeEndElement(); // </pubsub>
  }
  
  /**
   * Deserializes the object from XML
   */
  public Serializable fromXml(XmppStreamReader in)
    throws IOException, XMLStreamException
  {
    throw new UnsupportedOperationException();
  }
}
