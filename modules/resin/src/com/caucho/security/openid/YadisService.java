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
public class YadisService
{
  private ArrayList<String> _typeList = new ArrayList<String>();
  private int _priority;
  private ArrayList<String> _uriList = new ArrayList<String>();

  public void setPriority(int priority)
  {
    _priority = priority;
  }

  public int getPriority()
  {
    return _priority;
  }
  
  public void addType(String type)
  {
    _typeList.add(type);
  }

  public String getType()
  {
    if (_typeList.size() > 0)
      return _typeList.get(0);
    else
      return null;
  }

  public ArrayList<String> getTypeList()
  {
    return _typeList;
  }

  public void addUri(String uri)
  {
    _uriList.add(uri);
  }

  public String getUri()
  {
    if (_uriList.size() > 0)
      return _uriList.get(0);
    else
      return null;
  }
  
  public void addBuilderProgram(ConfigProgram program)
  {
  }

  void print(PrintWriter out)
    throws IOException
  {
    out.println();
    out.print("<Service");
    
    if (_priority > 0) {
      out.print(" priority='");
      out.print(_priority);
      out.print("'");
    }
      
    out.println(">");


    for (String type : _typeList) {
      out.print("  <Type>");
      out.print(type);
      out.println("</Type>");
    }

    for (String uri : _uriList) {
      out.print("  <URI>");
      out.print(uri);
      out.println("</URI>");
    }
    
    out.println("</Service>");
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[type=" + getType() + "," + _uriList + "]";
  }
}
