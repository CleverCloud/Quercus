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

package com.caucho.jsp.cfg;

import com.caucho.server.webapp.WebApp;

import java.util.ArrayList;

/**
 * Configuration from the web.xml.
 *
 * <pre>
 * element jsp-config { taglib*
                        jsp-property-group* }
 * </pre>
 */
public class JspConfig {
  private ArrayList<JspTaglib> _taglibList = new ArrayList<JspTaglib>();

  private WebApp _webApp;
  
  private ArrayList<JspPropertyGroup> _propertyGroupList
    = new ArrayList<JspPropertyGroup>();

  public JspConfig(WebApp webApp)
  {
    _webApp = webApp;
  }

  /**
   * Adds a new taglib to the configuration.
   */
  public void addTaglib(JspTaglib taglib)
  {
    _taglibList.add(taglib);
  }

  /**
   * Returns the taglibs.
   */
  public ArrayList<JspTaglib> getTaglibList()
  {
    return _taglibList;
  }

  public JspPropertyGroup createJspPropertyGroup()
  {
    return new JspPropertyGroup(_webApp);
  }

  /**
   * Adds a new JspPropertyGroup to the configuration.
   */
  public void addJspPropertyGroup(JspPropertyGroup propertyGroup)
  {
    _propertyGroupList.add(propertyGroup);
  }

  /**
   * Returns the JspPropertyGroup list from the configuration.
   */
  public ArrayList<JspPropertyGroup> getJspPropertyGroupList()
  {
    return _propertyGroupList;
  }

  /**
   * Returns the first matching property group.
   */
  public JspPropertyGroup findJspPropertyGroup(String url)
  {
    for (int i = 0; i < _propertyGroupList.size(); i++) {
      JspPropertyGroup group = _propertyGroupList.get(i);

      if (group.match(url)) {
        return group;
      }
    }

    return null;
  }

  /**
   * Returns the first matching property group.
   */
  public ArrayList<JspPropertyGroup> findJspPropertyGroupList(String url)
  {
    ArrayList<JspPropertyGroup> list = new ArrayList<JspPropertyGroup>();
    
    for (int i = 0; i < _propertyGroupList.size(); i++) {
      JspPropertyGroup group = _propertyGroupList.get(i);

      if (group.match(url)) {
        list.add(group);
      }
    }

    return list;
  }
}
