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

import com.caucho.xmpp.muc.MucUserItem;
import com.caucho.xmpp.muc.MucStatus;
import com.caucho.xmpp.muc.MucInvite;
import com.caucho.xmpp.muc.MucDestroy;
import com.caucho.xmpp.muc.MucDecline;
import java.util.*;

/**
 * Muc user query
 *
 * XEP-0045: http://www.xmpp.org/extensions/xep-0045.html
 *
 * http://jabber.org/protocol/muc#user
 *
 * <code><pre>
 * element x {
 *   decline?
 *   &amp; destroy?
 *   &amp; invite*
 *   &amp; item?
 *   &amp; password?
 *   &amp; status*
 * }
 *
 * element decline {
 *   @from?
 *   &amp; @to?
 *   &amp; reason?
 * }
 *
 * element invite {
 *   @from?
 *   &amp; @to?
 *   &amp; reason?
 * }
 *
 * element destroy {
 *   @jid?
 *   &amp; reason?
 * }
 *
 * element item {
 *   @affiliation?
 *   &amp; @jid?
 *   &amp; @nick?
 *   &amp; @role?
 *   &amp; actor?
 *   &amp; reason?
 *   &amp; continue?
 * }
 *
 * element actor {
 *   @jid
 * }
 *
 * element continue {
 *   @thread?
 * }
 *
 * element status {
 *   @code
 * }
 * </pre></code>
 */
public class MucUserQuery implements java.io.Serializable {
  private MucDecline _decline;
  private MucDestroy _destroy;
  private MucInvite []_invite;
  private MucUserItem _item;
  private String _password;
  private MucStatus []_status;
  
  public MucUserQuery()
  {
  }
  
  public MucUserQuery(MucInvite []invite)
  {
    _invite = invite;
  }
  
  public void setDecline(MucDecline decline)
  {
    _decline = decline;
  }
  
  public MucDecline getDecline()
  {
    return _decline;
  }
  
  public void setDestroy(MucDestroy destroy)
  {
    _destroy = destroy;
  }
  
  public MucDestroy getDestroy()
  {
    return _destroy;
  }
  
  public MucInvite []getInvite()
  {
    return _invite;
  }
  
  public void setInvite(MucInvite []invite)
  {
    _invite = invite;
  }
  
  public void setInviteList(ArrayList<MucInvite> inviteList)
  {
    if (inviteList != null && inviteList.size() > 0) {
      _invite = new MucInvite[inviteList.size()];
      inviteList.toArray(_invite);
    }
    else
      _invite = null;
  }
  
  public void setItem(MucUserItem item)
  {
    _item = item;
  }
  
  public MucUserItem getItem()
  {
    return _item;
  }
  
  public MucStatus []getStatus()
  {
    return _status;
  }
  
  public void setStatus(MucStatus []status)
  {
    _status = status;
  }
  
  public void setStatusList(ArrayList<MucStatus> statusList)
  {
    if (statusList != null && statusList.size() > 0) {
      _status = new MucStatus[statusList.size()];
      statusList.toArray(_status);
    }
    else
      _status = null;
  }
  
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName()).append("[");

    if (_item != null)
      sb.append("item=").append(_item).append(",");

    if (_invite != null) {
      sb.append("invite=[");
      
      for (int i = 0; i < _invite.length; i++) {
        if (i > 0)
          sb.append(",");

        sb.append(_invite[i]);
      }

      sb.append("]");
    }

    if (_decline != null)
      sb.append("decline=").append(_decline);

    if (_destroy != null)
      sb.append("destroy=").append(_destroy);

    if (_status != null) {
      sb.append("status=[");
      
      for (int i = 0; i < _status.length; i++) {
        if (i > 0)
          sb.append(",");

        sb.append(_status[i]);
      }

      sb.append("]");
    }

    sb.append("]");

    return sb.toString();
  }
}
