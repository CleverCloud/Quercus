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

package com.caucho.amber.cfg;

import com.caucho.amber.manager.AmberContainer;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.naming.*;
import com.caucho.config.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.loader.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

import javax.sql.DataSource;
import javax.naming.*;
import javax.persistence.spi.*;
import java.lang.instrument.*;
import java.security.*;
import java.net.URL;
import java.util.*;
import java.util.logging.*;

/**
 * <persistence-unit> tag in the persistence.xml
 */
public class PersistenceUnitProxy {
  private static final L10N L = new L10N(PersistenceUnitProxy.class);
  private static final Logger log
    = Logger.getLogger(PersistenceUnitProxy.class.getName());
  private String _name;

  private ArrayList<ConfigProgram> _programList
    = new ArrayList<ConfigProgram>();

  /**
   * Returns the unit name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the unit name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  public void addBuilderProgram(ConfigProgram program)
  {
    _programList.add(program);
  }

  public ArrayList<ConfigProgram> getProgramList()
  {
    return _programList;
  }
}
