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

package com.caucho.server.snmp.types;

/*
 * Represents a complete SNMP message.
 */
public class SnmpMessageValue extends SnmpValue
{
  IntegerValue _version = IntegerValue.ZERO;
  OctetStringValue _communityString = OctetStringValue.PUBLIC;
  
  PduValue _pdu;

  public SnmpMessageValue(int version,
                          String community,
                          PduValue pdu)
  {
    this(new IntegerValue(version), new OctetStringValue(community), pdu);
  }
  
  public SnmpMessageValue(IntegerValue version,
                          OctetStringValue community,
                          PduValue pdu)
  {
    _version = version;
    _communityString = community;
    _pdu = pdu;
  }
  
  @Override
  public int getType()
  {
    return SnmpValue.SEQUENCE;
  }
  
  @Override
  public void toAsn1(StringBuilder sb)
  {
    StringBuilder innerSb = new StringBuilder();
    
    _version.toAsn1(innerSb);
    _communityString.toAsn1(innerSb);
    _pdu.toAsn1(innerSb);
    
    header(sb, innerSb.length());
    
    sb.append(innerSb.toString());
  }
  
  public IntegerValue getVersion()
  {
    return _version;
  }
  
  public void setVersion(int version)
  {
    _version = new IntegerValue(version);
  }
  
  public void setVersion(IntegerValue version)
  {
    _version = version;
  }
  
  public OctetStringValue getCommunityString()
  {
    return _communityString;
  }
  
  public void setCommunityString(String communityString)
  {
    _communityString = new OctetStringValue(communityString);
  }
  
  public void setCommunityString(OctetStringValue communityString)
  {
    _communityString = communityString;
  }
  
  public PduValue getPdu()
  {
    return _pdu;
  }
  
  public void setPdu(PduValue pdu)
  {
    _pdu = pdu;
  }
  
  public void setError(int error)
  {
    _pdu.setError(error);
  }
  
  public void setError(IntegerValue error)
  {
    _pdu.setError(error);
  }
  
  public IntegerValue getError()
  {
    return _pdu.getError();
  }
  
  public void setErrorIndex(int errorIndex)
  {
    _pdu.setErrorIndex(errorIndex);
  }
  
  public void setErrorIndex(IntegerValue errorIndex)
  {
    _pdu.setErrorIndex(errorIndex);
  }
  
  public IntegerValue getErrorIndex()
  {
    return _pdu.getErrorIndex();
  }
  
  public void setRequestId(int id)
  {
    _pdu.setRequestId(id);
  }
  
  public void setRequestId(IntegerValue id)
  {
    _pdu.setRequestId(id);
  }
  
  public IntegerValue getRequestId()
  {
    return _pdu.getRequestId();
  }
  
  public void addVarBind(VarBindValue obj)
  {
    _pdu.addVarBind(obj);
  }
  
  public void addVarBindList(VarBindListValue list)
  {
    _pdu.addVarBindList(list);
  }
  
  public VarBindListValue getVarBindList()
  {
    return _pdu.getVarBindList();
  }
  
  public String toString()
  {
    return "SnmpMessage[" + _version + ","
                          + _communityString + ","
                          + _pdu + "]";
  }
}
