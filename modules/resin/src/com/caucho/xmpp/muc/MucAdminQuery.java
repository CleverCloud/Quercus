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

package com.caucho.xmpp.muc;

import java.util.*;

/**
 * Muc admin query
 *
 * XEP-0045: http://www.xmpp.org/extensions/xep-0045.html
 *
 * <code><pre>
 * namespace = http://jabber.org/protocol/muc#admin
 *
 * element query {
 *   item+
 * }
 *
 * element actor {
 *   attribute jid
 * }
 *
 * element item {
 *   attribute affiliation?,
 *   attribute jid?,
 *   attribute nick?,
 *   attribute role?,
 *
 *   actor?
 *   &amp; reason?
 * }
 *
 * element reason {
 *   string
 * }
 * </pre></code>
 */
public class MucAdminQuery implements java.io.Serializable {
  private MucUserItem []_items;
  
  public MucAdminQuery()
  {
  }
  
  public MucAdminQuery(MucUserItem []items)
  {
    _items = items;
  }
  
  public MucUserItem []getItems()
  {
    return _items;
  }
  
  public void setItems(MucUserItem []items)
  {
    _items = items;
  }
  
  public void setItemList(ArrayList<MucUserItem> itemsList)
  {
    if (itemsList != null && itemsList.size() > 0) {
      _items = new MucUserItem[itemsList.size()];
      itemsList.toArray(_items);
    }
    else
      _items = null;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName()).append("[");

    if (_items != null) {
      for (int i = 0; i < _items.length; i++) {
        if (i > 0)
          sb.append(",");

        sb.append("item=").append(_items[i]);
      }
    }

    sb.append("]");

    return sb.toString();
  }
}
