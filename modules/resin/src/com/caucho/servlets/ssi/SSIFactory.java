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
 * @author Sam
 */

package com.caucho.servlets.ssi;

import com.caucho.vfs.Path;

import java.util.HashMap;

/**
 * A factory that returns {@link Statement} for named commands.
 */
public class SSIFactory
{
  /**
   * Return a statement, or null if the cmd is not known.
   * @param cmd the name of a command
   * @param attr a map of attributes and values
   * @param path a path to the .shtml file
   *
   * @return the Statement, or null.
   */
  public Statement createStatement(String cmd, HashMap<String,String> attr, Path path)
  {
    if ("config".equals(cmd))
      return ConfigStatement.create(attr, path);
    else if ("echo".equals(cmd))
      return EchoStatement.create(attr, path);
    else if ("include".equals(cmd))
      return IncludeStatement.create(attr, path);
    else if ("set".equals(cmd))
      return SetStatement.create(attr, path);
    else if ("if".equals(cmd))
      return IfStatement.create(attr, path);
    else if ("else".equals(cmd))
      return ElseStatement.create(attr, path);
    else if ("elif".equals(cmd))
      return ElifStatement.create(attr, path);
    else if ("endif".equals(cmd))
      return EndifStatement.create(attr, path);
    else
      return null;
  }
}
