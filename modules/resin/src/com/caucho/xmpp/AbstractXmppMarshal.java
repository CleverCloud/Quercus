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

package com.caucho.xmpp;

import java.io.*;
import java.util.logging.*;
import javax.xml.stream.*;
import com.caucho.vfs.*;

/**
 * Marshals from an xmpp request to and from a serialized class
 */
abstract public class AbstractXmppMarshal implements XmppMarshal
{
  private static final Logger log
    = Logger.getLogger(AbstractXmppMarshal.class.getName());
  
  /**
   * Serializes the object to XML
   */
  abstract public void toXml(XmppStreamWriter out, Serializable object)
    throws IOException, XMLStreamException;
  
  /**
   * Deserializes the object from XML
   */
  abstract public Serializable fromXml(XmppStreamReader in)
    throws IOException, XMLStreamException;

  protected void skipToEnd(XMLStreamReader in, String tagName)
    throws IOException, XMLStreamException
  {
    if (in == null)
      return;

    int tag = in.getEventType();
    for (; tag > 0; tag = in.next()) {
      if (log.isLoggable(Level.FINEST))
        debug(in);

      if (tag == XMLStreamReader.START_ELEMENT) {
      }
      else if (tag == XMLStreamReader.END_ELEMENT) {
        if (tagName.equals(in.getLocalName()))
          return;
      }
    }
  }

  protected void expectEnd(XMLStreamReader in, String tagName)
    throws IOException, XMLStreamException
  {
    expectEnd(in, tagName, in.nextTag());
  }

  protected void expectEnd(XMLStreamReader in, String tagName, int tag)
    throws IOException, XMLStreamException
  {
    if (tag != XMLStreamReader.END_ELEMENT)
      throw new IllegalStateException("expected </" + tagName + ">"
                                      + " at <" + in.getLocalName() + ">");

    else if (! tagName.equals(in.getLocalName()))
      throw new IllegalStateException("expected </" + tagName + ">"
                                      + " at </" + in.getLocalName() + ">");
  }

  protected void debug(XMLStreamReader in)
    throws IOException, XMLStreamException
  {
    if (XMLStreamReader.START_ELEMENT == in.getEventType()) {
      StringBuilder sb = new StringBuilder();
      sb.append("<").append(in.getLocalName());

      if (in.getNamespaceURI() != null)
        sb.append("{").append(in.getNamespaceURI()).append("}");

      for (int i = 0; i < in.getAttributeCount(); i++) {
        sb.append(" ");
        sb.append(in.getAttributeLocalName(i));
        sb.append("='");
        sb.append(in.getAttributeValue(i));
        sb.append("'");
      }
      sb.append(">");

      log.finest(this + " " + sb);
    }
    else if (XMLStreamReader.END_ELEMENT == in.getEventType()) {
      log.finest(this + " </" + in.getLocalName() + ">");
    }
    else if (XMLStreamReader.CHARACTERS == in.getEventType()) {
      String text = in.getText().trim();

      if (! "".equals(text))
        log.finest(this + " text='" + text + "'");
    }
    else
      log.finest(this + " tag=" + in.getEventType());
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
