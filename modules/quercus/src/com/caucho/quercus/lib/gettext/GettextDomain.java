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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.gettext;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.lib.gettext.expr.PluralExpr;
import com.caucho.vfs.Path;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class GettextDomain
{
  private String _name; 
  private String _charset;
  private Path _path;
  
  public GettextDomain(Env env, String name)
  {
    _name = name;
    _path = env.getPwd();

    _charset = env.getRuntimeEncoding();
  }
  
  public String getName()
  {
    return _name;
  }
  
  public void setName(String name)
  {
    _name = name;
  }
  
  public String getCharset()
  {
    return _charset;
  }
  
  public void setCharset(String charset)
  {
    _charset = charset;
  }
  
  public Path getPath()
  {
    return _path;
  }
  
  public boolean setPath(Env env, StringValue directory)
  {
    Path path = env.lookupPwd(directory);
    
    if (path != null)
      _path = path;
    
    return path != null;
  }
  
  public String toString()
  {
    return _name;
  }
}
