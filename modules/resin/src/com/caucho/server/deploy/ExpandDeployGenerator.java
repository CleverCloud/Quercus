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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.cloud.deploy.DeployNetworkService;
import com.caucho.cloud.deploy.DeployUpdateListener;
import com.caucho.config.ConfigException;
import com.caucho.config.types.FileSetType;
import com.caucho.config.types.Period;
import com.caucho.env.repository.Repository;
import com.caucho.env.repository.RepositoryTagEntry;
import com.caucho.loader.Environment;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.Crc64;
import com.caucho.util.L10N;
import com.caucho.util.WeakAlarm;
import com.caucho.vfs.Path;

/**
 * The generator for the deploy
 */
abstract public class ExpandDeployGenerator<E extends ExpandDeployController<?>>
  extends DeployGenerator<E>
  implements AlarmListener, DeployUpdateListener
{
  private static final Logger log
    = Logger.getLogger(ExpandDeployGenerator.class.getName());
  private static final L10N L = new L10N(ExpandDeployGenerator.class);

  private static final long MIN_CRON_INTERVAL = 5000L;

  private Path _path; // default path
  private ClassLoader _loader;

  private Path _containerRootDirectory;
  private Path _archiveDirectory;
  private Path _expandDirectory;

  private Repository _repository;
  private String _repositoryTag;
  
  private DeployNetworkService _deployService;
  
  private String _entryNamePrefix = "";

  private String _extension = ".jar";
  
  private String _expandPrefix = "";
  private String _expandSuffix = "";

  private boolean _isVersioning;
  
  private ArrayList<String> _requireFiles = new ArrayList<String>();

  private TreeSet<String> _controllerNames = new TreeSet<String>();
  
  private FileSetType _expandCleanupFileSet;

  private Alarm _alarm;
  private long _cronInterval;

  private volatile long _lastCheckTime;
  private volatile boolean _isChecking;
  private long _checkInterval = 1000L;
  private long _digest;
  private volatile boolean _isModified;
  private volatile boolean _isDeploying;

  /**
   * Creates the deploy.
   */
  public ExpandDeployGenerator(DeployContainer<E> container,
                               Path containerRootDirectory)
  {
    super(container);

    _containerRootDirectory = containerRootDirectory;

    _alarm = new WeakAlarm(this);

    _cronInterval = Environment.getDependencyCheckInterval();
    if (_cronInterval < MIN_CRON_INTERVAL)
      _cronInterval = MIN_CRON_INTERVAL;

    _loader = Thread.currentThread().getContextClassLoader();
    
    _deployService = DeployNetworkService.getCurrent();
    
    if (_deployService != null)
      _deployService.addUpdateListener(this);
  }

  Path getContainerRootDirectory()
  {
    return _containerRootDirectory;
  }

  /**
   * Sets the war expand dir to check for new archive files.
   */
  public void setArchiveDirectory(Path path)
  {
    _archiveDirectory = path;
  }

  /**
   * Gets the war expand directory.
   */
  public Path getArchiveDirectory()
  {
    if (_archiveDirectory != null)
      return _archiveDirectory;
    else
      return _path;
  }

  /**
   * Returns the location for deploying an archive with the specified name.
   *
   * @param name a name, without an extension
   */
  public Path getArchivePath(String name)
  {
    return getArchiveDirectory().lookup(name + getExtension());
  }

  /**
   * Sets the war expand dir to check for new applications.
   */
  public void setExpandPath(Path path)
  {
    log.config("Use <expand-directory> instead of <expand-path>.  <expand-path> is deprecated.");

    setExpandDirectory(path);
  }

  /**
   * Sets the war expand dir to check for new applications.
   */
  public void setExpandDirectory(Path path)
  {
    _expandDirectory = path;
  }

  /**
   * Gets the war expand directory.
   */
  public Path getExpandDirectory()
  {
    if (_expandDirectory != null)
      return _expandDirectory;
    else
      return _path;
  }

  /**
   * Returns the location of an expanded archive, or null if no archive with
   * the passed name is deployed.
   *
   * @param name a name, without an extension
   */
  public Path getExpandPath(String name)
  {
    if (!isDeployedKey(nameToEntryName(name)))
      return null;

    return getExpandDirectory().lookup(getExpandName(name));

    /*
    if (expandDir.isDirectory())
      return expandDir;

    Path extPath = getExpandDirectory().lookup(name + _extension);
    
    if (extPath.isDirectory())
      return extPath;
    else
      return expandDir;
    */
  }

  /**
   * Returns the combination of prefix, name, and suffix used for expanded
   * archives.
   *
   * @return
   */
  protected String getExpandName(String name)
  {
    return getExpandPrefix() + name + getExpandSuffix();
  }

  /**
   * Sets the dependency check interval.
   */
  public void setDependencyCheckInterval(Period period)
  {
    _cronInterval = period.getPeriod();

    if (_cronInterval < 0)
      _cronInterval = Period.INFINITE;
    else if (_cronInterval < MIN_CRON_INTERVAL)
      _cronInterval = MIN_CRON_INTERVAL;
  }

  public long getDependencyCheckInterval()
  {
    return _cronInterval;
  }

  /**
   * Sets the expand remove file set.
   */
  public void setExpandCleanupFileset(FileSetType fileSet)
  {
    _expandCleanupFileSet = fileSet;
  }

  /**
   * Sets the extension.
   */
  public void setExtension(String extension)
    throws ConfigException
  {
    if (! extension.startsWith("."))
      throw new ConfigException(L.l("deployment extension '{0}' must begin with '.'",
                                    extension));

    _extension = extension;
  }

  /**
   * Returns the extension.
   */
  public String getExtension()
  {
    return _extension;
  }

  /**
   * Sets the expand prefix to check for new applications.
   */
  public void setExpandPrefix(String prefix)
    throws ConfigException
  {
    if (! prefix.equals("")
        && ! prefix.startsWith("_")
        && ! prefix.startsWith("."))
      throw new ConfigException(L.l("expand-prefix '{0}' must start with '.' or '_'.",
                                    prefix));

    _expandPrefix = prefix;
  }

  /**
   * Gets the expand prefix.
   */
  public String getExpandPrefix()
  {
    return _expandPrefix;
  }

  /**
   * Sets the expand suffix to check for new applications.
   */
  public void setExpandSuffix(String suffix)
    throws ConfigException
  {
    _expandSuffix = suffix;
  }

  /**
   * Gets the expand suffix.
   */
  public String getExpandSuffix()
  {
    return _expandSuffix;
  }

  /**
   * The repository
   */
  public void setRepository(Repository repository)
  {
    _repository = repository;
  }

  /**
   * The repository
   */
  public Repository getRepository()
  {
    return _repository;
  }

  /**
   * The repository tag
   */
  public void setRepositoryTag(String repositoryTag)
  {
    _repositoryTag = repositoryTag;
  }

  /**
   * The repository tag
   */
  public String getRepositoryTag()
  {
    return _repositoryTag;
  }

  public void setEntryNamePrefix(String entryNamePrefix)
  {
    _entryNamePrefix = entryNamePrefix;
  }

  /**
   * Gets the default path.
   */
  public Path getPath()
  {
    return _path;
  }

  /**
   * Sets the deploy directory.
   */
  public void setPath(Path path)
  {
    _path = path;
  }

  /**
   * Adds a required file in the expansion.
   */
  public void addRequireFile(String file)
    throws ConfigException
  {
    _requireFiles.add(file);
  }

  /**
   * Sets true to enable versioning
   */
  public void setVersioning(boolean isVersioning)
  {
    _isVersioning = isVersioning;
  }

  /**
   * Sets true to enable versioning
   */
  public boolean isVersioning()
  {
    return _isVersioning;
  }

  /**
   * Returns the log.
   */
  protected Logger getLog()
  {
    return log;
  }

  /**
   * Returns true if the deployment has modified.
   */
  @Override
  public boolean isModified()
  {
    synchronized (this) {
      long now = Alarm.getCurrentTime();
      
      if (now < _lastCheckTime + _checkInterval || _isChecking) {
        return _isModified;
      }

      _isChecking = true;
      _lastCheckTime = Alarm.getCurrentTime();
    }

    try {
      long digest = getDigest();

      _isModified = _digest != digest;

      return _isModified;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      return false;
    } finally {
      _isChecking = false;
    }
  }

  /**
   * Log the reason for modification
   */
  @Override
  public boolean logModified(Logger log)
  {
    long digest = getDigest();

    if (_digest != digest) {
      String reason = "";
      
      String name = getClass().getName();
      int p = name.lastIndexOf('.');
      if (p > 0)
        name = name.substring(p + 1);
    
      Path archiveDirectory = getArchiveDirectory();
      if (archiveDirectory != null)
        reason = name + "[" + archiveDirectory.getNativePath() + "] is modified";
    
      Path expandDirectory = getExpandDirectory();
      if (expandDirectory != null
          && ! expandDirectory.equals(archiveDirectory)) {
        if (! "".equals(reason))
          reason = reason + " or ";

        reason = name + "[" + expandDirectory.getNativePath() + "] is modified";
      }

      log.info(reason);

      return true;
    }

    return false;
  }

  /**
   * Configuration checks on init.
   */
  @Override
  protected void initImpl()
    throws ConfigException
  {
    super.initImpl();

    if (getExpandDirectory() == null)
      throw new ConfigException(L.l("<expand-directory> must be specified for deployment of archive expansion."));

    if (getArchiveDirectory() == null)
      throw new ConfigException(L.l("<archive-directory> must be specified for deployment of archive expansion."));
  }

  /**
   * Starts the deploy.
   */
  @Override
  protected void startImpl()
  {
    super.startImpl();
    
    handleAlarm(_alarm);
  }

  /**
   * Returns the deployed keys.
   */
  protected void fillDeployedKeys(Set<String> keys)
  {
    if (isModified()) {
      try {
        deployStart();
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    for (String name : _controllerNames) {
      keys.add(name);
    }
  }

  /**
   * Return true for a matching key.
   */
  protected boolean isDeployedKey(String key)
  {
    return _controllerNames.contains(key);
  }

  /**
   * Forces an update.
   */
  @Override
  public final void update()
  {
    // force modify check
    _lastCheckTime = 0;

    request();
  }
  

  /**
   * Redeploys if modified.
   */
  @Override
  public void request()
  {
    if (isModified()) {
      try {
        deployStart();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  /**
   * Deploys the objects.
   */
  private void deployStart()
    throws Exception
  {
    boolean isDeploying = false;

    if (! _isDeploying)
      log.finer(this + " deploy/start " + _isDeploying);

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);
      
      ArrayList<String> updatedNames = null;

      synchronized (this) {
        if (_isDeploying)
          return;
        else {
          _isDeploying = true;
          isDeploying = true;
        }

        TreeSet<String> entryNames = findEntryNames();

        _digest = getDigest();

        if (! _controllerNames.equals(entryNames)) {
          updatedNames = new ArrayList<String>();

          for (String name : _controllerNames) {
            if (! entryNames.contains(name))
              updatedNames.add(name);
          }

          for (String name : entryNames) {
            if (! _controllerNames.contains(name))
              updatedNames.add(name);
          }

          _controllerNames = entryNames;
        }
      }

      for (int i = 0; updatedNames != null && i < updatedNames.size(); i++) {
        String name = updatedNames.get(i);

        getDeployContainer().update(name);
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
      
      if (isDeploying) {
        _isModified = false;
        _isDeploying = false;
      }
    }
  }

  /**
   * Finds the matching entry.
   */
  public E generateController(String name)
  {
    request();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(getParentClassLoader());

      E controller = createController(name);

      if (controller == null)
        return null;

      controller.setExpandCleanupFileSet(_expandCleanupFileSet);

      _controllerNames.add(name); // server/1d19
      
      return controller;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  //
  // DeployNetworkService callbacks
  //
  
  @Override
  public void onUpdate(String tag)
  {
    update();
  }

  /**
   * Returns the digest of the expand and archive directories.
   */
  private long getDigest()
  {
    long digest = 0;
    
    long archiveDigest = 0;
    
    Path archiveDirectory = getArchiveDirectory();
    if (archiveDirectory != null)
      archiveDigest = archiveDirectory.getCrc64();

    digest = Crc64.generate(digest, archiveDigest);
    
    long expandDigest = 0;
    
    Path expandDirectory = getExpandDirectory();
    if (expandDirectory != null)
      expandDigest = expandDirectory.getCrc64();
    
    digest = Crc64.generate(digest, expandDigest);
    
    if (_repository != null && _repositoryTag != null) {
      digest = calculateRepositoryDigest(digest);
    }

    return digest;
  }

  private long calculateRepositoryDigest(long digest)
  {
    String prefix = getRepositoryTag() + "/";
    
    ArrayList<String> tags = new ArrayList<String>();
    Map<String,RepositoryTagEntry> tagMap = _repository.getTagMap();

    for (String key : tagMap.keySet()) {
      if (key.startsWith(prefix))
        tags.add(key);
    }

    Collections.sort(tags);

    for (String tag : tags) {
      digest = Crc64.generate(digest, tag);

      RepositoryTagEntry entry = tagMap.get(tag);

      if (entry.getRoot() != null)
        digest = Crc64.generate(digest, entry.getRoot());
    }

    return digest;
  }

  public VersionEntry getVersionEntry(String name)
  {
    if (! isVersioning())
      return null;
    
    TreeMap<String,VersionEntry> map = buildVersionMap();

    return map.get(name);
  }

  public VersionEntry getVersionEntryByRoot(String root)
  {
    if (! isVersioning())
      return null;
    
    TreeMap<String,VersionEntry> map = buildVersionMap();

    for (Map.Entry<String,VersionEntry> mapEntry : map.entrySet()) {
      VersionEntry vEntry = mapEntry.getValue();

      if (vEntry.getRoot().equals(root)
          && ! vEntry.getContextPath().equals(vEntry.getBaseContextPath())) {
        return vEntry;
      }
    }

    return null;
  }
  
  /**
   * Return the entry names for all deployed objects.
   */
  private TreeSet<String> findEntryNames()
    throws IOException
  {
    TreeSet<String> entryNames = new TreeSet<String>();

    addArchiveEntryNames(entryNames);
    addExpandEntryNames(entryNames);
    addRepositoryEntryNames(entryNames);

    return entryNames;
  }
  
  /**
   * Return the entry names for all deployed objects.
   */
  private void addArchiveEntryNames(TreeSet<String> entryNames)
    throws IOException
  {
    Path archiveDirectory = getArchiveDirectory();

    if (archiveDirectory == null)
      return;

    String []entryList = archiveDirectory.list();

    // collect all the new entries
    // loop:
    for (int i = 0; i < entryList.length; i++) {
      String archiveName = entryList[i];

      Path archivePath = archiveDirectory.lookup(archiveName);

      String entryName = null;
      
      if (! archivePath.canRead())
        continue;
      else
        entryName = archiveNameToEntryName(archiveName);

      if (entryName != null) {
        entryNames.add(entryName);

        if (_isVersioning) {
          int p = entryName.lastIndexOf('-');

          if (p >= 0) {
            entryName = entryName.substring(0, p);

            if (! entryNames.contains(entryName))
              entryNames.add(entryName);
          }
        }
      }
    }
  }
  
  /**
   * Return the entry names for all deployed objects.
   */
  private void addExpandEntryNames(TreeSet<String> entryNames)
    throws IOException
  {
    Path expandDirectory = getExpandDirectory();

    if (expandDirectory == null)
      return;
    
    String []entryExpandList = expandDirectory.list();

    // collect all the new war expand directories
    //loop:
    for (int i = 0; i < entryExpandList.length; i++) {
      String pathName = entryExpandList[i];

      /* XXX: this used to be needed to solve issues with NT
      if (CauchoSystem.isCaseInsensitive())
        pathName = pathName.toLowerCase();
      */

      Path rootDirectory = expandDirectory.lookup(pathName);
      
      String entryName = pathNameToEntryName(pathName);

      if (entryName == null)
        continue;
      else if (entryName.endsWith(getExtension()))
        continue;

      if (! isValidDirectory(rootDirectory, pathName))
        continue;

      if (! entryNames.contains(entryName))
        entryNames.add(entryName);

      if (_isVersioning) {
        int p = entryName.lastIndexOf('-');

        if (p >= 0) {
          entryName = entryName.substring(0, p);

          if (! entryNames.contains(entryName))
            entryNames.add(entryName);
        }
      }
    }
  }
  
  /**
   * Return the entry names for all repository objects.
   */
  private void addRepositoryEntryNames(TreeSet<String> entryNames)
    throws IOException
  {
    if (_repository == null || getRepositoryTag() == null)
      return;
    
    String prefix = getRepositoryTag() + "/";
    
    Map<String,RepositoryTagEntry> tagMap = _repository.getTagMap();

    for (String key : tagMap.keySet()) {
      if (key.startsWith(prefix)) {
        String pathName = key.substring(prefix.length());

        // collect all the new repository expand directories
        // server/12p6 make sure webapps are prefixed with a '/'
        entryNames.add(_entryNamePrefix + pathName);
      }
    }
  }

  public String getPrimaryVersion(String name)
  {
    VersionEntry entry = getVersionEntry(name);

    if (entry != null) {
      VersionEntry versionEntry = getVersionEntryByRoot(entry.getRoot());

      if (versionEntry != null)
        return versionEntry.getContextPath();
    }
    
    return null;
  }
  
  /**
   * Version map uses the repository
   */
  private TreeMap<String,VersionEntry> buildVersionMap()
  {
    if (_repository == null || getRepositoryTag() == null)
      return null;
    
    String prefix = getRepositoryTag() + "/";
    
    Map<String,RepositoryTagEntry> tagMap = _repository.getTagMap();

    TreeMap<String,VersionEntry> versionMap;
    versionMap = new TreeMap<String,VersionEntry>();

    for (Map.Entry<String,RepositoryTagEntry> entry : tagMap.entrySet()) {
      String tagName = entry.getKey();

      if (tagName.startsWith(prefix)) {
        String name = tagName.substring(prefix.length());
        String baseName = name;

        int p = baseName.lastIndexOf('-');
        if (p > 0 && p + 1 < baseName.length()
            && '0' <= baseName.charAt(p + 1)
            && baseName.charAt(p + 1) <= '9') {
          baseName = baseName.substring(0, p);
        }

        // XXX: url-prefix?
        String versionContextPath = "/" + name;
        String baseContextPath = "/" + baseName;

        VersionEntry versionEntry
          = new VersionEntry(name, versionContextPath, baseContextPath,
                             entry.getValue().getTagEntryHash(),
                             entry.getValue().getRoot());
        
        versionMap.put(versionContextPath, versionEntry);
      }
    }

    return versionMap;
  }

  protected boolean isValidDirectory(Path rootDirectory, String pathName)
  {
    
    if (! rootDirectory.isDirectory() || pathName.startsWith(".")) {
      return false;
    }

    if (pathName.equalsIgnoreCase("web-inf")
        || pathName.equalsIgnoreCase("meta-inf"))
      return false;

    for (int j = 0; j < _requireFiles.size(); j++) {
      String file = _requireFiles.get(j);

      if (! rootDirectory.lookup(file).canRead())
        return false;
    }

    return true;
  }

  /**
   * Converts the expand-path name to the entry name, returns null if
   * the path name is not valid.
   */
  protected String pathNameToEntryName(String name)
  {
    if (_expandPrefix == null) {
    }
    else if (_expandPrefix.equals("")
             && (name.startsWith("_")
                 || name.startsWith(".")
                 || name.endsWith(".") && CauchoSystem.isWindows()
                 || name.equalsIgnoreCase("META-INF")
                 || name.equalsIgnoreCase("WEB-INF"))) {
      return null;
    }
    else if (name.startsWith(_expandPrefix)) {
      name = name.substring(_expandPrefix.length());
    }
    else
      return null;


    if (_expandSuffix == null || "".equals(_expandSuffix)) {
    }
    else if (name.endsWith(_expandSuffix))
      return name.substring(0, name.length() - _expandSuffix.length());
    else
      return null;

    if (_extension != null && name.endsWith(_extension))
      return name.substring(0, name.length() - _extension.length());
    else
      return name;
  }

  /**
   * Converts the archive name to the entry name, returns null if
   * the archive name is not valid.
   */
  protected String entryNameToArchiveName(String entryName)
  {
    return entryName + getExtension();
  }

  /**
   * Converts the entry name to the archive name, returns null if
   * the entry name is not valid.
   */
  protected String archiveNameToEntryName(String archiveName)
  {
    if (! archiveName.endsWith(_extension))
      return null;
    else {
      int sublen = archiveName.length() - _extension.length();
      return pathNameToEntryName(archiveName.substring(0, sublen));
    }
  }

  /**
   * Creates a new entry.
   */
  abstract protected E createController(String name);

  private String nameToEntryName(String name)
  {
    return archiveNameToEntryName(name + getExtension());
  }

  private String entryNameToName(String name)
  {
    String archiveName = entryNameToArchiveName(name);

    if (archiveName == null)
      return null;
    else
      return archiveName.substring(0, archiveName.length() - getExtension().length());
  }

  public String[] getNames()
  {
    String[] names = new String[_controllerNames.size()];

    int i = 0;

    for (String controllerName : _controllerNames) {
      names[i++] = entryNameToName(controllerName);
    }

    return names;
  }

  private String getNamesAsString()
  {
    StringBuilder builder = new StringBuilder();

    for (String name : getNames()) {
      if (builder.length() > 0)
        builder.append(", ");

      builder.append(name);
    }

    builder.insert(0, '[');
    builder.append(']');

    return builder.toString();
  }

  /**
   * Deploy the archive.
   */
  public boolean deploy(String name)
  {
    update();
    
    DeployController<?> controller
      = getDeployContainer().findController(nameToEntryName(name));

    if (controller == null) {
      if (log.isLoggable(Level.FINE))
        log.finer(L.l("{0} can't deploy '{1}' because it's not a known controller: {2}",
                      this, name, getNamesAsString()));

      return false;
    }
    
    return true;
  }

  /**
   * Start the archive.
   */
  public boolean start(String name)
  {
    DeployController<?> controller
      = getDeployContainer().findController(nameToEntryName(name));

    if (controller == null) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, L.l("{0} unknown name '{1}' in start", this, name));

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l("{0} known names are {1} in start", this, getNamesAsString()));

      return false;
    }

    controller.start();
    
    return true;
  }

  /**
   * Returns an exception for the named archive or null if there is no exception
   */
  public Throwable getConfigException(String name)
  {
    DeployController<?> controller
      = getDeployContainer().findController(nameToEntryName(name));

    if (controller == null) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, L.l("unknown name '{0}'", name));

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l("known names are {0}", getNamesAsString()));

      return new ConfigException(L.l("unknown name '{0}'", name));
    }

    return controller.getConfigException();
  }
  /**
   * Stop the archive.
   */
  public boolean stop(String name)
  {
    DeployController<?> controller
      = getDeployContainer().findController(nameToEntryName(name));

    if (controller == null) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, L.l("unknown name '{0}'", name));

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l("known names are {0}", getNamesAsString()));

      return false;
    }

    controller.stop();
    return true;
  }

  /**
   * Undeploy the archive.
   */
  public boolean undeploy(String name)
  {
    DeployController<?> controller
      = getDeployContainer().findController(nameToEntryName(name));

    if (controller == null) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, L.l("unknown name '{0}'", name));

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l("known names are {0}", getNamesAsString()));

      return false;
    }

    Path archivePath = getArchivePath(name);
    Path expandPath = getExpandPath(name);

    controller.stop();

    try {
      if (log.isLoggable(Level.FINEST))
        log.log(Level.FINEST, L.l("deleting {0}", archivePath));

      archivePath.removeAll();
    }
    catch (IOException ex) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, ex.toString(), ex);
    }

    try {
      if (expandPath != null) {
        if (log.isLoggable(Level.FINEST))
          log.log(Level.FINEST, L.l("deleting {0}", expandPath));

        expandPath.removeAll();
      }
    }
    catch (IOException ex) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, ex.toString(), ex);
    }

    getDeployContainer().update(nameToEntryName(name));

    return true;
  }


  /**
   * Checks for updates.
   */
  public void handleAlarm(Alarm alarm)
  {
    if (isDestroyed())
      return;
    
    try {
      // XXX: tck, but no QA test

      // server/10ka
      if ("automatic".equals(getRedeployMode()) && isActive())
        request();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      _alarm.queue(_cronInterval);
    }
  }

  /**
   * Stops the deploy.
   */
  @Override
  protected void stopImpl()
  {
    _alarm.dequeue();
    
    if (_deployService != null)
      _deployService.removeUpdateListener(this);

    super.stopImpl();
  }

  /**
   * Tests for equality.
   */
  @Override
  public boolean equals(Object o)
  {
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    ExpandDeployGenerator<?> deploy = (ExpandDeployGenerator<?>) o;

    Path expandDirectory = getExpandDirectory();
    Path deployExpandDirectory = deploy.getExpandDirectory();

    if (expandDirectory != deployExpandDirectory &&
        (expandDirectory == null ||
         ! expandDirectory.equals(deployExpandDirectory)))
      return false;

    return true;
  }

  @Override
  public String toString()
  {
    String name = getClass().getName();
    int p = name.lastIndexOf('.');
    if (p > 0)
      name = name.substring(p + 1);
    
    return name + "[" + getExpandDirectory() + "]";
  }
}
