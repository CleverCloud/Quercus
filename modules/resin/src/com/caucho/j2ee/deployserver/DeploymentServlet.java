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

package com.caucho.j2ee.deployserver;

import com.caucho.config.Config;
import com.caucho.hessian.io.*;
import com.caucho.j2ee.deployclient.TargetImpl;
import com.caucho.j2ee.deployclient.TargetModuleIDImpl;
import com.caucho.util.IntMap;
import com.caucho.util.L10N;

import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.status.ProgressObject;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager for the deployments.
 */
public class DeploymentServlet
  extends GenericServlet
{
  private static final L10N L = new L10N(DeploymentServlet.class);
  private static final Logger log
    = Logger.getLogger(DeploymentServlet.class.getName());

  private static final int GET_TARGETS = 1;
  private static final int DISTRIBUTE = 2;
  private static final int GET_AVAILABLE_MODULES = 3;
  private static final int UNDEPLOY = 4;
  private static final int START = 5;
  private static final int STOP = 6;

  private static final IntMap _methodMap = new IntMap();

  private DeploymentService _deploymentService;

  public void init()
    throws ServletException
  {
    super.init();

    _deploymentService = DeploymentService.getDeploymentService();
  }

  /**
   * Serves the deployment.
   */
  public void service(ServletRequest req, ServletResponse res)
    throws IOException, ServletException
  {
    InputStream is = req.getInputStream();
    OutputStream os = res.getOutputStream();

    Hessian2Input in = new Hessian2Input(is);
    HessianOutput out = new HessianOutput(os);

    in.readCall();
    String method = in.readMethod();

    try {
      switch (_methodMap.get(method)) {
      case GET_TARGETS:
        in.completeCall();
        out.startReply();
        out.writeObject(_deploymentService.getTargets());
        out.completeReply();
        break;

      case GET_AVAILABLE_MODULES:
        {
          String type = in.readString();
          in.completeCall();

          out.startReply();
          out.writeObject(_deploymentService.getAvailableModules(type));
          out.completeReply();
          break;
        }

      case DISTRIBUTE:
        {
          TargetImpl []targets = (TargetImpl[]) in.readObject(TargetImpl[].class);
          DeploymentPlan plan = new DeploymentPlan();

          InputStream planIs = in.readInputStream();

          try {
            new Config().configure(plan, planIs);
          } finally {
            planIs.close();
          }

          InputStream archiveIs = in.readInputStream();

          ProgressObject po = _deploymentService.distribute(targets, archiveIs, plan);

          // use up all of the input or hessian throws
          // an execption and hides  error reported in the progress object
          try {
            while (archiveIs.read() != -1) {}
          }
          catch (Exception t) {
            if (log.isLoggable(Level.FINEST))
              log.log(Level.FINEST, t.toString(), t);
          }

          in.completeCall();

          if (log.isLoggable(Level.FINEST))
            log.log(Level.FINEST, String.valueOf(po));

          out.startReply();
          out.writeObject(po);
          out.completeReply();
          break;
        }

      case UNDEPLOY:
        {
          TargetModuleID []targetIDs;
          targetIDs = (TargetModuleID []) in.readObject(TargetModuleID[].class);

          ProgressObject po = _deploymentService.undeploy(targetIDs);

          in.completeCall();
          out.startReply();
          out.writeObject(po);
          out.completeReply();
          break;
        }

        case START:
        {
          TargetModuleID []targetModuleIDs;
          targetModuleIDs = (TargetModuleID []) in.readObject(TargetModuleIDImpl[].class);

          ProgressObject po = _deploymentService.start(targetModuleIDs);

          in.completeCall();
          out.startReply();
          out.writeObject(po);
          out.completeReply();
          break;
        }

        case STOP:
        {
          TargetModuleID []targetModuleIDs;
          targetModuleIDs = (TargetModuleID []) in.readObject(TargetModuleIDImpl[].class);

          ProgressObject po = _deploymentService.stop(targetModuleIDs);

          in.completeCall();
          out.startReply();
          out.writeObject(po);
          out.completeReply();
          break;
        }

      default:
        out.startReply();
        out.writeFault("UnknownMethod", "UnknownMethod: " + method, null);
        out.completeReply();
        break;
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      out.startReply();
      out.writeFault(e.toString(), e.toString(), e);
      out.completeReply();
    }
  }


  static {
    _methodMap.put("getTargets", GET_TARGETS);
    _methodMap.put("distribute", DISTRIBUTE);
    _methodMap.put("getAvailableModules", GET_AVAILABLE_MODULES);
    _methodMap.put("undeploy", UNDEPLOY);
    _methodMap.put("start", START);
    _methodMap.put("stop", STOP);
  }
}

