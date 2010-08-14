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
 * @author Emil Ong, Scott Ferguson
 */

package bam.hmtp
{
	import flash.errors.IOError;
	import flash.utils.IDataOutput;
	import hessian.io.Hessian2Output;
	import hessian.util.ByteUtils;

  import bam.*;

  public class ClientToLinkStream implements ActorStream {
    private var _jid:String;
    private var _os:IDataOutput;
    private var _out:Hessian2Output;

    public function ClientToLinkStream(os:IDataOutput):void
    {
      _os = os;
      _out = new Hessian2Output(os);
    }

    public function set jid(id:String):void
    {
      _jid = id;
    }

    public function get jid():String
    {
      return _jid;
    }

    public function message(to:String, from:String, value:Object):void
    {
      if (_out != null) {
        trace(this + " message " + value + 
              " {to:" + to +", from:" + from + "}" );
      }

      try {
        _out.startPacket();
        _out.writeInt(HmtpPacketType.MESSAGE);
        _out.writeString(to);
        _out.writeString(from);
        _out.writeObject(value);
        _out.endPacket();
        _out.flush();
      }
      catch (e:IOError) {
        throw new HmtpProtocolError(e);
      }
    }    
    
    public function messageError(to:String, from:String, 
                                 value:Object, error:ActorErrorMessage):void
    {
      if (_out != null) {
        trace(this + " messageError " + value + " error:" + error +
              " {to:" + to +", from:" + from + "}" );
      }

      try {
        _out.startPacket();
        _out.writeInt(HmtpPacketType.MESSAGE_ERROR);
        _out.writeString(to);
        _out.writeString(from);
        _out.writeObject(value);
        _out.writeObject(error);
        _out.endPacket();
        _out.flush();
      }
      catch (e:IOError) {
        throw new HmtpProtocolError(e);
      }
    }

    public function queryGet(id:Number, to:String, from:String, 
                             value:Object):void
    {
      if (_out != null) {
        trace(this + " queryGet " + value + 
              " {id:" + id + ", to:" + to +", from:" + from + "}" );
      }

      try {
        _out.startPacket();
        _out.writeInt(HmtpPacketType.QUERY_GET);
        _out.writeString(to);
        _out.writeString(from);
        _out.writeLong(id);
        _out.writeObject(value);
        _out.endPacket();
        _out.flush();
      }
      catch (e:IOError) {
        throw new HmtpProtocolError(e);
      }
    }    

    public function querySet(id:Number, to:String, from:String, 
                             value:Object):void
    {
      if (_out != null) {
        trace(this + " querySet " + value + 
              " {id:" + id + ", to:" + to +", from:" + from + "}" );
      }

      try {
        _out.startPacket();
        _out.writeInt(HmtpPacketType.QUERY_SET);
        _out.writeString(to);
        _out.writeString(from);
        _out.writeLong(id);
        _out.writeObject(value);
        _out.endPacket();
        _out.flush();
      }
      catch (e:IOError) {
        throw new HmtpProtocolError(e);
      }
    }    

    public function queryResult(id:Number, to:String, from:String, 
                                value:Object):void
    {
      if (_out != null) {
        trace(this + " queryResult " + value + 
              " {id:" + id + ", to:" + to +", from:" + from + "}" );
      }

      try {
        _out.startPacket();
        _out.writeInt(HmtpPacketType.QUERY_RESULT);
        _out.writeString(to);
        _out.writeString(from);
        _out.writeLong(id);
        _out.writeObject(value);
        _out.endPacket();
        _out.flush();
      }
      catch (e:IOError) {
        throw new HmtpProtocolError(e);
      }
    }    

    public function queryError(id:Number, to:String, from:String, 
                               value:Object, error:ActorErrorMessage):void
    {
      if (_out != null) {
        trace(this + " queryError " + value + " error:" + error +
              " {id:" + id + ", to:" + to +", from:" + from + "}" );
      }

      try {
        _out.startPacket();
        _out.writeInt(HmtpPacketType.QUERY_ERROR);
        _out.writeString(to);
        _out.writeString(from);
        _out.writeLong(id);
        _out.writeObject(value);
        _out.writeObject(error);
        _out.endPacket();
        _out.flush();
      }
      catch (e:IOError) {
        throw new HmtpProtocolError(e);
      }
    }    

    public function presence(to:String, from:String, value:Object):void
    {
      if (_out != null) {
        trace(this + " presence " + value + 
              " {to:" + to +", from:" + from + "}" );
      }

      try {
        _out.startPacket();
        _out.writeInt(HmtpPacketType.PRESENCE);
        _out.writeString(to);
        _out.writeString(from);
        _out.writeObject(value);
        _out.endPacket();
        _out.flush();
      }
      catch (e:IOError) {
        throw new HmtpProtocolError(e);
      }
    }    
    

    public function presenceUnavailable(to:String, from:String, 
                                        value:Object):void
    {
      if (_out != null) {
        trace(this + " presenceUnavailable " + value + 
              " {to:" + to +", from:" + from + "}" );
      }

      try {
        _out.startPacket();
        _out.writeInt(HmtpPacketType.PRESENCE_UNAVAILABLE);
        _out.writeString(to);
        _out.writeString(from);
        _out.writeObject(value);
        _out.endPacket();
        _out.flush();
      }
      catch (e:IOError) {
        throw new HmtpProtocolError(e);
      }
    }    
    

    public function presenceProbe(to:String, from:String, value:Object):void
    {
      if (_out != null) {
        trace(this + " presenceProbe " + value + 
              " {to:" + to +", from:" + from + "}" );
      }

      try {
        _out.startPacket();
        _out.writeInt(HmtpPacketType.PRESENCE_PROBE);
        _out.writeString(to);
        _out.writeString(from);
        _out.writeObject(value);
        _out.endPacket();
        _out.flush();
      }
      catch (e:IOError) {
        throw new HmtpProtocolError(e);
      }
    }    
    

    public function presenceSubscribe(to:String, from:String, value:Object):void
    {
      if (_out != null) {
        trace(this + " presenceSubscribe " + value + 
              " {to:" + to +", from:" + from + "}" );
      }

      try {
        _out.startPacket();
        _out.writeInt(HmtpPacketType.PRESENCE_SUBSCRIBE);
        _out.writeString(to);
        _out.writeString(from);
        _out.writeObject(value);
        _out.endPacket();
        _out.flush();
      }
      catch (e:IOError) {
        throw new HmtpProtocolError(e);
      }
    }    
    

    public function presenceSubscribed(to:String, from:String,  
                                       value:Object):void
    {
      if (_out != null) {
        trace(this + " presenceSubscribed " + value + 
              " {to:" + to +", from:" + from + "}" );
      }

      try {
        _out.startPacket();
        _out.writeInt(HmtpPacketType.PRESENCE_SUBSCRIBED);
        _out.writeString(to);
        _out.writeString(from);
        _out.writeObject(value);
        _out.endPacket();
        _out.flush();
      }
      catch (e:IOError) {
        throw new HmtpProtocolError(e);
      }
    }    

    public function presenceUnsubscribe(to:String, from:String, 
                                        value:Object):void
    {
      if (_out != null) {
        trace(this + " presenceUnsubscribe " + value + 
              " {to:" + to +", from:" + from + "}" );
      }

      try {
        _out.startPacket();
        _out.writeInt(HmtpPacketType.PRESENCE_UNSUBSCRIBE);
        _out.writeString(to);
        _out.writeString(from);
        _out.writeObject(value);
        _out.endPacket();
        _out.flush();
      }
      catch (e:IOError) {
        throw new HmtpProtocolError(e);
      }
    }    
    
    public function presenceUnsubscribed(to:String, from:String, 
                                         value:Object):void
    {
      if (_out != null) {
        trace(this + " presenceUnsubscribed " + value + 
              " {to:" + to +", from:" + from + "}" );
      }

      try {
        _out.startPacket();
        _out.writeInt(HmtpPacketType.PRESENCE_UNSUBSCRIBED);
        _out.writeString(to);
        _out.writeString(from);
        _out.writeObject(value);
        _out.endPacket();
        _out.flush();
      }
      catch (e:IOError) {
        throw new HmtpProtocolError(e);
      }
    }    

    public function presenceError(to:String, from:String, 
                                  value:Object, error:ActorErrorMessage):void
    {
      if (_out != null) {
        trace(this + " presenceError " + value + " error:" + error + 
              " {to:" + to +", from:" + from + "}" );
      }

      try {
        _out.startPacket();
        _out.writeInt(HmtpPacketType.PRESENCE_ERROR);
        _out.writeString(to);
        _out.writeString(from);
        _out.writeObject(value);
        _out.writeObject(error);
        _out.endPacket();
        _out.flush();
      }
      catch (e:IOError) {
        throw new HmtpProtocolError(e);
      }
    }    

    public function close():void
    {
      _out.close();
      _out = null;
    }
  }

}
