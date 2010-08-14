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

import java.io.IOException;
import java.io.InputStream;

import com.caucho.server.snmp.types.*;
import com.caucho.util.L10N;

public class SnmpParser
{
  private static final L10N L = new L10N(SnmpParser.class);

  private InputStream _is;
  private int _bytesRead = 0;
  
  private boolean _isCheckIdentifier = true;
  
  public SnmpParser(InputStream is) 
  {
    _is = is;
  }
  
  public SnmpParser(String s)
  {
    _is = new StringInputStream(s);
  }
  
  public SnmpMessageValue readMessage()
    throws IOException
  {
    checkIdentifier(SnmpValue.SEQUENCE);

    int expectedLen = readLength();
    int bytesRead = getBytesRead();
    
    IntegerValue version = readInteger();
    OctetStringValue communityString = readOctetString();

    int id = readByte();
    _isCheckIdentifier = false;
    
    PduValue pdu;
    switch(id) {
      case SnmpValue.GET_REQUEST_PDU:
        pdu = readGetRequestPdu();
        break;
      case SnmpValue.GET_NEXT_REQUEST_PDU:
        pdu = readGetNextRequestPdu();
        break;
      case SnmpValue.SET_REQUEST_PDU:
        pdu = readSetRequestPdu();
        break;
      case SnmpValue.GET_RESPONSE_PDU:
        pdu = readGetResponsePdu();
        break;
      //case SnmpValue.TRAP_PDU:
        //break;
      default:
        throw new SnmpParsingException(L.l("unknown PDU type '{0}'", id));
    }
    
    checkLength(expectedLen, getBytesRead() - bytesRead);
    
    return new SnmpMessageValue(version, communityString, pdu);
  }

  public PduValue readGetRequestPdu()
    throws IOException
  {
    return readPdu(SnmpValue.GET_REQUEST_PDU);
  }
  
  public PduValue readGetNextRequestPdu()
    throws IOException
  {
    return readPdu(SnmpValue.GET_NEXT_REQUEST_PDU);
  }
  
  public PduValue readSetRequestPdu()
    throws IOException
  {
    return readPdu(SnmpValue.SET_REQUEST_PDU);
  }
  
  public PduValue readGetResponsePdu()
    throws IOException
  {
    return readPdu(SnmpValue.GET_RESPONSE_PDU);
  }
  
  private PduValue readPdu(int type)
    throws IOException
  {
    checkIdentifier(type);
    
    int expectedLen = readLength();
    int bytesRead = getBytesRead();
    
    IntegerValue requestId = readInteger();
    
    // errors are ignored for requests
    IntegerValue error = readInteger();
    IntegerValue errorIndex = readInteger();
    
    VarBindListValue varBindList = readVarBindList();
    
    checkLength(expectedLen, getBytesRead() - bytesRead);
    
    return PduValue.create(type,
                           requestId,
                           error,
                           errorIndex,
                           varBindList);
  }

  public VarBindListValue readVarBindList()
     throws IOException
  {
    checkIdentifier(SnmpValue.SEQUENCE);
    
    int expectedLen = readLength();
    int bytesRead = 0;
    
    VarBindListValue varBindList = new VarBindListValue();
    
    while (bytesRead < expectedLen) {
      bytesRead -= getBytesRead();
      
      varBindList.addVarBind(readVarBind());
      
      bytesRead += getBytesRead();
    }
    
    checkLength(expectedLen, bytesRead);
    
    return varBindList;
  }
  
  public VarBindValue readVarBind()
    throws IOException
  {
    checkIdentifier(SnmpValue.SEQUENCE);
    
    int expectedLen = readLength();
    int bytesRead = getBytesRead();
    
    VarBindValue varBind = new VarBindValue(readObjectIdentifier(),
                                            read());

    checkLength(expectedLen, getBytesRead() - bytesRead);
    
    return varBind;
  }
  
  public void skipObject()
    throws IOException
  {
    readByte();
    
    int len = readLength();
    
    for (int i = 0; i < len; i++) {
      readByte();
    }
  }
  
  public SnmpValue read()
    throws IOException
  {
    int identifier = readByte();
    
    _isCheckIdentifier = false;

    switch (identifier) {
      case SnmpValue.NULL:
        return readNull();
      case SnmpValue.INTEGER:
        return readInteger();
      case SnmpValue.OCTET_STRING:
        return readOctetString();
      case SnmpValue.OBJECT_IDENTIFIER:
        return readObjectIdentifier();
      case SnmpValue.SEQUENCE:
        return readSequence();
      case SnmpValue.IP_ADDRESS:
        return readIpAddress();
      case SnmpValue.COUNTER:
        return readCounter();
      case SnmpValue.GAUGE:
        return readGauge();
      case SnmpValue.TIME_TICKS:
        return readTimeTicks();
      case SnmpValue.OPAQUE:
        return readOpaque();
      case SnmpValue.GET_REQUEST_PDU:
        return readGetRequestPdu();
      case SnmpValue.GET_NEXT_REQUEST_PDU:
        return readGetNextRequestPdu();
      case SnmpValue.GET_RESPONSE_PDU:
        return readGetResponsePdu();
      case SnmpValue.SET_REQUEST_PDU:
        return readSetRequestPdu();
      //case SnmpValue.TRAP_PDU:
      //  break;
      default:
        throw new SnmpParsingException(L.l("unknown identifier {0}", identifier));
    }
  }
  
  
  public NullValue readNull()
    throws IOException
  {
    checkIdentifier(SnmpValue.NULL);
    int len = readLength();

    if (len != 0)
      throw new SnmpParsingException("length must be zero for NULL");
    
    return NullValue.NULL;
  }
  
  public IntegerValue readInteger()
    throws IOException
  {
    checkIdentifier(SnmpValue.INTEGER);
    int len = readLength();

    if (len < 1 || len > 4)
      throw new SnmpParsingException(L.l("integer length {0} must be 1-4"));

    int value =  readByte();
    boolean isNeg = (value & 0x80) > 0;

    for (int i = 1; i < len; i++) {
      value <<= 8;
      value |= readByte();
    }
    
    if (isNeg) {
      while (len != 4) {
        value |= 0xFF << (8 * len++);
      }
    }
    
    return new IntegerValue(value);
  }

  public OctetStringValue readOctetString()
    throws IOException
  {
    checkIdentifier(SnmpValue.OCTET_STRING);
    int expectedLen = readLength();
    
    StringBuilder sb = new StringBuilder();
    
    for (int i = 0; i < expectedLen; i++) {
      sb.append((char) readByte());
    }

    return new OctetStringValue(sb.toString());
  }
  
  public ObjectIdentifierValue readObjectIdentifier()
    throws IOException
  {
    checkIdentifier(SnmpValue.OBJECT_IDENTIFIER);
    int len = readLength();
    
    StringBuilder sb = new StringBuilder();

    int total = 0;
    while (len-- > 0) {
      int b = readByte();
      
      total <<= 7;
      total += (b & 0x7F);
      
      if ((b & 0x80) == 0) {
        break;
      }
    }
    
    // total is (40x + y)
    // x can only be 0,1,2
    // y can not be more than 39 for x = 0,1
    
    int x;
    int y;
    
    if (total < 40) {
      x = 0;
      y = total;
    }
    else if (total < 80) {
      x = 1;
      y = total - 40;
    }
    else {
      x = 2;
      y = total - 80;
    }
    
    sb.append(x);
    sb.append('.');
    sb.append(y);
    
    while (len > 0) {
      int val = 0;
      
      while (len-- > 0) {
        int b = readByte();
        
        val <<= 7;
        val += (b & 0x7F);
        
        if ((b & 0x80) == 0) {
          break;
        }
      }
      
      sb.append('.');
      sb.append(val);
    }

    return new ObjectIdentifierValue(sb.toString());
  }
  
  public IpAddressValue readIpAddress()
    throws IOException
  {
    checkIdentifier(SnmpValue.IP_ADDRESS);
    int expectedLen = readLength();
    
    StringBuilder sb = new StringBuilder();
    
    for (int i = 0; i < expectedLen; i++) {
      int b = readByte();
      
      if (b < 0x00 || b > 0xFF) {
        throw new SnmpParsingException(L.l("IP address digit {0} out of range", b));
      }
      
      if (i != 0)
        sb.append('.');

      sb.append(b);
    }

    return new IpAddressValue(sb.toString());
  }
  
  public CounterValue readCounter()
    throws IOException
  {
    checkIdentifier(SnmpValue.COUNTER);
    int expectedLen = readLength();
    
    long value = 0;
    
    for (int i = 0; i < expectedLen; i++) {
      value <<= 8;
      value |= readByte();
    }

    return new CounterValue(value);
  }
  
  public GaugeValue readGauge()
    throws IOException
  {
    checkIdentifier(SnmpValue.GAUGE);
    int expectedLen = readLength();
    
    long value = 0;
    
    for (int i = 0; i < expectedLen; i++) {
      value <<= 8;
      value |= readByte();
    }

    return new GaugeValue(value);
  }
  
  public TimeTicksValue readTimeTicks()
    throws IOException
  {
    checkIdentifier(SnmpValue.TIME_TICKS);
    int expectedLen = readLength();
    
    long value = 0;
    
    for (int i = 0; i < expectedLen; i++) {
      value <<= 8;
      value |= readByte();
    }

    return new TimeTicksValue(value);
  }
  
  public OpaqueValue readOpaque()
    throws IOException
  {
    checkIdentifier(SnmpValue.OPAQUE);
    int expectedLen = readLength();
    
    StringBuilder sb = new StringBuilder();
    
    for (int i = 0; i < expectedLen; i++) {
      sb.append((char) readByte());
    }

    return new OpaqueValue(sb.toString());
  }

  public SequenceValue<SnmpValue> readSequence()
    throws IOException
  {
    checkIdentifier(SnmpValue.SEQUENCE);
    int expectedLen = readLength();
    
    SequenceValue<SnmpValue> sequence = new SequenceValue<SnmpValue>();
    
    int bytesRead = 0;
    while (bytesRead < expectedLen) {
      bytesRead -= getBytesRead();
      
      SnmpValue item = read();
      sequence.add(item);

      bytesRead += getBytesRead();
    }

    if (bytesRead != expectedLen) {
      throw new SnmpParsingException(L.l("expected sequence length {0} != {1}",
                                          expectedLen, bytesRead));
    }

    return sequence;
  }

  public int readLength()
    throws IOException
  {
    int octlet = readByte();

    if ((octlet & 0x80) == 0)
      return octlet & 0x7F;
    
    int numOfOctlets = octlet & 0x7F;

    int len = 0;
    for (int i = 0; i < numOfOctlets; i++) {
      octlet = readByte();
      
      len <<= 8;
      len |= octlet;
    }

    return len;
  }
  
  private int readByte()
    throws IOException
  {
    _bytesRead++;
    
    int ch = _is.read();
    
    if (ch < 0)
      throw new SnmpParsingException("unexpected EOF");
    
    return ch;
  }

  private int getBytesRead()
  {
    return _bytesRead;
  }
  
  private void checkLength(int expected, int bytesRead)
    throws SnmpParsingException
  {
    if (expected != bytesRead) {
      throw new SnmpParsingException(L.l("expected length {0} != {1}", expected, bytesRead));
    }
  }
  
  private void checkIdentifier(int expected)
    throws IOException
  {
    if (_isCheckIdentifier)
      checkIdentifier(expected, readByte());
    
    _isCheckIdentifier = true;
  }
  
  private void checkIdentifier(int expected, int identifier)
    throws SnmpParsingException
  {
    if ( expected != identifier) {
      throw new SnmpParsingException(L.l("saw '{0}' but expected type '{1}'", type(identifier), type(expected)));
    }
  }
  
  private static String type(int identifier)
  {
    return SnmpValue.typeName(identifier);
  }
  
  static class StringInputStream extends InputStream
  {
    private String _s;
    private int _len;
    private int _index;
    
    public StringInputStream(String s)
    {
      _s = s;
      _len = s.length();
    }
    
    public int read()
      throws IOException
    {
      if (_index < _len)
        return _s.charAt(_index++);
      else
        return -1;
    }
  }
}
