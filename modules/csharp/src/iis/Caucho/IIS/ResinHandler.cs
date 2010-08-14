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
 * @author Alex Rojkov
 */

using System;
using System.Collections.Specialized;
using System.Diagnostics;
using System.IO;
using System.Text;
using System.Web;
using System.Configuration;
using System.Web.Configuration;
using System.Threading;

namespace Caucho.IIS
{
  public class ResinHandler : IHttpHandler
  {
    private const int HTTP_STATUS_SERVICE_UNAVAIL = 503;
    private const String STATUS_OK_CLR = "#66ff66";
    private const String STATUS_BAD_CLR = "#ff6666";

    private const int EXIT_MASK = 0x1;
    private const int QUIT = 0x0;
    private const int EXIT = 0x1;

    private const int STATUS_MASK = 0x6;
    private const int OK = 0x0; // request succeeded
    private const int BUSY = 0x2; // server sends busy (retry GET/POST)
    private const int FAIL = 0x4; // server failed (retry GET)

    private Logger _log;
    private Exception _e;

    private LoadBalancer _loadBalancer;

    //session config
    private bool _isStickySessions = true;
    private String _sessionCookieName = "JSESSIONID";
    private String _sslSessionCookieName = "SSLJSESSIONID";
    private bool _isUrlRewriteEnabled = true;
    private String _sessionUrlPrefix = ";jsessionid=";
    private bool _isDebug = false;
    private bool _isCauchoStatusEnabled = true;

    public ResinHandler()
    {
      init();
    }

    private void init()
    {
      try {
        NameValueCollection appSettings = WebConfigurationManager.GetSection("appSettings") as NameValueCollection;

        _log = Logger.GetLogger();

        String servers = appSettings["resin.servers"];

        if ("".Equals(servers)) {
          servers = "127.0.0.1:6800";
          _log.Info("application setting 'resin.servers' is not specified. Using '{0}'", servers);
          Trace.TraceInformation("application setting 'resin.servers' is not specified. Using '{0}'", servers);
        } else {
          _log.Info("Setting servers to '{0}'", servers);
          Trace.TraceInformation("Setting servers to '{0}'", servers);
        }

        if (!String.IsNullOrEmpty(appSettings["resin.session-cookie"]))
          _sessionCookieName = appSettings["resin.session-cookie"];

        if (!String.IsNullOrEmpty(appSettings["resin.ssl-session-cookie"]))
          _sslSessionCookieName = appSettings["resin.ssl-session-cookie"];

        if ("false".Equals(appSettings["resin.sticky-sessions"], StringComparison.OrdinalIgnoreCase))
          _isStickySessions = false;

        _log.Info("Setting sticky sessions to {0}", _isStickySessions);

        if (!String.IsNullOrEmpty(appSettings["resin.session-url-prefix"]))
          _sessionUrlPrefix = appSettings["resin.session-url-prefix"];

        if (!String.IsNullOrEmpty(appSettings["resin.alternate-session-url-prefix"]))
          _sessionUrlPrefix = appSettings["resin.alternate-session-url-prefix"];

        int loadBalanceConnectTimeout = 5 * 1000;
        if (!String.IsNullOrEmpty(appSettings["resin.load-balance-connect-timeout"]))
          loadBalanceConnectTimeout = ParseTime("resin.load-balance-connect-timeout", appSettings["resin.load-balance-connect-timeout"]);

        int loadBalanceIdleTime = 5 * 1000;
        if (!String.IsNullOrEmpty(appSettings["resin.load-balance-idle-time"]))
          loadBalanceIdleTime = ParseTime("resin.load-balance-idle-time", appSettings["resin.load-balance-idle-time"]);

        int loadBalanceRecoverTime = 15 * 1000;
        if (!String.IsNullOrEmpty(appSettings["resin.load-balance-recover-time"]))
          loadBalanceRecoverTime = ParseTime("resin.load-balance-recover-time", appSettings["resin.load-balance-recover-time"]);

        int loadBalanceSocketTimeout = 665 * 1000;
        if (!String.IsNullOrEmpty(appSettings["resin.load-balance-socket-timeout"]))
          loadBalanceSocketTimeout = ParseTime("resin.load-balance-socket-timeout", appSettings["resin.load-balance-socket-timeout"]);

        int keepaliveTimeout = 15 * 1000;
        if (!String.IsNullOrEmpty(appSettings["resin.keepalive-timeout"]))
          keepaliveTimeout = ParseTime("resin.keepalive-timeout", appSettings["resin.keepalive-timeout"]);

        int socketTimeout = 65 * 1000;
        if (!String.IsNullOrEmpty(appSettings["resin.socket-timeout"]))
          socketTimeout = ParseTime("resin.socket-timeout", appSettings["resin.socket-timeout"]);

        _isDebug = "true".Equals(appSettings["resin.debug"], StringComparison.OrdinalIgnoreCase);

        if (_isDebug)
          Trace.TraceInformation("Setting debug to true");

        _isCauchoStatusEnabled = !"false".Equals(appSettings["resin.caucho-status"], StringComparison.OrdinalIgnoreCase);

        _loadBalancer = new LoadBalancer(servers,
          loadBalanceConnectTimeout,
          loadBalanceIdleTime,
          loadBalanceRecoverTime,
          loadBalanceSocketTimeout,
          keepaliveTimeout,
          socketTimeout,
          _isDebug);

      } catch (ConfigurationException e) {
        _e = e;
      } catch (FormatException e) {
        _e = new ConfigurationException(e.Message, e);
      }
    }

    private int ParseTime(String name, String str)
    {
      int value = 0;
      bool isDigitExpected = true;

      for (int i = 0; i < str.Length; i++) {
        char c = str[i];
        if (Char.IsDigit(c) && isDigitExpected) {
          value = value * 10 + c - '0';

        } else if ('s' == c || 'S' == c || 'm' == c || 'M' == c) {
          value = value * 1000;

          if ('m' == c || 'M' == c)
            value = value * 60;

          if (i + 1 < str.Length || !isDigitExpected) {
            String message = String.Format("Can't convert {0} ('{1}') to a number.", name, str);
            _log.Error(message);
            throw new FormatException(message);
          }

          isDigitExpected = false;

          break;
        } else {
          String message = String.Format("Can't convert {0} ('{1}') to a number.", name, str);
          _log.Error(message);
          throw new FormatException(message);
        }
      }

      return value;
    }

    public bool IsReusable
    {
      get { return true; }
    }

    public void ProcessRequest(HttpContext context)
    {
      String contextPath = context.Request.ApplicationPath;
      String cauchoStatus = "/caucho-status";
      if (!"/".Equals(contextPath)) {
        cauchoStatus = contextPath + "/caucho-status";
      }
      String path = context.Request.Path;
      if (_e != null) {
        DoConfigurationError(context);
      } else if (_isDebug) {
        if (path.Contains("/caucho__test__basic")) {
          DoTestBasic(context);
        } else if (path.Contains("/caucho__test__chunked")) {
          DoTestChunked(context);
        } else if (path.Contains("/caucho__test__ssl")) {
          DoTestSSL(context);
        } else if (path.Contains("/caucho-status")) {
          DoCauchoStatus(context);
        } else {
          DoHmux(context);
        }
      } else if (path.StartsWith(cauchoStatus)) {
        DoCauchoStatus(context);
      } else {
        DoHmux(context);
      }
    }

    public String GetRequestedSessionId(HttpRequest request)
    {
      String path = request.Path;

      int sessionIdx = path.LastIndexOf(_sessionUrlPrefix);
      String sessionId = null;

      if (sessionIdx > -1)
        sessionId = path.Substring(sessionIdx + _sessionUrlPrefix.Length);

      HttpCookie cookie = null;
      if (sessionId == null && request.IsSecureConnection)
        cookie = request.Cookies[_sslSessionCookieName];

      if (sessionId == null && cookie != null)
        sessionId = cookie.Value;

      if (sessionId == null)
        cookie = request.Cookies[_sessionCookieName];

      if (sessionId == null && cookie != null)
        sessionId = cookie.Value;

      return sessionId;
    }

    private void DoHmux(HttpContext context)
    {
      HttpRequest request = context.Request;
      HttpResponse response = context.Response;

      String sessionId;

      if (!_isStickySessions)
        sessionId = null;
      else
        sessionId = GetRequestedSessionId(request);

      TempBuffer tempBuf = TempBuffer.Allocate();
      byte[] buf = tempBuf.GetBuffer();

      try {
        Stream is_ = request.InputStream;
        int len = is_.Read(buf, 0, buf.Length);
        bool isComplete;

        if (len < 0)
          isComplete = true;
        else if (len < buf.Length) {
          int sublen = is_.Read(buf, len, 1);

          if (sublen == 0) //.NET return 0 for Stream.EOF
            isComplete = true;
          else {
            len += sublen;
            isComplete = false;
          }
        } else
          isComplete = false;

        Server client = null;
        int result = OK | EXIT;

        HmuxConnection channel = _loadBalancer.OpenServer(sessionId, null);

        // If everything fails, return an error
        if (channel == null) {
          SendNotAvailable(response);

          return;
        }

        client = channel.GetPool();

        BufferedStream rs = channel.GetSocketStream();
        BufferedStream ws = channel.GetSocketStream();
        result = FAIL | EXIT;
        long requestStartTime = Utils.CurrentTimeMillis();

        try {
          result = HandleRequest(request, response, channel, rs, ws,
                                 buf, len, isComplete, isComplete);

          if ((result & STATUS_MASK) == OK) {
            client.ClearBusy();

            return;
          } else if ((result & STATUS_MASK) == BUSY) {
            client.Busy();
          } else {
            client.FailSocket();
          }
        } catch (ClientDisconnectException) {
          _log.Info("Client disconnect detected for '{0}'", channel.GetTraceId());

          return;
        } catch (IOException e) {
          client.FailSocket();
          _log.Warning("IOException '{0}': '{1}' {2}", channel.GetTraceId(), e.Message, e.StackTrace);
        } finally {
          if ((result & EXIT_MASK) == QUIT)
            channel.Free(requestStartTime);
          else
            channel.Close();
        }

        // server/2675
        if (isComplete && (result & STATUS_MASK) == BUSY
            || "GET".Equals(request.HttpMethod)) {
          channel = _loadBalancer.OpenServer(sessionId, client);

          // If everything fails, return an error
          if (channel == null) {
            _log.Info("load-balance failed" + (client != null ? (" for " + client.GetDebugId()) : ""));

            SendNotAvailable(response);

            return;
          }

          Server client2 = channel.GetPool();

          if (_log.IsLoggable(EventLogEntryType.Information)) {
            _log.Info("load-balance failing over"
              + (client != null ? (" from " + client.GetDebugId()) : "")
             + " to " + client2.GetDebugId());
          }

          rs = channel.GetSocketStream();
          ws = channel.GetSocketStream();

          result = FAIL | EXIT;
          requestStartTime = Utils.CurrentTimeMillis();

          try {
            result = HandleRequest(request, response, channel, rs, ws,
                                   buf, len, isComplete, false);

            if ((result & STATUS_MASK) == OK) {
              client2.ClearBusy();

              return;
            } else if ((result & STATUS_MASK) == BUSY) {
              client2.Busy();
            } else {
              client2.FailSocket();
            }
          } catch (IOException e) {
            client2.FailSocket();

            _log.Info("Failover to '{0}' did not succeed '{1}', {2} ", client2.GetDebugId(), e.Message, e.StackTrace);
          } finally {
            if ((result & EXIT_MASK) == QUIT)
              channel.Free(requestStartTime);
            else
              channel.Close();
          }
        }

        SendNotAvailable(response);
      } finally {
        TempBuffer.Free(tempBuf);
        tempBuf = null;
      }
    }

    private int HandleRequest(HttpRequest request,
                            HttpResponse response,
                            HmuxConnection hmuxChannel,
                            BufferedStream rs,
                            BufferedStream ws,
                            byte[] buf, int length, bool isComplete,
                            bool allowBusy)
    {
      Trace.TraceInformation("Handle request: length: {0}, complete: {1}, allowBusy {2}", length, isComplete, allowBusy);
      String traceId = hmuxChannel.GetTraceId();

      StringBuilder cb = new StringBuilder();

      bool isDebugFiner = true;

      String uri = request.Path;
      uri = Uri.EscapeUriString(uri);

      Trace.TraceInformation("Hmux[{0}] >>U:uri {1}->{2}", traceId, request.RawUrl, uri);
      WriteRequestString(ws, HmuxConnection.HMUX_URI, uri, traceId);

      String rawUri = request.RawUrl;
      int queryIdx = rawUri.IndexOf('?');
      if (queryIdx > -1 && queryIdx + 1 < rawUri.Length) {
        String query = rawUri.Substring(queryIdx + 1);
        Trace.TraceInformation("Hmux[{0}] >>U:query {1}", traceId, query);
        WriteRequestString(ws, HmuxConnection.CSE_QUERY_STRING, query, traceId);
      }

      Trace.TraceInformation("Hmux[{0}] >>m:method {1}", traceId, request.HttpMethod);
      WriteRequestString(ws, HmuxConnection.HMUX_METHOD, request.HttpMethod, traceId);

      Trace.TraceInformation("Hmux[{0}] >>u:server type {1}", traceId, "IIS");
      WriteRequestString(ws, HmuxConnection.CSE_SERVER_TYPE, "IIS", traceId);

      NameValueCollection serverVariables = request.ServerVariables;

      String serverPort = serverVariables.Get("SERVER_PORT");
      String serverName = serverVariables.Get("SERVER_NAME") + ':' + serverPort;
      Trace.TraceInformation("Hmux[{0}] >>v:server name {1}", traceId, serverName);
      WriteRequestString(ws, HmuxConnection.HMUX_SERVER_NAME, serverName, traceId);

      Trace.TraceInformation("Hmux[{0}] >>g:server port {1}", traceId, serverPort);
      WriteRequestString(ws, HmuxConnection.CSE_SERVER_PORT, serverPort, traceId);

      String remoteAddr = serverVariables.Get("REMOTE_ADDR");
      Trace.TraceInformation("Hmux[{0}] >>i:remote address {1}", traceId, remoteAddr);
      WriteRequestString(ws, HmuxConnection.CSE_REMOTE_ADDR, remoteAddr, traceId);

      String remoteHost = serverVariables.Get("REMOTE_HOST");
      if (remoteHost == null)
        remoteHost = remoteAddr;

      Trace.TraceInformation("Hmux[{0}] >>h:remote host {1}", traceId, remoteHost);
      WriteRequestString(ws, HmuxConnection.CSE_REMOTE_HOST, remoteHost, traceId);

      String protocol = serverVariables.Get("HTTP_VERSION");
      Trace.TraceInformation("Hmux[{0}] >>c:protocol {1}", traceId, protocol);
      WriteRequestString(ws, HmuxConnection.CSE_PROTOCOL, protocol, traceId);

      HttpClientCertificate clientCertificate = request.ClientCertificate;
      if (request.IsSecureConnection) {
        Trace.TraceInformation("Hmux[{0}] >>r:secure", traceId);
        WriteRequestString(ws, HmuxConnection.CSE_IS_SECURE, "", traceId);

        WriteRequestHeader(ws, "HTTPS", "on", traceId);
        WriteRequestHeader(ws, "SSL_SECRETKEYSIZE", clientCertificate.KeySize.ToString(), traceId);
      }

      if (clientCertificate.IsPresent) {
        Trace.TraceInformation("Hmux[{0}] >>r:certificate ({1})", traceId, clientCertificate.Certificate.Length);
        ws.WriteByte(HmuxConnection.CSE_CLIENT_CERT);
        WriteHmuxLength(ws, clientCertificate.Certificate.Length);
        ws.Write(clientCertificate.Certificate, 0, clientCertificate.Certificate.Length);
      }


      NameValueCollection headers = request.Headers;
      foreach (String key in headers.AllKeys) {
        if ("Connection".Equals(key, StringComparison.OrdinalIgnoreCase))
          continue;

        String[] values = headers.GetValues(key);
        foreach (String value in values) {
          WriteRequestHeader(ws, key, value, traceId);
        }
      }

      if (_isDebug) {
        WriteRequestHeader(ws, "X-Resin-Debug", traceId, traceId);
      }

      Stream requestStream = request.InputStream;
      Stream responseStream = null;

      bool hasHeader = true;
      bool hasStatus = false;


      if (length > 0) {
        Trace.TraceInformation("Hmux[{0}] >>D: data ({1})", traceId, length);
        WriteRequestData(ws, HmuxConnection.HMUX_DATA, buf, length, traceId);
      }

      int len;

      int code;

      while (!isComplete && (len = requestStream.Read(buf, 0, buf.Length)) > 0) {
        Trace.TraceInformation("Hmux[{0}] >>D: data ({1})", traceId, length);
        WriteRequestData(ws, HmuxConnection.HMUX_DATA, buf, len, traceId);

        Trace.TraceInformation("Hmux[{0}] >>Y: (yield)", traceId);
        ws.WriteByte(HmuxConnection.HMUX_YIELD);
        ws.Flush();

        while (true) {
          code = rs.ReadByte();

          if (code < 0) {
            Trace.TraceInformation("Hmux[{0}] <<w: end of file", traceId);

            if (hasStatus)
              return OK | EXIT;
            else {
              Trace.TraceInformation("Hmux[{0}] <<w: unexpected end of file", traceId);

              return FAIL | EXIT;
            }
          } else if (code == HmuxConnection.HMUX_QUIT) {
            Trace.TraceInformation("Hmux[{0}] <<Q: (keepalive)", traceId);

            if (hasStatus)
              return OK | QUIT;
            else {
              Trace.TraceInformation("Hmux[{0}] <<Q: unexpected quit file", traceId);

              return FAIL | QUIT;
            }
          } else if (code == HmuxConnection.HMUX_EXIT) {
            Trace.TraceInformation("Hmux[{0}] <<X: (exit)", traceId);

            if (hasStatus) {
              return OK | EXIT;
            } else {
              Trace.TraceInformation("Hmux[{0}] <<X: unexpected exit", traceId);

              return FAIL | EXIT;
            }
          } else if (code == HmuxConnection.HMUX_YIELD) {
            Trace.TraceInformation("Hmux[{0}] <<Y: (yield)", traceId);

            continue;
          }

          int sublen = ReadHmuxLength(rs);

          if (code == HmuxConnection.HMUX_ACK) {
            if (isDebugFiner)
              Trace.TraceInformation("Hmux[{0}] <<A: (ack) ({1})", traceId, sublen);

            break;
          } else if (code == HmuxConnection.HMUX_CHANNEL) {
            int channel = sublen;
            Trace.TraceInformation("Hmux[{0}] <<C: (channel) ({1})", traceId, channel);
          } else if (code == HmuxConnection.HMUX_STATUS && hasHeader) {
            String status = ReadHmuxString(rs, sublen);
            Trace.TraceInformation("Hmux[{0}] <<s: (status) ({1})", traceId, status);
            int statusCode = 0;
            for (int i = 0; i < 3; i++)
              statusCode = 10 * statusCode + status[i] - '0';

            if (statusCode != 200)
              response.StatusCode = statusCode;

            hasStatus = true;
          } else if (code == HmuxConnection.HMUX_HEADER && hasHeader) {
            String name = ReadHmuxString(rs, sublen);
            rs.ReadByte();
            sublen = ReadHmuxLength(rs);
            String value = ReadHmuxString(rs, sublen);

            Trace.TraceInformation("Hmux[{0}] <<H,S: (header) ({1}={2})", traceId, name, value);

            RelayResponseHeader(response, name, value);
          } else if (code == HmuxConnection.HMUX_DATA) {
            Trace.TraceInformation("Hmux[{0}] <<D: (data)({1})", traceId, sublen);

            if (responseStream == null)
              responseStream = response.OutputStream;

            RelayResponseData(rs, responseStream, sublen);
          } else if (code == HmuxConnection.HMUX_META_HEADER) {
            String name = ReadHmuxString(rs, sublen);
            rs.ReadByte();
            sublen = ReadHmuxLength(rs);
            String value = ReadHmuxString(rs, sublen);

            Trace.TraceInformation("Hmux[{0}] <<M,S: header ({1}={2})", traceId, name, value);

            if ("cpu-load".Equals(name)) {
              double loadAvg = 0.001 * long.Parse(value);

              hmuxChannel.GetPool().SetCpuLoadAvg(loadAvg);
            }
          } else {
            Skip(rs, sublen);
          }
        }
      }

      ws.WriteByte(HmuxConnection.HMUX_QUIT);
      ws.Flush();

      code = rs.ReadByte();

      // #2369 - A slow modem can cause the app-tier and web-tier times
      // to get out of sync, with the app-tier thinking it's completed
      // (and starts the keepalive timeout) 30s before the web-tier reads
      // its data.
      // As a temporary measure, we start the idle time at the first data
      // read (later we might mark the time it takes to read an app-tier
      // packet.  If it's short, e.g. 250ms, don't update the time.)
      hmuxChannel.SetIdleStartTime(Utils.CurrentTimeMillis());

      bool isBusy = false;
      for (; code >= 0; code = rs.ReadByte()) {
        if (code == HmuxConnection.HMUX_QUIT) {
          if (isDebugFiner)
            Trace.TraceInformation("Hmux[{0}] <<Q: (keepalive)", traceId);

          return isBusy ? BUSY | QUIT : OK | QUIT;
        } else if (code == HmuxConnection.HMUX_EXIT) {

          Trace.TraceInformation("Hmux[{0}] <<X: (exit)", traceId);

          return (isBusy || !hasStatus) ? BUSY | EXIT : OK | EXIT;
        } else if (code == HmuxConnection.HMUX_YIELD) {
          Trace.TraceInformation("Hmux[{0}] <<Y: (yield)", traceId);

          continue;
        }

        int sublen = (rs.ReadByte() << 8) + rs.ReadByte();

        if (code == HmuxConnection.HMUX_DATA) {
          if (responseStream == null)
            responseStream = response.OutputStream;

          Trace.TraceInformation("Hmux[{0}] <<D: (data)({1})", traceId, sublen);

          if (!isBusy)
            RelayResponseData(rs, responseStream, sublen);
          else
            Skip(rs, sublen);
        } else if (code == HmuxConnection.HMUX_STATUS && hasHeader) {
          hasStatus = true;
          String status = ReadHmuxString(rs, sublen);
          Trace.TraceInformation("Hmux[{0}] <<s: (status) ({1})", traceId, status);

          int statusCode = 0;
          for (int i = 0; i < 3; i++)
            statusCode = 10 * statusCode + status[i] - '0';

          if (statusCode == 503 && allowBusy) {
            isBusy = true;
          } else if (statusCode != 200) {
            response.StatusCode = statusCode;
          }
        } else if (code == HmuxConnection.HMUX_HEADER && hasHeader) {
          String name = ReadHmuxString(rs, sublen);
          rs.ReadByte();
          sublen = ReadHmuxLength(rs);
          String value = ReadHmuxString(rs, sublen);

          Trace.TraceInformation("Hmux[{0}] <<H,S: (header) ({1}={2})", traceId, name, value);

          if (!isBusy)
            RelayResponseHeader(response, name, value);
        } else if (code == HmuxConnection.HMUX_META_HEADER) {
          String name = ReadHmuxString(rs, sublen);
          rs.ReadByte();
          sublen = ReadHmuxLength(rs);
          String value = ReadHmuxString(rs, sublen);

          Trace.TraceInformation("Hmux[{0}] <<M,S: header ({1}={2})", traceId, name, value);

          if ("cpu-load".Equals(name)) {
            double loadAvg = 0.001 * long.Parse(value);

            hmuxChannel.GetPool().SetCpuLoadAvg(loadAvg);
          }
        } else if (code == HmuxConnection.HMUX_CHANNEL) {
          int channel = sublen;
          Trace.TraceInformation("Hmux[{0}] <<C: (channel) ({1})", traceId, channel);
        } else if (code == 0) {
          Trace.TraceInformation("Hmux[{0}] <<0: unknown code (0)", traceId);

          return FAIL | EXIT;
        } else {
          Trace.TraceInformation("Hmux[{0}] <<?: unknown code ({1})", traceId, code);
          Skip(rs, sublen);
        }
      }
      Trace.TraceInformation("Hmux[{0}] end of file", traceId);

      // server/269q
      if (hasStatus)
        return isBusy ? BUSY | EXIT : OK | EXIT;
      else {
        Trace.TraceInformation("Hmux[{0}] unexpected end of file", traceId, code);
        return FAIL | EXIT;
      }
    }

    private int ReadHmuxLength(BufferedStream stream)
    {
      int length = (stream.ReadByte() << 8) + stream.ReadByte();
      return length;
    }

    private String ReadHmuxString(BufferedStream stream, int length)
    {
      if (length == 0)
        return "";

      byte[] data = new byte[length];
      stream.Read(data, 0, length);

      return System.Text.Encoding.ASCII.GetString(data);
    }

    private void RelayResponseData(BufferedStream hmuxIn, Stream response, int length)
    {
      if (length <= 0)
        return;

      byte[] data = new byte[length];
      while (length > 0) {
        int len = hmuxIn.Read(data, 0, length);
        response.Write(data, 0, len);
        length -= len;
      }
    }

    private void RelayResponseHeader(HttpResponse response, String name, String value)
    {
      if ("Cache-Control".Equals(name)) {
        String[] directives = value.Split(',', '=', ' ');
        for (int i = 0; i < directives.Length; i++) {
          String directive = directives[i];
          if ("no-cache".Equals(directive, StringComparison.OrdinalIgnoreCase)) {
            response.Cache.SetCacheability(HttpCacheability.NoCache);
          } else if ("public".Equals(directive, StringComparison.OrdinalIgnoreCase)) {
            response.Cache.SetCacheability(HttpCacheability.Public);
          } else if ("private".Equals(directive, StringComparison.OrdinalIgnoreCase)) {
            response.Cache.SetCacheability(HttpCacheability.Private);
          } else if ("must-revalidate".Equals(directive, StringComparison.OrdinalIgnoreCase)) {
            response.Cache.SetRevalidation(HttpCacheRevalidation.AllCaches);
          } else if ("proxy-revalidate".Equals(directive, StringComparison.OrdinalIgnoreCase)) {
            response.Cache.SetRevalidation(HttpCacheRevalidation.ProxyCaches);
          } else if ("max-age".Equals(directive, StringComparison.OrdinalIgnoreCase)) {
            response.Cache.SetMaxAge(TimeSpan.FromSeconds(int.Parse(directives[++i])));
          } else if ("s-maxage".Equals(directive, StringComparison.OrdinalIgnoreCase)) {
            response.Cache.SetProxyMaxAge(TimeSpan.FromSeconds(int.Parse(directives[++i])));
          } else if ("post-check".Equals(directive, StringComparison.OrdinalIgnoreCase)) {
            response.Cache.AppendCacheExtension("post-check=" + directives[++i]);
          } else if ("pre-check".Equals(directive, StringComparison.OrdinalIgnoreCase)) {
            response.Cache.AppendCacheExtension("pre-check=" + directives[++i]);
          }
        }
      } else if ("Content-Type".Equals(name)) {
        response.ContentType = value;
        int charsetIdx = value.IndexOf("charset");
        if (charsetIdx > -1) {
          String charset = null;
          int start = -1;
          int end = value.Length;
          for (int i = charsetIdx + 7; i < value.Length; i++) {
            char c = value[i];
            switch (c) {
              case '=': {
                  start = i;
                  break;
                }
              case ';':
              case ' ': {
                  end = i;
                  break;
                }
            }
          }
          if (start > -1 && end > start) {
            charset = value.Substring(start + 1, end - start - 1);
            response.Charset = charset;
          }
        }
      } else {
        response.Headers.Add(name, value);
      }
    }

    private void Skip(BufferedStream stream, int length)
    {
      byte[] junk = new byte[length];
      while (length > 0) {
        int len = stream.Read(junk, 0, length);
        length -= len;
      }
    }

    private void WriteHmuxLength(BufferedStream stream, int length)
    {
      stream.WriteByte((byte)(length >> 8));
      stream.WriteByte((byte)length);
    }

    private void WriteRequestString(BufferedStream stream, int code, String value, String traceId)
    {
      stream.WriteByte((byte)code);
      if (value == null) {
        WriteHmuxLength(stream, 0);
      } else {
        byte[] bytes = System.Text.Encoding.ASCII.GetBytes(value.ToCharArray());
        WriteHmuxLength(stream, bytes.Length);
        stream.Write(bytes, 0, bytes.Length);
      }
    }

    private void WriteRequestHeader(BufferedStream stream, String name, String value, String traceId)
    {
      Trace.TraceInformation("Hmux[{0}] >>H:{1}", traceId, name);
      WriteRequestString(stream, HmuxConnection.HMUX_HEADER, name, traceId);
      Trace.TraceInformation("Hmux[{0}] >>S:{1}", traceId, value);
      WriteRequestString(stream, HmuxConnection.HMUX_STRING, value, traceId);
    }

    private void WriteRequestData(BufferedStream stream, int code, byte[] data, int length, String traceId)
    {
      stream.WriteByte(HmuxConnection.HMUX_DATA);
      WriteHmuxLength(stream, length);
      stream.Write(data, 0, length);
    }

    private void WriteSSLCertificate(BufferedStream stream, byte[] cert, String traceId)
    {
      Trace.TraceInformation("Hmux[{0}] >>t:certificate({1})", traceId, cert.Length);
      stream.WriteByte(HmuxConnection.CSE_CLIENT_CERT);
      WriteHmuxLength(stream, cert.Length);
      stream.Write(cert, 0, cert.Length);
    }

    private void SendNotAvailable(HttpResponse response)
    {
      response.StatusCode = HTTP_STATUS_SERVICE_UNAVAIL;
      response.Output.WriteLine(@"<html>
<head><title>503 Service Temporarily Unavailable</title></head>
<body>
<h1>503 Service Temporarily Unavailable</h1>
<p /><hr />
<small>
Resin
</small>
</body></html>
");
    }

    private void DoCauchoStatus(HttpContext context)
    {
      TextWriter writer = context.Response.Output;
      writer.WriteLine("<html><title>Status : Caucho Servlet Engine</title>");
      if (!_isCauchoStatusEnabled) {
        writer.WriteLine("<body><h2>Caucho Status is disabled in configuration<h2>");
      } else {

        writer.WriteLine("<body bgcolor='white'> <h1>Status : Caucho Servlet Engine</h1>");

        writer.WriteLine("<h2>Backend Servers</h2>");
        writer.WriteLine("<center><table border='2' width='80%'>");
        writer.WriteLine("<tr><th width='30%'>Host</th><th>Active</th><th>Pooled</th></tr>");
        Server[] servers = _loadBalancer.GetServers();
        foreach (Server server in servers) {
          String bgcolor = STATUS_OK_CLR;
          String status = "(Ok)";
          if (!server.IsActive()) {
            bgcolor = STATUS_BAD_CLR;
            status = "(Down)";
          }
          writer.WriteLine("<tr><td bgcolor='{0}' width='30%'>{1} {2}</td><td>{3}</td><td>{4}</td></tr>",
            bgcolor,
            server.GetName(),
            status,
            server.IsActive(),
            server.GetPooledCount());
        }
        writer.WriteLine("</table></center>");

        writer.WriteLine("<h2>Configuration</h2>");
        writer.WriteLine("<center><table border='2' width='80%'>");
        writer.WriteLine("<tr><th width='30%'>Parameter</th><th>Value</th></tr>");

        writer.WriteLine("<tr><td width='30%'>{0}</td><td>{1}</td></tr>", "session-cookie", _sessionCookieName);
        writer.WriteLine("<tr><td width='30%'>{0}</td><td>{1}</td></tr>", "ssl-session-cookie", _sslSessionCookieName);
        writer.WriteLine("<tr><td width='30%'>{0}</td><td>{1}</td></tr>", "sticky-sessions", _isStickySessions);
        writer.WriteLine("<tr><td width='30%'>{0}</td><td>{1}</td></tr>", "session-url-prefix", _sessionUrlPrefix);
        writer.WriteLine("<tr><td width='30%'>{0}</td><td>{1}</td></tr>", "load-balance-connect-timeout", FormatTime(_loadBalancer.GetLoadBalanceConnectTimeout()));
        writer.WriteLine("<tr><td width='30%'>{0}</td><td>{1}</td></tr>", "load-balance-idle-time", FormatTime(_loadBalancer.GetLoadBalanceIdleTime()));
        writer.WriteLine("<tr><td width='30%'>{0}</td><td>{1}</td></tr>", "load-balance-recover-time", FormatTime(_loadBalancer.GetLoadBalanceRecoverTime()));
        writer.WriteLine("<tr><td width='30%'>{0}</td><td>{1}</td></tr>", "load-balance-socket-timeout", FormatTime(_loadBalancer.GetLoadBalanceSocketTimeout()));
        writer.WriteLine("<tr><td width='30%'>{0}</td><td>{1}</td></tr>", "keepalive-timeout", FormatTime(_loadBalancer.GetLoadBalanceKeepAliveTimeout()));
        writer.WriteLine("<tr><td width='30%'>{0}</td><td>{1}</td></tr>", "socket-timeout", FormatTime(_loadBalancer.GetSocketTimeout()));

        writer.WriteLine("</table></center>");
      }
      writer.WriteLine("</body></html>");
    }

    private String FormatTime(int time)
    {
      int milliseconds = time % 1000;
      if (milliseconds == 0) {
        return time / 1000 + " sec.";
      } else {
        return time + " ms.";
      }
    }

    private void DoTestBasic(HttpContext context)
    {
      Introspect(context);
    }

    private void DoTestChunked(HttpContext context)
    {
      for (int i = 0; i < 10; i++) {
        context.Response.Write("chunk:" + i);
        context.Response.Flush();
      }
    }

    private void DoTestSSL(HttpContext context)
    {
      HttpClientCertificate certificate = context.Request.ClientCertificate;
      context.Response.Output.WriteLine("issuer: " + certificate.Issuer);
      context.Response.Output.WriteLine("server-issuer: " + certificate.ServerIssuer);
      context.Response.Output.WriteLine("server-subject" + certificate.ServerSubject);
      context.Response.Output.WriteLine("valid-from: " + certificate.ValidFrom);
      context.Response.Output.WriteLine("valid-until: " + certificate.ValidUntil);
      foreach (String key in certificate.AllKeys) {
        context.Response.Output.Write(key + ": ");
        if (certificate.GetValues(key) != null)
          foreach (String value in certificate.GetValues(key))
            context.Response.Output.Write(value + ", ");
        context.Response.Output.WriteLine();
      }
      context.Response.Output.WriteLine("is-valid: " + certificate.IsValid);
      context.Response.Output.WriteLine("is-present: " + certificate.IsPresent);
      context.Response.Output.WriteLine("cert-cookie: " + certificate.Cookie);
      context.Response.Output.WriteLine("binary-cert: " + certificate.Certificate.Length);
      context.Response.Output.WriteLine("cert-encoding: " + certificate.CertEncoding);

      Introspect(context);
    }

    private void Introspect(HttpContext context)
    {
      context.Response.Output.WriteLine("url: " + context.Request.Url);
      context.Response.Output.WriteLine("raw-url: " + context.Request.RawUrl);
      context.Response.Output.WriteLine("path: " + context.Request.Path);
      context.Response.Output.WriteLine("path-info: " + context.Request.PathInfo);
      context.Response.Output.WriteLine("file-path: " + context.Request.FilePath);
    }

    public void DoConfigurationError(HttpContext context)
    {
      context.Response.StatusDescription = "500 Configuration Error";
      TextWriter output = context.Response.Output;
      output.WriteLine(@"<html>
<head><title>Resin IIS Plugin Configuration Error</title></head>
<body>Resin IIS Plugin Configuration Error");
      output.WriteLine(_e.Message);
      output.WriteLine("</body></html>");
      output.Flush();

    }
  }
}
