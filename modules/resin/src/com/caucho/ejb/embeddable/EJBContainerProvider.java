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
 * @author Emil Ong
 */

package com.caucho.ejb.embeddable;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import javax.ejb.embeddable.EJBContainer;
import javax.naming.Context;
import javax.xml.stream.*;

import com.caucho.java.WorkDir;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.xml.stream.*;

public class EJBContainerProvider implements javax.ejb.spi.EJBContainerProvider 
{
  public static final String WORK_DIR = "com.caucho.ejb.embeddable.workDir";

  private static final Logger log 
    = Logger.getLogger(EJBContainerProvider.class.getName());

  @Override
  public EJBContainer createEJBContainer(Map<?,?> properties)
    throws EJBException
  {
    EJBContainerImpl container = null;


    if (properties == null) {
      container = new EJBContainerImpl();

      addModulesFromClasspath(container, null);

      setWorkDir();
    }
    else {
      String provider = (String) properties.get(EJBContainer.PROVIDER);

      // This is the EJBContainer implementation class and not the
      // EJBContainerProvider implementation for some reason. The spec
      // is ambiguous, but the TCK is not.
      if (provider != null 
          && ! EJBContainerImpl.class.getName().equals(provider))
        return null;

      String name = (String) properties.get(EJBContainer.APP_NAME);

      container = new EJBContainerImpl(name);

      // FYI: only a single module is required for web profile
      addModules(container, properties.get(EJBContainer.MODULES));

      setWorkDir(properties.get(WORK_DIR));
    }

    container.start();

    return container;
  }

  private String scanEjbJarXml(Path ejbJarXml)
  {
    // XXX use Config.configure once com.caucho.ejb.cfg allows configuration
    // without a live EjbManager
    try {
      XMLStreamReaderImpl reader 
        = new XMLStreamReaderImpl(ejbJarXml.openRead());

      while (reader.hasNext()) { 
        if (reader.next() == XMLStreamReader.START_ELEMENT
            && "module-name".equals(reader.getLocalName()))
          return reader.getElementText();
      }
    }
    catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    catch (XMLStreamException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return null;
  }

  private String getModuleName(String classpathComponent)
  {
    Path path = Vfs.lookup(classpathComponent);
    String tail = path.getTail();
    String moduleName = classpathComponent;

    if (classpathComponent.endsWith(".jar")
        || classpathComponent.endsWith(".war")) {
      moduleName = tail.substring(0, tail.length() - ".jar".length());

      path = JarPath.create(path);
    }

    Path ejbJarXml = path.lookup("META-INF/ejb-jar.xml");

    if (ejbJarXml.canRead()) {
      String ejbJarModuleName = scanEjbJarXml(ejbJarXml);

      if (ejbJarModuleName != null)
        moduleName = ejbJarModuleName;
    }

    return moduleName;
  }

  private void addModulesFromClasspath(EJBContainerImpl container,
                                       Set<String> modules)
  {
    String classpath = System.getProperty("java.class.path");

    String[] components 
      = classpath.split(System.getProperty("path.separator"));

    for (String component : components) {
      String moduleName = getModuleName(component);
      
      if (moduleName.equals(""))
        continue;

      if (modules == null || modules.contains(moduleName)) {
        container.addModule(Vfs.lookup(component));
      }
    }
  }

  private void addModules(EJBContainerImpl container, Object modulesValue)
    throws EJBException
  {
    if (modulesValue == null) {
      addModulesFromClasspath(container, null);
    }
    else if (modulesValue instanceof String) {
      HashSet<String> modules = new HashSet<String>();
      modules.add((String) modulesValue);

      addModulesFromClasspath(container, modules);
    }
    else if (modulesValue instanceof String[]) {
      HashSet<String> modules = new HashSet<String>();

      for (String module : (String []) modulesValue) {
        modules.add(module);
      }

      addModulesFromClasspath(container, modules);
    }
    else if (modulesValue instanceof File) {
      File file = (File) modulesValue;

      container.addModule(Vfs.lookup(file.getPath()));
    }
    else if (modulesValue instanceof File[]) {
      File []files = (File []) modulesValue;

      for (File file : files ) {
        container.addModule(Vfs.lookup(file.getPath()));
      }
    }
    else 
      throw new EJBException("Value of '" + EJBContainer.MODULES + "' (EJBContainer.MODULES) must be one of the types String, String[], java.io.File, or java.io.File[]");
  }

  private void setWorkDir()
  {
    setWorkDir(null);
  }

  private void setWorkDir(Object workDirValue)
    throws EJBException
  {
    if (workDirValue != null) {
      Path workDir = null;

      if (workDirValue instanceof String) {
        workDir = Vfs.lookup((String) workDirValue);
      }
      else if (workDirValue instanceof File) {
        workDir = Vfs.lookup(((File) workDirValue).getPath());
      }
      else {
        throw new EJBException("Value of '" + WORK_DIR + "' must be either a String or java.io.File");
      }

      WorkDir.setLocalWorkDir(workDir);
    }
    else {
      Path tmpDir = Vfs.lookup(System.getProperty("java.io.tmpdir"));
      String userName = System.getProperty("user.name");
      Path workDir = tmpDir.lookup(userName).lookup("caucho-ejb");

      WorkDir.setLocalWorkDir(workDir);
    }
  }
}
