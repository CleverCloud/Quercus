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

package com.caucho.server.deploy;

/**
 * The abstract strategy implements the start, update, and stop commands which
 * are common to all strategies.
 *
 * <table>
 * <tr><th>input  <th>stopped  <th>active  <th>modified   <th>error
 * <tr><td>start  <td>startImpl<td>-       <td>restartImpl<td>restartImpl
 * <tr><td>update <td>startImpl<td>-       <td>restartImpl<td>restartImpl
 * <tr><td>stop   <td>-        <td>stopImpl<td>stopImpl   <td>stopImpl
 * </table>
 */
abstract public class AbstractDeployControllerStrategy
  implements DeployControllerStrategy {

  /**
   * Starts the instance.  Called from an admin start.
   *
   * @param controller the owning controller
   */
  public <I extends DeployInstance>
          void start(DeployController<I> controller)
  {
    if (controller.isStopped()) {
      // server/1d03
      controller.startImpl();
    }
    else if (controller.isModifiedNow()) {
      // server/1d0p
      controller.restartImpl();
    }
    else if (controller.isError()) {
      controller.restartImpl();
    }
    else { /* active */
    }
  }

  /**
   * Stops the instance from an admin command.
   *
   * @param controller the owning controller
   */
  public<I extends DeployInstance>
    void stop(DeployController<I> controller)
  {
    controller.stopImpl();
  }
}
