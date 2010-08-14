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

package com.caucho.jms.connection;

import com.caucho.VersionFactory;
import com.caucho.util.L10N;

import javax.jms.ConnectionMetaData;
import javax.jms.JMSException;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Metadata
 */
public class ConnectionMetaDataImpl implements ConnectionMetaData {
  static final L10N L = new L10N(ConnectionMetaDataImpl.class);

  private Vector<String> _propertyNames = new Vector<String>();

  ConnectionMetaDataImpl()
  {
    _propertyNames.add("JMSXGroupID");
    _propertyNames.add("JMSXGroupSeq");
  }
  
  /**
   * Returns the major version.
   */
  public int getJMSMajorVersion()
    throws JMSException
  {
    return 1;
  }

  /**
   * Returns the minor version.
   */
  public int getJMSMinorVersion()
    throws JMSException
  {
    return 1;
  }

  /**
   * Returns the provider name.
   */
  public String getJMSProviderName()
    throws JMSException
  {
    return "Caucho Technology";
  }

  /**
   * Returns the version name.
   */
  public String getJMSVersion()
    throws JMSException
  {
    return "1.1";
  }

  /**
   * Returns an enumeration of the property names.
   */
  public Enumeration getJMSXPropertyNames()
  {
    return _propertyNames.elements();
  }

  /**
   * Returns the provider's major version.
   */
  public int getProviderMajorVersion()
    throws JMSException
  {
    return 3;
  }

  /**
   * Returns the provider's minor version.
   */
  public int getProviderMinorVersion()
    throws JMSException
  {
    return 0;
  }

  /**
   * Returns the provider version.
   */
  public String getProviderVersion()
    throws JMSException
  {
    return VersionFactory.getVersion();
  }
}

