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

package com.caucho.jsp;

import javax.servlet.jsp.tagext.*;

/**
 * Temporary tag information for recursive tags.
 */
public class TempTagInfo extends TagInfo {
  private String _bodyContent;
  private String _infoString;
  private String _tagClassName;
  private TagExtraInfo _tagExtraInfo;
  private TagLibraryInfo _tagLibrary;
  private String _tagName;
  private TagAttributeInfo []_attributeInfo;
  private String _displayName;
  private String _smallIcon;
  private String _largeIcon;
  private TagVariableInfo []_tvi;
  private boolean _dynamicAttributes;

  public TempTagInfo()
  {
    super(null, null, null, null, null, null, null);
  }

  @Override
  public String getTagName()
  {
    return _tagName;
  }
  
  public void setTagName(String tagName)
  {
    _tagName = tagName;
  }
  
  @Override
  public String getInfoString()
  {
    return _infoString;
  }
  
  public void setInfoString(String infoString)
  {
    _infoString = infoString;
  }

  @Override
  public String getTagClassName()
  {
    return _tagClassName;
  }
  
  public void setTagClassName(String tagClassName)
  {
    _tagClassName = tagClassName;
  }

  @Override
  public String getBodyContent()
  {
    return _bodyContent;
  }

  public void setBodyContent(String bodyContent)
  {
    _bodyContent = bodyContent;
  }

  @Override
  public String getDisplayName()
  {
    return _displayName;
  }
  
  public void setDisplayName(String displayName)
  {
    _displayName = displayName;
  }

  @Override
  public String getLargeIcon()
  {
    return _largeIcon;
  }
  
  public void setLargeIcon(String largeIcon)
  {
    _largeIcon = largeIcon;
  }

  @Override
  public String getSmallIcon()
  {
    return _smallIcon;
  }

  public void setSmallIcon(String smallIcon)
  {
    _smallIcon = smallIcon;
  }

  @Override
  public TagAttributeInfo[] getAttributes()
  {
    return _attributeInfo;
  }

  public void setAttributes(TagAttributeInfo []attributeInfo)
  {
    _attributeInfo = attributeInfo;
  }
  
  @Override
  public TagVariableInfo []getTagVariableInfos()
  {
    return _tvi;
  }
  
  public void setTagVariableInfos(TagVariableInfo []tvi)
  {
    _tvi = tvi;
  }

  @Override
  public VariableInfo[] getVariableInfo(TagData data)
  {
    if (_tagExtraInfo == null)
      return null;
    else
      return _tagExtraInfo.getVariableInfo(data);
  }

  @Override
  public boolean hasDynamicAttributes()
  {
    return _dynamicAttributes;
  }

  public void setDynamicAttributes(boolean isDynamicAttributes)
  {
    _dynamicAttributes = isDynamicAttributes;
  }

  public boolean isValid(TagData data)
  {
    if (_tagExtraInfo == null)
      return true;
    else
      return _tagExtraInfo.isValid(data);
  }

  @Override
  public TagExtraInfo getTagExtraInfo()
  {
    return _tagExtraInfo;
  }

  public void setTagExtraInfo(TagExtraInfo tei)
  {
    _tagExtraInfo = tei;
  }

  @Override
  public TagLibraryInfo getTagLibrary()
  {
    return _tagLibrary;
  }

  public void setTagLibrary(TagLibraryInfo info)
  {
    _tagLibrary = info;
  }

  public ValidationMessage []validate(TagData data)
  {
    if (_tagExtraInfo == null)
      return null;
    else
      return _tagExtraInfo.validate(data);
  }
}
