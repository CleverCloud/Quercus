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

package com.caucho.server.snmp;

import java.util.ArrayList;
import java.util.HashMap;

import javax.management.ObjectName;
import com.caucho.jmx.Jmx;
import com.caucho.management.server.AbstractManagedObject;

public class SnmpAdmin extends AbstractManagedObject
{
  private String _host = "";
  private int _port;
  
  private String _sysContact = "";
  private String _sysLocation = "";
  
  private static int CAUCHO_PRIVATE_ENTERPRISE_NUMBER = 0;
  
  private HashMap<String, Oid> _mibMap
    = new HashMap<String, Oid>();
  
  public SnmpAdmin(String host, int port)
  {
    _host = host;
    _port = port;
    
    init();
    
    registerSelf();
  }
  
  private void init()
  {
    String mbean = getObjectName().toString();
    
    /*
    addAttribute(new Oid(mbean, "sysDescr", "1.3.6.1.2.1.1.1"));
    addAttribute(new Oid(mbean, "sysObjectId", "1.3.6.1.2.1.1.2", "OBJECT_IDENTIFIER"));
    addAttribute(new Oid(mbean, "sysUpTime", "1.3.6.1.2.1.1.3", "TIME_TICKS"));
    addAttribute(new Oid(mbean, "sysContact", "1.3.6.1.2.1.1.4"));
    addAttribute(new Oid(mbean, "sysName", "1.3.6.1.2.1.1.5"));
    addAttribute(new Oid(mbean, "sysLocation", "1.3.6.1.2.1.1.6"));
    addAttribute(new Oid(mbean, "sysServices", "1.3.6.1.2.1.1.7"));
    */
  }
  
  public void addAttribute(Oid attr)
  {
    _mibMap.put(attr.getName(), attr);
  }
  
  public Oid getAttribute(String s)
  {
    return _mibMap.get(s);
  }
  
  @Override
  public String getName()
  {
    return _host + ":" + _port;
  }

  public String getSysDescr()
  {
    try {
      ObjectName name = new ObjectName("resin:type=Resin");
    
      return Jmx.getMBeanServer().getAttribute(name, "Version").toString();
    }
    catch (Exception e) {
      return "";
    }
  }
  
  public String sysObjectID()
  {
    return "1.3.6.1.4.1." + CAUCHO_PRIVATE_ENTERPRISE_NUMBER;
  }
  
  public long getSysUpTime()
  {
    try {
      ObjectName name = new ObjectName("java.lang:type=Runtime");
    
      Object obj = Jmx.getMBeanServer().getAttribute(name, "Uptime");
    
      if (obj instanceof Number)
        return ((Number) obj).longValue();
      else
        return 0;
    }
    catch (Exception e) {
      return 0;
    }
  }
  
  public String getSysContact()
  {
    return _sysContact;
  }
  
  public void setSysContact(String sysContact)
  {
    _sysContact = sysContact;
  }
  
  public String getSysName()
  {
    try {
      ObjectName name = new ObjectName("resin:type=Host,name=default");
        
      Object obj = Jmx.getMBeanServer().getAttribute(name, "URL");
      
      return obj.toString();
    }
    catch (Exception e) {
      return "";
    }
  }
  
  public String getSysLocation()
  {
    return _sysLocation;
  }
  
  public void setSysLocation(String sysLocation)
  {
    _sysLocation = sysLocation;
  }
  
  public int getSysServices()
  {
    // this means that Resin is on the application layer of the OSI model
    return 0x02 << (7 - 1);
  }
  
  static class Mbean
  {
    private String _name;
    private String _prefix = "";
    
    private ArrayList<Oid> _attrList
      = new ArrayList<Oid>();
    
    public void setName(String name)
    {
      _name = name;
    }
    
    public String getName()
    {
      return _name;
    }
    
    public void setPrefix(String prefix)
    {
      _prefix = prefix;
    }
    
    public String getPrefix()
    {
      return _prefix;
    }
    
    public void addAttribute(Oid attr)
    {
      _attrList.add(attr);
    }
    
    public boolean equals(Object obj)
    {
      return obj instanceof Mbean
             && ((Mbean) obj).getPrefix().equals(_prefix);
    }
  }
}
