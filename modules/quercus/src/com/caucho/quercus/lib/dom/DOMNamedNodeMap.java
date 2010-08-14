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
 * @author Sam
 */

package com.caucho.quercus.lib.dom;

import org.w3c.dom.NamedNodeMap;

public class DOMNamedNodeMap
  extends DOMWrapper<NamedNodeMap>
{
  DOMNamedNodeMap(DOMImplementation impl, NamedNodeMap delegate)
  {
    super(impl, delegate);
  }

  public int getLength()
  {
    return _delegate.getLength();
  }

  public DOMNode getNamedItem(String name)
  {
    return wrap(_delegate.getNamedItem(name));
  }

  public DOMNode getNamedItemNS(String namespaceURI, String localName)
    throws DOMException
  {
    try {
      return wrap(_delegate.getNamedItemNS(namespaceURI, localName));
    }
    catch (org.w3c.dom.DOMException ex) {
      throw wrap(ex);
    }
  }

  public DOMNode item(int index)
  {
    return wrap(_delegate.item(index));
  }

  public DOMNode removeNamedItem(String name)
    throws DOMException
  {
    try {
      return wrap(_delegate.removeNamedItem(name));
    }
    catch (org.w3c.dom.DOMException ex) {
      throw wrap(ex);
    }
  }

  public DOMNode removeNamedItemNS(String namespaceURI, String localName)
    throws DOMException
  {
    try {
      return wrap(_delegate.removeNamedItemNS(namespaceURI, localName));
    }
    catch (org.w3c.dom.DOMException ex) {
      throw wrap(ex);
    }
  }

  public DOMNode setNamedItem(DOMNode arg)
    throws DOMException
  {
    try {
      return wrap(_delegate.setNamedItem(arg.getDelegate()));
    }
    catch (org.w3c.dom.DOMException ex) {
      throw wrap(ex);
    }
  }

  public DOMNode setNamedItemNS(DOMNode arg)
    throws DOMException
  {
    try {
      return wrap(_delegate.setNamedItemNS(arg.getDelegate()));
    }
    catch (org.w3c.dom.DOMException ex) {
      throw wrap(ex);
    }
  }
}
