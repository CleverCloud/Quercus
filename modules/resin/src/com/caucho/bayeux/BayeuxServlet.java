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
 * @author Emil Ong
 */

package com.caucho.bayeux;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.servlet.comet.*;

/**
 * Servlet to handle bayeux requests.
 */
public class BayeuxServlet extends GenericCometServlet 
{
  private static final Logger log = 
    Logger.getLogger(BayeuxServlet.class.getName());
  private static final L10N L = new L10N(BayeuxServlet.class);
  private static final ChannelTree _root = new ChannelTree();
  private static final ConcurrentHashMap<String,BayeuxClient> _clients = 
    new ConcurrentHashMap<String,BayeuxClient>();

  /**
   * Services the initial request.
   *
   * @param request the servlet request object
   * @param response the servlet response object
   * @param controller the controller to be passed to application code
   *
   * @return true for keepalive, false for the end of the request
   */
  @Override
  public boolean service(ServletRequest req,
                         ServletResponse resp,
                         CometController controller)
    throws IOException, ServletException
  {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) resp;

    if (! "POST".equals(request.getMethod())) {
      response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
      return false;
    }

    try {
      String message = request.getParameter("message");

      if (log.isLoggable(Level.FINEST)) {
        log.finest("message = " + message);
      }

      JsonObject object = JsonDecoder.decode(message);

      if (! (object instanceof JsonArray)) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        // XXX: Allow single JsonMap object
        return false;
      }

      JsonArray array = (JsonArray) object;

      for (int i = 0; i < array.size(); i++) {
        if (! (array.get(i) instanceof JsonMap))
          continue;

        JsonMap map = (JsonMap) array.get(i);

        JsonObject channelObject = map.get("channel");

        if (! (channelObject instanceof JsonString))
          continue;

        String channel = channelObject.toString();

        if ("/meta/handshake".equals(channel)) {
          handleHandshake(map, request, response, controller);

          // the handshake is handled as normal HTTP request, so disconnect
          return false;
        }
        else if ("/meta/connect".equals(channel)) {
          return handleConnect(map, request, response, controller);
        }
        else if ("/meta/subscribe".equals(channel)) {
          return handleSubscribe(map, request, response, controller);
        }
      }

      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown channel");

      return false;
    }
    catch (JsonDecodingException e) {
      throw new ServletException(e);
    }
  }

  /**
   * Resumes service the initial request.
   *
   * @param request the servlet request object
   * @param response the servlet response object
   * @param controller the controller to be passed to application code
   *
   * @return true for keepalive, false for the end of the request
   */
  @Override
  public boolean resume(ServletRequest request,
                        ServletResponse response,
                        CometController controller)
    throws IOException, ServletException
  {
    return false;
  }

  private void handleHandshake(JsonMap map,
                               HttpServletRequest request,
                               HttpServletResponse response,
                               CometController controller)
    throws IOException, ServletException
  {
    PrintWriter out = response.getWriter();

    JsonObject version = map.get("version");
    JsonObject supportedConnectionTypes = map.get("supportedConnectionTypes");
    JsonObject minimumVersion = map.get("minimumVersion");
    JsonObject ext = map.get("ext");
    JsonObject id = map.get("id");

    if (version == null) {
      sendHandshakeResponse(out, null, id, false, "missing version");
      return;
    }

    if (supportedConnectionTypes == null) {
      sendHandshakeResponse(out, null, id, false, 
                            "missing supportedConnectionTypes");
      return;
    }

    UUID uuid = UUID.randomUUID();
    sendHandshakeResponse(out, uuid.toString(), id, true, null);
  }

  private void sendHandshakeResponse(PrintWriter out, 
                                     String clientId, 
                                     JsonObject id, 
                                     boolean success,
                                     String error)
    throws IOException, ServletException
  {
    out.println("[");
    out.println("\t{");
    out.println("\t\t\"channel\": \"/meta/handshake\",");
    out.println("\t\t\"version\": \"1.0beta\",");
    out.println("\t\t\"supportedConnectionTypes\": " + 
                "[\"long-polling\", \"callback-polling\", \"iframe\"],");

    if (clientId != null) 
      out.println("\t\t\"clientId\": \"" + clientId + "\",");

    out.println("\t\t\"successful\": \"" + success + "\",");

    if (id != null && (id instanceof JsonString))
      out.println("\t\t\"id\": \"" + id + "\",");

    if (error != null)
      out.println("\t\t\"error\": \"" + error + "\",");

    out.println("\t}");
    out.println("]");
  }

  private boolean handleConnect(JsonMap map,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                CometController controller)
    throws IOException, ServletException
  {
    PrintWriter out = response.getWriter();

    JsonObject clientId = map.get("clientId");
    JsonObject connectionType = map.get("connectionType");
    JsonObject ext = map.get("ext");
    JsonObject id = map.get("id");

    BayeuxClient client = _clients.get(clientId);

    /* XXX
    if (client != null)
      clientId = UUID.randomUUID().toString();*/

    for (BayeuxConnectionType type : BayeuxConnectionType.values()) {
      if (type.toString().equals(connectionType)) {
        client = new BayeuxClient(controller, clientId.toString(), type);

        _clients.put(clientId.toString(), client);

        sendConnectResponse(out, clientId, id, true, null);
        return true;
      }
    }

    sendConnectResponse(out, clientId, id, false, "bad connection type");

    return false;
  }

  private void sendConnectResponse(PrintWriter out, 
                                   JsonObject clientId, 
                                   JsonObject id, 
                                   boolean success,
                                   String error)
    throws IOException, ServletException
  {
    out.println("[");
    out.println("\t{");
    out.println("\t\t\"channel\": \"/meta/connect\",");

    if (clientId != null) 
      out.println("\t\t\"clientId\": \"" + clientId + "\",");

    out.println("\t\t\"successful\": \"" + success + "\",");

    if (id != null)
      out.println("\t\t\"id\": \"" + id + "\",");

    if (error != null)
      out.println("\t\t\"error\": \"" + error + "\",");

    out.println("\t}");
    out.println("]");
  }

  private boolean handleSubscribe(JsonMap map,
                                  HttpServletRequest request,
                                  HttpServletResponse response,
                                  CometController controller)
    throws IOException, ServletException
  {
    PrintWriter out = response.getWriter();

    JsonObject clientId = map.get("clientId");
    JsonObject subscription = map.get("subscription");
    JsonObject id = map.get("id");
    JsonObject ext = map.get("ext");

    BayeuxClient client = _clients.get(clientId);

    if (client == null) {
      sendSubcribeResponse(out, clientId, subscription, id, false, 
                           "Unknown client");
      return false;
    }

    _root.subscribe(subscription.toString(), client);

    sendSubcribeResponse(out, clientId, subscription, id, true, null);

    return false;
  }

  private void sendSubcribeResponse(PrintWriter out, 
                                    JsonObject clientId, 
                                    JsonObject subscription, 
                                    JsonObject id, 
                                    boolean success,
                                    String error)
    throws IOException, ServletException
  {
    out.println("[");
    out.println("\t{");
    out.println("\t\t\"channel\": \"/meta/subscribe\",");

    if (clientId != null) 
      out.println("\t\t\"clientId\": \"" + clientId + "\",");

    if (subscription != null) 
      out.println("\t\t\"subscription\": \"" + subscription + "\",");

    out.println("\t\t\"successful\": \"" + success + "\",");

    if (id != null)
      out.println("\t\t\"id\": \"" + id + "\",");

    if (error != null)
      out.println("\t\t\"error\": \"" + error + "\",");

    out.println("\t}");
    out.println("]");
  }
}
