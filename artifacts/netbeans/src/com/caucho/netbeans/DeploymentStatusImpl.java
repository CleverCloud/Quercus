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
 * @author Sam
 */


package com.caucho.netbeans;

import javax.enterprise.deploy.shared.ActionType;
import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.shared.StateType;
import javax.enterprise.deploy.spi.status.DeploymentStatus;

public class DeploymentStatusImpl
  implements DeploymentStatus
{
  private final ActionType _action;
  private final CommandType _command;
  private final String _message;
  private final StateType _state;

  public DeploymentStatusImpl(CommandType command, String message, StateType state)
  {
    _action = ActionType.EXECUTE;
    _command = command;
    _message = message;
    _state = state;
  }

  public StateType getState()
  {
    return _state;
  }

  public CommandType getCommand()
  {
    return _command;
  }

  public ActionType getAction()
  {
    return _action;
  }

  public String getMessage()
  {
    return _message;
  }

  public boolean isCompleted()
  {
    return StateType.COMPLETED.equals(_state);
  }

  public boolean isFailed()
  {
    return StateType.FAILED.equals(_state);
  }

  public boolean isRunning()
  {
    return StateType.RUNNING.equals(_state);
  }
}
