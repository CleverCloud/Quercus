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

import com.caucho.jsp.cfg.TldTag;
import com.caucho.jsp.java.TagInfoExt;

/**
 * Stores the entire information for a tag library.
 */
public class TagInfoImpl extends TagInfoExt {
  private TldTag _tldTag;
  
  /**
   * Creates the tag info.
   */
  TagInfoImpl(TldTag tag, TldTag baseTag, Taglib taglib)
  {
    this(baseTag, taglib);

    _tldTag = tag;
  }
  
  /**
   * Creates the tag info.
   */
  TagInfoImpl(TldTag tag, Taglib taglib)
  {
    super(tag.getName(),
          tag.getTagClassName(),
          tag.getBodyContent(),
          tag.getDescription() != null ? tag.getDescription() : tag.getInfo(),
          taglib,
          tag.getTagExtraInfo(),
          tag.getAttributes(),
          tag.getDisplayName(),
          tag.getSmallIcon(),
          tag.getLargeIcon(),
          tag.getVariables(),
          tag.getDynamicAttributes(),
          tag.getDynamicAttributeName(),
          tag.getDependencyList());

    _tldTag = tag;
  }

  public TldTag getTldTag()
  {
    return _tldTag;
  }

  /**
   * Validates the tag.
   */
  public void validate()
    throws JspParseException
  {
    _tldTag.validate();
  }
}
