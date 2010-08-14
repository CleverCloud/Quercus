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

import java.util.HashMap;

import com.caucho.network.listen.AbstractProtocol;
import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.server.snmp.types.OctetStringValue;
import com.caucho.server.snmp.types.SnmpValue;

/*
 * SNMP v1 protocol.
 */
public class SnmpProtocol extends AbstractProtocol
{
  //holds the mappings from SNMP oids to MBeans
  private HashMap<String, Oid> _mibMap
    = new HashMap<String, Oid>();
  
  //specially reserved for Caucho by iana.org
  private final int PRIVATE_ENTERPRISE_NUMBER = 30350;
  
  private String _community;

  public SnmpProtocol()
  {
    setProtocolName("snmp");
    
    addOid("1.3.6.1.2.1.1.1",
           "resin:type=Resin",
           "Version",
           SnmpValue.OCTET_STRING);

    addOid("1.3.6.1.2.1.1.3",
           "java.lang:type=Runtime",
           "UpTime",
           SnmpValue.TIME_TICKS);

    addOid("1.3.6.1.2.1.1.5",
           "resin:type=Host,name=default",
           "URL",
           SnmpValue.OCTET_STRING);
    
    String penPrefix = "1.3.6.1.4.1." + PRIVATE_ENTERPRISE_NUMBER + "."; 
    
    addOid(penPrefix + "1.1",
           "resin:type=Server",
           "KeepaliveCountTotal",
           SnmpValue.GAUGE);
    
    addOid(penPrefix + "1.2",
            "resin:type=Server",
            "RequestCountTotal",
            SnmpValue.GAUGE);
    
    addOid(penPrefix + "1.3",
            "resin:type=Server",
            "RuntimeMemory",
            SnmpValue.GAUGE);
    
    addOid(penPrefix + "1.4",
            "resin:type=Server",
            "RuntimeMemoryFree",
            SnmpValue.GAUGE);
    
    addOid(penPrefix + "1.5",
            "resin:type=Server",
            "ThreadActiveCount",
            SnmpValue.GAUGE);
    
    addOid(penPrefix + "1.6",
            "resin:type=Server",
            "ThreadKeepaliveCount",
            SnmpValue.GAUGE);
   
    
    addOid(penPrefix + "2.1",
           "resin:type=ThreadPool",
           "ThreadActiveCount",
           SnmpValue.GAUGE);
    
    addOid(penPrefix + "2.2",
           "resin:type=ThreadPool",
           "ThreadCount",
           SnmpValue.GAUGE);
    
    addOid(penPrefix + "2.3",
           "resin:type=ThreadPool",
           "ThreadIdleCount",
           SnmpValue.GAUGE);
    
    addOid(penPrefix + "2.4",
           "resin:type=ThreadPool",
           "ThreadIdleMax",
           SnmpValue.GAUGE);
    
    addOid(penPrefix + "2.5",
           "resin:type=ThreadPool",
           "ThreadIdleMin",
           SnmpValue.GAUGE);
    
    addOid(penPrefix + "2.6",
           "resin:type=ThreadPool",
           "ThreadMax",
           SnmpValue.GAUGE);
    
    
    addOid(penPrefix + "3.1",
           "resin:type=ProxyCache",
           "HitCountTotal",
           SnmpValue.GAUGE);
    
    addOid(penPrefix + "3.2",
           "resin:type=ProxyCache",
           "MissCountTotal",
           SnmpValue.GAUGE);
    
    
    /* These return complex mbeans
    addOid(penPrefix + "4.1",
           "java.lang:type=Memory",
           "HeapMemoryUsage",
           SnmpValue.GAUGE);
    
    addOid(penPrefix + "4.2",
           "java.lang:type=Memory",
           "NonHeapMemoryUsage",
           SnmpValue.GAUGE);
    */
  }
  
  /*
   * Adds an SNMP-MBean mapping.
   */
  public void addOid(Oid oid)
    throws Exception
  {
    _mibMap.put(oid.getName(), oid);
  }
  
  private void addOid(String name,
                      String mbean,
                      String attribute,
                      int type)
  {
    Oid oid = new Oid(name, mbean, attribute, type);
    
    _mibMap.put(name, oid);
  }
  
  public HashMap<String, Oid> getMib()
  {
    return _mibMap;
  }
  
  public String getCommunity()
  {
    if (_community == null)
      return "public";
    else
      return _community;
  }
                     
  public void setCommunity(String s)
  {
    _community = s;
  }

  public ProtocolConnection createConnection(SocketLink connection)
  {
    OctetStringValue community;
    
    if (_community == null)
      community = OctetStringValue.PUBLIC;
    else
      community = new OctetStringValue(_community);
    
    return new SnmpRequest(connection, _mibMap, community);
  }
}
