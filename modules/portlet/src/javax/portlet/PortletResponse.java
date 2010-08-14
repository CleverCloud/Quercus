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
 * @author Sam
 */


package javax.portlet;



public interface PortletResponse
{
  public void addProperty(String key, String value);

  public void setProperty(String key, String value);

  /**
   * Encode a url to a resource.
   *
   * The <code>path</code> may be an absolute URL ("http://myserver/...")
   * or a URI with a full path ("/myapp/mypath/....").
   *
   * <code>path</code> may also be a relative path ("images/myimage.gif"), in
   * which case it is a url to a resource in the current "portal", typically a
   * path relative to the current webapp.  Allowing a relative path is an
   * extension of the behaviour defined by the portlet specification.
   *
   * The returned URL is always an absolute url.  Some browsers do not
   * understand relative url's supplied for certain parameters (such as the
   * location of css files).
   *
   * @return an absolute URL
   *
   * @see javax.portlet.PortletResponse#encodeURL
   */
  public String encodeURL(String path);

}

