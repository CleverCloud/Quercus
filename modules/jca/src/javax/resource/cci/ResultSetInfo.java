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

package javax.resource.cci;

import javax.resource.ResourceException;

/**
 * Returns meta-data information about the result set.
 */
public interface ResultSetInfo {
  /**
   * Returns true if row updates are detected.
   */
  public boolean updatesAreDetected(int type)
    throws ResourceException;

  /**
   * Returns true if inserts in a row are detected.
   */
  public boolean insertsAreDetected(int type)
    throws ResourceException;

  /**
   * Returns true if deletes in a row are detected.
   */
  public boolean deletesAreDetected(int type)
    throws ResourceException;

  /**
   * Returns true if the resource adapter supports the ResultSet.
   */
  public boolean supportsResultSetType(int type)
    throws ResourceException;

  /**
   * Returns true if the resource adapter supports the concurrency.
   */
  public boolean supportsResultTypeConcurrency(int type, int concurrency)
    throws ResourceException;

  /**
   * Returns true if updates made by others are visible.
   */
  public boolean othersUpdatesAreVisible(int type)
    throws ResourceException;

  /**
   * Returns true if deletes made by others are visible.
   */
  public boolean othersDeletesAreVisible(int type)
    throws ResourceException;

  /**
   * Returns true if inserts made by others are visible.
   */
  public boolean othersInsertsAreVisible(int type)
    throws ResourceException;

  /**
   * Returns true if updates made by self are visible.
   */
  public boolean ownUpdatesAreVisible(int type)
    throws ResourceException;

  /**
   * Returns true if deletes made by self are visible.
   */
  public boolean ownDeletesAreVisible(int type)
    throws ResourceException;

  /**
   * Returns true if inserts made by self are visible.
   */
  public boolean ownInsertsAreVisible(int type)
    throws ResourceException;
}
