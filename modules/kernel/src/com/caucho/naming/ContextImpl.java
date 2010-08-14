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

package com.caucho.naming;

import com.caucho.util.L10N;

import javax.naming.*;
import javax.naming.spi.NamingManager;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resin's implementation of the JNDI <code>Context</code>.  The actual storage
 * of the persistent data is in the <code>AbstractModel</code>.
 *
 * <p>The <code>ContextImpl</code> is just a Visitor around
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
public class ContextImpl implements Context {
  protected static final Logger log
    = Logger.getLogger(ContextImpl.class.getName());
  protected static final L10N L = new L10N(ContextImpl.class);

  protected Hashtable _env;
  protected AbstractModel _model;
  protected String _name;

  /**
   * Creates a <code>ContextImpl</code>.
   *
   * @param model The underlying storage node.
   * @param env The client's JNDI environment.
   */
  public ContextImpl(AbstractModel model, Hashtable env)
  {
    _model = model;
    _env = env;
    _name = "";

    if (_model == null)
      throw new NullPointerException();
  }

  /**
   * Creates a <code>ContextImpl</code>.
   *
   * @param name JNDI name, used for error messages, etc.
   * @param model The underlying storage node.
   * @param env The client's JNDI environment.
   */
  public ContextImpl(String name, AbstractModel model, Hashtable env)
  {
    _model = model;
    _env = env;
    _name = name;

    if (_model == null)
      throw new NullPointerException();
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
    return new ContextImpl(name, model, env);
  }

  /**
   * Creates a new <code>ContextImpl</code> when the name is irrelevant.
   *
   * @param model the underlying storage node
   * @param env the client's JNDI environment.
   *
   * @return a new instance of the implementing class.
   */
  final protected ContextImpl create(AbstractModel model, Hashtable env)
  {
    return create("", model, env);
  }

  /**
   * Returns the underlying storage node.
   */
  public AbstractModel getModel()
  {
    return _model;
  }

  /**
   * Returns the name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Looks up an object using its full string name.  The path is searched
   * recursively.  <code>parseFirst</code> returns the first segment.  The
   *
   * @param name the JNDI name to lookup for the child.
   *
   * @return the retrieved object
   */
  @Override
  public Object lookup(String name)
    throws NamingException
  {
    Object value = lookupImpl(name);

    if (value == NullValue.NULL) {
      // env/0gc9
      return null;
    }
    else if (value != null) {
     // server/1509
      return value;
    }
    else
      throw new NameNotFoundException(getFullPath(name));
  }

  /**
   * Looks up an object using its full string name.  The path is searched
   * recursively.  <code>parseFirst</code> returns the first segment.  The
   *
   * @param name the JNDI name to lookup for the child.
   *
   * @return the retrieved object
   */
  protected Object lookupImpl(String name)
    throws NamingException
  {
    String tail = name;
    AbstractModel model = _model;

    while (tail != null) {
      String first = parseFirst(tail);
      String rest = parseRest(tail);

      if (first == null)
        return create(getFullPath(name), model, _env);

      Object value = model.lookup(first);

      if (value instanceof AbstractModel) {
        model = (AbstractModel) value;
        tail = rest;
        continue;
      }

      value = dereference(value, null, model);

      if (rest == null)
        return value;
      else if (value instanceof Context)
        return ((Context) value).lookup(rest);
      else if (value != null)
        throw new NotContextException(L.l("{0}: expected intermediate context at `{1}'",
                                          getFullPath(name), value));
      else
        throw new NameNotFoundException(getFullPath(name));
    }

    return create(getFullPath(name), model, _env);
  }

  /**
   * Looks up an object with the given parsed JNDI name.
   */
  @Override
  public Object lookup(Name name)
    throws NamingException
  {
    Object value = lookupImpl(name);

    if (value == NullValue.NULL) {
      // env/0gf9
      return null;
    }
    else if (value != null) {
      return value;
    }
    else
      throw new NameNotFoundException(getFullPath(name));
  }
  
  private Object lookupImpl(Name name)
    throws NamingException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest(L.l("JNDI lookup `{0}'", name));

    if (name == null)
      return create(_model, _env);

    AbstractModel model = _model;

    for (int i = 0; i < name.size(); i++) {
      String first = name.get(i);

      Object value = model.lookup(first);

      if (value instanceof AbstractModel) {
        model = (AbstractModel) value;
        continue;
      }
      
      value = dereference(value, null, model);

      if (i + 1 == name.size()) {
        return value;
      }
      else if (value instanceof Context) {
        return ((Context) value).lookup(name.getSuffix(i + 1));
      }
      else if (value != null)
        throw new NotContextException(L.l("{0}: expected intermediate context at `{1}'",
                                          getFullPath(name), value));
      else
        throw new NameNotFoundException(getFullPath(name));
    }

    return create(getFullPath(name), model, _env);
  }

  /**
   * Looks up an object given the name, but doesn't dereference links.
   */
  @Override
  public Object lookupLink(String name)
    throws NamingException
  {
    String tail = name;
    AbstractModel model = _model;

    while (tail != null) {
      String first = parseFirst(tail);
      String rest = parseRest(tail);

      if (first == null) {
        return create(getFullPath(name), model, _env);
      }

      Object value = model.lookup(first);
   
      if (value instanceof AbstractModel) {
        model = (AbstractModel) value;
        tail = rest;
        continue;
      }

      if (rest == null) {
        if (value == NullValue.NULL)
          return null;
        else if (value != null)
          return value;
        else
          throw new NameNotFoundException(getFullPath(name));
      }

      value = dereference(value, null, model);

      if (value instanceof Context)
        return ((Context) value).lookupLink(rest);
      else if (value != null)
        throw new NotContextException(L.l("{0}: expected intermediate context at `{1}'",
                                          getFullPath(name), value));
      else
        throw new NameNotFoundException(getFullPath(name));
    }

    return create(getFullPath(name), model, _env);
  }

  /**
   * Looks up an object with the given parsed JNDI name, but don't
   * dereference the final object.
   */
  public Object lookupLink(Name name)
    throws NamingException
  {
    if (name == null)
      return create(_model, _env);
    
    AbstractModel model = _model;

    for (int i = 0; i < name.size(); i++) {
      String first = name.get(i);

      Object value = model.lookup(first);

      if (value instanceof AbstractModel) {
        model = (AbstractModel) value;
        continue;
      }

      if (i + 1 == name.size()) {
        if (value == NullValue.NULL)
          return null;
        else if (value != null)
          return value;
        else
          throw new NameNotFoundException(getFullPath(name));
      }
      
      value = dereference(value, null, model);

      if (value instanceof Context)
        return ((Context) value).lookupLink(name.getSuffix(i + 1));
      else if (value != null)
        throw new NotContextException(L.l("{0}: expected intermediate context at `{1}'",
                                          getFullPath(name), value));
      else
        throw new NameNotFoundException(getFullPath(name));
    }

    return create(getFullPath(name), model, _env);
  }

  /**
   * Binds an object to the context.
   */
  public void bind(String name, Object obj)
    throws NamingException
  {
    String tail = name;
    AbstractModel model = _model;

    if (obj == null)
      obj = NullValue.NULL;
    
    while (true) {
      String first = parseFirst(tail);
      String rest = parseRest(tail);
      
      if (first == null)
        throw new NamingException(L.l("can't bind root"));

      if (rest == null) {
        Object value = model.lookup(first);

        if (value != null)
          throw new NamingException(L.l("`{0}' is already bound to `{1}'",
                                        name, value));

        model.bind(first, getReference(model, obj));

        return;
      }

      Object value = model.lookup(first);

      if (value instanceof AbstractModel) {
        model = (AbstractModel) value;
        tail = rest;
        continue;
      }

      value = dereference(value, null, model);

      if (value instanceof Context) {
        ((Context) value).bind(rest, obj);
        return;
      }
      else if (value != null) {
        throw new NotContextException(L.l("{0}: expected intermediate context at `{1}'",
                                          getFullPath(name), value));
      }
      else {
        throw new NameNotFoundException(getFullPath(name));
      }
    }
  }
  
  /**
   * Binds an object to the context.
   */
  public void bind(Name name, Object obj)
    throws NamingException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest(L.l("JNDI bind `{0}'", name));
    
    if (name.size() == 0)
      throw new NamingException(L.l("can't bind root"));
      
    AbstractModel model = _model;

    int i = 0;
    for (; i + 1 < name.size(); i++) {
      String first = name.get(i);

      Object value = model.lookup(first);

      if (value instanceof AbstractModel) {
        model = (AbstractModel) value;
        continue;
      }

      value = dereference(value, null, model);

      if (value instanceof Context) {
        ((Context) value).bind(name.getSuffix(i + 1), obj);
        return;
      }
      else if (value != null)
        throw new NotContextException(L.l("{0}: expected intermediate context at `{1}'",
                                          getFullPath(name), value));
      else
        throw new NameNotFoundException(getFullPath(name));
    }

    String first = name.get(i);
    
    Object value = model.lookup(first);
    if (value != null)
      throw new NamingException(L.l("`{0}' is already bound to `{1}'",
                                    name, value));

    if (obj == null)
      obj = NullValue.NULL;
    
    model.bind(first, getReference(model, obj));
  }

  /**
   * Binds an object to the context, overriding any old value.
   *
   * @param name the name to bind
   * @param obj the object to bind
   */
  public void rebind(String name, Object obj)
    throws NamingException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest(L.l("JNDI rebind `{0}' value: {1}", name, obj));
    
    String tail = name;
    AbstractModel model = _model;
    
    // env/0gde
    if (obj == null)
      obj = NullValue.NULL;

    while (true) {
      String first = parseFirst(tail);
      String rest = parseRest(tail);

      if (first == null)
        throw new NamingException(L.l("can't bind root"));

      if (rest == null) {
        model.bind(first, getReference(model, obj));
        return;
      }

      Object value = model.lookup(first);

      if (value instanceof AbstractModel) {
        model = (AbstractModel) value;
        tail = rest;
        continue;
      }

      value = dereference(value, null, model);

      if (value instanceof Context) {
        ((Context) value).rebind(rest, obj);
        return;
      }
      else if (value != null)
        throw new NotContextException(L.l("{0}: expected intermediate context at `{1}'",
                                          getFullPath(name), value));
      else
        throw new NameNotFoundException(getFullPath(name));
    }
  }
  
  public void rebind(Name name, Object obj)
    throws NamingException
  {
    if (name.size() == 0)
      throw new NamingException(L.l("can't bind root"));
      
    AbstractModel model = _model;

    int i = 0;
    for (; i + 1 < name.size(); i++) {
      String first = name.get(i);

      Object value = model.lookup(first);

      if (value instanceof AbstractModel) {
        model = (AbstractModel) value;
        continue;
      }

      value = dereference(value, null, model);

      if (value instanceof Context) {
        ((Context) value).bind(name.getSuffix(i + 1), obj);
        return;
      }
      else if (value != null)
        throw new NotContextException(L.l("{0}: expected intermediate context at `{1}'",
                                          getFullPath(name), value));
      else
        throw new NameNotFoundException(getFullPath(name));
    }

    String first = name.get(i);
    
    if (obj == null)
      obj = NullValue.NULL;
    
    model.bind(first, getReference(model, obj));
  }

  private Object getReference(AbstractModel model, Object obj)
  {
    return obj;
  }

  /**
   * Unbinds an object from the context.
   *
   * @param name the name to unbind
   */
  public void unbind(String name)
    throws NamingException
  {
    unbindImpl(name, false);
  }
  
  private void unbindImpl(String name, boolean isRename)
    throws NamingException
  {
    String tail = name;
    AbstractModel model = _model;

    while (true) {
      String first = parseFirst(tail);
      String rest = parseRest(tail);
      
      if (first == null)
        throw new NamingException(L.l("can't unbind root"));

      if (rest == null) {
        if (! isRename && model.lookup(name) instanceof AbstractModel)
          throw new NamingException(L.l("can't unbind subcontext; use destroySubcontext"));
          
        model.unbind(first);
        return;
      }

      Object value = model.lookup(first);

      if (value instanceof AbstractModel) {
        model = (AbstractModel) value;
        tail = rest;
        continue;
      }

      value = dereference(value, null, model);

      if (value instanceof Context) {
        ((Context) value).unbind(rest);
        return;
      }
      else if (value != null)
        throw new NotContextException(L.l("{0}: expected intermediate context at `{1}'",
                                          getFullPath(name), value));
      else
        throw new NameNotFoundException(getFullPath(name));
    }
  }
  
  private Object dereference(Object value, Name tail, AbstractModel model)
    throws NamingException
  {
    try {
      if (value instanceof ObjectProxy)
        return ((ObjectProxy) value).createObject(_env);
      else if (value instanceof Reference) {
        Context context = create(model, _env);
        return NamingManager.getObjectInstance(value, null, context, _env);
      }
      else
        return value;
    } catch (RuntimeException e) {
      throw e;
    } catch (NamingException e) {
      throw e;
    } catch (Exception e) {
      throw new NamingExceptionWrapper(e);
    }
  }
  
  public void unbind(Name name)
    throws NamingException
  {
    if (name.size() == 0)
      throw new NamingException(L.l("can't unbind root"));
      
    AbstractModel model = _model;

    int i = 0;
    for (; i + 1 < name.size(); i++) {
      String first = name.get(i);

      Object value = model.lookup(first);

      if (value instanceof AbstractModel) {
        model = (AbstractModel) value;
        continue;
      }

      value = dereference(value, null, model);

      if (value instanceof Context) {
        ((Context) value).unbind(name.getSuffix(i + 1));
        return;
      }
      else if (value != null)
        throw new NotContextException(L.l("{0}: expected intermediate context at `{1}'",
                                          getFullPath(name), value));
      else
        throw new NameNotFoundException(getFullPath(name));
    }

    String first = name.get(i);
    
    model.unbind(first);
  }

  public void rename(String oldName, String newName)
    throws NamingException
  {
    Object value = lookup(oldName);
    unbindImpl(oldName, true);
    
    if (value instanceof ContextImpl)
      ((ContextImpl) value).setName(newName);
    
    bind(newName, value);
  }
  
  private void setName(String newName)
  {
    _name = newName;
  }

  public void rename(Name oldName, Name newName)
    throws NamingException
  {
    Object value = lookup(oldName);
    unbind(oldName);
    
    if (value instanceof ContextImpl)
      ((ContextImpl) value).setName(newName.toString());
    
    bind(newName, value);
  }

  /**
   * List the names for a context.
   */
  public NamingEnumeration list(String name)
    throws NamingException
  {
    String tail = name;
    AbstractModel model = _model;

    while (true) {
      String first = parseFirst(tail);
      String rest = parseRest(tail);
      
      if (first == null) {
        return new QNameClassEnumeration(create(model, _env),
                                         model.list());
      }

      Object value = model.lookup(first);

      if (value instanceof AbstractModel) {
        model = (AbstractModel) value;
        tail = rest;
        continue;
      }

      value = dereference(value, null, model);

      if (value instanceof Context) {
        if (rest == null)
          return ((Context) value).list("");
        else
          return ((Context) value).list(rest);
      }
      else if (value != null)
        throw new NotContextException(L.l("{0}: expected intermediate context at `{1}'",
                                          getFullPath(name), value));
      else
        throw new NameNotFoundException(getFullPath(name));
    }
  }

  /**
   * Lists the names for the context.
   */
  public NamingEnumeration list(Name name)
    throws NamingException
  {
    AbstractModel model = _model;

    if (name == null) {
      return new QNameClassEnumeration(create(model, _env),
                                       model.list());
    }

    for (int i = 0; i < name.size(); i++) {
      String first = name.get(i);

      Object value = model.lookup(first);

      if (value instanceof AbstractModel) {
        model = (AbstractModel) value;
        continue;
      }

      value = dereference(value, null, model);

      if (value instanceof Context)
        return ((Context) value).list(name.getSuffix(i + 1));
      else if (value != null)
        throw new NotContextException(L.l("{0}: expected intermediate context at `{1}'",
                                          getFullPath(name), value));
      else
        throw new NameNotFoundException(getFullPath(name));
    }
    
    return new QNameClassEnumeration(create(model, _env),
                                     model.list());
  }

  /**
   * List the bindings for a context.
   */
  public NamingEnumeration listBindings(String name)
    throws NamingException
  {
    String tail = name;
    AbstractModel model = _model;

    while (true) {
      String first = parseFirst(tail);
      String rest = parseRest(tail);
      
      if (first == null) {
        return new QBindingEnumeration(create(model, _env),
                                       model.list());
      }

      Object value = model.lookup(first);

      if (value instanceof AbstractModel) {
        model = (AbstractModel) value;
        tail = rest;
        continue;
      }

      value = dereference(value, null, model);

      if (value instanceof Context)
        return ((Context) value).listBindings(rest);
      else if (value != null)
        throw new NotContextException(L.l("{0}: expected intermediate context at `{1}'",
                                          getFullPath(name), value));
      else
        throw new NameNotFoundException(getFullPath(name));
    }
  }

  /**
   * Lists the bindings for the given name.
   */
  public NamingEnumeration listBindings(Name name)
    throws NamingException
  {
    AbstractModel model = _model;

    for (int i = 0; name != null && i < name.size(); i++) {
      String first = name.get(i);

      Object value = model.lookup(first);

      if (value instanceof AbstractModel) {
        model = (AbstractModel) value;
        continue;
      }

      value = dereference(value, null, model);

      if (value instanceof Context)
        return ((Context) value).listBindings(name.getSuffix(i + 1));
      else if (value != null)
        throw new NotContextException(L.l("{0}: expected intermediate context at `{1}'",
                                          getFullPath(name), value));
      else
        throw new NameNotFoundException(getFullPath(name));
    }
    
    return new QBindingEnumeration(create(model, _env),
                                   model.list());
  }

  /**
   * Creates a subcontext for the current model.
   */
  public Context createSubcontext(String name)
    throws NamingException
  {
    String tail = name;
    AbstractModel model = _model;

    while (true) {
      String first = parseFirst(tail);
      String rest = parseRest(tail);

      if (first == null)
        throw new NamingException(L.l("can't create root subcontext"));

      if (rest == null) {
        model = model.createSubcontext(first);
        return create(getFullPath(name), model, _env);
      }

      Object value = model.lookup(first);

      if (value instanceof AbstractModel) {
        model = (AbstractModel) value;
        tail = rest;
        continue;
      }

      value = dereference(value, null, model);

      if (value instanceof Context)
        return ((Context) value).createSubcontext(rest);
      else if (value != null)
        throw new NotContextException(L.l("{0}: expected intermediate context at `{1}'",
                                          getFullPath(name), value));
      else
        throw new NameNotFoundException(getFullPath(name));
    }
  }

  public Context createSubcontext(Name name)
    throws NamingException
  {
    if (name.size() == 0)
      throw new NamingException(L.l("can't create root subcontext"));
      
    AbstractModel model = _model;

    int i = 0;
    for (; i + 1 < name.size(); i++) {
      String first = name.get(i);

      Object value = model.lookup(first);

      if (value instanceof AbstractModel) {
        model = (AbstractModel) value;
        continue;
      }

      value = dereference(value, null, model);

      if (value instanceof Context) {
        return ((Context) value).createSubcontext(name.getSuffix(i + 1));
      }
      else if (value != null)
        throw new NotContextException(L.l("{0}: expected intermediate context at `{1}'",
                                          getFullPath(name), value));
      else
        throw new NameNotFoundException(getFullPath(name));
    }

    String first = name.get(i);
    
    model = model.createSubcontext(first);

    return create(getFullPath(name), model, _env);
  }

  /**
   * Destroys the named subcontext.
   */
  public void destroySubcontext(String name)
    throws NamingException
  {
    String tail = name;
    AbstractModel model = _model;

    while (true) {
      String first = parseFirst(tail);
      String rest = parseRest(tail);
      
      if (first == null)
        throw new NamingException(L.l("can't destroy root subcontext"));

      if (rest == null) {
        model.unbind(first);
        return;
      }

      Object value = model.lookup(first);

      if (value instanceof AbstractModel) {
        model = (AbstractModel) value;
        tail = rest;
        continue;
      }

      value = dereference(value, null, model);

      if (value instanceof Context) {
        ((Context) value).destroySubcontext(rest);
        return;
      }
      else if (value != null)
        throw new NotContextException(L.l("{0}: expected intermediate context at `{1}'",
                                          getFullPath(name), value));
      else
        throw new NameNotFoundException(getFullPath(name));
    }
  }
  
  public void destroySubcontext(Name name)
    throws NamingException
  {
    if (name.size() == 0)
      throw new NamingException(L.l("can't destroy root subcontext"));
      
    AbstractModel model = _model;

    int i = 0;
    for (; i + 1 < name.size(); i++) {
      String first = name.get(i);

      Object value = model.lookup(first);

      if (value instanceof AbstractModel) {
        model = (AbstractModel) value;
        continue;
      }

      value = dereference(value, null, model);

      if (value instanceof Context) {
        ((Context) value).destroySubcontext(name.getSuffix(i + 1));
        return;
      }
      else if (value != null)
        throw new NotContextException(L.l("{0}: expected intermediate context at `{1}'",
                                          getFullPath(name), value));
      else
        throw new NameNotFoundException(getFullPath(name));
    }

    String first = name.get(i);
    
    model.unbind(first);
  }

  public NameParser getNameParser(String name)
    throws NamingException
  {
    String first = parseFirst(name);
    String rest = parseRest(name);
    
    if (first == null)
      return new QNameParser(this);

    Object obj = lookupSingleObject(first);

    if (obj instanceof Context)
      return ((Context) obj).getNameParser(rest == null ? "" : rest);

    else
      return new QNameParser(this);
  }

  public NameParser getNameParser(Name name)
    throws NamingException
  {
    if (name.size() == 0)
      return new QNameParser(this);
      
    Object obj = lookupSingleObject(name.get(0));

    if (obj instanceof Context)
      return ((Context) obj).getNameParser(name.getSuffix(1));
    else
      return new QNameParser(this);
  }

  public String composeName(String suffix, String prefix)
    throws NamingException
  {
    if (suffix == null)
      throw new NamingException(L.l("suffix cannot be null"));
    else if (prefix == null)
      throw new NamingException(L.l("prefix cannot be null"));
    else if (prefix.length() == 0)
      return suffix;
    else
      return prefix + "/" + suffix;
  }

  public Name composeName(Name suffix, Name prefix)
    throws NamingException
  {
    if (suffix == null)
      throw new NamingException(L.l("suffix cannot be null"));
    else if (prefix == null)
      throw new NamingException(L.l("prefix cannot be null"));
    else
      return prefix.addAll(suffix);
  }

  public String getNameInNamespace()
    throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  /**
   * Looks up the object and dereferences any proxy objects.
   */
  private Object lookupSingleObject(String name)
    throws NamingException
  {
    Object obj = lookupSingle(name);

    if (obj instanceof ObjectProxy)
      return ((ObjectProxy) obj).createObject(_env);
    else
      return obj;
  }

  /**
   * Returns the object named by the single name segment.
   *
   * @param name the name segment.
   *
   * @return the object bound to the context.
   */
  protected Object lookupSingle(String name)
    throws NamingException
  {
    throw new UnsupportedOperationException();
  }

  protected void rebindSingle(String name, Object obj)
    throws NamingException
  {
    throw new UnsupportedOperationException();
  }

  protected void unbindSingle(String name)
    throws NamingException
  {
    throw new UnsupportedOperationException();
  }

  protected Context createSingleSubcontext(String name)
    throws NamingException
  {
    throw new UnsupportedOperationException();
  }
  
  protected void destroySingleSubcontext(String name)
    throws NamingException
  {
    unbindSingle(name);
  }

  protected Iterator listSingle()
  {
    throw new UnsupportedOperationException();
  }

  protected String parseFirst(String name)
    throws NamingException
  {
    if (name == null || name.equals(""))
      return null;
    
    int p = name.indexOf(getSeparator());

    if (p == 0)
      return parseFirst(name.substring(1));
    else if (p > 0)
      return name.substring(0, p);
    else
      return name;
  }
    
  protected String parseRest(String name)
    throws NamingException
  {
    if (name == null || name.equals(""))
      return null;
    
    int p = name.indexOf(getSeparator());

    if (p == 0)
      return parseRest(name.substring(1));
    else if (p > 0)
      return name.substring(p + 1);
    else
      return null;
  }

  protected char getSeparator()
  {
    return '/';
  }

  protected String getSeparatorString()
  {
    return "/";
  }

  /**
   * Returns the full name for the context.
   */
  protected String getFullPath(String name)
  {
    if (_name == null || _name.equals(""))
      return name;

    else if (name == null)
      return _name;

    String sep = getSeparatorString();
    
    while (name.endsWith(sep))
      name = name.substring(0, name.length() - sep.length());
        
    if (name.equals(""))
      return _name;
      
    else if (name.startsWith(sep))
      return _name + name;
    else
      return _name + sep + name;
  }

  /**
   * Returns the full name for the context.
   */
  protected String getFullPath(Name name)
  {
    if (_name == null || _name.equals(""))
      return name.toString();

    else if (name == null || name.size() == 0)
      return _name;

    String sep = getSeparatorString();

    return _name + sep + name;
  }

  /**
   * Adds a property to the context environment.
   */
  public Object addToEnvironment(String prop, Object value)
    throws NamingException
  {
    Object old = _env.get(prop);
    _env.put(prop, value);
    return old;
  }

  /**
   * Removes a property from the context environment.
   */
  public Object removeFromEnvironment(String prop)
    throws NamingException
  {
    Object old = _env.get(prop);
    _env.remove(prop);
    return old;
  }

  /**
   * Returns the context environment.
   */
  public Hashtable getEnvironment()
    throws NamingException
  {
    return _env;
  }

  /**
   * Close is intended to free any transient data, like a cached
   * socket.  It does not affect the JNDI tree.
   */
  public void close()
    throws NamingException
  {
  }

  /**
   * Returns a string value.
   */
  public String toString()
  {
    return "ContextImpl[" + _name + "]";
  }
  
  private static class NullValue
  {
    static final NullValue NULL = new NullValue();
  }
}
