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

package com.caucho.jsp;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.types.FileSetType;
import com.caucho.jsp.cfg.*;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.server.util.CauchoSystem;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.vfs.*;
import com.caucho.jsf.cfg.JsfPropertyGroup;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Stores the parsed tlds.
 */
public class TldManager {
  static final L10N L = new L10N(TldManager.class);
  private static final Logger log
    = Logger.getLogger(TldManager.class.getName());

  private static ArrayList<TldPreload> _cauchoTaglibs;
  private static ArrayList<TldPreload> _globalTaglibs;
  private static ArrayList<Path> _globalPaths;

  private static EnvironmentLocal<TldManager> _localManager
    = new EnvironmentLocal<TldManager>();

  private JspResourceManager _resourceManager;
  private WebApp _webApp;

  private HashMap<Path,SoftReference<TldTaglib>> _tldMap
    = new HashMap<Path,SoftReference<TldTaglib>>();

  private JspParseException _loadAllTldException;
  private String _tldDir;
  private FileSetType _tldFileSet;

  private boolean _isFastJsf = false; // ioc/0560

  private volatile boolean _isInit;

  private Config _config = new Config();
  private ArrayList<TldPreload> _preloadTaglibs;

  private TldManager(JspResourceManager resourceManager,
                     WebApp app)
    throws JspParseException, IOException
  {
    _resourceManager = resourceManager;
    _webApp = app;

    if (app != null) {
      JspPropertyGroup jsp = app.getJsp();
      if (jsp != null)
        _tldFileSet = jsp.getTldFileSet();


      JsfPropertyGroup jsf = app.getJsf();
      if (jsf != null)
        _isFastJsf = jsf.isFastJsf();
    }

    // JSF has a global listener hidden in one of the *.tld which
    // requires Resin to search all the JSPs.
    initGlobal();
  }

  static TldManager create(JspResourceManager resourceManager,
                           WebApp app)
    throws JspParseException, IOException
  {

    TldManager manager = null;

    synchronized (_localManager) {
      manager = _localManager.getLevel();

      if (manager != null)
        return manager;

      manager = new TldManager(resourceManager, app);
      _localManager.set(manager);
    }

    return manager;
  }

  /**
   * Sets the webApp.
   */
  void setWebApp(WebApp webApp)
  {
    _webApp = webApp;
  }

  public String getSchema()
  {
    return "com/caucho/jsp/cfg/jsp-tld.rnc";
  }

  public void setTldDir(String tldDir)
  {
    _tldDir = tldDir;
  }

  public void setTldFileSet(FileSetType tldFileSet)
  {
    _tldFileSet = tldFileSet;
  }

  /**
   * Loads all the .tld files in the WEB-INF and the META-INF for
   * the entire classpath.
   */
  public synchronized void init()
    throws JspParseException, IOException
  {
    if (_isInit)
      return;
    _isInit = true;

    log.fine("Loading .tld files");

    String dir;

    if (_tldDir == null)
      dir = "WEB-INF";
    else if (_tldDir.startsWith("/"))
      dir = _tldDir.substring(1);
    else if (_tldDir.startsWith("WEB-INF"))
      dir = _tldDir;
    else
      dir = "WEB-INF/" + _tldDir;

    FileSetType fileSet = _tldFileSet;
    if (fileSet == null) {
      fileSet = new FileSetType();
      fileSet.setDir(_resourceManager.resolvePath(dir));
      try {
        fileSet.init();
      } catch (Exception e) {
        log.config(e.toString());
      }
    }

    ArrayList<TldPreload> taglibs = new ArrayList<TldPreload>();

    taglibs.addAll(_globalTaglibs);

    ArrayList<Path> paths = getClassPath();

    for (int i = 0; i < paths.size(); i++) {
      Path subPath = paths.get(i);

      if (_globalPaths.contains(subPath))
        continue;

      if (subPath instanceof JarPath)
        loadJarTlds(taglibs, ((JarPath) subPath).getContainer(), "META-INF");
      else if (subPath.getPath().endsWith(".jar")) {
        loadJarTlds(taglibs, subPath, "META-INF");
      }
      else
        loadAllTlds(taglibs, subPath.lookup("META-INF"), 64, "META-INF");
    }

    if (fileSet != null)
      loadAllTlds(taglibs, fileSet);

    /*
    for (int i = 0; i < taglibs.size(); i++) {
      TldTaglib taglib = taglibs.get(i);

      if (taglib.getConfigException() != null &&
          taglib.getURI() == null) {
        _loadAllTldException = JspParseException.create(taglib.getConfigException());
      }
    }
    */
    taglibs.addAll(_cauchoTaglibs);

    _preloadTaglibs = taglibs;

    for (int i = 0; i < taglibs.size(); i++) {
      try {
        taglibs.get(i).initListeners(_webApp);
      } catch (Exception e) {
        throw new JspParseException(e);
      }
    }
  }

  public synchronized void initGlobal()
  {
    // loads tag libraries from the global context (so there's no
    // need to reparse the jars for each web-app
    if (_globalTaglibs == null) {
      if (! Alarm.isTest()) {
        log.info("Loading .tld files from global classpath");
      }

      ArrayList<TldPreload> globalTaglibs = new ArrayList<TldPreload>();
      ArrayList<TldPreload> cauchoTaglibs = new ArrayList<TldPreload>();

      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();
      ClassLoader globalLoader = TldManager.class.getClassLoader();
      thread.setContextClassLoader(globalLoader);
      try {
        ArrayList<Path> paths = getClassPath(globalLoader);
        _globalPaths = paths;

        loadClassPathTlds(globalTaglibs, paths, "");

        for (int i = globalTaglibs.size() - 1; i >= 0; i--) {
          TldPreload tld = globalTaglibs.get(i);

          if (tld.getPath() == null || tld.getPath().getPath() == null)
            continue;

          String tldPathName = tld.getPath().getPath();

          if (tldPathName.startsWith("/com/caucho")) {
            cauchoTaglibs.add(globalTaglibs.remove(i));
          }
        }
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      } finally {
        thread.setContextClassLoader(oldLoader);
      }

      _globalTaglibs = globalTaglibs;
      _cauchoTaglibs = cauchoTaglibs;
    }
  }

  private void loadClassPathTlds(ArrayList<TldPreload> taglibs,
                                 ArrayList<Path> paths,
                                 String prefix)
    throws JspParseException, IOException
  {
    for (int i = 0; i < paths.size(); i++) {
      Path subPath = paths.get(i);

      // skip jre libraries
      String pathName = subPath.getFullPath();
      if (pathName.endsWith("/jre/lib/rt.jar")
          || pathName.endsWith("/jre/lib/charsets.jar")
          || pathName.endsWith("/jre/lib/deploy.jar")) {
        continue;
      }

      if (subPath.getPath().endsWith(".jar")) {
        loadJarTlds(taglibs, subPath, prefix);
      }
      else if (prefix != null && ! prefix.equals(""))
        loadAllTlds(taglibs, subPath.lookup(prefix), 64, prefix);
      else
        loadAllTlds(taglibs, subPath.lookup("META-INF"), 64, "META-INF");
    }
  }

  /*
  ArrayList<TldTaglib> getTaglibs()
  {
    return new ArrayList<TldTaglib>(_preloadTaglibs);
  }
  */

  private void loadAllTlds(ArrayList<TldPreload> taglibs,
                           FileSetType fileSet)
    throws JspParseException, IOException
  {
    for (Path path : fileSet.getPaths()) {
      if (path.getPath().startsWith(".")) {
      }
      else if ((path.getPath().endsWith(".tld")
                || path.getPath().endsWith(".ftld"))
               && path.isFile() && path.canRead()) {
        try {
          TldPreload taglib = parseTldPreload(path);

          taglibs.add(taglib);

          if (taglib.getURI() == null &&
              taglib.getConfigException() != null &&
              _loadAllTldException == null)
            _loadAllTldException = new JspLineParseException(taglib.getConfigException());
        } catch (Exception e) {
          log.warning(e.getMessage());
        }
      }
    }
  }

  private void loadAllTlds(ArrayList<TldPreload> taglibs,
                           Path path, int depth, String userPath)
    throws JspParseException, IOException
  {
    if (depth < 0)
      throw new JspParseException(L.l("max depth exceeded while reading .tld files.  Probable loop in filesystem detected at `{0}'.", path));

    path.setUserPath(userPath);

    if (path.getPath().startsWith(".")) {
    }
    else if ((path.getPath().endsWith(".tld")
              || path.getPath().endsWith(".ftld"))
             && path.isFile() && path.canRead()) {
      try {
        TldPreload taglib = parseTldPreload(path);

        taglibs.add(taglib);

        if (taglib.getURI() == null &&
            taglib.getConfigException() != null &&
            _loadAllTldException == null)
          _loadAllTldException = new JspLineParseException(taglib.getConfigException());
      } catch (Exception e) {
        /*
        if (_loadAllTldException == null) {
        }
        else if (e instanceof JspParseException)
          _loadAllTldException = (JspParseException) e;
        else
          _loadAllTldException = new JspParseException(e);
        */

        log.warning(e.getMessage());
      }
    }
    else if (path.isDirectory()) {
      String []fileNames = path.list();

      for (int i = 0; fileNames != null && i < fileNames.length; i++) {
        String name = fileNames[i];

        ArrayList<Path> resources = path.getResources(name);

        for (int j = 0; resources != null && j < resources.size(); j++) {
          Path subpath = resources.get(j);

          loadAllTlds(taglibs, subpath, depth - 1, userPath + "/" + name);
        }
      }
    }
  }

  private void loadJarTlds(ArrayList<TldPreload> taglibs,
                           Path jarBacking,
                           String prefix)
    throws JspParseException, IOException
  {
    if (! jarBacking.canRead())
      return;

    String nativePath = jarBacking.getNativePath();
    ZipFile zipFile;
    JarPath jar = JarPath.create(jarBacking);

    if (nativePath.endsWith(".jar"))
      zipFile = new JarFile(nativePath);
    else
      zipFile = new ZipFile(nativePath);

    ArrayList<Path> tldPaths = new ArrayList<Path>();

    boolean isValidScan = false;

    ZipScanner scan = null;
    try {
      if (true)
        scan = new ZipScanner(jarBacking);

      if (scan != null && scan.open()) {
        while (scan.next()) {
          String name = scan.getName();

          if (name.startsWith(prefix)
              && name.endsWith(".tld") || name.endsWith(".ftld")) {
            tldPaths.add(jar.lookup(name));
          }
        }

        isValidScan = true;
      }
    } catch (Exception e) {
      log.log(Level.INFO, e.toString(), e);
    }

    if (! isValidScan) {
      try {
        Enumeration<? extends ZipEntry> en = zipFile.entries();
        while (en.hasMoreElements()) {
          ZipEntry entry = en.nextElement();
          String name = entry.getName();

          if (name.startsWith(prefix)
              && (name.endsWith(".tld") || name.endsWith(".ftld"))) {
            tldPaths.add(jar.lookup(name));
          }
        }
      } finally {
        zipFile.close();
      }
    }

    for (Path path : tldPaths) {
      try {
        TldPreload taglib = parseTldPreload(path);

        taglibs.add(taglib);

        if (taglib.getURI() == null
            && taglib.getConfigException() != null
            && _loadAllTldException == null)
          _loadAllTldException = new JspLineParseException(taglib.getConfigException());
      } catch (Exception e) {
        /*
          if (_loadAllTldException == null) {
          }
          else if (e instanceof JspParseException)
          _loadAllTldException = (JspParseException) e;
          else
          _loadAllTldException = new JspParseException(e);
        */

        log.warning(e.getMessage());
      }
    }
  }

  /**
   * Returns the tld parsed at the given location.
   */
  TldTaglib parseTld(String uri, String mapLocation, String location)
    throws JspParseException, IOException
  {
    init();

    TldTaglib taglib = null;
    TldTaglib jsfTaglib = null;

    for (int i = 0; i < _preloadTaglibs.size(); i++) {
      TldPreload preload = _preloadTaglibs.get(i);

      if (uri.equals(preload.getURI())
          && (mapLocation == null
              || mapLocation.equals(preload.getLocation())
              || mapLocation.equals(uri))) {
        if (preload.isJsf()) {
          if (_isFastJsf)
            jsfTaglib = parseTld(preload.getPath());
        }
        else if (taglib == null)
          taglib = parseTld(preload.getPath());
      }
    }

    if (jsfTaglib != null && taglib != null) {
      taglib.mergeJsf(jsfTaglib);

      return taglib;
    }
    else if (taglib != null)
      return taglib;

    return parseTld(location);
  }

  /**
   * Returns the tld parsed at the given location.
   */
  TldTaglib parseTld(String location)
    throws JspParseException, IOException
  {
    init();

    TldTaglib tld = findTld(location);

    /* XXX: jsp/18n0 handled on init
    if (tld != null) {
      try {
        tld.init(_webApp);
      } catch (Exception e) {
        throw new JspParseException(e);
      }
    }
    */

    return tld;
  }

  /**
   * Returns the tld parsed at the given location.
   */
  private TldTaglib findTld(String location)
    throws JspParseException, IOException
  {
    InputStream is = null;

    Path path;

    if (location.startsWith("file:")) {
      path = _resourceManager.resolvePath(location);
    }
    else if (location.indexOf(':') >= 0 && ! location.startsWith("file:")
             && location.indexOf(':') < location.indexOf('/')) {
      if (_loadAllTldException != null)
        throw _loadAllTldException;

      return null;
      /* XXX: jsp/0316
      throw new JspParseException(L.l("Unknown taglib `{0}'.  Taglibs specified with an absolute URI must either be:\n1) specified in the web.xml\n2) defined in a jar's .tld in META-INF\n3) defined in a .tld in WEB-INF\n4) predefined by Resin",
                                      location));
      */
    }
    else if (! location.startsWith("/"))
      path = _resourceManager.resolvePath("WEB-INF/" + location);
    else
      path = _resourceManager.resolvePath("." + location);

    path.setUserPath(location);

    Path jar = null;

    if (location.endsWith(".jar")) {
      path = findJar(location);

      if (path != null && path.exists()) {
        jar = JarPath.create(path);
        if (jar.lookup("META-INF/taglib.tld").exists())
          return parseTld(jar.lookup("META-INF/taglib.tld"));
        else if (jar.lookup("meta-inf/taglib.tld").exists())
          return parseTld(jar.lookup("meta-inf/taglib.tld"));
        else
          throw new JspParseException(L.l("can't find META-INF/taglib.tld in `{0}'",
                                          location));
      }
      else {
        throw new JspParseException(L.l("Can't find taglib `{0}'.  A taglib uri ending in *.jar must point to an actual jar or match a URI in a .tld file.", location));
      }
    }
    else if (path.exists() && path.canRead() && path.isFile())
      return parseTld(path);

    if (_loadAllTldException != null)
      throw _loadAllTldException;
    else
      throw new JspParseException(L.l("Can't find taglib-location `{0}'.  The taglib-location must match a tag library either:\n1) by pointing to a .tld directly, relative to the application's root directory\n2) specified in the web.xml\n3) defined in a jar's .tld in META-INF\n4) defined in a .tld in WEB-INF\n5) predefined by Resin",
                                      location));
  }

  /**
   * Parses the .tld
   *
   * @param is the input stream to the taglib
   */
  private TldTaglib parseTld(Path path)
    throws JspParseException, IOException
  {
    SoftReference<TldTaglib> taglibRef = _tldMap.get(path);
    TldTaglib taglib;

    if (taglibRef != null) {
      taglib = taglibRef.get();

      if (taglib != null && ! taglib.isModified())
        return taglib;
    }

    ReadStream is = path.openRead();

    try {
      taglib = parseTld(is);

      if (path instanceof JarPath)
        taglib.setJarPath(path.lookup("/"));

      _tldMap.put(path, new SoftReference<TldTaglib>(taglib));

      return taglib;
    } finally {
      is.close();
    }
  }

  /**
   * Parses the .tld
   *
   * @param is the input stream to the taglib
   */
  private TldTaglib parseTld(InputStream is)
    throws JspParseException, IOException
  {
    TldTaglib taglib = new TldTaglib();

    if (is instanceof ReadStream) {
      Path path = ((ReadStream) is).getPath();

      path.setUserPath(path.getURL());
    }

    String schema = null;

    if (_webApp.getJsp() == null
        || _webApp.getJsp().isValidateTaglibSchema()) {
      schema = getSchema();
    }

    try {
      Config config = new Config();
      config.setEL(false);
      config.configure(taglib, is, schema);
    } catch (ConfigException e) {
      log.warning(e.toString());
      log.log(Level.FINER, e.toString(), e);

      taglib.setConfigException(e);
    } catch (Exception e) {
      log.warning(e.toString());
      log.log(Level.FINER, e.toString(), e);

      taglib.setConfigException(e);
    } finally {
      is.close();
    }

    /* XXX: jsp/18n0 handled on init
    try {
      taglib.init(_webApp);
    } catch (Exception e) {
      throw new JspParseException(e);
    }
    */

    return taglib;
  }

  /**
   * Parses the .tld
   *
   * @param path location of the taglib
   */
  private TldPreload parseTldPreload(Path path)
    throws JspParseException, IOException
  {
    ReadStream is = path.openRead();

    try {
      TldPreload taglib = parseTldPreload(is);

      taglib.setPath(path);
      String appDir = _webApp.getAppDir().getPath();
      String tagPath = path.getPath();

      if (tagPath.startsWith(appDir))
        taglib.setLocation(tagPath.substring(appDir.length()));

      return taglib;
    } finally {
      is.close();
    }
  }

  /**
   * Parses the .tld
   *
   * @param is the input stream to the taglib
   */
  private TldPreload parseTldPreload(InputStream is)
    throws JspParseException, IOException
  {
    boolean isJsfTld = false;

    if (is instanceof ReadStream) {
      Path path = ((ReadStream) is).getPath();

      isJsfTld = path.getPath().endsWith(".ftld");

      path.setUserPath(path.getURL());
    }

    String schema = null;

    if (_webApp.getJsp() == null
        || _webApp.getJsp().isValidateTaglibSchema()) {
      schema = getSchema();
    }

    TldPreload taglib;

    if (isJsfTld)
      taglib = new JsfTldPreload();
    else
      taglib = new TldPreload();

    try {
      _config.configure(taglib, is, schema);
    } catch (ConfigException e) {
      log.warning(e.toString());
      log.log(Level.FINER, e.toString(), e);

      taglib.setConfigException(e);
    } catch (Exception e) {
      log.warning(e.toString());
      log.log(Level.FINER, e.toString(), e);

      taglib.setConfigException(e);
    } finally {
      is.close();
    }

    return taglib;
  }

  /**
   * Finds the path to the jar specified by the location.
   *
   * @param location the tag-location specified in the web.xml
   *
   * @return the found jar or null
   */
  private Path findJar(String location)
  {
    Path path;

    if (location.startsWith("file:"))
      path = Vfs.lookup(location);
    else if (location.startsWith("/"))
      path = _resourceManager.resolvePath("." + location);
    else
      path = _resourceManager.resolvePath(location);

    if (path.exists())
      return path;

    DynamicClassLoader loader;
    loader = (DynamicClassLoader) Thread.currentThread().getContextClassLoader();
    String classPath = loader.getClassPath();
    char sep = CauchoSystem.getPathSeparatorChar();

    int head = 0;
    int tail = 0;

    while ((tail = classPath.indexOf(sep, head)) >= 0) {
      String sub = classPath.substring(head, tail);

      path = Vfs.lookup(sub);

      if (sub.endsWith(location) && path.exists())
        return path;

      head = tail + 1;
    }

    if (classPath.length() <= head)
      return null;

    String sub = classPath.substring(head);

    path = Vfs.lookup(sub);

    if (sub.endsWith(location) && path.exists())
      return path;
    else
      return null;
  }

  /**
   * Adds the classpath as paths in the MergePath.
   */
  private ArrayList<Path> getClassPath()
  {
    return getClassPath(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Adds the classpath for the loader as paths in the MergePath.
   *
   * @param loader class loader whose classpath should be used to search.
   */
  private ArrayList<Path> getClassPath(ClassLoader loader)
  {
    String classpath = null;

    if (loader instanceof DynamicClassLoader)
      classpath = ((DynamicClassLoader) loader).getClassPath();
    else
      classpath = CauchoSystem.getClassPath();

    return getClassPath(classpath);
  }

  /**
   * Adds the classpath for the loader as paths in the MergePath.
   *
   * @param classpath class loader whose classpath should be used to search.
   */
  private ArrayList<Path> getClassPath(String classpath)
  {
    ArrayList<Path> list = new ArrayList<Path>();

    char sep = CauchoSystem.getPathSeparatorChar();
    int head = 0;
    int tail = 0;
    while (head < classpath.length()) {
      tail = classpath.indexOf(sep, head);

      String segment = null;
      if (tail < 0) {
        segment = classpath.substring(head);
        head = classpath.length();
      }
      else {
        segment = classpath.substring(head, tail);
        head = tail + 1;
      }

      if (! segment.equals("")) {
        Path path = Vfs.lookup(segment);

        if (! list.contains(path))
          list.add(path);
      }
    }

    return list;
  }
}
