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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Alex Rojkov
 */

package com.caucho.jsf.cfg;

import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;
import com.caucho.config.ConfigException;
import static com.caucho.jsf.application.SessionStateManager.StateSerializationMethod;

import javax.faces.application.Application;
import java.lang.reflect.Method;

/**
 * Configuration for the jsf-property-group.
 */
public class JsfPropertyGroup {
  private static final L10N L = new L10N(JsfPropertyGroup.class);

  private static boolean _isDefaultFastJsf;

  private WebApp _webApp;

  private String _id;

  private boolean _fastJsf = _isDefaultFastJsf;

  private StateSerializationMethod _stateSerializationMethod
    = StateSerializationMethod.HESSIAN;

  private boolean _enableDeveloperAid;
  private String _developerAidLinkStyle;

  public JsfPropertyGroup()
  {
  }

  public JsfPropertyGroup(WebApp webApp)
  {
    _webApp = webApp;
  }

  /**
   * Returns the group's identifier.
   */
  public String getId()
  {
    return _id;
  }

  /**
   * Sets the group's identifier.
   */
  public void setId(String id)
  {
    _id = id;
  }

  /**
   * Sets the group's description
   */
  public void setDescription(String description)
  {
  }

  /**
   * Sets the group's display name
   */
  public void setDisplayName(String displayName)
  {
  }

  /**
   * Set if fast jsf is allowed.
   */
  public void setFastJsf(boolean fastJsf)
  {
    _fastJsf = fastJsf;
  }

  /**
   * Return true if fast jsf is allowed.
   */
  public boolean isFastJsf()
  {
    return _fastJsf;
  }

  public StateSerializationMethod getStateSerializationMethod()
  {
    return _stateSerializationMethod;
  }

  public void setStateSerializationMethod(String stateSerializationMethod)
  {
    if ("hessian".equals(stateSerializationMethod))
      _stateSerializationMethod
        = StateSerializationMethod.HESSIAN;
    else if ("java".equals(stateSerializationMethod))
      _stateSerializationMethod
        = StateSerializationMethod.JAVA;
    else
      throw new ConfigException(L.l("'{0}' is a valid serialization method",
                                    stateSerializationMethod));
  }

  public void setEnableDeveloperAid(boolean enableDeveloperAid) {
    _enableDeveloperAid = enableDeveloperAid;
  }

  public boolean isEnableDeveloperAid()
  {
    return _enableDeveloperAid;
  }

  public String getDeveloperAidLinkStyle()
  {
    return _developerAidLinkStyle;
  }

  public void setDeveloperAidLinkStyle(String developerAidLinkStyle)
  {
    _developerAidLinkStyle = developerAidLinkStyle;
  }

  static {
    try {
      Method m = Application.class.getDeclaredMethod("__caucho__");

      if (m != null)
        _isDefaultFastJsf = true;
    } catch (Exception e) {
    }
  }
}