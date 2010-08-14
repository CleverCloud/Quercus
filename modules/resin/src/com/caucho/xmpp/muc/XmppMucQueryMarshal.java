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

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.xmpp.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.xml.stream.*;

/**
 * Muc query
 *
 * XEP-0045: http://www.xmpp.org/extensions/xep-0045.html
 *
 * <code><pre>
 * namespace = http://jabber.org/protocol/muc
 *
 * element x {
 *   history?,
 *   password?
 * }
 *
 * element item {
 *   attribute maxchars?,
 *   attribute maxstanzas?,
 *   attribute seconds?,
 *   attribute since?
 * }
 * </pre></code>
 */
public class XmppMucQueryMarshal extends AbstractXmppMarshal {
  private static final Logger log
    = Logger.getLogger(XmppMucQueryMarshal.class.getName());
  private static final boolean _isFinest = log.isLoggable(Level.FINEST);

  /**
   * Returns the namespace uri for the XMPP stanza value
   */
  public String getNamespaceURI()
  {
    return "http://jabber.org/protocol/muc";
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
    return MucQuery.class.getName();
  }
  
  /**
   * Serializes the object to XML
   */
  public void toXml(XmppStreamWriter out, Serializable object)
    throws IOException, XMLStreamException
  {
    MucQuery muc = (MucQuery) object;

    out.writeStartElement("", getLocalName(), getNamespaceURI());
    out.writeNamespace("", getNamespaceURI());

    if (muc.getPassword() != null)
      out.writeAttribute("password", muc.getPassword());

    if (muc.getHistory() != null)
      toXml(out, muc.getHistory());

    out.writeEndElement();
  }

  private void toXml(XmppStreamWriter out, MucHistory history)
    throws IOException, XMLStreamException
  {
    out.writeStartElement("history");

    if (history.getMaxChars() > 0)
      out.writeAttribute("maxchars", String.valueOf(history.getMaxChars()));

    if (history.getMaxStanzas() > 0)
      out.writeAttribute("maxstanzas", String.valueOf(history.getMaxStanzas()));

    if (history.getSeconds() > 0)
      out.writeAttribute("seconds", String.valueOf(history.getSeconds()));

    if (history.getSince() != null)
      out.writeAttribute("since", QDate.formatISO8601(history.getSince().getTime()));

    out.writeEndElement(); // </history>
  }
  
  /**
   * Deserializes the object from XML
   */
  public Serializable fromXml(XmppStreamReader in)
    throws IOException, XMLStreamException
  {
    boolean isFinest = log.isLoggable(Level.FINEST);
    int tag = in.nextTag();

    MucQuery muc = new MucQuery();
    
    while (tag > 0) {
      if (isFinest)
        debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
        return muc;
      }

      if (XMLStreamReader.START_ELEMENT == tag
          && "password".equals(in.getLocalName())) {
        muc.setPassword(in.getElementText());

        skipToEnd(in, "password");
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "history".equals(in.getLocalName())) {
        muc.setHistory(parseHistory(in));
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
  private MucHistory parseHistory(XMLStreamReader in)
    throws IOException, XMLStreamException
  {
    String maxChars = in.getAttributeValue(null, "maxchars");
    String maxStanzas = in.getAttributeValue(null, "maxstanzas");
    String seconds = in.getAttributeValue(null, "seconds");
    String since = in.getAttributeValue(null, "since");

    MucHistory history = new MucHistory();

    if (maxChars != null)
      history.setMaxChars(Integer.parseInt(maxChars));

    if (maxStanzas != null)
      history.setMaxStanzas(Integer.parseInt(maxStanzas));

    if (seconds != null)
      history.setSeconds(Integer.parseInt(seconds));

    if (since != null) {
      try {
        history.setSince(new Date(new QDate().parseDate(since)));
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    skipToEnd(in, "history");

    return history;
  }
}
