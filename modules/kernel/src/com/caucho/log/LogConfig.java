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

package com.caucho.log;

import com.caucho.config.ConfigException;
import com.caucho.config.types.RawString;
import com.caucho.jmx.Jmx;
import com.caucho.loader.CloseListener;
import com.caucho.loader.Environment;
import com.caucho.management.server.*;
import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;

import javax.annotation.PostConstruct;
import javax.management.*;
import java.util.ArrayList;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Environment-specific configuration.
 */
public class LogConfig extends RotateLog {
  private static final Logger log
    = Logger.getLogger(LogConfig.class.getName());
  private static final L10N L = new L10N(LogConfig.class);
  
  private ArrayList<Handler> _handlers;
  private Formatter _formatter;
  private String _timestamp;

  private ArrayList<SubLogger> _subLoggers = new ArrayList<SubLogger>();
  private SubLogger _subLogger;

  private String _mbeanName;
  
  private boolean _isInit;

  public LogConfig()
  {
    setTimestamp("[%Y/%m/%d %H:%M:%S.%s] ");
  }

  /**
   * Sets the name of the logger to configure.
   *
   * @deprecated Use setName()
   */
  public void setId(String name)
  {
    // backward compat
    if (name.equals("/"))
      name = "";
    
    setName(name);
  }

  /**
   * Sets the name of the logger to configure.
   */
  public void setName(String name)
  {
    getSubLogger().setName(name);
  }

  /**
   * Returns the name of the logger to configure.
   */
  public String getName()
  {
    return getSubLogger().getName();
  }

  /**
   * Sets the mbean-name of the logger to configure.
   */
  public void setMbeanName(String name)
  {
    _mbeanName = name;
  }

  /**
   * Sets the use-parent-handlers
   */
  public void setUseParentHandlers(boolean useParentHandlers)
    throws ConfigException
  {
    getSubLogger().setUseParentHandlers(useParentHandlers);
  }

  /**
   * Sets the output level.
   */
  public void setLevel(String level)
    throws ConfigException
  {
    getSubLogger().setLevel(level);

    if (_isInit && _handlers != null) {
      for (Handler handler : _handlers) {
        handler.setLevel(getSubLogger().getLevel());
      }
    }
  }

  /**
   * Sets the output level.
   */
  public String getLevel()
  {
    Level level = getSubLogger().getLevel();

    if (level != null)
      return level.getName();
    else
      return Level.INFO.getName();
  }

  /**
   * Sets the sublogger.
   */
  private SubLogger getSubLogger()
  {
    if (_subLogger == null) {
      _subLogger = new SubLogger();
      _subLoggers.add(_subLogger);
    }

    return _subLogger;
  }

  /**
   * Sets the timestamp.
   */
  public void setTimestamp(String timestamp)
  {
    _timestamp = timestamp;
  }

  /**
   * A format string uses EL expressions and the EL variable `log', which is an
   * instance of LogRecord.
   */
  public void setFormat(RawString format)
  {
    if (_formatter == null) {
      _formatter = new ELFormatter();
    }

    if (_formatter instanceof ELFormatter)
      ((ELFormatter)_formatter).setFormat(format);
  }

  /**
   * A format string uses EL expressions and the EL variable `log', which is an
   * instance of LogRecord.
   */
  public String getFormat()
  {
    if (_formatter != null && _formatter instanceof ELFormatter)
      return ((ELFormatter)_formatter).getFormat();
    else
      return null;
  }

  /**
   * Sets the formatter.
   */
  public void setFormatter(Formatter formatter)
  {
    _formatter = formatter;
  }

  /**
   * Adds a handler
   */
  public void addHandler(Handler handler)
  {
    if (handler == null)
      throw new NullPointerException();
    
    if (_handlers == null)
      _handlers = new ArrayList<Handler>();

    _handlers.add(handler);
  }

  /**
   * Returns the tag name.
   */
  public String getTagName()
  {
    return "log";
  }

  /**
   * Adds a logger.
   */
  public void addLogger(LoggerConfig logger)
  {
  }

  /**
   * Initialize the log.
   */
  @PostConstruct
  public void init()
    throws ConfigException, java.io.IOException
  {
    _isInit = true;

    if (_handlers == null)
      super.init();
    
    if (_subLoggers.size() == 0)
      getSubLogger();

    if (_subLogger != null)
      _subLogger.init();
    
    if (_formatter instanceof ELFormatter) {
      ((ELFormatter)_formatter).init();
    }

    WriteStream os = null;
    
    if (_handlers == null) {
      os = getRotateStream().getStream();

      if (_timestamp != null) {
        TimestampFilter filter = new TimestampFilter();
        filter.setTimestamp(_timestamp);
        filter.setStream(os);
        filter.setNullDelimited(true);
        os = new WriteStream(filter);
      }

      String encoding = System.getProperty("file.encoding");
      
      if (encoding != null)
        os.setEncoding(encoding);
      
      os.setDisableClose(true);
    }

    boolean hasCloseListener = false;
    for (int i = 0; i < _subLoggers.size(); i++) {
      SubLogger subLogger = _subLoggers.get(i);
      Logger logger = subLogger.getLogger();

      Level subLevel = subLogger.getLevel();
      Level level = logger.getLevel();

      if (subLevel != null
          && (level == null && subLevel.intValue() < Level.INFO.intValue()
              || level != null && subLevel.intValue() < level.intValue())) {
        logger.setLevel(subLogger.getLevel());
      }

      if (_handlers == null) {
        StreamHandler handler = new StreamHandler(os);
        handler.setFormatter(_formatter);
        handler.setNullDelimited(true);

        _handlers = new ArrayList<Handler>();
        _handlers.add(handler);
      }

      for (int j = 0; j < _handlers.size(); j++) {
        SubHandler subHandler = new SubHandler(_handlers.get(j));

        if (subLogger.getLevel() != null)
          subHandler.setLevel(subLogger.getLevel());

        if (! (logger instanceof EnvironmentLogger)) {
          CloseListener listener = new CloseListener(subHandler);

          Environment.addClassLoaderListener(listener);
        }

        logger.addHandler(subHandler);
      }
    }

    if (_mbeanName != null) {
      try {
        ObjectName objectName = Jmx.getObjectName(_mbeanName);
        
        Jmx.register(new LogAdmin(), objectName, LoggerMXBean.class);
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  static Level toLevel(String level)
    throws ConfigException
  {
    if (level.equals("off"))
      return Level.OFF;
    else if (level.equals("severe"))
      return Level.SEVERE;
    else if (level.equals("warning"))
      return Level.WARNING;
    else if (level.equals("info"))
      return Level.INFO;
    else if (level.equals("config"))
      return Level.CONFIG;
    else if (level.equals("fine"))
      return Level.FINE;
    else if (level.equals("finer"))
      return Level.FINER;
    else if (level.equals("finest"))
      return Level.FINEST;
    else if (level.equals("all"))
      return Level.ALL;
    else
      throw new ConfigException(L.l("`{0}' is an unknown log level.  Log levels are:\noff - disable logging\nsevere - severe errors only\nwarning - warnings\ninfo - information\nconfig - configuration\nfine - fine debugging\nfiner - finer debugging\nfinest - finest debugging\nall - all debugging", level));
  }

  public static class SubLogger {
    private Logger _logger;
    private String _name = "";
    private Level _level;
    private boolean _useParentHandlers = true;

    // for mbean management
    private Handler _handler;

    /**
     * Sets the name of the logger to configure.
     */
    public void setId(String name)
    {
      // backward compat
      if (name.equals("/"))
        name = "";
    
      setName(name);
    }

    /**
     * Sets the name of the logger to configure.
     */
    public void setName(String name)
    {
      _name = name;
    }

    /**
     * Gets the name of the logger to configure.
     */
    public String getName()
    {
      return _name;
    }

    /**
     * Sets the use-parent-handlers
     */
    public void setUseParentHandlers(boolean useParentHandlers)
      throws ConfigException
    {
      _logger = Logger.getLogger(_name);

      _logger.setUseParentHandlers(useParentHandlers);

      _useParentHandlers = useParentHandlers;
    }

    /**
     * Gets the logger.
     */
    public Logger getLogger()
    {
      return Logger.getLogger(_name);
    }

    /**
     * Gets the output level.
     */
    public Level getLevel()
    {
      return _level;
    }

    /**
     * Sets the output level.
     */
    public void setLevel(String level)
      throws ConfigException
    {
      if (level.equals("off"))
        _level = Level.OFF;
      else if (level.equals("severe"))
        _level = Level.SEVERE;
      else if (level.equals("warning"))
        _level = Level.WARNING;
      else if (level.equals("info"))
        _level = Level.INFO;
      else if (level.equals("config"))
        _level = Level.CONFIG;
      else if (level.equals("fine"))
        _level = Level.FINE;
      else if (level.equals("finer"))
        _level = Level.FINER;
      else if (level.equals("finest"))
        _level = Level.FINEST;
      else if (level.equals("all"))
        _level = Level.ALL;
      else
        throw new ConfigException(L.l("`{0}' is an unknown log level.  Log levels are:\noff - disable logging\nsevere - severe errors only\nwarning - warnings\ninfo - information\nconfig - configuration\nfine - fine debugging\nfiner - finer debugging\nfinest - finest debugging\nall - all debugging", level));
    }

    /**
     * Sets the handler.
     */
    public void setHandler(Handler handler)
    {
      _handler = handler;
    }

    /**
     * Gets the handler.
     */
    public Handler getHandler()
    {
      return _handler;
    }

    @PostConstruct
    public void init()
      throws ConfigException
    {
      if (_name == null)
        throw new ConfigException(L.l("`name' is a required attribute..  Each logger must configure the log name, e.g. com.caucho.server.webapp."));
    }
  }

  class LogAdmin extends AbstractManagedObject implements LoggerMXBean {
    public String getType()
    {
      return "Logger";
    }

    public String getName()
    {
      return LogConfig.this.getName();
    }

    public String getLevel()
    {
      return LogConfig.this.getLevel();
    }

    public void setLevel(String level)
    {
      if (_subLogger != null) {
        _subLogger.setLevel(level);
        _subLogger.getLogger().setLevel(_subLogger.getLevel());
      }

      for (int i = 0; i < _subLoggers.size(); i++) {
        SubLogger subLogger = _subLoggers.get(i);
        
        subLogger.setLevel(level);
        subLogger.getLogger().setLevel(_subLogger.getLevel());
      }
    }
  }
}
