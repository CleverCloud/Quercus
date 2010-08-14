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

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 * Error context always throwing NamingExceptions.
 */
public class ErrorContext implements Context {
  protected static final Logger log
    = Logger.getLogger(ErrorContext.class.getName());
  protected static L10N L = new L10N(ErrorContext.class);

  protected NamingException exception;

  public ErrorContext(Throwable exn)
  {
    if (exn instanceof NamingException)
      this.exception = (NamingException) exn;
    else
      this.exception = new NamingExceptionWrapper(exn);
  }

  /**
   * Looks up an object using its full string name.  The path is searched
   * recursively.  <code>parseFirst</code> returns the first segment.  The
   */
  public Object lookup(String name)
    throws NamingException
  {
    throw exception;
  }

  /**
   * Looks up an object with the given parsed JNDI name.
   */
  public Object lookup(Name name)
    throws NamingException
  {
    throw exception;
  }

  public Object lookupLink(String name)
    throws NamingException
  {
    throw exception;
  }

  public Object lookupLink(Name name)
    throws NamingException
  {
    throw exception;
  }

  public void bind(String name, Object obj)
    throws NamingException
  {
    throw exception;
  }
  
  public void bind(Name name, Object obj)
    throws NamingException
  {
    throw exception;
  }

  public void rebind(String name, Object obj)
    throws NamingException
  {
    throw exception;
  }
  
  public void rebind(Name name, Object obj)
    throws NamingException
  {
    throw exception;
  }

  public void unbind(String name)
    throws NamingException
  {
    throw exception;
  }
  
  public void unbind(Name name)
    throws NamingException
  {
    throw exception;
  }

  public void rename(String oldName, String newName)
    throws NamingException
  {
    throw exception;
  }

  public void rename(Name oldName, Name newName)
    throws NamingException
  {
    throw exception;
  }

  public NamingEnumeration list(String name)
    throws NamingException
  {
    throw exception;
  }

  public NamingEnumeration list(Name name)
    throws NamingException
  {
    throw exception;
  }

  public NamingEnumeration listBindings(String name)
    throws NamingException
  {
    throw exception;
  }

  public NamingEnumeration listBindings(Name name)
    throws NamingException
  {
    throw exception;
  }

  public Context createSubcontext(String name)
    throws NamingException
  {
    throw exception;
  }

  public Context createSubcontext(Name name)
    throws NamingException
  {
    throw exception;
  }

  public void destroySubcontext(String name)
    throws NamingException
  {
    throw exception;
  }
  
  public void destroySubcontext(Name name)
    throws NamingException
  {
    throw exception;
  }

  public NameParser getNameParser(String name)
    throws NamingException
  {
    throw exception;
  }

  public NameParser getNameParser(Name name)
    throws NamingException
  {
    throw exception;
  }

  public String composeName(String prefix, String suffix)
    throws NamingException
  {
    throw exception;
  }

  public Name composeName(Name prefix, Name suffix)
    throws NamingException
  {
    throw exception;
  }

  public String getNameInNamespace()
    throws NamingException
  {
    throw exception;
  }

  public Object addToEnvironment(String prop, Object value)
    throws NamingException
  {
    throw exception;
  }

  public Object removeFromEnvironment(String prop)
    throws NamingException
  {
    throw exception;
  }

  public Hashtable getEnvironment()
    throws NamingException
  {
    throw exception;
  }

  /**
   * Close is indended to free any transient data, like a cached
   * socket.  It does not affect the JNDI tree.
   */
  public void close()
    throws NamingException
  {
    throw exception;
  }
}
