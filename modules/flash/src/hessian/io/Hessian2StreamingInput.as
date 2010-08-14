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

package hessian.io
{
  import flash.errors.EOFError;
  import flash.utils.ByteArray;
	import flash.utils.IDataInput;
	import flash.utils.getQualifiedClassName;

  /**
   * An input for streaming Hessian data.  Data that is sent in Hessian
   * streaming packets is buffered until a whole object is received.  The
   * object is then decoded and stored within this object.  Such received
   * objects can be retrieved by calling #readObject().
   *
   * @see hessian.io.Hessian2Input
   */
  public class Hessian2StreamingInput
  {
    private var _length:int = 0;
    private var _chunkLength:int = 0;
    private var _offset:int = 0;
    private var _buffer:ByteArray;
    private var _input:Hessian2Input = new Hessian2Input();
    private var _queue:Array;
    private var _di:IDataInput;

    public function Hessian2StreamingInput(di:IDataInput = null)
    {
      if (di != null)
        init(di);
    }

    public function init(di:IDataInput):void
    {
      _di = di;
      _queue = [];
      _buffer = new ByteArray();
      _length = 0;
      _chunkLength = 0;
      _offset = 0;
    }

    /**
      * @return true if there are objects available to read from this input.
      */
    public function hasMoreObjects():Boolean
    {
      read();

      return _queue.length > 0;
    }

    public function readLong():Number
    {
      var obj:Object = readObject();

      if (! (obj is Number)) {
        throw new TypeError("expected long, but received " + obj + 
                            "(" + getQualifiedClassName(obj) + ")");
      }

      return obj as Number;
    }

    public function readInt():int
    {
      var obj:Object = readObject();

      if (! (obj is int)) {
        throw new TypeError("expected int, but received " + obj + 
                            "(" + getQualifiedClassName(obj) + ")");
      }

      return obj as int;
    }

    public function readString():String
    {
      var obj:Object = readObject();

      if (! (obj is String) && (obj != null)) {
        throw new TypeError("expected String, but received " + obj + 
                            "(" + getQualifiedClassName(obj) + ")");
      }

      return obj as String;
    }

    /**
      * @return The next object available from this input.
      */
    public function readObject():Object
    {
      return _queue.shift();
    }

    /**
      * Submits data to be read as stream data.
      */
    private function read():void
    {
      while (_di.bytesAvailable > 0) {
        if (! readChunkLength())
          return;

        if (_chunkLength == 0) {
          // this packet stream has ended
          // assume no 0 length chunks in the middle of an object

          _buffer.position = 0;
          _input.init(_buffer);
          _input.resetReferences();

          if (_buffer.bytesAvailable > 0) {
            while (true) {
              try {
                _queue.push(_input.readObject());
              }
              catch (e:EOFError) {
                break;
              }
            }
          }

          _offset = 0;
          _buffer.length = 0;
          _length = 0;
        }
        else {
          var length:int = _length;
          if (_di.bytesAvailable < _length)
            length = _di.bytesAvailable;

          _di.readBytes(_buffer, _offset, length);

          _offset = _buffer.length;

          _length -= length;
        }
      }
    }

    /**
      * Reads the next chunk length, if necessary. If the chunk length
      * could not be read yet, returns false.  If a chunk length was read
      * successfully or
      */
    private function readChunkLength():Boolean
    {
      if (_length > 0) 
        return true;

      _chunkLength = 0;

      while (_di.bytesAvailable > 0) {
        var code:int = _di.readUnsignedByte();

        _chunkLength = 0x80 * _chunkLength + (code & 0x7f);

        if ((code & 0x80) == 0) {
          _length = _chunkLength;

          return true;
        }
      }

      return false;
    }
  }
}

