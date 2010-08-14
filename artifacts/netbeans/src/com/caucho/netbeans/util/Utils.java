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
 * @author Sam
 */


package com.caucho.netbeans.util;

import org.openide.filesystems.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;

public final class Utils
{

  /**
   * Creates a new instance of Utils
   */
  private Utils()
  {
  }

  /**
   * Return URL representation of the specified file.
   */
  public static URL fileToUrl(File file)
    throws MalformedURLException
  {
    URL url = file.toURI().toURL();
    if (FileUtil.isArchiveFile(url)) {
      url = FileUtil.getArchiveRoot(url);
    }
    return url;
  }

  /**
   * Return string representation of the specified URL.
   */
  public static String urlToString(URL url)
  {
    if ("jar".equals(url.getProtocol())) { // NOI18N
      URL fileURL = FileUtil.getArchiveFile(url);
      if (FileUtil.getArchiveRoot(fileURL).equals(url)) {
        // really the root
        url = fileURL;
      }
      else {
        // some subdir, just show it as is
        return url.toExternalForm();
      }
    }
    if ("file".equals(url.getProtocol())) { // NOI18N
      File f = new File(URI.create(url.toExternalForm()));
      return f.getAbsolutePath();
    }
    else {
      return url.toExternalForm();
    }
  }

}
