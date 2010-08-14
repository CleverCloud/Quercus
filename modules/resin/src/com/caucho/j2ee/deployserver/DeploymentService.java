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

package com.caucho.j2ee.deployserver;

import com.caucho.config.ConfigException;
import com.caucho.j2ee.deployclient.ProgressObjectImpl;
import com.caucho.j2ee.deployclient.TargetImpl;
import com.caucho.j2ee.deployclient.TargetModuleIDImpl;
import com.caucho.jmx.Jmx;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.management.server.*;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.TargetException;
import javax.enterprise.deploy.spi.status.ProgressObject;
import javax.management.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class DeploymentService
{
  private static final L10N L = new L10N(DeploymentService.class);
  private static final Logger log
    = Logger.getLogger(DeploymentService.class.getName());

  private static final EnvironmentLocal<DeploymentService> _local
    = new EnvironmentLocal<DeploymentService>();

  public static DeploymentService getDeploymentService()
  {
    synchronized (_local) {
      DeploymentService deploymentService = _local.get();

      if (deploymentService == null) {
        deploymentService = new DeploymentService();
        _local.set(deploymentService);
      }

      return deploymentService;
    }
  }

  private DeploymentService()
  {
  }

  public TargetImpl[] getTargets()
    throws IllegalStateException
  {
    MBeanServer mbeanServer = Jmx.getMBeanServer();
    ArrayList<String> hosts = new ArrayList<String>();

    try {
      Set<ObjectName> objectNames = mbeanServer.queryNames(new ObjectName("resin:type=EarDeploy,*"), null);

      for (ObjectName objectName : objectNames) {
        String host = objectName.getKeyProperty("Host");
        if (! hosts.contains(host))
          hosts.add(host);
      }
    }
    catch (MalformedObjectNameException e) {
      if (log.isLoggable(Level.WARNING))
        log.log(Level.WARNING, e.toString(), e);
    }

    try {
      Set<ObjectName> objectNames = mbeanServer.queryNames(new ObjectName("resin:type=WebAppDeploy,*"), null);

      for (ObjectName objectName : objectNames) {
        String host = objectName.getKeyProperty("Host");
        if (! hosts.contains(host))
          hosts.add(host);
      }
    }
    catch (MalformedObjectNameException e) {
      if (log.isLoggable(Level.WARNING))
        log.log(Level.WARNING, e.toString(), e);

    }

    try {
      Set<ObjectName> objectNames = mbeanServer.queryNames(new ObjectName("resin:type=ResourceDeploy,*"), null);

      for (ObjectName objectName : objectNames) {
        String host = objectName.getKeyProperty("Host");
        if (! hosts.contains(host))
          hosts.add(host);
      }
    }
    catch (MalformedObjectNameException e) {
      if (log.isLoggable(Level.WARNING))
        log.log(Level.WARNING, e.toString(), e);
    }

    TargetImpl[] targets = new TargetImpl[hosts.size()];
    for (int i = 0; i < hosts.size(); i++) {
      String host = hosts.get(i);
      String description = "Virtual Host: " + host;
      
      targets[i] = new TargetImpl(host, description);
    }

    return targets;
  }

  public TargetModuleID []getAvailableModules(String type)
    throws TargetException, IllegalStateException
  {
    return new TargetModuleID[] {};
  }

  private String getExceptionMessage(Throwable exception)
  {
    if (exception == null)
      return "";
    else if (exception instanceof ConfigException)
      return exception.getMessage();
    else
      return exception.toString();
  }

  public ProgressObject distribute(TargetImpl []hostTargets,
                                   InputStream archiveIs,
                                   DeploymentPlan plan)
    throws IllegalStateException
  {
    ProgressObjectImpl progress;
    
    if (hostTargets == null || hostTargets.length != 1) {
      String msg = L.l("jsr88 can only support single-host deploy");
      
      log.warning(msg);

      progress = new ProgressObjectImpl(new TargetModuleID[0]);
    
      progress.failed(msg);

      return progress;
    }

    String hostName = hostTargets[0].getName();

    String name = plan.getName();
    String moduleID;

    ArchiveDeployMXBean mxbean = null;

    try {
      if ("ear".equals(plan.getArchiveType())) {
        moduleID = "resin:name=" + name + ",type=EApp,Host=" + hostName;
      
        String jmxName = "resin:type=EarDeploy,Host=" + hostName + ",*";
        mxbean = loadArchiveMXBean(jmxName);
      }
      else if ("war".equals(plan.getArchiveType())) {
        moduleID = "resin:name=/" + name + ",type=WebApp,Host=" + hostName;
      
        String jmxName = "resin:type=WebAppDeploy,Host=" + hostName + ",*";
        mxbean = loadArchiveMXBean(jmxName);
      }
      else if ("rar".equals(plan.getArchiveType())) {
        moduleID = "resin:name=" + name + ",type=Resource,Host=" + hostName;
      
        String jmxName = "resin:type=ResourceDeploy,Host=" + hostName + ",*";
        mxbean = loadArchiveMXBean(jmxName);
      }
      else
        throw new UnsupportedOperationException(plan.getArchiveType());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }


    boolean failed = false;
    StringBuilder message = new StringBuilder();

    Path archivePath = null;

    Throwable exception = null;

    TargetImpl childTarget = new TargetImpl(moduleID, "");
    
    TargetModuleIDImpl childModuleID
      = new TargetModuleIDImpl(childTarget, moduleID);
    
    if (mxbean != null) {
      try {
        Path deployPath = Vfs.lookup(mxbean.getArchivePath(name));

        deployPath.getParent().mkdirs();

        if (archivePath == null) {
          createArchive(deployPath, plan, archiveIs);
          archivePath = deployPath;
        }
        else {
          WriteStream deployStream = deployPath.openWrite();

          try {
            deployStream.writeFile(archivePath);
          }
          finally {
            deployStream.close();
          }
        }

        mxbean.update();

        exception = mxbean.getConfigException(name);
      }
      catch (Exception e) {
        if (log.isLoggable(Level.INFO))
          log.log(Level.INFO, e.toString(), e);

        exception = e;
      }

      if (exception != null) {
        failed = true;
        describe(message, childModuleID, false, getExceptionMessage(exception));

        /*
          if (mxbean != null) {
          try {
          mxbean.undeploy(moduleID);
          }
          catch (Throwable t) {
          log.log(Level.FINE, t.toString(), t);
          }
          }
        */
      }
      else {
        if ("ear".equals(plan.getArchiveType())) {
          try {
            EAppMXBean eApp = (EAppMXBean) Jmx.find(moduleID);

            if (eApp != null)
              childTarget.setClientRefs(eApp.getClientRefs());
          } catch (Exception e) {
            log.log(Level.FINEST, e.toString(), e);
          }
        }

        describe(message, childModuleID, true);
      }
    }
    else {
      failed = true;
      log.warning(L.l("jsr88 cannot deploy '{0}', can't find deployment resource (e.g. web-app-deploy, ear-deploy).", moduleID));
    }
    
    TargetModuleID []targetModuleIDs
      = new TargetModuleID[] { childModuleID };

    progress = new ProgressObjectImpl(targetModuleIDs);
    
    if (failed)
      progress.failed(message.toString());
    else
      progress.completed(message.toString());

    return progress;
  }

  private ArchiveDeployMXBean loadArchiveMXBean(String pattern)
  {
    try {
      ObjectName objectName = null;
      
      for (ObjectName subName
             : Jmx.getMBeanServer().queryNames(new ObjectName(pattern), null)) {
        if (objectName == null)
          objectName = subName;
      }

      if (objectName != null)
        return (ArchiveDeployMXBean) Jmx.find(objectName);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return null;
  }

  private ArchiveDeployMXBean getMXBean(ObjectName targetName)
    throws MalformedObjectNameException
  {
    String type = targetName.getKeyProperty("type");
    String host = targetName.getKeyProperty("Host");
    
    if (type.equals("EApp")) {
      String archiveName = "resin:type=EarDeploy,Host=" + host + ",*";

      return loadArchiveMXBean(archiveName);
    }
    else
      return null;
  }

  private void createArchive(Path archivePath, DeploymentPlan plan, InputStream archiveIs)
    throws IOException
  {
    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, L.l("jsr88 deploying archive {0}", archivePath));

    Path originalPath = archivePath;

    String earFileName = archivePath.getTail();

    if (earFileName.endsWith(".ear")) {
      // Uses ".__caucho_ear" to avoid premature expansion.
      String s = earFileName;
      s = s.substring(0, s.length() - 3) + "__caucho_ear";
      archivePath = archivePath.getParent().lookup(s);

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l("jsr88 creating temp archive {0}", archivePath));
    }
    else {
      earFileName = null;
    }

    WriteStream archiveStream =  null;
    ZipInputStream zipInputStream = null;
    ZipOutputStream zipOutputStream = null;

    try {
      archiveStream = archivePath.openWrite();
      zipOutputStream = new ZipOutputStream(archiveStream);

      zipInputStream = new ZipInputStream(archiveIs);

      ZipEntry zipEntry = zipInputStream.getNextEntry();

      TreeSet<String> entryNames = new TreeSet<String>();

      int copyCount = 0;

      while (zipEntry != null) {
        if (log.isLoggable(Level.FINEST))
          log.log(Level.FINEST, L.l("jsr88 copying entry {0}", zipEntry));

        entryNames.add(zipEntry.getName());

        zipOutputStream.putNextEntry(zipEntry);

        try {
          for (int ch = zipInputStream.read(); ch != -1; ch = zipInputStream.read())
            zipOutputStream.write(ch);
        } catch (IOException e) {
          // XXX: unexpected end of ZLIB input stream.
          log.log(Level.WARNING, L.l("exception copying entry {0}", zipEntry), e);
        }

        zipEntry = zipInputStream.getNextEntry();

        copyCount++;
      }

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l("copied {0} entries", copyCount));

      if (archiveIs.read() != -1) {
        if (log.isLoggable(Level.FINE))
          log.log(Level.FINE, L.l("unexpected data at end of archive"));

        while (archiveIs.read() != -1) {}
      }

      int fileCount = 0;

      for (DeploymentPlan.PlanFile file : plan.getFileList()) {
        String zipEntryName = file.getPath();
        if (zipEntryName.startsWith("/"))
          zipEntryName = zipEntryName.substring(1);

        if (log.isLoggable(Level.FINEST))
          log.log(Level.FINEST, L.l("jsr88 plan file {0} output to {1}", file, zipEntryName));

        if (entryNames.contains(zipEntryName))
          log.log(Level.WARNING, L.l("plan file {0} overwrites existing file", zipEntryName));

        entryNames.add(zipEntryName);

        zipEntry = new ZipEntry(zipEntryName);
        zipOutputStream.putNextEntry(zipEntry);
        file.writeToStream(zipOutputStream);

        fileCount++;
      }

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l("created {0} entries from plan", fileCount));

      zipInputStream.close();
      zipInputStream = null;

      zipOutputStream.close();
      zipOutputStream = null;

      archiveStream.close();
      archiveStream = null;
    }
    finally {
      if (zipInputStream != null) {
        try {
          zipInputStream.close();
        }
        catch (Exception ex) {
          log.log(Level.FINER, ex.toString(), ex);
        }
      }

      if (zipOutputStream != null) {
        try {
          zipOutputStream.close();
        }
        catch (Exception ex) {
          log.log(Level.FINER, ex.toString(), ex);
        }
      }

      if (archiveStream != null) {
        try {
          archiveStream.close();
        }
        catch (Exception ex) {
          log.log(Level.FINER, ex.toString(), ex);
        }
      }

      if (earFileName != null) {
        if (log.isLoggable(Level.FINER))
          log.log(Level.FINER, L.l("jsr88 renaming temp archive {0} to {1}", archivePath, originalPath));

        // Renames ".__caucho_ear" to ".ear" to allow expansion.
        archivePath.renameTo(originalPath);
      }
    }
  }

  public ProgressObject start(TargetModuleID[] ids)
  {
    ProgressObjectImpl progress =  new ProgressObjectImpl(ids);

    boolean failed = false;
    StringBuilder message = new StringBuilder();

    for (TargetModuleID targetModuleID : ids) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, L.l("jsr88 starting {0}", targetModuleID.getModuleID()));

      Throwable exception = null;
      DeployControllerMXBean mxbean = null;

      try {
        ObjectName objectName = new ObjectName(targetModuleID.getModuleID());
        mxbean = (DeployControllerMXBean) Jmx.find(objectName);
        if (mxbean != null)
          mxbean.start();
        else {
          log.finer("Jsr88[] " + objectName + " is an unknown module");
          failed = true;
        }
      }
      catch (Exception t) {
        log.log(Level.INFO, t.toString(), t);
        // XXX: need to handle depending on type
        exception = t;
      }

      /*
      if (exception == null && mxbean != null) {
        // XXX: temp for types
        exception = null;
      }
      */

      if (exception != null) {
        failed  = true;
        describe(message, targetModuleID, false, getExceptionMessage(exception));
      }
      else
        describe(message, targetModuleID, true);
    }

    if (failed)
      progress.failed(L.l("start {0}", message));
    else
      progress.completed(L.l("start {0}", message));

    return progress;
  }

  public ProgressObject stop(TargetModuleID[] ids)
  {
    ProgressObjectImpl progress =  new ProgressObjectImpl(ids);

    boolean failed = false;
    StringBuilder message = new StringBuilder();

    for (TargetModuleID targetModuleID : ids) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, L.l("jsr88 stopping {0}", targetModuleID.getModuleID()));

      Throwable exception = null;
      DeployControllerMXBean mxbean = null;

      try {
        ObjectName objectName = new ObjectName(targetModuleID.getModuleID());
        mxbean = (DeployControllerMXBean) Jmx.find(objectName);
        if (mxbean != null)
          mxbean.stop();
        else {
          log.finer("Jsr88[] " + objectName + " is an unknown module");
          failed = true;
        }
      }
      catch (Exception t) {
        log.log(Level.INFO, t.toString(), t);
        // XXX: need to handle depending on type
        exception = t;
      }

      /*
      if (exception == null && mxbean != null) {
        // XXX: temp for types
        exception = null;
      }
      */

      if (exception != null) {
        failed  = true;
        describe(message, targetModuleID, false, getExceptionMessage(exception));
      }
      else
        describe(message, targetModuleID, true);
    }

    if (failed)
      progress.failed(L.l("stop {0}", message));
    else
      progress.completed(L.l("stop {0}", message));

    return progress;
  }

  public ProgressObject undeploy(TargetModuleID []ids)
    throws IllegalStateException
  {
    ProgressObjectImpl progress =  new ProgressObjectImpl(ids);

    boolean failed = false;
    StringBuilder message = new StringBuilder();

    for (TargetModuleID targetModuleID : ids) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, L.l("undeploying {0}", targetModuleID.getModuleID()));

      ArchiveDeployMXBean mxbean = null;
      Throwable exception = null;

      try {
        ObjectName objectName
          = new ObjectName(targetModuleID.getModuleID());
        mxbean = getMXBean(objectName);
        if (mxbean != null)
          mxbean.undeploy(objectName.getKeyProperty("name"));
      }
      catch (Throwable t) {
        log.log(Level.INFO, t.toString(), t);
        exception = t;
      }

      if (exception != null) {
        failed  = true;
        describe(message, targetModuleID, false, getExceptionMessage(exception));
      }
      else
        describe(message, targetModuleID, true);
    }

    if (failed)
      progress.failed(L.l("undeploy {0}", message));
    else
      progress.completed(L.l("undeploy {0}", message));

    return progress;
  }

  private void describe(StringBuilder builder,
                        TargetModuleID targetModuleID,
                        boolean success)
  {
    describe(builder, targetModuleID, success, null);
  }

  private void describe(StringBuilder builder,
                        TargetModuleID targetModuleID,
                        boolean success,
                        String message)
  {
    if (builder.length() > 0)
      builder.append(", ");

    if (success)
      builder.append(L.l("successful for target {0} module {1}",
                         targetModuleID.getTarget().getName(),
                         targetModuleID.getModuleID()));
    else {
      builder.append(L.l("failed for target {0} module {1}",
                         targetModuleID.getTarget().getName(),
                         targetModuleID.getModuleID()));
    }

    if (message != null) {
      builder.append(" '");
      builder.append(message);
      builder.append("'");
    }
  }
}
