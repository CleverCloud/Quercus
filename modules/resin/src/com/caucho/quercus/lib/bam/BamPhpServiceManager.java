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

package com.caucho.quercus.lib.bam;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.ActorMissingEvent;
import com.caucho.bam.BrokerListener;
import com.caucho.bam.Broker;
import com.caucho.hemp.broker.HempBroker;
import com.caucho.config.ConfigException;
import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.ResinQuercus;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.JavaValue;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.quercus.page.InterpretedPage;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.util.L10N;
import com.caucho.vfs.NullWriteStream;
import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;
import com.caucho.xmpp.disco.DiscoInfoQuery;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

/**
 * BAM actor that calls into a PHP script to handle messages/queries.
 **/
public class BamPhpServiceManager implements BrokerListener {
  private static final L10N L = new L10N(BamPhpServiceManager.class);
  private static final Logger log
    = Logger.getLogger(BamPhpServiceManager.class.getName());

  private final QuercusContext _quercus = new ResinQuercus();

  private final HashMap<String,BamPhpActor> _children = 
    new HashMap<String,BamPhpActor>();

  private ArrayList<String> _featureNames = new ArrayList<String>();

  private QuercusProgram _program;
  private Path _script;
  private String _encoding = "ISO-8859-1";

  Broker _broker = HempBroker.getCurrent();

  public BamPhpServiceManager()
  {
  }

  public BamPhpServiceManager(Path script, String encoding)
  {
    _script = script;
    _encoding = encoding;
  }

  public Path getScript()
  {
    return _script;
  }

  public void setScript(Path script)
  {
    _script = script;
  }

  public String getEncoding()
  {
    return _encoding;
  }

  public void setEncoding(String encoding)
  {
    _encoding = encoding;
  }

  Broker getBroker()
  {
    return _broker;
  }

  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_script == null)
      throw new ConfigException(L.l("script path not specified"));

    _quercus.init();
    _quercus.start();

    _broker.addBrokerListener(this);
  }

  public boolean startActor(String jid)
  {
    Env env = null;
    boolean started = false;

    try {
      QuercusPage page = _quercus.parse(_script);
      env = createEnv(page, "_quercus_bam_start_service", jid);
      page.executeTop(env);

      started = env.getGlobalValue("_quercus_bam_function_return").toBoolean();
    }
    finally {
      if (env != null)
        env.close();

      return started;
    }
  }

  public boolean stopActor(String jid)
  {
    Env env = null;
    boolean stoped = false;

    try {
      QuercusPage page = _quercus.parse(_script);
      env = createEnv(page, "_quercus_bam_stop_service", jid);
      page.executeTop(env);

      stoped = env.getGlobalValue("_quercus_bam_function_return").toBoolean();
    }
    finally {
      if (env != null)
        env.close();

      return stoped;
    }
  }

  boolean hasChild(String jid)
  {
    return _children.containsKey(jid);
  }

  BamPhpActor removeChild(String jid)
  {
    return _children.remove(jid);
  }

  void addChild(String jid, BamPhpActor child)
  {
    _children.put(jid, child);
  }

  private Env createEnv(QuercusPage page, String type, String jid)
  {
    WriteStream out = new NullWriteStream();

    Env env = new Env(_quercus, page, out, null, null);
    env.start();

    JavaClassDef actorClassDef = 
      env.getJavaClassDefinition(BamPhpServiceManager.class);

    env.setGlobalValue("_quercus_bam_service_manager", 
                       actorClassDef.wrap(env, this));
    env.setGlobalValue(type, BooleanValue.TRUE);
    env.setGlobalValue("_quercus_bam_service_jid", StringValue.create(jid));

    return env;
  }

  public String toString()
  {
    return "BamPhpServiceManager[script=" + _script + "]";
  }

  @Override
  public void hostMissing(ActorMissingEvent event)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void resourceMissing(ActorMissingEvent event)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void userMissing(ActorMissingEvent event)
  {
    // TODO Auto-generated method stub
    
  }
}
