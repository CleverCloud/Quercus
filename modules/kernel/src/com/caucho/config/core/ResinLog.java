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

package com.caucho.config.core;

import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import java.util.logging.Logger;

/**
 * Logs an EL value.
 */
public class ResinLog {
  private static L10N L = new L10N(ResinLog.class);

  private String _name = "com.caucho.config.core.ResinLog";
  private CharBuffer _text = new CharBuffer();

  public ResinLog()
  {
  }

  /**
   * Sets the log name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * The value to be logged.
   */
  public void addText(String text)
  {
    _text.append(text);
  }

  /**
   * Initialization logs the data.
   */
  @PostConstruct
  public void init()
  {
    Logger log = Logger.getLogger(_name);

    log.info(_text.toString());
  }
}

