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

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Represents a text node.
 */
public class QText extends QCharacterData implements Text {
  /**
   * Creates a new text node.
   */
  public QText()
  {
    super();
  }

  /**
   * Creates a new text node with initial data.
   */
  public QText(String data)
  {
    super(data);
  }

  public String getNodeName() { return "#text"; }
  public short getNodeType() { return TEXT_NODE; }

  Node importNode(QDocument owner, boolean deep)
  {
    QText text = new QText(_data);
    text._owner = owner;
    text._filename = _filename;
    text._line = _line;
    return text;
  }

  public Text splitText(int offset)
    throws DOMException
  {
    QText text = new QText(_data.substring(offset));
    text._owner = _owner;

    _data = _data.substring(0, offset);

    text._parent = _parent;
    if (_next != null)
      _next._previous = text;
    else if (_parent != null)
      _parent._lastChild = text;
    text._previous = this;
    text._next = _next;
    _next = text;

    return text;
  }

  public Text joinText(Text node1, Text node2)
    throws DOMException
  {
    return new QText(node1.getData() + node2.getData());
  }

  // DOM level 3
  public boolean getIsWhitespaceInElementContent()
  {
    throw new UnsupportedOperationException();
  }

  public String getWholeText()
  {
    throw new UnsupportedOperationException();
  }

  public Text replaceWholeText(String content)
    throws DOMException
  {
    throw new UnsupportedOperationException();
  }

  private Object writeReplace()
  {
    return new SerializedXml(this);
  }

  public String toString()
  {
    return "Text[" + getData() + "]";
  }
}
