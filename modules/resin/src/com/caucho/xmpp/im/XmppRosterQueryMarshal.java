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
 * Roster query (jabber:iq:roster defined in rfc3921)
 *
 * <code><pre>
 * element query {
 *   item*
 * }
 *
 * element item {
 *   attribute ask?,
 *   attribute jid,
 *   attribute name?,
 *   attribute subscription?,
 *
 *   group*
 * }
 *
 * element group {
 *   string
 * }
 * </pre></code>
 */
public class XmppRosterQueryMarshal extends AbstractXmppMarshal {
  private static final Logger log
    = Logger.getLogger(XmppRosterQueryMarshal.class.getName());

  /**
   * Returns the namespace uri for the XMPP stanza value
   */
  public String getNamespaceURI()
  {
    return "jabber:iq:roster";
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
    return RosterQuery.class.getName();
  }
  
  /**
   * Serializes the object to XML
   */
  public void toXml(XmppStreamWriter out, Serializable object)
    throws IOException, XMLStreamException
  {
    RosterQuery roster = (RosterQuery) object;

    out.writeStartElement("", "query", "jabber:iq:roster");
    out.writeNamespace("", "jabber:iq:roster");

    RosterItem []items = roster.getItems();

    if (items != null) {
      for (RosterItem item : items) {
        out.writeStartElement("item");

        if (item.getAsk() != null)
          out.writeAttribute("ask", item.getAsk());

        if (item.getJid() != null)
          out.writeAttribute("jid", item.getJid());

        if (item.getName() != null)
          out.writeAttribute("name", item.getName());

        if (item.getSubscription() != null)
          out.writeAttribute("subscription", item.getSubscription());

        String []groups = item.getGroup();

        if (groups != null) {
          for (String group : groups) {
            out.writeStartElement("group");
            out.writeCharacters(group);
            out.writeEndElement(); // </group>
          }
        }

        out.writeEndElement(); // </item>
      }
    }
    
    out.writeEndElement(); // </query>
  }
  
  /**
   * Deserializes the object from XML
   */
  public Serializable fromXml(XmppStreamReader in)
    throws IOException, XMLStreamException
  {
    ArrayList<RosterItem> itemList = new ArrayList<RosterItem>();

    boolean isFinest = log.isLoggable(Level.FINEST);
    int tag = in.nextTag();
    
    while (tag > 0) {
      if (isFinest)
        debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
        return new RosterQuery(itemList);
      }
      else if (XMLStreamReader.START_ELEMENT != tag
               || ! "item".equals(in.getLocalName())) {
        log.warning("expected start");

        skipToEnd(in, "query");

        return null;
      }

      String ask = null;
      String jid = null;
      String name = null;
      String subscription = null;
    
      for (int i = 0; i < in.getAttributeCount(); i++) {
        String attr = in.getAttributeLocalName(i);

        if ("ask".equals(attr))
          ask = in.getAttributeValue(i);
        else if ("jid".equals(attr))
          jid = in.getAttributeValue(i);
        else if ("name".equals(attr))
          name = in.getAttributeValue(i);
        else if ("subscription".equals(attr))
          subscription = in.getAttributeValue(i);
      }

      ArrayList<String> groups = new ArrayList<String>();
      
      tag = in.nextTag();
      while (XMLStreamReader.START_ELEMENT == in.getEventType()
             && "group".equals(in.getLocalName())) {
        groups.add(in.getElementText());

        skipToEnd(in, "group");
      }
      
      skipToEnd(in, "item");

      RosterItem item = new RosterItem(ask, jid, name, subscription, groups);

      itemList.add(item);

      tag = in.nextTag();
    }

    return null;
  }
}
