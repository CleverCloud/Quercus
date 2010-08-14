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

import com.caucho.config.ConfigException;
import com.caucho.config.types.RawString;
import com.caucho.server.deploy.DeployConfig;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * The configuration for a host in the resin.conf
 */
public class HostConfig extends DeployConfig {
  static final L10N L = new L10N(HostConfig.class);
  static final Logger log = Logger.getLogger(HostConfig.class.getName());

  // The raw host aliases
  private ArrayList<String> _hostAliases = new ArrayList<String>();
  
  private ArrayList<Pattern> _hostAliasRegexps
    = new ArrayList<Pattern>();

  private String _hostName;

  // The regexp pattern
  private Pattern _regexp;

  public HostConfig()
  {
    super.setId(null);
  }

  /**
   * Sets the host name.
   */
  public void setHostName(RawString name)
    throws ConfigException
  {
    _hostName = cleanHostName(name);

    if (_hostName.indexOf("${") < 0) {
      for (int i = 0; i < _hostName.length(); i++) {
        char ch = _hostName.charAt(i);

        if (ch == ' ' || ch == '\t' || ch == ',') {
          throw new ConfigException(L.l("Host name `{0}' must not contain multiple names.  Use <host-alias> to specify aliases for a host.",
                                        _hostName));
        }
      }
    }

    if (_hostName.startsWith("xn--")) {
      String domainName = DomainName.fromAscii(_hostName);
      
      if (! _hostAliases.contains(domainName))
        _hostAliases.add(domainName);
    }
  }
  
  /**
   * Gets the host name.
   */
  public String getHostName()
  {
    return _hostName;
  }

  /**
   * Sets the id.
   */
  public void setId(RawString id)
    throws ConfigException
  {
    String cleanName = cleanHostName(id);
    
    setId(cleanName);
  }

  public void setId(String cleanName)
  {
    super.setId(cleanName);

    if (_hostName == null)
      _hostName = cleanName;

    if (cleanName.startsWith("xn--")) {
      String name = DomainName.fromAscii(cleanName);
      
      if (! _hostAliases.contains(name))
        _hostAliases.add(name);
    }
  }

  /**
   * Sets the host name.
   */
  private String cleanHostName(RawString name)
    throws ConfigException
  {
    String hostName = name.getValue();

    if (hostName.indexOf("${") < 0) {
      for (int i = 0; i < hostName.length(); i++) {
        char ch = hostName.charAt(i);

        if (ch == ' ' || ch == '\t' || ch == ',') {
          throw new ConfigException(L.l("Host name `{0}' must not contain multiple names.  Use <host-alias> to specify aliases for a host.",
                                        hostName));
        }
      }
    }

    return hostName;
  }

  /**
   * Adds a host alias.
   */
  public void addHostAlias(RawString rawName)
    throws ConfigException
  {
    String name = rawName.getValue().trim();

    if (name.indexOf("${") < 0) {
      for (int i = 0; i < name.length(); i++) {
        char ch = name.charAt(i);

        if (ch == ' ' || ch == '\t' || ch == ',') {
          throw new ConfigException(L.l("<host-alias> `{0}' must not contain multiple names.  Use multiple <host-alias> tags to specify aliases for a host.",
                                        name));
        }
      }
    }

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
   * Adds a host alias regexp.
   */
  public void addHostAliasRegexp(String name)
  {
    name = name.trim();

    Pattern pattern = Pattern.compile(name, Pattern.CASE_INSENSITIVE);

    if (! _hostAliasRegexps.contains(pattern))
      _hostAliasRegexps.add(pattern);
  }

  /**
   * Returns the host aliases regexps.
   */
  public ArrayList<Pattern> getHostAliasRegexps()
  {
    return _hostAliasRegexps;
  }

  /**
   * Sets the regexp.
   */
  public void setRegexp(RawString regexp)
  {
    String value = regexp.getValue();

    if (! value.endsWith("$"))
      value = value + "$";
    
    _regexp = Pattern.compile(value, Pattern.CASE_INSENSITIVE);
  }

  /**
   * Gets the regexp.
   */
  public Pattern getRegexp()
  {
    return _regexp;
  }

  /**
   * Sets the root-dir (obsolete).
   */
  public void setRootDir(RawString rootDir)
  {
    setRootDirectory(rootDir);
  }

  /**
   * Sets the lazy-init property
   */
  public void setLazyInit(boolean lazyInit)
    throws ConfigException
  {
    if (lazyInit)
      setStartupMode("lazy");
    else
      setStartupMode("automatic");
  }

  /**
   * Initialize the config.
   */
  @PostConstruct
  public void init()
  {
    if (_regexp != null && getHostName() == null)
      log.config(L.l("<host regexp=\"{0}\"> should include a <host-name> tag.",
                     _regexp.pattern()));
  }
}
