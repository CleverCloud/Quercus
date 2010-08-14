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

package com.caucho.netbeans;

import org.netbeans.modules.j2ee.deployment.devmodules.api.J2eeModule;
import org.netbeans.modules.j2ee.deployment.plugins.spi.config.ModuleConfiguration;
import org.openide.util.Lookup;

import java.util.logging.*;
import org.openide.util.lookup.Lookups;

public class ResinModuleConfiguration
  implements ModuleConfiguration
{
  private static final Logger log
    = Logger.getLogger(ResinModuleConfiguration.class.getName());
  
  private J2eeModule _module;
  
  public ResinModuleConfiguration(J2eeModule module)
  {
    _module = module;
  }

  public Lookup getLookup()
  {
    //return new ResinLookup();
    return Lookups.fixed(this);
  }

  public J2eeModule getJ2eeModule() 
  {
    return _module;
  }

  public void dispose() {
  }
}
