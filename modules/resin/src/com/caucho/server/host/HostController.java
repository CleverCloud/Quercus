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

package com.caucho.server.host;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.types.PathBuilder;
import com.caucho.el.EL;
import com.caucho.management.server.HostMXBean;
import com.caucho.server.deploy.DeployController;
import com.caucho.server.deploy.DeployControllerAdmin;
import com.caucho.server.deploy.EnvironmentDeployController;
import com.caucho.server.e_app.EarConfig;
import com.caucho.server.webapp.WebAppConfig;
import com.caucho.util.L10N;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A configuration entry for a host
 */
public class HostController
  extends EnvironmentDeployController<Host,HostConfig>
{
  private static final Logger log
    = Logger.getLogger(HostController.class.getName());
  private static final L10N L = new L10N(HostController.class);
  
  private HostContainer _container;

  // The host name is the canonical name
  private String _hostName;
  // The regexp name is the matching name of the regexp
  private String _regexpName;

  private Pattern _regexp;
  private String _rootDirectoryPattern;

  // Any host aliases.
  private ArrayList<String> _entryHostAliases
    = new ArrayList<String>();
  private ArrayList<Pattern> _entryHostAliasRegexps
    = new ArrayList<Pattern>();

  // includes aliases from the Host, e.g. server/1f35
  private ArrayList<String> _hostAliases = new ArrayList<String>();
  private ArrayList<Pattern> _hostAliasRegexps = new ArrayList<Pattern>();

  // The host variables.
  private final Var _hostVar = new Var();
  private final HostAdmin _admin = new HostAdmin(this);

  private ArrayList<Dependency> _dependList = new ArrayList<Dependency>();

  HostController(String id,
                 HostConfig config,
                 HostContainer container,
                 Map<String,Object> varMap)
  {
    super(id, config);

    setHostName(id);

    if (varMap != null)
      getVariableMap().putAll(varMap);
    
    getVariableMap().put("host", _hostVar);

    setContainer(container);
    
    setRootDirectory(config.calculateRootDirectory(getVariableMap()));
  }

  public HostController(String id,
                        Path rootDirectory,
                        HostContainer container)
  {
    super(id, rootDirectory);

    addHostAlias(id);
    setHostName(id);

    // jsp/101r
    // getVariableMap().put("name", id);
    getVariableMap().put("host", _hostVar);
    
    setContainer(container);
  }

  public void setContainer(HostContainer container)
  {
    _container = container;
    
    if (_container != null) {
      for (HostConfig defaultConfig : _container.getHostDefaultList())
        addConfigDefault(defaultConfig);
    }
  }

  /**
   * Returns the Resin host name.
   */
  public String getName()
  {
    String name = super.getId();
    
    if (name != null)
      return name;
    else
      return getHostName();
  }

  /**
   * Returns the host's canonical name
   */
  public String getHostName()
  {
    if ("".equals(_hostName))
      return "default";
    else
      return _hostName;
  }

  /**
   * Sets the host's canonical name
   */
  public void setHostName(String name)
  {
    if (name != null)
      name = name.trim();
    
    if (name == null || name.equals("*"))
      name = "";
    
    name = name.toLowerCase();

    _hostName = name;
  }

  /**
   * Returns the host's canonical name
   */
  public void setRegexpName(String name)
  {
    _regexpName = name.toLowerCase();
  }
  
  /**
   * Adds a host alias.
   */
  public void addHostAlias(String name)
  {
    if (name != null)
      name = name.trim();
    
    if (name == null || name.equals("*"))
      name = ""; // XXX: default?
    
    name = name.toLowerCase();

    if (! _entryHostAliases.contains(name))
      _entryHostAliases.add(name);

    addExtHostAlias(name);
  }

  /**
   * Adds an extension host alias, e.g. from a resin:import
   */
  public void addExtHostAlias(String name)
  {
    if (! _hostAliases.contains(name))
      _hostAliases.add(name);
  }

  /**
   * Returns the host aliases.
   */
  public ArrayList<String> getHostAliases()
  {
    return _hostAliases;
  }

  /**
   * Adds an extension host alias, e.g. from a resin:import
   */
  public void addExtHostAliasRegexp(Pattern name)
  {
    if (! _hostAliasRegexps.contains(name))
      _hostAliasRegexps.add(name);
  }

  /**
   * Sets the regexp pattern
   */
  public void setRegexp(Pattern regexp)
  {
    _regexp = regexp;
  }

  /**
   * Sets the root directory pattern
   */
  public void setRootDirectoryPattern(String rootDirectoryPattern)
  {
    _rootDirectoryPattern = rootDirectoryPattern;
  }

  /**
   * Adds a dependent file.
   */
  public void addDepend(Path depend)
  {
    if (! _dependList.contains(depend))
      _dependList.add(new Depend(depend));
  }

  /**
   * Returns the host admin.
   */
  public HostMXBean getAdmin()
  {
    return _admin;
  }

  /**
   * Returns the deploy admin.
   */
  protected DeployControllerAdmin getDeployAdmin()
  {
    return _admin;
  }

  /**
   * Initialize the entry.
   */
  protected void initBegin()
  {
    try {
      try {
        if (getConfig() == null || getHostName() != null) {
        }
        else if (getConfig().getHostName() != null)
          setHostName(EL.evalString(getConfig().getHostName(),
                                    EL.getEnvironment()));
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      if (_regexpName != null && _hostName == null)
        _hostName = _regexpName;

      if (_hostName == null)
        _hostName = "";

      ArrayList<String> aliases = null;

      if (getConfig() != null) {
        aliases = getConfig().getHostAliases();

        _entryHostAliasRegexps.addAll(getConfig().getHostAliasRegexps());
        _hostAliasRegexps.addAll(getConfig().getHostAliasRegexps());
      }
      
      for (int i = 0; aliases != null && i < aliases.size(); i++) {
        String alias = aliases.get(i);

        alias = EL.evalString(alias, EL.getEnvironment());

        addHostAlias(alias);
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    super.initBegin();
  }

  /**
   * Returns the "name" property.
   */
  protected String getMBeanId()
  {
    String name = _hostName;
    
    if (name == null)
      name = "";
    else if (name.indexOf(':') >= 0)
      name = name.replace(':', '-');

    if (name.equals(""))
      return "default";
    else
      return name;
  }

  /**
   * Returns true for a matching name.
   */
  public boolean isNameMatch(String name)
  {
    if (name.equals("default"))
      name = "";
    
    if (_hostName.equalsIgnoreCase(name))
      return true;

    for (int i = _hostAliases.size() - 1; i >= 0; i--) {
      if (name.equalsIgnoreCase(_hostAliases.get(i)))
        return true;
    }

    for (int i = _hostAliasRegexps.size() - 1; i >= 0; i--) {
      Pattern alias = _hostAliasRegexps.get(i);

      if (alias.matcher(name).find())
        return true;
    }

    if (_regexp != null) {
      // server/0523
      
      Matcher matcher = _regexp.matcher(name);

      if (matcher.matches()) {
        Path rootDirectory = calculateRoot(matcher);

        if (getRootDirectory().equals(rootDirectory))
          return true;
      }
    }
    
    return false;
  }

  private Path calculateRoot(Matcher matcher)
  {
    // XXX: duplicates HostRegexp

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getParentClassLoader());

      if (_rootDirectoryPattern == null) {
        // server/129p
        return Vfs.lookup();
      }
      
      int length = matcher.end() - matcher.start();

      ArrayList<String> vars = new ArrayList<String>();

      HashMap<String,Object> varMap = new HashMap<String,Object>();
        
      for (int j = 0; j <= matcher.groupCount(); j++) {
        vars.add(matcher.group(j));
        varMap.put("host" + j, matcher.group(j));
      }

      varMap.put("regexp", vars);
      varMap.put("host", new TestVar(matcher.group(0), vars));

      Path path = PathBuilder.lookupPath(_rootDirectoryPattern, varMap);
      
      return path;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      // XXX: not quite right
      return Vfs.lookup(_rootDirectoryPattern);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Merges two entries.
   */
  protected HostController merge(HostController newController)
  {
    if (getConfig() != null && getConfig().getRegexp() != null)
      return newController;
    else if (newController.getConfig() != null
             && newController.getConfig().getRegexp() != null)
      return this;
    else {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        thread.setContextClassLoader(getParentClassLoader());

        HostController mergedController
          = new HostController(newController.getHostName(),
                               getRootDirectory(),
                               _container);

        mergedController.mergeController(this);
        mergedController.mergeController(newController);

        if (! isNameMatch(newController.getHostName())
            && ! newController.isNameMatch(getHostName())) {
          ConfigException e;

          e = new ConfigException(L.l("Illegal merge of {0} and {1}.  Both hosts have the same root-directory '{2}'.",
                                        getHostName(),
                                        newController.getHostName(),
                                        getRootDirectory()));

          log.warning(e.getMessage());
          log.log(Level.FINEST, e.toString(), e);

          mergedController.setConfigException(e);
        }

        return mergedController;
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);

        return null;
      } finally {
        thread.setContextClassLoader(oldLoader);
      }
    }
  }

  /**
   * Merges with the old controller.
   */
  protected void mergeController(DeployController oldControllerV)
  {
    super.mergeController(oldControllerV);

    HostController oldController = (HostController) oldControllerV;
    
    _entryHostAliases.addAll(oldController._entryHostAliases);
    if (! oldController.getHostName().equals(""))
      _entryHostAliases.add(oldController.getHostName());
    _entryHostAliasRegexps.addAll(oldController._entryHostAliasRegexps);
    
    _hostAliases.addAll(oldController._hostAliases);
    _hostAliasRegexps.addAll(oldController._hostAliasRegexps);

    if (_regexp == null) {
      _regexp = oldController._regexp;
      _rootDirectoryPattern = oldController._rootDirectoryPattern;
    }
  }

  /**
   * Creates a new instance of the host object.
   */
  protected Host instantiateDeployInstance()
  {
    return new Host(_container, this, _hostName);
  }

  /**
   * Creates the host.
   */
  protected void configureInstance(Host host)
    throws Throwable
  {
    _hostAliases.clear();
    _hostAliases.addAll(_entryHostAliases);

    InjectManager webBeans = InjectManager.create();
    Config.setProperty("host", _hostVar);

    for (Map.Entry<String,Object> entry : getVariableMap().entrySet()) {
      Object value = entry.getValue();
      
      if (value != null)
        Config.setProperty(entry.getKey(), value);
    }

    if (_container != null) {
      for (EarConfig config : _container.getEarDefaultList())
        host.addEarDefault(config);

      for (WebAppConfig config : _container.getWebAppDefaultList())
        host.addWebAppDefault(config);
    }

    super.configureInstance(host);
  }

  protected void extendJMXContext(Map<String,String> context)
  {
    context.put("Host", getMBeanId());
  }

  /**
   * Returns the appropriate log for debugging.
   */
  protected Logger getLog()
  {
    return log;
  }

  /**
   * Returns equality.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof HostController))
      return false;

    HostController entry = (HostController) o;

    return _hostName.equals(entry._hostName);
  }

  /**
   * Returns a printable view.
   */
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "]";
  }

  /**
   * EL variables for the host.
   */
  public class Var {
    public String getName()
    {
      return HostController.this.getName();
    }
    
    public String getHostName()
    {
      return HostController.this.getHostName();
    }
    
    public String getUrl()
    {
      Host host = getDeployInstance();
      
      if (host != null)
        return host.getURL();
      else if (_hostName.equals(""))
        return "";
      else if (_hostName.startsWith("http:")
               || _hostName.startsWith("https:"))
        return _hostName;
      else
        return "http://" + _hostName;
    }

    public ArrayList getRegexp()
    {
      return (ArrayList) getVariableMap().get("regexp");
    }
    
    public Path getRoot()
    {
      Host host = getDeployInstance();
      
      if (host != null)
        return host.getRootDirectory();
      else
        return HostController.this.getRootDirectory();
    }
    
    /**
     * @deprecated
     */
    public Path getRootDir()
    {
      return getRoot();
    }

    /**
     * @deprecated
     */
    public Path getRootDirectory()
    {
      return getRoot();
    }
    
    public Path getDocumentDirectory()
    {
      Host host = getDeployInstance();
      
      if (host != null)
        return host.getDocumentDirectory();
      else
        return null;
    }
    
    public Path getDocDir()
    {
      return getDocumentDirectory();
    }
    
    public Path getWarDirectory()
    {
      Host host = getDeployInstance();
      
      if (host != null)
        return host.getWarDir();
      else
        return null;
    }
    
    public Path getWarDir()
    {
      return getWarDirectory();
    }
    
    public Path getWarExpandDirectory()
    {
      Host host = getDeployInstance();
      
      if (host != null)
        return host.getWarExpandDir();
      else
        return null;
    }
    
    public Path getWarExpandDir()
    {
      return getWarExpandDirectory();
    }
    
    public String toString()
    {
      return "Host[" + getId() + "]";
    }
  }

  /**
   * EL variables for the host, when testing for regexp identity .
   */
  public class TestVar {
    private String _name;
    private ArrayList<String> _regexp;

    TestVar(String name, ArrayList<String> regexp)
    {
      _name = name;
      _regexp = regexp;
    }
    
    public String getName()
    {
      return _name;
    }
    
    public String getHostName()
    {
      return _name;
    }
    
    public ArrayList<String> getRegexp()
    {
      // server/13t0
      return _regexp;
    }
  }
}
