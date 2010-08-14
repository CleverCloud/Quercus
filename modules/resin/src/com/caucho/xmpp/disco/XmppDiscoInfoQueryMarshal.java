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
 * DiscoInfo query
 *
 * XEP-0030: http://www.xmpp.org/extensions/xep-0030.html
 *
 * <code><pre>
 * namespace = http://jabber.org/protocol/disco#info
 *
 * element query {
 *   attribute node?,
 *   identity*,
 *   feature*
 * }
 *
 * element identity {
 *    attribute category,
 *    attribute name?,
 *    attribute type
 * }
 *
 * element feature {
 *    attribute var
 * }
 * </pre></code>
 */
public class XmppDiscoInfoQueryMarshal extends AbstractXmppMarshal {
  private static final Logger log
    = Logger.getLogger(XmppDiscoInfoQueryMarshal.class.getName());
  private static final boolean _isFinest = log.isLoggable(Level.FINEST);

  /**
   * Returns the namespace uri for the XMPP stanza value
   */
  public String getNamespaceURI()
  {
    return "http://jabber.org/protocol/disco#info";
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
    return DiscoInfoQuery.class.getName();
  }
  
  /**
   * Serializes the object to XML
   */
  public void toXml(XmppStreamWriter out, Serializable object)
    throws IOException, XMLStreamException
  {
    DiscoInfoQuery discoInfo = (DiscoInfoQuery) object;

    out.writeStartElement("", getLocalName(), getNamespaceURI());
    out.writeNamespace("", getNamespaceURI());

    if (discoInfo.getNode() != null)
      out.writeAttribute("node", discoInfo.getNode());
    
    DiscoIdentity []identityList = discoInfo.getIdentity();

    if (identityList != null) {
      for (DiscoIdentity identity : identityList) {
        out.writeStartElement("identity");

        out.writeAttribute("category", identity.getCategory());
        out.writeAttribute("type", identity.getType());

        if (identity.getName() != null)
          out.writeAttribute("name", identity.getName());

        out.writeEndElement(); // </identity>
      }
    }
    
    DiscoFeature []featureList = discoInfo.getFeature();

    if (featureList != null) {
      for (DiscoFeature feature : featureList) {
        out.writeStartElement("feature");

        out.writeAttribute("var", feature.getVar());

        out.writeEndElement(); // </feature>
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
    boolean isFinest = log.isLoggable(Level.FINEST);
    int tag = in.nextTag();

    String node = in.getAttributeValue(null, "node");

    DiscoInfoQuery discoInfo = new DiscoInfoQuery(node);
    
    ArrayList<DiscoIdentity> identityList = new ArrayList<DiscoIdentity>();
    ArrayList<DiscoFeature> featureList = new ArrayList<DiscoFeature>();
    
    while (tag > 0) {
      if (isFinest)
        debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
        discoInfo.setIdentityList(identityList);
        discoInfo.setFeatureList(featureList);

        return discoInfo;
      }

      if (XMLStreamReader.START_ELEMENT == tag
          && "feature".equals(in.getLocalName())) {
        featureList.add(parseFeature(in));
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "identity".equals(in.getLocalName())) {
        identityList.add(parseIdentity(in));
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
  public DiscoFeature parseFeature(XMLStreamReader in)
    throws IOException, XMLStreamException
  {
    String var = in.getAttributeValue(null, "var");

    skipToEnd(in, "feature");

    return new DiscoFeature(var);
  }
  
  /**
   * Deserializes the object from XML
   */
  public DiscoIdentity parseIdentity(XMLStreamReader in)
    throws IOException, XMLStreamException
  {
    String category = in.getAttributeValue(null, "category");
    String type = in.getAttributeValue(null, "type");
    String name = in.getAttributeValue(null, "name");

    skipToEnd(in, "identity");

    return new DiscoIdentity(category, type, name);
  }
}
