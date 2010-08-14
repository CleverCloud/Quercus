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

package com.caucho.security.openid;

import com.caucho.config.program.ConfigProgram;

import java.io.*;
import java.util.*;

/**
 * Represents a single Yadis service, e.g. an openid
 */
public class YadisXrd
{
  private String _id;

  private String _location;
  
  private ArrayList<YadisService> _serviceList
    = new ArrayList<YadisService>();

  public void setId(String id)
  {
    _id = id;
  }

  public String getId()
  {
    return _id;
  }

  public void setLocation(String loc)
  {
    _location = loc;
  }

  public String getLocation()
  {
    return _location;
  }

  public YadisXrd createXrd()
  {
    return this;
  }
  
  public void addService(YadisService service)
  {
    _serviceList.add(service);
  }

  public ArrayList<YadisService> getServiceList()
  {
    return _serviceList;
  }
  
  public String findService(String type)
  {
    for (YadisService service : getServiceList()) {
      if (service.getTypeList().contains(type))
        return service.getUri();
    }

    return null;
  }
  

  public void addBuilderProgram(ConfigProgram program)
  {
  }

  void print(PrintWriter out)
    throws IOException
  {
    out.print("<xrds:XRDS");
    out.print(" xmlns:xrds='xri://$xrds'");
    out.println(" xmlns='xri://$xrd*($v*2.0)'>");

    out.println("<XRD>");

    for (YadisService service : _serviceList) {
      service.print(out);
    }
    
    out.println("</XRD>");

    out.println("</xrds:XRDS>");
  }
}
