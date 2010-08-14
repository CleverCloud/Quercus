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

import com.caucho.config.ConfigELContext;
import com.caucho.config.ConfigException;
import com.caucho.config.scope.ThreadRequestFactory;
import com.caucho.config.types.RawString;
import com.caucho.el.AbstractVariableResolver;
import com.caucho.el.ELParser;
import com.caucho.el.Expr;
import com.caucho.util.FreeList;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.el.ELContext;
import javax.el.ELException;
import javax.servlet.http.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A Formatter that accepts an EL format string, and.
 */
public class ELFormatter extends MessageFormatter {
  private static final L10N L = new L10N(ELFormatter.class);

  private static final ThreadLocal<LogRecord> _threadLogRecord
    = new ThreadLocal<LogRecord>();

  private final FreeList<ELContext> _freeContextList
    = new FreeList<ELContext>(8);

  private String _format;
  private Expr _expr;
  
  public ELFormatter()
  {
  }

  public void setFormat(RawString format)
  {
    _format = format.getValue();
  }

  public String getFormat()
  {
    return _format;
  }

  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_format != null) {
      try {
        ELParser elParser = new ELParser(new ConfigELContext(), _format);
        
        _expr = elParser.parse();
      } catch (Exception ex) {
        throw ConfigException.create(ex);
      }
    }
  }

  public String format(LogRecord logRecord)
  {
    if (_expr == null) {
      return super.format(logRecord);
    }

    String ret;
    if (_expr == null) {
      ret = super.format(logRecord);
    }
    else {
      LogRecord oldLogRecord = _threadLogRecord.get();
      
      try {
        _threadLogRecord.set(logRecord);

        ELContext context = _freeContextList.allocate();

        if (context == null) {
          ELFormatterVariableResolver vr = new ELFormatterVariableResolver();
          context = new ConfigELContext(vr);
        }

        ret = _expr.evalString(context);

        _freeContextList.free(context);
      } 
      catch (Exception ex) {
        throw new RuntimeException(ex);
      }
      finally {
        _threadLogRecord.set(oldLogRecord);
      }
    }

    return ret;
  }

  class ELFormatterVariableResolver extends AbstractVariableResolver {
    private ELFormatterLogRecord _logRecord
      = new ELFormatterLogRecord();

    @Override
    public Object getValue(ELContext env, Object base, Object property)
      throws ELException 
    {
      if (base != null || ! (property instanceof String))
        return null;

      if ("log".equals(property)) {
        env.setPropertyResolved(true);

        return _logRecord;
      }
      else if ("request".equals(property)) {
        env.setPropertyResolved(true);

        return ThreadRequestFactory.getCurrentHttpRequest();
      }
      else if ("cookie".equals(property)) {
        env.setPropertyResolved(true);

        HttpServletRequest req = ThreadRequestFactory.getCurrentHttpRequest();

        if (req != null) {
          Cookie []cookies = req.getCookies();

          if (cookies != null)
            return new CookieMap(cookies);
        }

        return null;
      }
      else if ("session".equals(property)) {
        env.setPropertyResolved(true);

        HttpServletRequest req = ThreadRequestFactory.getCurrentHttpRequest();
        
        if (req != null) {
          HttpSession session = req.getSession(false);

          return session;
        }

        return null;
      }

      return null;
    }
  }

  /**
   * An api similar to java.util.logging.LogRecord that provides more complete
   * information for logging purposes.
   */
  public class ELFormatterLogRecord
  {
    /**
     * The "formatted" log message, after localization, substitution of
     * parameters, and the inclusion of an exception stack trace if applicable.
     * <p>
     * During formatting, if the source logger has a localization
     * ResourceBundle and if that ResourceBundle has an entry for
     * this message string, then the message string is replaced
     * with the localized value.
     * <p>
     * If the message has parameters, java.text.MessageFormat is used to format
     * the message with the parameters.
     * <p>
     * If the log record has an associated exception, the stack trace is
     * appended to the log message.
     *
     * @see java.text.MessageFormat 
     * @see java.lang.Throwable.printStackTrace() 
     */ 
    public String getMessage()
    { 
      return formatMessage(getLogRecord()); 
    }

    /** 
     * The source Logger's name.
     *
     * @return source logger name, which may be null
     */ 
    public String getName()
    {
      return getLogRecord().getLoggerName();
    }
   
    /** 
     * The source Logger's name.
     *
     * @return source logger name, which may be null
     */ 
    public String getLoggerName()
    {
      return getLogRecord().getLoggerName();
    }
   
    /** 
     * The last component of the source Logger's name.  The last component
     * is everything that occurs after the last `.' character, usually 
     * it is the class name. 
     *
     *
     * @return short version of the source logger name, or null
     */ 
    public String getShortName()
    { 
      String name = getLogRecord().getLoggerName();

      if (name != null) {
        int index = name.lastIndexOf('.') + 1;
        if (index > 0 && index < name.length()) {
          name = name.substring(index);
        }
      }

      return name;
    }
   
    /**
     * The logging message level, for example Level.INFO.
     *
     * @see java.util.logging.Level
     */
    public Level getLevel()
    {
      return getLogRecord().getLevel();
    }

    /** 
     * The time of the logging event, in milliseconds since 1970.
     */ 
    public long getMillis()
    {
      return getLogRecord().getMillis();
    }

    /** 
     * An identifier for the thread where the message originated.
     */ 
    public int getThreadID()
    {
      return getLogRecord().getThreadID();
    }

    /** 
     * The throwable associated with the log record, if one was associated.
     */ 
    public Throwable getThrown()
    {
      return getLogRecord().getThrown();
    }

    /** 
     * The sequence number, normally assigned in the constructor of LogRecord.
     */ 
    public long getSequenceNumber()
    {
      return getLogRecord().getSequenceNumber();
    }

    /** 
     * The name of the class that issued the logging request.  
     * This name may be unavailable, or not actually the name of the class that
     * issued the logging message.
     */
    public String getSourceClassName()
    {
      return getLogRecord().getSourceClassName();
    }

    /** 
     * The last component of the name (everthing after the last `.') of the
     * class that issued the logging request.
     * This name may be unavailable, or not actually the name of the class that
     * issued the logging message.
     *
     * @return short version of the sourceClassName
     */ 
    public String getShortSourceClassName()
    { 
      String name = getLogRecord().getSourceClassName();

      if (name != null) {
        int index = name.lastIndexOf('.') + 1;
        if (index > 0 && index < name.length()) {
          name = name.substring(index);
        }
      }

      return name;
    }
   
    /** 
     * The name of the method that issued the logging request.  This name
     * may be unavailable, or not actually the name of the class that issued
     * the logging message.
     */
    public String getSourceMethodName()
    {
      return getLogRecord().getSourceMethodName();
    }

    /**
     * The "raw" log message, before localization or substitution 
     * of parameters. 
     * <p>
     * This returned message will be either the final text, text containing
     * parameter substitution "format elements" (like `{0}') for use by
     * java.text.MessageFormat, or a localization key.
     *
     * @see java.text.MessageFormat 
     */ 
    public String getRawMessage()
    {
      return getLogRecord().getMessage();
    }

    /** 
     * The resource bundle for localization.
     */ 
    public ResourceBundle getResourceBundle()
    {
      return getLogRecord().getResourceBundle();
    }
   
    /** 
     * The name of resource bundle for localization.
     */ 
    public String getResourceBundleName()
    {
      return getLogRecord().getResourceBundleName();
    }
   
    public Object[] getParameters()
    {
      return getLogRecord().getParameters();
    }

    private LogRecord getLogRecord()
    {
      return _threadLogRecord.get();
    }
  }

  public static class CookieMap extends AbstractMap {
    private Cookie []_cookies;

    CookieMap(Cookie []cookies)
    {
      _cookies = cookies;
    }
    
    public Object get(Object key)
    {
      for (Cookie cookie : _cookies) {
        if (cookie.getName().equals(key))
          return cookie;
      }
      return null;
    }

    public int size()
    {
      return _cookies.length;
    }

    public Set entrySet()
    {
      return null;
    }
  }
}

