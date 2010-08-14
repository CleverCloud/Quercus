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

package com.caucho.xmpp.atom;

import com.caucho.vfs.*;
import com.caucho.xmpp.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.xml.stream.*;

/**
 * atom
 *
 * RFC-4287: http://www.ietf.org/html/rfc4287.html
 *
 * <code><pre>
 * namespace = http://www.w3c.org/2005/Atom
 *
 * element entry {
 * }
 * </pre></code>
 */
public class XmppAtomEntryMarshal extends AbstractXmppMarshal {
  private static final Logger log
    = Logger.getLogger(XmppAtomEntryMarshal.class.getName());
  private static final boolean _isFinest = log.isLoggable(Level.FINEST);

  /**
   * Returns the namespace uri for the XMPP stanza value
   */
  public String getNamespaceURI()
  {
    return "http://www.w3c.org/2005/Atom";
  }

  /**
   * Returns the local name for the XMPP stanza value
   */
  public String getLocalName()
  {
    return "entry";
  }

  /**
   * Returns the java classname of the object
   */
  public String getClassName()
  {
    return AtomEntry.class.getName();
  }
  
  /**
   * Serializes the object to XML
   */
  public void toXml(XmppStreamWriter out, Serializable object)
    throws IOException, XMLStreamException
  {
    AtomEntry entry = (AtomEntry) object;

    out.writeStartElement("", getLocalName(), getNamespaceURI());
    out.writeNamespace("", getNamespaceURI());
    
    out.writeEndElement(); // </entry>
  }
  
  /**
   * Deserializes the object from XML
   */
  public Serializable fromXml(XmppStreamReader in)
    throws IOException, XMLStreamException
  {
    boolean isFinest = log.isLoggable(Level.FINEST);

    AtomEntry entry = new AtomEntry();

    skipToEnd(in, "entry");

    return entry;
  }
}
