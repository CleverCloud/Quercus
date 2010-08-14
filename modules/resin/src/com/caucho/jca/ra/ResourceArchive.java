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

package com.caucho.jca.ra;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.jca.cfg.AdminObjectConfig;
import com.caucho.jca.cfg.ConnectionDefinition;
import com.caucho.jca.cfg.ConnectorConfig;
import com.caucho.jca.cfg.MessageListenerConfig;
import com.caucho.jca.cfg.ResourceAdapterConfig;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentBean;
import com.caucho.util.L10N;
import com.caucho.vfs.Jar;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A resource archive (rar)
 */
public class ResourceArchive implements EnvironmentBean
{
  static final L10N L = new L10N(ResourceArchive.class);
  static final Logger log = Logger.getLogger(ResourceArchive.class.getName());

  private ClassLoader _loader;
  
  private Path _rootDir;

  private Path _rarPath;

  private ConnectorConfig _config;

  /**
   * Creates the application.
   */
  ResourceArchive()
  {
    _loader = Thread.currentThread().getContextClassLoader();
  }

  /**
   * Sets the root directory.
   */
  public void setRootDirectory(Path rootDir)
  {
    _rootDir = rootDir;
  }

  /**
   * Sets the root directory.
   */
  public Path getRootDirectory()
  {
    return _rootDir;
  }

  /**
   * Returns the class loader.
   */
  public ClassLoader getClassLoader()
  {
    return _loader;
  }

  /**
   * Sets the path to the .ear file
   */
  public void setRarPath(Path rarPath)
  {
    _rarPath = rarPath;
  }

  /**
   * Returns the name.
   */
  public String getDisplayName()
  {
    return _config.getDisplayName();
  }

  /**
   * Returns the resource adapter class.
   */
  public ResourceAdapterConfig getResourceAdapter()
  {
    return _config.getResourceAdapter();
  }

  /**
   * Returns the resource adapter class.
   */
  public Class getResourceAdapterClass()
  {
    return _config.getResourceAdapter().getResourceadapterClass();
  }

  /**
   * Returns the transaction support.
   */
  public String getTransactionSupport()
  {
    if (getResourceAdapter() != null)
      return getResourceAdapter().getTransactionSupport();
    else
      return null;
  }

  /**
   * Returns the matching connection factory class.
   */
  public ConnectionDefinition getConnectionDefinition(String type)
  {
    ResourceAdapterConfig raConfig = _config.getResourceAdapter();

    if (raConfig != null)
      return raConfig.getConnectionDefinition(type);
    else
      return null;
  }

  /**
   * Returns the activation spec.
   */
  public MessageListenerConfig getMessageListener(String type)
  {
    ResourceAdapterConfig raConfig = _config.getResourceAdapter();

    if (raConfig != null)
      return raConfig.getMessageListener(type);
    else
      return null;
  }

  /**
   * Returns the managed object definition.
   */
  public AdminObjectConfig getAdminObject(String type)
  {
    ResourceAdapterConfig raConfig = _config.getResourceAdapter();

    if (raConfig != null)
      return raConfig.getAdminObject(type);
    else
      return null;
  }

  /**
   * Configures the resource.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    try {
      expandRar();
      
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      for (; loader != null; loader = loader.getParent()) {
        if (loader instanceof DynamicClassLoader)
          break;
      }

      if (loader == null)
        throw new ConfigException(L.l("loader issues with resource adapter"));

      addJars((DynamicClassLoader) loader, _rootDir);
      addNative((DynamicClassLoader) loader, _rootDir);
      
      Path raXml = _rootDir.lookup("META-INF/ra.xml");

      if (! raXml.canRead())
        throw new ConfigException(L.l("missing ra.xml for rar {0}.  .rar files require a META-INF/ra.xml file.",
                                      _rarPath));

      _config = new ConnectorConfig();

      new Config().configure(_config, raXml, "com/caucho/jca/jca.rnc");
    } catch (ConfigException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }

    log.info("ResourceArchive[" + _config.getDisplayName() + "] loaded");
  }

  void destroy()
  {
  }

  /**
   * Adds the jars from the rar file to the class loader.
   */
  private void addJars(DynamicClassLoader loader, Path path)
    throws IOException
  {
    if (path.getPath().endsWith(".jar")) {
      loader.addJar(path);
    }
    else if (path.isDirectory()) {
      String []list = path.list();

      for (int i = 0; i < list.length; i++)
        addJars(loader, path.lookup(list[i]));
    }
  }

  /**
   * Adds the native paths from the rar file to the class loader.
   */
  private void addNative(DynamicClassLoader loader, Path path)
    throws IOException
  {
    String fileName = path.getPath();

    if (fileName.endsWith(".so")
        || fileName.endsWith(".dll")
        || fileName.endsWith(".jnilib")) {
      loader.addNative(path);
    }
    
    else if (path.isDirectory()) {
      String []list = path.list();

      for (int i = 0; i < list.length; i++)
        addNative(loader, path.lookup(list[i]));
    }
  }

  /**
   * Expand an rar file.  The _rarExpandLock must be obtained before the
   * expansion.
   *
   * @param rar the rar file
   * @param expandDir the directory which will contain the rar contents
   */
  private void expandRar()
    throws IOException
  {
    Path rar = _rarPath;
    
    if (! rar.canRead())
      return;

    try {
      _rootDir.mkdirs();
    } catch (Throwable e) {
    }

    Path expandDir = _rootDir;
    Path tempDir = _rootDir.getParent().lookup(".temp");
    Path dependPath = _rootDir.lookup("META-INF/resin-rar.timestamp");

      // XXX: change to a hash
    if (dependPath.canRead()) {
      ReadStream is = null;
      ObjectInputStream ois = null;
      try {
        is = dependPath.openRead();
        ois = new ObjectInputStream(is);

        long lastModified = ois.readLong();
        long length = ois.readLong();

        if (lastModified == rar.getLastModified() &&
            length == rar.getLength())
          return;
      } catch (IOException e) {
      } finally {
        try {
          if (ois != null)
            ois.close();
        } catch (IOException e) {
        }

        if (is != null)
          is.close();
      }
    }

    try {
      if (log.isLoggable(Level.INFO))
        log.info("expanding rar " + rar + " to " + tempDir);

      
      if (! tempDir.equals(expandDir)) {
        tempDir.removeAll();
      }
      tempDir.mkdirs();

      ReadStream rs = rar.openRead(); 
      ZipInputStream zis = new ZipInputStream(rs);

      try {
        ZipEntry entry;

        byte []buffer = new byte[1024];
      
        while ((entry = zis.getNextEntry()) != null) {
          String name = entry.getName();
          Path path = tempDir.lookup(name);

          if (entry.isDirectory())
            path.mkdirs();
          else {
            long length = entry.getSize();
            long lastModified = entry.getTime();
            path.getParent().mkdirs();

            WriteStream os = path.openWrite();
            try {
              int len;
              while ((len = zis.read(buffer, 0, buffer.length)) > 0)
                os.write(buffer, 0, len);
            } catch (IOException e) {
              log.log(Level.FINE, e.toString(), e);
            } finally {
              os.close();
            }

            if (lastModified > 0)
              path.setLastModified(lastModified);
          }
        }
      } finally {
        try {
          zis.close();
        } catch (IOException e) {
        }

        rs.close();
      }

      if (! tempDir.equals(expandDir)) {
        if (log.isLoggable(Level.INFO))
          log.info("moving rar " + rar + " to " + expandDir);

        // Close the cached zip streams because on NT that can lock
        // the filesystem.
        try {
          Jar.clearJarCache();
          removeAll(expandDir);
        } catch (Throwable e) {
          Jar.clearJarCache();
          removeAll(expandDir);
        }

        moveAll(tempDir, expandDir);
        removeAll(tempDir);
      }
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
      // If the jar is incomplete, it should throw an exception here.
      return;
    }
    
    try {
      dependPath.getParent().mkdirs();
      WriteStream os = dependPath.openWrite();
      ObjectOutputStream oos = new ObjectOutputStream(os);

      oos.writeLong(rar.getLastModified());
      oos.writeLong(rar.getLength());
      oos.close();
      os.close();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Recursively remove all files in a directory.  Used for wars when
   * they change.
   *
   * @param dir root directory to start removal
   */
  private static void removeAll(Path path)
  {
    try {
      if (path.isDirectory()) {
        String []list = path.list();
        for (int i = 0; list != null && i < list.length; i++) {
          removeAll(path.lookup(list[i]));
        }
      }
        
      path.remove();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Move directory A to directory B.
   *
   * @param source source directory
   * @param target target directory
   */
  private static void moveAll(Path source, Path target)
  {
    try {
      if (source.isDirectory()) {
        try {
          target.mkdirs();
        } catch (IOException e) {
        }
        
        String []list = source.list();
        for (int i = 0; list != null && i < list.length; i++) {
          moveAll(source.lookup(list[i]), target.lookup(list[i]));
        }
      }
      else
        source.renameTo(target);
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getResourceAdapterClass() + "]";
  }
}
