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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */

package com.caucho.server.admin;

import com.caucho.util.L10N;

import javax.enterprise.deploy.shared.StateType;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.OperationUnsupportedException;
import javax.enterprise.deploy.spi.status.ClientConfiguration;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressEvent;
import javax.enterprise.deploy.spi.status.ProgressListener;
import javax.enterprise.deploy.spi.status.ProgressObject;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProgressObjectImpl
  implements ProgressObject
{
  private static final Logger log = Logger.getLogger(ProgressObjectImpl.class.getName());
  private static final L10N L = new L10N(ProgressObjectImpl.class);

  private final TargetModuleID[] _targetModuleIDs;
  private final DeploymentStatusImpl _status = new DeploymentStatusImpl();
  private final List<ProgressListener> _listeners = new LinkedList<ProgressListener>();

  ProgressObjectImpl()
  {
    _targetModuleIDs = null;
  }

  public ProgressObjectImpl(TargetModuleID []targetModuleIDs)
  {
    _targetModuleIDs = targetModuleIDs;
  }

  public DeploymentStatus getDeploymentStatus()
  {
    if (_status != null)
      return _status;
    else
      return new DeploymentStatusImpl();
  }

  public TargetModuleID []getResultTargetModuleIDs()
  {
    return _targetModuleIDs;
  }

  public ClientConfiguration getClientConfiguration(TargetModuleID id)
  {
    throw new UnsupportedOperationException();
  }

  public boolean isCancelSupported()
  {
    return false;
  }

  public void cancel()
    throws OperationUnsupportedException
  {
    throw new OperationUnsupportedException(L.l("'cancel' is not supported"));
  }

  public boolean isStopSupported()
  {
    return false;
  }

  public void stop()
    throws OperationUnsupportedException
  {
    throw new OperationUnsupportedException(L.l("'stop' is not supported"));
  }

  public void completed(String message)
  {
    boolean isChanged = _status.getState() != StateType.COMPLETED;

    _status.setState(StateType.COMPLETED);
    _status.setMessage(message);

    if (isChanged)
      fireProgressEvent();
  }

  public void failed(String message)
  {
    if (log.isLoggable(Level.FINEST))
      log.log(Level.FINEST, "failed " +  message);

    boolean isChanged = _status.getState() != StateType.FAILED;

    _status.setState(StateType.FAILED);
    _status.setMessage(message);

    if (isChanged)
      fireProgressEvent();
  }

  public void addProgressListener(ProgressListener listener)
  {
    synchronized (_listeners) {
      _listeners.add(listener);
    }
  }

  public void removeProgressListener(ProgressListener listener)
  {
    synchronized (_listeners) {
      Iterator<ProgressListener> iter = _listeners.iterator();

      while (iter.hasNext()) {
        if (iter.next() == listener)
          iter.remove();
      }
    }
  }

  private void fireProgressEvent()
  {
    if (_targetModuleIDs == null || _targetModuleIDs.length == 0)
      return;

    ProgressListener[] listeners;

    synchronized (_listeners) {
      if (_listeners.isEmpty())
        return;

      listeners = _listeners.toArray(new ProgressListener[_listeners.size()]);
    }

    ProgressEvent[] events = new ProgressEvent[_targetModuleIDs.length];

    for (int i = 0; i < _targetModuleIDs.length; i++)
      events[i] = new ProgressEvent(this, _targetModuleIDs[i], _status);

    for (ProgressListener listener : listeners) {
      for (ProgressEvent event : events)
        listener.handleProgressEvent(event);
    }
  }

  public String toString()
  {
    return "ProgressObjectImpl[" + _status + "]";
  }
}
