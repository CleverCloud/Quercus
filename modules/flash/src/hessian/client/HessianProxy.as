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
  import flash.net.URLRequest;
  import flash.utils.describeType;
  import flash.utils.getDefinitionByName;
  import flash.utils.ByteArray;
  import flash.utils.Proxy;
	import flash.utils.flash_proxy;

  import hessian.io.Hessian2Input;
  import hessian.io.Hessian2Output;

  /**
   * Pure flash proxy client for Hessian.
   */
  public dynamic class HessianProxy extends Proxy
  {
    private var _api:Class;
    private var _destination:String;
    private var _returnTypes:Object = new Object();
    private var _input:Hessian2Input = new Hessian2Input();
    private var _output:Hessian2Output = new Hessian2Output();

    /**
     * Constructor.
     *
     * @param destination The URL of the destination service.
     * @param api The API associated with the service, if any.
     *
     */
    public function HessianProxy(destination:String = null, api:Class = null)
    {
      _destination = destination;
      _api = api;

      if (api != null)
        introspect();
    }

    public function set autoAlias(a:Boolean):void
    {
      _input.autoAlias = a;
    }

    public function get autoAlias():Boolean
    {
      return _input.autoAlias;
    }

    public function set addHessianTypeName(a:Boolean):void
    {
      _input.addHessianTypeName = a;
    }

    public function get addHessianTypeName():Boolean
    {
      return _input.addHessianTypeName;
    }

    /** @private */
    protected function introspect():void
    {
      var type:XML = describeType(_api);
      var factories:XMLList = type.factory;
      var methods:XMLList = type.method;

      if (methods.length() == 0 && factories.length() > 0)
        methods = factories[0].method;

      for each(var method:XML in methods) {
        try {
          _returnTypes[method.@name] = 
            getDefinitionByName(method.@returnType) as Class;
        }
        catch (e:Error) {
        }
      }
    }

		override flash_proxy function callProperty(name:*, ...args):*
		{
      var data:ByteArray = new ByteArray();
      var callArguments:Array = args;

      if (callArguments == null || callArguments.length == 0) {
        callArguments = new Array();
      }

      _output.init(data);
      _output.call(name, callArguments);
      data.position = 0;

      var request:URLRequest = new URLRequest();
      request.data = data;
      request.url = destination;
      request.method = "POST";
      request.contentType = "binary/octet-stream";

      return new HessianAsyncToken(request, _returnTypes[name]);
    }

    /**
     * The API associated with the service, if any.
     */
    public function get api():Class
    {
      return _api;
    }

    public function set api(api:Class):void
    {
      _api = api;

      if (api != null)
        introspect();
    }

    /**
     * The URL of the destination service.
     */
    public function get destination():String
    {
      return _destination;
    }

    public function set destination(destination:String):void
    {
      _destination = destination;
    }
  }
}
