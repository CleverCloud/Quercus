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

package com.caucho.management.server;

import com.caucho.jmx.Description;
import com.caucho.jmx.Units;

public interface ArchiveDeployMXBean
  extends DeployMXBean
{
  @Description("The configured millisecond interval between checks for new archives")
  @Units("milliseconds")
  public long getDependencyCheckInterval();

  @Description("The configured directory where archive files are found")
  public String getArchiveDirectory();

  @Description("The configured extension used to recognize archive files")
  public String getExtension();

  @Description("The configured directory where archives should be expanded")
  public String getExpandDirectory();

  @Description("The configured prefix to use for the subdirectory created in the expand directory")
  public String getExpandPrefix();

  @Description("The configured suffix to use for the subdirectory created in the expand directory")
  public String getExpandSuffix();

  @Description("Returns the location for deploying an archive with the specified name")
  public String getArchivePath(@Description("The archive name, without a file extension") String name);

  @Description("Returns the location of an expanded archive, or null if no archive with the passed name is deployed")
  public String getExpandPath(String name);

  /**
   * Deploys the resource with the given name
   *
   * @param name the resource's name, e.g. "/my-web-app"
   */
  @Description("Deploy the resource associated with the archive")
  public void deploy(String name);

  /**
   * Starts the resource with the given name
   *
   * @param name the resource's name, e.g. "/my-web-app"
   */
  @Description("Start the resource associated with the archive")
  public void start(String name);

  /**
   * Stops the resource with the given name
   *
   * @param name the resource's name, e.g. "/my-web-app"
   */
  @Description("Stop the resource associated with the archive")
  public void stop(String name);

  /**
   * Undeploys the resource with the given name
   *
   * @param name the resource's name, e.g. "/my-web-app"
   */
  @Description("Stop the resource associated with the archive and delete the archive")
  public void undeploy(String name);

  @Description("Returns a list of the current set of archive names")
  public String[] getNames();

  @Description("Returns an exception for the named archive or null if there is no exception")
  public Throwable getConfigException(String name);
}
