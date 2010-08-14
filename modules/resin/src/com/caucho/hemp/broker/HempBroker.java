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

package com.caucho.hemp.broker;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;

import com.caucho.bam.Actor;
import com.caucho.bam.ActorError;
import com.caucho.bam.ActorStream;
import com.caucho.bam.Broker;
import com.caucho.bam.BrokerListener;
import com.caucho.config.inject.InjectManager;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.server.admin.AdminService;
import com.caucho.util.Alarm;
import com.caucho.util.Base64;
import com.caucho.util.L10N;

/**
 * Broker
 */
public class HempBroker
  implements Broker, ActorStream, Extension
{
  private static final Logger log
    = Logger.getLogger(HempBroker.class.getName());
  private static final L10N L = new L10N(HempBroker.class);
  
  private final static EnvironmentLocal<HempBroker> _localBroker
    = new EnvironmentLocal<HempBroker>();

  private final AtomicLong _jidGenerator
    = new AtomicLong(Alarm.getCurrentTime());

  private HempBrokerManager _manager;
  private DomainManager _domainManager;

  // actors and clients
  private final
    ConcurrentHashMap<String,WeakReference<ActorStream>> _actorStreamMap
    = new ConcurrentHashMap<String,WeakReference<ActorStream>>();

  // permanent registered actors
  private final HashMap<String,ActorStream> _actorMap
    = new HashMap<String,ActorStream>();

  private final Map<String,WeakReference<ActorStream>> _actorCache
    = Collections.synchronizedMap(new HashMap<String,WeakReference<ActorStream>>());

  private String _domain = "localhost";
  private String _managerJid = "localhost";

  private ArrayList<String> _aliasList = new ArrayList<String>();

  private BrokerListener []_actorManagerList
    = new BrokerListener[0];

  private volatile boolean _isClosed;

  public HempBroker(HempBrokerManager manager)
  {
    _manager = manager;

    Environment.addCloseListener(this);

    if (_localBroker.getLevel() == null)
      _localBroker.set(this);
  }

  public HempBroker(HempBrokerManager manager, String domain)
  {
    this(manager);
    
    _domain = domain;
    _managerJid = domain;
  }

  public static HempBroker getCurrent()
  {
    return _localBroker.get();
  }
  
  public void setDomainManager(DomainManager domainManager)
  {
    _domainManager = domainManager;
  }

  /**
   * Returns true if the broker is closed
   */
  @Override
  public boolean isClosed()
  {
    return _isClosed;
  }

  /**
   * Adds a domain alias
   */
  public void addAlias(String domain)
  {
    _aliasList.add(domain);
  }

  /**
   * Returns the stream to the broker
   */
  @Override
  public ActorStream getBrokerStream()
  {
    return this;
  }

  //
  // configuration
  //

  /**
   * Adds a broker implementation, e.g. the IM broker.
   */
  public void addBrokerListener(BrokerListener actorManager)
  {
    BrokerListener []actorManagerList
      = new BrokerListener[_actorManagerList.length + 1];

    System.arraycopy(_actorManagerList, 0, actorManagerList, 0,
                     _actorManagerList.length);
    actorManagerList[actorManagerList.length - 1] = actorManager;
    _actorManagerList = actorManagerList;
  }

  //
  // API
  //

  /**
   * Creates a session
   */
  public String createClient(ActorStream clientStream,
                             String uid,
                             String resourceId)
  {
    String jid = generateJid(uid, resourceId);

    _actorStreamMap.put(jid, new WeakReference<ActorStream>(clientStream));

    if (log.isLoggable(Level.FINE))
      log.fine(clientStream + " " + jid + " created");

    return jid;
  }

  protected String generateJid(String uid, String resource)
  {
    StringBuilder sb = new StringBuilder();

    if (uid == null)
      uid = "anonymous";

    if (uid.indexOf('@') > 0)
      sb.append(uid);
    else
      sb.append(uid).append('@').append(getDomain());
    sb.append("/");

    if (resource != null)
      sb.append(resource);
    else {
      Base64.encode(sb, _jidGenerator.incrementAndGet());
    }

    return sb.toString();
  }

  /**
   * Registers a actor
   */
  @Override
  public void addActor(ActorStream actor)
  {
    String jid = actor.getJid();

    synchronized (_actorMap) {
      ActorStream oldActor = _actorMap.get(jid);

      if (oldActor != null)
        throw new IllegalStateException(L.l("duplicated jid='{0}' is not allowed",
                                            jid));

      _actorMap.put(jid, actor);
    }

    synchronized (_actorStreamMap) {
      WeakReference<ActorStream> oldRef = _actorStreamMap.get(jid);

      if (oldRef != null && oldRef.get() != null)
        throw new IllegalStateException(L.l("duplicated jid='{0}' is not allowed",
                                            jid));

      _actorStreamMap.put(jid, new WeakReference<ActorStream>(actor));
    }

    if (log.isLoggable(Level.FINE))
      log.fine(this + " addActor jid=" + jid + " " + actor);
 }

  /**
   * Removes a actor
   */
  public void removeActor(ActorStream actor)
  {
    String jid = actor.getJid();

    synchronized (_actorMap) {
      _actorMap.remove(jid);
    }

    synchronized (_actorStreamMap) {
      _actorStreamMap.remove(jid);
    }

    if (log.isLoggable(Level.FINE))
      log.fine(this + " removeActor jid=" + jid + " " + actor);
  }

  /**
   * Returns the manager's own id.
   */
  protected String getManagerJid()
  {
    return _managerJid;
  }

  /**
   * Returns the domain
   */
  protected String getDomain()
  {
    return _domain;
  }

  /**
   * getJid() returns null for the broker
   */
  public String getJid()
  {
    return _domain;
  }

  /**
   * Sends a message
   */
  public void message(String to, String from, Serializable value)
  {
    ActorStream stream = findActorStream(to);

    if (stream != null)
      stream.message(to, from, value);
    else {
      log.fine(this + " sendMessage to=" + to + " from=" + from
               + " is an unknown actor stream.");
    }
  }

  /**
   * Sends a message
   */
  public void messageError(String to,
                               String from,
                               Serializable value,
                               ActorError error)
  {
    ActorStream stream = findActorStream(to);

    if (stream != null)
      stream.messageError(to, from, value, error);
    else {
      log.fine(this + " sendMessageError to=" + to + " from=" + from
               + " error=" + error + " is an unknown actor stream.");
    }
  }

  /**
   * Query an entity
   */
  public void queryGet(long id, String to, String from,
                              Serializable payload)
  {
    ActorStream stream = findActorStream(to);

    if (stream != null) {
      try {
        stream.queryGet(id, to, from, payload);
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);

        ActorError error = ActorError.create(e);

        queryError(id, from, to, payload, error);
      }

      return;
    }

    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " queryGet to unknown stream to='" + to
               + "' from=" + from);
    }

    String msg = L.l("'{0}' is an unknown actor for queryGet", to);

    ActorError error = new ActorError(ActorError.TYPE_CANCEL,
                                    ActorError.SERVICE_UNAVAILABLE,
                                    msg);

    queryError(id, from, to, payload, error);
  }

  /**
   * Query an entity
   */
  public void querySet(long id,
                       String to,
                       String from,
                       Serializable payload)
  {
    ActorStream stream = findActorStream(to);

    if (stream == null) {
      if (log.isLoggable(Level.FINE)) {
        log.fine(this + " querySet to unknown stream '" + to
                 + "' from=" + from);
      }

      String msg = L.l("'{0}' is an unknown actor for querySet", to);

      ActorError error = new ActorError(ActorError.TYPE_CANCEL,
                                      ActorError.SERVICE_UNAVAILABLE,
                                      msg);

      queryError(id, from, to, payload, error);

      return;
    }

    stream.querySet(id, to, from, payload);
  }

  /**
   * Query an entity
   */
  public void queryResult(long id, String to, String from, Serializable value)
  {
    ActorStream stream = findActorStream(to);

    if (stream != null)
      stream.queryResult(id, to, from, value);
    else
      throw new RuntimeException(L.l("{0}: {1} is an unknown actor stream.",
                                     this, to));
  }

  /**
   * Query an entity
   */
  public void queryError(long id,
                         String to,
                         String from,
                         Serializable payload,
                         ActorError error)
  {
    ActorStream stream = findActorStream(to);

    if (stream != null)
      stream.queryError(id, to, from, payload, error);
    else
      throw new RuntimeException(L.l("{0} is an unknown actor stream.", to));
  }

  protected ActorStream findActorStream(String jid)
  {
    if (jid == null)
      return null;

    WeakReference<ActorStream> ref = _actorStreamMap.get(jid);

    if (ref != null) {
      ActorStream stream = ref.get();

      if (stream != null)
        return stream;
    }

    if (jid.endsWith("@")) {
      // jms/3d00
      jid = jid + getDomain();
    }

    ActorStream actorStream;
    Actor actor = findParentActor(jid);

    if (actor == null) {
      return putActorStream(jid, findDomain(jid));
    }
    else if (jid.equals(actor.getJid())) {
      actorStream = actor.getActorStream();

      if (actorStream != null) {
        return putActorStream(jid, actorStream);
      }
    }
    else {
      /*
      if (! actor.startChild(jid))
        return null;
        */

      ref = _actorStreamMap.get(jid);

      if (ref != null)
        return ref.get();
    }

    return null;
  }

  private ActorStream putActorStream(String jid, ActorStream actorStream)
  {
    if (actorStream == null)
      return null;

    synchronized (_actorStreamMap) {
      WeakReference<ActorStream> ref = _actorStreamMap.get(jid);

      if (ref != null)
        return ref.get();

      _actorStreamMap.put(jid, new WeakReference<ActorStream>(actorStream));

      return actorStream;
    }
  }

  protected Actor findParentActor(String jid)
  {
    return null;
    /*
    if (jid == null)
      return null;

    WeakReference<Actor> ref = _actorCache.get(jid);

    if (ref != null)
      return ref.get();

    if (startActorFromManager(jid)) {
      ref = _actorCache.get(jid);

      if (ref != null)
        return ref.get();
    }

    if (jid.indexOf('/') < 0 && jid.indexOf('@') < 0) {
      Broker broker = _manager.findBroker(jid);
      Actor actor = null;

      if (actor != null) {
        ref = _actorCache.get(jid);

        if (ref != null)
          return ref.get();

        _actorCache.put(jid, new WeakReference<Actor>(actor));

        return actor;
      }
    }

    int p;

    if ((p = jid.indexOf('/')) > 0) {
      String uid = jid.substring(0, p);

      return findParentActor(uid);
    }
    else if ((p = jid.indexOf('@')) > 0) {
      String domainName = jid.substring(p + 1);

      return findParentActor(domainName);
    }
    else
      return null;
      */
  }

  protected ActorStream findDomain(String domain)
  {
    if (domain == null)
      return null;

    if ("local".equals(domain))
      return getBrokerStream();

    Broker broker = null;
    
    if (_manager != null)
      broker = _manager.findBroker(domain);

    if (broker == this)
      return null;

    ActorStream stream = null;

    if (_domainManager != null)
      stream = _domainManager.findDomain(domain);

    return stream;
  }

  protected boolean startActorFromManager(String jid)
  {
    for (BrokerListener manager : _actorManagerList) {
      /*
      if (manager.startActor(jid))
        return true;
        */
    }

    return false;
  }

  /**
   * Closes a connection
   */
  void closeActor(String jid)
  {
    int p = jid.indexOf('/');
    if (p > 0) {
      String owner = jid.substring(0, p);

      Actor actor = findParentActor(owner);

      /*
      if (actor != null) {
        try {
          actor.onChildStop(jid);
        } catch (Exception e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
      */
    }

    _actorCache.remove(jid);

    synchronized (_actorStreamMap) {
      _actorStreamMap.remove(jid);
    }
  }

  //
  // webbeans callbacks
  //

  public void addStartupActor(Bean bean,
                              String name,
                              int threadMax)
  {
    ActorStartup startup
      = new ActorStartup(bean, name, threadMax);

    Environment.addEnvironmentListener(startup);
  }

  private void startActor(Bean bean,
                          String name,
                          int threadMax)
  {
    InjectManager beanManager = InjectManager.getCurrent();

    Actor actor = (Actor) beanManager.getReference(bean);

    actor.setLinkStream(this);

    String jid = name;

    if (jid == null || "".equals(jid))
      jid = bean.getName();

    if (jid == null || "".equals(jid))
      jid = bean.getBeanClass().getSimpleName();

    if (jid.indexOf('@') < 0)
      jid = jid + '@' + getJid();
    else if (jid.endsWith("@"))
      jid = jid.substring(0, jid.length() - 1);

    actor.setJid(jid);

    Actor bamActor = actor;

    // queue
    if (threadMax > 0) {
      ActorStream actorStream = bamActor.getActorStream();
      actorStream = new HempMemoryQueue(actorStream, this, threadMax);
      bamActor.setActorStream(actorStream);
    }

    addActor(bamActor.getActorStream());

    Environment.addCloseListener(new ActorClose(bamActor));
  }

  private void startActor(Bean bean, AdminService bamService)
  {
    InjectManager beanManager = InjectManager.getCurrent();

    Actor actor = (Actor) beanManager.getReference(bean);

    actor.setLinkStream(this);

    String jid = bamService.name();

    if (jid == null || "".equals(jid))
      jid = bean.getName();

    if (jid == null || "".equals(jid))
      jid = bean.getBeanClass().getSimpleName();

    actor.setJid(jid);

    int threadMax = bamService.threadMax();

    Actor bamActor = actor;

    // queue
    if (threadMax > 0) {
      ActorStream actorStream = bamActor.getActorStream();
      actorStream = new HempMemoryQueue(actorStream, this, threadMax);
      bamActor.setActorStream(actorStream);
    }

    addActor(bamActor.getActorStream());

    Environment.addCloseListener(new ActorClose(bamActor));
  }

  public void close()
  {
    _isClosed = true;

    _manager.removeBroker(_domain);

    for (String alias : _aliasList)
      _manager.removeBroker(alias);

    _actorMap.clear();
    _actorCache.clear();
    _actorStreamMap.clear();
  }

  private String getJid(Actor actor, Annotation []annList)
  {
    com.caucho.remote.BamService bamAnn = findActor(annList);

    String name = "";

    if (bamAnn != null)
      name = bamAnn.name();

    if (name == null || "".equals(name))
      name = actor.getJid();

    if (name == null || "".equals(name))
      name = actor.getClass().getSimpleName();

    String jid = name;
    if (jid.indexOf('@') < 0 && jid.indexOf('/') < 0)
      jid = name + "@" + getJid();

    return jid;
  }

  private int getThreadMax(Annotation []annList)
  {
    com.caucho.remote.BamService bamAnn = findActor(annList);

    if (bamAnn != null)
      return bamAnn.threadMax();
    else
      return 1;
  }

  private com.caucho.remote.BamService findActor(Annotation []annList)
  {
    for (Annotation ann : annList) {
      if (ann.annotationType().equals(com.caucho.remote.BamService.class))
        return (com.caucho.remote.BamService) ann;

      // XXX: stereotypes
    }

    return null;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _domain + "]";
  }

  public class ActorStartup implements EnvironmentListener{
    private Bean<?> _bean;
    private String _name;
    private int _threadMax;

    ActorStartup(Bean<?> bean, String name, int threadMax)
    {
      _bean = bean;

      _name = name;
      _threadMax = threadMax;
    }

    Bean<?> getBean()
    {
      return _bean;
    }

    String getName()
    {
      return _name;
    }

    int getThreadMax()
    {
      return _threadMax;
    }

    public void environmentConfigure(EnvironmentClassLoader loader)
    {
    }

    public void environmentBind(EnvironmentClassLoader loader)
    {
    }

    public void environmentStart(EnvironmentClassLoader loader)
    {
      startActor(_bean, _name, _threadMax);
    }

    public void environmentStop(EnvironmentClassLoader loader)
    {
    }
  }

  public class ActorClose {
    private Actor _actor;

    ActorClose(Actor actor)
    {
      _actor = actor;
    }

    public void close()
    {
      removeActor(_actor.getActorStream());
    }
  }
}
