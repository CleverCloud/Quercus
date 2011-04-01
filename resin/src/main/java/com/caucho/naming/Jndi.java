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

import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.L10N;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingException;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static utility functions.
 */
public class Jndi {
  private static Logger log
    = Logger.getLogger(Jndi.class.getName());
  private static L10N L = new L10N(Jndi.class);

  private static final EnvironmentLocal<Context> _javaCompEnvMap
    = new EnvironmentLocal<Context>();

  private Jndi() {}

  /**
   * Returns the full name.
   */
  public static String getFullName(String shortName)
  {
    if (shortName.startsWith("java:"))
      return shortName;
    else
      return "java:comp/env/" + shortName;
  }

  private static Context getShortContext(String shortName)
    throws NamingException
  {
    if (shortName.startsWith("java:"))
      return getInitialContext();
    else {
      Context context = (Context) _javaCompEnvMap.getLevel();

      if (context == null) {
        context = (Context) new InitialContext().lookup("java:comp/env");
        _javaCompEnvMap.set(context);
      }

      return context;
    }
  }
  
  public static void bindDeepShort(String name, Object obj)
    throws NamingException
  {
    String fullName = getFullName(name);
    Context context = getShortContext(name);
      
    bindImpl(context, name, obj, fullName);
  }

  public static void bindDeepShort(Context context, String name, Object obj)
    throws NamingException
  {
    bindImpl(context, getFullName(name), obj, name);
  }

  public static void bindDeep(String name, Object obj)
    throws NamingException
  {
    bindImpl(getInitialContext(), name, obj, name);
  }

  public static void bindDeep(Context context, String name, Object obj)
    throws NamingException
  {
    bindImpl(context, name, obj, name);
  }
  
  private static Context getInitialContext()
    throws NamingException
  {
    // return new InitialContext();
    return InitialContextFactoryImpl.createInitialContext();
  }

  private static void bindImpl(Context context, String name,
                               Object obj, String fullName)
    throws NamingException
  {
    NameParser parser = context.getNameParser("");
    Name parsedName = parser.parse(name);

    if (parsedName.size() == 1) {
      Object value = null;

      try {
        if (context instanceof ContextImpl)
          value = ((ContextImpl) context).lookupLink(name);
        else
          value = context.lookupLink(name);
      } catch (NameNotFoundException e) {
      }
      
      context.rebind(name, obj);

      // server/1620
      if (value != null && value != obj)
        log.config(L.l("'{0}' overrides a previous JNDI resource.  The old resource is '{1}' and the new one is '{2}'",
                        fullName, value, obj));

      return;
    }

    Object sub = null;

    try {
      if (context instanceof ContextImpl)
        sub = ((ContextImpl) context).lookupImpl(parsedName.get(0));
      else
        sub = context.lookup(parsedName.get(0));
    } catch (NameNotFoundException e) {
    }

    if (sub == null)
      sub = context.createSubcontext(parsedName.get(0));
      
    if (sub instanceof Context)
      bindImpl((Context) sub, parsedName.getSuffix(1).toString(), obj, fullName);

    else
      throw new NamingException(L.l("'{0}' is an invalid JNDI name because '{1} is not a Context.  One of the subcontexts is not a Context as expected.",
                                    fullName, sub));
  }

  /**
   * Binds the object into JNDI without warnings if an old
   * object exists.  The name may be a full name or the short
   * form.
   */
  public static void rebindDeepShort(String name, Object obj)
    throws NamingException
  {
    rebindImpl(new InitialContext(), getFullName(name), obj, name);
  }

  /**
   * Binds the object into JNDI without warnings if an old
   * object exists.  The name may be a full name or the short
   * form.
   */
  public static void rebindDeepShort(Context context, String name, Object obj)
    throws NamingException
  {
    rebindImpl(context, getFullName(name), obj, name);
  }

  /**
   * Binds the object into JNDI without warnings if an old
   * object exists, using the full JNDI name.
   */
  public static void rebindDeep(String name, Object obj)
    throws NamingException
  {
    rebindImpl(new InitialContext(), name, obj, name);
  }

  /**
   * Binds the object into JNDI without warnings if an old
   * object exists, using the full JNDI name.
   */
  public static void rebindDeep(Context context, String name, Object obj)
    throws NamingException
  {
    rebindImpl(context, name, obj, name);
  }

  /**
   * Binds the object into JNDI without warnings if an old
   * object exists.
   */
  private static void rebindImpl(Context context, String name,
                                 Object obj, String fullName)
    throws NamingException
  {
    NameParser parser = context.getNameParser("");
    Name parsedName = parser.parse(name);

    if (parsedName.size() == 1) {
      context.rebind(name, obj);
      return;
    }

    Object sub = null;

    try {
      sub = context.lookup(parsedName.get(0));
    } catch (NameNotFoundException e) {
      log.log(Level.FINEST, e.toString(), e);
    }

    if (sub == null)
      sub = context.createSubcontext(parsedName.get(0));
      
    if (sub instanceof Context)
      rebindImpl((Context) sub, parsedName.getSuffix(1).toString(), obj,
                 fullName);

    else
      throw new NamingException(L.l("'{0}' is an invalid JNDI name because '{1} is not a Context.  One of the subcontexts is not a Context as expected.",
                                    fullName, sub));
  }

  // For EL
  public static Object lookup(String name)
  {
    Exception ex = null;

    try {
      Object value = new InitialContext().lookup(name);
        
      if (value != null)
        return value;
    } catch (NamingException e) {
      ex = e;
    }

    if (! name.startsWith("java:")) {
      try {
        Object value = new InitialContext().lookup("java:comp/env/" + name);
        
        if (value != null)
          return value;
      } catch (NamingException e) {
        ex = e;
      }
    }

    if (ex != null && log.isLoggable(Level.FINEST))
      log.log(Level.FINEST, ex.toString(), ex);

    return null;
  }
}

