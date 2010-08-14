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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.generic.core.internal.CorePlugin;
import org.eclipse.jst.server.generic.internal.core.util.FileUtil;
import org.eclipse.jst.server.generic.servertype.definition.Publisher;
import org.eclipse.jst.server.generic.servertype.definition.PublisherData;
import org.eclipse.jst.server.generic.servertype.definition.ServerRuntime;
import org.osgi.framework.Bundle;

/**
 * @author Emil Ong
 *
 */
@SuppressWarnings("restriction")
class PublisherUtil {
  public static File locateBundleFile(ServerRuntime typeDef, String filename) 
    throws CoreException 
  {
    String bundleName = typeDef.getConfigurationElementNamespace();
    Bundle bundle = Platform.getBundle(bundleName);
    URL bundleUrl = bundle.getEntry(filename);
    URL fileUrl = FileUtil.resolveURL(bundleUrl);
    
    // the file is stuck in the plugin jar, so we have to copy it 
    // out to a temporary directory
    if (fileUrl.getProtocol().equals("jar")) {
      OutputStream os = null;
      InputStream is = null;
      
      try {
        String dir = CorePlugin.getDefault().getStateLocation().toOSString(); 
        File tempFile = FileUtil.createTempFile(filename, dir);
        os = new FileOutputStream(tempFile);
        is = fileUrl.openStream();
        FileUtil.copy(is, os);
        
        return tempFile;
      } 
      catch (IOException e) {
        throwCoreException("error extracting file from bundle: " + filename, 
                           e);
      } 
      finally {
        try {
          if (is != null)
            is.close();
          
          if (os != null)
            os.close();  
        } 
        catch (IOException e) {
          throwCoreException("error closing file from bundle: " + filename, e);
        }
      }
    } 
   
    return FileUtil.resolveFile(fileUrl);
  }
  
  /**
   * Iterate through the arguments for the publisher (this class) and find
   * the value, then do variable resolution on that string value.
   *  
   * @return the resolved value of the variable
   */
  @SuppressWarnings("unchecked")
  public static String getPublisherData(ServerRuntime typeDef,
                                        String publisherId,
                                        String key) 
  {
    Publisher publisher = typeDef.getPublisher(publisherId);
    
    if (publisher == null)
      return null;
    
    Iterator<PublisherData> iterator = publisher.getPublisherdata().iterator();
    
    while (iterator.hasNext()) {
      PublisherData data = iterator.next();
      
      if (key.equals(data.getDataname())) {
        return typeDef.getResolver().resolveProperties(data.getDatavalue());
      }
    }
    
    return null;
  }
  
  public static void throwCoreException(String message)
    throws CoreException
  {
    throwCoreException(message, null);
  }
  
  public static void throwCoreException(String message, Exception e)
    throws CoreException
  {
    IStatus s = new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID, 0, 
                           message, e);
    CorePlugin.getDefault().getLog().log(s);

    throw new CoreException(s);
  }
}
