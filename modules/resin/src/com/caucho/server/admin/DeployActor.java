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

package com.caucho.server.admin;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.management.ObjectName;

import com.caucho.bam.ActorError;
import com.caucho.bam.Broker;
import com.caucho.bam.QueryGet;
import com.caucho.bam.QuerySet;
import com.caucho.bam.SimpleActor;
import com.caucho.cloud.deploy.DeployNetworkService;
import com.caucho.cloud.deploy.DeployTagItem;
import com.caucho.config.ConfigException;
import com.caucho.config.Service;
import com.caucho.env.repository.Repository;
import com.caucho.env.repository.RepositoryService;
import com.caucho.env.repository.RepositoryTagEntry;
import com.caucho.jmx.Jmx;
import com.caucho.management.server.DeployControllerMXBean;
import com.caucho.management.server.EAppMXBean;
import com.caucho.management.server.EarDeployMXBean;
import com.caucho.management.server.WebAppDeployMXBean;
import com.caucho.management.server.WebAppMXBean;
import com.caucho.server.cluster.Server;
import com.caucho.server.host.HostController;
import com.caucho.server.webapp.WebAppController;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

@Service
public class DeployActor extends SimpleActor
{
  private static final Logger log
    = Logger.getLogger(DeployActor.class.getName());

  private static final L10N L = new L10N(DeployActor.class);

  private Server _server;

  private Repository _repository;

  private AtomicBoolean _isInit = new AtomicBoolean();

  public DeployActor()
  {
  }

  @Override
  public String getJid()
  {
    return "deploy@resin.caucho";
  }

  private Broker getBroker()
  {
    return _server.getAdminBroker();
  }

  @PostConstruct
  public void init()
  {
    if (_isInit.getAndSet(true))
      return;

    _server = Server.getCurrent();

    if (_server == null)
      throw new ConfigException(L.l("resin:DeployService requires an active Server.\n  {0}",
                                    Thread.currentThread().getContextClassLoader()));

    _repository = RepositoryService.getCurrentRepository();

    setLinkStream(getBroker().getBrokerStream());
    getBroker().addActor(getActorStream());
  }

  @QueryGet
  public boolean commitList(long id, String to, String from,
                            DeployCommitListQuery commitList)
  {
    ArrayList<String> uncommittedList = new ArrayList<String>();

    if (commitList.getCommitList() != null) {
      for (String commit : commitList.getCommitList()) {
        if (! _repository.exists(commit))
          uncommittedList.add(commit);
      }
    }

    DeployCommitListQuery resultList
      = new DeployCommitListQuery(uncommittedList);

    getLinkStream().queryResult(id, from, to, resultList);

    return true;
  }

  @QuerySet
  public void tagCopy(long id,
                      String to,
                      String from,
                      CopyTagQuery query)
  {
    String tag = query.getTag();
    String sourceTag = query.getSourceTag();

    RepositoryTagEntry entry = _repository.getTagMap().get(sourceTag);

    if (entry == null) {
      log.fine(this + " copyError dst='" + query.getTag() + "' src='" + query.getSourceTag() + "'");

      getLinkStream().queryError(id, from, to, query,
                                   new ActorError(ActorError.TYPE_CANCEL,
                                                  ActorError.ITEM_NOT_FOUND,
                                                  "unknown tag"));
      return;
    }

    log.fine(this + " copy dst='" + query.getTag() + "' src='" + query.getSourceTag() + "'");
    
    String server = "default";
    
    TreeMap<String,String> metaDataMap = new TreeMap<String,String>();
    
    if (query.getUser() != null)
      metaDataMap.put("user", query.getUser());
    
    if (server != null)
      metaDataMap.put("server", server);
    
    if (query.getVersion() != null)
      metaDataMap.put("version", query.getVersion());

    boolean result = _repository.putTag(tag,
                                        entry.getRoot(),  
                                        query.getMessage(),
                                        metaDataMap);

    getLinkStream().queryResult(id, from, to, result);
  }

  @QueryGet
  public void tagState(long id,
                       String to,
                       String from,
                       TagStateQuery query)
  {
    // XXX: just ping the tag?
    // updateDeploy();
    
    String tag = query.getTag();
    
    DeployNetworkService deploy = DeployNetworkService.getCurrent();
    DeployTagItem item = null;
    
    if (deploy != null) {
      deploy.update(tag);
      item = deploy.getTagItem(tag);
    }
    
    if (item != null) {
      TagStateQuery result = new TagStateQuery(tag, item.getState(), 
                                               item.getDeployException());
      
      getLinkStream().queryResult(id, from, to, result);
    }
    else
      getLinkStream().queryResult(id, from, to, null);
  }

  @QuerySet
  public void removeTag(long id,
                        String to,
                        String from,
                        RemoveTagQuery query)
  {
    String server = "default";
    
    HashMap<String,String> commitMetaData = new HashMap<String,String>();
    
    commitMetaData.put("server", server);
    
    if (query.getUser() != null)
      commitMetaData.put("user", query.getUser());
    
    boolean result = _repository.removeTag(query.getTag(),
                                           query.getMessage(),
                                           commitMetaData);

    getLinkStream().queryResult(id, from, to, result);
  }

  @QuerySet
  public boolean sendFileQuery(long id, String to, String from,
                               DeploySendQuery query)
  {
    String sha1 = query.getSha1();

    if (log.isLoggable(Level.FINER))
      log.finer(this + " sendFileQuery sha1=" + sha1);

    InputStream is = null;
    try {
      is = query.getInputStream();

      _repository.writeRawGitFile(sha1, is);

      getLinkStream().queryResult(id, from, to, true);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      getLinkStream().queryResult(id, from, to, false);
    } finally {
      IoUtil.close(is);
    }

    return true;
  }

  @QuerySet
  public boolean setTagQuery(long id, String to, String from, SetTagQuery query)
  {
    String tagName = query.getTag();
    String contentHash = query.getHex();

    String server = "default";
    
    TreeMap<String,String> commitMetaData = new TreeMap<String,String>();
    
    commitMetaData.put("server", server);
    
    if (query.getUser() != null)
      commitMetaData.put("user", query.getUser());
    
    if (query.getVersion() != null)
      commitMetaData.put("user", query.getVersion());
    
    boolean result = _repository.putTag(tagName, 
                                        contentHash,
                                        query.getMessage(),
                                        commitMetaData);

    getLinkStream().queryResult(id, from, to, String.valueOf(result));

    return true;
  }

  @QueryGet
  public boolean queryTags(long id,
                           String to,
                           String from,
                           QueryTagsQuery tagsQuery)
  {
    ArrayList<TagResult> tags = new ArrayList<TagResult>();

    Pattern pattern = Pattern.compile(tagsQuery.getPattern());

    for (Map.Entry<String, RepositoryTagEntry> entry :
         _repository.getTagMap().entrySet()) {
      String tag = entry.getKey();

      if (pattern.matcher(tag).matches())
        tags.add(new TagResult(tag, entry.getValue().getRoot()));
    }

    getLinkStream()
      .queryResult(id, from, to, tags.toArray(new TagResult[tags.size()]));

    return true;
  }

  /**
   * @deprecated
   */
  @QuerySet
  public boolean controllerDeploy(long id,
                                  String to,
                                  String from,
                                  ControllerDeployQuery query)
  {
    String status = deploy(query.getTag());

    log.fine(this + " deploy '" + query.getTag() + "' -> " + status);

    getLinkStream().queryResult(id, from, to, true);

    return true;
  }

  private String deploy(String gitPath)
  {
    int p = gitPath.indexOf('/');
    int q = gitPath.indexOf('/', p + 1);
    int r = gitPath.lastIndexOf('/');

    if (p < 0 || q < 0 || r < 0 || r <= q)
      return L.l("'{0}' is an unknown type", gitPath);

    String type = gitPath.substring(0, p);
    // String stage = gitPath.substring(p + 1, q);
    // String host = gitPath.substring(q + 1, r);
    String name = gitPath.substring(r + 1);

    try {
      if (type.equals("ears")) {
        ObjectName pattern = new ObjectName("resin:type=EarDeploy,*");

        for (Object proxy : Jmx.query(pattern)) {
          EarDeployMXBean earDeploy = (EarDeployMXBean) proxy;

          earDeploy.deploy(name);


          return statusMessage(gitPath);
        }
      }
      else if (type.equals("wars")) {
        ObjectName pattern = new ObjectName("resin:type=WebAppDeploy,*");

        for (Object proxy : Jmx.query(pattern)) {
          WebAppDeployMXBean warDeploy = (WebAppDeployMXBean) proxy;

          warDeploy.deploy(name);

          return statusMessage(gitPath);
        }
      }

      return L.l("'{0}' is an unknown type", gitPath);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return L.l("deploy '{0}' failed\n{1}", gitPath, e.toString());
    }
  }

  /**
   * @deprecated
   */
  @QuerySet
  public boolean controllerStart(long id,
                                 String to,
                                 String from,
                                 ControllerStartQuery query)
  {
    String status = start(query.getTag());

    log.fine(this + " start '" + query.getTag() + "' -> " + status);

    getLinkStream().queryResult(id, from, to, true);

    return true;
  }

  private String start(String tag)
  {
    DeployControllerMXBean controller = findController(tag);

    if (controller == null)
      return L.l("'{0}' is an unknown controller", controller);

    try {
      controller.start();

      return controller.getState();
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return e.toString();
    }
  }

  /**
   * @deprecated
   */
  @QuerySet
  public boolean controllerStop(long id,
                                String to,
                                String from,
                                ControllerStopQuery query)
  {
    String status = stop(query.getTag());

    log.fine(this + " stop '" + query.getTag() + "' -> " + status);

    getLinkStream().queryResult(id, from, to, true);

    return true;
  }

  private String stop(String tag)
  {
    DeployControllerMXBean controller = findController(tag);

    if (controller == null)
      return L.l("'{0}' is an unknown controller", controller);

    try {
      controller.stop();

      return controller.getState();
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return e.toString();
    }
  }

  /**
   * @deprecated
   */
  @QuerySet
  public boolean controllerUndeploy(long id,
                                    String to,
                                    String from,
                                    ControllerUndeployQuery query)
  {
    String status = undeploy(query.getTag());

    log.fine(this + " undeploy '" + query.getTag() + "' -> " + status);

    getLinkStream().queryResult(id, from, to, true);

    return true;
  }

  /**
   * @deprecated
   */
  private String undeploy(String tag)
  {
    DeployControllerMXBean controller = findController(tag);

    if (controller == null)
      return L.l("'{0}' is an unknown controller", controller);

    try {
      Path root = Vfs.lookup(controller.getRootDirectory());

      root.removeAll();

      controller.stop();

      if (controller.destroy())
        return "undeployed";
      else
        return L.l("'{0}' failed to undeploy application '{1}'",
                   controller,
                   tag);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return e.toString();
    }
  }

  /**
   * @deprecated
   */
  @QuerySet
  public boolean controllerUndeploy(long id,
                                    String to,
                                    String from,
                                    UndeployQuery query)
  {
    String status = undeploy(query.getTag(), query.getUser(), query.getMessage());

    log.fine(this + " undeploy '" + query.getTag() + "' -> " + status);

    getLinkStream().queryResult(id, from, to, true);

    return true;
  }

  private String undeploy(String tag, String user, String commitMessage)
  {
    DeployControllerMXBean controller = findController(tag);

    if (controller == null)
      return L.l("'{0}' is an unknown controller", controller);

    try {
      Path root = Vfs.lookup(controller.getRootDirectory());

      root.removeAll();

      controller.stop();

      if (controller.destroy()) {
        String server = "default";
        
        HashMap<String,String> commitMetaData = new HashMap<String,String>();
        
        commitMetaData.put("server", server);
        
        if (user != null)
          commitMetaData.put("user", user);
        
        _repository.removeTag(tag, commitMessage, commitMetaData);

        return "undeployed";
      }
      else
        return L.l("'{0}' failed to remove application '{1}'",
                   controller,
                   tag);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return e.toString();
    }
  }

  /**
   * @deprecated
   */
  @QuerySet
  public boolean sendAddFileQuery(long id, String to, String from,
                                  DeployAddFileQuery query)
  {
    String tag = query.getTag();
    String name = query.getName();
    String contentHash = query.getHex();

    try {
      DeployControllerMXBean deploy = findController(tag);

      if (deploy == null) {
        if (log.isLoggable(Level.FINE))
          log.fine(this + " sendAddFileQuery '" + tag + "' is an unknown DeployController");

        getLinkStream().queryResult(id, from, to, "no-deploy: " + tag);

        return true;
      }

      Path root = Vfs.lookup(deploy.getRootDirectory());
      root = root.createRoot();

      Path path = root.lookup(name);

      if (! path.getParent().exists())
        path.getParent().mkdirs();

      _repository.expandToPath(contentHash, path);

      getLinkStream().queryResult(id, from, to, "ok");

      return true;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      getLinkStream().queryResult(id, from, to, "fail");

      return true;
    }
  }

  /**
   * @deprecated
   **/
  @QueryGet
  public boolean listWebApps(long id,
                             String to,
                             String from,
                             ListWebAppsQuery listQuery)
  {
    ArrayList<WebAppQuery> apps = new ArrayList<WebAppQuery>();

    String stage = _server.getStage();

    for (HostController host : _server.getHostControllers()) {
      if (listQuery.getHost().equals(host.getName())) {
        for (WebAppController webApp
            : host.getDeployInstance().getWebAppList()) {
          WebAppQuery q = new WebAppQuery();
          String name = webApp.getId();

          if (name.startsWith("/"))
            name = name.substring(1);

          q.setTag("wars/" + stage + "/" + host.getName() + "/" + name);

          q.setHost(host.getName());
          q.setUrl(webApp.getURL());

          apps.add(q);
        }
      }
    }

    getLinkStream()
      .queryResult(id, from, to, apps.toArray(new WebAppQuery[apps.size()]));

    return true;
  }

  /**
   * @deprecated
   **/
  @QueryGet
  public boolean listTags(long id,
                          String to,
                          String from,
                          ListTagsQuery listQuery)
  {
    ArrayList<TagQuery> tags = new ArrayList<TagQuery>();

    for (String tag : _repository.getTagMap().keySet()) {
      if (tag.startsWith("wars/default") || tag.startsWith("ears/default")) {
        int p = "wars/default/".length();
        int q = tag.indexOf('/', p + 1);

        if (q < 0)
          continue;

        String host = tag.substring(p, q);
        // String name = tag.substring(q + 1);

        tags.add(new TagQuery(host, tag));
      }
    }

    getLinkStream()
      .queryResult(id, from, to, tags.toArray(new TagQuery[tags.size()]));

    return true;
  }

  /**
   * @deprecated
   **/
  @QueryGet
  public boolean listHosts(long id,
                           String to,
                           String from,
                           ListHostsQuery query)
  {
    List<HostQuery> hosts = new ArrayList<HostQuery>();

    for (HostController controller : _server.getHostControllers()) {
      if ("admin.resin".equals(controller.getName()))
         continue;

      HostQuery q = new HostQuery();
      q.setName(controller.getName());

      hosts.add(q);
    }

    getLinkStream()
      .queryResult(id, from, to, hosts.toArray(new HostQuery[hosts.size()]));

    return true;
  }

  /**
   * @deprecated
   **/
  @QueryGet
  public boolean status(long id,
                        String to,
                        String from,
                        StatusQuery query)
  {
    String tag = query.getTag();

    String errorMessage = statusMessage(tag);
    String state = null;

    StatusQuery result = new StatusQuery(tag, state, errorMessage);

    getLinkStream().queryResult(id, from, to, result);

    return true;
  }

  private String statusMessage(String tag)
  {
    int p = tag.indexOf('/');
    int q = tag.indexOf('/', p + 1);
    int r = tag.lastIndexOf('/');

    if (p < 0 || q < 0 || r < 0)
      return L.l("'{0}' is an unknown type", tag);

    String type = tag.substring(0, p);
    // String stage = tag.substring(p + 1, q);
    String host = tag.substring(q + 1, r);
    String name = tag.substring(r + 1);

    // String state = null;
    String errorMessage = tag + " is an unknown resource";

    try {
      if (type.equals("ears")) {
        String pattern
          = "resin:type=EApp,Host=" + host + ",name=" + name;

        EAppMXBean ear = (EAppMXBean) Jmx.findGlobal(pattern);

        if (ear != null) {
          ear.update();
          // state = ear.getState();
          errorMessage = ear.getErrorMessage();

          return errorMessage;
        }
        else
          return L.l("'{0}' is an unknown ear", tag);
      }
      else if (type.equals("wars")) {
        String pattern
          = "resin:type=WebApp,Host=" + host + ",name=/" + name;

        WebAppMXBean war = (WebAppMXBean) Jmx.findGlobal(pattern);

        if (war != null) {
          war.update();
          // state = war.getState();
          errorMessage = war.getErrorMessage();

          return errorMessage;
        }
        else
          return L.l("'{0}' is an unknown war", tag);
      }
      else
        return L.l("'{0}' is an unknown tag", tag);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return errorMessage;
  }

  private DeployControllerMXBean findController(String tag)
  {
    int p = tag.indexOf('/');
    int q = tag.indexOf('/', p + 1);
    int r = tag.lastIndexOf('/');

    if (p < 0 || q < 0 || r < 0)
      return null;

    String type = tag.substring(0, p);
    // String stage = tag.substring(p + 1, q);

    String host;
    String name;

    if (q < r) {
      host = tag.substring(q + 1, r);
      name = tag.substring(r + 1);
    }
    else {
      host = tag.substring(q + 1);
      name = "";
    }

    try {
      if (type.equals("ears")) {
        String pattern
          = "resin:type=EApp,Host=" + host + ",name=" + name;

        EAppMXBean ear = (EAppMXBean) Jmx.findGlobal(pattern);

        return ear;
      }
      else if (type.equals("wars")) {
        String pattern
          = "resin:type=WebApp,Host=" + host + ",name=/" + name;

        WebAppMXBean war = (WebAppMXBean) Jmx.findGlobal(pattern);

        return war;
      }
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);
    }

    return null;
  }
}
