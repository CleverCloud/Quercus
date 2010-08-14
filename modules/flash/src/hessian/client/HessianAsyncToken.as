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

package hessian.client
{
  import flash.events.Event;
  import flash.events.EventDispatcher;
  import flash.events.IOErrorEvent;
  import flash.net.URLRequest;
  import flash.net.URLStream;
  import flash.utils.ByteArray;
  import flash.utils.describeType;

  import hessian.events.HessianErrorEvent;
  import hessian.events.HessianResultEvent;
  import hessian.io.Hessian2Input;
  import hessian.io.HessianOutput;
  import hessian.io.HessianServiceError;

  /**
   * A token returned by HessianProxy that manages results and faults from
   * a Hessian service.
   */
  public class HessianAsyncToken extends EventDispatcher
  {
    private var _returnType:Class;
    private var _input:Hessian2Input;

    /** @private */
    public function HessianAsyncToken(request:URLRequest, 
                                      input:Hessian2Input,
                                      returnType:Class = null)
    {
      var stream:URLStream = new URLStream();
      stream.addEventListener(Event.COMPLETE, handleComplete);
      stream.addEventListener(IOErrorEvent.IO_ERROR, handleIOError);
      stream.load(request);

      _returnType = returnType;
      _input = input;
    }

    /** @private */
    public function handleIOError(event:IOErrorEvent):void
    {
      dispatchEvent(event);
    }

    /** @private */
    public function handleComplete(event:Event):void
    {
      var stream:URLStream = event.target as URLStream;

      var ret:Object = null;
      var event:Event = null;

      try {
        _input.init(stream);
        ret = _input.readReply(_returnType);
        event = new HessianResultEvent(this, ret);
      }
      catch (e:Error) {
        event = new HessianErrorEvent(this, e);
      }
      finally {
        stream.close();
      }

      dispatchEvent(event);
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
  }
}
