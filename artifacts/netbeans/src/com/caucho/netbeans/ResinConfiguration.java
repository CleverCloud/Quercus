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
 * @author Sam
 */

package com.caucho.netbeans;

import com.caucho.netbeans.PluginL10N;
import com.caucho.netbeans.PluginLogger;

import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.modules.j2ee.deployment.plugins.api.InstanceCreationException;
import org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URL;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;

public class ResinConfiguration
  implements Cloneable
{
  private static final PluginL10N L = new PluginL10N(ResinConfiguration.class);
  private static final Logger log
    = Logger.getLogger(ResinConfiguration.class.getName());


  private static final String PROPERTY_AUTOLOAD_ENABLED = "autoload_enabled";
  private static final String PROPERTY_DEBUG_PORT = "debugger_port";
  private static final String PROPERTY_DISPLAY_NAME = InstanceProperties.DISPLAY_NAME_ATTR;
  private static final String PROPERTY_JAVA_PLATFORM = "java_platform";
  private static final String PROPERTY_JAVA_OPTS = "java_opts";

  private static final String PLATFORM_PROPERTY_ANT_NAME = "platform.ant.name";

  private static final String URI_TOKEN_HOME = ":home=";
  private static final String URI_TOKEN_CONF = ":conf=";
  private static final String URI_TOKEN_SERVER_ID = ":server-id=";
  private static final String URI_TOKEN_SERVER_PORT = ":server-port=";
  private static final String URI_TOKEN_SERVER_ADDRESS = ":server-address=";

  private File _resinHome;
  private File _resinConf;
  private String _serverId;
  private String _serverAddress;
  private int _serverPort;
  private JavaPlatform _javaPlatform;
  private int _debugPort = 0;
  private int _startTimeout = 60 * 1000;
  private int _stopTimeout = 60 * 1000;

  private String _displayName = "Resin";
  private String _username = "Username";
  private String _password = "Password";
  
  private String _uri;
  private InstanceProperties _ip;
  private boolean _isInit;

  public ResinConfiguration()
  {
    JavaPlatformManager platformManager = JavaPlatformManager.getDefault();
    _javaPlatform = platformManager.getDefaultPlatform();
  }

  public ResinConfiguration(InstanceProperties ip) throws DeploymentManagerCreationException
  {
    _ip = ip;
    
    _uri = ip.getProperty(InstanceProperties.URL_ATTR);

    parseURI(_uri);
  }

  String getContextPath()
  {
    return "/test";
  }

  int getPort()
  {
    String port = _ip.getProperty("resin.port");
    
    try {
      return Integer.parseInt(port);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  private void init()
  {
    if (_isInit)
      return;
    
    _isInit = true;
    
    setUsername(_ip.getProperty(InstanceProperties.USERNAME_ATTR));
    setPassword(_ip.getProperty(InstanceProperties.PASSWORD_ATTR));
    setDisplayName(_ip.getProperty(InstanceProperties.DISPLAY_NAME_ATTR));

    setJavaPlatformByName(_ip.getProperty(PROPERTY_JAVA_PLATFORM));
    
    String resinHome = _ip.getProperty("resin.home");
    
    if (resinHome == null)
      throw new RuntimeException("resin.home is invalid");
    
    _resinHome = new File(resinHome);
    String debugPort = _ip.getProperty(PROPERTY_DEBUG_PORT);

    if (debugPort != null) {
      try {
        setDebugPort(Integer.parseInt(debugPort));
      }
      catch (NumberFormatException e) {
        // no-op
      }
    }
    
    String port = _ip.getProperty("resin.port");
    if (port != null) {
      try {
        _serverPort = Integer.parseInt(port);
      } catch (NumberFormatException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  protected Object clone()
  {
    try {
      return super.clone();
    }
    catch (CloneNotSupportedException e) {
      throw new AssertionError(e);
    }
  }

  public InstanceProperties toInstanceProperties()
    throws InstanceCreationException
  {
    String username = _username;
    String password = _password;
    String displayName = _displayName;

    if (username == null)
      username = "resin";

    if (password == null)
      password = "resin";

    if (displayName == null)
      displayName = "Resin";

    InstanceProperties instanceProperties
      = InstanceProperties.createInstanceProperties(getURI(),
                                                    username,
                                                    password,
                                                    displayName);


    if (_debugPort > 0)
      instanceProperties.setProperty(PROPERTY_DEBUG_PORT, String.valueOf(_debugPort));

    if (_javaPlatform != null)
      instanceProperties.setProperty(PROPERTY_JAVA_PLATFORM, getJavaPlatformName(_javaPlatform));

    return instanceProperties;
  }

  public File getResinConf()
  {
    init();
    
    return _resinConf;
  }

  public void setResinConf(File resinConf)
  {
    if (!resinConf.isAbsolute()) {
      if (_resinHome == null)
        throw new IllegalArgumentException(L.l("no resin.home set for relative conf {0}", resinConf));

      resinConf = new File(_resinHome, resinConf.getPath());
    }

    _resinConf = resinConf;
  }

  public File getResinHome()
  {
    init();
    
    return _resinHome;
  }

  public void setResinHome(File resinHome)
  {
    _resinHome = resinHome;
  }

  public String getServerId()
  {
    return _serverId;
  }

  public void setServerId(String serverId)
  {
    _serverId = serverId;
  }

  public String getServerAddress()
  {
    return _serverAddress == null ? "127.0.0.1" : _serverAddress;
  }

  private void setServerAddress(String serverAddress)
    throws IllegalArgumentException

  {
    try {
      InetAddress.getByName(serverAddress);

      _serverAddress = serverAddress;
    }
    catch (UnknownHostException e) {
      throw new IllegalArgumentException(L.l("The address ''{0}'' is not valid: {1}",
                                             serverAddress, e.getLocalizedMessage()));
    }
  }

  public int getServerPort()
  {
    return _serverPort;
  }

  public void setServerPort(int serverPort)
  {
    _serverPort = serverPort;
  }

  public void setServerPort(String serverPort)
    throws IllegalArgumentException
  {
    int port = 0;

    try {
      port = Integer.parseInt(serverPort);
    }
    catch (NumberFormatException ex) {
      // no-op
    }

    if (!(0 < port && port < 65536))
      throw new IllegalArgumentException(L.l("server-port must have a value between 0 and 65536"));

    setServerPort(port);
  }

  public String getDisplayName()
  {
    return _displayName;
  }

  public void setDisplayName(String displayName)
  {
    _displayName = displayName;
  }

  public String getPassword()
  {
    return _password;
  }

  public void setPassword(String password)
  {
    _password = password;
  }

  public String getUsername()
  {
    return _username;
  }

  public void setUsername(String username)
  {
    _username = username;
  }

  /**
   * Returns the debug port, 0 means a free port should be determnined.
   */
  public int getDebugPort()
  {
    return _debugPort;
  }

  public void setDebugPort(int debugPort)
  {
    _debugPort = debugPort;
  }

  /**
   * Returns the java platform.
   */
  public JavaPlatform getJavaPlatform()
  {
    return _javaPlatform;
  }

  public void setJavaPlatform(JavaPlatform javaPlatform)
  {
    _javaPlatform = javaPlatform;
  }

  public static String getJavaPlatformName(JavaPlatform javaPlatform)
  {
    return ((String) javaPlatform.getProperties().get(PLATFORM_PROPERTY_ANT_NAME));
  }

  public void setJavaPlatformByName(String javaPlatformName)
  {
    JavaPlatformManager platformManager = JavaPlatformManager.getDefault();
    JavaPlatform javaPlatform = platformManager.getDefaultPlatform();

    JavaPlatform[] installedPlatforms
      = platformManager.getPlatforms(null, new Specification("J2SE", null));

    for (JavaPlatform installedPlatform : installedPlatforms) {
      String platformName = getJavaPlatformName(installedPlatform);

      if (platformName != null && platformName.equals(javaPlatformName)) {
        javaPlatform = installedPlatform;
        break;
      }
    }

    _javaPlatform = javaPlatform;
  }

  public int getStartTimeout()
  {
    return _startTimeout;
  }

  public void setStartTimeout(int startTimeout)
  {
    _startTimeout = startTimeout;
  }

  public int getStopTimeout()
  {
    return _stopTimeout;
  }

  public void setStopTimeout(int stopTimeout)
  {
    _stopTimeout = stopTimeout;
  }

  /**
   * Calculates a javaHome based on the {@link #getJavaPlatform()}
   * javaHome.
   */
  public File calculateJavaHome()
  {
    JavaPlatform javaPlatform = _javaPlatform;


    if (javaPlatform == null)
      javaPlatform = JavaPlatformManager.getDefault().getDefaultPlatform();

    return FileUtil.toFile((FileObject) javaPlatform.getInstallFolders().iterator().next());
  }

  public List<URL> getClasses()
  {
    // XXX: s/b urls to Resin libraries
    return new ArrayList<URL>();
  }

  public List<URL> getSources()
  {
    // XXX: s/b urls to Resin sources
    return new ArrayList<URL>();
  }

  public List<URL> getJavadocs()
  {
    return new ArrayList<URL>();
  }

  private void requiredFile(String name, File file)
    throws IllegalStateException
  {
    if (file == null)
      throw new IllegalStateException(L.l("''{0}'' is required", name));

    if (!file.exists())
      throw new IllegalStateException(L.l("''{0}'' does not exist", file));
  }

  public void validate()
    throws IllegalStateException
  {
    log.info("validate");
    requiredFile("resin.home", getResinHome());
    //requiredFile("resin-conf", _resinConf);
/*
    try {
      InetAddress.getByName(getServerAddress());
    }
    catch (UnknownHostException e) {
      throw new IllegalStateException(L.l("server-address ''{0}'' is not valid: {1}",
                                             getServerAddress(),
                                             e.getLocalizedMessage()));
    }
*/
    if (!(0 < _serverPort && _serverPort < 65536))
      throw new IllegalStateException(L.l("''server-port'' must have a value between 0 and 65536"));

  }

  void parseURI(String uri)
    throws IllegalArgumentException
  {
    if (!uri.startsWith("resin"))
      throw new IllegalArgumentException(L.l("''{0}'' is not a Resin URI", uri));

    String token = null;
    int i  = "resin".length();
    int lexemeStart = i;

    try {
      while (true)
      {
        String nextToken = parseURIToken(uri, i);

        if (nextToken != null || i == uri.length()) {
          if (token != null && i > lexemeStart) {
            String lexeme = uri.substring(lexemeStart, i);

            if (token == URI_TOKEN_HOME)
              setResinHome(new File(lexeme));
            else if (token == URI_TOKEN_CONF)
              setResinConf(new File(lexeme));
            else if (token == URI_TOKEN_SERVER_ID)
              setServerId(lexeme);
            else if (token == URI_TOKEN_SERVER_PORT)
              setServerPort(lexeme);
            else if (token == URI_TOKEN_SERVER_ADDRESS)
              setServerAddress(lexeme);
            else
              throw new AssertionError(token);
          }

          if (i == uri.length())
            break;

          token = nextToken;
          i += token.length();
          lexemeStart = i;
        }
        else
          i++;
      }
    }
    catch (Exception ex) {
      log.log(Level.FINER, ex.toString(), ex);

      throw new IllegalArgumentException(L.l("problem parsing URI ''{0}'': {1}",
                                             uri, ex));
    }
  }

  private String parseURIToken(String uri, int i)
  {
   if (uri.regionMatches(i, URI_TOKEN_HOME, 0, URI_TOKEN_HOME.length()))
     return URI_TOKEN_HOME;
    if (uri.regionMatches(i, URI_TOKEN_CONF, 0, URI_TOKEN_CONF.length()))
      return URI_TOKEN_CONF;
    if (uri.regionMatches(i, URI_TOKEN_SERVER_ID, 0, URI_TOKEN_SERVER_ID.length()))
      return URI_TOKEN_SERVER_ID;
    if (uri.regionMatches(i, URI_TOKEN_SERVER_PORT, 0, URI_TOKEN_SERVER_PORT.length()))
      return URI_TOKEN_SERVER_PORT;
    if (uri.regionMatches(i, URI_TOKEN_SERVER_ADDRESS, 0, URI_TOKEN_SERVER_ADDRESS.length()))
      return URI_TOKEN_SERVER_ADDRESS;
    else
      return null;
  }

  public String getURI()
  {
    StringBuilder uri = new StringBuilder();

    uri.append("resin");

    if (_resinHome != null) {
      uri.append(URI_TOKEN_HOME);
      uri.append(_resinHome.getAbsolutePath());
    }

    if (_resinConf != null) {
      uri.append(":conf=");
      uri.append(_resinConf.getAbsolutePath());
    }


    if (_serverId != null) {
      uri.append(URI_TOKEN_SERVER_ID);
      uri.append(_serverId);
    }

    if ((0 < _serverPort && _serverPort < 65536)) {
      uri.append(URI_TOKEN_SERVER_PORT);
      uri.append(_serverPort);
    }

    if (_serverAddress != null) {
      uri.append(URI_TOKEN_SERVER_ADDRESS);
      uri.append(_serverAddress);
    }

    return uri.toString();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getURI() + "]";
  }

}
