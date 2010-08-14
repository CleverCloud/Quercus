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
 * IM bind - RFC 3921
 *
 * <pre>
 * element bind{urn:ieft:params:xml:ns:xmpp-bind} {
 *   &amp; resource?
 *   &amp; jid?
 * }
 * </pre>
 */
public class ImBindQuery implements Serializable {
  private String _resource;
  private String _jid;

  public ImBindQuery()
  {
  }

  public ImBindQuery(String resource, String jid)
  {
    _resource = resource;
    _jid = jid;
  }

  public String getResource()
  {
    return _resource;
  }

  public String getJid()
  {
    return _jid;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append("[resource=").append(_resource);
    sb.append(",jid=").append(_jid);
    sb.append("]");
    
    return sb.toString();
  }
}
