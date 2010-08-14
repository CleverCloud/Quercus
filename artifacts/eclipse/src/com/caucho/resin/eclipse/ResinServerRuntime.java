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

package com.caucho.resin.eclipse;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.generic.core.internal.CorePlugin;
import org.eclipse.jst.server.generic.core.internal.GenericServerRuntime;
import org.eclipse.jst.server.generic.servertype.definition.ServerRuntime;

@SuppressWarnings("restriction")
public class ResinServerRuntime extends GenericServerRuntime {

  @Override
  protected IStatus validateClasspaths(ServerRuntime serverTypeDefinition)
  {
    // Just pretend that the class paths are valid until we've actually
    // had a chance to set the resin.home    
    String serverPropertiesEntered =
      (String) getServerInstanceProperties().get(ResinServerWizardFragment.SERVER_PROPERTIES_ENTERED);
      
    if (! "true".equals(serverPropertiesEntered))
      return new Status(IStatus.OK, CorePlugin.PLUGIN_ID, 0, "", null);
    
    return super.validateClasspaths(serverTypeDefinition);
  }

}
