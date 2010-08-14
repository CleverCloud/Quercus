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

import com.caucho.util.CharBuffer;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

import java.io.IOException;

public class QDocumentFragment extends QNode implements DocumentFragment {
  protected Document _masterDoc;

  QDocumentFragment()
  {
  }

  protected QDocumentFragment(QDocument owner)
  {
    super(owner);
  }

  public short getNodeType() { return DOCUMENT_FRAGMENT_NODE; }

  public Document getMasterDoc()
  {
    return _masterDoc;
  }

  public String getNodeName() { return "#document"; }

  /**
   * Clones the fragment into the new document.
   *
   * @param doc the new document
   * @param deep if true, return a recursive copy.
   *
   * @return the imported node.
   */
  Node importNode(QDocument owner, boolean deep)
  {
    QDocumentFragment frag = new QDocumentFragment();
    frag._owner = owner;

    if (! deep)
      return frag;

    for (Node node = getFirstChild();
         node != null;
         node = node.getNextSibling()) {
      frag.appendChild(node.cloneNode(true));
    }

    return frag;
  }

  public String getTextValue()
  {
    CharBuffer cb = new CharBuffer();

    for (QAbstractNode node = _firstChild; node != null; node = node._next) {
      cb.append(node.getTextValue());
    }

    return cb.toString();
  }

  void print(XmlPrinter out) throws IOException
  {
    out.print("<#fragment>");
    for (QAbstractNode node = _firstChild; node != null; node = node._next) {
      node.print(out);
    }
    out.print("</#fragment>");
  }

  private Object writeReplace()
  {
    return new SerializedXml(this);
  }

  public String toString()
  {
    if (_firstChild == null)
      return "DocumentFragment[]";
    else if (_firstChild == _lastChild)
      return "DocumentFragment[" + _firstChild.getNodeName() + "]";
    else
      return ("DocumentFragment[" + _firstChild.getNodeName() +
              " " + _lastChild.getNodeName() + "]");
  }
}
