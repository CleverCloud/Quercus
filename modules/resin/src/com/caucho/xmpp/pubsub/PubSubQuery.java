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
 * element affiliation {
 *   attribute affiliation,
 *   attribute node
 * }
 *
 * element affiliations {
 *   affiliation*
 * }
 *
 * element configure {
 *   x{jabber:x:data}?
 * }
 *
 * element create {
 *   attribute node?
 * }
 *
 * element item {
 *   attribute id?,
 *
 *   other?
 * }
 *
 * element items {
 *   attribute max_items?,
 *   attribute node,
 *   attribute subid?,
 *
 *   item*
 * }
 *
 * element options {
 *   attribute jid,
 *   attribute node?,
 *   attribute subid?,
 *
 *   x{jabber:x:data}*
 * }
 *
 * element publish {
 *   attribute node,
 *
 *   item*
 * }
 *
 * element retract {
 *   attribute node,
 *   attribute notify?,
 *
 *   item+
 * }
 *
 * element subscribe {
 *   attribute jid,
 *   attribute node?
 * }
 *
 * element subscribe-options {
 *   required?
 * }
 *
 * element subscription {
 *   attribute jid,
 *   attribute node?,
 *   attribute subid?,
 *   attribute subscription?,
 *
 *   subscribe-options?
 * }
 *
 * element unsubscribe {
 *   attribute jid,
 *   attribute node?
 *   attribute subid?
 * }
 * </pre></code>
 */
abstract public class PubSubQuery implements Serializable {
}
