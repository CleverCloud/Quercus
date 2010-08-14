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

package javax.enterprise.deploy.spi.status;

import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.OperationUnsupportedException;
/**
 * Status of the progress.
 */
public interface ProgressObject {
  /**
   * Returns the status.
   */
  public DeploymentStatus getDeploymentStatus();
  
  /**
   * Returns the target ids.
   */
  public TargetModuleID []getResultTargetModuleIDs();
  
  /**
   * Returns the client configuration.
   */
  public ClientConfiguration getClientConfiguration(TargetModuleID id);
  
  /**
   * Returns true if cancel is supported.
   */
  public boolean isCancelSupported();
  
  /**
   * Cancels the operation.
   */
  public void cancel()
    throws OperationUnsupportedException;
  
  /**
   * Returns true if stop is supported.
   */
  public boolean isStopSupported();
  
  /**
   * Stops the operation.
   */
  public void stop()
    throws OperationUnsupportedException;
  
  /**
   * Adds a listener.
   */
  public void addProgressListener(ProgressListener listener);
  
  /**
   * Removes a listener.
   */
  public void removeProgressListener(ProgressListener listener);
}

