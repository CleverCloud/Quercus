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

import javax.enterprise.deploy.shared.ActionType;
import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.shared.StateType;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import java.io.Serializable;

/**
 * Represents the status of a deployed module.
 */
public class DeploymentStatusImpl implements DeploymentStatus, Serializable {
  public static final int RUNNING = 0;
  public static final int COMPLETED = 1;
  public static final int FAILED = 2;
  public static final int RELEASED = 3;

  private String _message;
  private int _state = RUNNING;

  /**
   * Returns the StateType value.
   */
  public StateType getState()
  {
    switch (_state) {
      case RUNNING:
        return StateType.RUNNING;
      case COMPLETED:
        return StateType.COMPLETED;
      case FAILED:
        return StateType.FAILED;
      case RELEASED:
        return StateType.RELEASED;
      default:
        return StateType.FAILED;
    }
  }

  public void setState(StateType stateType)
  {
    if (stateType == StateType.RUNNING)
      _state = RUNNING;
    else if (stateType == StateType.COMPLETED)
      _state = COMPLETED;
    else if (stateType == StateType.FAILED)
      _state = FAILED;
    else if (stateType == StateType.RELEASED)
      _state = RELEASED;
    else throw new AssertionError(stateType);
  }

  /**
   * Returns the CommandType value.
   */
  public CommandType getCommand()
  {
    return CommandType.DISTRIBUTE;
  }

  /**
   * Returns the ActionType value.
   */
  public ActionType getAction()
  {
    return ActionType.EXECUTE;
  }
  
  /**
   * Returns additional information.
   */
  public String getMessage()
  {
    return _message;
  }

  /**
   * Sets the message.
   */
  public void setMessage(String message)
  {
    _message = message;
  }
  
  /**
   * Returns true if the deployment is completed.
   */
  public boolean isCompleted()
  {
    return _state == COMPLETED;
  }
  
  /**
   * Returns true if the deployment is failed.
   */
  public boolean isFailed()
  {
    return _state == FAILED;
  }

  /**
   * Returns true if the deployment is running.
   */
  public boolean isRunning()
  {
    return _state == RUNNING;
  }

  public String toString()
  {
    return "DeploymentStatusImpl[" + getState() + "," + getMessage() + "]";
  }
}

