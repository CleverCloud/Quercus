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

package com.caucho.jca.cfg;

import com.caucho.config.types.DescriptionGroupConfig;
import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * Configuration for a connector.
 */
public class ConnectorConfig extends DescriptionGroupConfig {
  private static final L10N L = new L10N(ConnectorConfig.class);
  private static final Logger log
    = Logger.getLogger(ConnectorConfig.class.getName());

  private String _id;

  private String _displayName;
  
  private String _vendorName;
  private String _specVersion;
  private String _eisType;
  private String _resourceAdapterVersion;

  private ResourceAdapterConfig _resourceAdapter;
  
  public ConnectorConfig()
  {
    _resourceAdapter = new ResourceAdapterConfig();
  }

  /**
   * Sets the id/name.
   */
  public void setId(String id)
  {
    _id = id;
  }

  /**
   * Gets the id/name.
   */
  public String getId()
  {
    return _id;
  }

  /**
   * Sets the version of JCA supported by the ResourceAdapter.
   */
  public void setVersion(String version)
  {
  }

  /**
   * Sets the schema.
   */
  public void setSchemaLocation(String schema)
  {
  }

  /**
   * Sets the vendor-name of the resource.
   */
  public void setVendorName(String vendorName)
  {
    _vendorName = vendorName;
  }

  /**
   * Sets the license.
   */
  public License createLicense()
  {
    return new License();
  }

  /**
   * Sets the eis type of the resource.
   */
  public void setEISType(String eisType)
  {
    _eisType = eisType;
  }

  /**
   * Sets the version of the resource adapter.
   */
  public void setResourceadapterVersion(String version)
  {
    _resourceAdapterVersion = version;
  }

  /**
   * Sets the spec version.
   */
  public void setSpecVersion(String version)
  {
  }

  /**
   * Sets the resource adapter itself.
   */
  public ResourceAdapterConfig createResourceadapter()
  {
    return _resourceAdapter;
  }

  /**
   * Returns the resource adapter config.
   */
  public ResourceAdapterConfig getResourceAdapter()
  {
    return _resourceAdapter;
  }

  public void init()
  {
  }

  public static class License {
    public void setDescription(String value)
    {
    }
    
    public void setLicenseRequired(boolean required)
    {
    }
  }
}
