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
 * @author Scott Ferguson
 */
package com.caucho.j2ee.deployclient;

import javax.enterprise.deploy.shared.StateType;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.OperationUnsupportedException;
import javax.enterprise.deploy.spi.status.ClientConfiguration;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressListener;
import javax.enterprise.deploy.spi.status.ProgressObject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Status of the progress.
 */
public class ProgressObjectImpl implements ProgressObject, java.io.Serializable {
  private static final Logger log = Logger.getLogger(ProgressObjectImpl.class.getName());

  private TargetModuleID []_targetModuleIDs;
  private final DeploymentStatusImpl _status = new DeploymentStatusImpl();

  ProgressObjectImpl()
  {
  }

  public ProgressObjectImpl(TargetModuleID []targetModuleIDs)
  {
    _targetModuleIDs = targetModuleIDs;
  }

  /**
   * Returns the status.
   */
  public DeploymentStatus getDeploymentStatus()
  {
    if (_status != null)
      return _status;
    else
      return new DeploymentStatusImpl();
  }

  /**
   * Returns the target ids.
   */
  public TargetModuleID []getResultTargetModuleIDs()
  {
    return _targetModuleIDs;
  }

  /**
   * Returns the client configuration.
   */
  public ClientConfiguration getClientConfiguration(TargetModuleID id)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns true if cancel is supported.
   */
  public boolean isCancelSupported()
  {
    return false;
  }

  /**
   * Cancels the operation.
   */
  public void cancel()
    throws OperationUnsupportedException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns true if stop is supported.
   */
  public boolean isStopSupported()
  {
    return false;
  }

  /**
   * Stops the operation.
   */
  public void stop()
    throws OperationUnsupportedException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Adds a listener.
   */
  public void addProgressListener(ProgressListener listener)
  {
  }

  /**
   * Removes a listener.
   */
  public void removeProgressListener(ProgressListener listener)
  {
  }

  /**
   * Change to the failed state.
   */
  public void failed(String message)
  {
    if (log.isLoggable(Level.FINEST))
      log.log(Level.FINEST, "failed " +  message);

    _status.setState(StateType.FAILED);
    _status.setMessage(message);
  }

  /**
   * Change to the running state.
   */
  public void completed(String message)
  {
    _status.setState(StateType.COMPLETED);
    _status.setMessage(message);
  }

  public String toString()
  {
    return "ProgressObjectImpl[" + _status + "]";
  }
}

