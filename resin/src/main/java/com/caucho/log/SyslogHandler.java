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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.log;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import com.caucho.vfs.Syslog;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Logs to the syslog stream
 */
public class SyslogHandler extends Handler {
  private static final L10N L = new L10N(SyslogHandler.class);
  
  private int _facility = Syslog.LOG_DAEMON;
  private int _severity = Syslog.LOG_INFO;

  public SyslogHandler()
  {
  }

  /**
   * Sets the facility.
   */
  public void setFacility(String facility)
    throws ConfigException
  {
    if ("user".equals(facility))
      _facility = Syslog.LOG_USER;
    else if ("mail".equals(facility))
      _facility = Syslog.LOG_MAIL;
    else if ("daemon".equals(facility))
      _facility = Syslog.LOG_DAEMON;
    else if ("auth".equals(facility))
      _facility = Syslog.LOG_AUTH;
    else if ("lpr".equals(facility))
      _facility = Syslog.LOG_LPR;
    else if ("news".equals(facility))
      _facility = Syslog.LOG_NEWS;
    else if ("uucp".equals(facility))
      _facility = Syslog.LOG_UUCP;
    else if ("cron".equals(facility))
      _facility = Syslog.LOG_CRON;
    else if ("authpriv".equals(facility))
      _facility = Syslog.LOG_AUTHPRIV;
    else if ("ftp".equals(facility))
      _facility = Syslog.LOG_FTP;
    else if ("local0".equals(facility))
      _facility = Syslog.LOG_LOCAL0;
    else if ("local1".equals(facility))
      _facility = Syslog.LOG_LOCAL1;
    else if ("local2".equals(facility))
      _facility = Syslog.LOG_LOCAL2;
    else if ("local3".equals(facility))
      _facility = Syslog.LOG_LOCAL3;
    else if ("local4".equals(facility))
      _facility = Syslog.LOG_LOCAL4;
    else if ("local5".equals(facility))
      _facility = Syslog.LOG_LOCAL5;
    else if ("local6".equals(facility))
      _facility = Syslog.LOG_LOCAL6;
    else if ("local7".equals(facility))
      _facility = Syslog.LOG_LOCAL7;
    else
      throw new ConfigException(L.l("'{0}' is an unknown syslog facility.",
                                    facility));
  }

  /**
   * Sets the severity.
   */
  public void setSeverity(String severity)
    throws ConfigException
  {
    if ("emerg".equals(severity))
      _severity = Syslog.LOG_EMERG;
    else if ("alert".equals(severity))
      _severity = Syslog.LOG_ALERT;
    else if ("crit".equals(severity))
      _severity = Syslog.LOG_CRIT;
    else if ("err".equals(severity))
      _severity = Syslog.LOG_ERR;
    else if ("warning".equals(severity))
      _severity = Syslog.LOG_WARNING;
    else if ("notice".equals(severity))
      _severity = Syslog.LOG_NOTICE;
    else if ("info".equals(severity))
      _severity = Syslog.LOG_INFO;
    else if ("debug".equals(severity))
      _severity = Syslog.LOG_DEBUG;
    else
      throw new ConfigException(L.l("'{0}' is an unknown syslog severity.",
                                    severity));
  }

  /**
   * Publishes the record.
   */
  public void publish(LogRecord record)
  {
    if (! isLoggable(record))
      return;

    Syslog.syslog(_facility, _severity, record.getMessage());
  }

  /**
   * Flushes the buffer.
   */
  public void flush()
  {
  }

  /**
   * Closes the handler.
   */
  public void close()
  {
  }
}
