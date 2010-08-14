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
 */

package hessian.io
{
	import flash.errors.IllegalOperationError;
	import flash.utils.ByteArray;
	import flash.utils.IDataOutput;

  internal class AbstractHessianOutput 
  {
    /**
     * Initialize the Hessian stream with the underlying input stream.
     */
    public function init(out:IDataOutput):void
    {
    }

    /**
     * Writes a complete method call.
     */
    public function call(method:String, args:Array):void 
    {
      var length:int = args != null ? args.length : 0;

      startCall(method, length);

      if (args != null) {
        for (var i:int = 0; i < args.length; i++)
          writeObject(args[i]);
      }

      completeCall();
    }

    public function startCall(method:String, length:int):void
    {
      throw new IllegalOperationError();
    }

    /**
     * Writes a header name.  The header value must immediately follow.
     *
     * <code><pre>
     * H b16 b8 foo <em>value</em>
     * </pre></code>
     */
    public function writeHeader(name:String):void
    {
      throw new IllegalOperationError();
    }

    /**
     * Completes the method call:
     *
     * <code><pre>
     * z
     * </pre></code>
     */
    public function completeCall():void
    {
      throw new IllegalOperationError();
    }

    /**
     * Writes a boolean value to the stream.  The boolean will be written
     * with the following syntax:
     *
     * <code><pre>
     * T
     * F
     * </pre></code>
     *
     * @param value the boolean value to write.
     */
    public function writeBoolean(value:Boolean):void
    {
      throw new IllegalOperationError();
    }

    /**
     * Writes an integer value to the stream.  The integer will be written
     * with the following syntax:
     *
     * <code><pre>
     * I b32 b24 b16 b8
     * </pre></code>
     *
     * @param value the integer value to write.
     */
    public function writeInt(value:int):void
    {
      throw new IllegalOperationError();
    }

    /**
     * Writes a long value to the stream.  The long will be written
     * with the following syntax:
     *
     * <code><pre>
     * L b64 b56 b48 b40 b32 b24 b16 b8
     * </pre></code>
     *
     * @param value the long value to write.
     */
    public function writeLong(value:Number):void
    {
      throw new IllegalOperationError();
    }

    /**
     * Writes a double value to the stream.  The double will be written
     * with the following syntax:
     *
     * <code><pre>
     * D b64 b56 b48 b40 b32 b24 b16 b8
     * </pre></code>
     *
     * @param value the double value to write.
     */
    public function writeDouble(value:Number):void
    {
      throw new IllegalOperationError();
    }

    /**
     * Writes a date to the stream.
     *
     * <code><pre>
     * T  b64 b56 b48 b40 b32 b24 b16 b8
     * </pre></code>
     *
     * @param time the date in milliseconds from the epoch in UTC
     */
    public function writeUTCDate(time:Number):void
    {
      throw new IllegalOperationError();
    }

    /**
     * Writes a null value to the stream.
     * The null will be written with the following syntax
     *
     * <code><pre>
     * N
     * </pre></code>
     *
     * @param value the string value to write.
     */
    public function writeNull():void
    {
      throw new IllegalOperationError();
    }

    /**
     * Writes a string value to the stream using UTF-8 encoding.
     * The string will be written with the following syntax:
     *
     * <code><pre>
     * S b16 b8 string-value
     * </pre></code>
     *
     * If the value is null, it will be written as
     *
     * <code><pre>
     * N
     * </pre></code>
     *
     * @param value the string value to write.
     */
    public function writeString(value:*, offset:int = 0, length:int = 0):void
    {
      throw new IllegalOperationError();
    }

    /**
     * Writes a byte array to the stream.
     * The array will be written with the following syntax:
     *
     * <code><pre>
     * B b16 b18 bytes
     * </pre></code>
     *
     * If the value is null, it will be written as
     *
     * <code><pre>
     * N
     * </pre></code>
     *
     * @param value the string value to write.
     */
    public function writeBytes(buffer:ByteArray, 
                               offset:int = 0, 
                               length:int = -1):void
    {
      throw new IllegalOperationError();
    }
  
    /**
     * Writes a byte buffer to the stream.
     */
    public function writeByteBufferStart():void
    {
      throw new IllegalOperationError();
    }
  
    /**
     * Writes a byte buffer to the stream.
     *
     * <code><pre>
     * b b16 b18 bytes
     * </pre></code>
     *
     * @param value the string value to write.
     */
    public function writeByteBufferPart(buffer:ByteArray,
                                        offset:int,
                                        length:int):void
    {
      throw new IllegalOperationError();
    }
  
    /**
     * Writes the last chunk of a byte buffer to the stream.
     *
     * <code><pre>
     * b b16 b18 bytes
     * </pre></code>
     *
     * @param value the string value to write.
     */
    public function writeByteBufferEnd(buffer:ByteArray,
                                       offset:int,
                                       length:int):void
    {
      throw new IllegalOperationError();
    }

    /**
     * Writes a reference.
     *
     * <code><pre>
     * R b32 b24 b16 b8
     * </pre></code>
     *
     * @param value the integer value to write.
     */
    public function writeRef(value:int):void
    {
      throw new IllegalOperationError();
    }

    /**
     * Removes a reference.
     */
    public function removeRef(obj:Object):Boolean
    {
      throw new IllegalOperationError();
    }

    /**
     * Replaces a reference from one object to another.
     */
    public function replaceRef(oldRef:Object, newRef:Object):Boolean
    {
      throw new IllegalOperationError();
    }

    /**
     * Adds an object to the reference list.  If the object already exists,
     * writes the reference, otherwise, the caller is responsible for
     * the serialization.
     *
     * <code><pre>
     * R b32 b24 b16 b8
     * </pre></code>
     *
     * @param object the object to add as a reference.
     *
     * @return true if the object has already been written.
     */
    public function addRef(object:Object):Boolean
    {
      throw new IllegalOperationError();
    }

    /**
     * Resets the references for streaming.
     */
    public function resetReferences():void
    {
    }

    /**
     * Writes a generic object to the output stream.
     */
    public function writeObject(object:Object, className:String = null):void
    {
      throw new IllegalOperationError();
    }

    /**
     * Writes the list header to the stream.  List writers will call
     * <code>writeListBegin</code> followed by the list contents and then
     * call <code>writeListEnd</code>.
     *
     * <code><pre>
     * &lt;list>
     *   &lt;type>java.util.ArrayList&lt;/type>
     *   &lt;length>3&lt;/length>
     *   &lt;int>1&lt;/int>
     *   &lt;int>2&lt;/int>
     *   &lt;int>3&lt;/int>
     * &lt;/list>
     * </pre></code>
     */
    public function writeListBegin(length:int, type:String = null):Boolean
    {
      throw new IllegalOperationError();
    }

    /**
     * Writes the tail of the list to the stream.
     */
    public function writeListEnd():void
    {
      throw new IllegalOperationError();
    }

    /**
     * Writes the map header to the stream.  Map writers will call
     * <code>writeMapBegin</code> followed by the map contents and then
     * call <code>writeMapEnd</code>.
     *
     * <code><pre>
     * Mt b16 b8 type (<key> <value>)z
     * </pre></code>
     */
    public function writeMapBegin(type:String):void
    {
      throw new IllegalOperationError();
    }

    /**
     * Writes the tail of the map to the stream.
     */
    public function writeMapEnd():void
    {
      throw new IllegalOperationError();
    }

    /**
     * Writes the object header to the stream (for Hessian 2.0), or a
     * Map for Hessian 1.0.  Object writers will call
     * <code>writeObjectBegin</code> followed by the map contents and then
     * call <code>writeObjectEnd</code>.
     *
     * <code><pre>
     * Ot b16 b8 type (<key> <value>)* z
     * o b32 b24 b16 b8 <value>* z
     * </pre></code>
     *
     * @return true if the object has already been defined.
     */
    public function writeObjectBegin(className:String, 
                                     type:XML):ObjectDefinition
    {
      writeMapBegin(className);

      return null;
    }

    /**
     * Writes the end of the class.
     */
    public function writeClassFieldLength(len:int):void
    {
    }

    /**
     * Writes the tail of the object to the stream.
     */
    public function writeObjectEnd():void
    {
    }

    /**
     * Writes a remote object reference to the stream.  The type is the
     * type of the remote interface.
     *
     * <code><pre>
     * 'r' 't' b16 b8 type url
     * </pre></code>
     */
    public function writeRemote(type:String, url:String):void
    {
      throw new IllegalOperationError();
    }

    public function writeReply(o:Object):void
    {
      startReply();
      writeObject(o);
      completeReply();
    }

    public function startReply():void
    {
    }

    public function completeReply():void
    {
    }

    public function writeFault(code:String, message:String, detail:Object):void
    {
    }
  }
}
