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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.xml;

import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

import java.io.IOException;

public class QProcessingInstruction extends QNode
  implements ProcessingInstruction {
  String _name;
  String _data;

  public QProcessingInstruction(String name)
  {
    _name = name.intern();
  }

  public QProcessingInstruction(String name, String data)
  {
    _name = name.intern();
    _data = data;
  }

  public String getNodeName() { return _name; }
  public String getNodeValue() { return _data; }
  public short getNodeType() { return PROCESSING_INSTRUCTION_NODE; }

  public String getTarget() { return getNodeName(); }

  public String getData() { return _data; }
  public void setData(String arg) { _data = arg; }

  Node importNode(QDocument owner, boolean deep) 
  {
    QProcessingInstruction pi = new QProcessingInstruction(_name, _data);

    pi._owner = owner;

    return pi;
  }

  public String getNamespaceURI()
  {
    return null;
  }

  public void print(XmlPrinter os) throws IOException
  {
    os.processingInstruction(getNodeName(), getData());
  }

  private Object writeReplace()
  {
    return new SerializedXml(this);
  }

  public String toString()
  {
    return "PI[" + getNodeName() + " " + getData() + "]";
  }
}
