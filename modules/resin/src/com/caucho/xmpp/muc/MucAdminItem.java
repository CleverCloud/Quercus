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
public class MucAdminItem implements java.io.Serializable {
  private String _actor;
  private String _reason;

  // "admin", "member", "none", "outcast", "owner"
  private String _affiliation;
  private String _jid;
  private String _nick;
  // "moderator", "none", "participant", "visitor"
  private String _role;
  
  public MucAdminItem()
  {
  }

  public MucAdminItem(String jid)
  {
    _jid = jid;
  }

  public MucAdminItem(String jid, String nick)
  {
    _jid = jid;
    _nick = nick;
  }

  public String getActor()
  {
    return _actor;
  }

  public void setActor(String actor)
  {
    _actor = actor;
  }

  public String getAffiliation()
  {
    return _affiliation;
  }

  public void setAffiliation(String affiliation)
  {
    _affiliation = affiliation;
  }

  public String getJid()
  {
    return _jid;
  }

  public void setJid(String jid)
  {
    _jid = jid;
  }

  public String getNick()
  {
    return _nick;
  }

  public void setNick(String nick)
  {
    _nick = nick;
  }

  public String getRole()
  {
    return _role;
  }

  public void setRole(String role)
  {
    _role = role;
  }

  public String getReason()
  {
    return _reason;
  }

  public void setReason(String reason)
  {
    _reason = reason;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName()).append("[");

    if (_jid != null)
      sb.append("jid=").append(_jid);

    if (_nick != null)
      sb.append(",nick=").append(_nick);

    if (_actor != null)
      sb.append(",actor=").append(_actor);

    if (_affiliation != null)
      sb.append(",affiliation=").append(_affiliation);

    if (_role != null)
      sb.append(",role=").append(_role);

    if (_reason != null)
      sb.append(",reason=").append(_reason);

    sb.append("]");

    return sb.toString();
  }
}
