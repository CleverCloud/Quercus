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
 * @author Emil Ong
 */

package com.caucho.maven.aws;

import com.caucho.resin.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;
import org.apache.maven.plugin.*;

import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.xerox.amazonws.ec2.ReservationDescription.Instance;

/**
 * The MavenEc2StartTriad
 * @goal ec2-start-triad
 */
public class MavenEc2StartTriad extends AbstractMojo
{
  private static final Logger log = 
    Logger.getLogger(MavenEc2StartTriad.class.getName());

  private String _amiId;
  private String _accessKeyId;
  private String _secretAccessKey;
  private String[] _triadIps;
  private String _frontendIp;
  private String _password;
  private String _keyPair;
  private List<String> _securityGroups;

  /**
   * Sets the triad member elastic ips
   * @parameter
   * @required
   */
  public void setTriadIps(String[] triadIps)
  {
    _triadIps = triadIps;
  }

  /**
   * Sets the frontend elastic ips
   * @parameter
   */
  public void setFrontendIp(String frontendIp)
  {
    _frontendIp = frontendIp;
  }

  /**
   * Sets the administration password for the Resin admin console and
   * inter-server communication
   * @parameter expression="{resin.admin.password}"
   * @required
   */
  public void setAdminPassword(String password)
  {
    _password = password;
  }

  /**
   * Sets the AWS access key id
   * @parameter
   * @required
   */
  public void setAccessKeyId(String accessKeyId)
  {
    _accessKeyId = accessKeyId;
  }

  /**
   * Sets the AWS secret access key
   * @parameter
   * @required
   */
  public void setSecretAccessKey(String secretAccessKey)
  {
    _secretAccessKey = secretAccessKey;
  }

  /**
   * Sets the ID of the Resin AMI
   * @parameter
   * @required
   */
  public void setAmi(String amiId)
  {
    _amiId = amiId;
  }

  /**
   * Sets the ssh key pair used to log in to AMI
   * @parameter
   * @required
   */
  public void setKeyPair(String keyPair)
  {
    _keyPair = keyPair;
  }

  /**
   * Sets the security groups for the triad
   * @parameter
   */
  public void setSecurityGroups(List<String> groups)
  {
    _securityGroups = groups;
  }

  protected String buildUserDataScript(int index)
  {
    StringBuilder script = new StringBuilder();
    script.append("#!/bin/bash\n");
    script.append("\n");

    // configuration script, sourced by /etc/init.d/resin
    script.append("EC2_CONFIG=/var/ec2/resin-ec2-config.sh\n");
    script.append("\n");
    script.append("mkdir -p /var/ec2\n");
    script.append("cat > $EC2_CONFIG <<EOF\n");
    script.append("#!/bin/bash\n");
    script.append("\n");
    script.append("# Lookup the internal IP of the various instances \n");
    script.append("# using trick from this page: \n");
    script.append("# http://alestic.com/2009/06/ec2-elastic-ip-internal\n");
    script.append("# Essentially, do a lookup of the external hostname on\n");
    script.append("# the instance, which returns the internal IP.  A \n");
    script.append("# strange, but useful behavior on EC2.\n");
    script.append("\n");
    script.append("lookup_internal_ip()\n");
    script.append("{\n");
    script.append("  dig +short -x \\$1 | xargs dig +short\n");
    script.append("}\n");
    script.append("\n");

    if (_triadIps != null) {
      if (_triadIps.length >= 1) {
        script.append("_EXTERNAL_TRIAD_A=");
        script.append(_triadIps[0]);
        script.append('\n');
        script.append("export TRIAD_A=\\`lookup_internal_ip \\$_EXTERNAL_TRIAD_A\\`\n");
      }
      if (_triadIps.length >= 2) {
        script.append("_EXTERNAL_TRIAD_B=");
        script.append(_triadIps[1]);
        script.append('\n');
        script.append("export TRIAD_B=\\`lookup_internal_ip \\$_EXTERNAL_TRIAD_B\\`\n");
      }
      if (_triadIps.length >= 3) {
        script.append("_EXTERNAL_TRIAD_C=");
        script.append(_triadIps[2]);
        script.append('\n');
        script.append("export TRIAD_C=\\`lookup_internal_ip \\$_EXTERNAL_TRIAD_C\\`\n");
      }
    }
    script.append("\n");

    if (_frontendIp != null) {
      script.append("_EXTERNAL_FRONTEND=");
      script.append(_frontendIp);
      script.append('\n');
      script.append("export FRONTEND=\\`lookup_internal_ip \\$_EXTERNAL_FRONTEND\\`\n");
      script.append(_frontendIp);
      script.append('\n');
    }

    script.append("\n");

    if (_password != null) {
      script.append("export RESIN_ADMIN_PASSWORD=");
      script.append(_password);
      script.append('\n');
    }

    script.append("\n");

    switch (index) {
      case -1: 
        script.append("export MY_SERVER_ID=frontend\n");
        break;

      case 0: 
        script.append("export MY_SERVER_ID=triad-a\n");
        break;

      case 1: 
        script.append("export MY_SERVER_ID=triad-b\n");
        break;

      case 2: 
        script.append("export MY_SERVER_ID=triad-c\n");
        break;
    }
    script.append("\n");
    script.append("EOF\n");
    script.append("\n");
    script.append("chmod +x $EC2_CONFIG\n");

    // elastic ip daemon
    script.append("\n");
    script.append("EIP_DAEMON=/var/ec2/resin-elastic-ip-daemon.sh\n");
    script.append("\n");
    script.append("cat > $EIP_DAEMON <<EOF\n");
    script.append("#!/bin/bash\n");
    script.append("\n");
    script.append("_PID_FILE=/var/run/resin-elastic-ip-daemon.pid\n");
    script.append("\n");
    script.append("if [ -f \\$_PID_FILE ]; then\n");
    script.append("  exit 0\n");
    script.append("fi\n");
    script.append("\n");
    script.append("echo \\$$ > \\$_PID_FILE\n");
    script.append("\n");
    script.append("# loop forever - elastic ip binding may take time\n");
    script.append("while true; do\n");
    script.append("\n");
    script.append("if ( /usr/bin/jps | /bin/grep -q Resin ); then\n");
    script.append("  sleep 60\n");
    script.append("  continue\n");
    script.append("fi\n");
    script.append("\n");
    script.append("source $EC2_CONFIG\n");
    script.append("\n");

    script.append("# Spin until the elastic ip is associated\n");

    script.append("while [ -z \"\\$TRIAD_A\" ");

    if (_triadIps.length >= 2)
      script.append("-o -z \"\\$TRIAD_B\" ");

    if (_triadIps.length == 3)
      script.append("-o -z \"\\$TRIAD_C\" ");

    if (_frontendIp != null)
      script.append("-o -z \"\\$FRONTEND\" ");

    script.append(" ]; do \n");

    script.append("sleep 5\n");
    script.append("source $EC2_CONFIG\n");
    script.append("\n");
    script.append("done\n");

    script.append("\n");
    script.append("/etc/init.d/resin start\n");
    script.append("\n");
    script.append("done\n");
    script.append("\n");
    script.append("EOF\n");
    script.append("\n");
    script.append("chmod +x $EIP_DAEMON\n");

    return script.toString();
  }

  /**
    * Runs a new instance on EC2 using the generated user data.
    *
    * @return instance descriptor of the created instance
    */
  protected ReservationDescription.Instance launchServer(Jec2 ec2, int i)
    throws MojoExecutionException
  {
    String userData = buildUserDataScript(i);

    try {
      // XXX instance type, public ip_p
      ReservationDescription description =
        ec2.runInstances(_amiId, 1, 1, _securityGroups, userData, _keyPair);

      List<ReservationDescription.Instance> instances =
        description.getInstances();

      if (instances.size() != 1)
        throw new MojoExecutionException("Incorrect number of instances launched: " + instances.size());

      ReservationDescription.Instance instance = instances.get(0);

      switch (i) {
        case -1:
          getLog().info("New instance [frontend]: " + instance.getInstanceId());
          break;

        case 0:
          getLog().info("New instance [triad-a]: " + instance.getInstanceId());
          break;

        case 1:
          getLog().info("New instance [triad-b]: " + instance.getInstanceId());
          break;

        case 2:
          getLog().info("New instance [triad-c]: " + instance.getInstanceId());
          break;
      }

      return instance;
    }
    catch (EC2Exception e) {
      throw new MojoExecutionException("Exception while launching instance", e);
    }
  }

  protected void associateElasticIp(Jec2 ec2, 
                                    int i,
                                    ReservationDescription.Instance instance)
    throws MojoExecutionException
  {
    try {
      String[] instanceIds = new String[] { instance.getInstanceId() };

      while (! instance.isRunning()) {
        getLog().info("Instance " + instance.getInstanceId() + 
                      " not yet running (" + instance.getState() + "). " +
                      "Sleeping...");

        try {
          Thread.sleep(10000);
        }
        catch (InterruptedException e) {
          getLog().debug("Sleep interrupted", e);
        }

        List<ReservationDescription> descriptions =
          ec2.describeInstances(instanceIds);

        if (descriptions.size() != 1)
          throw new MojoExecutionException("Unknown status for instance " + instance.getInstanceId() + ": " + descriptions);

        List<ReservationDescription.Instance> instances =
          descriptions.get(0).getInstances();

        if (instances.size() != 1)
          throw new MojoExecutionException("Unknown status for instance " + instance.getInstanceId() + ": " + instances);

        instance = instances.get(0);

        if (instance.isShuttingDown() || instance.isTerminated())
          throw new MojoExecutionException("Instance shutting down or terminated before associating address: " + instance.getInstanceId());
      }

      getLog().info("Instance " + instance.getInstanceId() + 
                    " is now running.  Pausing before associating address...");

      // even when running, ec2 may not accept the elastic ip association
      // right away
      try {
        Thread.sleep(10000);
      }
      catch (InterruptedException e) {
        getLog().debug("Sleep interrupted", e);
      }

      String elasticIp = null;

      if (i < 0)
        elasticIp = _frontendIp;
      else
        elasticIp = _triadIps[i];

      ec2.associateAddress(instance.getInstanceId(), elasticIp);

      getLog().info("Instance " + instance.getInstanceId() + 
                    " now associated with Elastic IP " + elasticIp);
    }
    catch (EC2Exception e) {
      throw new MojoExecutionException("Exception while associating Elastic IP",
                                       e);
    }
  }

  /**
   * Executes the maven resin:ec2-start-triad task
   */
  public void execute() throws MojoExecutionException
  {
    Jec2 ec2 = new Jec2(_accessKeyId, _secretAccessKey);

    if (_triadIps == null || _triadIps.length < 1 || _triadIps.length > 3)
      throw new MojoExecutionException("Please specify between 1 and 3 triad server elastic IPs");

    // XXX check that triad members are not already running
    getLog().info("----- Starting instances -----");
    ReservationDescription.Instance frontend = null;
    List<ReservationDescription.Instance> triads = 
      new ArrayList<ReservationDescription.Instance>();

    if (_frontendIp != null)
      frontend = launchServer(ec2, -1);

    for (int i = 0; i < _triadIps.length; i++)
      triads.add(launchServer(ec2, i));

    getLog().info("");
    getLog().info("----- Associating Elastic IPs -----");

    if (frontend != null)
      associateElasticIp(ec2, -1, frontend);

    for (int i = 0; i < _triadIps.length; i++) {
      ReservationDescription.Instance triad = triads.get(i);
      associateElasticIp(ec2, i, triad);
    }
  }
}
