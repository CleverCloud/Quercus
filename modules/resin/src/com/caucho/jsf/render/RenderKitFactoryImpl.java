/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.jsf.render;

import javax.faces.render.*;

import java.util.*;

import javax.faces.context.*;

import com.caucho.jsf.html.HtmlBasicRenderKit;
import com.caucho.util.*;

/**
 * Factory for registering JSF render kits.
 */
public class RenderKitFactoryImpl extends RenderKitFactory {
  private static final L10N L = new L10N(RenderKitFactoryImpl.class);
  
  private final HashMap<String,RenderKit> _renderKitMap
    = new HashMap<String,RenderKit>();
  
  public static final String HTML_BASIC_RENDER_KIT = "HTML_BASIC";

  public RenderKitFactoryImpl()
  {
    _renderKitMap.put(HTML_BASIC_RENDER_KIT, new HtmlBasicRenderKit());
  }

  /**
   * Adds a new named RenderKit to the factory map.
   */
  public void addRenderKit(String name,
                           RenderKit renderKit)
  {
    if (name == null)
      throw new NullPointerException();
    if (renderKit == null)
      throw new NullPointerException();

    _renderKitMap.put(name, renderKit);
  }

  /**
   * Returns the named render kit.
   */
  public RenderKit getRenderKit(FacesContext context,
                                String name)
  {
    return _renderKitMap.get(name);
  }

  /**
   * Returns the names of the registered render kits.
   */
  public Iterator<String> getRenderKitIds()
  {
    return _renderKitMap.keySet().iterator();
  }

  public String toString()
  {
    return "RenderKitFactoryImpl[]";
  }
}
