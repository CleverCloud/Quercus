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

/**
 * Returns meta-data for a resource adapter.
 */
public interface ResourceAdapterMetaData {
  /**
   * Returns the version of the resource adapter.
   */
  public String getAdapterVersion();

  /**
   * Returns the vendor name for the adapter.
   */
  public String getAdapterVendorName();

  /**
   * Returns a tool name for the resource adapter.
   */
  public String getAdapterName();

  /**
   * Returns a short description of the resource adapter.
   */
  public String getAdapterShortDescription();

  /**
   * Returns a string representation of the version of the spec.
   */
  public String getSpecVersion();

  /**
   * Returns the interaction spec types.
   */
  public String []getInteractionSpecsSupported();

  /**
   * Returns true if the execute with the input and output records
   * are supported.
   */
  public boolean supportsExecuteWithInputAndOutputRecord();

  /**
   * Returns true if the execute with the input records
   * are supported.
   */
  public boolean supportsExecuteWithInputRecordOnly();

  /**
   * Returns true if local transactions are supported.
   */
  public boolean supportsLocalTransactionDemarcation();
}
