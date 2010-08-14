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

package com.caucho.server.dispatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import javax.el.ELContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import com.caucho.config.ConfigELContext;
import com.caucho.config.ConfigException;
import com.caucho.config.program.ContainerProgram;
import com.caucho.el.EL;
import com.caucho.el.MapVariableResolver;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;

/**
 * Configuration for a servlet.
 */
public class ServletMapping extends ServletConfigImpl {
  private static final L10N L = new L10N(ServletMapping.class);

  private ArrayList<Mapping> _mappingList
    = new ArrayList<Mapping>();

  private boolean _isStrictMapping;
  private boolean _ifAbsent;

  /**
   * Creates a new servlet mapping object.
   */
  public ServletMapping()
  {
  }

  public void setIfAbsent(boolean ifAbsent)
  {
    _ifAbsent = ifAbsent;
  }

  /**
   * Sets the url pattern
   */
  public void addURLPattern(String pattern)
  {
    if (pattern.indexOf('\n') > -1) {
      throw new ConfigException(L.l("'url-pattern' cannot contain newline"));
    }

    _mappingList.add(new Mapping(pattern, null));

    // server/13f4
    if (getServletNameDefault() == null)
      setServletNameDefault(pattern);
  }

  /**
   * Sets the url regexp
   */
  public void addURLRegexp(String pattern)
  {
    _mappingList.add(new Mapping(null, pattern));
  }

  /**
   * True if strict mapping should be enabled.
   */
  public boolean isStrictMapping()
  {
    return _isStrictMapping;
  }

  /**
   * Set if strict mapping should be enabled.
   */
  public void setStrictMapping(boolean isStrictMapping)
  {
    _isStrictMapping = isStrictMapping;
  }

  /**
   * initialize.
   */
  public void init(ServletMapper mapper)
    throws ServletException
  {
    boolean hasInit = false;

    if (getServletName() == null)
      setServletName(getServletNameDefault());

    for (int i = 0; i < _mappingList.size(); i++) {
      Mapping mapping = _mappingList.get(i);

      String urlPattern = mapping.getUrlPattern();
      String urlRegexp = mapping.getUrlRegexp();

      if (getServletName() == null
          && getServletClassName() != null
          && urlPattern != null) {
        setServletName(urlPattern);
      }

      if (urlPattern != null && ! hasInit) {
        hasInit = true;
        super.init();

        if (getServletClassName() != null)
          mapper.getServletManager().addServlet(this);
      }

      if (urlPattern != null)
        mapper.addUrlMapping(urlPattern, getServletName(), this, _ifAbsent);
      else
        mapper.addUrlRegexp(urlRegexp, getServletName(), this);
    }

    /*
    if (_urlRegexp == null) {
      if (getServletName() == null && getServletClassName() != null) {
        // server/13f4
      }

    }
    */
  }

  /**
   * Initialize for a regexp.
   */
  String initRegexp(ServletContext webApp,
                    ServletManager manager,
                    ArrayList<String> vars)
    throws ServletException
  {
    HashMap<String,Object> map = new HashMap<String,Object>();
    map.put("regexp", vars);

    ELContext mapEnv = new ConfigELContext(new MapVariableResolver(map));

    String rawName = getServletName();
    String rawClassName = getServletClassName();

    if (rawName == null)
      rawName = rawClassName;

    if (rawClassName == null)
      rawClassName = rawName;

    try {
      String servletName = EL.evalString(rawName, mapEnv);

      if (manager.getServletConfig(servletName) != null)
        return servletName;

      String className = EL.evalString(rawClassName, mapEnv);

      try {
        WebApp app = (WebApp) getServletContext();

        Class.forName(className, false, app.getClassLoader());
      } catch (ClassNotFoundException e) {
        log.log(Level.WARNING, e.toString(), e);

        return null;
      }

      ServletConfigImpl config = new ServletConfigImpl();

      config.setServletName(servletName);
      config.setServletClass(className);
      config.setServletContext(webApp);

      ContainerProgram program = getInit();

      if (program != null)
        config.setInit(program);

      config.init();

      manager.addServlet(config);

      return servletName;
    } catch (RuntimeException e) {
      throw e;
    } catch (ServletException e) {
      throw e;
    } catch (Throwable e) {
      throw new ServletException(e);
    }
  }

  /**
   * Returns a printable representation of the servlet config object.
   */
  public String toString()
  {
    StringBuilder builder = new StringBuilder();

    builder.append("ServletMapping[");

    for (int i = 0; i < _mappingList.size(); i++) {
      Mapping mapping = _mappingList.get(i);

      if (mapping.getUrlPattern() != null) {
        builder.append("url-pattern=");
        builder.append(mapping.getUrlPattern());
        builder.append(", ");
      }
      else if (mapping.getUrlRegexp() != null) {
        builder.append("url-regexp=");
        builder.append(mapping.getUrlRegexp());
        builder.append(", ");
      }
    }

    builder.append("name=");
    builder.append(getServletName());

    if (getServletClass() != null) {
      builder.append(", class=");
      builder.append(getServletClass().getName());

    }

    builder.append("]");

    return builder.toString();
  }

  static class Mapping {
    private final String _urlPattern;
    private final String _urlRegexp;

    Mapping(String urlPattern, String urlRegexp)
    {
      _urlPattern = urlPattern;
      _urlRegexp = urlRegexp;
    }

    String getUrlPattern()
    {
      return _urlPattern;
    }

    String getUrlRegexp()
    {
      return _urlRegexp;
    }

    public String toString()
    {
      return "ServletMapping[" + _urlPattern + ", " + _urlRegexp + "]";
    }
  }
}
