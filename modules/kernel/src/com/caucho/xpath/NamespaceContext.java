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

package com.caucho.xpath;

public class NamespaceContext {
  private NamespaceContext _prev;
  private String _prefix;
  private String _url;

  public NamespaceContext(NamespaceContext prev, String prefix, String url)
  {
    _prev = prev;
    _prefix = prefix;
    _url = url;
  }

  public NamespaceContext getPrev()
  {
    return _prev;
  }

  public String getPrefix()
  {
    return _prefix;
  }

  public String getUrl()
  {
    return _url;
  }

  public static String find(NamespaceContext ns, String prefix)
  {
    for (; ns != null; ns = ns._prev) {
      if (ns._prefix.equals(prefix))
        return ns._url;
    }

    return null;
  }
}
