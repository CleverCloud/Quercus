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

package com.caucho.naming;

import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import javax.naming.*;
import java.io.IOException;
import java.util.Hashtable;

public class PathJndiContext implements Context {
  private static L10N L = new L10N(PathJndiContext.class);

  private PathJndiContext _root;
  private Path _path;

  public PathJndiContext(Path path)
  {
    _root = this;
    _path = path;
  }

  PathJndiContext(Path path, PathJndiContext root)
  {
    _root = root;
    _path = path;
    if (root == null)
      _root = root;
  }

  public Path getPath()
  {
    return _path;
  }

  public Object lookup(String name)
    throws NamingException
  {
    if (name == null || name.equals(""))
      return new PathJndiContext(_path.lookup((String) null), _root);

    Path subpath = _path.lookup(name);

    if (subpath == null) {
      throw new NamingException(L.l("bad path {0}", name));
    }
      
    if (subpath.isDirectory())
      return new PathJndiContext(subpath, _root);

    else if (subpath.isObject()) {
      try {
        return subpath.getValue();
      } catch (Exception e) {
        throw new NamingException(e.toString());
      }
    }

    else if (! subpath.exists())
      return null;
    
    else
      throw new NamingException(L.l("lookup can't handle files"));
  }
  
  public Object lookup(Name name)
    throws NamingException
  {
    return lookup(name.toString());
  }

  public void bind(String name, Object obj)
    throws NamingException
  {
    Path subpath = _path.lookup(name);

    Path parent = subpath.getParent();
    if (! parent.exists()) {
      try {
        parent.mkdirs();
      } catch (IOException e) {
        throw new NamingException(e.toString());
      }
    }

    if (! parent.isDirectory())
      throw new NamingException(L.l("bind expects directory for `{0}'",
                                    subpath.getParent()));
    else if (subpath.exists())
      throw new NameAlreadyBoundException(L.l("`{0}' already has a binding",
                                              subpath));

      try {
      subpath.setValue(obj);
    } catch (Exception e) {
      throw new NamingException(e.toString());
    }
  }
  
  public void bind(Name name, Object obj)
    throws NamingException
  {
    bind(name.toString(), obj);
  }

  public void rebind(String name, Object obj)
    throws NamingException
  {
    Path subpath = _path.lookup(name);

    try {
      Path parent = subpath.getParent();
      if (! parent.exists())
        parent.mkdirs();
      subpath.setValue(obj);
    } catch (Exception e) {
      throw new NamingException(e.toString());
    }
  }
  
  public void rebind(Name name, Object obj)
    throws NamingException
  {
    rebind(name.toString(), obj);
  }

  public void unbind(String name)
    throws NamingException
  {
    Path subpath = _path.lookup(name);

    try {
      subpath.remove();
    } catch (IOException e) {
      throw new NamingException(L.l("can't remove `{0}'"));
    }
  }
  
  public void unbind(Name name)
    throws NamingException
  {
    unbind(name.toString());
  }

  public void rename(String oldName, String newName)
    throws NamingException
  {
    Object obj = lookup(oldName);
    bind(newName, obj);
    unbind(oldName);
  }

  public void rename(Name oldName, Name newName)
    throws NamingException
  {
    Object obj = lookup(oldName);
    bind(newName, obj);
    unbind(oldName);
  }

  public NamingEnumeration list(String name)
    throws NamingException
  {
    return null;
  }

  public NamingEnumeration list(Name name)
    throws NamingException
  {
    return list(name.toString());
  }

  public NamingEnumeration listBindings(String name)
    throws NamingException
  {
    return null;
  }

  public NamingEnumeration listBindings(Name name)
    throws NamingException
  {
    return list(name.toString());
  }

  public void destroySubcontext(String name)
    throws NamingException
  {
    Path subpath = _path.lookup(name);

    if (! subpath.exists())
      throw new NameNotFoundException(name);
    if (! subpath.isDirectory())
      throw new NotContextException(name);

    try {
      subpath.remove();
    } catch (IOException e) {
      throw new ContextNotEmptyException(name);
    }
  }

  public void destroySubcontext(Name name)
    throws NamingException
  {
    destroySubcontext(name.toString());
  }

  public Context createSubcontext(String name)
    throws NamingException
  {
    Path subpath = _path.lookup(name);

    if (! subpath.getParent().isDirectory())
      throw new NamingException(L.l("parent of `{0}' must be directory",
                                    name));

    try {
      subpath.mkdir();
    } catch (IOException e) {
      throw new ContextNotEmptyException(name);
    }

    return new PathJndiContext(subpath, _root);
  }

  public Context createSubcontext(Name name)
    throws NamingException
  {
    return createSubcontext(name.toString());
  }

  public Object lookupLink(String name)
    throws NamingException
  {
    throw new NamingException(L.l("links not supported"));
  }

  public Object lookupLink(Name name)
    throws NamingException
  {
    return lookupLink(name.toString());
  }

  public NameParser getNameParser(String name)
    throws NamingException
  {
    return new PathNameParser();
  }

  public NameParser getNameParser(Name name)
    throws NamingException
  {
    return getNameParser(name.toString());
  }

  public String composeName(String suffix, String prefix)
    throws NamingException
  {
    return prefix + "/" + suffix;
  }

  public Name composeName(Name suffix, Name prefix)
    throws NamingException
  {
    return null;
  }

  public String getNameInNamespace()
    throws NamingException
  {
    return _path.getPath();
  }

  public Object addToEnvironment(String prop, Object value)
    throws NamingException
  {
    throw new UnsupportedOperationException();
    /*
    try {
      Object old = _path.getAttribute(prop);
      _path.setAttribute(prop, value);
      return old;
    } catch (IOException e) {
      throw new NamingException(e.toString());
    }
    */
  }

  public Object removeFromEnvironment(String prop)
    throws NamingException
  {
    throw new UnsupportedOperationException();
    /*
    try {
      Object old = _path.getAttribute(prop);
      _path.removeAttribute(prop);
      return old;
    } catch (IOException e) {
      throw new NamingException(e.toString());
    }
    */
  }

  public Hashtable getEnvironment()
    throws NamingException
  {
    return null;
  }

  public void close()
    throws NamingException
  {
  }

  static class PathNameParser implements NameParser {
    public Name parse(String name)
      throws NamingException
    {
      return new CompositeName(name);
    }
  }
}
