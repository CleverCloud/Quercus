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
 * @author Emil Ong
 */

package com.caucho.mule;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import java.util.ArrayList;
import javax.annotation.PostConstruct;

import org.mule.MuleManager;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.umo.UMOException;
import org.mule.umo.manager.UMOManager;

public class MuleConfig
{
  private static final L10N L = new L10N(MuleConfig.class);

  private ResinContainerContext _containerContext = new ResinContainerContext();
  private ArrayList<String> _configs = new ArrayList<String>();
  private UMOManager _muleManager = MuleManager.getInstance();
  
  @PostConstruct
  public void init()
    throws ConfigException
  {
    try {
      _muleManager.setContainerContext(_containerContext);
      MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
      builder.configure(getConfigList());
    }
    catch (UMOException e) {
      throw new ConfigException(L.l("Exception while configuring Mule"), e);
    }
  }

  public void addMuleConfig(String muleConfig)
  {
    _configs.add(muleConfig);
  }

  private String getConfigList()
  {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < _configs.size(); i++) { 
      sb.append(_configs.get(i));
      
      if (i != _configs.size() - 1)
        sb.append(',');
    }

    return sb.toString();
  }
}
