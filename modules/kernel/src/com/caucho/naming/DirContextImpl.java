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

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 * Resin's implementation of the JNDI <code>DirContext</code>.
 * The actual storage
 * of the persistent data is in the <code>AbstractModel</code>.
 *
 * <p>The <code>DirContextImpl</code> is just a Visitor around
 * the <code>AbstractModel</code> which also encapsulate
 * the JNDI environment.
 *
 * <p>In JNDI, each <code>Context</code> is a &lt;model, env> pair.
 * Each client might pass a different environment
 * to the <code>InitialContext</code> so each <code>ContextImpl</code>
 * must be unique for each client.  (Granted, this is a bit wasteful of
 * space which is why JNDI values should be cached.)
 *
 * <p>Applications which want a different model can still use
 * <code>ContextImpl</code> and specify the root
 * object for <code>AbstractModel</code>.  <code>ContextImpl</code> will
 * take care of the JNDI API for the model.
 */
public class DirContextImpl extends ContextImpl implements DirContext {
  protected static L10N L = new L10N(DirContextImpl.class);
  protected static Logger log 
    = Logger.getLogger(DirContextImpl.class.getName());

  /**
   * Creates a <code>DirContextImpl</code>.
   *
   * @param model The underlying storage node.
   * @param env The client's JNDI environment.
   */
  public DirContextImpl(AbstractModel model, Hashtable env)
  {
    super(model, env);
  }

  /**
   * Creates a <code>DirContextImpl</code>.
   *
   * @param name JNDI name, used for error messages, etc.
   * @param model The underlying storage node.
   * @param env The client's JNDI environment.
   */
  public DirContextImpl(String name, AbstractModel model, Hashtable env)
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
    return new DirContextImpl(name, model, env);
  }

  // stubs for now

  public void bind(Name name, Object obj, Attributes attrs)
  {
    throw new UnsupportedOperationException();
  }

  public void bind(String name, Object obj, Attributes attrs)
  {
    throw new UnsupportedOperationException();
  }

  public DirContext createSubcontext(Name name, Attributes attrs)
  {
    throw new UnsupportedOperationException();
  }

  public DirContext createSubcontext(String name, Attributes attrs)
  {
    throw new UnsupportedOperationException();
  }

  public Attributes getAttributes(Name name)
  {
    throw new UnsupportedOperationException();
  }

  public Attributes getAttributes(String name)
  {
    throw new UnsupportedOperationException();
  }

  public Attributes getAttributes(Name name, String []attrIds)
  {
    throw new UnsupportedOperationException();
  }

  public Attributes getAttributes(String name, String []attrIds)
  {
    throw new UnsupportedOperationException();
  }

  public DirContext getSchema(Name name)
  {
    throw new UnsupportedOperationException();
  }

  public DirContext getSchema(String name)
  {
    throw new UnsupportedOperationException();
  }

  public DirContext getSchemaClassDefinition(Name name)
  {
    throw new UnsupportedOperationException();
  }

  public DirContext getSchemaClassDefinition(String name)
  {
    throw new UnsupportedOperationException();
  }

  public void modifyAttributes(Name name, int mod_op, Attributes attrs)
  {
    throw new UnsupportedOperationException();
  }

  public void modifyAttributes(String name, int mod_op, Attributes attrs)
  {
    throw new UnsupportedOperationException();
  }

  public void modifyAttributes(Name name, ModificationItem []mods)
  {
    throw new UnsupportedOperationException();
  }

  public void modifyAttributes(String name, ModificationItem []mods)
  {
    throw new UnsupportedOperationException();
  }

  public void rebind(Name name, Object obj, Attributes attrs)
  {
    throw new UnsupportedOperationException();
  }

  public void rebind(String name, Object obj, Attributes attrs)
  {
    throw new UnsupportedOperationException();
  }

  public NamingEnumeration search(Name name, Attributes attrs)
  {
    throw new UnsupportedOperationException();
  }

  public NamingEnumeration search(String name, Attributes attrs)
  {
    throw new UnsupportedOperationException();
  }

  public NamingEnumeration search(Name name, Attributes attrs, String []args)
  {
    throw new UnsupportedOperationException();
  }

  public NamingEnumeration search(String name, Attributes attrs, String []args)
  {
    throw new UnsupportedOperationException();
  }

  public NamingEnumeration search(Name name, String filterExpr,
                                  Object []filterArgs, SearchControls cons)
  {
    throw new UnsupportedOperationException();
  }

  public NamingEnumeration search(String name, String filterExpr,
                                  Object []filterArgs, SearchControls cons)
  {
    throw new UnsupportedOperationException();
  }

  public NamingEnumeration search(Name name, String filterExpr,
                                  SearchControls cons)
  {
    throw new UnsupportedOperationException();
  }

  public NamingEnumeration search(String name, String filterExpr,
                                  SearchControls cons)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a string value.
   */
  public String toString()
  {
    return "[DirContextImpl " + _name + "]";
  }
}
