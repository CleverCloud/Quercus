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
 * @author Alex Rojkov
 */

package com.caucho.server.webapp;

import com.caucho.config.Configurable;
import com.caucho.config.SchemaBean;
import com.caucho.util.L10N;

import java.util.logging.Logger;

public class WebAppFragmentConfig extends WebAppConfig
  implements SchemaBean {

  private static final L10N L = new L10N(WebApp.class);
  private static final Logger log
    = Logger.getLogger(WebAppFragmentConfig.class.getName());

  //web-fragment name
  private String _name;

  //web-fragment metadata-complete
  private boolean _isMetadataComplete;

  //web-fragment jar
  private String _jarPath;

  private Ordering _ordering;

  public String getName() {
    return _name;
  }

  @Configurable
  public void setName(String name) {
    _name = name;
  }


  public boolean isMetadataComplete() {
    return _isMetadataComplete;
  }

  @Configurable
  public void setMetadataComplete(boolean metadataComplete) {
    _isMetadataComplete = metadataComplete;
  }

  // XXX: this will make tck tests with misspelled metadata-complete deploy.
  // tck test generally seems valid except for this problem
  @Configurable
  public void setMetaDataComplete(boolean metadataComplete) {
    _isMetadataComplete = metadataComplete;
  }

  public String getJarPath() {
    return _jarPath;
  }

  public void setJarPath(String jarPath) {
    _jarPath = jarPath;
  }

  public Ordering createOrdering() {
    if (_ordering != null)
      throw new IllegalStateException();

    _ordering = new Ordering();

    return _ordering;
  }

  public Ordering getOrdering() {
    return _ordering;
  }

  public Ordering createAbsoluteOrdering() {
    log.finer(L.l("'{0}' absolute-ordering tag should not be used inside web application descriptor.", this));

    return new Ordering();
  }

  @Override
  public String getSchema()
  {
    return "com/caucho/server/webapp/resin-web-xml.rnc";
  }

  public String toString() {
    return "WebAppFragmentConfig [" + _name + "]";
  }
}
