/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.server.admin;

import com.caucho.bam.*;
import com.caucho.env.git.*;
import com.caucho.hmtp.HmtpClient;
import com.caucho.server.resin.*;
import com.caucho.util.L10N;
import com.caucho.util.QDate;
import com.caucho.vfs.*;

import java.io.*;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Deploy Client API
 */
public class WebAppDeployClient extends DeployClient
{
  private static final L10N L = new L10N(WebAppDeployClient.class);
  private static final Logger log 
    = Logger.getLogger(WebAppDeployClient.class.getName());

  public WebAppDeployClient()
  {
    super();
  }

  public WebAppDeployClient(String serverId)
  {
    super(serverId);
  }

  public WebAppDeployClient(String host, int port,
                            String userName, String password)
  {
    super(host, port, userName, password);
  }

  /*
  public String deployPomegranateWar(String tag,
                                     Path pomegranateWar,
                                     String pomegranateTag,
                                     HashMap<String,PomegranateJar> knownJars,
                                     HashMap<String,String> attributes)
    throws IOException
  {
    TagResult []results = queryTags(pomegranateTag);

    if (results.length == 0) {
      throw new RuntimeException("Pomegranate repository at tag " 
                                 + pomegranateTag + " does not exist");
    }

    GitWorkingTree pomegranateTree = getWorkingTree(results[0].getRoot());
    GitCommitTree pomCommitTree = new GitCommitTree(pomegranateTree);

    PomegranateGitCommitWar commit 
      = new PomegranateGitCommitWar(pomegranateWar, knownJars, pomCommitTree);

    try {
      return sendPomegranateWarFiles(tag, commit, pomegranateTag, attributes);
    } finally {
      commit.close();
    }
  }*/

  //
  // low-level routines
  //

  /*
  private String sendPomegranateWarFiles(String tag,
                                         PomegranateGitCommitWar commit,
                                         String pomegranateTag,
                                         HashMap<String,String> attributes)
    throws IOException
  {
    String result = deployJar(tag, commit, attributes);

    String []files = getCommitList(commit.getPomegranateCommitList());

    for (String sha1 : files) { 
      GitJarStreamSource gitSource = new GitJarStreamSource(sha1, commit);
      StreamSource source = new StreamSource(gitSource);

      DeploySendQuery sendQuery = new DeploySendQuery(sha1, source);

      querySet(sendQuery);
    }

    setTag(pomegranateTag, commit.getPomegranateDigest(), attributes);

    return result;
  }
  */

  //
  // tag construction
  //

  public static String createTag(String stage, 
                                 String host, 
                                 String name)
  {
    while (name.startsWith("/"))
      name = name.substring(1);
    
    return "wars/" + stage + "/" + host + "/" + name;
  }

  public static String createTag(String stage, 
                                 String host,
                                 String name,
                                 String version)
  {
    if (version != null)
      return createTag(stage, host, name) + "-" + version;

    return createTag(stage, host, name);
  }

  public static String createArchiveTag(String host, 
                                        String name, 
                                        String version)
  {
    QDate qDate = new QDate();
    long time = qDate.getTimeOfDay() / 1000;

    StringBuilder sb = new StringBuilder();

    sb.append(createTag("archive", host, name, version));

    sb.append('/');

    sb.append(qDate.printISO8601Date());

    sb.append('T');
    sb.append((time / 36000) % 10);
    sb.append((time / 3600) % 10);

    sb.append(':');
    sb.append((time / 600) % 6);
    sb.append((time / 60) % 10);

    sb.append(':');
    sb.append((time / 10) % 6);
    sb.append((time / 1) % 10);

    return sb.toString();
  }
}

