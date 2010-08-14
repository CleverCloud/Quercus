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
	import flash.utils.Dictionary;
	import flash.utils.IDataOutput;
  import flash.utils.describeType;
  import flash.utils.getQualifiedClassName;

  import hessian.util.IntrospectionUtil;

  /**
   * A writer for the Hessian 1.0 protocol.  A Hessian 2.0 compatible reader
   * must be able to read the output of this implementation.
   *
   */
  public class HessianOutput extends AbstractHessianOutput
  {
    private var _out:IDataOutput;
    private var _version:int = 1;
    private var _refs:Dictionary;
    private var _numRefs:int = 0;

    /**
     * Creates a new HessianOutput.
     *
     * @param out The IDataOutput to which this HessianOutput will write.
     *
     * @see #init(IDataOutput)
     *
     */
    public function HessianOutput(out:IDataOutput = null)
    {
      init(out);
    }

    /**
     * Initialize the Hessian stream with the underlying IDataOutput.  This
     * method will reset the internal data of this instance, meaning this
     * HessianOutput may be reused.
     *
     * @param out The IDataOutput to which this HessianOutput will write.
     */
    public override function init(out:IDataOutput):void
    {
      _out = out;
      resetReferences();
    }

    /**
     * Starts the method call.
     *
     * <p>
     *   <code><pre>
     *   c major minor
     *   m b16 b8 method-namek
     *   </pre></code>
     * </p>
     *
     * @param method The method name to call.
    public override function startCall(method:String = null):void
    {
      if (method == null) {
        _out.writeByte('c'.charCodeAt());
        _out.writeByte(0);
        _out.writeByte(1);
      }
      else {
        _out.writeByte('c'.charCodeAt());
        _out.writeByte(_version);
        _out.writeByte(0);

        _out.writeByte('m'.charCodeAt());
        var len:int = method.length;
        _out.writeByte(len >> 8);
        _out.writeByte(len);
        printString(method, 0, len);
      }
    }
     */

    /**
     * Writes the method tag.
     *
     * <p>
     *   <code><pre>
     *   m b16 b8 method-name
     *   </pre></code>
     * </p>
     *
     * @param method The method name to call.
     */
    public function writeMethod(method:String):void
    {
      _out.writeByte('m'.charCodeAt());
      var len:int = method.length;
      _out.writeByte(len >> 8);
      _out.writeByte(len);
      printString(method, 0, len);
    }

    /**
     * Completes the method call.
     *
     * <p>
     *   <code><pre>
     *   z
     *   </pre></code>
     * </p>
     *
     */
    public override function completeCall():void
    {
      _out.writeByte('z'.charCodeAt());
    }

    /**
     * Starts the reply.
     *
     * <p>A successful completion will have a single value:
     *   <pre>
     *   r
     *   </pre>
     * </p>
     */
    public override function startReply():void
    {
      _out.writeByte('r'.charCodeAt());
      _out.writeByte(1);
      _out.writeByte(0);
    }

    /**
     * Completes reading the reply.
     *
     * <p>A successful completion will have a single value:
     *   <pre>
     *   z
     *   </pre>
     * </p>
     */
    public override function completeReply():void
    {
      _out.writeByte('z'.charCodeAt());
    }

    /**
     * Writes a header name.  The header value must immediately follow.
     *
     * <p>
     *   <code><pre>
     *   H b16 b8 foo <em>value</em>
     *   </pre></code>
     * </p>
     *
     * @param name The header name.
     */
    public override function writeHeader(name:String):void
    {
      var len:int = name.length;

      _out.writeByte('H'.charCodeAt());
      _out.writeByte(len >> 8);
      _out.writeByte(len);

      printString(name);
    }

    /**
     * Writes a fault.  
     *
     * <p>
     *   The fault will be written
     *   as a descriptive string followed by an object:
     *   <code><pre>
     *   f
     *   &lt;string>code
     *   &lt;string>the fault code
     *
     *   &lt;string>message
     *   &lt;string>the fault mesage
     *
     *   &lt;string>detail
     *   mt\x00\xnnjavax.ejb.FinderException
     *       ...
     *   z
     *   z
     *   </pre></code>
     * </p>
     *
     * @param code The fault code, a three digit number.
     * @param message The fault message.
     * @param detail The fault detail.
     */
    public override function writeFault(code:String, 
                                        message:String, 
                                        detail:Object):void
    {
      _out.writeByte('f'.charCodeAt());
      writeString("code");
      writeString(code);

      writeString("message");
      writeString(message);

      if (detail != null) {
        writeString("detail");
        writeObject(detail);
      }
      _out.writeByte('z'.charCodeAt());
    }

    /**
     * Writes a generic object to the output stream.
     *
     * @param object The object to write.
     * @param className The name of the class to write to the stream.
     *                  May be the name of a primitive or user created class.
     */
    public override function writeObject(object:Object, 
                                         className:String = null):void
    {
      if (object == null) {
        writeNull();
        return;
      }

      if (object is Boolean || className == "Boolean") {
        writeBoolean(object as Boolean);
        return;
      }
      else if (className == "Number") {
        writeDouble(object as Number); // XXX should this be writeLong?
        return;
      }
      else if (className == "Double") {
        writeDouble(object as Number); // special hack for double
        return;
      }
      else if (className == "Long") {
        writeLong(object as Number); // special hack for long 
        return;
      }
      else if (object is int || className == "int") {
        writeInt(object as int);
        return;
      }
      else if (object is Number) {
        if (isNaN(Number(object))) {
          writeNull();
        } 
        else {
          writeDouble(object as Number); // XXX should this be writeLong?
        }
        return;
      }
      else if (object is Date || className == "Date") {
        writeUTCDate((object as Date).valueOf());
        return;
      }
      else if (object is Array || className == "Array") {
        if (addRef(object))
          return;

        var array:Array = object as Array;

        var hasEnd:Boolean = writeListBegin(array.length, className);

        for (var i:int = 0; i < array.length; i++)
          writeObject(array[i]);

        if (hasEnd)
          writeListEnd();

        return;
      }
      else if (object is String || className == "String") {
        writeString(object as String);
        return;
      }
      else if (object is ByteArray || className == "ByteArray") {
        writeBytes(object as ByteArray);
        return;
      }

      /* XXX: Figure out how to get a real associative array in AS that 
         associates objects by their value rather than their toString value
      if (addRef(object))
        return;*/

      // writeReplace not supported at this time
      // to save processing time

      var type:XML = describeType(object);

      if (type.@alias != null && type.@alias.length() != 0)
        className = type.@alias;
      else if (object.hasOwnProperty("hessianTypeName"))
        className = object.hessianTypeName;
      else {
        className = getQualifiedClassName(object) as String;
        className = className.replace("::", ".");
      }

      writeObjectBegin(className, type);
      writeObject10(object, type);
    }

    private function writeObject10(obj:Object, type:XML):void
    {
      var metadata:XMLList = null;
      var variables:XMLList = type.variable;
      var accessors:XMLList = type.accessor;	

      var key:String = null;

      for each(var variable:XML in variables) {
        metadata = variable.metadata;

        if (IntrospectionUtil.getMetadata(metadata, "Transient") != null)
          continue;

        key = variable.@name;
        if (key != "hessianTypeName") {
          writeObject(key);
          writeObject(obj[key]);
        }
      }

      // This is needed to handle Bindable properties:
      // they do not appear as variables, but rather as 
      // <accessor>'s with Bindable metadata children
      for each(var accessor:XML in accessors) {
        metadata = accessor.metadata;

        if (IntrospectionUtil.getMetadata(metadata, "Transient") != null)
          continue;

        if (IntrospectionUtil.getMetadata(metadata, "Bindable") != null) {
          key = accessor.@name;

          if (key != "hessianTypeName") {
            writeObject(key);
            writeObject(obj[key]);
          }
        }
      }

      for (key in obj) {
        if (key != "hessianTypeName") {
          writeObject(key);
          writeObject(obj[key]);
        }
      }

      writeMapEnd();
    }

    /**
     * Writes the list header to the stream.  List writers will call
     * <code>writeListBegin</code> followed by the list contents and then
     * call <code>writeListEnd</code>.
     *
     * <p>
     *   <code><pre>
     *   &lt;list>
     *     &lt;type>java.util.ArrayList&lt;/type>
     *     &lt;length>3&lt;/length>
     *     &lt;int>1&lt;/int>
     *     &lt;int>2&lt;/int>
     *     &lt;int>3&lt;/int>
     *   &lt;/list>
     *   </pre></code>
     * </p>
     * 
     * @param length The length of the list.
     * @param type The type of the elements in the list.
     *
     * @return If this list will have an end.
     */
    public override function writeListBegin(length:int, 
                                            type:String = null):Boolean
    {
      _out.writeByte('V'.charCodeAt());

      if (type != null) {
        _out.writeByte('t'.charCodeAt());
        printLenString(type);
      }

      if (length >= 0) {
        _out.writeByte('l'.charCodeAt());
        _out.writeByte(length >> 24);
        _out.writeByte(length >> 16);
        _out.writeByte(length >> 8);
        _out.writeByte(length);
      }

      return true;
    }

    /**
     * Writes the tail of the list to the stream.
     */
    public override function writeListEnd():void
    {
      _out.writeByte('z'.charCodeAt());
    }

    /**
     * Writes the map header to the stream.  Map writers will call
     * <code>writeMapBegin</code> followed by the map contents and then
     * call <code>writeMapEnd</code>.
     *
     * <p>
     *   <code><pre>
     *   Mt b16 b8 type (<key> <value>)z
     *   </pre></code>
     * </p>
     *
     * @param type The type of the map to write.
     */
    public override function writeMapBegin(type:String):void
    {
      _out.writeByte('M'.charCodeAt());
      _out.writeByte('t'.charCodeAt());

      if (type == null || type == "Object")
        type = "";

      printLenString(type);
    }

    /**
     * Writes the tail of the map to the stream.
     */
    public override function writeMapEnd():void
    {
      _out.writeByte('z'.charCodeAt());
    }

    /**
     * Writes a boolean value to the stream.  The boolean will be written
     * with the following syntax:
     *
     * <p>
     *   <code><pre>
     *   T
     *   F
     *   </pre></code>
     * </p>
     *
     * @param value The boolean value to write.
     */
    public override function writeBoolean(value:Boolean):void
    {
      if (value)
        _out.writeByte('T'.charCodeAt());
      else
        _out.writeByte('F'.charCodeAt());
    }

    /**
     * Writes an integer value to the stream.  The integer will be written
     * with the following syntax:
     *
     * <p>
     *   <code><pre>
     *   I b32 b24 b16 b8
     *   </pre></code>
     * </p>
     *
     * @param value The integer value to write.
     */
    public override function writeInt(value:int):void
    {
      _out.writeByte('I'.charCodeAt());
      _out.writeByte(value >> 24);
      _out.writeByte(value >> 16);
      _out.writeByte(value >> 8);
      _out.writeByte(value);
    }

    /**
     * Writes a long value to the stream.  The long will be written
     * with the following syntax:
     *
     * <p>
     *   <code><pre>
     *   L b64 b56 b48 b40 b32 b24 b16 b8
     *   </pre></code>
     * </p>
     *
     * @param value The long value to write.
     */
    public override function writeLong(value:Number):void
    {
      _out.writeByte('L'.charCodeAt());
      _out.writeByte(0xFF & (value >> 56));
      _out.writeByte(0xFF & (value >> 48));
      _out.writeByte(0xFF & (value >> 40));
      _out.writeByte(0xFF & (value >> 32));
      _out.writeByte(0xFF & (value >> 24));
      _out.writeByte(0xFF & (value >> 16));
      _out.writeByte(0xFF & (value >> 8));
      _out.writeByte(0xFF & (value));
    }

    /**
     * Writes a double value to the stream.  The double will be written
     * with the following syntax:
     *
     * <p>
     *   <code><pre>
     *   D b64 b56 b48 b40 b32 b24 b16 b8
     *   </pre></code>
     * </p>
     *
     * @param value The double value to write.
     */
    public override function writeDouble(value:Number):void
    {
      var bits:ByteArray = Double.doubleToLongBits(value);

      _out.writeByte('D'.charCodeAt());
      _out.writeBytes(bits);
    }

    /**
     * Writes a date to the stream.
     *
     * <p>
     *   <code><pre>
     *   T  b64 b56 b48 b40 b32 b24 b16 b8
     *   </pre></code>
     * </p>
     *
     * @param time The date in milliseconds from the epoch in UTC
     */
    public override function writeUTCDate(time:Number):void
    {
      _out.writeByte('d'.charCodeAt());

      if (time >= 0) {
        _out.writeByte(0xFF & (time / 0x100000000000000));
        _out.writeByte(0xFF & (time / 0x1000000000000));
        _out.writeByte(0xFF & (time / 0x10000000000));
        _out.writeByte(0xFF & (time / 0x100000000));
        _out.writeByte(0xFF & (time >> 24));
        _out.writeByte(0xFF & (time >> 16));
        _out.writeByte(0xFF & (time >> 8));
        _out.writeByte(0xFF & (time));
      }
      else {
        var abs:Number = Math.abs(time);

        var msi:Number = 0x100000000 - Math.abs(abs / 0x100000000);
        if (msi == 0x100000000)
          msi = 0xFFFFFFFF;

        var lsi:Number = 0x100000000 - (abs % 0x100000000);

        _out.writeByte(0xFF & (msi >> 24));
        _out.writeByte(0xFF & (msi >> 16));
        _out.writeByte(0xFF & (msi >> 8));
        _out.writeByte(0xFF & (msi));

        _out.writeByte(0xFF & (lsi >> 24));
        _out.writeByte(0xFF & (lsi >> 16));
        _out.writeByte(0xFF & (lsi >> 8));
        _out.writeByte(0xFF & (lsi));
      }
    }

    /**
     * Writes a null value to the stream.
     * The null will be written with the following syntax
     *
     * <p>
     *   <code><pre>
     *   N
     *   </pre></code>
     * </p>
     */
    public override function writeNull():void
    {
      _out.writeByte('N'.charCodeAt());
    }

    /**
     * Writes a string value to the stream using UTF-8 encoding.
     * The string will be written with the following syntax:
     *
     * <p>
     *   <code><pre>
     *   S b16 b8 string-value
     *   </pre></code>
     * </p>
     *
     * <p>
     *   If the value is null, it will be written as
     *
     *   <code><pre>
     *   N
     *   </pre></code>
     * </p>
     *
     * @param value The string value to write.
     * @param offset The offset in the string of where to start.
     * @param length The length in the string to write.
     */
    public override function writeString(value:*, 
                                         offset:int = 0, 
                                         length:int = 0):void
    {
      if (value == null)
        _out.writeByte('N'.charCodeAt());

      else if (value is Array)
        writeCharArray(value as Array, offset, length);

      else {
        length = value.length;
        offset = 0;

        while (length > 0x8000) {
          var sublen:int = 0x8000;

          // chunk can't end in high surrogate
          var tail:int = value.charCodeAt(offset + sublen - 1);

          if (0xd800 <= tail && tail <= 0xdbff)
            sublen--;

          _out.writeByte('s'.charCodeAt());
          _out.writeByte(sublen >> 8);
          _out.writeByte(sublen);

          printString(value, offset, sublen);

          length -= sublen;
          offset += sublen;
        }

        _out.writeByte('S'.charCodeAt());
        _out.writeByte(length >> 8);
        _out.writeByte(length);

        printString(value, offset, length);
      }
    }

    /**
     * Writes a string value to the stream using UTF-8 encoding.
     * The string will be written with the following syntax:
     *
     * <p>
     *   <code><pre>
     *   S b16 b8 string-value
     *   </pre></code>
     * </p>
     *
     * <p>
     *   If the value is null, it will be written as
     *
     *   <code><pre>
     *   N
     *   </pre></code>
     * </p>
     *
     * @param buffer The character buffer value to write.
     * @param offset The offset in the array of where to start.
     * @param length The length in the array to write.
     */
    private function writeCharArray(buffer:Array, offset:int, length:int):void
    {
      while (length > 0x8000) {
        var sublen:int = 0x8000;

        // chunk can't end in high surrogate
        var tail:int = buffer[offset + sublen - 1];

        if (0xd800 <= tail && tail <= 0xdbff)
          sublen--;

        _out.writeByte('s'.charCodeAt());
        _out.writeByte(sublen >> 8);
        _out.writeByte(sublen);

        printCharArray(buffer, offset, sublen);

        length -= sublen;
        offset += sublen;
      }

      _out.writeByte('S'.charCodeAt());
      _out.writeByte(length >> 8);
      _out.writeByte(length);

      printCharArray(buffer, offset, length);
    }

    /**
     * Writes a byte array to the stream.
     * The array will be written with the following syntax:
     *
     * <p>
     *   <code><pre>
     *   B b16 b18 bytes
     *   </pre></code>
     * </p>
     *
     * <p>
     *   If the value is null, it will be written as
     *
     *   <code><pre>
     *   N
     *   </pre></code>
     * </p>
     *
     * @param buffer The byte buffer value to write.
     * @param offset The offset in the array of where to start.
     * @param length The length in the array to write.
     */
    public override function writeBytes(buffer:ByteArray, 
                                        offset:int = 0, 
                                        length:int = -1):void
    {
      if (buffer == null)
        _out.writeByte('N'.charCodeAt());

      else {
        if (length < 0)
          length = buffer.length;

        while (length > 0x8000) {
          var sublen:int = 0x8000;

          _out.writeByte('b'.charCodeAt());
          _out.writeByte(sublen >> 8);
          _out.writeByte(sublen);

          _out.writeBytes(buffer, offset, sublen);

          length -= sublen;
          offset += sublen;
        }

        _out.writeByte('B'.charCodeAt());
        _out.writeByte(length >> 8);
        _out.writeByte(length);
        _out.writeBytes(buffer, offset, length);
      }
    }
  
    /**
     * Writes a byte buffer to the stream.
     */
    public override function writeByteBufferStart():void
    {
    }
  
    /**
     * Writes a byte buffer to the stream.
     *
     * <p>
     *   <code><pre>
     *   b b16 b18 bytes
     *   </pre></code>
     * </p>
     *
     * @param buffer The byte buffer value to write.
     * @param offset The offset in the array of where to start.
     * @param length The length in the array to write.
     */
    public override function writeByteBufferPart(buffer:ByteArray,
                                                 offset:int,
                                                 length:int):void
    {
      while (length > 0) {
        var sublen:int = length;

        if (0x8000 < sublen)
          sublen = 0x8000;

        _out.writeByte('b'.charCodeAt());
        _out.writeByte(sublen >> 8);
        _out.writeByte(sublen);

        _out.writeBytes(buffer, offset, sublen);

        length -= sublen;
        offset += sublen;
      }
    }
  
    /**
     * Writes the last chunk of a byte buffer to the stream.
     *
     * <p>
     *   <code><pre>
     *   b b16 b18 bytes
     *   </pre></code>
     * </p>
     *
     * @param buffer The byte buffer value to write.
     * @param offset The offset in the array of where to start.
     * @param length The length in the array to write.
     */
    public override function writeByteBufferEnd(buffer:ByteArray,
                                                offset:int,
                                                length:int):void
    {
      writeBytes(buffer, offset, length);
    }

    /**
     * Writes a reference.
     *
     * <p>
     *   <code><pre>
     *   R b32 b24 b16 b8
     *   </pre></code>
     * </p>
     *
     * @param value The integer value to write.
     */
    public override function writeRef(value:int):void
    {
      _out.writeByte('R'.charCodeAt());
      _out.writeByte(value >> 24);
      _out.writeByte(value >> 16);
      _out.writeByte(value >> 8);
      _out.writeByte(value);
    }

    /**
     * Adds an object to the reference list.  If the object already exists,
     * writes the reference, otherwise, the caller is responsible for
     * the serialization.
     *
     * <p>
     *   <code><pre>
     *   R b32 b24 b16 b8
     *   </pre></code>
     * </p>
     *
     * @param object The object to add as a reference.
     *
     * @return true if the object has already been written.
     */
    public override function addRef(object:Object):Boolean
    {
      if (_refs == null)
        _refs = new Dictionary();

      var ref:Object = _refs[object];

      if (ref != null) {
        var value:int = ref as int;

        writeRef(value);
        return true;
      }
      else {
        _refs[object] = _numRefs++;

        return false;
      }
    }

    /**
     * Resets the references for streaming.
     */
    public override function resetReferences():void
    {
      if (_refs != null) {
        _refs = new Dictionary();
        _numRefs = 0;
      }
    }

    /**
     * Removes a reference.
     *
     * @param obj The object which has a reference.
     * 
     * @return true if a reference was present for the given object.
     */
    public override function removeRef(obj:Object):Boolean
    {
      if (_refs != null) {
        delete _refs[obj];
        _numRefs--;

        return true;
      }
      else
        return false;
    }

    /**
     * Replaces a reference from one object to another.
     * 
     * @param oldObj The object which has a reference to replace.
     * @param newObj The object to which to assign the reference.
     *
     * @return true if a reference was present for the given object.
     */
    public override function replaceRef(oldRef:Object, newRef:Object):Boolean
    {
      var value:Object = _refs[oldRef];

      if (value != null) {
        delete _refs[oldRef];
        _refs[newRef] = value;
        return true;
      }
      else
        return false;
    }

    /**
     * Prints a string to the stream, encoded as UTF-8 with preceeding length.
     *
     * @param value The string value to write.
     * @param offset The offset in the string of where to start.
     * @param length The length in the string to write.
     */
    public function printLenString(value:String):void
    {
      if (value == null) {
        _out.writeByte(0);
        _out.writeByte(0);
      }
      else {
        var len:int = value.length;
        _out.writeByte(len >> 8);
        _out.writeByte(len);

        printString(value, 0, len);
      }
    }

    /**
     * Prints a string to the stream, encoded as UTF-8.
     *
     * @param value The string value to write.
     * @param offset The offset in the string of where to start.
     * @param length The length in the string to write.
     */
    public function printString(value:String, 
                                offset:int = 0, 
                                length:int = -1):void
    {
      if (length < 0)
        length = value.length;

      for (var i:int = 0; i < length; i++) {
        var ch:int = value.charCodeAt(i + offset);

        if (ch < 0x80)
          _out.writeByte(ch);

        else if (ch < 0x800) {
          _out.writeByte(0xc0 + ((ch >> 6) & 0x1f));
          _out.writeByte(0x80 + (ch & 0x3f));
        }

        else {
          _out.writeByte(0xe0 + ((ch >> 12) & 0xf));
          _out.writeByte(0x80 + ((ch >> 6) & 0x3f));
          _out.writeByte(0x80 + (ch & 0x3f));
        }
      }
    }

    /**
     * Prints a character array to the stream, encoded as UTF-8.
     *
     * @param value The character array value to write.
     * @param offset The offset in the string of where to start.
     * @param length The length in the string to write.
     */
    public function printCharArray(value:Array, offset:int, length:int):void
    {
      for (var i:int = 0; i < length; i++) {
        var ch:int = value[i + offset];

        if (ch < 0x80)
          _out.writeByte(ch);

        else if (ch < 0x800) {
          _out.writeByte(0xc0 + ((ch >> 6) & 0x1f));
          _out.writeByte(0x80 + (ch & 0x3f));
        }

        else {
          _out.writeByte(0xe0 + ((ch >> 12) & 0xf));
          _out.writeByte(0x80 + ((ch >> 6) & 0x3f));
          _out.writeByte(0x80 + (ch & 0x3f));
        }
      }
    }
  }
}
