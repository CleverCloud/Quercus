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
  import flash.net.URLStream;
  import flash.utils.describeType;
  import flash.utils.getDefinitionByName;
  import flash.utils.Proxy;

  import mx.rpc.AbstractOperation;
  import mx.rpc.AbstractService;

  import mx.core.mx_internal;

	use namespace mx_internal;

  [Bindable]
  /**
   * The HessianService class provides access to Hessian-based web services
   * on remote servers.
   *
   * @see hessian.client.HessianOperation
   * @see hessian.mxml.HessianService
   */
  public dynamic class HessianService extends AbstractService
  {
    private var _api:Class;
    private var _autoAlias:Boolean = false;
    private var _addHessianTypeName:Boolean = false;

    /**
     * Constructor.
     *
     * @param destination The URL of the destination service.
     * @param api The API associated with this HessianService, if any.
     *
     */
    public function HessianService(destination:String = null, api:Class = null)
    {
      super(destination);

      _api = api;

      if (api != null)
        introspect();
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
          var name:String = method.@name;
          var returnType:Class = 
            getDefinitionByName(method.@returnType) as Class;

          operations[name] = new HessianOperation(this, name, returnType);
        }
        catch (e:Error) {
        }
      }
    }

    /**
     * Retrieves a HessianOperation by name, creating one if it does not
     * already exist.
     *
     * @param name The name of the operation
     *
     * @return The HessianOperation named by <code>name</code>.
     *
     */
    public override function getOperation(name:String):AbstractOperation
    {
      var operation:AbstractOperation = super.getOperation(name);

      if (operation == null) {
        operation = new HessianOperation(this, name);
        operations[name] = operation;
      }

      return operation;
    }

    /**
     * The API associated with this HessianService, if any.
     */
    public function get api():String
    {
      if (_api == null)
        return null;

      return (_api as Object).toString();
    }

    public function set api(api:String):void
    {
      _api = getDefinitionByName(api) as Class;

      if (api != null)
        introspect();
    }

    public function set autoAlias(a:Boolean):void
    {
      _autoAlias = a;
    }

    public function get autoAlias():Boolean
    {
      return _autoAlias;
    }

    public function set addHessianTypeName(a:Boolean):void
    {
      _addHessianTypeName = a;
    }

    public function get addHessianTypeName():Boolean
    {
      return _addHessianTypeName;
    }

    public function toString():String
    {
      return "HessianService[destination=" + destination + ",api=" + _api + "]";
    }
  }
}
