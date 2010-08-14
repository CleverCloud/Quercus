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

package com.caucho.server.deploy;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.caucho.config.types.FileSetType;
import com.caucho.env.repository.AbstractRepository;
import com.caucho.env.repository.Repository;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.util.L10N;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * A deployment entry that expands from an archive (Jar/Zip) file.
 */
abstract public class ExpandDeployController<I extends DeployInstance>
  extends DeployController<I> {
  private static final L10N L = new L10N(ExpandDeployController.class);
  private static final Logger log
    = Logger.getLogger(ExpandDeployController.class.getName());

  private Object _archiveExpandLock = new Object();

  private Path _rootDirectory;
  private Path _archivePath;

  private Repository _repository;
  private String _repositoryTag;
  private String _baseRepositoryTag;

  private FileSetType _expandCleanupFileSet;

  // classloader for the manifest entries
  private DynamicClassLoader _manifestLoader;
  private Manifest _manifest;

  protected ExpandDeployController(String id)
  {
    this(id, null, null);
  }

  protected ExpandDeployController(String id,
                                   ClassLoader loader,
                                   Path rootDirectory)
  {
    super(id, loader);

    if (rootDirectory == null)
      rootDirectory = Vfs.getPwd(getParentClassLoader());

    _rootDirectory = rootDirectory;
  }

  /**
   * Gets the root directory
   */
  public Path getRootDirectory()
  {
    return _rootDirectory;
  }

  /**
   * Sets the root directory
   */
  protected void setRootDirectory(Path rootDirectory)
  {
    _rootDirectory = rootDirectory;
  }

  /**
   * Gets the archive path.
   */
  public Path getArchivePath()
  {
    return _archivePath;
  }

  /**
   * Sets the archive path.
   */
  public void setArchivePath(Path path)
  {
    _archivePath = path;
  }

  /**
   * Returns the repository
   */
  public Repository getRepository()
  {
    return _repository;
  }

  /**
   * Sets the repository
   */
  public void setRepository(Repository repository)
  {
    _repository = repository;
  }

  /**
   * The repository tag
   */
  public String getRepositoryTag()
  {
    return _repositoryTag;
  }

  /**
   * The repository tag
   */
  public void setRepositoryTag(String tag)
  {
    _repositoryTag = tag;
  }

  /**
   * The base repository tag
   */
  public String getBaseRepositoryTag()
  {
    return _baseRepositoryTag;
  }

  /**
   * The base repository tag
   */
  public void setBaseRepositoryTag(String tag)
  {
    _baseRepositoryTag = tag;
  }

  /**
   * Returns the manifest.
   */
  public Manifest getManifest()
  {
    return _manifest;
  }

  /**
   * Returns the manifest as an attribute map
   */
  public Map<String,String> getManifestAttributes()
  {
    if (_manifest == null)
      return null;
    
    Map<String,String> map = new TreeMap<String,String>();

    Attributes attr = _manifest.getMainAttributes();

    if (attr != null) {
      for (Map.Entry<Object,Object> entry : attr.entrySet()) {
        map.put(String.valueOf(entry.getKey()),
                String.valueOf(entry.getValue()));
      }
    }

    return map;
  }

  /**
   * Sets the manifest class loader.
   */
  public void setManifestClassLoader(DynamicClassLoader loader)
  {
    _manifestLoader = loader;
  }

  /**
   * Sets the archive auto-remove file set.
   */
  public void setExpandCleanupFileSet(FileSetType fileSet)
  {
    _expandCleanupFileSet = fileSet;
  }

  /**
   * Merges with the old controller.
   */
  @Override
  protected void mergeController(DeployController<I> oldControllerV)
  {
    super.mergeController(oldControllerV);

    ExpandDeployController<I> oldController;
    oldController = (ExpandDeployController<I>) oldControllerV;

    if (oldController._expandCleanupFileSet != null)
      _expandCleanupFileSet = oldController._expandCleanupFileSet;

    if (oldController.getArchivePath() != null)
      setArchivePath(oldController.getArchivePath());

    if (oldController.getRepositoryTag() != null)
      setRepositoryTag(oldController.getRepositoryTag());
  }

  /**
   * Deploys the controller
   */
  public void deploy()
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " deploying");
    
    try {
      expandArchive();
    } catch (Exception e) {
      // XXX: better exception
      throw new RuntimeException(e);
    }
  }

  /**
   * Expand an archive file.  The _archiveExpandLock must be obtained
   * before the expansion.
   */
  protected void expandArchive()
    throws IOException
  {
    synchronized (_archiveExpandLock) {
      if (expandRepositoryImpl()) {
      }
      else if (expandArchiveImpl()) {
      }
      else {
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
        }

        if (expandRepositoryImpl()) {
        }
        else
          expandArchiveImpl();
      }

      Path path = getRootDirectory().lookup("META-INF/MANIFEST.MF");
      if (path.canRead()) {
        ReadStream is = path.openRead();
        try {
          _manifest = new Manifest(is);
        } catch (IOException e) {
          log.warning(L.l("Manifest file cannot be read for '{0}'.\n{1}",
                          getRootDirectory(), e));

          log.log(Level.FINE, e.toString(), e);
        } finally {
          is.close();
        }
      }
    }
  }

  /**
   * Adds any class path from the manifest.
   */
  protected void addManifestClassPath()
    throws IOException
  {
    DynamicClassLoader loader = Environment.getDynamicClassLoader();
    if (loader == null)
      return;

    Manifest manifest = getManifest();

    if (manifest == null)
      return;

    Attributes main = manifest.getMainAttributes();

    if (main == null)
      return;

    String classPath = main.getValue("Class-Path");

    Path pwd = null;

    if (getArchivePath() != null)
      pwd = getArchivePath().getParent();
    else
      pwd = getRootDirectory();

    if (classPath == null) {
    }
    else if (_manifestLoader != null)
      _manifestLoader.addManifestClassPath(classPath, pwd);
    else
      loader.addManifestClassPath(classPath, pwd);
  }

  /**
   * Expand an archive.  The _archiveExpandLock must be obtained before the
   * expansion.
   */
  private boolean expandRepositoryImpl()
    throws IOException
  {
    try {
      if (_repository == null)
        return false;
      
      String treeHash = _repository.getTagContentHash(getRepositoryTag());

      if (treeHash == null)
        return false;
      
      Path pwd = getRootDirectory();

      pwd.mkdirs();

      if (log.isLoggable(Level.FINE))
        log.fine(this + " expanding .git repository tag=" + getRepositoryTag()
                 + " tree=" + treeHash + " -> root=" + getRootDirectory());

      _repository.expandToPath(treeHash, pwd);

      return true;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Expand an archive.  The _archiveExpandLock must be obtained before the
   * expansion.
   */
  private boolean expandArchiveImpl()
    throws IOException
  {
    Path archivePath = getArchivePath();

    if (archivePath == null)
      return true;

    if (! archivePath.canRead())
      return true;

    Path expandDir = getRootDirectory();
    Path parent = expandDir.getParent();

    try {
      parent.mkdirs();
    } catch (Throwable e) {
    }

    Path dependPath = expandDir.lookup("META-INF/resin-war.digest");
    Depend depend = null;

    // XXX: change to a hash
    if (dependPath.canRead()) {
      ReadStream is = null;
      try {
        is = dependPath.openRead();

        String line = is.readLine();

        long digest;

        if (line != null) {
          digest = Long.parseLong(line.trim());

          depend = new Depend(archivePath, digest);

          if (! depend.isModified())
            return true;
        }
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      } finally {
        if (is != null)
          is.close();
      }
    }

    if (depend == null)
      depend = new Depend(archivePath);

    try {
      if (log.isLoggable(Level.INFO))
        getLog().info("expanding " + archivePath + " to " + expandDir);

      removeExpandDirectory(expandDir);

      expandDir.mkdirs();

      ReadStream rs = archivePath.openRead();
      ZipInputStream zis = new ZipInputStream(rs);

      try {
        ZipEntry entry = zis.getNextEntry();

        byte []buffer = new byte[1024];

        while (entry != null) {
          String name = entry.getName();
          Path path = expandDir.lookup(name);

          if (entry.isDirectory())
            path.mkdirs();
          else {
            long entryLength = entry.getSize();
            long length = entryLength;

            // XXX: avoids unexpected end of ZLIB input stream.
            // XXX: This should be a really temp. workaround.
            int bufferLen = buffer.length;

            // XXX: avoids unexpected end of ZLIB input stream.
            if (length < 0) {
              // bufferLen = 1;
              length = Long.MAX_VALUE / 2;
            }
            else if (length < bufferLen) {
              bufferLen = (int) length;
            }

            long lastModified = entry.getTime();
            path.getParent().mkdirs();

            WriteStream os = path.openWrite();
            int len = 0;
            try {
              if (bufferLen == 1) {
                for (int ch = zis.read(); ch != -1; ch = zis.read())
                  os.write(ch);
              }
              else {
                while ((len = zis.read(buffer, 0, bufferLen)) > 0) {
                  os.write(buffer, 0, len);

                  // XXX: avoids unexpected end of ZLIB input stream.
                  /*
                  if (len < bufferLen) {
                    for (int ch = zis.read(); ch != -1; ch = zis.read())
                      os.write(ch);

                    break;
                  }
                  */

                  length -= len;

                  if (length < bufferLen) {
                    bufferLen = (int) length;
                  }
                }
              }
            } catch (IOException e) {
              Exception ex = new Exception("IOException when expanding entry "
                                           + entry
                                           +" in " + archivePath
                                           + ", entry.length: " + entryLength
                                           + " entry.compressed: " + entry.getCompressedSize()
                                           + ", bufferLen: " + bufferLen
                                           + ", read len: " + len
                                           + ", remaining: " + length, e);

              log.log(Level.FINE, ex.toString(), ex);
            } finally {
              os.close();
            }

            if (lastModified > 0)
              path.setLastModified(lastModified);
          }

          try {
            entry = zis.getNextEntry();
          } catch (IOException e) {
            log.log(Level.FINE, e.toString(), e);

            // XXX: avoids unexpected end of ZLIB input stream.
            break;
          }
        }
      } finally {
        try {
          zis.close();
        } catch (IOException e) {
        }

        rs.close();
      }
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
      // If the jar is incomplete, it should throw an exception here.
      return false;
    }

    try {
      dependPath.getParent().mkdirs();
      WriteStream os = dependPath.openWrite();

      os.println(depend.getDigest());

      os.close();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    return true;
  }

  protected void addDependencies()
  {
    if (getArchivePath() != null)
      Environment.addDependency(getArchivePath());

    if (getRepository() != null && getRepositoryTag() != null) {
      String tag = getRepositoryTag();
      String value = getRepository().getTagContentHash(tag);

      Environment.addDependency(new RepositoryDependency(tag, value));
    }
  }

  /**
   * Recursively remove all files in a directory.  Used for wars when
   * they change.
   *
   * @param path root directory to start removal
   */
  protected void removeExpandDirectory(Path path)
  {
    String prefix = path.getPath();

    if (! prefix.endsWith("/"))
      prefix = prefix + "/";

    removeExpandDirectory(path, prefix);
  }

  /**
   * Recursively remove all files in a directory.  Used for wars when
   * they change.
   *
   * @param dir root directory to start removal
   */
  protected void removeExpandDirectory(Path path, String prefix)
  {
    try {
      if (path.isDirectory()) {
        String []list = path.list();
        for (int i = 0; list != null && i < list.length; i++) {
          removeExpandDirectory(path.lookup(list[i]), prefix);
        }
      }

      removeExpandFile(path, prefix);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Removes an expanded file.
   */
  protected void removeExpandFile(Path path, String prefix)
    throws IOException
  {
    if (_expandCleanupFileSet == null
        || _expandCleanupFileSet.isMatch(path, prefix)) {
      path.remove();
    }
  }

  /**
   * Returns the hash code.
   */
  public int hashCode()
  {
    return getId().hashCode();
  }

  /**
   * Returns equality.
   */
  public boolean equals(Object o)
  {
    // server/125g
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    DeployController<?> controller = (DeployController<?>) o;

    // XXX: s/b getRootDirectory?
    return getId().equals(controller.getId());
  }
}
