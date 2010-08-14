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
 * The start-mode="automatic", redeploy-model="automatic" controller strategy.
 *
 * <table>
 * <tr><th>input  <th>stopped  <th>active  <th>modified   <th>error
 * <tr><td>request<td>startImpl<td>-       <td>restartImpl<td>restartImpl
 * <tr><td>include<td>startImpl<td>-       <td>-          <td>-
 * <tr><td>start  <td>startImpl<td>-       <td>restartImpl<td>restartImpl
 * <tr><td>restart<td>startImpl<td>-       <td>restartImpl<td>restartImpl
 * <tr><td>stop   <td>-        <td>stopImpl<td>stopImpl   <td>stopImpl
 * <tr><td>alarm  <td>-        <td>-       <td>stopImpl   <td>stopImpl
 * </table>
 */
public class StartAutoRedeployAutoStrategy
  extends AbstractDeployControllerStrategy {
  private final static StartAutoRedeployAutoStrategy STRATEGY =
          new StartAutoRedeployAutoStrategy();

  private StartAutoRedeployAutoStrategy()
  {
  }

  /**
   * Returns the start="lazy" redeploy="automatic" strategy
   *
   * @return the singleton strategy
   */
  public static DeployControllerStrategy create()
  {
    return STRATEGY;
  }

  /**
   * Called at initialization time for automatic start.
   *
   * @param controller the owning controller
   */
  public<I extends DeployInstance>
    void startOnInit(DeployController<I> controller)
  {
    controller.startImpl();
  }

  /**
   * Checks for updates from an admin command.  The target state is
   * the started state.
   *
   * @param controller the owning controller
   */
  public<I extends DeployInstance>
    void update(DeployController<I> controller)
  {
    if (controller.isStoppedLazy()) {
      // server/1d18
    }
    else if (controller.isStopped()) {
      // server/1d15
      controller.startImpl();
    }
    else if (controller.isModifiedNow()) {
      // 1d1n, 1d1o
      controller.restartImpl();
    }
    else if (controller.isError()) {
      controller.restartImpl();
    }
    else if (controller.isActiveIdle()) {
      controller.restartImpl();
    }
    else { /* active */
    }
  }


  /**
   * Returns the current instance, redeploying if necessary.
   *
   * @param controller the owning controller
   * @return the current deploy instance
   */
  public <I extends DeployInstance>
          I request(DeployController<I> controller)
  {
    if (controller.isStoppedLazy()) {
      // server/1d16
      return controller.startImpl();
    }
    else if (controller.isStopped()) {
      // server/1d10
      return controller.getDeployInstance();
    }
    else if (controller.isModified()) {
      // server/1d1i
      return controller.restartImpl();
    }
    else { /* active */
      // server/1d1c
      return controller.getDeployInstance();
    }
  }

  /**
   * Returns the current instance, starting if necessary.
   *
   * @param controller the owning controller
   * @return the current deploy instance
   */
  public <I extends DeployInstance>
          I subrequest(DeployController<I> controller)
  {
    if (controller.isStoppedLazy()) {
      // server/1d17
      return controller.startImpl();
    }
    else if (controller.isStopped()) {
      // server/1d11
      return controller.getDeployInstance();
    }
    else if (controller.isModified()) {
      // server/1d1j
      return controller.getDeployInstance();
    }
    else { /* active */
      // server/1d1d
      return controller.getDeployInstance();
    }
  }

  /**
   * Redeployment on a timeout alarm.
   *
   * @param controller the owning controller
   */
  public <I extends DeployInstance>
          void alarm(DeployController<I> controller)
  {
    if (controller.isStopped()) {
      // server/1d12
    }
    else if (controller.isStoppedLazy()) {
      // server/1d18
    }
    else if (controller.isModified()) {
      // server/1d1k
      controller.logModified(controller.getLog());
      controller.restartImpl();
    }
    else if (controller.isActiveIdle()) {
    }
    else { /* active */
    }
  }
}
