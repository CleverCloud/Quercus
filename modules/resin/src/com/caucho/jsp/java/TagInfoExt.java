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

package com.caucho.jsp.java;

import java.util.ArrayList;

import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagLibraryInfo;
import javax.servlet.jsp.tagext.TagVariableInfo;

import com.caucho.make.DependencyContainer;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.PersistentDependency;

/**
 * Information for a tag file.
 */
public class TagInfoExt extends TagInfo {
  private String _dynamicAttributesName;
  
  private ArrayList<PersistentDependency> _dependList
    = new ArrayList<PersistentDependency>();

  public TagInfoExt(String tagName,
                    String tagClassName,
                    String bodyContent,
                    String infoString,
                    TagLibraryInfo taglib,
                    TagExtraInfo tagExtraInfo,
                    TagAttributeInfo []attributeInfo,
                    String displayName,
                    String smallIcon,
                    String largeIcon,
                    TagVariableInfo []tvi,
                    boolean hasDynamicAttributes,
                    String dynamicAttributesName,
                    ArrayList<Dependency> dependList)
  {
    super(tagName, tagClassName, bodyContent, infoString,
          taglib, tagExtraInfo, attributeInfo, displayName,
          smallIcon, largeIcon, tvi, hasDynamicAttributes);

    _dynamicAttributesName = dynamicAttributesName;
    
    if (dependList != null) {
      for (Dependency depend : dependList) {
        _dependList.add((PersistentDependency) depend);
      }
    }
  }

  public String getDynamicAttributesName()
  {
    return _dynamicAttributesName;
  }
  
  /**
   * Returns the dependency list.
   */
  public ArrayList<PersistentDependency> getDependList()
  {
    return _dependList;
  }
}
