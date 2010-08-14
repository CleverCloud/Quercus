/*
 * Copyright (c) 2001-2008 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Burlap", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Emil Ong
 * 
 */

package com.caucho.hmtp
{
  import flash.events.Event;
  import flash.events.EventDispatcher;
  import flash.events.ProgressEvent;
  import flash.events.TimerEvent;
  import flash.net.URLRequest;
  import flash.net.URLStream;
  import flash.net.Socket;
  import flash.system.Security;
  import flash.utils.describeType;
  import flash.utils.Timer;

  import hessian.io.Hessian2StreamingInput;
  import hessian.util.URL;

  import com.caucho.bam.*;
  import com.caucho.xmpp.ping.*;

  public class HmtpClient extends EventDispatcher 
                          implements BamConnection 
  {
    public static const MESSAGE:String = "message";
    public static const QUERY:String = "query";
    public static const PRESENCE:String = "presence";
    public static const LOGIN:String = "login";

    private var _url:String;
    private var _scheme:String;
    private var _host:String;
    private var _port:int;
    private var _path:String;

    private var _jid:String;

    private var _policyPort:int = -1;
    private var _policyUrl:String = null;
    private var _socket:Socket = new Socket();

    private var _readHTTPHeader:Boolean = false;
    private var _readHTTPStatus:Boolean = false;
    private var _headerHistory:Array = new Array(4);
    private var _httpStatus:String = "";

    private var _stream:ClientBrokerStream;

    private var _queryId:int = 0;
    private var _outstandingQueries:Object = new Object();

    private var _messageListeners:Array = new Array();

    /**
     * Constructor.
     *
     * @param url  The URL of the destination service.
     *
     */
    public function HmtpClient(url:String):void
    {
      _url = url;
      parseURL(url);
    }

    protected function parseURL(url:String):void
    {
      var p:int = url.indexOf("://");

      if (p < 0)
        throw new ArgumentError("URL '" + url + "' is not well-formed");

      _scheme = url.substring(0, p);

      url = url.substring(p + 3);

      p = url.indexOf("/");
      if (p >= 0) {
        _path = url.substring(p);
        url = url.substring(0, p);
      }
      else {
        _path = "/";
      }

      p = url.indexOf(':');
      if (p > 0) {
        _host = url.substring(0, p);
        _port = parseInt(url.substring(p + 1));
      }
      else {
        _host = url;

        if ("https" == _scheme)
          _port = 443;
        else
          _port = 80;
      }
    }

    public function get host():String
    {
      return _host;
    }

    public function get port():int
    {
      return _port;
    }

    public function connect():void
    {
      if (_policyPort != -1) {
        var policy:String = "xmlsocket://" + _host + ":" + _policyPort;

        Security.loadPolicyFile(policy);
      }
      else if (_policyUrl != null) {
        Security.loadPolicyFile(_policyUrl);
      }
      else {
        Security.loadPolicyFile(_scheme + "://" + _host + ":" + _port + 
                                "/crossdomain.xml");
      }

      trace("host = " + _host + " port = " + _port);

      _socket = new Socket(_host, _port);
      _socket.addEventListener(Event.CONNECT, handleConnect);

      _stream = new ClientBrokerStream(_socket);
    }
    
    private function handleConnect(event:Event):void
    {
      _socket.writeUTFBytes("POST " + _path + "/hemp HTTP/1.1\r\n");
      _socket.writeUTFBytes("Host: " + _host + ":" + _port + "\r\n");
      _socket.writeUTFBytes("Upgrade: HMTP/0.9\r\n");
      _socket.writeUTFBytes("Content-Length: 0\r\n");
      _socket.writeUTFBytes("\r\n");
      _socket.addEventListener(ProgressEvent.SOCKET_DATA, handleData);
    }

    private function handleData(event:Event):void
    {
      if (_socket.bytesAvailable <= 0)
        return;

      if (! _readHTTPHeader) {
        do {
          var char:String = _socket.readUTFBytes(1);

          if (_readHTTPStatus == false) {
            _httpStatus += char;
            //trace("updated status: '" + _httpStatus + "'");

            if (_httpStatus.indexOf("\r\n") >= 0) {
              _readHTTPStatus = true;
            }
          }

          _headerHistory.push(char);
          _headerHistory.shift();

          if (_headerHistory[0] == '\r' && _headerHistory[1] == '\n' &&
              _headerHistory[2] == '\r' && _headerHistory[3] == '\n') {
            _headerHistory = null;
            _readHTTPHeader = true;

            if (_httpStatus.indexOf("HTTP/1.1 101") != 0) {
              trace("unexpected HTTP status");
            }
            else {
              dispatchEvent(new Event(Event.CONNECT));
            }

            break;
          }
        }
        while (_socket.bytesAvailable > 0);
      }

      _stream.input.read(_socket);

      while (_stream.input.hasMoreObjects()) {
        handlePacket(Packet(_stream.input.nextObject()));
      }
    }

    private function handlePacket(packet:Packet):void
    {
      var bundle:QueryCallbackBundle = null;

      trace("got packet: " + packet);

      if (packet is QueryResult) {
        var queryResult:QueryResult = QueryResult(packet);

        if (_outstandingQueries.hasOwnProperty(queryResult.id)) {
          bundle = 
            QueryCallbackBundle(_outstandingQueries[queryResult.id.toString()]);

          bundle.onResult(queryResult.to, queryResult.from, queryResult.value);
        }
        else {
          trace("Recieved unknown QueryResult: " + queryResult.id);
        }
      }
      else if (packet is QueryError) {
        var queryError:QueryError = QueryError(packet);

        if (_outstandingQueries.hasOwnProperty(queryError.id)) {
          bundle = 
            QueryCallbackBundle(_outstandingQueries[queryError.id.toString()]);

          bundle.onError(queryError.to, queryError.from,
                         queryError.value, queryError.error);
        }
        else {
          trace("Recieved unknown QueryError: " + queryError.id);
        }
      }
      else if (packet is QueryGet) {
        trace("packet is QueryGet: " + packet);
        var queryGet:QueryGet = QueryGet(packet);

        if (queryGet.value is PingQuery) {
          _stream.sendQueryResult(queryGet.id, 
                                  queryGet.from, 
                                  queryGet.to, 
                                  queryGet.value);
        }
      }
      else if (packet is Message) {
        var payload:Object = Message(packet).value;

        for (var i:int = 0; i < _messageListeners.length; i++) { 
          if (_messageListeners[i].invoke(packet.to, packet.from, payload))
            return;
        }

        dispatchEvent(packet);
      }
      else {
        dispatchEvent(packet);
      }

      /*
      else if (packet is Message) {
        if (messageHandler != null) {
          var msg:Message = Message(packet);

          messageHandler.sendMessage(msg.to, msg.from, msg.value);
        }
      }
      else if (packet is MessageError) {
        if (messageHandler != null) {
          var msg:MessageError = MessageError(packet);

          messageHandler.sendMessageError(msg.to, msg.from, 
                                          msg.value, msg.error);
        }
      }*/
    }

    public function addMessageListener(messageClass:Class, 
                                       listener:Function):void
    {
      var entry:MessageListenerMapEntry = 
        new MessageListenerMapEntry(messageClass, listener);
      _messageListeners.push(entry);
    }

    /**
     * Sets the port on which the XMLSocket server is listening to serve
     * the policy file.
     */
    public function get policyUrl():String
    {
      return _policyUrl;
    }

    public function set policyUrl(url:String):void
    {
      _policyUrl = url;
    }

    /**
     * Sets the port on which the XMLSocket server is listening to serve
     * the policy file.
     */
    public function get policyPort():int
    {
      return _policyPort;
    }

    public function set policyPort(policyPort:int):void
    {
      _policyPort = policyPort;
    }

    public function login(uid:String, password:String):void
    {
      querySet("", new AuthQuery(uid, password), onLoginResult, onLoginError);
    }

    public function get jid():String
    {
      return _jid;
    }

    public function set jid(value:String):void
    {
      _jid = value;
    }

    public function isClosed():Boolean
    {
      return _stream == null;
    }

    public function close():void
    {
      _stream.close();
      _stream = null;
    }

    public function sendMessage(to:String, value:Object):void
    {
      _stream.sendMessage(to, null, value);
    }

    public function queryGet(to:String, value:Object, 
                             onResult:Function, onError:Function):void
    {
      var queryId:int = _queryId++;

      _outstandingQueries[queryId.toString()]
        = new QueryCallbackBundle(onResult, onError);

      _stream.sendQueryGet(queryId, to, null, value);
    }

    public function querySet(to:String, value:Object,
                             onResult:Function, onError:Function):void
    {
      var queryId:int = _queryId++;

      _outstandingQueries[queryId.toString()]
        = new QueryCallbackBundle(onResult, onError);

      _stream.sendQuerySet(queryId, to, null, value);
    }

    public function get stream():BamStream
    {
      return _stream;
    }

    private function onLoginResult(to:String, from:String, value:Object):void
    {
      var result:AuthResult = AuthResult(value);

      this.jid = result.jid;
      dispatchEvent(new LoginSuccessEvent());
    }

    private function onLoginError(to:String, from:String,
                                  value:Object, error:BamError):void
    {
      dispatchEvent(new LoginFailureEvent());
    }
  }
}

class MessageListenerMapEntry {
  private var _class:Class;
  private var _listener:Function;

  public function MessageListenerMapEntry(clazz:Class, listener:Function):void
  {
    _class = clazz;
    _listener = listener;
  }

  public function invoke(to:String, from:String, payload:Object):Boolean
  {
    if (payload is _class) {
      _listener(to, from, payload);
      return true;
    }

    return false;
  }
}

class QueryCallbackBundle {
  private var _onResult:Function;
  private var _onError:Function;

  public function QueryCallbackBundle(onResult:Function,
                                      onError:Function):void
  {
    _onResult = onResult;
    _onError = onError;
  }

  public function get onResult():Function
  {
    return _onResult;
  }

  public function get onError():Function
  {
    return _onError;
  }
}
