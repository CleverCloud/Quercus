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

package hessian.client
{
  import flash.events.Event;
  import flash.events.IOErrorEvent;
  import flash.net.URLRequest;
  import flash.net.URLStream;
  import flash.utils.ByteArray;
  import flash.utils.Dictionary;
  import flash.utils.describeType;

  import hessian.io.Hessian2Input;
  import hessian.io.Hessian2Output;
  import hessian.io.HessianServiceError;

  import mx.core.mx_internal;

  import mx.rpc.AbstractOperation;
  import mx.rpc.AbstractService;
  import mx.rpc.AsyncToken;
  import mx.rpc.Fault;
  import mx.rpc.events.FaultEvent;
  import mx.rpc.events.ResultEvent;
  import mx.rpc.events.InvokeEvent;

  use namespace mx_internal;

  /**
   * The HessianOperation class is an AbstractOperation used exclusively
   * by HessianServices.
   *
   * @see hessian.client.HessianService
   */
  public class HessianOperation extends AbstractOperation
  {
    protected var _returnType:Class;
    protected var _tokens:Dictionary = new Dictionary();
    protected var _input:Hessian2Input = new Hessian2Input();
    protected var _output:Hessian2Output = new Hessian2Output();

    /** @private */
    public function HessianOperation(service:HessianService, 
                                     name:String, returnType:Class = null)
    {
      super(service, name);

      _returnType = returnType;
      _input.autoAlias = service.autoAlias;
      _input.addHessianTypeName = service.addHessianTypeName;
    }

    /** 
     * Invokes the operation on the remote service.  The return value is
     * an AsyncToken which can be used to retrieve the result of the 
     * invocation.  The lastResult property may also be used.
     *
     * @param args The arguments to be sent.
     *
     * @return The AsyncToken that may be used to retrieve the result of
     * the invocation.
     */
    public override function send(...args):AsyncToken
    {
      var data:ByteArray = new ByteArray();
      var callArguments:Array = args;

      if (callArguments == null || callArguments.length == 0) {
        callArguments = arguments as Array;
      }

      _output.init(data);
      _output.call(name, callArguments);
      data.position = 0;

      var request:URLRequest = new URLRequest();
      request.data = data;
      request.url = service.destination;
      request.method = "POST";
      request.contentType = "binary/octet-stream";

      var msg:HessianMessage = new HessianMessage(service.destination);
      
      // XXX This seems to be necessary to avoid a Fault
      msg.headers = new Object();
      msg.headers.DSMessageStore = new Object();

      mx_internal::asyncRequest.destination = service.destination;
      var token:AsyncToken = new AsyncToken(msg);
      var stream:URLStream = new URLStream();

      _tokens[stream] = token;

      clearResult(true);
      stream.addEventListener(Event.COMPLETE, handleComplete);
      stream.addEventListener(IOErrorEvent.IO_ERROR, handleIOError);
      stream.load(request);

      var invoke:Event = new InvokeEvent("Hessian", false, false, token, msg);
      service.dispatchEvent(invoke);

      return token;
    }

    /** @private */
    public function handleIOError(event:IOErrorEvent):void
    {
      dispatchEvent(event);
      service.dispatchEvent(event);
    }

    /** @private */
    public function handleComplete(event:Event):void
    {
      var stream:URLStream = event.target as URLStream;
      var token:AsyncToken = _tokens[stream] as AsyncToken;

      if (token == null) {
        trace("Unknown stream completed: " + stream);
        return;
      }

      delete _tokens[stream];

      var ret:Object = null;
      var event:Event = null;
      var fault:Fault = null;

      try {
        var ch:int = stream.readByte();

        if (ch != 'H'.charCodeAt()) 
          throw new Error("expected hessian reply at " + codeName(ch));

        var major:int = stream.readByte();
        var minor:int = stream.readByte();

        // XXX do something with version

        _input.init(stream);
        ret = _input.readReply(_returnType);

        // this event is to trigger changes to those bound to lastResult
        event = new ResultEvent(BINDING_RESULT, 
                                /*bubbles=*/false, /*cancelable=*/false, 
                                ret, token, token.message);

        token.applyResult(ResultEvent(event));
        dispatchEvent(event);

        // this is for those explicitly waiting for a result event
        dispatchEvent(ResultEvent.createEvent(ret, token, token.message));
      }
      catch (e:HessianServiceError) {
        fault = new Fault(e.code, e.message, String(e.detail));
        fault.rootCause = e;

        event = new FaultEvent(BINDING_RESULT, 
                               /*bubbles=*/false, /*cancelable=*/false, 
                               fault, token, token.message);

        token.applyFault(FaultEvent(event));

        // this is for those explicitly waiting for a fault event
        dispatchEvent(FaultEvent.createEvent(fault, token, token.message));
      }
      catch (e:Error) {
        fault = new Fault("", e.message, "");
        fault.rootCause = e;

        event = new FaultEvent(BINDING_RESULT, 
                               /*bubbles=*/false, /*cancelable=*/false, 
                               fault, token, token.message);

        token.applyFault(FaultEvent(event));

        // this is for those explicitly waiting for a fault event
        dispatchEvent(FaultEvent.createEvent(fault, token, token.message));
      }
      finally {
        stream.close();
      }

      mx_internal::_result = ret;
    }

    /**
     * The return type to which results will be cast.  Optional.
     */
    public function get returnType():Class
    {
      return _returnType;
    }

    public function set returnType(returnType:Class):void
    {
      _returnType = returnType;
    }

    /** @private */
    protected function codeName(ch:int):String
    {
      if (ch < 0) 
        return "end of file";
      else
        return "0x" + (ch & 0xff).toString(16) + 
               " (" + String.fromCharCode(ch) + ")";
    }

    public override function toString():String
    {
      return "HessianOperation[" + name + "]";
    }
  }
}
