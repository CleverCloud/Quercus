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

package hessian.io
{
	import flash.errors.IllegalOperationError;
	import flash.utils.ByteArray;
	import flash.utils.IDataInput;

  internal class AbstractHessianInput 
  {
    /**
     * Initialize the Hessian stream with the underlying input stream.
     */
    public function init(di:IDataInput):void
    {
    }

    /**
     * Returns the call's method
     */
    public function getMethod():String
    {
      throw new IllegalOperationError();
    }

    /**
     * Reads the call
     *
     * <pre>
     * c major minor
     * </pre>
     */
    public function readCall():int
    {
      throw new IllegalOperationError();
    }

    /**
     * For backward compatibility with HessianSkeleton
     */
    public function skipOptionalCall():void
    {
    }

    /**
     * Reads a header, returning null if there are no headers.
     *
     * <p>
     *   <pre>
     *   H b16 b8 value
     *   </pre>
     * </p>
     */
    public function readHeader():String 
    {
      throw new IllegalOperationError();
    }

    /**
     * Starts reading the call
     *
     * <p>A successful completion will have a single value:
     *
     *   <pre>
     *   m b16 b8 method
     *   </pre>
     * </p>
     */
    public function readMethod():String 
    {
      throw new IllegalOperationError();
    }

    /**
     * Reads the number of method arguments
     *
     * @return -1 for a variable length (hessian 1.0)
     */
    public function readMethodArgLength():int
    {
      return -1;
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
    public function startCall():void
    {
      throw new IllegalOperationError();
    }

    /**
     * Completes reading the call
     *
     * <p>
     *   The call expects the following protocol data
     *
     *   <pre>
     *   Z
     *   </pre>
     * </p>
     */
    public function completeCall():void
    {
      throw new IllegalOperationError();
    }

    /**
     * Reads a reply as an object.
     * If the reply has a fault, throws the exception.
     */
    public function readReply(expectedClass:Class = null):Object 
    {
      throw new IllegalOperationError();
    }

    /**
     * Starts reading the reply
     *
     * <p>A successful completion will have a single value:
     *
     * <pre>
     * r
     * v
     * </pre>
     */
    public function startReply():void
    {
      throw new IllegalOperationError();
    }

    /**
     * Completes reading the call
     *
     * <p>A successful completion will have a single value:
     *
     * <pre>
     * z
     * </pre>
     */
    public function completeReply():void
    {
      throw new IllegalOperationError();
    }

    /**
     * Reads a boolean
     *
     * <pre>
     * T
     * F
     * </pre>
     */
    public function readBoolean():Boolean 
    {
      throw new IllegalOperationError();
    }

    /**
     * Reads a null
     *
     * <pre>
     * N
     * </pre>
     */
    public function readNull():void
    {
      throw new IllegalOperationError();
    }

    /**
     * Reads an integer
     *
     * <pre>
     * I b32 b24 b16 b8
     * </pre>
     */
    public function readInt():int
    {
      throw new IllegalOperationError();
    }

    /**
     * Reads a long
     *
     * <pre>
     * L b64 b56 b48 b40 b32 b24 b16 b8
     * </pre>
     */
    public function readLong():Number
    {
      throw new IllegalOperationError();
    }

    /**
     * Reads a double.
     *
     * <pre>
     * D b64 b56 b48 b40 b32 b24 b16 b8
     * </pre>
     */
    public function readDouble():Number
    {
      throw new IllegalOperationError();
    }

    /**
     * Reads a date.
     *
     * <pre>
     * T b64 b56 b48 b40 b32 b24 b16 b8
     * </pre>
     */
    public function readUTCDate():Number
    {
      throw new IllegalOperationError();
    }

    /**
     * Reads a string encoded in UTF-8
     *
     * <pre>
     * s b16 b8 non-final string chunk
     * S b16 b8 final string chunk
     * </pre>
     */
    public function readString():String
    {
      throw new IllegalOperationError();
    }

    /**
     * Reads a byte array.
     *
     * <pre>
     * b b16 b8 non-final binary chunk
     * B b16 b8 final binary chunk
     * </pre>
     */
    public function readBytes():ByteArray
    {
      throw new IllegalOperationError();
    }

    /**
     * Reads an arbitrary object from the input stream.
     *
     * @param expectedClass the expected class if the protocol doesn't supply it.
     */
    public function readObject(expectedClass:Class = null):Object
    {
      throw new IllegalOperationError();
    }

    /**
     * Reads a reference
     *
     * <pre>
     * R b32 b24 b16 b8
     * </pre>
     */
    public function readRef():Object
    {
      throw new IllegalOperationError();
    }

    /**
     * Adds an object reference.
     */
    public function addRef(obj:Object):int
    {
      throw new IllegalOperationError();
    }

    /**
     * Sets an object reference.
     */
    public function setRef(i:int, obj:Object):void
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
     * Reads the start of a list
     */
    public function readListStart():int
    {
      throw new IllegalOperationError();
    }

    /**
     * Reads the length of a list.
     */
    public function readLength():int
    {
      throw new IllegalOperationError();
    }

    /**
     * Reads the start of a map
     */
    public function readMapStart():int
    {
      throw new IllegalOperationError();
    }

    /**
     * Reads an object type.
     */
    public function readType():String
    {
      throw new IllegalOperationError();
    }

    /**
     * Returns true if the data has ended.
     */
    public function isEnd():Boolean
    {
      throw new IllegalOperationError();
    }

    /**
     * Read the end byte
     */
    public function readEnd():void
    {
      throw new IllegalOperationError();
    }

    /**
     * Read the end byte
     */
    public function readMapEnd():void
    {
      throw new IllegalOperationError();
    }

    /**
     * Read the end byte
     */
    public function readListEnd():void
    {
      throw new IllegalOperationError();
    }

  }
}
