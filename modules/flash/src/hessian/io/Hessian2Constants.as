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
 * @author Scott Ferguson, Emil Ong
 * 
 */

package hessian.io 
{
  /**
   * Constants used in the Hessian 2.0 protocol.
   *
   */
  public class Hessian2Constants
  {
    // final chunk
    public static const BC_BINARY:int = 'B'.charCodeAt(); 
    // non-final chunk
    public static const BC_BINARY_CHUNK:int = 'A'.charCodeAt(); 
    // 1-byte length binary
    public static const BC_BINARY_DIRECT:int = 0x20; 
    public static const BINARY_DIRECT_MAX:int = 0x0f;
    // 2-byte length binary
    public static const BC_BINARY_SHORT:int = 0x34; 
    // 0-1023 binary
    public static const BINARY_SHORT_MAX:int = 0x3ff; 

    // object/class definition
    public static const BC_CLASS_DEF:int = 'C'.charCodeAt(); 

    public static const BC_DATE:int = 0x4a; // 64-bit millisecond UTC date
    public static const BC_DATE_MINUTE:int = 0x4b; // 32-bit minute UTC date

    public static const BC_DOUBLE:int = 'D'.charCodeAt(); // IEEE 64-bit double

    public static const BC_DOUBLE_ZERO:int = 0x5b;
    public static const BC_DOUBLE_ONE:int = 0x5c;
    public static const BC_DOUBLE_BYTE:int = 0x5d;
    public static const BC_DOUBLE_SHORT:int = 0x5e;
    public static const BC_DOUBLE_MILL:int = 0x5f;

    public static const BC_FALSE:int = 'F'.charCodeAt(); // boolean false

    public static const BC_INT:int = 'I'.charCodeAt(); // 32-bit int

    public static const INT_DIRECT_MIN:int = -0x10;
    public static const INT_DIRECT_MAX:int = 0x2f;
    public static const BC_INT_ZERO:int = 0x90;

    public static const INT_BYTE_MIN:int = -0x800;
    public static const INT_BYTE_MAX:int = 0x7ff;
    public static const BC_INT_BYTE_ZERO:int = 0xc8;

    public static const BC_END:int = 'Z'.charCodeAt();

    public static const INT_SHORT_MIN:int = -0x40000;
    public static const INT_SHORT_MAX:int = 0x3ffff;
    public static const BC_INT_SHORT_ZERO:int = 0xd4;

    public static const BC_LIST_VARIABLE:int =0x55;
    public static const BC_LIST_FIXED:int = 'V'.charCodeAt();
    public static const BC_LIST_VARIABLE_UNTYPED:int = 0x57;
    public static const BC_LIST_FIXED_UNTYPED:int =0x58;

    public static const BC_LIST_DIRECT:int = 0x70;
    public static const BC_LIST_DIRECT_UNTYPED:int = 0x78;
    public static const LIST_DIRECT_MAX:int = 0x7;

    public static const BC_LONG:int = 'L'.charCodeAt(); // 64-bit signed integer
    public static const LONG_DIRECT_MIN:int = -0x08;
    public static const LONG_DIRECT_MAX:int =  0x0f;
    public static const BC_LONG_ZERO:int = 0xe0;

    public static const LONG_BYTE_MIN:int = -0x800;
    public static const LONG_BYTE_MAX:int =  0x7ff;
    public static const BC_LONG_BYTE_ZERO:int = 0xf8;

    public static const LONG_SHORT_MIN:int = -0x40000;
    public static const LONG_SHORT_MAX:int = 0x3ffff;
    public static const BC_LONG_SHORT_ZERO:int = 0x3c;

    public static const BC_LONG_INT:int = 0x59;

    public static const BC_MAP:int = 'M'.charCodeAt();
    public static const BC_MAP_UNTYPED:int = 'H'.charCodeAt();

    public static const BC_NULL:int = 'N'.charCodeAt();

    public static const BC_OBJECT:int = 'O'.charCodeAt();
    public static const BC_OBJECT_DEF:int = 'C'.charCodeAt();

    public static const BC_OBJECT_DIRECT:int = 0x60;
    public static const OBJECT_DIRECT_MAX:int = 0x0f;

    public static const BC_REF:int = 0x51;

    // final string
    public static const BC_STRING:int = 'S'.charCodeAt(); 
    // non-final string
    public static const BC_STRING_CHUNK:int = 'R'.charCodeAt(); 

    public static const BC_STRING_DIRECT:int = 0x00;
    public static const STRING_DIRECT_MAX:int = 0x1f;
    public static const BC_STRING_SHORT:int = 0x30;
    public static const STRING_SHORT_MAX:int = 0x3ff;

    public static const BC_TRUE:int = 'T'.charCodeAt();

    public static const P_PACKET_CHUNK:int = 0x4f;
    public static const P_PACKET:int = 'P'.charCodeAt();

    public static const P_PACKET_DIRECT:int = 0x80;
    public static const PACKET_DIRECT_MAX:int = 0x7f;

    public static const P_PACKET_SHORT:int = 0x70;
    public static const PACKET_SHORT_MAX:int = 0xfff;
  }
}
