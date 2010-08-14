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

package javax.jcr;

import java.util.HashMap;

public final class SimpleCredentials implements Credentials {
  private final HashMap attributes = new HashMap();
  private final char []password;
  private final String userID;
  
  public SimpleCredentials(String userID, char []password)
  {
    this.userID = userID;
    this.password = (char []) password.clone();
  }

  public char []getPassword()
  {
    return this.password;
  }

  public String getUserID()
  {
    return this.userID;
  }
  
  public void setAttribute(String name, Object value)
  {
    if (name == null)
      throw new NullPointerException();

    synchronized (this.attributes) {
      if (value != null)
        this.attributes.put(name, value);
      else
        this.attributes.remove(name);
    }
  }

  public Object getAttribute(String name)
  {
    synchronized (this.attributes) {
      return this.attributes.get(name);
    }
  }

  public void removeAttribute(String name)
  {
    synchronized (this.attributes) {
      this.attributes.remove(name);
    }
  }

  public String[] getAttributeNames()
  {
    synchronized (attributes) {
      String []names = new String[this.attributes.size()];

      this.attributes.keySet().toArray(names);

      return names;
    }
  }
}
