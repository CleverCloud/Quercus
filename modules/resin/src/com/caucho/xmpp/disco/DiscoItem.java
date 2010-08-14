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

import java.util.*;

/**
 * service discovery items
 *
 * http://www.xmpp.org/extensions/xep-0030.html
 *
 * xmlns="http://jabber.org/protocol/disco#items"
 *
 * <code><pre>
 * element query {
 *   attribute node?,
 *   item*
 * }
 *
 * element item {
 *    attribute jid,
 *    attribute name?,
 *    attribute node?
 * }
 * </pre></code>
 */
public class DiscoItem implements java.io.Serializable {
  private String _jid;
  private String _node;
  private String _name;
  
  private DiscoItem()
  {
  }
  
  public DiscoItem(String jid)
  {
    _jid = jid;
  }
  
  public DiscoItem(String jid,
                   String name,
                   String node)
  {
    _jid = jid;
    _name = name;
    _node = node;
  }

  public String getName()
  {
    return _name;
  }

  public String getNode()
  {
    return _node;
  }

  public String getJid()
  {
    return _jid;
  }
  
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName()).append("[");

    sb.append("jid=").append(_jid);

    if (_name != null)
      sb.append(",name=").append(_name);
    
    if (_node != null)
      sb.append(",node=").append(_node);

    sb.append("]");

    return sb.toString();
  }
}
