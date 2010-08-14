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
 * MucUser query
 *
 *
 * XEP-0045: http://www.xmpp.org/extensions/xep-0045.html
 *

 * <code><pre>
 * namespace = http://jabber.org/protocol/muc#user
 *
 * element x {
 *   decline?
 *   &amp; destroy?
 *   &amp; invite*
 *   &amp; item?
 *   &amp; password?
 *   &amp; status*
 * }
 *
 * element decline {
 *   @from?
 *   &amp; @to?
 *   &amp; reason?
 * }
 *
 * element invite {
 *   @from?
 *   &amp; @to?
 *   &amp; reason?
 * }
 *
 * element destroy {
 *   @jid?
 *   &amp; reason?
 * }
 *
 * element item {
 *   @affiliation?
 *   &amp; @jid?
 *   &amp; @nick?
 *   &amp; @role?
 *   &amp; actor?
 *   &amp; reason?
 *   &amp; continue?
 * }
 *
 * element actor {
 *   @jid
 * }
 *
 * element continue {
 *   @thread?
 * }
 *
 * element status {
 *   @code
 * }
 * </pre></code>
 */
public class XmppMucUserQueryMarshal extends AbstractXmppMarshal {
  private static final Logger log
    = Logger.getLogger(XmppMucUserQueryMarshal.class.getName());
  private static final boolean _isFinest = log.isLoggable(Level.FINEST);

  /**
   * Returns the namespace uri for the XMPP stanza value
   */
  public String getNamespaceURI()
  {
    return "http://jabber.org/protocol/muc#user";
  }

  /**
   * Returns the local name for the XMPP stanza value
   */
  public String getLocalName()
  {
    return "x";
  }

  /**
   * Returns the java classname of the object
   */
  public String getClassName()
  {
    return MucUserQuery.class.getName();
  }
  
  /**
   * Serializes the object to XML
   */
  public void toXml(XmppStreamWriter out, Serializable object)
    throws IOException, XMLStreamException
  {
    MucUserQuery mucUser = (MucUserQuery) object;

    out.writeStartElement("", getLocalName(), getNamespaceURI());
    out.writeNamespace("", getNamespaceURI());

    if (mucUser.getItem() != null)
      toXml(out, mucUser.getItem());
    
    MucInvite []invites = mucUser.getInvite();

    if (invites != null) {
      for (MucInvite invite : invites) {
        out.writeStartElement("invite");

        if (invite.getTo() != null)
          out.writeAttribute("to", invite.getTo());

        if (invite.getFrom() != null)
          out.writeAttribute("from", invite.getFrom());

        if (invite.getReason() != null) {
          out.writeStartElement("reason");
          out.writeCharacters(invite.getReason());
          out.writeEndElement(); // </reason>
        }

        out.writeEndElement(); // </invite>
      }
    }

    MucDecline decline = mucUser.getDecline();
    if (decline != null) {
      out.writeStartElement("decline");

      if (decline.getTo() != null)
        out.writeAttribute("to", decline.getTo());

      if (decline.getFrom() != null)
        out.writeAttribute("from", decline.getFrom());

      if (decline.getReason() != null) {
        out.writeStartElement("reason");
        out.writeCharacters(decline.getReason());
        out.writeEndElement(); // </reason>
      }

      out.writeEndElement(); // </decline>
    }

    MucDestroy destroy = mucUser.getDestroy();
    if (destroy != null) {
      out.writeStartElement("destroy");

      if (destroy.getJid() != null)
        out.writeAttribute("jid", destroy.getJid());

      if (destroy.getReason() != null) {
        out.writeStartElement("reason");
        out.writeCharacters(destroy.getReason());
        out.writeEndElement(); // </reason>
      }

      out.writeEndElement(); // </destroy>
    }
    
    MucStatus []statusList = mucUser.getStatus();

    if (statusList != null) {
      for (MucStatus status : statusList) {
        out.writeStartElement("status");

        out.writeAttribute("code", String.valueOf(status.getCode()));

        out.writeEndElement(); // </status>
      }
    }
    
    out.writeEndElement(); // </x>
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
    MucUserQuery mucUser = new MucUserQuery();
    ArrayList<MucInvite> inviteList = new ArrayList<MucInvite>();
    ArrayList<MucStatus> statusList = new ArrayList<MucStatus>();
    
    while (tag > 0) {
      if (isFinest)
        debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
        mucUser.setInviteList(inviteList);
        mucUser.setStatusList(statusList);

        return mucUser;
      }

      if (XMLStreamReader.START_ELEMENT == tag
          && "decline".equals(in.getLocalName())) {
        mucUser.setDecline(parseDecline(in));
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "destroy".equals(in.getLocalName())) {
        mucUser.setDestroy(parseDestroy(in));
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "invite".equals(in.getLocalName())) {
        inviteList.add(parseInvite(in));
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "item".equals(in.getLocalName())) {
        mucUser.setItem(parseItem(in));
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "status".equals(in.getLocalName())) {
        statusList.add(parseStatus(in));
      }

      tag = in.nextTag();
    }

    return null;
  }
  
  /**
   * Deserializes the object from XML
   */
  public MucInvite parseInvite(XMLStreamReader in)
    throws IOException, XMLStreamException
  {
    String to = in.getAttributeValue(null, "to");
    String from = in.getAttributeValue(null, "from");
    String reason = null;

    int tag = in.nextTag();
    
    while (tag > 0) {
      if (_isFinest)
        debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
        return new MucInvite(to, from, reason);
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "reason".equals(in.getLocalName())) {
        reason = in.getElementText();

        skipToEnd(in, "reason");
      }

      tag = in.nextTag();
    }

    return null;
  }
  
  /**
   * Deserializes the object from XML
   */
  public MucDecline parseDecline(XMLStreamReader in)
    throws IOException, XMLStreamException
  {
    String to = in.getAttributeValue(null, "to");
    String from = in.getAttributeValue(null, "from");
    String reason = null;

    int tag = in.nextTag();
    
    while (tag > 0) {
      if (_isFinest)
        debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
        return new MucDecline(to, from, reason);
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "reason".equals(in.getLocalName())) {
        reason = in.getElementText();

        skipToEnd(in, "reason");
      }

      tag = in.nextTag();
    }

    return null;
  }
  
  /**
   * Deserializes the object from XML
   */
  public MucDestroy parseDestroy(XMLStreamReader in)
    throws IOException, XMLStreamException
  {
    String jid = in.getAttributeValue(null, "jid");
    String reason = null;

    int tag = in.nextTag();
    
    while (tag > 0) {
      if (_isFinest)
        debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
        return new MucDestroy(jid, reason);
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "reason".equals(in.getLocalName())) {
        reason = in.getElementText();

        skipToEnd(in, "reason");
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
  
  /**
   * Deserializes the object from XML
   */
  public MucStatus parseStatus(XMLStreamReader in)
    throws IOException, XMLStreamException
  {
    String codeString = in.getAttributeValue(null, "code");
    int code = 0;

    if (codeString != null)
      code = Integer.parseInt(codeString);

    int tag = in.nextTag();
    
    while (tag > 0) {
      if (_isFinest)
        debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
        return new MucStatus(code);
      }

      tag = in.nextTag();
    }

    return null;
  }
}
