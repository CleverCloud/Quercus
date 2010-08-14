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
 * XEP-0045
 * http://www.xmpp.org/extensions/xep-0045.html
 *
 * Muc query
 *
 * <code><pre>
 * namespace = http://jabber.org/protocol/muc
 *
 * element x {
 *   history?,
 *   password?
 * }
 *
 * element history {
 *   attribute maxchars?,
 *   attribute maxstanzas?
 *   attribute seconds?
 *   attribute since?
 * }
 * </pre></code>
 */
public class MucHistory implements java.io.Serializable {
  private int _maxChars;
  private int _maxStanzas;
  private int _seconds;
  private Date _since;
  
  public MucHistory()
  {
  }

  public MucHistory(int maxChars,
                    int maxStanzas,
                    int seconds,
                    Date since)
  {
    _maxChars = maxChars;
    _maxStanzas = maxStanzas;
    _seconds = seconds;
    _since = since;
  }

  public int getMaxChars()
  {
    return _maxChars;
  }

  public void setMaxChars(int maxChars)
  {
    _maxChars = maxChars;
  }

  public int getMaxStanzas()
  {
    return _maxStanzas;
  }

  public void setMaxStanzas(int maxStanzas)
  {
    _maxStanzas = maxStanzas;
  }

  public int getSeconds()
  {
    return _seconds;
  }

  public void setSeconds(int seconds)
  {
    _seconds = seconds;
  }

  public Date getSince()
  {
    return _since;
  }

  public void setSince(Date since)
  {
    _since = since;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName()).append("[");
    
    if (_maxChars > 0)
      sb.append("max-chars=").append(_maxChars);
    
    if (_maxStanzas > 0)
      sb.append(",max-stanzas=").append(_maxStanzas);
    
    if (_seconds > 0)
      sb.append(",seconds=").append(_seconds);
    
    if (_since != null)
      sb.append(",since=").append(_since);

    sb.append("]");

    return sb.toString();
  }
}
