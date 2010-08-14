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

import com.caucho.config.ConfigException;
import com.caucho.config.types.CronType;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.RewriteRuleMXBean;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.logging.Logger;

abstract public class AbstractRule
  implements Rule
{
  private static final L10N L = new L10N(AbstractRule.class);
  private static final Logger log = Logger.getLogger(AbstractRule.class.getName());

  private final RewriteDispatch _rewriteDispatch;

  private String _name;
  private volatile boolean _isEnabled = true;
  private CronType _enableAt;
  private CronType _disableAt;

  private String _logPrefix;

  private FilterChainMapper _passFilterChainMapper;
  private FilterChainMapper _failFilterChainMapper;

  private RewriteRuleAdmin _admin;

  private EnableAlarmListener _enableAlarmListener;
  private EnableAlarmListener _disableAlarmListener;

  public AbstractRule(RewriteDispatch rewriteDispatch)
  {
    _logPrefix = getTagName();
    _rewriteDispatch = rewriteDispatch;
  }

  protected RewriteDispatch getRewriteDispatch()
  {
    return _rewriteDispatch;
  }

  public void setName(String name)
  {
    _name = name;
  }

  public String getName()
  {
    return _name;
  }

  public void setEnabled(boolean isEnabled)
  {
    if (_isEnabled != isEnabled) {
      _isEnabled = isEnabled;

      _rewriteDispatch.clearCache();
    }
  }

  public boolean isEnabled()
  {
    return _isEnabled;
  }

  public void setDisableAt(CronType disableAt)
  {
    _disableAt = disableAt;
  }

  public void setEnableAt(CronType enableAt)
  {
    _enableAt = enableAt;
  }

  public void setPassFilterChainMapper(FilterChainMapper passFilterChainMapper)
  {
    _passFilterChainMapper = passFilterChainMapper;
  }

  protected final FilterChainMapper getPassFilterChainMapper()
  {
    return _passFilterChainMapper;
  }

  public void setFailFilterChainMapper(FilterChainMapper failFilterChainMapper)
  {
    _failFilterChainMapper = failFilterChainMapper;
  }

  protected final FilterChainMapper getFailFilterChainMapper()
  {
    return _failFilterChainMapper;
  }

  public void setLogPrefix(String logPrefix)
  {
    _logPrefix = logPrefix;
  }

  public String getLogPrefix()
  {
    return _logPrefix;
  }

  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_name == null &&  !_isEnabled && _enableAt == null)
        throw new ConfigException(L.l("{0} requires 'name' if enabled='false' and enable-at is undefined",
                                      getTagName()));
    if (_enableAt != null)
      _enableAlarmListener = new EnableAlarmListener(this, _enableAt, true);

    if (_disableAt != null)
      _disableAlarmListener = new EnableAlarmListener(this, _disableAt, false);
  }

  public void register()
  {
    if (_admin == null && _name != null) {
      _admin = createAdmin();
      _admin.register();
    }
  }

  public void unregister()
  {
    RewriteRuleAdmin admin = _admin;
    _admin = null;

    if (admin != null)
      admin.unregister();
  }

  protected RewriteRuleAdmin createAdmin()
  {
    return new RewriteRuleAdmin(this);
  }

  protected void clearCache()
  {
    _rewriteDispatch.clearCache();
  }

  @PreDestroy
  public void destroy()
  {
    try {
      unregister();
    }
    finally {
      EnableAlarmListener enableAlarmListener = _enableAlarmListener;
      _enableAlarmListener = null;

      EnableAlarmListener disableAlarmListener = _disableAlarmListener;
      _disableAlarmListener = null;

      if (enableAlarmListener != null)
        enableAlarmListener.destroy();

      if (disableAlarmListener != null)
        disableAlarmListener.destroy();
    }
  }

  public static class RewriteRuleAdmin
    extends AbstractManagedObject
    implements RewriteRuleMXBean
  {
    private final AbstractRule _rule;

    public RewriteRuleAdmin(AbstractRule rule)
    {
      _rule = rule;
    }

    public String getName()
    {
      return _rule.getName();
    }

    @Override
    public String getType()
    {
      return "RewriteRule";
    }

    public String getState()
    {
      if (_rule.isEnabled())
        return "active";
      else
        return "stopped";
    }

    public void start()
    {
      _rule.setEnabled(true);
    }

    public void stop()
    {
      _rule.setEnabled(false);
    }

    public void register()
    {
      registerSelf();
    }

    public void unregister()
    {
      unregisterSelf();
    }
  }

  private static class EnableAlarmListener
    implements AlarmListener
  {
    private final AbstractRule _rule;
    private final CronType _cron;
    private final boolean _isEnable;

    private volatile Alarm _alarm;

    public EnableAlarmListener(AbstractRule rule, CronType cron, boolean isEnable)
    {
      _rule = rule;
      _cron = cron;
      _isEnable = isEnable;

      String type = isEnable ? "enable" : "disable";
      _alarm = new Alarm("rewrite-rule-" + type, this);

      queue();
    }

    private void queue()
    {
      long now = Alarm.getCurrentTime();

      long nextTime = _cron.nextTime(now);

      Alarm alarm = _alarm;

      if (alarm == null)
        return;

      _rule.setEnabled(_isEnable);

      alarm.queue(nextTime - now);
    }

    public void handleAlarm(Alarm alarm)
    {
      alarm = _alarm;

      if (alarm == null)
        return;

      _rule.setEnabled(_isEnable);

      queue();
    }

    public void destroy()
    {
      Alarm alarm = _alarm;
      _alarm = null;

      if (alarm != null)
        alarm.dequeue();
    }
  }
}
