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
 * @author Scott Ferguson;
 */

package com.caucho.config.types;

public class DescriptionGroupConfig
{
  private String _id;
  private String _description;
  private String _displayName;
  private Icon   _icon;

  public void setId(String id)
  {
    _id = id;
  }

  public void setDescription(String description)
  {
    _description = description;
  }

  public void setDisplayName(String displayName)
  {
    _displayName = displayName;
  }

  public String getDisplayName()
  {
    return _displayName;
  }

  public Icon createIcon()
  {
    return (_icon = new Icon());
  }

  public class Icon {
    private String _id;
    private String _lang;
    private String _smallIcon;
    private String _largeIcon;

    public String getId()
    {
      return _id;
    }

    public void setId(String id)
    {
      _id = id;
    }

    public String getLang()
    {
      return _lang;
    }

    public void setLang(String lang)
    {
      _lang = lang;
    }

    public String getSmallIcon()
    {
      return _smallIcon;
    }

    public void setSmallIcon(String icon)
    {
      _smallIcon = icon;
    }

    public String getLargeIcon()
    {
      return _largeIcon;
    }

    public void setLargeIcon(String largeIcon)
    {
      _largeIcon = largeIcon;
    }
  }
}
