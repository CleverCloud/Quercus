/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is finishThread software; you can redistribute it and/or modify
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

package com.caucho.network.listen;

import com.caucho.inject.Module;

@Module
enum SocketLinkState {
  /**
   * The allocated, ready to accept state
   */
  INIT {
    @Override
    SocketLinkState toInit() 
    { 
      return INIT; 
    }

    @Override
    SocketLinkState toAccept() 
    { 
      return ACCEPT; 
    }
  },
  
  /**
   * Waiting in an accept() for a new connection
   */
  ACCEPT {               // accepting
    @Override
    boolean isActive() { return true; }

    @Override
    SocketLinkState toActiveWithKeepalive(TcpSocketLink conn) 
    { 
      conn.getPort().keepaliveAllocate();
      
      return REQUEST_ACTIVE_KA;
    }

    @Override
    SocketLinkState toActiveNoKeepalive(TcpSocketLink conn) 
    { 
      return REQUEST_ACTIVE_NKA;
    }
  },

  /**
   * Connection opened, waiting for the request.
   */
  REQUEST_READ {         // after accept, but before any data is read
    @Override
    boolean isActive() { return true; }

    @Override
    boolean isRequestActive() { return true; }

    /**
     * A slow initial read might go into the keepalive state.
     * 
     * XXX: qa
     */
    @Override
    SocketLinkState toKeepalive(TcpSocketLink conn)
    {
      conn.getPort().keepaliveAllocate();
      
      return REQUEST_KEEPALIVE;
    }
    
    @Override
    SocketLinkState toActiveWithKeepalive(TcpSocketLink conn) 
    { 
      conn.getPort().keepaliveAllocate();
      
      return REQUEST_ACTIVE_KA; 
    }

    @Override
    SocketLinkState toActiveNoKeepalive(TcpSocketLink conn) 
    { 
      return REQUEST_ACTIVE_NKA;
    }
  },

  /**
   * Processing a request with a keepalive slot allocated.
   */
  REQUEST_ACTIVE_KA {       // processing a request
    @Override
    boolean isActive() { return true; }

    @Override
    boolean isRequestActive() { return true; }

    @Override
    boolean isKeepaliveAllocated() { return true; }
    
    @Override
    SocketLinkState toActiveWithKeepalive(TcpSocketLink conn) 
    { 
      return REQUEST_ACTIVE_KA; 
    }

    @Override
    SocketLinkState toActiveNoKeepalive(TcpSocketLink conn) 
    { 
      conn.getPort().keepaliveFree();
      
      return REQUEST_ACTIVE_NKA;
    }

    @Override
    SocketLinkState toKillKeepalive(TcpSocketLink conn)
    {
      conn.getPort().keepaliveFree();
      
      return REQUEST_ACTIVE_NKA;
    }

    @Override
    SocketLinkState toKeepalive(TcpSocketLink conn)
    {
      return REQUEST_KEEPALIVE;
    }

    @Override
    SocketLinkState toComet()
    {
      return COMET_KA;
    }

    @Override
    SocketLinkState toDuplex(TcpSocketLink conn)
    {
      conn.getPort().keepaliveFree();
      
      return DUPLEX;
    }
    
    @Override
    SocketLinkState toClosed(TcpSocketLink conn)
    {
      conn.getPort().keepaliveFree();
      
      return CLOSED;
    }
  },

  /**
   * Request active with keepalive forbidden.
   */
  REQUEST_ACTIVE_NKA {          // processing a request, but keepalive forbidden
    @Override
    boolean isActive() { return true; }

    @Override
    SocketLinkState toComet()
    {
      return COMET_NKA;
    }

    @Override
    SocketLinkState toDuplex(TcpSocketLink conn)
    {
      return DUPLEX;
    }
  },

  /**
   * Waiting for a read from the keepalive connection.
   */
  REQUEST_KEEPALIVE {   // waiting for keepalive data
    @Override
    boolean isKeepalive() { return true; }

    @Override
    boolean isKeepaliveAllocated() { return true; }

    @Override
    SocketLinkState toActiveWithKeepalive(TcpSocketLink conn) 
    { 
      return REQUEST_ACTIVE_KA; 
    }

    @Override
    SocketLinkState toActiveNoKeepalive(TcpSocketLink conn) 
    {
      conn.getPort().keepaliveFree();
      
      return REQUEST_ACTIVE_KA; 
    }

    @Override
    SocketLinkState toKeepaliveSelect()
    {
      return REQUEST_KEEPALIVE_SELECT;
    }

    @Override
    SocketLinkState toClosed(TcpSocketLink conn)
    {
      conn.getPort().keepaliveFree();

      return CLOSED;
    }
  },    

  REQUEST_KEEPALIVE_SELECT {   // waiting for keepalive data (select)
    @Override
    boolean isKeepalive() { return true; }

    @Override
    boolean isKeepaliveAllocated() { return true; }

    @Override
    SocketLinkState toActiveWithKeepalive(TcpSocketLink conn) 
    { 
      return REQUEST_ACTIVE_KA; 
    }

    @Override
    SocketLinkState toActiveNoKeepalive(TcpSocketLink conn) 
    {
      conn.getPort().keepaliveFree();

      return REQUEST_ACTIVE_NKA; 
    }

    @Override
    SocketLinkState toClosed(TcpSocketLink conn)
    {
      conn.getPort().keepaliveFree();

      return CLOSED;
    }
  },    

  /**
   * Comet request with a keepalive allocated.
   */
  COMET_KA {                // processing an active comet service
    @Override
    boolean isComet() { return true; }

    @Override
    boolean isCometActive() { return true; }

    @Override
    boolean isKeepaliveAllocated() { return true; }

    @Override
    SocketLinkState toCometSuspend()
    {
      return COMET_SUSPEND_KA;
    }
    
    @Override
    SocketLinkState toCometDispatch()
    {
      return REQUEST_ACTIVE_KA;
    }

    @Override
    SocketLinkState toCometComplete()
    {
      return COMET_COMPLETE_KA;
    }

    @Override
    SocketLinkState toKillKeepalive(TcpSocketLink conn)
    {
      conn.getPort().keepaliveFree();
      
      return COMET_NKA;
    }
    
    @Override
    SocketLinkState toClosed(TcpSocketLink conn)
    {
      conn.getPort().keepaliveFree();
      
      return CLOSED;
    }
  },

  COMET_NKA {            // processing an active comet service
    @Override
    boolean isComet() { return true; }

    @Override
    boolean isCometActive() { return true; }

    @Override
    SocketLinkState toCometDispatch() 
    { 
      return REQUEST_ACTIVE_NKA;
    }

    @Override
    SocketLinkState toCometSuspend()
    {
      return COMET_SUSPEND_NKA;
    }

    @Override
    SocketLinkState toCometComplete()
    {
      return COMET_COMPLETE_NKA;
    }
  },

  COMET_SUSPEND_KA {        // suspended waiting for a wake
    @Override
    boolean isComet() { return true; }

    @Override
    boolean isCometSuspend() { return true; }

    @Override
    boolean isKeepaliveAllocated() { return true; }

    @Override
    SocketLinkState toKillKeepalive(TcpSocketLink conn)
    {
      conn.getPort().keepaliveFree();
      
      return COMET_SUSPEND_NKA;
    }

    @Override
    SocketLinkState toCometResume()
    {
      return COMET_KA;
    }
    
    @Override
    SocketLinkState toClosed(TcpSocketLink conn)
    {
      throw new IllegalStateException();
    }

    @Override
    SocketLinkState toDestroy(TcpSocketLink conn)
    {
      throw new IllegalStateException();
    }
  },

  COMET_SUSPEND_NKA {    // suspended waiting for a wake
    @Override
    boolean isComet() { return true; }

    @Override
    boolean isCometSuspend() { return true; }

    @Override
    SocketLinkState toCometResume()
    {
      return COMET_NKA;
    }
    
    @Override
    SocketLinkState toClosed(TcpSocketLink conn)
    {
      throw new IllegalStateException();
    }

    @Override
    SocketLinkState toDestroy(TcpSocketLink conn)
    {
      throw new IllegalStateException();
    }
  },

  COMET_COMPLETE_KA {       // complete or timeout
    @Override
    boolean isComet() { return true; }

    @Override
    boolean isCometComplete() { return true; }

    @Override
    boolean isKeepaliveAllocated() { return true; }

    @Override
    SocketLinkState toActiveWithKeepalive(TcpSocketLink conn) 
    { 
      return REQUEST_ACTIVE_KA;
    }

    @Override
    SocketLinkState toActiveNoKeepalive(TcpSocketLink conn) 
    { 
      conn.getPort().keepaliveFree();
      
      return REQUEST_ACTIVE_NKA;
    }

    @Override
    SocketLinkState toKillKeepalive(TcpSocketLink conn)
    {
      conn.getPort().keepaliveFree();
      
      return COMET_COMPLETE_NKA;
    }

    @Override
    SocketLinkState toCometComplete()
    {
      return this;
    }

    @Override
    SocketLinkState toKeepalive(TcpSocketLink conn)
    {
      return REQUEST_KEEPALIVE;
    }
    
    @Override
    SocketLinkState toClosed(TcpSocketLink conn)
    {
      conn.getPort().keepaliveFree();
      
      return CLOSED;
    }
  },

  COMET_COMPLETE_NKA {   // complete or timeout
    @Override
    boolean isComet() { return true; }

    @Override
    boolean isCometComplete() { return true; }

    @Override
    SocketLinkState toCometComplete()
    {
      return this;
    }
  },

  DUPLEX {               // converted to a duplex/websocket
    @Override
    boolean isDuplex() { return true; }

    @Override
    SocketLinkState toKeepalive(TcpSocketLink conn)
    {
      conn.getPort().duplexKeepaliveBegin();

      return DUPLEX_KEEPALIVE;
    }

    @Override
    SocketLinkState toDuplexActive(TcpSocketLink conn)
    {
      return DUPLEX;
    }
  },

  DUPLEX_KEEPALIVE {     // waiting for duplex read data
    @Override
    boolean isDuplex() { return true; }

    @Override
    boolean isKeepalive() { return true; }

    @Override
    SocketLinkState toDuplexActive(TcpSocketLink conn)
    {
      conn.getPort().duplexKeepaliveEnd();

      return DUPLEX;
    }

    @Override
    SocketLinkState toClosed(TcpSocketLink conn)
    {
      conn.getPort().duplexKeepaliveEnd();

      return CLOSED;
    }
  },

  CLOSED {               // connection closed, ready for accept
    @Override
    boolean isClosed() { return true; }

    @Override
    boolean isAllowIdle() { return true; }

    @Override
    SocketLinkState toAccept() 
    { 
      return ACCEPT; 
    }

    @Override
    SocketLinkState toIdle()
    {
      return IDLE;
    }
  },

  IDLE {                // TcpConnection in free list 
    @Override
    boolean isIdle() { return true; }

    @Override
    SocketLinkState toInit() 
    { 
      return INIT; 
    }

    @Override
    SocketLinkState toDestroy(TcpSocketLink conn)
    {
      throw new IllegalStateException(this + " is an illegal destroy state");
    }
  },                    

  DESTROYED {            // connection destroyed
    @Override
    boolean isClosed() { return true; }

    @Override
    boolean isDestroyed() { return true; }

    @Override
    SocketLinkState toIdle()
    {
      return this;
    }

    @Override
    SocketLinkState toClosed(TcpSocketLink conn)
    {
      return this;
    }

    @Override
    SocketLinkState toDestroy(TcpSocketLink conn)
    {
      return this;
    }
  };

  //
  // predicates
  //

  boolean isIdle()
  {
    return false;
  }
  
  boolean isComet()
  {
    return false;
  }

  boolean isCometActive()
  {
    return false;
  }

  boolean isCometSuspend()
  {
    return false;
  }

  boolean isCometComplete()
  {
    return false;
  }

  boolean isDuplex()
  {
    return false;
  }
  
  /**
   * True if a keepalive has been allocated, i.e. if the connection
   * is allowed to keepalive to the next request.
   */
  boolean isKeepaliveAllocated()
  {
    return false;
  }

  /**
   * True if the state is one of the keepalive states, either
   * a true keepalive-select or duplex.
   */
  boolean isKeepalive()
  {
    return false;
  }

  boolean isActive()
  {
    return false;
  }

  boolean isRequestActive()
  {
    return false;
  }

  boolean isClosed()
  {
    return false;
  }

  boolean isDestroyed()
  {
    return false;
  }

  boolean isAllowIdle()
  { 
    return false; 
  }

  //
  // state changes
  //

  /**
   * Convert from the idle (pooled) or closed state to the initial state
   * before accepting a connection.
   */
  SocketLinkState toInit()
  {
    throw new IllegalStateException(this + " cannot switch to init");
  }

  /**
   * Change to the accept state.
   */
  SocketLinkState toAccept()
  {
    throw new IllegalStateException(this + " cannot switch to accept");
  }

  /**
   * Changes to the active state with the keepalive allocated.
   */
  SocketLinkState toActiveWithKeepalive(TcpSocketLink conn)
  {
    throw new IllegalStateException(this + " cannot switch to active");
  }

  /**
   * Changes to the active state with no keepalive allocatedn.
   */
  SocketLinkState toActiveNoKeepalive(TcpSocketLink conn)
  {
    throw new IllegalStateException(this + " cannot switch to active");
  }

  /**
   * Kill the keepalive, i.e. remove the keepalive allocation.
   */
  SocketLinkState toKillKeepalive(TcpSocketLink conn)
  {
    return this;
  }

  SocketLinkState toKeepalive(TcpSocketLink conn)
  {
    throw new IllegalStateException(this + " cannot switch to keepalive");
  }

  SocketLinkState toKeepaliveSelect()
  {
    throw new IllegalStateException(this + " cannot switch to keepalive select");
  }

  //
  // comet
  //

  SocketLinkState toComet()
  {
    throw new IllegalStateException(this + " cannot switch to comet");
  }

  SocketLinkState toCometSuspend()
  {
    throw new IllegalStateException(this + " cannot suspend comet");
  }

  SocketLinkState toCometResume()
  {
    throw new IllegalStateException(this + " cannot resume comet");
  }

  SocketLinkState toCometDispatch()
  {
    throw new IllegalStateException(this + " cannot dispatch comet");
  }
    
  SocketLinkState toCometComplete()
  {
    throw new IllegalStateException(this + " cannot complete comet");
  }

  //
  // duplex/websocket
  //

  SocketLinkState toDuplex(TcpSocketLink conn)
  {
    throw new IllegalStateException(this + " cannot switch to duplex/websocket");
  }

  SocketLinkState toDuplexActive(TcpSocketLink conn)
  {
    throw new IllegalStateException(this + " cannot switch to duplex/websocket");
  }

  //
  // idle/close
  //

  SocketLinkState toIdle()
  {
    throw new IllegalStateException(this + " is an illegal idle state");
  }

  SocketLinkState toClosed(TcpSocketLink conn)
  {
    return CLOSED;
  }

  SocketLinkState toDestroy(TcpSocketLink conn)
  {
    toClosed(conn);

    conn.getPort().destroy(conn);

    return DESTROYED;
  }
}
