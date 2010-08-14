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

package com.caucho.loader;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;

import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * JarEntry.
 */
class JarEntry {
  private static final L10N L = new L10N(JarEntry.class);
  private static final Logger log
    = Logger.getLogger(JarEntry.class.getName());

  private Manifest _manifest;
  private JarPath _jarPath;
  private ArrayList<ClassPackage> _packages = new ArrayList<ClassPackage>();

  private CodeSource _codeSource;

  /**
   * Creates a JarEntry.
   */
  JarEntry(JarPath jarPath)
  {
    _jarPath = jarPath;

    try {
      _codeSource = new CodeSource(new URL(jarPath.getURL()),
                                   (Certificate []) jarPath.getCertificates());
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    readManifest();
  }

  /**
   * Reads the jar's manifest.
   */
  private void readManifest()
  {
    try {
      _manifest = _jarPath.getManifest();
      if (_manifest == null)
        return;

      Attributes attr = _manifest.getMainAttributes();
      if (attr != null)
        addManifestPackage("", attr);

      Map entries = _manifest.getEntries();
      
      Iterator iter = entries.keySet().iterator();
      while (iter.hasNext()) {
        String pkg = (String) iter.next();
        
        attr = _manifest.getAttributes(pkg);
        if (attr == null)
          continue;

        addManifestPackage(pkg, attr);
      }
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Adds package information from the manifest.
   */
  private void addManifestPackage(String name, Attributes attr)
  {
    // only add packages
    if (! name.endsWith("/") && ! name.equals(""))
      return;

    String specTitle = attr.getValue("Specification-Title");
    String specVersion = attr.getValue("Specification-Version");
    String specVendor = attr.getValue("Specification-Vendor");
    String implTitle = attr.getValue("Implementation-Title");
    String implVersion = attr.getValue("Implementation-Version");
    String implVendor = attr.getValue("Implementation-Vendor");

    // If all none, then it isn't a package entry
    if (specTitle == null && specVersion == null && specVendor != null &&
        implTitle == null && implVersion == null && implVendor != null)
      return;

    ClassPackage pkg = new ClassPackage(name);
    pkg.setSpecificationTitle(specTitle);
    pkg.setSpecificationVersion(specVersion);
    pkg.setSpecificationVendor(specVendor);
    pkg.setImplementationTitle(implTitle);
    pkg.setImplementationVersion(implVersion);
    pkg.setImplementationVendor(implVendor);

    _packages.add(pkg);
  }

  /**
   * Validates the jar.
   */
  public void validate()
    throws ConfigException
  {
    if (_manifest != null)
      validateManifest(_jarPath.getContainer().getURL(), _manifest);
  }

  /**
   * Validates the manifest.
   */
  public static void validateManifest(String manifestName, Manifest manifest)
    throws ConfigException
  {
    Attributes attr = manifest.getMainAttributes();
    if (attr == null)
      return;

    String extList = attr.getValue("Extension-List");
    if (extList == null)
      return;

    Pattern pattern = Pattern.compile("[, \t]+");
    String []split = pattern.split(extList);

    for (int i = 0; i < split.length; i++) {
      String ext = split[i];

      String name = attr.getValue(ext + "-Extension-Name");
      if (name == null)
        continue;

      Package pkg = Package.getPackage(name);

      if (pkg == null) {
        log.warning(L.l("package {0} is missing.  {1} requires package {0}.",
                        name, manifestName));
        continue;
      }

      String version = attr.getValue(ext + "-Specification-Version");

      if (version == null)
        continue;

      if (pkg.getSpecificationVersion() == null ||
          pkg.getSpecificationVersion().equals("")) {
        log.warning(L.l("installed {0} is not compatible with version `{1}'.  {2} requires version {1}.",
                     name, version, manifestName));
      }
      else if (! pkg.isCompatibleWith(version)) {
        log.warning(L.l("installed {0} is not compatible with version `{1}'.  {2} requires version {1}.",
                     name, version, manifestName));
      }
    }
  }

  public ClassPackage getPackage(String name)
  {
    ClassPackage bestPackage = null;
    int bestLength = -1;

    for (int i = 0; i < _packages.size(); i++) {
      ClassPackage pkg = _packages.get(i);

      String prefix = pkg.getPrefix();

      if (name.startsWith(prefix) && bestLength < prefix.length()) {
        bestPackage = pkg;
        bestLength = prefix.length();
      }
    }

    return bestPackage;
  }

  public JarPath getJarPath()
  {
    return _jarPath;
  }

  /**
   * Returns the code source.
   */
  public CodeSource getCodeSource(String path)
  {
    try {
      Path jarPath = _jarPath.lookup(path);
      
      Certificate []certificates = jarPath.getCertificates();

      return new CodeSource(new URL(jarPath.getURL()), certificates);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  /**
   * Tests for equality.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof JarEntry))
      return false;

    JarEntry entry = (JarEntry) o;

    return _jarPath.equals(entry._jarPath);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _jarPath + "]";
  }
}
