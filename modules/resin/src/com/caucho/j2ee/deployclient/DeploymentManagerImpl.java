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

package com.caucho.j2ee.deployclient;

import com.caucho.server.admin.DeployClient;
import com.caucho.server.admin.HostQuery;
import com.caucho.server.admin.WebAppQuery;
import com.caucho.server.admin.TagQuery;
import com.caucho.server.admin.StatusQuery;
import com.caucho.util.L10N;
import com.caucho.vfs.*;
import com.caucho.xml.*;
import com.caucho.xpath.XPath;
import com.caucho.bam.RemoteConnectionFailedException;
import com.caucho.bam.TimeoutException;

import org.w3c.dom.*;

import javax.enterprise.deploy.model.DeployableObject;
import javax.enterprise.deploy.shared.DConfigBeanVersionType;
import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.DeploymentConfiguration;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.DConfigBeanVersionUnsupportedException;
import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;
import javax.enterprise.deploy.spi.exceptions.InvalidModuleException;
import javax.enterprise.deploy.spi.exceptions.TargetException;
import javax.enterprise.deploy.spi.status.ProgressObject;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

/**
 * Manager for the deployments.
 */
public class DeploymentManagerImpl
  implements DeploymentManager
{
  private static final L10N L = new L10N(DeploymentManagerImpl.class);
  private static final Logger log
    = Logger.getLogger(DeploymentManagerImpl.class.getName());

  private DeployClient _deployClient;

  final private String _host;
  final private int _port;
  private String _user;
  private String _password;

  private String _uri;

  DeploymentManagerImpl(String uri)
  {
    int p = uri.indexOf("http");

    if (p < 0)
      throw new IllegalArgumentException(L.l(
        "'{0}' is an illegal URI for DeploymentManager.",
        uri));

    _uri = uri;

    int hostIdx = uri.indexOf("://") + 3;

    int portIdx = uri.indexOf(':', hostIdx);

    int fileIdx = uri.indexOf('/', portIdx + 1);

    _host = uri.substring(hostIdx, portIdx);


    if (fileIdx > -1)
      _port = Integer.parseInt(uri.substring(portIdx + 1, fileIdx));
    else
      _port = Integer.parseInt(uri.substring(portIdx + 1));
  }

  /**
   * Connect to the manager.
   */
  void connect(String user, String password)
    throws DeploymentManagerCreationException
  {
    _user = user;
    _password = password;

    _deployClient = new DeployClient(_host, _port, _user, _password);
  }

  // XXX: hack
  private void reset()
  {
    _deployClient = new DeployClient(_host, _port, _user, _password);
  }

  /**
   * Returns the targets supported by the manager.
   */
  public Target[] getTargets()
    throws IllegalStateException
  {
    try {
      HostQuery[] hosts = _deployClient.listHosts();

      if (hosts == null)
        throw new IllegalStateException(L.l("'{0}' does not return any hosts",
                                            _deployClient));

      Target[] targets = new Target[hosts.length];

      for (int i = 0; i < hosts.length; i++) {
        HostQuery host = hosts[i];

        Target target = new TargetImpl(host.getName(), null);

        targets[i] = target;
      }

      return targets;
    } // XXX: hack
    catch (RemoteConnectionFailedException e) {
      reset();

      return getTargets();
    }
    catch (TimeoutException e) {
      reset();

      return getTargets();
    }
  }

  /**
   * Returns the current running modules.
   */
  public TargetModuleID[] getRunningModules(ModuleType moduleType,
                                            Target[] targetList)
    throws TargetException, IllegalStateException
  {
    return getAvailableModules(moduleType, targetList);
  }

  /**
   * Returns the current non-running modules.
   */
  public TargetModuleID[] getNonRunningModules(ModuleType moduleType,
                                               Target[] targetList)
    throws TargetException, IllegalStateException
  {
    return new TargetModuleID[0];
  }

  /**
   * Returns all available modules.
   */
  public TargetModuleID[] getAvailableModules(ModuleType moduleType,
                                              Target[] targetList)
    throws TargetException, IllegalStateException
  {
    try {
      ArrayList<TargetModuleID> resultList = new ArrayList<TargetModuleID>();

      for (int i = 0; i < targetList.length; i++) {
        Target target = targetList[i];

        TagQuery[] tags = _deployClient.listTags(target.getName());

        for (int j = 0; tags != null && j < tags.length; j++) {
          String host = tags[j].getHost();
          String tag = tags[j].getTag();

          resultList.add(new TargetModuleIDImpl(new TargetImpl(host, null),
                                                tag));
        }
      }

      TargetModuleID[] result = new TargetModuleID[resultList.size()];
      resultList.toArray(result);

      return result;
    } // XXX: hack
    catch (RemoteConnectionFailedException e) {
      reset();

      return getAvailableModules(moduleType, targetList);
    }
  }

  /**
   * Returns a configuration for the deployable object.
   */
  public DeploymentConfiguration createConfiguration(DeployableObject dObj)
    throws InvalidModuleException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Deploys the object.
   */
  public ProgressObject distribute(Target[] targetList,
                                   File archive,
                                   File deploymentPlan)
    throws IllegalStateException
  {
    return distributeImpl(targetList, archive, null, deploymentPlan, null);
  }

  /**
   * Deploys the object.
   */
  public ProgressObject distribute(Target[] targetList,
                                   InputStream archive,
                                   InputStream deploymentPlan)
    throws IllegalStateException
  {
    return distributeImpl(targetList, null, archive, null, deploymentPlan);
  }


  /**
   * Deploys the object.
   */
  public ProgressObject distributeImpl(Target[] targetList,
                                       File archive,
                                       InputStream archiveStream,
                                       File deploymentPlan,
                                       InputStream deploymentPlanStream)
    throws IllegalStateException
  {
    try {
      QDocument doc = new QDocument();

      DOMBuilder builder = new DOMBuilder();

      builder.init(doc);

      Xml xml = new Xml();
      xml.setOwner(doc);
      xml.setNamespaceAware(false);
      xml.setContentHandler(builder);
      xml.setCoalescing(true);

      if (deploymentPlan != null)
        xml.parse(Vfs.lookup(deploymentPlan.getAbsolutePath()));
      else
        xml.parse(deploymentPlanStream);

      String type = XPath.evalString("/deployment-plan/archive-type", doc);
      String name = XPath.evalString("/deployment-plan/name", doc);

      String tag = type + "s/default/default/" + name;

      HashMap<String,String> attributes = new HashMap<String,String>();
      attributes.put(DeployClient.USER_ATTRIBUTE, _user);
      attributes.put(DeployClient.MESSAGE_ATTRIBUTE, "");

      if (archive != null)
        _deployClient.deployJarContents(tag, 
                                        Vfs.lookup(archive.getAbsolutePath()),
                                        attributes);
      else
        _deployClient.deployJarContents(tag, 
                                        archiveStream,
                                        attributes);

      _deployClient.deploy(tag);

      deployExtraFiles(tag, doc);

      TargetModuleID[] targetModules = new TargetModuleID[targetList.length];

      for (int i = 0; i < targetList.length; i++) {
        Target target = targetList[i];

        targetModules[i] = new TargetModuleIDImpl((TargetImpl) target, tag);
      }

      ProgressObjectImpl result = new ProgressObjectImpl(targetModules);

      StatusQuery status = _deployClient.status(tag);

      String archiveName;

      if (archive != null)
        archiveName = String.valueOf(archive);
      else
        archiveName = "stream";

      if (status.getMessage() == null)
        result.completed(L.l("application {0} deployed from {1}",
                             name, archiveName));
      else
        result.failed(L.l("application {0} failed from {1}: {2}",
                          name, archiveName, status.getMessage()));

      return result;
    } // XXX: hack
    catch (RemoteConnectionFailedException e) {
      reset();

      return distributeImpl(targetList,
                            archive,
                            archiveStream,
                            deploymentPlan,
                            deploymentPlanStream);
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
      /*
      IllegalStateException ex;

      ex = new IllegalStateException(e.getMessage());
      ex.initCause(e);

      throw ex;
      */
    }
  }

  private void deployExtraFiles(String tag, Node doc)
  {
    try {
      Iterator iter = XPath.select("/deployment-plan/ext-file", doc);

      while (iter.hasNext()) {
        Node node = (Node) iter.next();

        String name = XPath.evalString("name", node);
        Node data = XPath.find("data", node);

        if (data != null) {
          data = data.getFirstChild();

          TempOutputStream os = new TempOutputStream();

          XmlPrinter printer = new XmlPrinter(os);

          printer.printXml(data);

          os.close();

          long length = os.getLength();

          if (length == 0)
            continue;

          InputStream is = os.openInputStreamNoFree();

          String sha1 = _deployClient.calculateFileDigest(is, length);

          _deployClient.sendFile(sha1, length, os.openInputStream());

          _deployClient.deploy(tag);

          _deployClient.addDeployFile(tag, name, sha1);
        }
      }
    } // XXX: hack
    catch (RemoteConnectionFailedException e) {
      reset();

      deployExtraFiles(tag, doc);
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Starts the modules.
   */
  public ProgressObject start(TargetModuleID[] moduleIDList)
    throws IllegalStateException
  {
    try {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < moduleIDList.length; i++) {
        TargetModuleID targetModuleID = moduleIDList[i];

        String host = targetModuleID.getTarget().getName();
        String tag = targetModuleID.getModuleID();

        _deployClient.start(tag);

        sb.append(tag).append(' ');
      }

      ProgressObjectImpl result = new ProgressObjectImpl(moduleIDList);

      result.completed(L.l("modules ${0} started", sb.toString()));

      return result;
    } // XXX: hack
    catch (RemoteConnectionFailedException e) {
      reset();

      return start(moduleIDList);
    }
  }

  /**
   * Stops the modules.
   */
  public ProgressObject stop(TargetModuleID[] moduleIDList)
    throws IllegalStateException
  {
    try {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < moduleIDList.length; i++) {
        TargetModuleID targetModuleID = moduleIDList[i];

        String host = targetModuleID.getTarget().getName();
        String tag = targetModuleID.getModuleID();

        _deployClient.stop(tag);

        sb.append(tag).append(' ');
      }

      ProgressObjectImpl result = new ProgressObjectImpl(moduleIDList);

      result.completed(L.l("modules ${0} stop", sb.toString()));

      return result;
    } // XXX: hack
    catch (RemoteConnectionFailedException e) {
      reset();

      return stop(moduleIDList);
    }
  }

  /**
   * Undeploys the modules.
   */
  public ProgressObject undeploy(TargetModuleID[] moduleIDList)
    throws IllegalStateException
  {
    try {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < moduleIDList.length; i++) {
        TargetModuleID targetModuleID = moduleIDList[i];

        String host = targetModuleID.getTarget().getName();
        String tag = targetModuleID.getModuleID();

        _deployClient.undeploy(tag, _user, "", null);

        sb.append(tag).append(' ');
      }

      ProgressObjectImpl result = new ProgressObjectImpl(moduleIDList);

      result.completed(L.l("modules ${0} undeployed", sb.toString()));

      return result;
    }// XXX: hack
    catch (RemoteConnectionFailedException e) {
      reset();

      return undeploy(moduleIDList);
    }
  }

  /**
   * Returns true if the redeploy is supported.
   */
  public boolean isRedeploySupported()
  {
    return false;
  }

  /**
   * Redeploys the object.
   */
  public ProgressObject redeploy(TargetModuleID[] targetList,
                                 File archive,
                                 File deploymentPlan)
    throws IllegalStateException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Redeploys the object.
   */
  public ProgressObject redeploy(TargetModuleID[] targetList,
                                 InputStream archive,
                                 InputStream deploymentPlan)
    throws IllegalStateException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Frees any resources.
   */
  public void release()
  {
  }

  /**
   * Returns the default locale.
   */
  public Locale getDefaultLocale()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the current locale.
   */
  public Locale getCurrentLocale()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets the default locale.
   */
  public void setLocale(Locale locale)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the supported locales.
   */
  public Locale[] getSupportedLocales()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns true if the locale is supported.
   */
  public boolean isLocaleSupported(Locale locale)
  {
    return false;
  }

  /**
   * Returns the bean's J2EE version.
   */
  public DConfigBeanVersionType getDConfigBeanVersion()
  {
    return DConfigBeanVersionType.V1_4;
  }

  /**
   * Returns true if the given version is supported.
   */
  public boolean isDConfigBeanVersionSupported(DConfigBeanVersionType version)
  {
    return true;
  }

  /**
   * Sets true if the given version is supported.
   */
  public void setDConfigBeanVersionSupported(DConfigBeanVersionType version)
    throws DConfigBeanVersionUnsupportedException
  {
  }

  /**
   * Return the debug view of the manager.
   */
  public String toString()
  {
    return "DeploymentManagerImpl[" + _uri + "]";
  }

  public ProgressObject distribute(Target[] arg0,
                                   ModuleType arg1,
                                   InputStream arg2,
                                   InputStream arg3)
    throws IllegalStateException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void setDConfigBeanVersion(DConfigBeanVersionType arg0)
    throws DConfigBeanVersionUnsupportedException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
