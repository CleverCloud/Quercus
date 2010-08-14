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

package bam.hmtp
{
  import bam.ActorError;
  import bam.ActorErrorMessage;
  import bam.ActorStream;
	import flash.errors.IllegalOperationError;
	import flash.utils.IDataInput;
	import hessian.io.Hessian2StreamingInput;
	import hessian.util.ByteUtils;

  public class ClientFromLinkStream {
    private var _di:IDataInput;
    private var _input:Hessian2StreamingInput;
    private var _toClientStream:ActorStream;
    private var _toLinkStream:ActorStream;

    private var _inPacket:Boolean = false;
    private var _type:int;
    private var _to:String;
    private var _from:String;
    private var _value:Object;
    private var _error:ActorErrorMessage;
    private var _id:Number;

    private var _readType:Boolean;
    private var _readTo:Boolean;
    private var _readFrom:Boolean;
    private var _readValue:Boolean;
    private var _readError:Boolean;
    private var _readId:Boolean;

    public function ClientFromLinkStream(client:HmtpClient, di:IDataInput):void
    {
      _input = new Hessian2StreamingInput(di);

      _toClientStream = client.actorStream;
      _toLinkStream = client.brokerStream;
    }

    public function set toClientStream(stream:ActorStream):void
    {
      _toClientStream = stream;
    }

    private function reset():void
    {
      _readType = false;
      _readTo = false;
      _readFrom = false;
      _readValue = false;
      _readError = false;
      _readId = false;
    }

    /**
      * reads the next packet.  returns true only when a complete packet
      * has been received.
      * 
      * this behavior differs from the Java implementation.
      **/
    public function readPacket():Boolean
    {
      if (_input == null)
        return false;

      if (! readType())
        return false;

      if (! readTo())
        return false;

      if (! readFrom())
        return false;

      switch (_type) {
        case HmtpPacketType.MESSAGE:
          {
            if (! readValue())
              return false;

            trace(this + " message " + _value + 
                  " {to:" + _to +", _from:" + _from + "}" );

            reset();

            _toClientStream.message(_to, _from, _value);

            break;
          }

        case HmtpPacketType.MESSAGE_ERROR:
          {
            if (! readValue())
              return false;

            if (! readError())
              return false;

            trace(this + " messageError " + _value + " error:" + _error +
                  " {to:" + _to +", _from:" + _from + "}" );

            reset();

            _toClientStream.messageError(_to, _from, _value, _error);

            break;
          }

        case HmtpPacketType.QUERY_GET:
          {
            if (! readId())
              return false;

            if (! readValue())
              return false;

            trace(this + " queryGet " + _value + 
                  " {id:" + _id + ", to:" + _to +", _from:" + _from + "}" );

            reset();

            try {
              _toClientStream.queryGet(_id, _to, _from, _value);
            }
            catch (e:ActorError) {
              _toLinkStream.queryError(_id, _from, _to, _value, 
                                       e.actorErrorMessage);
            }

            break;
          }

        case HmtpPacketType.QUERY_SET:
          {
            if (! readId())
              return false;

            if (! readValue())
              return false;

            trace(this + " querySet " + _value + 
                  " {id:" + _id + ", to:" + _to +", _from:" + _from + "}" );

            reset();

            try {
              _toClientStream.querySet(_id, _to, _from, _value);
            }
            catch (e:ActorError) {
              _toLinkStream.queryError(_id, _from, _to, _value, 
                                       e.actorErrorMessage);
            }

            break;
          }

        case HmtpPacketType.QUERY_RESULT:
          {
            if (! readId())
              return false;

            if (! readValue())
              return false;

            trace(this + " queryResult " + _value + 
                  " {id:" + _id + ", to:" + _to +", _from:" + _from + "}" );

            reset();

            _toClientStream.queryResult(_id, _to, _from, _value);

            break;
          }

        case HmtpPacketType.QUERY_ERROR:
          {
            if (! readId())
              return false;

            if (! readValue())
              return false;

            if (! readError())
              return false;

            trace(this + " queryError " + _value + " error:" + _error +
                  " {id:" + _id + ", to:" + _to +", _from:" + _from + "}" );

            reset();

            _toClientStream.queryError(_id, _to, _from, _value, _error);

            break;
          }

        case HmtpPacketType.PRESENCE:
          {
            if (! readValue())
              return false;

            trace(this + " presence " + _value + 
                  " {to:" + _to +", _from:" + _from + "}" );

            reset();

            _toClientStream.presence(_to, _from, _value);

            break;
          }

        case HmtpPacketType.PRESENCE_UNAVAILABLE:
          {
            if (! readValue())
              return false;

            trace(this + " presenceUnavailable " + _value + 
                  " {to:" + _to +", _from:" + _from + "}" );

            reset();

            _toClientStream.presenceUnavailable(_to, _from, _value);

            break;
          }

        case HmtpPacketType.PRESENCE_PROBE:
          {
            if (! readValue())
              return false;

            trace(this + " presenceProbe " + _value + 
                  " {to:" + _to +", _from:" + _from + "}" );

            reset();

            _toClientStream.presenceProbe(_to, _from, _value);

            break;
          }

        case HmtpPacketType.PRESENCE_SUBSCRIBE:
          {
            if (! readValue())
              return false;

            trace(this + " presenceSubscribe " + _value + 
                  " {to:" + _to +", _from:" + _from + "}" );

            reset();

            _toClientStream.presenceSubscribe(_to, _from, _value);

            break;
          }

        case HmtpPacketType.PRESENCE_SUBSCRIBED:
          {
            if (! readValue())
              return false;

            trace(this + " presenceSubscribed " + _value + 
                  " {to:" + _to +", _from:" + _from + "}" );

            reset();

            _toClientStream.presenceSubscribed(_to, _from, _value);

            break;
          }

        case HmtpPacketType.PRESENCE_UNSUBSCRIBE:
          {
            if (! readValue())
              return false;

            trace(this + " presenceUnsubscribe " + _value + 
                  " {to:" + _to +", _from:" + _from + "}" );

            reset();

            _toClientStream.presenceUnsubscribe(_to, _from, _value);

            break;
          }

        case HmtpPacketType.PRESENCE_UNSUBSCRIBED:
          {
            if (! readValue())
              return false;

            trace(this + " presenceUnsubscribed " + _value + 
                  " {to:" + _to +", _from:" + _from + "}" );

            reset();

            _toClientStream.presenceUnsubscribed(_to, _from, _value);

            break;
          }

         case HmtpPacketType.PRESENCE_ERROR:
          {
            if (! readValue())
              return false;

            if (! readError())
              return false;

            trace(this + " presenceError " + _value + " error:" + _error + 
                  " {to:" + _to +", _from:" + _from + "}" );

            reset();

            _toClientStream.presenceError(_to, _from, _value, _error);

            break;
          }

         default:
          throw new IllegalOperationError("ERROR: unknown HMTP type " + _type);
      }

      return true;
    }

    private function readType():Boolean
    {
      if (_readType)
        return true;

      if (_input.hasMoreObjects()) {
        _type = _input.readInt();
        _readType = true;

        return true;
      }
      else {
        return false;
      }
    }

    private function readTo():Boolean
    {
      if (_readTo)
        return true;

      if (_input.hasMoreObjects()) {
        _to = _input.readString();
        _readTo = true;

        return true;
      }
      else {
        return false;
      }
    }

    private function readFrom():Boolean
    {
      if (_readFrom)
        return true;

      if (_input.hasMoreObjects()) {
        _from = _input.readString();
        _readFrom = true;

        return true;
      }
      else {
        return false;
      }
    }

    private function readId():Boolean
    {
      if (_readId)
        return true;

      if (_input.hasMoreObjects()) {
        _id = _input.readLong();
        _readId = true;

        return true;
      }
      else {
        return false;
      }
    }

    private function readValue():Boolean
    {
      if (_readValue)
        return true;

      if (_input.hasMoreObjects()) {
        _value = _input.readObject();
        _readValue = true;

        return true;
      }
      else {
        return false;
      }
    }

    private function readError():Boolean
    {
      if (_readError)
        return true;

      if (_input.hasMoreObjects()) {
        _error = _input.readObject() as ActorErrorMessage;
        _readError = true;

        return true;
      }
      else {
        return false;
      }
    }

    public function close():void
    {
      _input = null;
      _di = null;
    }
  }
}
