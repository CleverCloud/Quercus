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

package com.caucho.util;

import java.io.*;
import java.net.*;
import java.util.*;

import com.caucho.vfs.*;

/**
 * Returns the META-INF/services defined for a given name.
 */
public class Services
{
  public static ArrayList<String> getServices(String name)
    throws IOException
  {
    ArrayList<String> services = new ArrayList<String>();
    
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    Enumeration e = loader.getResources("META-INF/services/" + name);
    while (e.hasMoreElements()) {
      URL url = (URL) e.nextElement();

      ReadStream is = Vfs.lookup(url.toString()).openRead();

      try {
        String line;

        while ((line = is.readLine()) != null) {
          int p = line.indexOf('#');
          if (p > 0)
            line = line.substring(0, p);

          line = line.trim();

          if (line.length() > 0) {
            services.add(line);
          }
        }
      } finally {
        is.close();
      }
    }

    return services;
  }
}
