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

package com.caucho.vfs;

import com.caucho.util.L10N;
import com.caucho.util.Log;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Adapts the JNDI to the Path API.  The name separator is always '/'.
 *
 * @since Resin 1.2
 */
public class JndiPath extends FilesystemPath {
  protected static final Logger log = Log.open(JndiPath.class);
  protected static final L10N L = new L10N(JndiPath.class);

  private Context parent;

  /**
   * Creates a new JndiPath root.
   */
  public JndiPath()
  {
    super(null, "/", "/");

    _root = this;
  }

  /**
   * Create a new JndiPath with the given name.
   */
  protected JndiPath(FilesystemPath root, String userPath, String path)
  {
    super(root, userPath, path);
  }

  /**
   * Walking down the path just stores the new name in the created Path.
   *
   * @param userPath the string used in the <code>lookup</code> call.
   * @param attributes any inherited attributes.
   * @param path the normalized slash-separated path.
   * @return a new JndiPath representing the new path.
   */
  public Path fsWalk(String userPath,
                     Map<String,Object> attributes,
                     String path)
  {
    return new JndiPath(_root, userPath, path);
  }

  /**
   * The scheme is always "jndi:".
   */
  public String getScheme()
  {
    return "jndi";
  }

  /**
   * Create a new subcontext
   */
  public boolean mkdir()
    throws IOException
  {
    try {
      Context parent = getAllButLast(getPath());

      parent.createSubcontext(getTail());

      return true;
    } catch (Exception e) {
      throw new IOExceptionWrapper(e);
    }
  }
  
  /**
   * Returns the object bound at this path.
   */
  public Object getObject()
    throws IOException
  {
    try {
      Context parent = getAllButLast(getPath());

      return parent.lookup(getTail());
    } catch (Exception e) {
      throw new IOExceptionWrapper(e);
    }
  }
  
  /**
   * Sets the object bound at this path.
   *
   * @param value the new value
   */
  public void setObject(Object value)
    throws IOException
  {
    try {
      Context parent = getAllButLast(getPath());

      parent.rebind(getTail(), value);
    } catch (Exception e) {
      e.printStackTrace();
      throw new IOExceptionWrapper(e);
    }
  }

  /**
   * Returns the context found by looking up all but the last segment
   * of the path.
   *
   * @param path slash-separated path
   * @return context of the parent path
   */
  private Context getAllButLast(String path)
    throws NamingException
  {
    if (parent != null)
      return parent;
    
    Context context = new InitialContext();

    int head = 1;
    int tail;

    while ((tail = path.indexOf('/', head)) > 0) {
      String section = path.substring(head, tail);

      if (context == null)
        throw new NamingException(L.l("null context for `{0}'", path));
      
      context = (Context) context.lookup(section);

      head = tail + 1;
    }

    parent = context;

    return parent;
  }
}
