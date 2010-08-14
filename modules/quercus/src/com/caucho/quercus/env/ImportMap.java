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

package com.caucho.quercus.env;

import java.util.ArrayList;
import java.util.HashMap;

public class ImportMap
{
  HashMap<String, String> _qualifiedMap;
  ArrayList<String> _wildcardList;
 
  ArrayList<String> _wildcardPhpList;
  
  public ImportMap()
  {
  }
  
  public void addWildcardImport(String name)
  {
    if (_wildcardList == null) {
      _wildcardList = new ArrayList<String>();
      _wildcardPhpList = new ArrayList<String>(); 
    }
    
    _wildcardList.add(name);
    _wildcardPhpList.add(name.replaceAll("\\.", "/"));
  }
  
  public String putQualified(String name)
  {
    if (_qualifiedMap == null) {
      _qualifiedMap = new HashMap<String, String>();
    }

    int i = name.lastIndexOf('.');

    String shortName;
    if (i > 0)
      shortName = name.substring(i + 1);
    else
      shortName = name;
    
    putQualified(shortName, name);

    return shortName;
  }
  
  public void putQualified(String shortName, String name)
  {
    if (_qualifiedMap == null)
      _qualifiedMap = new HashMap<String, String>();

    _qualifiedMap.put(shortName, name);
  }
  
  public String getQualified(String name)
  {
    if (_qualifiedMap == null)
      return null;
    else
      return _qualifiedMap.get(name);
  }
  
  public String getQualifiedPhp(String name)
  {
    if (_qualifiedMap == null)
      return null;
    
    String fullName = _qualifiedMap.get(name);
    
    if (fullName != null)
      fullName = fullName.replaceAll("\\.", "/") + ".php";

    return fullName;
  }
  
  public ArrayList<String> getWildcardList()
  {
    if (_wildcardList == null)
      _wildcardList = new ArrayList<String>();
    
    return _wildcardList;
  }
  
  public ArrayList<String> getWildcardPhpList()
  {
    if (_wildcardPhpList == null)
      _wildcardPhpList = new ArrayList<String>();
    
    return _wildcardPhpList;
  }
  
  public ImportMap copy()
  {
    ImportMap copy = new ImportMap();
    
    if (_qualifiedMap != null)
      copy._qualifiedMap = new HashMap<String,String>(_qualifiedMap);
    
    if (_wildcardList != null)
      copy._wildcardList = new ArrayList<String>(_wildcardList);

    if (_wildcardPhpList != null)
      copy._wildcardPhpList = new ArrayList<String>(_wildcardPhpList);
    
    return copy;
  }
  
  public void clear()
  {
    if (_qualifiedMap != null)
      _qualifiedMap.clear();
    
    if (_wildcardList != null)
      _wildcardList.clear();

    if (_wildcardPhpList != null)
      _wildcardPhpList.clear();
  }
}
