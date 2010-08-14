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

package com.caucho.xmpp.muc;

import com.caucho.vfs.*;
import com.caucho.xmpp.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.xml.stream.*;

/**
 * MucAdmin query
 *
 * XEP-0045: http://www.xmpp.org/extensions/xep-0045.html
 *
 *
 * <code><pre>
 * namespace = http://jabber.org/protocol/muc#admin
 *
 * element query {
 *   item+
 * }
 *
 * element actor {
 *   attribute jid
 * }
 *
 * element item {
 *   attribute affiliation?,
 *   attribute jid?,
 *   attribute nick?,
 *   attribute role?,
 *
 *   actor?
 *   &amp; reason?
 * }
 *
 * element reason {
 *   string
 * }
 * </pre></code>
 */
public class XmppMucAdminQueryMarshal extends AbstractXmppMarshal {
  private static final Logger log
    = Logger.getLogger(XmppMucAdminQueryMarshal.class.getName());
  private static final boolean _isFinest = log.isLoggable(Level.FINEST);

  /**
   * Returns the namespace uri for the XMPP stanza value
   */
  public String getNamespaceURI()
  {
    return "http://jabber.org/protocol/muc#admin";
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
    return MucAdminQuery.class.getName();
  }
  
  /**
   * Serializes the object to XML
   */
  public void toXml(XmppStreamWriter out, Serializable object)
    throws IOException, XMLStreamException
  {
    MucAdminQuery mucAdmin = (MucAdminQuery) object;

    out.writeStartElement("", getLocalName(), getNamespaceURI());
    out.writeNamespace("", getNamespaceURI());

    MucUserItem []items = mucAdmin.getItems();

    if (items != null) {
      for (MucUserItem item : items) {
        toXml(out, item);
      }
    }

    out.writeEndElement();
  }

  private void toXml(XmppStreamWriter out, MucUserItem item)
    throws IOException, XMLStreamException
  {
    out.writeStartElement("item");

    if (item.getAffiliation() != null)
      out.writeAttribute("affiliation", item.getAffiliation());

    if (item.getJid() != null)
      out.writeAttribute("jid", item.getJid());

    if (item.getNick() != null)
      out.writeAttribute("nick", item.getNick());

    if (item.getRole() != null)
      out.writeAttribute("role", item.getRole());

    if (item.getActor() != null) {
      out.writeStartElement("actor");
      out.writeAttribute("jid", item.getActor());
      out.writeEndElement(); // </actor>
    }

    if (item.getReason() != null) {
      out.writeStartElement("reason");
      out.writeCharacters(item.getReason());
      out.writeEndElement(); // </reason>
    }

    // continue

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
    
    MucAdminQuery mucAdmin = new MucAdminQuery();
    ArrayList<MucUserItem> itemList = new ArrayList<MucUserItem>();
    
    while (tag > 0) {
      if (isFinest)
        debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
        mucAdmin.setItemList(itemList);

        return mucAdmin;
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
  private MucUserItem parseItem(XMLStreamReader in)
    throws IOException, XMLStreamException
  {
    String affiliation = in.getAttributeValue(null, "affiliation");
    String jid = in.getAttributeValue(null, "jid");
    String nick = in.getAttributeValue(null, "nick");
    String role = in.getAttributeValue(null, "role");

    MucUserItem item = new MucUserItem();
    if (affiliation != null)
      item.setAffiliation(affiliation);
    
    if (jid != null)
      item.setJid(jid);
    
    if (nick != null)
      item.setNick(nick);
    
    if (role != null)
      item.setRole(role);
    
    int tag = in.nextTag();
    
    while (tag > 0) {
      if (_isFinest)
        debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
        return item;
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "actor".equals(in.getLocalName())) {
        item.setActor(in.getAttributeValue(null, "jid"));

        skipToEnd(in, "actor");
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "continue".equals(in.getLocalName())) {
        String thread = in.getAttributeValue(null, "thread");

        item.setContinue(new MucContinue(thread));

        skipToEnd(in, "continue");
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "reason".equals(in.getLocalName())) {
        item.setReason(in.getElementText());

        skipToEnd(in, "reason");
      }

      tag = in.nextTag();
    }

    return null;
  }
}
