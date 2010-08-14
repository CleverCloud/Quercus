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

package com.caucho.server.hmux;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import com.caucho.loader.EnvironmentLocal;
import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.server.http.AbstractHttpProtocol;

/**
 * Dispatches the HMUX protocol.
 *
 * @see com.caucho.network.listen.AbstractProtocol
 */
public class HmuxProtocol extends AbstractHttpProtocol {
  private static EnvironmentLocal<HmuxProtocol> _localManager
    = new EnvironmentLocal<HmuxProtocol>();

  private HashMap<Integer,WeakReference<HmuxExtension>> _extensionMap
    = new HashMap<Integer,WeakReference<HmuxExtension>>();

  public HmuxProtocol()
  {
    setProtocolName("server");

    _localManager.set(this);
  }

  public static HmuxProtocol getLocal()
  {
    synchronized (_localManager) {
      return _localManager.get();
    }
  }

  /**
   * Create a HmuxRequest object for the new thread.
   */
  @Override
  public ProtocolConnection createConnection(SocketLink conn)
  {
    return new HmuxRequest(getServer(), conn, this);
  }

  public HmuxExtension getExtension(Integer id)
  {
    WeakReference<HmuxExtension> ref = _extensionMap.get(id);

    if (ref != null)
      return ref.get();
    else
      return null;
  }

  public void putExtension(Integer id, HmuxExtension extension)
  {
    _extensionMap.put(id, new WeakReference<HmuxExtension>(extension));
  }
}
