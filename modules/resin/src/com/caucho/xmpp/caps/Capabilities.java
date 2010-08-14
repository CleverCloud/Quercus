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

package com.caucho.xmpp.caps;

import java.util.*;

/**
 * capabilities
 *
 * XEP-0115: http://www.xmpp.org/extensions/xep-0115.html
 *
 * <code><pre>
 * namespace = http://jabber.org/protocol/caps
 *
 * element c {
 *   attribute ext?,
 *   attribute hash,
 *   attribute node,
 *   attribute ver
 * }
 * </pre></code>
 */
public class Capabilities implements java.io.Serializable {
  private String _ext;
  private String _hash;
  private String _node;
  private String _ver;
  
  public Capabilities()
  {
  }
  
  public Capabilities(String hash, String node, String ver)
  {
    _hash = hash;
    _node = node;
    _ver = ver;
  }

  public String getExt()
  {
    return _ext;
  }
  
  public void setExt(String ext)
  {
    _ext = ext;
  }

  public String getHash()
  {
    return _hash;
  }
  
  public void setHash(String hash)
  {
    _hash = hash;
  }

  public String getNode()
  {
    return _node;
  }
  
  public void setNode(String node)
  {
    _node = node;
  }

  public String getVer()
  {
    return _ver;
  }
  
  public void setVer(String ver)
  {
    _ver = ver;
  }
  
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append("[hash=").append(_hash);

    if (_node != null)
      sb.append(",node=").append(_node);

    if (_ver != null)
      sb.append(",ver=").append(_ver);

    if (_ext != null)
      sb.append(",ext=").append(_ext);

    sb.append("]");
    
    return sb.toString();
  }
}
