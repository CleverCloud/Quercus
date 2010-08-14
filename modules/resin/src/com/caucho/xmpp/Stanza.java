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

import java.io.IOException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.caucho.vfs.WriteStream;

/**
 * XMPP protocol
 */
public class Stanza {
  private String _id;
  private String _from;
  private String _to;
  private String _type;

  public Stanza()
  {
  }

  public Stanza(String id, String type, String from, String to)
  {
    _id = id;
    _type = type;
    _from = from;
    _to = to;
  }

  public Stanza(XMLStreamReader in)
    throws IOException, XMLStreamException
  {
    for (int i = in.getAttributeCount() - 1; i >= 0; i--) {
      String name = in.getAttributeLocalName(i);

      if ("id".equals(name))
        _id = in.getAttributeValue(i);
      else if ("type".equals(name))
        _type = in.getAttributeValue(i);
      else if ("from".equals(name))
        _from = in.getAttributeValue(i);
      else if ("to".equals(name))
        _to = in.getAttributeValue(i);
    }
  }

  public String getId()
  {
    return _id;
  }

  public void setId(String id)
  {
    _id = id;
  }

  public String getTo()
  {
    return _to;
  }

  public void setTo(String to)
  {
    _to = to;
  }

  public String getFrom()
  {
    return _from;
  }

  public void setFrom(String from)
  {
    _from = from;
  }

  protected void print(WriteStream out, String from, String to)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _id + "," + _type + ",from=" + _from + ",to=" + _to + "]");
  }
}
