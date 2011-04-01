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

import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;

import java.io.IOException;

public class QEntityReference extends QNode implements EntityReference {
  String _name;

  public QEntityReference(String name)
  {
    _name = name;
  }

  protected QEntityReference(QDocument owner, String name)
  {
    super(owner);

    _name = name;
  }

  public String getNodeName() { return _name; }
  public String getTagName() { return _name; }
  public short getNodeType() { return ENTITY_REFERENCE_NODE; }

  private void lazyEvaluateChild()
  {
    if (_owner == null || _owner._dtd == null)
      return;

    QEntity entity = _owner._dtd.getEntity(_name);
    if (entity == null || entity._firstChild == null)
      return;

    _firstChild = entity._firstChild;
    _lastChild = entity._lastChild;
  }

  public Node getFirstChild()
  {
    if (_firstChild != null)
      return _firstChild;

    lazyEvaluateChild();

    return _firstChild;
  }

  public Node getLastChild()
  {
    if (_lastChild != null)
      return _lastChild;

    lazyEvaluateChild();

    return _lastChild;
  }

  public void print(XmlPrinter os) throws IOException
  {
    if (os.finishAttributes())
      os.print(">");
    
    os.print("&");
    os.print(getNodeName());
    os.print(";");
  }

  public String toString()
  {
    return "EntityRef[" + getNodeName() + "]";
  }
}
