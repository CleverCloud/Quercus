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

package com.caucho.xmpp.disco;

import com.caucho.vfs.*;
import com.caucho.xmpp.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.xml.stream.*;

/**
 * DiscoItems query
 *
 * XEP-0030: http://www.xmpp.org/extensions/xep-0030.html
 *
 * <code><pre>
 * namespace = http://jabber.org/protocol/disco#items
 *
 * element query {
 *   attribute node?,
 *   item*
 * }
 *
 * element item {
 *    attribute jid,
 *    attribute name?,
 *    attribute node?
 * }
 * </pre></code>
 */
public class XmppDiscoItemsQueryMarshal extends AbstractXmppMarshal {
  private static final Logger log
    = Logger.getLogger(XmppDiscoItemsQueryMarshal.class.getName());
  private static final boolean _isFinest = log.isLoggable(Level.FINEST);

  /**
   * Returns the namespace uri for the XMPP stanza value
   */
  public String getNamespaceURI()
  {
    return "http://jabber.org/protocol/disco#items";
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
    return DiscoItemsQuery.class.getName();
  }
  
  /**
   * Serializes the object to XML
   */
  public void toXml(XmppStreamWriter out, Serializable object)
    throws IOException, XMLStreamException
  {
    DiscoItemsQuery discoItems = (DiscoItemsQuery) object;

    out.writeStartElement("", getLocalName(), getNamespaceURI());
    out.writeNamespace("", getNamespaceURI());

    if (discoItems.getNode() != null)
      out.writeAttribute("node", discoItems.getNode());
    
    DiscoItem []items = discoItems.getItems();

    if (items != null) {
      for (DiscoItem item : items) {
        out.writeStartElement("item");

        out.writeAttribute("jid", item.getJid());

        if (item.getName() != null)
          out.writeAttribute("name", item.getName());

        if (item.getNode() != null)
          out.writeAttribute("node", item.getNode());

        out.writeEndElement(); // </item>
      }
    }
    
    out.writeEndElement(); // </item>
  }
  
  /**
   * Deserializes the object from XML
   */
  public Serializable fromXml(XmppStreamReader in)
    throws IOException, XMLStreamException
  {
    boolean isFinest = log.isLoggable(Level.FINEST);
    int tag = in.nextTag();

    String node = in.getAttributeValue(null, "node");

    DiscoItemsQuery discoItems = new DiscoItemsQuery(node);
    
    ArrayList<DiscoItem> itemList = new ArrayList<DiscoItem>();
    
    while (tag > 0) {
      if (isFinest)
        debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
        discoItems.setItemList(itemList);

        return discoItems;
      }

      if (XMLStreamReader.START_ELEMENT == tag
          && "item".equals(in.getLocalName())) {
        itemList.add(parseItem(in));
      }
      else if (XMLStreamReader.START_ELEMENT == tag) {
        log.finer(this + " <" + in.getLocalName() + "> is an unknown tag");

        skipToEnd(in, in.getLocalName());
      }

      tag = in.nextTag();
    }

    return null;
  }
  
  /**
   * Deserializes the object from XML
   */
  public DiscoItem parseItem(XMLStreamReader in)
    throws IOException, XMLStreamException
  {
    String jid = in.getAttributeValue(null, "jid");
    String name = in.getAttributeValue(null, "name");
    String node = in.getAttributeValue(null, "node");

    skipToEnd(in, "item");

    return new DiscoItem(jid, name, node);
  }
}
