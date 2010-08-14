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


package com.caucho.quercus.lib;

import com.caucho.network.listen.SocketLinkDuplexListener;
import com.caucho.network.listen.SocketLinkDuplexController;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Value;
import com.caucho.server.http.CauchoRequest;
import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.*;
import java.util.logging.*;

public class WebSocketModule
  extends AbstractQuercusModule
{
  private static final L10N L = new L10N(WebSocketModule.class);
  private static final Logger log
    = Logger.getLogger(WebSocketModule.class.getName());
  
  /**
   * Writes a string to the websocket.
   */
  public static Value websocket_write(Env env, StringValue string)
  {
    try {
      OutputStream out = env.getResponse().getOutputStream();
      int length = string.length();
      int offset = 0;

      out.write(0x00);

      for (; offset < length; offset++) {
        char ch = string.charAt(offset);

        if ((ch & 0xf0) == 0xf0) {
          env.error("websocket_write expects utf-8 encoded string");
        }

        out.write(ch);
      }

      out.write(0xff);
      out.flush();
    
      return BooleanValue.TRUE;
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
      
      return BooleanValue.FALSE;
    }
  }
  
  /**
   * Reads a string from the websocket.
   */
  public static Value websocket_read(Env env)
  {
    try {
      InputStream is = env.getRequest().getInputStream();

      int ch;

      for (ch = is.read(); Character.isWhitespace(ch); ch = is.read()) {
      }

      if (ch != 0x00) {
        log.fine("websocket_read expected 0x00 at '0x" + Integer.toHexString(ch) + "'");
        
        return BooleanValue.FALSE;
      }

      StringValue sb = env.createStringBuilder();

      while ((ch = is.read()) >= 0 && ch != 0xff) {
        if (ch < 0x80)
          sb.append((char) ch);
        else if ((ch & 0xe0) == 0xc0) {
          int ch2 = is.read();
          
          if ((ch2 & 0x80) == 0x80) {
            sb.append(ch);
            sb.append(ch2);
          }
          else {
            log.fine("websocket_read expected 0x80 character at '0x"
                     + Integer.toHexString(ch2) + "' for string " + sb);
            sb.append(0xfe);
            sb.append(0xdd);
          }
        }
        else if ((ch & 0xf0) == 0xe0) {
          int ch2 = is.read();
          int ch3 = is.read();
          
          if ((ch2 & 0x80) == 0x80 && (ch3 & 0x80) == 0x80) {
            sb.append(ch);
            sb.append(ch2);
            sb.append(ch3);
          }
          else {
            log.fine("websocket_read expected 0x80 character at "
                     + " '0x" + Integer.toHexString(ch2)
                     + "' '0x" + Integer.toHexString(ch3)
                     + "' for string " + sb);
            
            sb.append(0xfe);
            sb.append(0xdd);
          }
        }
        else {
          log.fine("websocket_read invalid lead character "
                   + " '0x" + Integer.toHexString(ch)
                   + "' for string " + sb);
            
          sb.append(0xfe);
          sb.append(0xdd);
        }
      }

      if (ch != 0xff) {
          log.fine("websocket_read expected 0xff "
                   + " '0x" + Integer.toHexString(ch)
                   + "' for string " + sb);
            
      }
    
      return sb;
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
      
      return BooleanValue.FALSE;
    }
  }
  
  /**
   * Reads a string from the websocket.
   */
  public static SocketLinkDuplexController websocket_start(Env env, StringValue path)
  {
    if (! (env.getRequest() instanceof CauchoRequest)) {
      env.warning("websocket_start requires a Resin request at "
                  + env.getRequest());
      return null;
    }

    CauchoRequest request = (CauchoRequest) env.getRequest();

    String connection = request.getHeader("Connection");
    String upgrade = request.getHeader("Upgrade");

    if (! "WebSocket".equals(upgrade)) {
      env.warning("request Upgrade header '" + upgrade + "' must be 'WebSocket' for a websocket_start");
      return null;
    }

    if (! "Upgrade".equalsIgnoreCase(connection)) {
      env.warning("request connection header '" + connection + "' must be 'Upgrade' for a websocket_start");
      return null;
    }
    
    String origin = request.getHeader("Origin");

    if (origin == null) {
      env.warning("websocket_start requires an 'Origin' header in the request");
      return null;
    }

    env.getResponse().setStatus(101, "Web Socket Protocol Handshake");
    env.getResponse().setHeader("Upgrade", "WebSocket");

    String protocol = request.getHeader("WebSocket-Protocol");

    StringBuilder sb = new StringBuilder();
    if (request.isSecure())
      sb.append("wss://");
    else
      sb.append("ws://");
    sb.append(request.getServerName());

    if (! request.isSecure() && request.getServerPort() != 80
        || request.isSecure() && request.getServerPort() != 443) {
      sb.append(":");
      sb.append(request.getServerPort());
    }
    
    sb.append(request.getContextPath());
    if (request.getServletPath() != null)
      sb.append(request.getServletPath());

    String url = sb.toString();

    if (origin != null)
      env.getResponse().setHeader("WebSocket-Origin", origin.toLowerCase());

    if (protocol != null)
      env.getResponse().setHeader("WebSocket-Protocol", protocol);
    
    env.getResponse().setHeader("WebSocket-Location", url);
    
    // XXX: validate path

    WebSocketListener listener = new WebSocketListener(env, path);

    SocketLinkDuplexController context = null;//request.startDuplex(listener);

    // context.setTimeout(30 * 60000L);

    env.startDuplex(context);

    return context;
  }

  public static class WebSocketListener implements SocketLinkDuplexListener {
    private Env _env;
    private StringValue _path;

    WebSocketListener(Env env, StringValue path)
    {
      _env = env;
      _path = path;
    }
    
    @Override
    public void onRead(SocketLinkDuplexController context)
    {
      boolean isValid = false;
      
      try {
        if (log.isLoggable(Level.FINE))
          log.fine(this + " WebSocket read " + _path);
        
        _env.include(_path);
        
        isValid = true;
      } finally {
        if (! isValid) {
          if (log.isLoggable(Level.FINE))
            log.fine(this + " WebSocket exit " + _path);
          
          context.complete();
        }
      }
    }
    
    public void onComplete(SocketLinkDuplexController context)
    {
      try {
        _env.closeDuplex();
      } finally {
        context.complete();
      }
    }
    
    public void onTimeout(SocketLinkDuplexController context)
    {
      try {
        _env.closeDuplex();
      } finally {
        context.complete();
      }
    }

    public String toString()
    {
      return getClass().getSimpleName() + "[" + _path + "]";
    }

    @Override
    public void onStart(SocketLinkDuplexController context) throws IOException
    {
    }
  }
}
