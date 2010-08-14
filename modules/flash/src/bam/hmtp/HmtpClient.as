/*
 * Copyright (c) 2001-2009 Caucho Technology, Inc.  All rights reserved.
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

package bam.hmtp
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

  import bam.*;
  import com.caucho.xmpp.ping.*;

  import mx.core.Application;
  import mx.events.FlexEvent;
  import mx.utils.URLUtil;

  public class HmtpClient extends ActorClient
  {
    private var _url:URL;
    private var _destination:String;
    /*
    private var _scheme:String;
    private var _host:String;
    private var _port:int;
    private var _path:String;
    */

    private var _policyPort:int = -1;
    private var _policyUrl:String = null;
    private var _socket:Socket = new Socket();

    private var _readHTTPHeader:Boolean = false;
    private var _readHTTPStatus:Boolean = false;
    private var _headerHistory:Array = new Array(4);
    private var _httpStatus:String = "";

    private var _actorStream:ActorStream;
    private var _fromLinkStream:ClientFromLinkStream;

    private var _user:String;
    private var _password:String;
    private var _onLoginResultCallback:Function;
    private var _onLoginErrorCallback:Function;

    /**
     * Constructor.
     *
     * @param destination The URL of the destination service.
     *
     */
    public function HmtpClient(destination:String):void
    {
      _destination = destination;

      if (Application.application.url == null) {
        Application.application.addEventListener(FlexEvent.CREATION_COMPLETE,
                                                 initUrl);
      }
      else {
        initUrl();
      }

      _actorStream = new ClientActorStream(this);
    }

    public function get actorStream():ActorStream
    {
      return _actorStream;
    }

    public function set actorStream(stream:ActorStream):void
    {
      _actorStream = actorStream;

      if (_fromLinkStream != null)
        _fromLinkStream.toClientStream = actorStream;
    }

    public function get brokerStream():ActorStream
    {
      return _toLinkStream;
    }

    protected function initUrl():void
    {
      _url = 
        new URL(URLUtil.getFullURL(Application.application.url, _destination));
    }

    public function get host():String
    {
      return _url.host;
    }

    public function get port():int
    {
      return _url.port;
    }

    public function connect(user:String, password:String,
                            onLoginResultCallback:Function, 
                            onLoginErrorCallback:Function):void
    {
      connectImpl();

      _user = user;
      _password = password;
      _onLoginResultCallback = onLoginResultCallback;
      _onLoginErrorCallback = onLoginErrorCallback;
    }

    protected function connectImpl():void
    {
      if (_policyPort != -1) {
        var policy:String = "xmlsocket://" + _url.host + ":" + _policyPort;

        Security.loadPolicyFile(policy);
      }
      else if (_policyUrl != null) {
        Security.loadPolicyFile(_policyUrl);
      }
      else {
        if (_url.port == 80) {
          Security.loadPolicyFile(_url.protocol + "://" + _url.host + 
                                  "/crossdomain.xml");
        }
        else {
          Security.loadPolicyFile(_url.protocol + "://" + 
                                  _url.host + ":" + _url.port + 
                                  "/crossdomain.xml");
        }
      }

      trace("Connecting to " + _url.host + " at " + _url.port);

      _socket = new Socket();
      _socket.addEventListener(Event.CONNECT, handleConnect);
      _socket.connect(_url.host, _url.port);

      _toLinkStream = new ClientToLinkStream(_socket);
      _fromLinkStream = new ClientFromLinkStream(this, _socket);
    }
    
    private function handleConnect(event:Event):void
    {
      _socket.writeUTFBytes("POST " + _url.path + " HTTP/1.1\r\n");
      _socket.writeUTFBytes("Host: " + _url.host + ":" + _url.port + "\r\n");
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

              loginImpl(_user, _password);
            }

            break;
          }
        }
        while (_socket.bytesAvailable > 0);
      }

      // by here, we're in the data stream

      while (_fromLinkStream.readPacket()) {
      } 
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

    protected function loginImpl(uid:String, password:String):void
    {
      querySet(null, new AuthQuery(uid, password), onLoginResult, onLoginError);
    }

    private function onLoginResult(id:Number, to:String, from:String, 
                                   value:Object):void
    {
      var result:AuthResult = AuthResult(value);

      _jid = result.jid;

      _onLoginResultCallback(_jid);
    }

    private function onLoginError(to:String, from:String,
                                  value:Object, error:ActorErrorMessage):void
    {
      _onLoginErrorCallback(error);
    }
  }
}

