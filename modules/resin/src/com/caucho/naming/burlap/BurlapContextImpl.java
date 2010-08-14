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

package com.caucho.naming.burlap;

import com.caucho.naming.AbstractModel;
import com.caucho.naming.ContextImpl;
import com.caucho.util.L10N;

import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 * Burlap implementation of the JNDI <code>Context</code>.
 *  The actual storage of the persistent data is in
 * the <code>AbstractModel</code>.
 */
public class BurlapContextImpl extends ContextImpl {
  protected static final Logger dbg
    = Logger.getLogger(BurlapContextImpl.class.getName());
  protected static final L10N L = new L10N(BurlapContextImpl.class);

  /**
   * Creates a <code>ContextImpl</code>.
   *
   * @param model The underlying storage node.
   * @param env The client's JNDI environment.
   */
  public BurlapContextImpl(AbstractModel model, Hashtable env)
  {
    super(model, env);
  }

  /**
   * Creates a <code>ContextImpl</code>.
   *
   * @param name JNDI name, used for error messages, etc.
   * @param model The underlying storage node.
   * @param env The client's JNDI environment.
   */
  public BurlapContextImpl(String name, AbstractModel model, Hashtable env)
  {
    super(name, model, env);
  }

  /**
   * Creates a new instance of the <code>ContextImpl</code>.  Subclasses will
   * override this method to return a new instance of the subclass.
   *
   * @param name the JNDI name for the new context
   * @param model the underlying storage node
   * @param env the client's JNDI environment.
   *
   * @return a new instance of the implementing class.
   */
  protected ContextImpl create(String name, AbstractModel model, Hashtable env)
  {
    return new BurlapContextImpl(name, model, env);
  }

  /**
   * Parses the head of the name.
   */
  protected String parseFirst(String name)
    throws NamingException
  {
    return name;
  }
    
  /**
   * Parses the tail of the name.
   */
  protected String parseRest(String name)
    throws NamingException
  {
    return null;
  }

  /**
   * Returns a string value.
   */
  public String toString()
  {
    return "BurlapContextImpl[" + getName() + "]";
  }
}
