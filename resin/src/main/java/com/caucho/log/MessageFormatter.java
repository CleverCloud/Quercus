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

import com.caucho.vfs.StringWriter;
import com.caucho.vfs.WriteStream;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * A simple formatter that handles localization, substitution of
 * parameters, and the inclusion of an exception stack trace if applicable.
 */
public class MessageFormatter extends Formatter {
  public String format(LogRecord record) 
  {
    return formatMessage(record);
  }

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
  public String formatMessage(LogRecord record) 
  {
    String message = super.formatMessage(record);
    Throwable thrown = record.getThrown();

    try {
      if (thrown != null) {
        StringWriter sw = new StringWriter();
        WriteStream os =  sw.openWrite();

        if (message != null &&
            ! message.equals(thrown.toString()) &&
            ! message.equals(thrown.getMessage()))
          os.println(message);

        Throwable rootExn = thrown;

        // server/023g
        /*
        for (;
             rootExn != null && rootExn.getCause() != null;
             rootExn = rootExn.getCause()) {
        }
        */
        rootExn.printStackTrace(os.getPrintWriter());

        message = sw.getString();
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }

    return message;
  }
}

