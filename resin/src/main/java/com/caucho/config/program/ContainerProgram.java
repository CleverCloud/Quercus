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
 * @author Scott Ferguson;
 */

package com.caucho.config.program;

import java.util.ArrayList;

import javax.enterprise.context.spi.CreationalContext;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

public class ContainerProgram extends ConfigProgram {
  static final L10N L = new L10N(ContainerProgram.class);

  private ArrayList<ConfigProgram> _programList
    = new ArrayList<ConfigProgram>();


  public ArrayList<ConfigProgram> getProgramList()
  {
    return _programList;
  }
  
  /**
   * Adds a new program to the container
   * 
   * @param program the new program
   */
  public void addProgram(ConfigProgram program)
  {
    _programList.add(program);
  }

  /**
   * Adds a new program to the container
   * 
   * @param program the new program
   */
  public void addProgram(int index, ConfigProgram program)
  {
    _programList.add(index, program);
  }

  /**
   * Invokes the child programs on the bean
   * 
   * @param bean the bean to configure
   * @param env the configuration environment
   * 
   * @throws com.caucho.config.ConfigException
   */
  public <T> void inject(T bean, CreationalContext<T> env)
    throws ConfigException
  {
    int size = _programList.size();
    
    for (int i = 0; i < size; i++) {
      ConfigProgram program = _programList.get(i);

      program.inject(bean, env);
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + _programList;
  }
}
