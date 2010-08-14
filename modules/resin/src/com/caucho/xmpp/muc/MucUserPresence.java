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

import com.caucho.xmpp.muc.MucStatus;
import com.caucho.xmpp.muc.MucDestroy;
import com.caucho.xmpp.muc.MucDecline;
import com.caucho.xmpp.muc.MucContinue;
import java.util.*;

/**
 * Muc user presence
 *
 * <code><pre>
 * element x:{http://jabber.org/protocol/muc#user} {
 *   password
 * }
 * </pre></code>
 */
public class MucUserPresence implements java.io.Serializable {
  // actor jid
  private String _actor;
  private String _reason;
  private MucContinue _continue;

  // "admin", "member", "none", "outcast", "owner"
  private String _affiliation;
  // "moderator", "none", "participant", "visitor"
  private String _role;
  
  private String _jid;
  private String _nick;
  
  private MucDecline _decline;
  private MucDestroy _destroy;

  private MucStatus []_status;
  
  private String _password;
  
  public MucUserPresence()
  {
  }

  public String getAffiliation()
  {
    return _affiliation;
  }

  public void setAffiliation(String affiliation)
  {
    _affiliation = affiliation;
  }

  public String getRole()
  {
    return _role;
  }

  public void setRole(String role)
  {
    _role = role;
  }

  public void setStatus(int []status)
  {
    if (status == null) {
      _status = null;
      return;
    }

    _status = new MucStatus[status.length];
    
    for (int i = 0; i < status.length; i++) {
      _status[i] = new MucStatus(status[i]);
    }
  }

  public void setStatus(MucStatus []status)
  {
    _status = status;
  }

  public MucStatus []getStatus()
  {
    return _status;
  }
  
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");
    sb.append("affiliation=");
    sb.append(_affiliation);
    sb.append(",role=");
    sb.append(_role);

    if (_status != null) {
      sb.append(",status=[");
      
      for (int i = 0; i < _status.length; i++) {
        if (i != 0)
          sb.append(",");

        sb.append(_status[i].getCode());
      }
      
      sb.append("]");
    }

    sb.append("]");

    return sb.toString();
  }
}
