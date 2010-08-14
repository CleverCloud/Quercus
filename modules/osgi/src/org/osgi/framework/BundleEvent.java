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

package org.osgi.framework;

import java.util.EventListener;
import java.util.EventObject;

/**
 * A bundle event
 */
public class BundleEvent extends EventObject
{
  public static final int INSTALLED = 0x00000001;
  public static final int STARTED = 0x00000002;
  public static final int STOPPED = 0x00000004;
  public static final int UPDATED = 0x00000008;
  public static final int UNINSTALLED = 0x00000010;
  public static final int RESOLVED = 0x00000020;
  public static final int UNRESOLVED = 0x00000040;
  public static final int STARTING = 0x00000080;
  public static final int STOPPING = 0x00000100;
  public static final int LAZY_ACTIVATION = 0x00000200;
  
  private final int _type;

  public BundleEvent(int type, Bundle bundle)
  {
    super(bundle);

    _type = type;
  }

  public Bundle getBundle()
  {
    return (Bundle) getSource();
  }

  public int getType()
  {
    return _type;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getName()).append("[");
    
    switch (_type) {
    case INSTALLED:
      sb.append("installed");
      break;
      
    case STARTED:
      sb.append("started");
      break;
      
    case STOPPED:
      sb.append("stopped");
      break;
      
    case UPDATED:
      sb.append("updated");
      break;
      
    case UNINSTALLED:
      sb.append("uninstalled");
      break;
      
    case RESOLVED:
      sb.append("resolved");
      break;
      
    case UNRESOLVED:
      sb.append("unresolved");
      break;
      
    case STARTING:
      sb.append("starting");
      break;
      
    case STOPPING:
      sb.append("stopping");
      break;
      
    default:
      sb.append("type=").append(getType());
      break;
    }

    sb.append(",").append(getSource()).append("]");

    return sb.toString();
  }
}
