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

package com.caucho.xmpp.pubsub;

import java.io.Serializable;
import java.util.*;

/**
 * pubsub query
 *
 * XEP-0060: http://www.xmpp.org/extensions/xep-0060.html
 *
 * <code><pre>
 * namespace = http://jabber.org/protocol/pubsub
 *
 * element pubsub {
 *   (create, configure?)
 *   | (subscribe?, options?)
 *   | affiliations
 *   | items
 *   | publish
 *   | retract
 *   | subscription
 *   | subscriptions
 *   | unsubscribe
 * }
 *
 * element subscribe {
 *   attribute jid,
 *   attribute node?
 * }
 *
 * element options {
 *   attribute jid,
 *   attribute node?,
 *   attribute subid?,
 *
 *   x{jabber:x:data}*
 * }
 * </pre></code>
 */
public class PubSubSubscribeQuery extends PubSubQuery {
  private String _jid;
  private String _node;

  private PubSubOptions _options;

  public PubSubSubscribeQuery()
  {
  }

  public PubSubSubscribeQuery(String jid)
  {
    _jid = jid;
  }

  public PubSubSubscribeQuery(String jid, String node)
  {
    _jid = jid;
    _node = node;
  }

  public String getJid()
  {
    return _jid;
  }

  public String getNode()
  {
    return _node;
  }
  
  public PubSubOptions getOptions()
  {
    return _options;
  }
  
  public void setOptions(PubSubOptions options)
  {
    _options = options;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName()).append("[");

    sb.append("jid=").append(_jid);

    if (_node != null)
      sb.append(",node=").append(_node);

    if (_options != null)
      sb.append(",options=").append(_options);

    sb.append("]");

    return sb.toString();
  }
}
