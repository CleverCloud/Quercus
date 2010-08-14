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

package com.caucho.server.rewrite;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.loader.Environment;
import com.caucho.management.server.RewriteImportMXBean;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;
import com.caucho.jmx.Description;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImportRule
  extends AbstractRule
  implements AlarmListener
{
  private static L10N L = new L10N(ImportRule.class);
  private static Logger log = Logger.getLogger(ImportRule.class.getName());

  private Path _path;
  private boolean _isOptional;
  private long _dependencyCheckInterval = 2 * 1000L;
  private long _errorCheckInterval = 2 * 1000L;

  private Depend _depend;

  private volatile MatchRule _matchRule;
  private volatile boolean _isError;
  private volatile String _redeployError;
  private volatile boolean _isDestroyed = false;
  private Alarm _alarm;

  protected ImportRule(RewriteDispatch rewriteDispatch)
  {
    super(rewriteDispatch);

    _dependencyCheckInterval = Environment.getDependencyCheckInterval();
  }

  public String getTagName()
  {
    return "import";
  }

  public void setDependencyCheckInterval(Period dependencyCheckInterval)
  {
    _dependencyCheckInterval = dependencyCheckInterval.getPeriod();
  }

  public long getDependencyCheckInterval()
  {
    return _dependencyCheckInterval;
  }

  public String getRedeployError()
  {
    return _redeployError;
  }

  public void setOptional(boolean isOptional)
  {
    _isOptional = isOptional;
  }

  public void setPath(Path path)
  {
    _path = path;
  }

  @Override
  public void init()
  {
    if (_path == null)
      throw new ConfigException(L.l("'path' attribute missing from resin:import."));

    if (getName() == null)
      setName(_path.getNativePath());

    try {
      load();
    }
    catch (ConfigException e) {
      throw e;
    }
    catch (Exception e) {
      throw ConfigException.create(e);
    }

    super.init();

    _alarm = new Alarm("rewrite-dispatch-import", this, _dependencyCheckInterval);
  }

  @Override
  protected RewriteRuleAdmin createAdmin()
  {
    return new RewriteImportAdmin(this);
  }

  public void setPassFilterChainMapper(FilterChainMapper passFilterChainMapper)
  {
    super.setPassFilterChainMapper(passFilterChainMapper);

    if (_matchRule != null)
      _matchRule.setPassFilterChainMapper(passFilterChainMapper);
  }

  public void setFailFilterChainMapper(FilterChainMapper failFilterChainMapper)
  {
    super.setFailFilterChainMapper(failFilterChainMapper);

    if (_matchRule != null)
      _matchRule.setFailFilterChainMapper(failFilterChainMapper);
  }

  public FilterChain map(String uri, String query, FilterChain accept)
    throws ServletException
  {
    if (isEnabled() && _matchRule != null)
      return _matchRule.map(uri, query, accept);
    else if (getPassFilterChainMapper() != null)
      return getPassFilterChainMapper().map(uri, query, accept);
    else
      return accept;
  }

  private synchronized void load()
    throws Exception
  {
    if (_isDestroyed)
      return;

    if (_depend != null && !_depend.isModified())
      return;

    try {
      _depend = new Depend(_path);

      _isError = true;

      MatchRule matchRule = new MatchRule(getRewriteDispatch());
      matchRule.setPassFilterChainMapper(getPassFilterChainMapper());
      matchRule.setFailFilterChainMapper(getFailFilterChainMapper());

      if (_path.canRead() && ! _path.isDirectory()) {
      }
      else if (_isOptional) {
        log.finer(L.l("import '{0}' is not readable.", _path));

        matchRule.init();

        _isError = false;

        setMatchRule(matchRule);

        return;
      }
      else {
        throw new ConfigException(L.l("Required file '{0}' can not be read for import.",
                                      _path.getNativePath()));
      }

      Config config = new Config();

      config.configure(matchRule, _path);

      _isError = false;

      setMatchRule(matchRule);
    }
    finally {
      clearCache();
    }
  }

  private void setMatchRule(MatchRule matchRule)
  {
    MatchRule oldMatchRule = _matchRule;

    if (oldMatchRule != null)
      oldMatchRule.unregister();

    matchRule.register();

    _matchRule = matchRule;

    if (oldMatchRule != null)
      oldMatchRule.destroy();
  }

  public void handleAlarm(Alarm alarm)
  {
    alarm = _alarm;

    if (_isDestroyed)
      return;

    try {
      _redeployError = null;

      load();
    }
    catch (Exception ex) {
      _redeployError = ex.toString();

      log.log(Level.WARNING, ex.toString(), ex);
    }
    finally {
      if (! _isDestroyed) {
        long delta = _isError ? _errorCheckInterval : _dependencyCheckInterval;

        alarm.queue(delta);
      }
    }
  }

  private void update()
  {
    handleAlarm(null);
  }

  @Override
  public void destroy()
  {
    try {
      _isDestroyed = true;

      Alarm alarm = _alarm;
      _alarm = null;

      MatchRule matchRule = _matchRule;
      _matchRule = null;

      if (alarm != null)
        alarm.dequeue();

      if (matchRule != null)
        matchRule.destroy();

    }
    finally {
      super.destroy();
    }
  }

  public static class RewriteImportAdmin
    extends RewriteRuleAdmin
    implements RewriteImportMXBean
  {
    private final ImportRule _rule;

    public RewriteImportAdmin(ImportRule rule)
    {
      super(rule);

      _rule = rule;
    }

    @Override
    public String getType()
    {
      return "RewriteImport";
    }

    public long getDependencyCheckInterval()
    {
      return _rule.getDependencyCheckInterval();
    }

    public String getRedeployError()
    {
      return _rule.getRedeployError();
    }

    @Description("Updates the imported rules if the file has changed")
    public void update()
    {
      _rule.update();
    }
  }
}
