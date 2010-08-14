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
import javax.xml.stream.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.xml.stream.*;

/**
 * Marshals from an xmpp request to and from a serialized class
 */
public class XmppStreamWriterImpl extends XMLStreamWriterImpl
  implements XmppStreamWriter
{
  private static final L10N L = new L10N(XmppStreamWriterImpl.class);
  
  private WriteStream _os;
  private XmppMarshalFactory _marshalFactory;
  
  XmppStreamWriterImpl(WriteStream os, XmppMarshalFactory factory)
  {
    super(os);

    _marshalFactory = factory;
    _os = os;
  }

  public void writeValue(Serializable value)
    throws IOException, XMLStreamException
  {
    if (value == null) {
    }
    else if (value instanceof String) {
      writeCharacters(""); // flush
      _os.print((String) value);
    }
    else {
      String name = value.getClass().getName();

      XmppMarshal marshal = _marshalFactory.getSerialize(name);

      if (marshal == null)
        throw new IllegalArgumentException(L.l("'{0}' is an unknown XMPP marshal class", name));

      marshal.toXml(this, value);
    }
  }
}
