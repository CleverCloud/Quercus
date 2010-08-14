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
 */

package bam
{
  import flash.utils.getQualifiedClassName;

  public class ClientActorStream implements ActorStream {
    protected var _client:ActorClient;

    public function ClientActorStream(client:ActorClient):void
    {
      _client = client;
    }

    public function get jid():String
    {
      return _client.jid;
    }

    public function message(to:String, from:String, value:Object):void
    {
      if (! _client.handleMessage(to, from, value))
        trace("unhandled message: " + value + 
              "{to: " + to + ", from: " + from + "}");
    }

    public function messageError(to:String, from:String, 
                                 value:Object, error:ActorErrorMessage):void
    {
      if (! _client.handleMessageError(to, from, value, error))
        trace("unhandled messageError: " + value + " error: " + error +
              "{to: " + to + ", from: " + from + "}");
    }

    public function queryGet(id:Number, to:String, from:String, 
                             value:Object):void
    {
      if (! _client.handleQueryGet(id, to, from, value)) {
        trace("unhandled queryGet: " + value + 
              "{id: " + id + ", to: " + to + ", from: " + from + "}");

        var msg:ActorErrorMessage =
          new ActorErrorMessage(ActorErrorMessage.TYPE_CANCEL,
                                ActorErrorMessage.INTERNAL_SERVER_ERROR,
                                "no handler for queryGet " + 
                                getQualifiedClassName(value));

        throw new ActorError(msg);
      }
    }

    public function querySet(id:Number, to:String, from:String, 
                             value:Object):void
    {
      if (! _client.handleQuerySet(id, to, from, value)) {
        trace("unhandled querySet: " + value + 
              "{id: " + id + ", to: " + to + ", from: " + from + "}");

        var msg:ActorErrorMessage =
          new ActorErrorMessage(ActorErrorMessage.TYPE_CANCEL,
                                ActorErrorMessage.INTERNAL_SERVER_ERROR,
                                "no handler for querySet " + 
                                getQualifiedClassName(value));

        throw new ActorError(msg);
      }
    }

    public function queryResult(id:Number, to:String, from:String, 
                                value:Object):void
    {
      if (! _client.handleQueryResult(id, to, from, value))
        trace("unhandled queryResult: " + value + 
              "{id: " + id + ", to: " + to + ", from: " + from + "}");
    }

    public function queryError(id:Number, to:String, from:String, 
                               value:Object, error:ActorErrorMessage):void
    {
      if (! _client.handleQueryError(id, to, from, value, error))
        trace("unhandled queryError: " + value + " error: " + error +
              "{id: " + id + ", to: " + to + ", from: " + from + "}");
    }

    public function presence(to:String, from:String, value:Object):void
    {
      if (! _client.handlePresence(to, from, value))
        trace("unhandled presence: " + value + 
              "{to: " + to + ", from: " + from + "}");
    }

    public function presenceUnavailable(to:String, from:String, 
                                        value:Object):void
    {
      if (! _client.handlePresenceUnavailable(to, from, value))
        trace("unhandled presenceUnavailable: " + value + 
              "{to: " + to + ", from: " + from + "}");
    }

    public function presenceProbe(to:String, from:String, value:Object):void
    {
      if (! _client.handlePresenceProbe(to, from, value))
        trace("unhandled presenceProbe: " + value + 
              "{to: " + to + ", from: " + from + "}");
    }

    public function presenceSubscribe(to:String, from:String, 
                                      value:Object):void
    {
      if (! _client.handlePresenceSubscribe(to, from, value))
        trace("unhandled presenceSubscribe: " + value + 
              "{to: " + to + ", from: " + from + "}");
    }

    public function presenceSubscribed(to:String, from:String,  
                                       value:Object):void
    {
      if (! _client.handlePresenceSubscribed(to, from, value))
        trace("unhandled presenceSubscribed: " + value + 
              "{to: " + to + ", from: " + from + "}");
    }

    public function presenceUnsubscribe(to:String, from:String, 
                                        value:Object):void
    {
      if (! _client.handlePresenceUnsubscribe(to, from, value))
        trace("unhandled presenceUnsubscribe: " + value + 
              "{to: " + to + ", from: " + from + "}");
    }

    public function presenceUnsubscribed(to:String, from:String, 
                                         value:Object):void
    {
      if (! _client.handlePresenceUnsubscribed(to, from, value))
        trace("unhandled presenceUnsubscribed: " + value + 
              "{to: " + to + ", from: " + from + "}");
    }

    public function presenceError(to:String, from:String, 
                                  value:Object, error:ActorErrorMessage):void
    {
      if (! _client.handlePresenceError(to, from, value, error))
        trace("unhandled presenceError: " + value + " error: " + error +
              "{to: " + to + ", from: " + from + "}");
    }

    public function close():void
    {
    }
  }
}

