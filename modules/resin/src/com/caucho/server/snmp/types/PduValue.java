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

import java.util.Iterator;

import com.caucho.server.snmp.SnmpRuntimeException;

/*
 * Parent class for all SNMP PDU types.
 */
abstract public class PduValue extends SnmpValue
{
  IntegerValue _requestId;
  IntegerValue _error;
  IntegerValue _errorIndex;
  VarBindListValue _varBindList;

  public PduValue()
  {
    _requestId = IntegerValue.ZERO;
    _error = IntegerValue.ZERO;
    _errorIndex = IntegerValue.ZERO;
    _varBindList = new VarBindListValue();
  }
  
  public PduValue(int requestId,
                  int error,
                  int errorIndex)
  {
    _requestId = new IntegerValue(requestId);
    _error = new IntegerValue(error);
    _errorIndex = new IntegerValue(errorIndex);
    _varBindList = new VarBindListValue();
  }
  
  public PduValue(IntegerValue requestId,
                  IntegerValue error,
                  IntegerValue errorIndex,
                  VarBindListValue varBindList)
  {
    _requestId = requestId;
    _error = error;
    _errorIndex = errorIndex;
    _varBindList = varBindList;
  }
  
  @Override
  public void toAsn1(StringBuilder sb)
  {
    StringBuilder innerSb = new StringBuilder();
    
    _requestId.toAsn1(innerSb);
    _error.toAsn1(innerSb);
    _errorIndex.toAsn1(innerSb);
    _varBindList.toAsn1(innerSb);
    
    header(sb, innerSb.length());
    
    sb.append(innerSb.toString());
  }
  
  public void setError(int error)
  {
    _error = new IntegerValue(error);
  }
  
  public void setError(IntegerValue error)
  {
    _error = error;
  }
  
  public IntegerValue getError()
  {
    return _error;
  }
  
  public void setErrorIndex(int errorIndex)
  {
    _errorIndex = new IntegerValue(errorIndex);
  }
  
  public void setErrorIndex(IntegerValue errorIndex)
  {
    _errorIndex = errorIndex;
  }
  
  public IntegerValue getErrorIndex()
  {
    return _errorIndex;
  }
  
  public void setRequestId(int id)
  {
    _requestId = new IntegerValue(id);
  }
  
  public void setRequestId(IntegerValue id)
  {
    _requestId = id;
  }
  
  public IntegerValue getRequestId()
  {
    return _requestId;
  }
  
  public void addVarBind(VarBindValue obj)
  {
    _varBindList.add(obj);
  }
  
  public void addVarBindList(VarBindListValue list)
  {
    Iterator<VarBindValue> iter = list.iterator();
    
    while (iter.hasNext()) {
      addVarBind(iter.next());
    }
  }
  
  public VarBindListValue getVarBindList()
  {
    return _varBindList;
  }
  
  public static PduValue create(int type)
  {
    switch (type) {
      case SnmpValue.GET_REQUEST_PDU:
        return new GetRequestPduValue();
      case SnmpValue.GET_NEXT_REQUEST_PDU:
        return new GetNextRequestPduValue();
      case SnmpValue.SET_REQUEST_PDU:
        return new SetRequestPduValue(); 
      case SnmpValue.GET_RESPONSE_PDU:
        return new GetResponsePduValue();
      default:
        throw new SnmpRuntimeException("invalid type: " + typeName(type));
    }
  }
  
  public static PduValue create(int type,
                                IntegerValue requestId,
                                IntegerValue error,
                                IntegerValue errorIndex,
                                VarBindListValue varBindList)
  {
    switch (type) {
      case SnmpValue.GET_REQUEST_PDU:
        return new GetRequestPduValue(requestId, error, errorIndex, varBindList);
      case SnmpValue.GET_NEXT_REQUEST_PDU:
        return new GetNextRequestPduValue(requestId, error, errorIndex, varBindList);
      case SnmpValue.SET_REQUEST_PDU:
        return new SetRequestPduValue(requestId, error, errorIndex, varBindList);
      case SnmpValue.GET_RESPONSE_PDU:
        return new GetResponsePduValue(requestId, error, errorIndex, varBindList);
      default:
        throw new SnmpRuntimeException("invalid type: " + typeName(type));
    }
  }
}
