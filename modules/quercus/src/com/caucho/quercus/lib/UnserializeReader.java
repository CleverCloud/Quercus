/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.lib;

import com.caucho.quercus.env.*;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

public final class UnserializeReader {
  private static final L10N L = new L10N(UnserializeReader.class);
  private static final Logger log
    = Logger.getLogger(UnserializeReader.class.getName());

  private static final LruCache<StringKey,StringValue> _keyCache
    = new LruCache<StringKey,StringValue>(4096);

  private final char []_buffer;
  private final int _length;

  private int _index;
  private StringKey _key = new StringKey();
  
  private ArrayList<Value> _valueList
    = new ArrayList<Value>();
  
  private ArrayList<Boolean> _referenceList
    = new ArrayList<Boolean>();
  
  private boolean _useReference;

  public UnserializeReader(StringValue s)
    throws IOException
  {
    _buffer = s.toCharArray();
    _length = _buffer.length;
    
    if (s.indexOf("R:") >= 0
        || s.indexOf("r:") >= 0)
      initReferenceList();
  }

  public UnserializeReader(String s)
     throws IOException
  {
    _buffer = s.toCharArray();
    _length = _buffer.length;
    
    if (s.indexOf("R:") >= 0
        || s.indexOf("r:") >= 0)
      initReferenceList();
  }

  boolean useReference()
  {
    return _useReference;
  }

  public Value unserialize(Env env)
    throws IOException
  {
    int ch = read();

    switch (ch) {
    case 'b':
      {
        expect(':');
        long v = readInt();
        expect(';');
        
        Value value = v == 0 ? BooleanValue.FALSE : BooleanValue.TRUE;
        
        if (_useReference)
          value = createReference(value);
        
        return value;
      }

    case 's':
    case 'S':
      {
        expect(':');
        int len = (int) readInt();
        expect(':');
        expect('"');

        if (! isValidString(len)) {
          env.notice(L.l("expected string length of {0}", len));
          return BooleanValue.FALSE;
        }
        
        Value value = readStringValue(env, len);

        expect('"');
        expect(';');
        
        if (_useReference)
          value = createReference(value);

        return value;
      }
    case 'u':
    case 'U':
      {
        expect(':');
        int len = (int) readInt();
        expect(':');
        expect('"');

        if (! isValidString(len)) {
          env.notice(L.l("expected string length of {0}", len));
          return BooleanValue.FALSE;
        }
        
        Value value = readUnicodeValue(env, len);

        expect('"');
        expect(';');
        
        if (_useReference)
          value = createReference(value);

        return value;
      }

    case 'i':
      {
        expect(':');

        long l = readInt();

        expect(';');
        
        Value value = LongValue.create(l); 
        
        if (_useReference)
          value = createReference(value);

        return value;
      }

    case 'd':
      {
        expect(':');

        StringBuilder sb = new StringBuilder();
        for (ch = read(); ch >= 0 && ch != ';'; ch = read()) {
          sb.append((char) ch);
        }

        if (ch != ';')
          throw new IOException(L.l("expected ';'"));

        Value value = new DoubleValue(Double.parseDouble(sb.toString()));
        
        if (_useReference)
          value = createReference(value);
        
        return value;
      }

    case 'a':
      {
        expect(':');
        int len = (int) readInt();
        expect(':');
        expect('{');
        
        Value array = new ArrayValueImpl(len);

        if (_useReference)
          array = createReference(array);
        
        for (int i = 0; i < len; i++) {
          Value key = unserializeKey(env);
          Value value = unserialize(env);

          array.put(key, value);
        }

        expect('}');
          
        return array;
      }

    case 'O':
      {
        expect(':');
        int len = (int) readInt();
        expect(':');
        expect('"');

        if (! isValidString(len))
          return BooleanValue.FALSE;
        
        String className = readString(len);

        expect('"');
        expect(':');
        int count = (int) readInt();
        expect(':');
        expect('{');

        QuercusClass qClass = env.findClass(className);
        Value obj;

        if (qClass != null)
          obj = qClass.createObject(env);
        else {
          log.fine(L.l("{0} is an undefined class in unserialize",
                   className));
          
          obj = env.createIncompleteObject(className);
        }
        
        Value ref = null;
        
        if (_useReference)
          ref = createReference(obj);

        for (int i = 0; i < count; i++) {
          StringValue key = unserializeKey(env).toStringValue();
          
          FieldVisibility visibility = FieldVisibility.PUBLIC;

          if (key.length() > 3 && key.charAt(0) == 0) {
            switch (key.charAt(1)) {
              case 'A': // 0x41
                visibility = FieldVisibility.PRIVATE;
                break;
              case '*': // 0x2A
                visibility = FieldVisibility.PROTECTED;
                break;
              default:
                throw new IOException(
                    L.l("field visibility modifier is not valid: 0x{0}",
                                          Integer.toHexString(key.charAt(2))));
            }
            
            if (key.charAt(2) != 0) {
              throw new IOException(
                  L.l("end of field visibility modifier is not valid: 0x{0}",
                                        Integer.toHexString(key.charAt(2))));
            }
            
            key = key.substring(3);
          }
          
          Value value = unserialize(env);

          obj.initField(key, value, visibility);
        }

        expect('}');
        
        if (ref != null)
          return ref;
        else
          return obj;
      }

    case 'N':
      {
        expect(';');
        
        Value value = NullValue.NULL;
        
        if (_useReference)
          value = createReference(value);

        return value;
      }
    case 'R':
      {
        expect(':');

        int index = (int) readInt();

        expect(';');
        
        if (index - 1 >= _valueList.size()) {
          throw new IOException(
              L.l("reference out of range: {0}, size {1}, index {2}",
                                    index - 1, _valueList.size(), _index));
          //return BooleanValue.FALSE;
        }
        
        Value ref = _valueList.get(index - 1);

        return ref;
      }
    case 'r':
      {
        expect(':');

        int index = (int) readInt();

        expect(';');
        
        if (index - 1 >= _valueList.size()) {
          throw new IOException(
              L.l("reference out of range: {0}, size {1}, index {2}",
                                    index - 1, _valueList.size(), _index));
          //return BooleanValue.FALSE;
        }
        
        Value value = _valueList.get(index - 1).copy();
        
        if (_useReference)
          value = createReference(value);
        
        return value;
        
      }

    default:
      throw new IOException(
          L.l("option not recognized '{0}' (0x{1}) at index {2}",
                                String.valueOf((char) ch),
                                Integer.toHexString(ch),
                                _index));
      
      //return BooleanValue.FALSE;
    }
  }
  
  public Value createReference(Value value)
  {
    if (_referenceList.get(_valueList.size()) == Boolean.FALSE) {
      
      _valueList.add(value);
      return value;
    }
    else {
      Var var = new Var(value);
      
      _valueList.add(var);
      return var;
    }
  }

  private void initReferenceList()
    throws IOException
  {
    populateReferenceList();
    
    _index = 0;
  }
  
  private void populateReferenceList()
    throws IOException
  {
    int ch = read();

    switch (ch) {
      case 'b':
      {
        _referenceList.add(Boolean.FALSE);
        
        expect(':');
        for (ch = read(); ch >= 0 && ch != ';'; ch = read()) {
        }
        
        return;
      }

      case 's':
      case 'S':
      {
        _referenceList.add(Boolean.FALSE);
        
        expect(':');
        int len = (int) readInt();
        expect(':');
        expect('"');

        _index += len;

        expect('"');
        expect(';');
        
        return;
      }
      case 'u':
      case 'U':
      {
        _referenceList.add(Boolean.FALSE);
        
        expect(':');
        int len = (int) readInt();
        expect(':');
        expect('"');

        _index += len;

        expect('"');
        expect(';');
        
        return;
      }

      case 'i':
      {
        _referenceList.add(Boolean.FALSE);
        
        expect(':');

        for (ch = read(); ch >= 0 && ch != ';'; ch = read()) {
        }
        
        return;
      }

      case 'd':
      {
        _referenceList.add(Boolean.FALSE);
        
        expect(':');

        for (ch = read(); ch >= 0 && ch != ';'; ch = read()) {
        }
        
        return;
      }

      case 'a':
      {
        _referenceList.add(Boolean.FALSE);
        
        expect(':');
        int len = (int) readInt();
        expect(':');
        expect('{');

        for (int i = 0; i < len; i++) {
          switch (read()) {
          case 's':
          case 'S':
            {
              expect(':');
              int keyLen = (int) readInt();
              expect(':');
              expect('"');

              _index += keyLen;

              expect('"');
              expect(';');
              
              break;
              
            }

          case 'i':
            {
              expect(':');

              for (ch = read(); ch >= 0 && ch != ';'; ch = read()) {
              }
              
              break;
            }
          }

          populateReferenceList();
        }

        expect('}');
        
        return;
      }

      case 'O':
      {
        _referenceList.add(Boolean.FALSE);
        
        expect(':');
        int len = (int) readInt();
        expect(':');
        expect('"');

        _index += len;

        expect('"');
        expect(':');
        int count = (int) readInt();
        expect(':');
        expect('{');

        for (int i = 0; i < count; i++) {
          switch (read()) {
            case 's':
            case 'S':
            {
              expect(':');
              int keyLen = (int) readInt();
              expect(':');
              expect('"');

              _index += keyLen;

              expect('"');
              expect(';');
                
              break;
            }

            case 'i':
            {
              expect(':');

              for (ch = read(); ch >= 0 && ch != ';'; ch = read()) {
              }
                
              break;
            }
          }

          populateReferenceList();
        }

        expect('}');
        
        return;
      }

      case 'N':
      {
        _referenceList.add(Boolean.FALSE);
        
        expect(';');
        
        return;
      }

      case 'R':
      {
        _referenceList.add(Boolean.FALSE);
        
        _useReference = true;
        
        expect(':');

        int value = (int) readInt();

        expect(';');
        
        _referenceList.set(value - 1, Boolean.TRUE);
        
        return;
      }
      
      case 'r':
      {
        _referenceList.add(Boolean.FALSE);
        
        _useReference = true;

        expect(':');

        int value = (int) readInt();

        expect(';');
        
        return;
      }
    }
  }

  public Value unserializeKey(Env env)
    throws IOException
  {
    int ch = read();

    switch (ch) {
    case 's':
    case 'S':
      {
        expect(':');
        int len = (int) readInt();
        expect(':');
        expect('"');

        StringValue v;

        if (len < 32) {
          _key.init(_buffer, _index, len);

          v = _keyCache.get(_key);

          if (v != null) {
            _index += len;
          }
          else {
            StringKey key = new StringKey(_buffer, _index, len);

            v = readStringValue(env, len);

            _keyCache.put(key, v);
          }
        }
        else {
          v = readStringValue(env, len);
        }

        expect('"');
        expect(';');

        return v;
      }

    case 'i':
      {
        expect(':');

        long value = readInt();

        expect(';');

        return LongValue.create(value);
      }

    default:
      return BooleanValue.FALSE;
    }
  }

  private String unserializeString()
    throws IOException
  {
    int ch = read();
    
    if (ch != 's' && ch != 'S') {
      throw new IOException(L.l("expected 's' at '{1}' (0x{2})",
                                String.valueOf((char) ch),
                                Integer.toHexString(ch)));
    }

    expect(':');
    int len = (int) readInt();
    expect(':');
    expect('"');

    String s = readString(len);

    expect('"');
    expect(';');

    return s;
  }

  public final void expect(int expectCh)
    throws IOException
  {
    if (_length <= _index)
      throw new IOException(L.l("expected '{0}' at end of string",
                                String.valueOf((char) expectCh)));

    int ch = _buffer[_index++];

    if (ch != expectCh) {
      String context = String.valueOf((char) ch);
      
      if (_index - 2 >= 0)
        context = _buffer[_index - 2] + context;
      if (_index < _buffer.length)
        context += _buffer[_index];
      
      throw new IOException(
          L.l("expected '{0}' at '{1}' (0x{2}) (context '{3}', index {4})",
                                String.valueOf((char) expectCh),
                                String.valueOf((char) ch),
                                Integer.toHexString(ch),
                                context,
                                _index));
    }
  }

  public final long readInt()
  {
    int ch = read();

    long sign = 1;
    long value = 0;

    if (ch == '-') {
      sign = -1;
      ch = read();
    }
    else if (ch == '+') {
      ch = read();
    }

    for (; '0' <= ch && ch <= '9'; ch = read()) {
      value = 10 * value + ch - '0';
    }

    unread();

    return sign * value;
  }
  
  public final boolean isValidString(int len)
  {
    if (_index + len >= _buffer.length)
      return false;
    
    return true;
  }

  public final String readString(int len)
  {
    String s = new String(_buffer, _index, len);

    _index += len;

    return s;
  }

  public final StringValue readStringValue(Env env, int len)
  {
    StringValue s = env.createString(_buffer, _index, len);

    _index += len;

    return s;
  }
  
  public final StringValue readUnicodeValue(Env env, int len)
  {
    StringValue s = new UnicodeBuilderValue(_buffer, _index, len);

    _index += len;

    return s;
  }

  public final int read()
  {
    if (_index < _length)
      return _buffer[_index++];
    else
      return -1;
  }

  public final int read(char []buffer, int offset, int length)
  {
    System.arraycopy(_buffer, _index, buffer, offset, length);

    _index += length;

    return length;
  }

  public final void unread()
  {
    _index--;
  }

  public final static class StringKey
  {
    char []_buffer;
    int _offset;
    int _length;

    StringKey()
    {
    }

    StringKey(char []buffer, int offset, int length)
    {
      _buffer = new char[length];
      System.arraycopy(buffer, offset, _buffer, 0, length);
      _offset = 0;
      _length = length;
    }

    void init(char []buffer, int offset, int length)
    {
      _buffer = buffer;
      _offset = offset;
      _length = length;
    }

    public int hashCode()
    {
      char []buffer = _buffer;
      int offset = _offset;
      int end = offset + _length;
      int hash = 17;

      for (; offset < end; offset++)
        hash = 65521 * hash + buffer[offset];

      return hash;
    }

    public boolean equals(Object o)
    {
      if (! (o instanceof StringKey))
        return false;

      StringKey key = (StringKey) o;

      int length = _length;

      if (length != key._length)
        return false;

      char []aBuf = _buffer;
      char []bBuf = key._buffer;

      int aOffset = _offset;
      int bOffset = key._offset;

      int aEnd = aOffset + length;

      while (aOffset < aEnd) {
        if (aBuf[aOffset++] != bBuf[bOffset++])
          return false;
      }

      return true;
    }
  }
}


