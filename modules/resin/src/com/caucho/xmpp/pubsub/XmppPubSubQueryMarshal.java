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
 * element affiliation {
 *   attribute affiliation,
 *   attribute node
 * }
 *
 * element affiliations {
 *   affiliation*
 * }
 *
 * element configure {
 *   x{jabber:x:data}?
 * }
 *
 * element create {
 *   attribute node?
 * }
 *
 * element item {
 *   attribute id?,
 *
 *   other?
 * }
 *
 * element items {
 *   attribute max_items?,
 *   attribute node,
 *   attribute subid?,
 *
 *   item*
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
 * element publish {
 *   attribute node,
 *
 *   item*
 * }
 *
 * element retract {
 *   attribute node,
 *   attribute notify?,
 *
 *   item+
 * }
 *
 * element subscribe {
 *   attribute jid,
 *   attribute node?
 * }
 *
 * element subscribe-options {
 *   required?
 * }
 *
 * element subscription {
 *   attribute jid,
 *   attribute node?,
 *   attribute subid?,
 *   attribute subscription?,
 *
 *   subscribe-options?
 * }
 *
 * element unsubscribe {
 *   attribute jid,
 *   attribute node?
 *   attribute subid?
 * }
 * </pre></code>
 */
public class XmppPubSubQueryMarshal extends AbstractXmppMarshal {
  private static final Logger log
    = Logger.getLogger(XmppPubSubQueryMarshal.class.getName());
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
    return "pubsub";
  }

  /**
   * Returns the java classname of the object
   */
  public String getClassName()
  {
    return null;
  }
  
  /**
   * Serializes the object to XML
   */
  public void toXml(XmppStreamWriter out, Serializable object)
    throws IOException, XMLStreamException
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Deserializes the object from XML
   */
  public Serializable fromXml(XmppStreamReader in)
    throws IOException, XMLStreamException
  {
    boolean isFinest = log.isLoggable(Level.FINEST);

    PubSubQuery query = null;
    
    int tag = in.nextTag();
    while (tag > 0) {
      if (isFinest)
        debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
        return query;
      }

      if (XMLStreamReader.START_ELEMENT == tag
          && "options".equals(in.getLocalName())) {
        PubSubOptions options = parseOptions(in);

        if (query instanceof PubSubSubscribeQuery) {
          PubSubSubscribeQuery subscribe = (PubSubSubscribeQuery) query;

          subscribe.setOptions(options);
        }
        else
          log.fine(this + " options with no subscribe: " + query);
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "items".equals(in.getLocalName())) {
        query = parseItems(in);
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "publish".equals(in.getLocalName())) {
        query = parsePublish(in);
      }
      else if (XMLStreamReader.START_ELEMENT == tag
          && "subscribe".equals(in.getLocalName())) {
        query = parseSubscribe(in);
      }
      else if (XMLStreamReader.START_ELEMENT == tag
          && "unsubscribe".equals(in.getLocalName())) {
        query = parseUnsubscribe(in);
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
  public PubSubItemsQuery parseItems(XmppStreamReader in)
    throws IOException, XMLStreamException
  {
    String node = in.getAttributeValue(null, "node");
    String subid = in.getAttributeValue(null, "subid");
    String maxItemsString = in.getAttributeValue(null, "max-items");

    int maxItems = 0;

    if (maxItemsString != null)
      maxItems = Integer.parseInt(maxItemsString);
    
    PubSubItemsQuery items = new PubSubItemsQuery(node, subid, maxItems);

    ArrayList<PubSubItem> itemList = new ArrayList<PubSubItem>();
    
    int tag = in.nextTag();
    while (tag > 0) {
      if (_isFinest)
        debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
        items.setItemList(itemList);

        return items;
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

    skipToEnd(in, "items");

    return items;
  }
  
  /**
   * Deserializes the object from XML
   */
  public PubSubPublishQuery parsePublish(XmppStreamReader in)
    throws IOException, XMLStreamException
  {
    String node = in.getAttributeValue(null, "node");
    
    PubSubPublishQuery pubsub = new PubSubPublishQuery(node);

    /*
    ArrayList<DataValue> valueList = new ArrayList<DataValue>();
    ArrayList<DataOption> optionList = new ArrayList<DataOption>();
    */
    
    int tag = in.nextTag();
    while (tag > 0) {
      if (_isFinest)
        debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
        return pubsub;
      }
    
      if (XMLStreamReader.START_ELEMENT == tag
          && "item".equals(in.getLocalName())) {
        pubsub.setItem(parseItem(in));
      }
      else if (XMLStreamReader.START_ELEMENT == tag) {
        log.finer(this + " <" + in.getLocalName() + "> is an unknown tag");

        skipToEnd(in, in.getLocalName());
      }

      tag = in.nextTag();
    }

    skipToEnd(in, "publish");

    return pubsub;
  }
  
  /**
   * Deserializes the object from XML
   */
  public PubSubSubscribeQuery parseSubscribe(XmppStreamReader in)
    throws IOException, XMLStreamException
  {
    String jid = in.getAttributeValue(null, "jid");
    String node = in.getAttributeValue(null, "node");
    
    PubSubSubscribeQuery subscribe = new PubSubSubscribeQuery(jid, node);

    skipToEnd(in, "subscribe");

    return subscribe;
  }
  
  /**
   * Deserializes the object from XML
   */
  public PubSubUnsubscribeQuery parseUnsubscribe(XmppStreamReader in)
    throws IOException, XMLStreamException
  {
    String jid = in.getAttributeValue(null, "jid");
    String node = in.getAttributeValue(null, "node");
    String subid = in.getAttributeValue(null, "subid");
    
    PubSubUnsubscribeQuery unsubscribe
      = new PubSubUnsubscribeQuery(jid, node, subid);

    skipToEnd(in, "unsubscribe");

    return unsubscribe;
  }
  
  /**
   * Deserializes the object from XML
   */
  public PubSubOptions parseOptions(XmppStreamReader in)
    throws IOException, XMLStreamException
  {
    String jid = in.getAttributeValue(null, "jid");
    String node = in.getAttributeValue(null, "node");
    String subid = in.getAttributeValue(null, "subid");
    
    PubSubOptions options = new PubSubOptions(jid, node, subid);

    skipToEnd(in, "options");

    return options;
  }

  private PubSubItem parseItem(XmppStreamReader in)
    throws IOException, XMLStreamException
  {
    String id = in.getAttributeValue(null, "id");

    PubSubItem item = new PubSubItem(id);
    
    int tag = in.next();

    Serializable value = in.readValue();

    item.setValue(value);

    skipToEnd(in, "item");

    return item;
  }
}
