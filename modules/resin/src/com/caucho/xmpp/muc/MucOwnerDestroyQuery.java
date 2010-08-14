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
 * MucOwner query
 *
 * XEP-0045: http://www.xmpp.org/extensions/xep-0045.html
 *
 * <code><pre>
 * namespace = http://jabber.org/protocol/muc#owner
 *
 * element query {
 *   x{jabber:x:data}
 *   | destroy?
 * }
 *
 * element destroy {
 *   attribute jid?,
 *
 *   password?
 *   &amp; reason?
 * }
 * </pre></code>
 */
public class MucOwnerDestroyQuery extends MucOwnerQuery {
  private String _jid;
  private String _password;
  private String _reason;
  
  public MucOwnerDestroyQuery()
  {
  }
  
  public MucOwnerDestroyQuery(String jid)
  {
    _jid = jid;
  }
  
  public MucOwnerDestroyQuery(String jid, String password, String reason)
  {
    _jid = jid;
    _password = password;
    _reason = reason;
  }

  public String getJid()
  {
    return _jid;
  }

  public String getPassword()
  {
    return _password;
  }

  public String getReason()
  {
    return _reason;
  }
  
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName()).append("[");

    if (_jid != null)
      sb.append("jid=").append(_jid);

    if (_password != null)
      sb.append(",password=").append(_password);

    if (_reason != null)
      sb.append(",reason=").append(_reason);

    return sb.toString();
  }
}
