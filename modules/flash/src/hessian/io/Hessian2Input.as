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

package hessian.io
{
	import flash.errors.EOFError;
	import flash.errors.IllegalOperationError;
	import flash.errors.IOError;
  import flash.net.getClassByAlias;
	import flash.net.registerClassAlias;
	import flash.utils.ByteArray;
	import flash.utils.getDefinitionByName;
	import flash.utils.IDataInput;

	import hessian.util.ByteUtils;

	import mx.collections.ArrayCollection;

  /**
   * A reader for the Hessian 2.0 protocol.
   *
   */
  public class Hessian2Input extends AbstractHessianInput 
  {
    private static const END_OF_DATA:int = -2;
    private static const SIZE:int = 256;

    /** @private */
    protected var _di:IDataInput;

    /** @private */
    protected var _method:String;

    /** @private */
    protected var _offset:int = 0;

    /** @private */
    protected var _length:int = 0;

    /** @private */
    protected var _buffer:ByteArray;

    /** @private */
    protected var _isLastChunk:Boolean;

    /** @private */
    protected var _isStreaming:Boolean;

    /** @private */
    protected var _chunkLength:int;

    /** @private */
    protected var _sbuf:String;

    /** @private */
    protected var _replyFault:Error;

    [ArrayElementType("hessian.io::ObjectDefinition")]
    /** @private */
    protected var _classRefs:Array;

    [ArrayElementType("String")]
    /** @private */
    protected var _types:Array;

    /** @private */
    protected var _refs:Array;

    /** @private */
    protected var _autoAlias:Boolean = false;

    /** @private */
    protected var _addHessianTypeName:Boolean = false;

    /**
     * Creates a new Hessian2Input.
     *
     * @param di The IDataInput from which this Hessian2Input will read.
     *
     * @see #init(IDataInput)
     *
     */
    public function Hessian2Input(di:IDataInput = null)
    {
      init(di);
    }

    /**
     * Initialize the Hessian stream with the underlying IDataInput.  This
     * method will reset the internal data of this instance, meaning this
     * Hessian2Input may be reused.
     *
     * @param di The IDataInput from which this Hessian2Input will read.
     * 
     */
    public override function init(di:IDataInput):void
    {
      _di = di;
      _buffer = new ByteArray();
      _offset = 0;
      _length = 0;
    }

    /**
      * When set to true, autoAlias forces an alias to be registered for
      * classes received by this input.  This may help performance in cases
      * where libraries are linked as RSLs.  Defaults to false.
      */
    public function set autoAlias(a:Boolean):void
    {
      _autoAlias = a;
    }

    public function get autoAlias():Boolean
    {
      return _autoAlias;
    }

    /**
      * When set to true, addHessianTypeName causes objects deserialized
      * without type information (i.e. as <code>Object</code>s) to include
      * an additional field/key called "hessianTypeName" with the value of
      * the type name received in the Hessian stream. This feature may be
      * useful when certain equivalent types are not available in the Flash/Flex
      * application. Applications receiving maps as <code>Object</code>s 
      * may want to disable this feature to avoid having the additional key
      * in the map. Defaults to false.
      */
    public function set addHessianTypeName(a:Boolean):void
    {
      _addHessianTypeName = a;
    }

    public function get addHessianTypeName():Boolean
    {
      return _addHessianTypeName;
    }

    /**
     * Returns the call's method.
     */
    public override function getMethod():String
    {
      return _method;
    }

    /**
     * Returns any reply fault.
     *
     * @return The reply fault, if available.
     */
    public function getReplyFault():Error
    {
      return _replyFault;
    }

    /**
     * Reads the call.
     *
     * <p>
     *   <pre>
     *   c major minor
     *   </pre>
     * </p>
     *
     * @param The version of the call.
     */
    public override function readCall():int
    {
      var tag:int = read();

      if (tag != 'C'.charCodeAt())
        throw new Error("expected hessian call ('C') at code=" + tag + " ch=" + String.fromCharCode(tag));

      return 0;
    }

    /**
     * Starts reading the envelope.
     *
     * <p>
     *   <pre>
     *   E major minor
     *   </pre>
     * </p>
     * 
     * @param The version of the envelope.
     */
    public function readEnvelope():int
    {
      var tag:int = read();
      var version:int = 0;
      
      if (tag == 'H'.charCodeAt()) {
        var major:int = read();
        var minor:int = read();

        version = (major << 16) + minor;

        tag = read();
      }

      if (tag != 'E'.charCodeAt())
        throw new Error("expected hessian Envelope ('E') at code=" + tag + " ch=" + String.fromCharCode(tag));

      return version;
    }

    /**
     * Completes reading the envelope.
     *
     * <p>A successful completion will have a single value:
     *   <pre>
     *   Z
     *   </pre>
     * </p>
     */
    public function completeEnvelope():void
    {
      var tag:int = read();

      if (tag != 'Z'.charCodeAt())
        throw new Error("expected end of envelope");
    }

    /**
     * Starts reading the call.
     *
     * <p>A successful completion will have a single value:
     *
     *   <pre>
     *   string
     *   </pre>
     * </p>
     *
     * @return The method name as read.
     */
    public override function readMethod():String 
    {
      _method = readString();

      return _method;
    }

    /**
     * Returns the number of method arguments
     *
     * <pre>
     * int
     * </pre>
     */
    public override function readMethodArgLength():int
    {
      return readInt();
    }

    /**
     * Starts reading the call, including the headers.
     *
     * <p>The call expects the following protocol data
     *
     *   <pre>
     *   c major minor
     *   m b16 b8 method
     *   </pre>
     * </p>
     */
    public override function startCall():void
    {
      readCall();

      readMethod();
    }

    /**
     * Completes reading the call.
     *
     * <p>The call expects the following protocol data
     *
     *   <pre>
     *   </pre>
     * </p>
     */
    public override function completeCall():void
    {
    }

    /**
     * Reads a reply as an object.
     * If the reply has a fault, throws the exception.
     *
     * @param expectedClass The expected class of the reply.
     * 
     * @return The reply value.
     */
    public override function readReply(expectedClass:Class = null):Object 
    {
      var tag:int = read();

      if (tag == 'R'.charCodeAt())
        return readObject(expectedClass);
      else if (tag == 'F'.charCodeAt())
        throw prepareFault(readObject());
      else {
        var response:String = new String(tag);

        try {
          var ch:int;

          while ((ch = read()) >= 0) {
            response += String.fromCharCode(ch);
          }
        }
        catch (e:Error) {
          trace(e);
        }

        throw new Error("expected hessian reply at " + codeName(tag) + "\n" +
                        response);
      }
    }

    /**
     * Starts reading the reply.
     *
     * <p>A successful completion will have a single value:
     *
     *   <pre>
     *   r
     *   </pre>
     * </p>
     */
    public override function startReply():void
    {
      // XXX: for variable length (?)
      readReply(Object);
    }

    /**
     * Prepares a fault.
     * 
     * @return The fault.
     */
    private function prepareFault(fault:Object):Error
    {
      var detail:Object = fault.detail;
      var msg:String = fault.message;

      if (detail is Error) {
        // XXX will we ever get a detail that is an Error?
        // (as opposed to a Throwable?)
        _replyFault = detail as Error;

        if (msg != null)
          _replyFault.message = msg;

        return _replyFault;
      }

      else {
        var code:String = String(fault.code);

        _replyFault = new HessianServiceError(msg, code, detail);

        return _replyFault;
      }
    }

    /**
     * Completes reading the call.
     *
     * <p>A successful completion will have a single value:
     *  <pre>
     *  z
     *  </pre>
     * </p>
     */
    public override function completeReply():void
    {
    }

    /**
     * Completes reading the call.
     *
     * <p>A successful completion will have a single value:
     *   <pre>
     *   Z
     *   </pre>
     * </p>
     */
    public function completeValueReply():void
    {
      var tag:int = read();
      
      if (tag != 'Z'.charCodeAt())
        error("expected end of reply");
    }

    /**
     * Reads a header, returning null if there are no headers.
     *
     * <p>
     *   <pre>
     *   H b16 b8 value
     *   </pre>
     * </p>
     * 
     * @return The header if available or null otherwise.
     */
    public override function readHeader():String 
    {
      return null;
    }

    /**
     * Starts reading the message.
     *
     * <p>
     *   <pre>
     *   p major minor
     *   </pre>
     * </p>
     *
     * @return The version of the message.
     */
    public function startMessage():int
    {
      var tag:int = read();

      if (tag == 'p'.charCodeAt())
        _isStreaming = false;

      else if (tag == 'P'.charCodeAt())
        _isStreaming = true;

      else
        throw error("expected Hessian message ('p') at code=" + tag + " ch=" + String.fromCharCode(tag));

      var major:int = read();
      var minor:int = read();

      return (major << 16) + minor;
    }

    /**
     * Completes reading the message.
     *
     * <p>A successful completion will have a single value:
     *   <pre>
     *   Z
     *   </pre>
     * </p>
     */
    public function completeMessage():void
    {
      var tag:int = read();
      
      if (tag != 'Z'.charCodeAt())
        error("expected end of message");
    }

    /**
     * Reads a null.
     *
     * <p>
     *   <pre>
     *   N
     *   </pre>
     * </p>
     */
    public override function readNull():void
    {
      var tag:int = read();

      switch (String.fromCharCode(tag)) {
        case 'N'.charCodeAt(): return;
 
        default:
          throw new Error("expected end of reply");
      }
    }

    /**
     * Reads a boolean.
     *
     * <p>
     *   <pre>
     *   T
     *   F
     *   </pre>
     * </p>
     *
     * @return The boolean value read.
     */
    public override function readBoolean():Boolean 
    {
      var tag:int = _offset < _length ? (_buffer[_offset++] & 0xff) : read();

      switch (tag) {
        case 'T'.charCodeAt(): return true;
        case 'F'.charCodeAt(): return false;

        // direct integer
        case 0x80: case 0x81: case 0x82: case 0x83:
        case 0x84: case 0x85: case 0x86: case 0x87:
        case 0x88: case 0x89: case 0x8a: case 0x8b:
        case 0x8c: case 0x8d: case 0x8e: case 0x8f:

        case 0x90: case 0x91: case 0x92: case 0x93:
        case 0x94: case 0x95: case 0x96: case 0x97:
        case 0x98: case 0x99: case 0x9a: case 0x9b:
        case 0x9c: case 0x9d: case 0x9e: case 0x9f:

        case 0xa0: case 0xa1: case 0xa2: case 0xa3:
        case 0xa4: case 0xa5: case 0xa6: case 0xa7:
        case 0xa8: case 0xa9: case 0xaa: case 0xab:
        case 0xac: case 0xad: case 0xae: case 0xaf:

        case 0xb0: case 0xb1: case 0xb2: case 0xb3:
        case 0xb4: case 0xb5: case 0xb6: case 0xb7:
        case 0xb8: case 0xb9: case 0xba: case 0xbb:
        case 0xbc: case 0xbd: case 0xbe: case 0xbf:
          return tag != Hessian2Constants.BC_INT_ZERO;

        // INT_BYTE = 0
        case 0xc8: 
          return read() != 0;

        // INT_BYTE != 0
        case 0xc0: case 0xc1: case 0xc2: case 0xc3:
        case 0xc4: case 0xc5: case 0xc6: case 0xc7:
        case 0xc9: case 0xca: case 0xcb:
        case 0xcc: case 0xcd: case 0xce: case 0xcf:
          read();
          return true;

        // INT_SHORT = 0
        case 0xd4: 
          return (256 * read() + read()) != 0;

        // INT_SHORT != 0
        case 0xd0: case 0xd1: case 0xd2: case 0xd3:
        case 0xd5: case 0xd6: case 0xd7:
          read();
          read();
        return true;

        case 'I'.charCodeAt(): 
          return parseInteger() != 0;

        case 0xd8: case 0xd9: case 0xda: case 0xdb:
        case 0xdc: case 0xdd: case 0xde: case 0xdf:

        case 0xe0: case 0xe1: case 0xe2: case 0xe3:
        case 0xe4: case 0xe5: case 0xe6: case 0xe7:
        case 0xe8: case 0xe9: case 0xea: case 0xeb:
        case 0xec: case 0xed: case 0xee: case 0xef:
          return tag != Hessian2Constants.BC_LONG_ZERO;

        // LONG_BYTE = 0
        case 0xf8: 
          return read() != 0;

        // LONG_BYTE != 0
        case 0xf0: case 0xf1: case 0xf2: case 0xf3:
        case 0xf4: case 0xf5: case 0xf6: case 0xf7:
        case 0xf9: case 0xfa: case 0xfb:
        case 0xfc: case 0xfd: case 0xfe: case 0xff:
          read();
          return true;

        // INT_SHORT = 0
        case 0x3c: 
          return (256 * read() + read()) != 0;

        // INT_SHORT != 0
        case 0x38: case 0x39: case 0x3a: case 0x3b:
        case 0x3d: case 0x3e: case 0x3f:
          read();
          read();
          return true;

        case Hessian2Constants.BC_LONG_INT:
          return (0x1000000 * read()
                  + 0x10000 * read()
                  + 0x100 * read()
                  + read()) != 0;

        case 'L'.charCodeAt():
          return parseLong() != 0;

        case Hessian2Constants.BC_DOUBLE_ZERO:
          return false;

        case Hessian2Constants.BC_DOUBLE_ONE:
          return true;

        case Hessian2Constants.BC_DOUBLE_BYTE:
          return read() != 0;

        case Hessian2Constants.BC_DOUBLE_SHORT:
          return (0x100 * read() + read()) != 0;

        case Hessian2Constants.BC_DOUBLE_MILL:
          {
            var mills:int = parseInteger();

            return mills != 0;
          }

        case 'D'.charCodeAt():
          return parseDouble() != 0.0;

        case 'N'.charCodeAt():
          return false;

        default:
          throw expect("boolean", tag);
      }
    }

    /**
     * Reads an integer.
     *
     * <p>
     *   <pre>
     *   I b32 b24 b16 b8
     *   </pre>
     * </p>
     *
     * @return The integer value read.
     */
    public override function readInt():int
    {
      var tag:int = read();

      switch (tag) {
        case 'N'.charCodeAt():
          return 0;

        case 'F'.charCodeAt():
          return 0;

        case 'T'.charCodeAt():
          return 1;

        // direct integer
        case 0x80: case 0x81: case 0x82: case 0x83:
        case 0x84: case 0x85: case 0x86: case 0x87:
        case 0x88: case 0x89: case 0x8a: case 0x8b:
        case 0x8c: case 0x8d: case 0x8e: case 0x8f:

        case 0x90: case 0x91: case 0x92: case 0x93:
        case 0x94: case 0x95: case 0x96: case 0x97:
        case 0x98: case 0x99: case 0x9a: case 0x9b:
        case 0x9c: case 0x9d: case 0x9e: case 0x9f:

        case 0xa0: case 0xa1: case 0xa2: case 0xa3:
        case 0xa4: case 0xa5: case 0xa6: case 0xa7:
        case 0xa8: case 0xa9: case 0xaa: case 0xab:
        case 0xac: case 0xad: case 0xae: case 0xaf:

        case 0xb0: case 0xb1: case 0xb2: case 0xb3:
        case 0xb4: case 0xb5: case 0xb6: case 0xb7:
        case 0xb8: case 0xb9: case 0xba: case 0xbb:
        case 0xbc: case 0xbd: case 0xbe: case 0xbf:
          return tag - Hessian2Constants.BC_INT_ZERO;

        /* byte int */
        case 0xc0: case 0xc1: case 0xc2: case 0xc3:
        case 0xc4: case 0xc5: case 0xc6: case 0xc7:
        case 0xc8: case 0xc9: case 0xca: case 0xcb:
        case 0xcc: case 0xcd: case 0xce: case 0xcf:
          return ((tag - Hessian2Constants.BC_INT_BYTE_ZERO) << 8) + read();

        /* short int */
        case 0xd0: case 0xd1: case 0xd2: case 0xd3:
        case 0xd4: case 0xd5: case 0xd6: case 0xd7:
          return ((tag - Hessian2Constants.BC_INT_SHORT_ZERO) << 16) + 
                 256 * read() + read();

        case 'I'.charCodeAt():
        case Hessian2Constants.BC_LONG_INT:
          return ((read() << 24)
                + (read() << 16)
                + (read() << 8)
                + read());

        // direct long
        case 0xd8: case 0xd9: case 0xda: case 0xdb:
        case 0xdc: case 0xdd: case 0xde: case 0xdf:

        case 0xe0: case 0xe1: case 0xe2: case 0xe3:
        case 0xe4: case 0xe5: case 0xe6: case 0xe7:
        case 0xe8: case 0xe9: case 0xea: case 0xeb:
        case 0xec: case 0xed: case 0xee: case 0xef:
          return tag - Hessian2Constants.BC_LONG_ZERO;

        /* byte long */
        case 0xf0: case 0xf1: case 0xf2: case 0xf3:
        case 0xf4: case 0xf5: case 0xf6: case 0xf7:
        case 0xf8: case 0xf9: case 0xfa: case 0xfb:
        case 0xfc: case 0xfd: case 0xfe: case 0xff:
          return ((tag - Hessian2Constants.BC_LONG_BYTE_ZERO) << 8) + read();

        /* short long */
        case 0x38: case 0x39: case 0x3a: case 0x3b:
        case 0x3c: case 0x3d: case 0x3e: case 0x3f:
          return ((tag - Hessian2Constants.BC_LONG_SHORT_ZERO) << 16) + 
                 256 * read() + read();

        case 'L'.charCodeAt():
          return int(parseLong());

        case Hessian2Constants.BC_DOUBLE_ZERO:
          return 0;

        case Hessian2Constants.BC_DOUBLE_ONE:
          return 1;

        //case LONG_BYTE:
        case Hessian2Constants.BC_DOUBLE_BYTE:
          return _offset < _length ? _buffer[_offset++] : read();

        //case INT_SHORT:
        //case LONG_SHORT:
        case Hessian2Constants.BC_DOUBLE_SHORT:
          return int(256 * read() + read());

        case Hessian2Constants.BC_DOUBLE_MILL:
          {
            var mills:int = parseInteger();

            return int(0.001 & mills);
          }

        case 'D'.charCodeAt():
          return int(parseDouble());

        default:
          throw expect("int", tag);
      }
    }

    /**
     * Reads a long.
     *
     * <p>
     *   <pre>
     *   L b64 b56 b48 b40 b32 b24 b16 b8
     *   </pre>
     * </p>
     *
     * @return The long value read as a Number.
     */
    public override function readLong():Number
    {
      var tag:int = read();

      switch (tag) {
        case 'N'.charCodeAt():
          return 0;

        case 'F'.charCodeAt():
          return 0;

        case 'T'.charCodeAt():
          return 1;

        // direct integer
        case 0x80: case 0x81: case 0x82: case 0x83:
        case 0x84: case 0x85: case 0x86: case 0x87:
        case 0x88: case 0x89: case 0x8a: case 0x8b:
        case 0x8c: case 0x8d: case 0x8e: case 0x8f:

        case 0x90: case 0x91: case 0x92: case 0x93:
        case 0x94: case 0x95: case 0x96: case 0x97:
        case 0x98: case 0x99: case 0x9a: case 0x9b:
        case 0x9c: case 0x9d: case 0x9e: case 0x9f:

        case 0xa0: case 0xa1: case 0xa2: case 0xa3:
        case 0xa4: case 0xa5: case 0xa6: case 0xa7:
        case 0xa8: case 0xa9: case 0xaa: case 0xab:
        case 0xac: case 0xad: case 0xae: case 0xaf:

        case 0xb0: case 0xb1: case 0xb2: case 0xb3:
        case 0xb4: case 0xb5: case 0xb6: case 0xb7:
        case 0xb8: case 0xb9: case 0xba: case 0xbb:
        case 0xbc: case 0xbd: case 0xbe: case 0xbf:
          return tag - Hessian2Constants.BC_INT_ZERO;

        /* byte int */
        case 0xc0: case 0xc1: case 0xc2: case 0xc3:
        case 0xc4: case 0xc5: case 0xc6: case 0xc7:
        case 0xc8: case 0xc9: case 0xca: case 0xcb:
        case 0xcc: case 0xcd: case 0xce: case 0xcf:
          return ((tag - Hessian2Constants.BC_INT_BYTE_ZERO) << 8) + read();

        /* short int */
        case 0xd0: case 0xd1: case 0xd2: case 0xd3:
        case 0xd4: case 0xd5: case 0xd6: case 0xd7:
          return ((tag - Hessian2Constants.BC_INT_SHORT_ZERO) << 16) + 
                 256 * read() + read();

        case Hessian2Constants.BC_DOUBLE_BYTE:
          return ByteUtils.castToByte((_offset < _length ? _buffer[_offset++] 
                                                         : read()));

        case Hessian2Constants.BC_DOUBLE_SHORT:
          return ByteUtils.castToShort((256 * read() + read()) & 0xFFFF);

        case 'I'.charCodeAt():
        case Hessian2Constants.BC_LONG_INT:
          return parseInteger();

        // direct long
        case 0xd8: case 0xd9: case 0xda: case 0xdb:
        case 0xdc: case 0xdd: case 0xde: case 0xdf:

        case 0xe0: case 0xe1: case 0xe2: case 0xe3:
        case 0xe4: case 0xe5: case 0xe6: case 0xe7:
        case 0xe8: case 0xe9: case 0xea: case 0xeb:
        case 0xec: case 0xed: case 0xee: case 0xef:
          return tag - Hessian2Constants.BC_LONG_ZERO;

        /* byte long */
        case 0xf0: case 0xf1: case 0xf2: case 0xf3:
        case 0xf4: case 0xf5: case 0xf6: case 0xf7:
        case 0xf8: case 0xf9: case 0xfa: case 0xfb:
        case 0xfc: case 0xfd: case 0xfe: case 0xff:
          return ((tag - Hessian2Constants.BC_LONG_BYTE_ZERO) << 8) + read();

        /* short long */
        case 0x38: case 0x39: case 0x3a: case 0x3b:
        case 0x3c: case 0x3d: case 0x3e: case 0x3f:
          return ((tag - Hessian2Constants.BC_LONG_SHORT_ZERO) << 16) + 
                 256 * read() + read();

        case 'L'.charCodeAt():
          return parseLong();

        case Hessian2Constants.BC_DOUBLE_ZERO:
          return 0;

        case Hessian2Constants.BC_DOUBLE_ONE:
          return 1;

        case Hessian2Constants.BC_DOUBLE_MILL:
          {
            var mills:int = parseInteger();

            return Math.floor(Number(0.001 * mills));
          }

        case 'D'.charCodeAt():
          return parseDouble();

        default:
          throw expect("long", tag);
      }
    }

    /**
     * Reads a double.
     *
     * <p>
     *   <pre>
     *   D b64 b56 b48 b40 b32 b24 b16 b8
     *   </pre>
     * </p>
     *
     * @return The double value read as a Number.
     */
    public override function readDouble():Number
    {
      var tag:int = read();

      switch (tag) {
        case 'N'.charCodeAt():
          return 0;

        case 'F'.charCodeAt():
          return 0;

        case 'T'.charCodeAt():
          return 1;

        // direct integer
        case 0x80: case 0x81: case 0x82: case 0x83:
        case 0x84: case 0x85: case 0x86: case 0x87:
        case 0x88: case 0x89: case 0x8a: case 0x8b:
        case 0x8c: case 0x8d: case 0x8e: case 0x8f:

        case 0x90: case 0x91: case 0x92: case 0x93:
        case 0x94: case 0x95: case 0x96: case 0x97:
        case 0x98: case 0x99: case 0x9a: case 0x9b:
        case 0x9c: case 0x9d: case 0x9e: case 0x9f:

        case 0xa0: case 0xa1: case 0xa2: case 0xa3:
        case 0xa4: case 0xa5: case 0xa6: case 0xa7:
        case 0xa8: case 0xa9: case 0xaa: case 0xab:
        case 0xac: case 0xad: case 0xae: case 0xaf:

        case 0xb0: case 0xb1: case 0xb2: case 0xb3:
        case 0xb4: case 0xb5: case 0xb6: case 0xb7:
        case 0xb8: case 0xb9: case 0xba: case 0xbb:
        case 0xbc: case 0xbd: case 0xbe: case 0xbf:
          return tag - 0x90;

        /* byte int */
        case 0xc0: case 0xc1: case 0xc2: case 0xc3:
        case 0xc4: case 0xc5: case 0xc6: case 0xc7:
        case 0xc8: case 0xc9: case 0xca: case 0xcb:
        case 0xcc: case 0xcd: case 0xce: case 0xcf:
          return ((tag - Hessian2Constants.BC_INT_BYTE_ZERO) << 8) + read();

        /* short int */
        case 0xd0: case 0xd1: case 0xd2: case 0xd3:
        case 0xd4: case 0xd5: case 0xd6: case 0xd7:
          return ((tag - Hessian2Constants.BC_INT_SHORT_ZERO) << 16) + 
                 256 * read() + read();

        case 'I'.charCodeAt():
        case Hessian2Constants.BC_LONG_INT:
          return parseInteger();

        // direct long
        case 0xd8: case 0xd9: case 0xda: case 0xdb:
        case 0xdc: case 0xdd: case 0xde: case 0xdf:

        case 0xe0: case 0xe1: case 0xe2: case 0xe3:
        case 0xe4: case 0xe5: case 0xe6: case 0xe7:
        case 0xe8: case 0xe9: case 0xea: case 0xeb:
        case 0xec: case 0xed: case 0xee: case 0xef:
          return tag - Hessian2Constants.BC_LONG_ZERO;

        /* byte long */
        case 0xf0: case 0xf1: case 0xf2: case 0xf3:
        case 0xf4: case 0xf5: case 0xf6: case 0xf7:
        case 0xf8: case 0xf9: case 0xfa: case 0xfb:
        case 0xfc: case 0xfd: case 0xfe: case 0xff:
          return ((tag - Hessian2Constants.BC_LONG_BYTE_ZERO) << 8) + read();

        /* short long */
        case 0x38: case 0x39: case 0x3a: case 0x3b:
        case 0x3c: case 0x3d: case 0x3e: case 0x3f:
          return ((tag - Hessian2Constants.BC_LONG_SHORT_ZERO) << 16) + 
                 256 * read() + read();

        case 'L'.charCodeAt():
          return /* (double) */ parseLong();

        case Hessian2Constants.BC_DOUBLE_ZERO:
          return 0;

        case Hessian2Constants.BC_DOUBLE_ONE:
          return 1;

        case Hessian2Constants.BC_DOUBLE_BYTE:
          return ByteUtils.castToByte(_offset < _length ? _buffer[_offset++]
                                                        : read());

        case Hessian2Constants.BC_DOUBLE_SHORT:
          return ByteUtils.castToShort((256 * read() + read()) & 0xFFFF);

        case Hessian2Constants.BC_DOUBLE_MILL:
          {
            var mills:int = parseInteger();

            return 0.001 * mills;
          }

        case 'D'.charCodeAt():
          return parseDouble();

        default:
          throw expect("double", tag);
      }
    }

    /**
     * Reads a date.
     *
     * <p>
     *   <pre>
     *   T b64 b56 b48 b40 b32 b24 b16 b8
     *   </pre>
     * </p>
     * 
     * @return The date value read as a Number (milliseconds since the epoch).
     */
    public override function readUTCDate():Number
    {
      var tag:int = read();

      if (tag == Hessian2Constants.BC_DATE)
        return parseLong();
      else if (tag == Hessian2Constants.BC_DATE_MINUTE)
        return parseInteger() * 60000;
      else
        throw expect("date", tag);
    }

    /**
     * Reads a character from the stream.
     *
     * @return The UTF8 character value read as an integer.
     */
    public function readChar():int
    {
      if (_chunkLength > 0) {
        _chunkLength--;

        if (_chunkLength == 0 && _isLastChunk)
          _chunkLength = END_OF_DATA;

        var ch:int = parseUTF8Char();

        return ch;
      }
      else if (_chunkLength == END_OF_DATA) {
        _chunkLength = 0;

        return -1;
      }
      
      var tag:int = read();

      switch (tag) {
        case 'N'.charCodeAt():
          return -1;

        case 'S'.charCodeAt():
        case Hessian2Constants.BC_STRING_CHUNK:
          _isLastChunk = tag == 'S'.charCodeAt();
          _chunkLength = (read() << 8) + read();

          _chunkLength--;

          var value:int = parseUTF8Char();

          // special code so successive read byte won't
          // be read as a single object.
          if (_chunkLength == 0 && _isLastChunk)
            _chunkLength = END_OF_DATA;

          return value;
          
        default:
          throw new IOError("expected 'S' at " + String.fromCharCode(tag));
      }
    }

    /**
     * Reads a string.
     *
     * <p>
     *   <pre>
     *   S b16 b8 string value
     *   </pre>
     * </p>
     *
     * @return The string value read.
     */
    public override function readString():String
    {
      var tag:int = read();

      switch (tag) {
        case 'N'.charCodeAt():
          return null;

        case 'I'.charCodeAt():
          return parseInteger().toString();
        case 'L'.charCodeAt():
          return parseLong().toString();
        case 'D'.charCodeAt():
          return parseDouble().toString();

        case 'S'.charCodeAt():
        case Hessian2Constants.BC_STRING_CHUNK:
          _isLastChunk = tag == 'S'.charCodeAt();
          _chunkLength = (read() << 8) + read();

          _sbuf = "";

          var ch:int;

          while ((ch = parseChar()) >= 0)
            _sbuf += String.fromCharCode(ch);

          return _sbuf;

        // 0-byte string
        case 0x00: case 0x01: case 0x02: case 0x03:
        case 0x04: case 0x05: case 0x06: case 0x07:
        case 0x08: case 0x09: case 0x0a: case 0x0b:
        case 0x0c: case 0x0d: case 0x0e: case 0x0f:

        case 0x10: case 0x11: case 0x12: case 0x13:
        case 0x14: case 0x15: case 0x16: case 0x17:
        case 0x18: case 0x19: case 0x1a: case 0x1b:
        case 0x1c: case 0x1d: case 0x1e: case 0x1f:
          _isLastChunk = true;
          _chunkLength = tag - 0x00;

          _sbuf = "";

          while ((ch = parseChar()) >= 0)
            _sbuf += String.fromCharCode(ch);

          return _sbuf;

        case 0x30: case 0x31: case 0x32: case 0x33:
          _isLastChunk = true;
          _chunkLength = (tag - 0x30) * 256 + read();

          _sbuf = "";

          while ((ch = parseChar()) >= 0)
            _sbuf += String.fromCharCode(ch);

          return _sbuf;

        default:
          throw expect("string", tag);
      }
    }

    /**
     * Reads a byte array.
     *
     * <p>
     *   <pre>
     *   b b16 b8 non-final binary chunk
     *   B b16 b8 final binary chunk
     *   </pre>
     * </p>
     *
     * @return A ByteArray with the bytes that were read.
     */
    public override function readBytes():ByteArray
    {
      var b:int;
      var tag:int = read();
      var buffer:ByteArray = null;

      switch (tag) {
        case 'N'.charCodeAt():
          return null;

        case 'B'.charCodeAt():
        case Hessian2Constants.BC_BINARY_CHUNK:
          _isLastChunk = tag == 'B'.charCodeAt();
          _chunkLength = (read() << 8) + read();

          buffer = new ByteArray();

          while ((b = parseByte()) >= 0)
            buffer.writeByte(b);

          buffer.position = 0;

          return buffer;

        case 0x20: case 0x21: case 0x22: case 0x23:
        case 0x24: case 0x25: case 0x26: case 0x27:
        case 0x28: case 0x29: case 0x2a: case 0x2b:
        case 0x2c: case 0x2d: case 0x2e: case 0x2f:
          _isLastChunk = true;
          _chunkLength = tag - 0x20;

          buffer = new ByteArray();

          while ((b = parseByte()) >= 0)
            buffer.writeByte(b);

          buffer.position = 0;

          return buffer;

        case 0x34: case 0x35: case 0x36: case 0x37:
          _isLastChunk = true;
          _chunkLength = (tag - 0x34) * 256 + read();

          buffer = new ByteArray();

          while ((b = parseByte()) >= 0)
            buffer.writeByte(b);

          buffer.position = 0;

          return buffer;

        default:
          throw expect("bytes", tag);
      }
    }

    /**
     * Reads a fault.
     *
     * @return The fault value read.
     */
    private function readFault():Object
    {
      var map:Object = new Object();

      var code:int = read();
      for (; code > 0 && code != 'Z'.charCodeAt(); code = read()) {
        _offset--;

        var key:Object = readObject();
        var value:Object = readObject();

        if (key != null && value != null)
          map[key] = value;
      }

      if (code != 'Z'.charCodeAt())
        throw expect("fault", code);

      return map;
    }

    /**
     * Reads an arbitrary object from the input stream.
     *
     * @param cl the expected class if the protocol doesn't supply it.
     *
     * @return The object value read.
     */
    public override function readObject(cl:Class = null):Object
    {
      if (cl == null || cl == Object)
        return readArbitraryObject();
      
      var tag:int = _offset < _length ? (_buffer[_offset++] & 0xff) : read();

      var ref:int;
      var size:int;
      var type:String;
      var length:int;
      var def:ObjectDefinition;

      switch (tag) {
        case 'N'.charCodeAt():
          return null;

        case 'H'.charCodeAt():
          return readMap(cl);

        case 'M'.charCodeAt():
          type = readType();

          var obj:Object = readMap(getClassDefinition(type, cl));

          if (_addHessianTypeName
              && cl == Object 
              && type != null 
              && type.length > 0)
            obj.hessianTypeName = type;

          return obj;

        case 'C'.charCodeAt():
          {
            readObjectDefinition();

            return readObject(cl);
          }

        case 0x60: case 0x61: case 0x62: case 0x63:
        case 0x64: case 0x65: case 0x66: case 0x67:
        case 0x68: case 0x69: case 0x6a: case 0x6b:
        case 0x6c: case 0x6d: case 0x6e: case 0x6f:
          {
            ref = tag - 0x60;
            size = _classRefs.length;

            if (ref < 0 || size <= ref)
              throw new HessianProtocolError("'" + ref + "' is an unknown class definition");

            def = _classRefs[ref] as ObjectDefinition;

            return readObjectInstance(def, cl);
          }

        case 'O'.charCodeAt():
          {
            ref = readInt();
            size = _classRefs.length;

            if (ref < 0 || size <= ref)
              throw new HessianProtocolError("'" + ref + "' is an unknown class definition");

            def = _classRefs[ref] as ObjectDefinition;

            return readObjectInstance(def, cl);
          }

        case Hessian2Constants.BC_LIST_VARIABLE:
          {
            type = readType();

            return readList(-1, type, cl);
          }

        case Hessian2Constants.BC_LIST_FIXED:
          {
            type = readType();
            length = readInt();

            return readLengthList(length, type, cl);
          }

        case 0x70: case 0x71: case 0x72: case 0x73:
        case 0x74: case 0x75: case 0x76: case 0x77:
          {
            length = tag - 0x70;
            type = readType();

            return readLengthList(length, type, cl);
          }

        case Hessian2Constants.BC_LIST_VARIABLE_UNTYPED:
          {
            return readList(-1, null, cl);
          }

        case 0x78: case 0x79: case 0x7a: case 0x7b:
        case 0x7c: case 0x7d: case 0x7e: case 0x7f:
          {
            length = tag - 0x78;
            type = readType();

            return readLengthList(length, type, cl);
          }

        case Hessian2Constants.BC_REF: 
          {
            return _refs[readInt()];
          }
      }

      if (tag >= 0)
        _offset--;

      // XXX?
      // hessian/3b2i vs hessian/3406
      // return readObject();

      return readArbitraryObject();
    }

    /**
     * Reads an arbitrary object from the input stream.
     *
     * @return The object value read.
     */
    private function readArbitraryObject():Object
    {
      var tag:int = _offset < _length ? (_buffer[_offset++] & 0xff) : read();

      var ch:int;
      var buffer:ByteArray;
      var type:String;
      var length:int;
      var ref:int;
      var len:int;
      var i:int;
      var def:ObjectDefinition;

      switch (tag) {
        case 'N'.charCodeAt():
          return null;

        case 'T'.charCodeAt():
          return true;

        case 'F'.charCodeAt():
          return false;

        // direct integer
        case 0x80: case 0x81: case 0x82: case 0x83:
        case 0x84: case 0x85: case 0x86: case 0x87:
        case 0x88: case 0x89: case 0x8a: case 0x8b:
        case 0x8c: case 0x8d: case 0x8e: case 0x8f:

        case 0x90: case 0x91: case 0x92: case 0x93:
        case 0x94: case 0x95: case 0x96: case 0x97:
        case 0x98: case 0x99: case 0x9a: case 0x9b:
        case 0x9c: case 0x9d: case 0x9e: case 0x9f:

        case 0xa0: case 0xa1: case 0xa2: case 0xa3:
        case 0xa4: case 0xa5: case 0xa6: case 0xa7:
        case 0xa8: case 0xa9: case 0xaa: case 0xab:
        case 0xac: case 0xad: case 0xae: case 0xaf:

        case 0xb0: case 0xb1: case 0xb2: case 0xb3:
        case 0xb4: case 0xb5: case 0xb6: case 0xb7:
        case 0xb8: case 0xb9: case 0xba: case 0xbb:
        case 0xbc: case 0xbd: case 0xbe: case 0xbf:
          return tag - Hessian2Constants.BC_INT_ZERO;

        /* byte int */
        case 0xc0: case 0xc1: case 0xc2: case 0xc3:
        case 0xc4: case 0xc5: case 0xc6: case 0xc7:
        case 0xc8: case 0xc9: case 0xca: case 0xcb:
        case 0xcc: case 0xcd: case 0xce: case 0xcf:
          return ((tag - Hessian2Constants.BC_INT_BYTE_ZERO) << 8) + read();

        /* short int */
        case 0xd0: case 0xd1: case 0xd2: case 0xd3:
        case 0xd4: case 0xd5: case 0xd6: case 0xd7:
          return ((tag - Hessian2Constants.BC_INT_SHORT_ZERO) << 16) + 
                 256 * read() + read();

        case 'I'.charCodeAt():
          return parseInteger();

        // direct long
        case 0xd8: case 0xd9: case 0xda: case 0xdb:
        case 0xdc: case 0xdd: case 0xde: case 0xdf:

        case 0xe0: case 0xe1: case 0xe2: case 0xe3:
        case 0xe4: case 0xe5: case 0xe6: case 0xe7:
        case 0xe8: case 0xe9: case 0xea: case 0xeb:
        case 0xec: case 0xed: case 0xee: case 0xef:
          return tag - Hessian2Constants.BC_LONG_ZERO;

        /* byte long */
        case 0xf0: case 0xf1: case 0xf2: case 0xf3:
        case 0xf4: case 0xf5: case 0xf6: case 0xf7:
        case 0xf8: case 0xf9: case 0xfa: case 0xfb:
        case 0xfc: case 0xfd: case 0xfe: case 0xff:
          return ((tag - Hessian2Constants.BC_LONG_BYTE_ZERO) << 8) + read();

        /* short long */
        case 0x38: case 0x39: case 0x3a: case 0x3b:
        case 0x3c: case 0x3d: case 0x3e: case 0x3f:
          return ((tag - Hessian2Constants.BC_LONG_SHORT_ZERO) << 16) + 
                 256 * read() + read();

        case Hessian2Constants.BC_LONG_INT:
          return parseInteger();

        case 'L'.charCodeAt():
          return parseLong();

        case Hessian2Constants.BC_DOUBLE_ZERO:
          return new Number(0);

        case Hessian2Constants.BC_DOUBLE_ONE:
          return new Number(1);

        case Hessian2Constants.BC_DOUBLE_BYTE:
          return ByteUtils.castToByte(_offset < _length ? _buffer[_offset++]
                                                        : read());
        case Hessian2Constants.BC_DOUBLE_SHORT:
          return ByteUtils.castToShort((256 * read() + read()) & 0xFFFF);

        case Hessian2Constants.BC_DOUBLE_MILL:
          {
            var mills:int = parseInteger();

            return Number(0.001 * mills)
          }

        case 'D'.charCodeAt():
          return parseDouble();

        case Hessian2Constants.BC_DATE:
          return new Date(parseLong());

        case Hessian2Constants.BC_DATE_MINUTE:
          return new Date(parseInteger() * 60000);

        case Hessian2Constants.BC_STRING_CHUNK:
        case 'S'.charCodeAt(): 
          {
            _isLastChunk = (tag == 'S'.charCodeAt());
            _chunkLength = (read() << 8) + read();

            _sbuf = "";

            while ((ch = parseChar()) >= 0)
              _sbuf += String.fromCharCode(ch);

            return _sbuf;
          }

        case 0x00: case 0x01: case 0x02: case 0x03:
        case 0x04: case 0x05: case 0x06: case 0x07:
        case 0x08: case 0x09: case 0x0a: case 0x0b:
        case 0x0c: case 0x0d: case 0x0e: case 0x0f:

        case 0x10: case 0x11: case 0x12: case 0x13:
        case 0x14: case 0x15: case 0x16: case 0x17:
        case 0x18: case 0x19: case 0x1a: case 0x1b:
        case 0x1c: case 0x1d: case 0x1e: case 0x1f:
          {
            _isLastChunk = true;
            _chunkLength = tag - 0x00;

            _sbuf = "";

            while ((ch = parseChar()) >= 0)
              _sbuf += String.fromCharCode(ch);

            return _sbuf;
          }

        case 0x30: case 0x31: case 0x32: case 0x33:
          {
            _isLastChunk = true;
            _chunkLength = (tag - 0x30) * 256 + read();

            _sbuf = "";

            while ((ch = parseChar()) >= 0)
              _sbuf += String.fromCharCode(ch);

            return _sbuf;
          }

        case Hessian2Constants.BC_BINARY_CHUNK:
        case 'B'.charCodeAt(): 
          {
            _isLastChunk = (tag == 'B'.charCodeAt());
            _chunkLength = (read() << 8) + read();

            buffer = new ByteArray();

            var b:int;

            while ((b = parseByte()) >= 0)
              buffer.writeByte(b);

            buffer.position = 0;

            return buffer;
          }

        case 0x20: case 0x21: case 0x22: case 0x23:
        case 0x24: case 0x25: case 0x26: case 0x27:
        case 0x28: case 0x29: case 0x2a: case 0x2b:
        case 0x2c: case 0x2d: case 0x2e: case 0x2f:
          {
            _isLastChunk = true;
            len = tag - 0x20;
            _chunkLength = 0;

            buffer = new ByteArray();

            for (i = 0; i < len; i++)
              buffer.writeByte(read());

            buffer.position = 0;

            return buffer;
          }

        case 0x34: case 0x35: case 0x36: case 0x37:
          {
            _isLastChunk = true;
            len = (tag - 0x34) * 256 + read();
            _chunkLength = 0;

            buffer = new ByteArray();

            for (i = 0; i < len; i++)
              buffer.writeByte(read());

            buffer.position = 0;

            return buffer;
          }

        case Hessian2Constants.BC_LIST_VARIABLE:
          {
            type = readType();

            return readList(-1, type);
          }

        case Hessian2Constants.BC_LIST_VARIABLE_UNTYPED:
          {
            return readList(-1, null);
          }

        case Hessian2Constants.BC_LIST_FIXED:
          {
            type = readType();
            length = readInt();

            return readLengthList(length, type);
          }

        case Hessian2Constants.BC_LIST_FIXED_UNTYPED:
          {
            length = readInt();

            return readLengthList(length, null);
          }

        // compact fixed list
        case 0x70: case 0x71: case 0x72: case 0x73:
        case 0x74: case 0x75: case 0x76: case 0x77:
          {
            type = readType();
            length = tag - 0x70;

            return readLengthList(length, type);
          }

        // compact fixed untyped list
        case 0x78: case 0x79: case 0x7a: case 0x7b:
        case 0x7c: case 0x7d: case 0x7e: case 0x7f:
          {
            length = tag - 0x78;

            return readLengthList(length, null);
          }

        case 'H'.charCodeAt(): 
          return readMap();

        case 'M'.charCodeAt(): 
          type = readType();

          var obj:Object = readMap(getClassDefinition(type));

          if (_addHessianTypeName
              && type != null 
              && type.length > 0)
            obj.hessianTypeName = type;

          return obj;

        case 'C'.charCodeAt(): 
          {
            readObjectDefinition();

            return readObject();
          }

        case 0x60: case 0x61: case 0x62: case 0x63:
        case 0x64: case 0x65: case 0x66: case 0x67:
        case 0x68: case 0x69: case 0x6a: case 0x6b:
        case 0x6c: case 0x6d: case 0x6e: case 0x6f:
          {
            ref = tag - 0x60;

            if (_classRefs == null)
              throw error("No classes defined at reference '{0}'" + tag);

            def = _classRefs[ref] as ObjectDefinition;

            return readObjectInstance(def);
          }

        case 'O'.charCodeAt(): 
          {
            ref = readInt();

            if (_classRefs == null)
              throw error("No classes defined at reference '{0}'" + tag);

            def = _classRefs[ref] as ObjectDefinition;

            return readObjectInstance(def);
          }

        case Hessian2Constants.BC_REF:
          {
            return _refs[readInt()];
          }

        default:
          throw error("unknown code: " + codeName(tag));
      }
    }

    /**
     * Reads an object definition.
     *
     * <p>
     *   <pre>
     *   O type <int> (string)* <value>*
     *   </pre>
     * </p>
     */
    private function readObjectDefinition():void
    {
      var type:String = readString();
      var len:int = readInt();

      var fieldNames:Array = new Array();
      for (var i:int = 0; i < len; i++)
        fieldNames.push(readString());

      var def:ObjectDefinition = new ObjectDefinition(type, fieldNames);

      if (_classRefs == null)
        _classRefs = new Array();

      _classRefs.push(def);
    }

    /**
     * Reads an object instance.
     *
     * @param def The ObjectDefinition on which to base the read.
     * @param expectedClass A class to instantiate.
     *
     * @return The object instance as read.
     *
     */
    private function readObjectInstance(def:ObjectDefinition,
                                        expectedClass:Class = null):Object
    {
      var type:String = def.type;
      var fieldNames:Array = def.fieldNames;
      var value:Object = null;

      var cl:Class = getClassDefinition(type, expectedClass);

      value = new cl();

      if (_addHessianTypeName
          && cl == Object 
          && type != null 
          && type.length > 0)
        value.hessianTypeName = type;

      addRef(value);

      for each (var key:String in fieldNames) {
        var lastValue:Object = readObject();

        try {
          value[key] = lastValue;
        }
        catch (e:TypeError) {
          // if we read an array and got a TypeError, it's possible that
          // the field is actually ArrayCollection and needs to be wrapped.
          // 
          // XXX Two other possible implementations would be :
          // 1) check the type of every variable before assignment, but this 
          // would punish all reads, not just ArrayCollections
          // 2) use object skeletons and field deserializers - a major
          // refactor and possible problems with dynamic nature of AS3
        
          if (lastValue is Array) {
            lastValue = new ArrayCollection(lastValue as Array);
            value[key] = lastValue;
          }
        }
      }

      return value;
    }

    private function readLenString(len:int = -1):String
    {
      if (len < 0)
        len = readInt();

      _isLastChunk = true;
      _chunkLength = len;

      _sbuf = "";

      var ch:int;

      while ((ch = parseChar()) >= 0)
        _sbuf += String.fromCharCode(ch);

      return _sbuf;
    }

    /**
     * Reads a reference.
     *
     * <p>
     *   <pre>
     *   R b32 b24 b16 b8
     *   </pre>
     * </p>
     * 
     * @return The object to which the read reference refers.
     */
    public override function readRef():Object
    {
      return _refs[parseInteger()];
    }

    /**
     * Reads the start of a list.
     */
    public override function readListStart():int
    {
      return read();
    }

    /**
     * Reads the start of a map.
     */
    public override function readMapStart():int
    {
      return read();
    }

    /**
     * Returns true if the data has ended.
     *
     * @return Whether the data has ended.
     */
    public override function isEnd():Boolean
    {
      var code:int;

      if (_offset < _length)
        code = (_buffer[_offset] & 0xff);
      else {
        code = read();

        if (code >= 0)
          _offset--;
      }

      return (code < 0 || code == 'Z'.charCodeAt());
    }

    /**
     * Read the end byte.
     */
    public override function readEnd():void
    {
      var code:int = _offset < _length ? (_buffer[_offset++] & 0xff) : read();

      if (code != 'Z'.charCodeAt())
        throw error("unknown code:" + String.fromCharCode(code));
    }

    /**
     * Read the end byte.
     */
    public override function readMapEnd():void
    {
      var code:int = _offset < _length ? (_buffer[_offset++] & 0xff) : read();

      if (code != 'Z'.charCodeAt())
        throw error("unknown code:" + String.fromCharCode(code));
    }

    /**
     * Read the end byte.
     */
    public override function readListEnd():void
    {
      var code:int = _offset < _length ? (_buffer[_offset++] & 0xff) : read();

      if (code != 'Z'.charCodeAt())
        throw error("unknown code:" + String.fromCharCode(code));
    }

    /**
     * Adds an object reference.
     *
     * @param obj The object to which to add the reference.
     *
     * @return The reference number.
     *
     */
    public override function addRef(obj:Object):int
    {
      if (_refs == null)
        _refs = new Array();

      _refs.push(obj);

      return _refs.length - 1;
    }

    /**
     * Sets an object reference.
     *
     * @param i The reference number.
     * @param obj The object to which to add the reference.
     */
    public override function setRef(i:int, obj:Object):void
    {
      _refs[i] = obj;
    }

    /**
     * Resets the references for streaming.
     */
    public override function resetReferences():void
    {
      if (_refs != null)
        _refs = new Array();
    }

    /**
     * Reads an object type.
     *
     * <pre>
     * type ::= string
     * type ::= int
     * </pre>
     *
     * @return The type value read as a String.
     */
    public override function readType():String
    {
      var code:int = _offset < _length ? (_buffer[_offset++] & 0xff) : read();
      _offset--;

      switch (code) {
        case 0x00: case 0x01: case 0x02: case 0x03:
        case 0x04: case 0x05: case 0x06: case 0x07:
        case 0x08: case 0x09: case 0x0a: case 0x0b:
        case 0x0c: case 0x0d: case 0x0e: case 0x0f:
        case 0x10: case 0x11: case 0x12: case 0x13:
        case 0x14: case 0x15: case 0x16: case 0x17:
        case 0x18: case 0x19: case 0x1a: case 0x1b:
        case 0x1c: case 0x1d: case 0x1e: case 0x1f:
        case 0x30: case 0x31: case 0x32: case 0x33:
        case Hessian2Constants.BC_STRING_CHUNK: case 'S':
          {
            var type:String = readString();

            if (_types == null)
              _types = new Array();

            _types.push(type);

            return type;
          }

        default:
          {
            var ref:int = readInt();

            if (_types == null || _types.size <= ref)
              throw new RangeError("type ref #" + ref + " is greater than the number of valid types (" + (_types == null ? 0 : _types.size) + ")");
          
            return _types[ref] as String;
          }
      }
    }

    /**
     * Parses the length for an array.
     *
     * <p>
     *   <pre>
     *   l b32 b24 b16 b8
     *   </pre>
     * </p>
     *
     * @return The length value read as an int.
     */
    public override function readLength():int
    {
      throw new IllegalOperationError();
    }

    /**
     * Parses a 32-bit integer value from the stream.
     *
     * <pre>
     * b32 b24 b16 b8
     * </pre>
     */
    private function parseInteger():int
    {
      var offset:int = _offset;
      var b32:int;
      var b24:int;
      var b16:int;
      var b8:int;      

      if (offset + 3 < _length) {
        var buffer:ByteArray = _buffer;
        
        b32 = buffer[offset + 0] & 0xff;
        b24 = buffer[offset + 1] & 0xff;
        b16 = buffer[offset + 2] & 0xff;
        b8 = buffer[offset + 3] & 0xff;

        _offset = offset + 4;
      }
      else {
        b32 = read();
        b24 = read();
        b16 = read();
        b8 = read();
      }

      return (b32 << 24) + (b24 << 16) + (b16 << 8) + b8;
    }

    /**
     * Parses a 64-bit long value from the stream.
     *
     * <pre>
     * b64 b56 b48 b40 b32 b24 b16 b8
     * </pre>
     */
    private function parseLong():Number
    {
      var b64:Number = read();
      var b56:Number = read();
      var b48:Number = read();
      var b40:Number = read();
      var b32:Number = read();
      var b24:Number = read();
      var b16:Number = read();
      var b8:Number = read();

      // The << operator in actionscript returns a 32-bit int so we can only
      // use it for the lowest 32 bits of any computation.  Note that we also 
      // have to be careful about the sign bit of the lower 32 bits: if the 
      // MSB of byte b32 is 1, then it can make the bottom 32 bits actually 
      // represent a negative number, so we really can only use << for the
      // lowest 24 bits.

      // Notice that we've also split the long into the most and least 
      // significant 32-bits.  This is because the arithmetic to deal with
      // 64-bit negative numbers doesn't always work because AS Numbers are
      // 64-bit floats underneath.  Thus we split and do 32-bit arithmetic
      // to avoid overflows.

      var msi:Number = (b64 * 0x1000000) +
                       (b56 << 16) +
                       (b48 << 8) +
                        b40;
      var lsi:Number = (b32 * 0x1000000) +
                       (b24 << 16) +
                       (b16 << 8) +
                        b8;

      var sign:Number = 1;

      if ((b64 & 0x80) != 0) {
        msi = 0xFFFFFFFF - msi;
        lsi = 0x100000000 - lsi;

        sign = -1;
      }

      return sign * (msi * 0x100000000 + lsi);
    }
  
    /**
     * Parses a 64-bit double value from the stream.
     *
     * <pre>
     * b64 b56 b48 b40 b32 b24 b16 b8
     * </pre>
     */
    private function parseDouble():Number
    {
      var buffer:ByteArray = new ByteArray();

      for (var i:int = 0; i < 8; i++)
        buffer.writeByte(read());

      buffer.position = 0;
    
      return buffer.readDouble();
    }

    /**
     * Reads a character from the underlying stream.
     */
    private function parseChar():int
    {
      while (_chunkLength <= 0) {
        if (_isLastChunk)
          return -1;

        var code:int = _offset < _length ? (_buffer[_offset++] & 0xff) : read();

        switch (code) {
          case Hessian2Constants.BC_STRING_CHUNK:
            _isLastChunk = false;

            _chunkLength = (read() << 8) + read();
            break;
            
          case 'S'.charCodeAt():
            _isLastChunk = true;

            _chunkLength = (read() << 8) + read();
            break;
      
          case 0x00: case 0x01: case 0x02: case 0x03:
          case 0x04: case 0x05: case 0x06: case 0x07:
          case 0x08: case 0x09: case 0x0a: case 0x0b:
          case 0x0c: case 0x0d: case 0x0e: case 0x0f:

          case 0x10: case 0x11: case 0x12: case 0x13:
          case 0x14: case 0x15: case 0x16: case 0x17:
          case 0x18: case 0x19: case 0x1a: case 0x1b:
          case 0x1c: case 0x1d: case 0x1e: case 0x1f:
            _isLastChunk = true;
            _chunkLength = code - 0x00;
            break;

          case 0x30: case 0x31: case 0x32: case 0x33:
            _isLastChunk = true;
            _chunkLength = ((code - 0x30) << 8) + read();
            break;

          default:
            throw expect("string", code);
        }
      }

      _chunkLength--;

      return parseUTF8Char();
    }

    /**
     * Parses a single UTF8 character.
     */
    private function parseUTF8Char():int
    {
      var ch:int = _offset < _length ? (_buffer[_offset++] & 0xff) : read();
      var ch1:int;

      if (ch < 0x80)
        return ch;

      else if ((ch & 0xe0) == 0xc0) {
        ch1 = read();

        return ((ch & 0x1f) << 6) + (ch1 & 0x3f);
      }

      else if ((ch & 0xf0) == 0xe0) {
        ch1 = read();
        var ch2:int = read();

        return ((ch & 0x0f) << 12) + ((ch1 & 0x3f) << 6) + (ch2 & 0x3f);
      }

      else
        throw error("bad utf-8 encoding");
    }
  
    /**
     * Reads a byte from the underlying stream.
     */
    private function parseByte():int
    {
      while (_chunkLength <= 0) {
        if (_isLastChunk)
          return -1;

        var code:int = read();

        switch (code) {
          case Hessian2Constants.BC_BINARY_CHUNK:
            _isLastChunk = false;

            _chunkLength = (read() << 8) + read();
            break;
            
          case 'B'.charCodeAt():
            _isLastChunk = true;

            _chunkLength = (read() << 8) + read();
            break;

          case 0x20: case 0x21: case 0x22: case 0x23:
          case 0x24: case 0x25: case 0x26: case 0x27:
          case 0x28: case 0x29: case 0x2a: case 0x2b:
          case 0x2c: case 0x2d: case 0x2e: case 0x2f:
            _isLastChunk = true;

            _chunkLength = code - 0x20;
            break;

          case 0x34: case 0x35: case 0x36: case 0x37:
            _isLastChunk = true;
            _chunkLength = (code - 0x34) * 256 + read();
            break;

          default:
            throw expect("byte[]", code);
        }
      }

      _chunkLength--;

      return read();
    }

    /**
     * Reads a byte from the stream.
     *
     * @return The byte read as a int.
     */
    public final function read():int
    {
      if (_length <= _offset && ! readBuffer())
        throw new EOFError();

      return _buffer[_offset++] & 0xff;
    }

    private function readBuffer():Boolean
    {
      var buffer:ByteArray = new ByteArray();
      var offset:int = _offset;
      var length:int = _length;
      var eof:Boolean = false;
    
      if (offset < length) {
        _buffer.readBytes(buffer, offset, length - offset);

        offset = length - offset;
      }
      else
        offset = 0;

      _buffer = buffer;

      var len:int = buffer.length;

      // XXX is this the right way to do it?  
      // Reading from a ByteArray requires this kind of nonsense, but 
      // will it work for a Socket or something else that actually streams?
      var readAmount:int = Math.min(_di.bytesAvailable, SIZE - offset);
      
      _di.readBytes(buffer, offset, readAmount);

      // calculate how many bytes were actually read in
      len = buffer.length - len;

      if (len <= 0) {
        _length = offset;
        _offset = 0;

        return offset > 0;
      }

      _length = offset + len;
      _offset = 0;

      return true;
    }

    /** @private */
    protected function expect(expect:String, ch:int):IOError
    {
      if (ch < 0)
        return error("expected " + expect + " at end of file");
      else
        return error("expected " + expect + " at " + ch);
    }
  
    /** @private */
    protected function error(msg:String):IOError
    {
      if (_method != null)
        return new HessianProtocolError(_method + ": " + msg);
      else
        return new HessianProtocolError(msg);
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

    // The following functions duplicate the functionality of the 
    // SerializerFactory infrastructure in the Java implementation

    private function readMap(cl:Class = null):Object
    {
      // have to do this because "Object" is not a compile-time constant
      if (cl == null)
        cl = Object;

      var obj:Object = new cl();

      addRef(obj);

      while (! isEnd()) {
        var key:String = String(readObject());
        obj[key] = readObject();
      }

      readMapEnd();

      return obj;
    }

    private function readList(length:int, 
                              type:String = null, 
                              expectedClass:Class = null):Object
    {
      var array:Array = new Array();
      var cl:Class = getClassDefinition(type, expectedClass);

      addRef(array);

      if (length >= 0) {
        for (var i:int = 0; i < length; i++)
          array.push(readObject(cl));

        readListEnd();
      }
      else {
        while (! isEnd())
          array.push(readObject(cl));

        readListEnd();
      }

      return array;
    }

    private function readLengthList(length:int, 
                                    type:String = null, 
                                    expectedClass:Class = null):Object
    {
      var array:Array = new Array();
      var cl:Class = getClassDefinition(type, expectedClass);

      addRef(array);

      for (var i:int = 0; i < length; i++)
        array.push(readObject(cl));

      return array;
    }

    private function getClassDefinition(type:String, 
                                        expectedClass:Class = null):Class
    {
      var cl:Class = null;

      if (type != null && type.length > 0) {
        try {
          cl = getClassByAlias(type) as Class;
          trace("Found class " + cl + " by alias " + type);
        }
        catch (e:Error) {
          trace("Cannot find class by alias '" + type + "': " + e);
        }

        if (cl == null) {
          try {
            cl = getDefinitionByName(type) as Class;

            if (cl != null) {
              trace("Found class " + cl + " by alias " + type);

              if (_autoAlias)
                registerClassAlias(type, cl);
            }
          }
          catch (e:Error) {
            trace("Cannot file class by name '" + type + "': " + e);
          }
        }
      }

      if (cl == null)
        cl = expectedClass;

      if (cl == null)
        cl = Object;

      return cl;
    }
  }
}
