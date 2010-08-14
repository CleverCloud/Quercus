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
import java.io.OutputStream;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import com.caucho.jmx.Jmx;
import com.caucho.network.listen.AbstractProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.server.snmp.types.GetResponsePduValue;
import com.caucho.server.snmp.types.IntegerValue;
import com.caucho.server.snmp.types.NullValue;
import com.caucho.server.snmp.types.ObjectIdentifierValue;
import com.caucho.server.snmp.types.OctetStringValue;
import com.caucho.server.snmp.types.SnmpMessageValue;
import com.caucho.server.snmp.types.SnmpValue;
import com.caucho.server.snmp.types.VarBindListValue;
import com.caucho.server.snmp.types.VarBindValue;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;

/*
 * Responds to SNMP requests.
 */
public class SnmpRequest extends AbstractProtocolConnection
{
  private static final Logger log
    = Logger.getLogger(SnmpRequest.class.getName());
  
  private static final L10N L = new L10N(SnmpRequest.class);
  
  public static final int NO_ERROR = 0;
  public static final int TOO_BIG = 1;
  public static final int NO_SUCH_NAME = 2;
  public static final int BAD_VALUE = 3;
  public static final int READ_ONLY = 4;
  public static final int GENERAL_ERROR = 5;
  
  private final SocketLink _connection;

  private IntegerValue _version = IntegerValue.ZERO;
  private final OctetStringValue _communityString;
  
  private HashMap<String, Oid> _mibMap;
  
  public SnmpRequest(SocketLink connection,
                     HashMap<String, Oid> mibMap,
                     OctetStringValue community)
  {
    _connection = connection;
    _mibMap = mibMap;
    
    _communityString = community;
  }

  /**
   * Initialize the connection.  At this point, the current thread is the
   * connection thread.
   */
  public void init()
  {
  }

  /**
   * Return true if the connection should wait for a read before
   * handling the request.
   */
  public boolean isWaitForRead()
  {
    return true;
  }

  public void startConnection()
  {
  }
  
  /**
   * Handles a new connection.  The controlling TcpServer may call
   * handleConnection again after the connection completes, so 
   * the implementation must initialize any variables for each connection.
   */
  public boolean handleRequest() 
    throws IOException
  {
    ReadStream in = _connection.getReadStream();
    
    SnmpParser parser = new SnmpParser(in);
    SnmpMessageValue req = parser.readMessage();
    
    checkVersion(req);
    authenticate(req);
    
    SnmpMessageValue response = composeResponse(req);

    sendResponse(response);
    
    return true;
  }
  
  final protected void checkVersion(SnmpMessageValue req)
  {
    if (! _version.equals(req.getVersion())) {
      log.fine(L.l("expected version {0} != {1}",
                   _version,
                   req.getVersion()));
      
      throw new SnmpRuntimeException(L.l("expected version {0} != {1}",
                                         _version,
                                         req.getVersion()));
    }
  }
  
  final protected void authenticate(SnmpMessageValue req)
  {
    if (! _communityString.equals(req.getCommunityString())) {
      log.fine(L.l("non-matching community string"));
      
      throw new SnmpRuntimeException(L.l("non-matching community string"));
    }
  }
  
  final protected void sendResponse(SnmpValue response)
    throws IOException
  {
    OutputStream out = _connection.getWriteStream();
    
    StringBuilder sb = new StringBuilder();
    response.toAsn1(sb);

    for (int i = 0; i < sb.length(); i++) {
      out.write((byte) sb.charAt(i));
    }
    out.flush();
  }
  
  final protected SnmpMessageValue composeResponse(SnmpMessageValue req)
  {
    IntegerValue error = IntegerValue.ZERO;
    IntegerValue errorIndex = IntegerValue.ZERO;
    VarBindListValue varBindList = new VarBindListValue();
    
    int i = -1;
    try {
      switch (req.getPdu().getType()) {
        case SnmpValue.GET_REQUEST_PDU:
        {
          ObjectIdentifierValue []oids = req.getVarBindList().getNames();
          
          for (i = 0; i < oids.length; i++) {
            SnmpValue attr = getMBean(oids[i]);

            VarBindValue varBind;
            
            if (attr != null)
              varBind = new VarBindValue(oids[i], attr);
            else {
              varBind = new VarBindValue(oids[i], NullValue.NULL);
              
              if (error.getLong() == 0) {
                error = new IntegerValue(NO_SUCH_NAME);
                errorIndex = new IntegerValue(i + 1);
              }
            }
            
            varBindList.add(varBind);
          }

          break;
        }
        case SnmpValue.GET_NEXT_REQUEST_PDU:
        {
          break;
        }
        case SnmpValue.SET_REQUEST_PDU:
        {
          break;
        }
        default:
          log.fine(L.l("invalid pdu type {0}", req.getType()));
          
          throw new SnmpRuntimeException(L.l("invalid pdu type {0}",
                                             req.getType()));
      }
    }
    catch (Exception e) {
      if (error.getLong() == 0) {
        error = new IntegerValue(GENERAL_ERROR);
        errorIndex = new IntegerValue(i + 1);
      }
      
      if (req.getVarBindList().size() > i) {
        VarBindValue varBind
          = new VarBindValue(req.getVarBindList().get(i).getName(),
                             new OctetStringValue(e.getMessage()));
        
        varBindList.add(varBind);
      }
    }

    GetResponsePduValue pdu = new GetResponsePduValue(req.getRequestId(),
                                                      error,
                                                      errorIndex,
                                                      varBindList);

    return new SnmpMessageValue(req.getVersion(),
                                req.getCommunityString(),
                                pdu);
    
  }
  
  //@In MBeanServer _jmx;
  protected SnmpValue getMBean(ObjectIdentifierValue objectIdentifier)
  {
    MBeanServer mbeanServer = getMBeanServer();

    Oid oid = _mibMap.get(objectIdentifier.getString());

    if (oid == null)
      return null;

    try {
      ObjectName mbean = new ObjectName(oid.getMbean());

      Object attr = mbeanServer.getAttribute(mbean, oid.getAttribute());
      
      //XXX: complex attributes that are not either Integer or String
      //     i.e. from java.lang:type=Memory mbean
      
      return SnmpValue.create(attr, oid.getType());
    }
    catch (MalformedObjectNameException e) {
      log.fine(e.getMessage());
      
      throw new SnmpRuntimeException(e);
    }
    catch (AttributeNotFoundException e) {
      log.fine(e.getMessage());
      
      return null;
    }
    catch (InstanceNotFoundException e) {
      log.fine(e.getMessage());
      
      return null;
    }
    catch (MBeanException e) {
      log.fine(e.getMessage());
      
      throw new SnmpRuntimeException(e);
    }
    catch (ReflectionException e) {
      log.fine(e.getMessage());
      
      throw new SnmpRuntimeException(e);
    }
    catch (Exception e) {
      log.fine(e.getMessage());
      
      return null;
    }
  }
  
  //@In MBeanServer _jmx;
  protected MBeanServer getMBeanServer()
  {
    return Jmx.getMBeanServer();
  }

  /**
   * Resumes processing after a wait.
   */
  public boolean handleResume() 
    throws IOException
  {
    return false;
  }

  /**
   * Handles a close event when the connection is closed.
   */
  public void onCloseConnection()
  {
  }

  /*
   * Sets the SNMP version.
   */
  public void setVersion(int version)
  {
    _version = new IntegerValue(version);
  }
  
  public HashMap<String, Oid> getMib()
  {
    return _mibMap;
  }
}
