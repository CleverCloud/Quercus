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

package hessian.io
{
  import flash.utils.ByteArray;
	import flash.utils.IDataOutput;

  public class Hessian2StreamingOutput
  {
    private static const MAX_BUFFER_SIZE:int = 0xffff;
    private var _buffer:ByteArray = new ByteArray();
    private var _output:HessianOutput = new HessianOutput();
    private var _out:IDataOutput;

    public function Hessian2StreamingOutput(out:IDataOutput = null):void
    {
      init(out);
    }

    public function init(out:IDataOutput):void
    {
      _out = out;
      _output.init(_buffer);
    }

    public function writeObject(obj:Object):void
    {
      _output.writeObject(obj);

      _buffer.position = 0;

      while (_buffer.bytesAvailable > 0) {
        var length:int = Math.min(_buffer.bytesAvailable, MAX_BUFFER_SIZE);

        if (length == _buffer.bytesAvailable) {
          _out.writeByte('P'.charCodeAt());
        }
        else {
          _out.writeByte('p'.charCodeAt());
        }

        _out.writeByte(length >> 8);
        _out.writeByte(length);

        _out.writeBytes(_buffer, _buffer.position, length);

        _buffer.position += length;
      }

      _buffer = new ByteArray();
      _output.init(_buffer);
    }
  }
}

