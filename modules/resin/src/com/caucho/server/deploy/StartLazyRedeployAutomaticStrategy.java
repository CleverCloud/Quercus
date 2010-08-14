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
 * The start-mode="lazy", redeploy-model="automatic" controller strategy.
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
public class StartLazyRedeployAutomaticStrategy
  extends AbstractDeployControllerStrategy {
  private final static StartLazyRedeployAutomaticStrategy STRATEGY =
          new StartLazyRedeployAutomaticStrategy();


  private StartLazyRedeployAutomaticStrategy()
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
    controller.stopLazyImpl();
  }


  /**
   * Checks for updates from an admin command.  The target state will be the
   * initial state, i.e. update will not start a lazy instance.
   *
   * @param controller the owning controller
   */
  public<I extends DeployInstance>
    void update(DeployController<I> controller)
  {
    if (controller.isStoppedLazy()) {
      // server/1d08
    }
    else if (controller.isStopped()) {
      // server/1d05
      controller.stopLazyImpl();
    }
    else if (controller.isModifiedNow()) {
      // 1d0n, 1d0o
      controller.stopLazyImpl();
    }
    else if (controller.isError()) {
      controller.stopLazyImpl();
    }
    else if (controller.isActiveIdle()) {
      controller.stopLazyImpl();
    }
    else { /* active */
      // server/1d0h
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
      // server/1d06
      return controller.startImpl();
    }
    else if (controller.isStopped()) {
      // server/1d00
      return controller.getDeployInstance();
    }
    else if (controller.isModified()) {
      // server/1d0i
      return controller.restartImpl();
    }
    else { /* active */
      // server/1d0c
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
      // server/1d07
      return controller.startImpl();
    }
    else if (controller.isStopped()) {
      // server/1d01
      return controller.getDeployInstance();
    }
    else if (controller.isError() && controller.isModified()) {
      // server/1d0x
      return controller.restartImpl();
    }
    else if (controller.isModified()) {
      // server/1d0j
      return controller.getDeployInstance();
    }
    else { /* active */
      // server/1d0d
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
    if (controller.isStoppedLazy()) {
      // server/1d08
    }
    else if (controller.isStopped()) {
      // server/1d02
    }
    else if (controller.isError()) {
      // server/1d0w
    }
    else if (controller.isModified()) {
      // server/1d0k
      controller.stopLazyImpl();
    }
    else if (controller.isActiveIdle()) {
      controller.stopLazyImpl();
    }
    else { /* active */
    }
  }
}
