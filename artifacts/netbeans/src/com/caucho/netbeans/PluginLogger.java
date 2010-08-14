package com.caucho.netbeans;

import org.openide.ErrorManager;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;

public class PluginLogger
{
  private final ErrorManager _errorManager;

  private static final boolean _isDebug = true;

  public PluginLogger(Class<?> cl)
  {
    String name = cl.getName();

    _errorManager = ErrorManager.getDefault().getInstance(name);
  }

  public void log(Level level, String msg)
  {
    _errorManager.log(getLevel(level), msg);
  }

  public void log(Level level, Throwable t)
  {
    _errorManager.notify(getLevel(level), t);
  }

  private int getLevel(Level level)
  {
    if (level.intValue() >= Level.SEVERE.intValue())
      return ErrorManager.ERROR;
    else if (level.intValue() >= Level.WARNING.intValue())
      return ErrorManager.EXCEPTION;
    else if (level.intValue() >= Level.INFO.intValue())
      return ErrorManager.USER;
    else
      return ErrorManager.INFORMATIONAL;
  }

  public boolean isLoggable(Level level)
  {
    return _errorManager.isLoggable(getLevel(level));
  }

  static
  {
    if (_isDebug) {
      System.setProperty("com.caucho.level", "0");

      try {
        LogManager.getLogManager().readConfiguration();
      }
      catch (IOException e) {
        // no-op
      }
    }
  }

}
