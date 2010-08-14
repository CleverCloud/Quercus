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

package com.caucho.server.webapp;

import com.caucho.VersionFactory;
import com.caucho.config.Configurable;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bare-bones servlet context implementation.
 */
public class ServletContextImpl extends ServletContextCompat
  implements ServletContext
{
  static final Logger log
    = Logger.getLogger(ServletContextImpl.class.getName());
  static final L10N L = new L10N(ServletContextImpl.class);

  private String _name;
  
  private HashMap<String,Object> _attributes = new HashMap<String,Object>();

  private ArrayList<ServletContextAttributeListener>
    _applicationAttributeListeners;

  private HashMap<String,String> _initParams = new HashMap<String,String>();

  public Path getRootDirectory()
  {
    return Vfs.lookup();
  }

  /**
   * Sets the servlet context name
   */
  public void setDisplayName(String name)
  {
    _name = name;
  }

  /**
   * Gets the servlet context name
   */
  public String getServletContextName()
  {
    return _name;
  }

  /**
   * Gets the servlet context name
   */
  public String getContextPath()
  {
    return _name;
  }

  /**
   * Adds the listener.
   */
  protected void addAttributeListener(ServletContextAttributeListener listener)
  {
    if (_applicationAttributeListeners == null)
      _applicationAttributeListeners = new ArrayList<ServletContextAttributeListener>();
    
    _applicationAttributeListeners.add(listener);
  }

  /**
   * Returns the server information
   */
  public String getServerInfo()
  {
    return "Resin/" + VersionFactory.getVersion();
  }

  /**
   * Returns the servlet major version
   */
  public int getMajorVersion()
  {
    return 3;
  }

  public int getEffectiveMajorVersion()
  {
    return 3;
  }

  /**
   * Returns the servlet minor version
   */
  public int getMinorVersion()
  {
    return 0;
  }

  public int getEffectiveMinorVersion()
  {
    return 0;
  }

  /**
   * Sets an init param
   */
  public boolean setInitParameter(String name, String value)
  {

    if (isActive())
      throw new IllegalStateException(L.l("setInitParameter must be called before the web-app has been initialized, because it's required by the servlet spec."));

    // server/1h12
    if (_initParams.containsKey(name))
      return false;

    _initParams.put(name, value);

    return true;
  }

  /**
   * Sets an init param
   */
  protected void setInitParam(String name, String value)
  {
    _initParams.put(name, value);
  }

  /**
   * Gets the init params
   */
  public String getInitParameter(String name)
  {
    return _initParams.get(name);
  }

  /**
   * Gets the init params
   */
  public Enumeration<String> getInitParameterNames()
  {
    return Collections.enumeration(_initParams.keySet());
  }

  /**
   * Returns the named attribute.
   */
  public Object getAttribute(String name)
  {
    synchronized (_attributes) {
      Object value = _attributes.get(name);

      return value;
    }
  }

  /**
   * Returns an enumeration of the attribute names.
   */
  public Enumeration<String> getAttributeNames()
  {
    synchronized (_attributes) {
      return Collections.enumeration(_attributes.keySet());
    }
  }

  /**
   * Sets an application attribute.
   *
   * @param name the name of the attribute
   * @param value the value of the attribute
   */
  public void setAttribute(String name, Object value)
  {
    Object oldValue;
    
    synchronized (_attributes) {
      if (value != null)
        oldValue = _attributes.put(name, value);
      else
        oldValue = _attributes.remove(name);
    }
    
    // Call any listeners
    if (_applicationAttributeListeners != null) {
      ServletContextAttributeEvent event;

      if (oldValue != null)
        event = new ServletContextAttributeEvent(this, name, oldValue);
      else
        event = new ServletContextAttributeEvent(this, name, value);
        
      for (int i = 0; i < _applicationAttributeListeners.size(); i++) {
        ServletContextAttributeListener listener;

        Object objListener = _applicationAttributeListeners.get(i);
        listener = (ServletContextAttributeListener) objListener;

        try {
          if (oldValue != null)
            listener.attributeReplaced(event);
          else
            listener.attributeAdded(event);
        } catch (Exception e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }
  }

  /**
   * Removes an attribute from the servlet context.
   *
   * @param name the name of the attribute to remove.
   */
  public void removeAttribute(String name)
  {
    Object oldValue;
    
    synchronized (_attributes) {
      oldValue = _attributes.remove(name);
    }

    // Call any listeners
    if (_applicationAttributeListeners != null) {
      ServletContextAttributeEvent event;

      event = new ServletContextAttributeEvent(this, name, oldValue);
        
      for (int i = 0; i < _applicationAttributeListeners.size(); i++) {
        ServletContextAttributeListener listener;

        Object objListener = _applicationAttributeListeners.get(i);
        listener = (ServletContextAttributeListener) objListener;

        try {
          listener.attributeRemoved(event);
        } catch (Throwable e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }
  }

  /**
   * Maps from a URI to a real path.
   */
  public String getRealPath(String uri)
  {
    return getRootDirectory().lookup("./" + uri).getNativePath();
  }

  /**
   * Returns a resource for the given uri.
   *
   * <p>XXX: jdk 1.1.x doesn't appear to allow creation of private
   * URL streams.
   */
  public URL getResource(String name)
    throws java.net.MalformedURLException
  {
    if (! name.startsWith("/"))
      throw new java.net.MalformedURLException(name);
    
    String realPath = getRealPath(name);

    Path rootDirectory = getRootDirectory();
    Path path = rootDirectory.lookupNative(realPath);

    if (path.exists())
      return new URL(path.getURL());

    return null;
  }
  
  public Path getCauchoPath(String name)
  {
    String realPath = getRealPath(name);

    Path rootDirectory = getRootDirectory();
    Path path = rootDirectory.lookupNative(realPath);

    return path;
  }

  /**
   * Returns the resource for a uripath as an input stream.
   */
  public InputStream getResourceAsStream(String uripath)
  {
    Path rootDirectory = getRootDirectory();
    Path path = rootDirectory.lookupNative(getRealPath(uripath));

    try {
      if (path.canRead())
        return path.openRead();
      else
        return null;
    } catch (IOException e) {
      log.log(Level.FINEST, e.toString(), e);

      return null;
    }
  }

  /**
    * Returns an enumeration of all the resources.
    */
  public Set getResourcePaths(String prefix)
  {
    if (! prefix.endsWith("/"))
      prefix = prefix + "/";

    Path path = getRootDirectory().lookup(getRealPath(prefix));

    HashSet<String> set = new HashSet<String>();

    try {
      String []list = path.list();

      for (int i = 0; i < list.length; i++) {
        if (path.lookup(list[i]).isDirectory())
          set.add(prefix + list[i] + '/');
        else
          set.add(prefix + list[i]);
      }
    } catch (IOException e) {
    }
     
    return set;
  }

  /**
   * Returns the servlet context for the name.
   */
  public ServletContext getContext(String uri)
  {
    return this;
  }

  /**
   * Returns the mime type for the name.
   */
  public String getMimeType(String uri)
  {
    return null;
  }

  /**
   * Returns the dispatcher.
   */
  public RequestDispatcher getRequestDispatcher(String uri)
  {
    return null;
  }

  /**
   * Returns a dispatcher for the named servlet.
   */
  public RequestDispatcher getNamedDispatcher(String servletName)
  {
    return null;
  }

  /**
   * Logging.
   */

  /**
   * Logs a message to the error file.
   *
   * @param msg the message to log
   */
  public final void log(String message) 
  {
    log(message, null);
  }

  /**
   * @deprecated
   */
  public final void log(Exception e, String msg)
  {
    log(msg, e);
  }

  /**
   * Error logging
   *
   * @param message message to log
   * @param e stack trace of the error
   */
  public void log(String message, Throwable e)
  {
    if (e != null)
      log.log(Level.WARNING, message, e);
    else
      log.info(message);
  }

  //
  // Deprecated methods
  //

  public Servlet getServlet(String name)
  {
    throw new UnsupportedOperationException("getServlet is deprecated");
  }

  public Enumeration<String> getServletNames()
  {
    throw new UnsupportedOperationException("getServletNames is deprecated");
  }

  public Enumeration<Servlet> getServlets()
  {
    throw new UnsupportedOperationException("getServlets is deprecated");
  }

  public void setSessionTrackingModes(Set<SessionTrackingMode> modes)
  {
    throw new UnsupportedOperationException("unimplemented");
  }

  public ServletRegistration.Dynamic addServlet(String servletName,
                                                String className)
  {
    throw new UnsupportedOperationException("unimplemented");
  }

  public ServletRegistration.Dynamic addServlet(String servletName,
                                                Servlet servlet)
  {
    throw new UnsupportedOperationException("unimplemented");
  }

  public ServletRegistration.Dynamic addServlet(String servletName,
                                                Class<? extends Servlet> servletClass)
  {
    throw new UnsupportedOperationException("unimplemented");
  }

  public <T extends Servlet> T createServlet(Class<T> c)
    throws ServletException
  {
    throw new UnsupportedOperationException("unimplemented");
  }

  public ServletRegistration getServletRegistration(String servletName)
  {
    throw new UnsupportedOperationException("unimplemented");
  }

  public Map<String, ServletRegistration> getServletRegistrations()
  {
    throw new UnsupportedOperationException("unimplemented");
  }

  public FilterRegistration.Dynamic addFilter(String filterName,
                                              String className)
  {
    throw new UnsupportedOperationException("unimplemented");
  }

  public FilterRegistration.Dynamic addFilter(String filterName, Filter filter)
  {
    throw new UnsupportedOperationException("unimplemented");
  }

  public FilterRegistration.Dynamic addFilter(String filterName,
                                              Class<? extends Filter> filterClass)
  {
    throw new UnsupportedOperationException("unimplemented");
  }

  public <T extends Filter> T createFilter(Class<T> c)
    throws ServletException
  {
    throw new UnsupportedOperationException("unimplemented");
  }

  public FilterRegistration getFilterRegistration(String filterName)
  {
    throw new UnsupportedOperationException("unimplemented");
  }
  
  /**
   * Returns filter registrations
   * @return
   */
  @Override
  public Map<String, ? extends FilterRegistration> getFilterRegistrations()
  {
    throw new UnsupportedOperationException("unimplemented");
  }
  
  @Configurable
  public void addListener(ListenerConfig config)
    throws Exception
  {
    
  }

  public void addListener(String className)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public <T extends EventListener> void addListener(T t)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void addListener(Class<? extends EventListener> listenerClass)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public JspConfigDescriptor getJspConfigDescriptor()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public ClassLoader getClassLoader()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void declareRoles(String... roleNames)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  protected boolean isActive()
  {
    throw new UnsupportedOperationException("unimplemented");
  }

  /* (non-Javadoc)
   * @see javax.servlet.ServletContext#createListener(java.lang.Class)
   */
  @Override
  public <T extends EventListener> T createListener(Class<T> listenerClass)
    throws ServletException 
  {
    // TODO Auto-generated method stub
    return null;
  }
}
