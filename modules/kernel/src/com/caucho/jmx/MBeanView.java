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

package com.caucho.jmx;

import com.caucho.util.L10N;
import com.caucho.loader.*;

import javax.management.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * The view for administration.
 */
public class MBeanView {
  private static final Logger log
    = Logger.getLogger(MBeanView.class.getName());
  private static final L10N L = new L10N(MBeanView.class);


  private ClassLoader _classLoader;

  private AbstractMBeanServer _mbeanServer;

  private MBeanServerDelegateImpl _delegate;
  
  private HashMap<ObjectName,MBeanWrapper> _mbeans
    = new HashMap<ObjectName,MBeanWrapper>();

  MBeanView(AbstractMBeanServer mbeanServer,
            ClassLoader loader,
            String agentId)
  {
    for (; loader != null
           && loader != ClassLoader.getSystemClassLoader()
           && ! (loader instanceof EnvironmentClassLoader);
         loader = loader.getParent()) {
    }

    if (loader == null)
      loader = ClassLoader.getSystemClassLoader();
    
    _mbeanServer = mbeanServer;

    _classLoader = loader;
    _delegate = new MBeanServerDelegateImpl(agentId);
  }

  /**
   * Returns the class loader.
   */
  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }

  /**
   * Returns the parent local view.
   */
  protected MBeanView getParentView()
  {
    if (_classLoader == null)
      return null;

    MBeanContext context = _mbeanServer.createContext(_classLoader.getParent());

    if (context.getView() == this)
      return null;
    else
      return context.getView();

    /*
    if (_classLoader != null)
      return Jmx.getLocalView(_classLoader.getParent());
    else
      return null;
    */
  }

  /**
   * Returns the parent global view.
   */
  protected MBeanView getParentGlobalView()
  {
    return null;
    /*
    if (_classLoader != null)
      return Jmx.getLocalView(_classLoader.getParent());
    else
      return null;
    */
  }

  /**
   * Returns the mbean count.
   */
  public int getMBeanCount()
  {
    MBeanView parentView = getParentView();

    if (parentView != null)
      return _mbeans.size() + parentView.getMBeanCount();
    else
      return _mbeans.size();
  }

  /**
   * Returns the mbean domains.
   */
  public String []getDomains()
  {
    ArrayList<String> domains = new ArrayList<String>();

    getDomains(domains);
    
    return domains.toArray(new String[domains.size()]);
  }

  /**
   * Returns the mbean domains.
   */
  protected void getDomains(ArrayList<String> domains)
  {
    synchronized (_mbeans) {
      Iterator<ObjectName> names = _mbeans.keySet().iterator();
      while (names.hasNext()) {
        ObjectName name = names.next();

        String domain = name.getDomain();

        if (! domains.contains(domain))
          domains.add(domain);
      }
    }

    MBeanView parent = getParentView();

    if (parent != null)
      parent.getDomains(domains);
  }

  /**
   * Finds names matching the query.
   */
  public Set<ObjectName> queryNames(ObjectName queryName, QueryExp query)
    throws BadStringOperationException,
           BadBinaryOpValueExpException,
           BadAttributeValueExpException,
           InvalidApplicationException
  {
    // TreeSet would be better but it causes jconsole to fail
    HashSet<ObjectName> set = new HashSet<ObjectName>();

    queryNames(set, queryName, query);
    
    return set;
  }

  /**
   * Finds names matching the query.
   */
  protected void queryNames(Set<ObjectName> set,
                            ObjectName queryName,
                            QueryExp query)
    throws BadStringOperationException,
           BadBinaryOpValueExpException,
           BadAttributeValueExpException,
           InvalidApplicationException
  {
    synchronized (_mbeans) {
      Iterator<ObjectName> iter = _mbeans.keySet().iterator();

      while (iter.hasNext()) {
        ObjectName name = iter.next();

        if (isMatch(name, queryName, query)) {
          set.add(name);
        }
      }
    }

    MBeanView parentView = getParentView();

    if (parentView != null)
      parentView.queryNames(set, queryName, query);
  }

  /**
   * Finds names matching the query.
   */
  public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query)
    throws BadStringOperationException,
           BadBinaryOpValueExpException,
           BadAttributeValueExpException,
           InvalidApplicationException
  {
    HashSet<ObjectInstance> set = new HashSet<ObjectInstance>();

    queryMBeans(set, name, query);

    return set;
  }

  /**
   * Finds names matching the query.
   */
  protected void queryMBeans(Set<ObjectInstance> set,
                             ObjectName name,
                             QueryExp query)
    throws BadStringOperationException,
           BadBinaryOpValueExpException,
           BadAttributeValueExpException,
           InvalidApplicationException
  {
    synchronized (_mbeans) {
      Iterator<ObjectName> iter = _mbeans.keySet().iterator();

      while (iter.hasNext()) {
        ObjectName testName = iter.next();

        if (isMatch(testName, name, query)) {
          MBeanWrapper mbean = _mbeans.get(testName);

          if (mbean != null)
            set.add(mbean.getObjectInstance());
        }
      }
    }

    MBeanView parentView = getParentView();

    if (parentView != null)
      parentView.queryMBeans(set, name, query);
  }

  /**
   * Tests if the name matches.
   *
   * @param name the object name to match
   * @param queryName the name of the query pattern
   */
  private boolean isMatch(ObjectName name,
                          ObjectName queryName,
                          QueryExp query)
    throws BadStringOperationException,
           BadBinaryOpValueExpException,
           BadAttributeValueExpException,
           InvalidApplicationException
  {
    if (queryName == null)
      return true;

    if (! queryName.isDomainPattern() &&
        ! name.getDomain().equals(queryName.getDomain()))
      return false;

    if (queryName.isPropertyPattern()) {
      // If the queryName has a '*' in the properties, then check
      // the queryName properties to see if they match
      
      Hashtable<String,String> map = queryName.getKeyPropertyList();
      Iterator<String> iter = map.keySet().iterator();
      while (iter.hasNext()) {
        String key = iter.next();
        String value = map.get(key);

        if (! value.equals(name.getKeyProperty(key)))
          return false;
      }
    }
    else {
      String testProps = name.getCanonicalKeyPropertyListString();
      String queryProps = queryName.getCanonicalKeyPropertyListString();
    
      if (! testProps.equals(queryProps))
        return false;
    }

    if (query != null && ! query.apply(name))
      return false;

    return true;
  }

  /**
   * Adds an mbean instance to the view.
   */
  boolean add(ObjectName name, MBeanWrapper mbean)
  {
    return add(name, mbean, false);
  }

  /**
   * Adds an mbean instance to the view.
   */
  boolean add(ObjectName name, MBeanWrapper mbean, boolean overwrite)
  {
    synchronized (_mbeans) {
      if (overwrite || _mbeans.get(name) == null) {
        _mbeans.put(name, mbean);

        return true;
      }
      else
        return false;
    }
  }

  /**
   * Removes an mbean instance from the view.
   */
  MBeanWrapper remove(ObjectName name)
  {
    synchronized (_mbeans) {
      return _mbeans.remove(name);
    }
  }

  /**
   * Removes an mbean instance from the view.
   */
  MBeanWrapper remove(ObjectName name, MBeanWrapper mbean)
  {
    synchronized (_mbeans) {
      if (mbean != null && _mbeans.get(name) != mbean)
        return null;
    
      return _mbeans.remove(name);
    }
  }
  
  /**
   * Returns the object.
   */
  public MBeanWrapper getMBean(ObjectName name)
  {
    synchronized (_mbeans) {
      MBeanWrapper mbean = _mbeans.get(name);
      if (mbean != null)
        return mbean;

      if (_classLoader == null)
        return null;
    }

    MBeanView parentView = getParentView();

    if (parentView != null)
      return parentView.getMBean(name);
    else
      return null;
  }

  /**
   * Closes the view.
   */
  void close()
  {
    _mbeans = null;
  }

  public String toString()
  {
    return "MBeanView[" + _classLoader + "]";
  }

}

