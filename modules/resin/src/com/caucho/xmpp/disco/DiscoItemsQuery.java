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

package com.caucho.xmpp.disco;

import com.caucho.xmpp.disco.DiscoItem;
import java.util.*;

/**
 * service discovery query
 *
 * http://www.xmpp.org/extensions/xep-0030.html
 *
 * <code><pre>
 * namespace="http://jabber.org/protocol/disco#items"
 *
 * element query {
 *   attribute node?,
 *   item*
 * }
 *
 * element item {
 *    attribute jid,
 *    attribute node?,
 *    attribute name?,
 *    attribute action { remove, update}?,
 * }
 * </pre></code>
 */
public class DiscoItemsQuery implements java.io.Serializable {
  private String _node;
  
  private DiscoItem []_items;
  
  public DiscoItemsQuery()
  {
  }

  public DiscoItemsQuery(String node)
  {
    _node = node;
  }

  public String getNode()
  {
    return _node;
  }
  
  public DiscoItem []getItems()
  {
    return _items;
  }
  
  public void setItems(DiscoItem []items)
  {
    _items = items;
  }
  
  public void setItemList(ArrayList<DiscoItem> itemList)
  {
    if (itemList != null && itemList.size() > 0) {
      _items = new DiscoItem[itemList.size()];
      itemList.toArray(_items);
    }
    else
      _items = null;
  }
  
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append("[");

    if (_node != null) {
      sb.append("node=");
      sb.append(_node);
      sb.append(",");
    }

    sb.append("items=[");
    
    if (_items != null) {
      for (int i = 0; i < _items.length; i++) {
        if (i != 0)
          sb.append(",");
        sb.append(_items[i]);
      }
    }
    
    sb.append("]]");
    
    return sb.toString();
  }
}
