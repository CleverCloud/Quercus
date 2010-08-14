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

import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.ImageDescription;
import com.xerox.amazonws.ec2.Jec2;

/**
 * The MavenEc2ListResinAmis
 * @goal ec2-list-resin-amis
 */
public class MavenEc2ListResinAmis extends AbstractMojo
{
  private static final Logger log = 
    Logger.getLogger(MavenEc2ListResinAmis.class.getName());

  private static final String CAUCHO_USER_ID = "972637075895";

  private String _accessKeyId;
  private String _secretAccessKey;

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
   * Executes the maven resin:ec2-start-triad task
   */
  public void execute() throws MojoExecutionException
  {
    Jec2 ec2 = new Jec2(_accessKeyId, _secretAccessKey);

    ArrayList<String> owners = new ArrayList<String>();
    owners.add(CAUCHO_USER_ID);

    try {
      List<ImageDescription> images = ec2.describeImagesByOwner(owners);

      for (ImageDescription image : images) {
        getLog().info("Image [" + image.getImageId() + "]:");
        getLog().info("  Location     : " + image.getImageLocation());
        getLog().info("  Architecture : " + image.getArchitecture());
        getLog().info("  Kernel       : " + image.getKernelId());
        getLog().info("  Ramdisk      : " + image.getRamdiskId());
        getLog().info("");
      }
    }
    catch (EC2Exception e) {
      getLog().warn("Exception while fetching Resin AMI info");
    }

  }
}
