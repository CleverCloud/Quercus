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

package com.caucho.xmpp.disco;

import java.util.*;

/**
 * service discovery identity
 *
 * http://jabber.org/protocol/disco#info
 *
 * <code><pre>
 * element query {
 *   attribute node?,
 *   identity*,
 *   feature*
 * }
 *
 * element identity {
 *    attribute category,
 *    attribute name?,
 *    attribute type
 * }
 *
 * element feature {
 *    attribute var
 * }
 * </pre></code>
 */
public class DiscoIdentity implements java.io.Serializable {
  private String _category;
  private String _type;
  private String _name;
  
  public DiscoIdentity()
  {
  }
  
  public DiscoIdentity(String category, String type)
  {
    _category = category;
    _type = type;
  }
  
  public DiscoIdentity(String category, String type, String name)
  {
    _category = category;
    _type = type;
    _name = name;
  }

  public String getCategory()
  {
    return _category;
  }

  public String getType()
  {
    return _type;
  }

  public String getName()
  {
    return _name;
  }
  
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());

    sb.append("[category=").append(_category);
    sb.append(",type=").append(_type);

    if (_name != null)
      sb.append(",name=").append(_name);

    sb.append("]");
    
    return sb.toString();
  }
}
