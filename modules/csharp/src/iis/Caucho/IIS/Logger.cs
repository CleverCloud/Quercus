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
 * @author Alex Rojkov
 */

using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.Linq;
using System.Text;
using System.Diagnostics;
using System.Web.Configuration;

namespace Caucho.IIS
{
  public class Logger
  {
    private const String LOG_SOURCE = "Resin IIS Handler";

    private EventLog _log;
    private EventLogEntryType _logLevel;

    private static Logger _logger = null;

    internal Logger(EventLog log, EventLogEntryType logLevel)
    {
      _log = log;
      _logLevel = logLevel;
    }

    public static Logger GetLogger()
    {
      if (_logger != null)
        return _logger;

      NameValueCollection appSettings = WebConfigurationManager.GetSection("appSettings") as NameValueCollection;

      String loggingLevel = null;

      if (appSettings != null)
        loggingLevel = appSettings["resin.log-level"];

      if ("".Equals(loggingLevel))
        loggingLevel = null;

      if (_logger == null && !"None".Equals(loggingLevel, StringComparison.OrdinalIgnoreCase)) {
        try {
          if (!EventLog.SourceExists(LOG_SOURCE)) {
            EventLog.CreateEventSource(LOG_SOURCE, "Application");
          }

          EventLogEntryType logLevel;

          if ("Information".Equals(loggingLevel, StringComparison.OrdinalIgnoreCase))
            logLevel = EventLogEntryType.Information;
          else if ("Error".Equals(loggingLevel, StringComparison.OrdinalIgnoreCase))
            logLevel = EventLogEntryType.Error;
          else if ("Warning".Equals(loggingLevel, StringComparison.OrdinalIgnoreCase))
            logLevel = EventLogEntryType.Warning;
          else
            logLevel = EventLogEntryType.Error;

          EventLog log = new EventLog();
          log.Log = "Application";
          log.Source = LOG_SOURCE;

          String message = String.Format("Initializing logging at {0} logging level", loggingLevel);
          log.WriteEntry(message, EventLogEntryType.Information);

          Trace.TraceInformation(message);

          _logger = new Logger(log, logLevel);
        } catch (Exception) {
          //security does not allow to write create source or use EventLog
        }
      }

      if (_logger == null)
        _logger = new NoopLogger();

      return _logger;
    }

    public void Error(String message, params Object[] args)
    {
      Log(EventLogEntryType.Error, message, args);
    }

    public void Warning(String message, params Object[] args)
    {
      Log(EventLogEntryType.Warning, message, args);
    }

    public void Info(String message, params Object[] args)
    {
      Log(EventLogEntryType.Information, message, args);
    }

    public virtual void Log(EventLogEntryType entryType, String message, params Object[] args)
    {
      if (entryType <= _logLevel)
        _log.WriteEntry(String.Format(message, args), entryType);
    }

    internal bool IsLoggable(EventLogEntryType entryType)
    {
      return entryType < _logLevel;
    }
  }

  public class NoopLogger : Logger
  {

    public NoopLogger()
      : base(null, EventLogEntryType.FailureAudit)
    {
    }

    public override void Log(EventLogEntryType entryType, string message, object[] args)
    {
      //nothing
    }
  }
}