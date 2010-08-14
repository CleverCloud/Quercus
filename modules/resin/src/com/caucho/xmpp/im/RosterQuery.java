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

package com.caucho.xmpp.im;

import com.caucho.xmpp.im.RosterItem;
import java.io.Serializable;
import java.util.*;

/**
 * Roster query (jabber:iq:roster defined in rfc3921)
 *
 * <code><pre>
 * element query {
 *   item*
 * }
 *
 * element item {
 *   jid,
 *   ask?,
 *   name?,
 *   subscription?,
 *   group*
 * }
 * </pre></code>
 */
public class RosterQuery implements Serializable {
  private final RosterItem []_items;

  public RosterQuery()
  {
    _items = null;
  }

  public RosterQuery(RosterItem []items)
  {
    _items = items;
  }

  public RosterQuery(ArrayList<RosterItem> itemList)
  {
    RosterItem []items = null;
    
    if (itemList != null) {
      items = new RosterItem[itemList.size()];
      itemList.toArray(items);
    }
    
    _items = items;
  }

  public RosterItem []getItems()
  {
    return _items;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append("[");

    if (_items != null) {
      for (int i = 0; i < _items.length; i++) {
        if (i != 0)
          sb.append(",");
        sb.append(_items[i]);
      }
    }
    sb.append("]");

    return sb.toString();
  }
}
