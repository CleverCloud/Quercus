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
 * Muc query
 */
public class MucDecline implements java.io.Serializable {
  private String _to;
  private String _from;
  private String _reason;
  
  public MucDecline()
  {
  }
  
  public MucDecline(String to, String from, String reason)
  {
    _to = to;
    _from = from;
    _reason = reason;
  }

  public String getTo()
  {
    return _to;
  }

  public String getFrom()
  {
    return _from;
  }

  public String getReason()
  {
    return _reason;
  }
  
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName()).append("[to=").append(_to);

    if (_from != null)
      sb.append(",from=").append(_from);

    if (_reason != null)
      sb.append(",reason=").append(_reason);

    sb.append("]");

    return sb.toString();
  }
}
