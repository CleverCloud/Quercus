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
import java.text.SimpleDateFormat;

import org.apache.maven.plugin.*;

import com.xerox.amazonws.ec2.AddressInfo;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.xerox.amazonws.ec2.ReservationDescription.Instance;

/**
 * The MavenEc2ListCluster
 * @goal ec2-list-cluster
 */
public class MavenEc2ListCluster extends AbstractMojo
{
  private static final Logger log = 
    Logger.getLogger(MavenEc2ListCluster.class.getName());

  private static final SimpleDateFormat DATE_FORMAT 
    = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");

  private String _accessKeyId;
  private String _secretAccessKey;
  private String[] _triadIps;
  private String _frontendIp;
  private String _password;


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

  protected void describeTriadMembers(Jec2 ec2)
    throws MojoExecutionException
  {
    try {
      List<String> addresses = new ArrayList<String>();

      for (String triadIp : _triadIps)
        addresses.add(triadIp);

      List<AddressInfo> addressInfos = ec2.describeAddresses(addresses);

      List<String> instanceIds = new ArrayList<String>();

      for (AddressInfo addressInfo : addressInfos)
        instanceIds.add(addressInfo.getInstanceId());

      List<ReservationDescription> descriptions =
        ec2.describeInstances(instanceIds);

      /// XXX should ignore address info index and use elastic ip
      for (int i = 0; i < _triadIps.length; i++) {
        String instanceId = addressInfos.get(i).getInstanceId();

        for (ReservationDescription description : descriptions) {
          List<ReservationDescription.Instance> instances =
            description.getInstances();

          for (ReservationDescription.Instance instance : instances) {
            if (instance.getInstanceId().equals(instanceId)) {
              getLog().info(indexToName(i) + ":");
              getLog().info("  Instance Id : " + instance.getInstanceId());
              getLog().info("  State       : " + instance.getState());

              String launchTime = 
                DATE_FORMAT.format(instance.getLaunchTime().getTime());

              getLog().info("  Launch Time : " + launchTime);
            }
          }
        }
      }
    }
    catch (EC2Exception e) {
      throw new MojoExecutionException("Exception while finding triad members",
                                       e);
    }
  }

  protected void describeFrontend(Jec2 ec2)
    throws MojoExecutionException
  {
    // XXX
  }

  protected void describeDynamicServers(Jec2 ec2)
    throws MojoExecutionException
  {
    // XXX
  }

  /**
   * Executes the maven resin:ec2-start-triad task
   */
  public void execute() throws MojoExecutionException
  {
    Jec2 ec2 = new Jec2(_accessKeyId, _secretAccessKey);

    if (_triadIps == null || _triadIps.length < 1 || _triadIps.length > 3)
      throw new MojoExecutionException("Please specify between 1 and 3 triad server elastic IPs");

    describeTriadMembers(ec2);

    if (_frontendIp != null)
      describeFrontend(ec2);

    describeDynamicServers(ec2);
  }

  private String indexToName(int i)
  {     
    switch (i) {
      case -1:
        return "[frontend]";

      case 0:
        return "[triad-a]";

      case 1:
        return "[triad-b]";

      case 2:
        return "[triad-c]";
    }

    return "[]";
  }
}
