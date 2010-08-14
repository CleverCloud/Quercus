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

package com.caucho.quercus.profile;

import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.io.OutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

/**
 * Report of profile entries
 */
public class ProfileReport
{
  private long _id;
  
  private String _url;
  private long _timestamp;

  private ArrayList<ProfileItem> _itemList = new ArrayList<ProfileItem>();

  private HashMap<String,ProfileMethod> _methodMap
    = new HashMap<String,ProfileMethod>();

  private long _totalMicros;

  public ProfileReport(long id, String url, long timestamp)
  {
    _id = id;
    _url = url;
    _timestamp = timestamp;
  }

  /**
   * Returns the report id
   */
  public long getId()
  {
    return _id;
  }

  /**
   * Returns the url
   */
  public String getUrl()
  {
    return _url;
  }

  /**
   * Returns the time
   */
  public long getTimestamp()
  {
    return _timestamp;
  }

  /**
   * Returns the total time in microseconds
   */
  public long getTotalMicros()
  {
    return _totalMicros;
  }

  /**
   * Returns the list of profile items.
   */
  public ArrayList<ProfileItem> getItemList()
  {
    return _itemList;
  }

  /**
   * Returns the list of methods, sorted by self.
   */
  public ArrayList<ProfileMethod> getMethods()
  {
    ArrayList<ProfileMethod> methodList
      = new ArrayList<ProfileMethod>(_methodMap.values());

    return methodList;
  }

  /**
   * Returns the list of methods, sorted by self.
   */
  public ArrayList<ProfileMethod> getMethodsBySelfMicros()
  {
    ArrayList<ProfileMethod> methodList
      = new ArrayList<ProfileMethod>(_methodMap.values());

    Collections.sort(methodList, new SelfMicrosComparator());

    return methodList;
  }

  /**
   * Adds a profile item.
   */
  public void addItem(String name, String parent, long count, long micros)
  {
    ProfileItem item = new ProfileItem(name, parent, count, micros);
    _itemList.add(item);

    ProfileMethod method = getMethod(name);
    method.addParent(item);

    if ("__top__".equals(name)) {
      _totalMicros = item.getMicros();
    }
    else {
      ProfileMethod parentMethod = getMethod(parent);
      parentMethod.addChild(item);
    }
  }

  /**
   * Returns the method by its id.
   */
  public ProfileMethod findMethodByIndex(int id)
  {
    for (ProfileMethod method : _methodMap.values()) {
      if (id == method.getId())
        return method;
    }

    return null;
  }

  /**
   * Returns the method by its name.
   */
  public ProfileMethod findMethodByName(String name)
  {
    return _methodMap.get(name);
  }

  /**
   * Returns the ProfileMethod for the given method name
   */
  protected ProfileMethod getMethod(String name)
  {
    ProfileMethod method = _methodMap.get(name);

    if (method == null) {
      method = new ProfileMethod(_methodMap.size(), name);
      _methodMap.put(name, method);
    }

    return method;
  }

  /**
   * Printable flat report
   */
  public void printHotSpotReport(OutputStream os)
    throws IOException
  {
    WriteStream out = Vfs.openWrite(os);
    
    ArrayList<ProfileMethod> methodList
      = new ArrayList<ProfileMethod>(_methodMap.values());

    Collections.sort(methodList, new SelfMicrosComparator());

    double totalMicros = 0;
    int maxNameLength = 0;

    for (ProfileMethod method : methodList) {
      int len = method.getName().length();

      if (maxNameLength < len)
        maxNameLength = len;
      
      totalMicros += method.getSelfMicros();
    }

    out.println();
    out.println("Hot Spot Profile: " + _url + " at " + new Date(_timestamp));
    out.println();
    out.println(" self(us)  total(us)  count   %time     %sum   name");
    out.println("----------------------------------------------------");

    double sumMicros = 0;

    for (ProfileMethod method : methodList) {
      String name = method.getName();
      long selfMicros = method.getSelfMicros();
      sumMicros += selfMicros;

      out.print(String.format("%7dus", selfMicros));
      out.print(String.format(" %8dus", method.getTotalMicros()));
      out.print(String.format(" %6d", method.getCount()));
      out.print(String.format(" %6.2f%%", 100.0 * selfMicros / totalMicros));
      out.print(String.format("  %6.2f%%", 100.0 * sumMicros / totalMicros));
      out.print("   " + name);

      out.println();
    }

    out.println();
    out.close();
  }

  /**
   * Printable hierarchy report
   */
  public void printHierarchyReport(OutputStream os)
    throws IOException
  {
    WriteStream out = Vfs.openWrite(os);
    
    ArrayList<ProfileMethod> methodList
      = new ArrayList<ProfileMethod>(_methodMap.values());

    Collections.sort(methodList, new TotalMicrosComparator());

    double totalMicros = methodList.get(0).getTotalMicros();
    int maxNameLength = 0;

    out.println();
    out.println("Hierarchy: " + _url + " at " + new Date(_timestamp));
    out.println();
    out.println(" total(us)  self(us)  count   %time     %sum   name");
    out.println("----------------------------------------------------");

    double sumMicros = 0;

    for (ProfileMethod method : methodList) {
      String name = method.getName();
      long ownTotalMicros = method.getTotalMicros();
      long selfMicros = method.getSelfMicros();
      sumMicros += selfMicros;

      out.println();

      ArrayList<ProfileItem> parentList
        = new ArrayList<ProfileItem>(method.getParentItems());

      Collections.sort(parentList, new ItemMicrosComparator());

      for (ProfileItem item : parentList) {
        out.print("        ");
        out.print(String.format(" %7dus", item.getMicros()));
        out.print(String.format(" %6d", item.getCount()));

        out.print(String.format("     %-19s", item.getParent()));
        out.print(String.format("%6.2f%%",
                                100.0 * item.getMicros() / ownTotalMicros));
        out.println();
      }

      out.print(String.format(" %6.2f%%",
                              100.0 * ownTotalMicros / totalMicros));
      out.print(String.format(" %7dus", method.getTotalMicros()));
      out.print(String.format(" %6d", method.getCount()));
      out.print(String.format("  %-22s", name));
      out.print(String.format("%6.2f%%",
                              100.0 * selfMicros / ownTotalMicros));
      out.print(String.format(" %7dus", method.getSelfMicros()));
      out.println();

      ArrayList<ProfileItem> childList
        = new ArrayList<ProfileItem>(method.getChildItems());

      Collections.sort(childList, new ItemMicrosComparator());
      
      for (ProfileItem item : childList) {
        out.print("        ");
        out.print(String.format(" %7dus", item.getMicros()));
        out.print(String.format(" %6d", item.getCount()));

        out.print(String.format("     %-19s", item.getName()));
        out.print(String.format("%6.2f%%",
                                100.0 * item.getMicros() / ownTotalMicros));
        out.println();
      }
    }

    out.println();

    out.close();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  static class SelfMicrosComparator implements Comparator<ProfileMethod> {
    public int compare(ProfileMethod a, ProfileMethod b)
    {
      long delta = b.getSelfMicros() - a.getSelfMicros();

      if (delta == 0)
        return 0;
      else if (delta < 0)
        return -1;
      else
        return 1;
    }
  }

  static class TotalMicrosComparator implements Comparator<ProfileMethod> {
    public int compare(ProfileMethod a, ProfileMethod b)
    {
      long delta = b.getTotalMicros() - a.getTotalMicros();

      if (delta == 0)
        return 0;
      else if (delta < 0)
        return -1;
      else
        return 1;
    }
  }

  static class ItemMicrosComparator implements Comparator<ProfileItem> {
    public int compare(ProfileItem a, ProfileItem b)
    {
      long delta = b.getMicros() - a.getMicros();

      if (delta == 0)
        return 0;
      else if (delta < 0)
        return -1;
      else
        return 1;
    }
  }
}

