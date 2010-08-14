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

import com.caucho.xmpp.im.Text;
import java.io.Serializable;
import java.util.*;

/**
 * Presence - rfc3921
 *
 * <pre><code>
 * element presence {
 *   attribute from?
 *   &amp; attribute id?
 *   &amp; attribute to?
 *   &amp; attribute type?
 *
 *   &amp; show?
 *   &amp; status*
 *   &amp; priority?
 *   &amp; other*
 *   &amp; error?
 * }
 *
 * element priority {
 *   integer
 * }
 *
 * element show {
 *   "away" | "chat" | "dnd" | "xa"
 * }
 *
 * element status {
 *   attribute xml:lang?,
 *   string
 * }
 * </code></pre>
 */
public class ImPresence implements Serializable {
  private String _id;
  private String _to;
  private String _from;
  
  // "away", "chat", "dnd", "xa"
  private String _show;
  private Text _status;
  private int _priority;

  private Serializable []_extra;

  private ImPresence()
  {
  }

  public ImPresence(String status)
  {
    _status = new Text(status);
  }

  public ImPresence(String to,
                    String from,
                    String show,
                    Text status,
                    int priority,
                    ArrayList<Serializable> extraList)
  {
    _to = to;
    _from = from;
    
    _show = show;
    _status = status;
    _priority = priority;

    Serializable []extra = null;

    if (extraList != null) {
      extra = new Serializable[extraList.size()];
      extraList.toArray(extra);
    }

    _extra = extra;
  }

  public Text getStatus()
  {
    return _status;
  }

  public String getTo()
  {
    return _to;
  }

  public String getFrom()
  {
    return _from;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");

    if (_to != null)
      sb.append("to=").append(_to);

    if (_from != null)
      sb.append(",from=").append(_from);

    if (_id != null)
      sb.append("id=").append(_id);

    if (_show != null)
      sb.append(",show=").append(_show);

    if (_status != null)
      sb.append(",status=").append(_status.getValue());

    if (_priority != 0)
      sb.append(",priority=").append(_priority);

    if (_extra != null) {
      for (Serializable extra : _extra) {
        sb.append(",").append(extra);
      }
    }
    
    sb.append("]");
    
    return sb.toString();
  }
}
