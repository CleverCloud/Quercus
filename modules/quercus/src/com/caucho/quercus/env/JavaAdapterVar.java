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
 * @author Nam Nguyen
 */

package com.caucho.quercus.env;

import com.caucho.quercus.Location;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;

/**
 * Represents a PHP variable value.
 */
public class JavaAdapterVar extends Value
{
  private JavaAdapter _adapter;
  private Value _key;
  private Value _value;

  public JavaAdapterVar(JavaAdapter adapter, Value key)
  {
    _adapter = adapter;
    _key = key;
  }

  public Value getValue()
  {
    return _adapter.get(_key);
  }
  
  public void setValue(Value value)
  {
    _adapter.putImpl(_key, value);
  }
  
  /**
   * Sets the value.
   */
  @Override
  public Value set(Value value)
  {
    setRaw(getValue());
    
    value = super.set(value);
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Returns the type.
   */
  @Override
  public String getType()
  {
    return getValue().getType();
  }
  
  /**
   * Returns the type of the resource.
   */
  @Override
  public String getResourceType()
  {
    return getValue().getResourceType();
  }

  /**
   * Returns the ValueType.
   */
  @Override
  public ValueType getValueType()
  {
    return getValue().getValueType();
  }

  /**
   * Returns the class name.
   */
  @Override
  public String getClassName()
  {
    return getValue().getClassName();
  }
  
  /**
   * Returns true for an object.
   */
  @Override
  public boolean isObject()
  {
    return getValue().isObject();
  }
  
  /**
   * Returns true for an object.
   */
  @Override
  public boolean isResource()
  {
    return getValue().isResource();
  }

  /**
   * Returns true for a set type.
   */
  @Override
  public boolean isset()
  {
    return getValue().isset();
  }

  /**
   * Returns true for an implementation of a class
   */
  @Override
  public boolean isA(String name)
  {
    return getValue().isA(name);
  }

  /**
   * True for a number
   */
  @Override
  public boolean isNull()
  {
    return getValue().isNull();
  }

  /**
   * True for a long
   */
  @Override
  public boolean isLongConvertible()
  {
    return getValue().isLongConvertible();
  }

  /**
   * True to a double.
   */
  @Override
  public boolean isDoubleConvertible()
  {
    return getValue().isDoubleConvertible();
  }

  /**
   * True for a number
   */
  @Override
  public boolean isNumberConvertible()
  {
    return getValue().isNumberConvertible();
  }

  /**
   * Returns true for a long-value.
   */
  public boolean isLong()
  {
    return getValue().isLong();
  }
  
  /**
   * Returns true for a long-value.
   */
  public boolean isDouble()
  {
    return getValue().isDouble();
  }
  
  /**
   * Returns true for is_numeric
   */
  @Override
  public boolean isNumeric()
  {
    return getValue().isNumeric();
  }

  /**
   * Returns true for a scalar
   */
  /*
  public boolean isScalar()
  {
    return getValue().isScalar();
  }
  */

  /**
   * Returns true for a StringValue.
   */
  @Override
  public boolean isString()
  {
    return getValue().isString();
  }

  /**
   * Returns true for a BinaryValue.
   */
  @Override
  public boolean isBinary()
  {
    return getValue().isBinary();
  }

  /**
   * Returns true for a UnicodeValue.
   */
  @Override
  public boolean isUnicode()
  {
    return getValue().isUnicode();
  }

  /**
   * Returns true for a BooleanValue
   */
  @Override
  public boolean isBoolean()
  {
    return getValue().isBoolean();
  }
  
  /**
   * Returns true for a DefaultValue
   */
  @Override
  public boolean isDefault()
  {
    return getValue().isDefault();
  }
  
  //
  // Conversions
  //

  @Override
  public String toString()
  {
    return getValue().toString();
  }

  /**
   * Converts to a boolean.
   */
  @Override
  public boolean toBoolean()
  {
    return getValue().toBoolean();
  }

  /**
   * Converts to a long.
   */
  @Override
  public long toLong()
  {
    return getValue().toLong();
  }

  /**
   * Converts to a double.
   */
  @Override
  public double toDouble()
  {
    return getValue().toDouble();
  }

  /**
   * Converts to a string.
   * @param env
   */
  @Override
  public StringValue toString(Env env)
  {
    return getValue().toString(env);
  }

  /**
   * Converts to an object.
   */
  @Override
  public Object toJavaObject()
  {
    return getValue().toJavaObject();
  }

  /**
   * Converts to an object.
   */
  @Override
  public Object toJavaObject(Env env, Class type)
  {
    return getValue().toJavaObject(env, type);
  }

  /**
   * Converts to an object.
   */
  @Override
  public Object toJavaObjectNotNull(Env env, Class type)
  {
    return getValue().toJavaObjectNotNull(env, type);
  }

  /**
   * Converts to a java Collection object.
   */
  @Override
  public Collection toJavaCollection(Env env, Class type)
  {
    return getValue().toJavaCollection(env, type);
  }
  
  /**
   * Converts to a java List object.
   */
  @Override
  public List toJavaList(Env env, Class type)
  {
    return getValue().toJavaList(env, type);
  }
  
  /**
   * Converts to a java Map object.
   */
  @Override
  public Map toJavaMap(Env env, Class type)
  {
    return getValue().toJavaMap(env, type);
  }


  /**
   * Converts to an array
   */
  @Override
  public Value toArray()
  {
    return getValue().toArray();
  }

  /**
   * Converts to an array
   */
  @Override
  public ArrayValue toArrayValue(Env env)
  {
    return getValue().toArrayValue(env);
  }

  /**
   * Converts to an array
   */
  @Override
  public Value toAutoArray()
  {
    setRaw(getValue());
    
    Value value = super.toAutoArray();
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Converts to an object.
   */
  @Override
  public Value toObject(Env env)
  {
    return getValue().toObject(env);
  }

  /**
   * Converts to a Java Calendar.
   */
  @Override
  public Calendar toJavaCalendar()
  {
    return getValue().toJavaCalendar();
  }
  
  /**
   * Converts to a Java Date.
   */
  @Override
  public Date toJavaDate()
  {
    return getValue().toJavaDate();
  }
  
  /**
   * Converts to a Java URL.
   */
  @Override
  public URL toJavaURL(Env env)
  {
    return getValue().toJavaURL(env);
  }
  
  /**
   * Converts to a Java BigDecimal.
   */
  public BigDecimal toBigDecimal()
  {
    return getValue().toBigDecimal();
  }
  
  /**
   * Converts to a Java BigInteger.
   */
  public BigInteger toBigInteger()
  {
    return getValue().toBigInteger();
  }
  
  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(UnicodeBuilderValue sb)
  {
    return getValue().appendTo(sb);
  }

  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(BinaryBuilderValue sb)
  {
    return getValue().appendTo(sb);
  }

  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(StringBuilderValue sb)
  {
    return getValue().appendTo(sb);
  }
  
  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(LargeStringBuilderValue sb)
  {
    return getValue().appendTo(sb);
  }

  /**
   * Converts to a raw value.
   */
  @Override
  public Value toValue()
  {
    return getValue();
  }

  /**
   * Converts to a function argument value that is never assigned or modified.
   */
  @Override
  public Value toLocalValueReadOnly()
  {
    return getValue();
  }

  /**
   * Converts to a raw value.
   */
  @Override
  public Value toLocalValue()
  {
    return getValue().toLocalValue();
  }

  /**
   * Converts to a function argument ref value, i.e. an argument
   * declared as a reference, but not assigned
   */
  @Override
  public Value toRefValue()
  {
    setRaw(getValue());
    
    Value value = super.toRefValue();
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Converts to a variable
   */
  @Override
  public Var toVar()
  {
    setRaw(getValue());
    
    Var value = super.toVar();
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Converts to a key.
   */
  @Override
  public Value toKey()
  {
    return getValue().toKey();
  }

  @Override
  public StringValue toStringValue()
  {
    return getValue().toStringValue();
  }

  @Override
  public StringValue toBinaryValue(Env env)
  {
    return getValue().toBinaryValue(env);
  }

  @Override
  public StringValue toUnicodeValue(Env env)
  {
    return getValue().toUnicodeValue(env);
  }

  @Override
  public StringValue toStringBuilder()
  {
    return getValue().toStringBuilder();
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env)
  {
    return getValue().toStringBuilder(env);
  }

  //
  // Operations
  //

  /**
   * Copy the value.
   */
  @Override
  public Value copy()
  {
    setRaw(getValue());
    
    Value value = super.copy();
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Copy the value as a return value.
   */
  @Override
  public Value copyReturn()
  {
    setRaw(getValue());
    
    Value value = super.copyReturn();
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Converts to a variable reference (for function  arguments)
   */
  @Override
  public Value toRef()
  {
    setRaw(getValue());
    
    Value value = super.toRef();
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Returns true for an array.
   */
  @Override
  public boolean isArray()
  {
    return getValue().isArray();
  }

  /**
   * Negates the value.
   */
  @Override
  public Value neg()
  {
    return getValue().neg();
  }

  /**
   * Adds to the following value.
   */
  @Override
  public Value add(Value rValue)
  {
    return getValue().add(rValue);
  }

  /**
   * Adds to the following value.
   */
  @Override
  public Value add(long rValue)
  {
    return getValue().add(rValue);
  }

  /**
   * Pre-increment the following value.
   */
  @Override
  public Value preincr(int incr)
  {
    setRaw(getValue());
    
    Value value = increment(incr);
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Post-increment the following value.
   */
  @Override
  public Value postincr(int incr)
  {
    setRaw(getValue());
    
    Value value = increment(incr);
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Subtracts to the following value.
   */
  @Override
  public Value sub(Value rValue)
  {
    return getValue().sub(rValue);
  }

  /**
   * Subtracts to the following value.
   */
  @Override
  public Value sub(long rValue)
  {
    return getValue().sub(rValue);
  }
  
  /**
   * Multiplies to the following value.
   */
  @Override
  public Value mul(Value rValue)
  {
    return getValue().mul(rValue);
  }

  /**
   * Multiplies to the following value.
   */
  @Override
  public Value mul(long lValue)
  {
    return getValue().mul(lValue);
  }

  /**
   * Divides the following value.
   */
  @Override
  public Value div(Value rValue)
  {
    return getValue().div(rValue);
  }

  /**
   * Shifts left by the value.
   */
  @Override
  public Value lshift(Value rValue)
  {
    return getValue().lshift(rValue);
  }

  /**
   * Shifts right by the value.
   */
  @Override
  public Value rshift(Value rValue)
  {
    return getValue().rshift(rValue);
  }
  
  /**
   * Binary And.
   */
  public Value bitAnd(Value rValue)
  {
    return getValue().bitAnd(rValue);
  }
  
  /**
   * Binary or.
   */
  public Value bitOr(Value rValue)
  {
    return getValue().bitOr(rValue);
  }
  
  /**
   * Binary xor.
   */
  @Override
  public Value bitXor(Value rValue)
  {
    return getValue().bitXor(rValue);
  }
  
  /**
   * Absolute value.
   */
  public Value abs()
  {
    return getValue().abs();
  }

  /**
   * Returns true for equality
   */
  @Override
  public boolean eq(Value rValue)
  {
    return getValue().eq(rValue);
  }

  /**
   * Returns true for equality
   */
  @Override
  public boolean eql(Value rValue)
  {
    return getValue().eql(rValue);
  }

  /**
   * Compares the two values
   */
  @Override
  public int cmp(Value rValue)
  {
    return getValue().cmp(rValue);
  }

  /**
   * Returns the length as a string.
   */
  @Override
  public int length()
  {
    return getValue().length();
  }
  
  /**
   * Returns the array/object size
   */
  @Override
  public int getSize()
  {
    return getValue().getSize();
  }

  @Override
  public Iterator<Map.Entry<Value, Value>> getIterator(Env env)
  {
    return getValue().getIterator(env);
  }

  @Override
  public Iterator<Value> getKeyIterator(Env env)
  {
    return getValue().getKeyIterator(env);
  }

  @Override
  public Iterator<Value> getValueIterator(Env env)
  {
    return getValue().getValueIterator(env);
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Value getArray()
  {
    setRaw(getValue());
    
    Value value = super.getArray();
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Returns the value, creating an object if unset.
   */
  @Override
  public Value getObject(Env env)
  {
    setRaw(getValue());
    
    Value value = super.getObject(env);
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Value get(Value index)
  {
    return getValue().get(index);
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Var getVar(Value index)
  {
    setRaw(getValue());
    
    Var value = super.getVar(index);
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Value getArg(Value index, boolean isTop)
  {
    setRaw(getValue());
    
    Value value = super.getArg(index, isTop);
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Returns the value, creating an object if unset.
   */
  @Override
  public Value getArray(Value index)
  {
    setRaw(getValue());
    
    Value value = super.getArray(index);
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Returns the value, doing a copy-on-write if needed.
   */
  @Override
  public Value getDirty(Value index)
  {
    return getValue().getDirty(index);
  }

  /**
   * Returns the value, creating an object if unset.
   */
  @Override
  public Value getObject(Env env, Value index)
  {
    setRaw(getValue());
    
    Value value = super.getObject(env, index);
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Value put(Value index, Value value)
  {
    setRaw(getValue());
    
    Value retValue = super.put(index, value);
    
    setValue(getRawValue());
    
    return retValue;
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Value put(Value value)
  {
    setRaw(getValue());
    
    Value retValue = super.put(value);
    
    setValue(getRawValue());
    
    return retValue;
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Var putVar()
  {
    setRaw(getValue());
    
    Var retValue = super.putVar();
    
    setValue(getRawValue());
    
    return retValue;
  }
  
  /**
   * Sets the array value, returning the new array, e.g. to handle
   * string update ($a[0] = 'A').
   */
  @Override
  public Value append(Value index, Value value)
  {
    setRaw(getValue());
    
    Value retValue = super.append(index, value);
    
    setValue(getRawValue());
    
    return retValue;
  }

  /**
   * Return unset the value.
   */
  @Override
  public Value remove(Value index)
  {
    return getValue().remove(index);
  }

  /**
   * Returns the field ref.
   */
  @Override
  public Value getField(Env env, StringValue index)
  {
    return getValue().getField(env, index);
  }

  /**
   * Returns the field ref.
   */
  @Override
  public Var getFieldVar(Env env, StringValue index)
  {
    setRaw(getValue());
    
    Var retValue = super.getFieldVar(env, index);
    
    setValue(getRawValue());

    return retValue;
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Value getFieldArg(Env env, StringValue index, boolean isTop)
  {
    setRaw(getValue());
    
    Value retValue = super.getFieldArg(env, index, isTop);
    
    setValue(getRawValue());
    
    return retValue;
  }

  /**
   * Returns the field value as an array
   */
  @Override
  public Value getFieldArray(Env env, StringValue index)
  {
    setRaw(getValue());
    
    Value retValue = super.getFieldArray(env, index);
    
    setValue(getRawValue());
    
    return retValue;
  }

  /**
   * Returns the field value as an object
   */
  @Override
  public Value getFieldObject(Env env, StringValue index)
  {
    setRaw(getValue());
    
    Value retValue = super.getFieldObject(env, index);
    
    setValue(getRawValue());
    
    return retValue;
  }

  /**
   * Sets the field.
   */
  @Override
  public Value putField(Env env, StringValue index, Value value)
  {
    setRaw(getValue());
    
    Value retValue = super.putField(env, index, value);
    
    setValue(getRawValue());
    
    return retValue;
  }

  /**
   * Initializes a new field, does not call __set if it is defined.
   */
  public void initField(StringValue key,
                        Value value,
                        FieldVisibility visibility)
  {
    setRaw(getValue());
    
    super.initField(key, value, visibility);
    
    setValue(getRawValue());
  }
  
  /**
   * Sets the field.
   */
  @Override
  public Value putThisField(Env env, StringValue index, Value value)
  {
    setRaw(getValue());
    
    Value retValue = super.putThisField(env, index, value);
    
    setValue(getRawValue());

    return retValue;
  }
  
  /**
   * Unsets the field.
   */
  @Override
  public void unsetField(StringValue index)
  {
    getValue().unsetField(index);
  }

  /**
   * Takes the values of this array, unmarshalls them to objects of type
   * <i>elementType</i>, and puts them in a java array.
   */
  @Override
  public Object valuesToArray(Env env, Class elementType)
  {
    return getValue().valuesToArray(env, elementType);
  }
  
  /**
   * Returns the character at an index
   */
  @Override
  public Value charValueAt(long index)
  {
    return getValue().charValueAt(index);
  }

  /**
   * Sets the character at an index
   */
  @Override
  public Value setCharValueAt(long index, Value value)
  {
    return getValue().setCharValueAt(index, value);
  }

  /**
   * Returns true if there are more elements.
   */
  @Override
  public boolean hasCurrent()
  {
    return getValue().hasCurrent();
  }

  /**
   * Returns the current key
   */
  @Override
  public Value key()
  {
    return getValue().key();
  }

  /**
   * Returns the current value
   */
  @Override
  public Value current()
  {
    return getValue().current();
  }

  /**
   * Returns the current value
   */
  @Override
  public Value next()
  {
    return getValue().next();
  }
  
  /**
   * Returns the previous value
   */
  @Override
  public Value prev()
  {
    return getValue().prev();
  }
  
  /**
   * Returns the end value.
   */
  @Override
  public Value end()
  {
    return getValue().end();
  }
  
  /**
   * Returns the array pointer.
   */
  @Override
  public Value reset()
  {
    return getValue().reset();
  }
  
  /**
   * Shuffles the array.
   */
  @Override
  public Value shuffle()
  {
    return getValue().shuffle();
  }
  
  /**
   * Pops the top array element.
   */
  @Override
  public Value pop(Env env)
  {
    return getValue().pop(env);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, StringValue methodName, int hash,
                          Value []args)
  {
    return getValue().callMethod(env, methodName, hash, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, StringValue methodName, int hash)
  {
    return getValue().callMethod(env, methodName, hash);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, StringValue methodName, int hash,
                          Value a0)
  {
    return getValue().callMethod(env, methodName, hash, a0);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, StringValue methodName, int hash,
                          Value a0, Value a1)
  {
    return getValue().callMethod(env, methodName, hash, a0, a1);
  }

  /**
   * Evaluates a method with 3 args.
   */
  @Override
  public Value callMethod(Env env, StringValue methodName, int hash,
                          Value a0, Value a1, Value a2)
  {
    return getValue().callMethod(env, methodName, hash, a0, a1, a2);
  }

  /**
   * Evaluates a method with 4 args.
   */
  @Override
  public Value callMethod(Env env, StringValue methodName, int hash,
                          Value a0, Value a1, Value a2, Value a3)
  {
    return getValue().callMethod(env, methodName, hash,
                                 a0, a1, a2, a3);
  }

  /**
   * Evaluates a method with 5 args.
   */
  @Override
  public Value callMethod(Env env, StringValue methodName, int hash,
                          Value a0, Value a1, Value a2, Value a3, Value a4)
  {
    return getValue().callMethod(env, methodName, hash,
                                 a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env,
                             StringValue methodName, int hash,
                             Value []args)
  {
    return getValue().callMethodRef(env, methodName, hash, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, StringValue methodName, int hash)
  {
    return getValue().callMethodRef(env, methodName, hash);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, StringValue methodName, int hash,
                             Value a0)
  {
    return getValue().callMethodRef(env, methodName, hash, a0);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, StringValue methodName, int hash,
                             Value a0, Value a1)
  {
    return getValue().callMethodRef(env, methodName, hash,
                                    a0, a1);
  }

  /**
   * Evaluates a method with 3 args.
   */
  @Override
  public Value callMethodRef(Env env, StringValue methodName, int hash,
                             Value a0, Value a1, Value a2)
  {
    return getValue().callMethodRef(env, methodName, hash,
                                    a0, a1, a2);
  }

  /**
   * Evaluates a method with 4 args.
   */
  @Override
  public Value callMethodRef(Env env, StringValue methodName, int hash,
                             Value a0, Value a1, Value a2, Value a3)
  {
    return getValue().callMethodRef(env, methodName, hash,
                                    a0, a1, a2, a3);
  }

  /**
   * Evaluates a method with 5 args.
   */
  @Override
  public Value callMethodRef(Env env, StringValue methodName, int hash,
                             Value a0, Value a1, Value a2, Value a3, Value a4)
  {
    return getValue().callMethodRef(env, methodName, hash,
                                    a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  /*
  @Override
  public Value callClassMethod(Env env, AbstractFunction fun, Value []args)
  {
    return getValue().callClassMethod(env, fun, args);
  }
  */

  /**
   * Prints the value.
   */
  @Override
  public void print(Env env)
  {
    getValue().print(env);
  }

  /**
   * Serializes the value.
   */
  @Override
  public void serialize(Env env, StringBuilder sb, SerializeMap map)
  {
    getValue().serialize(env, sb, map);
  }

  @Override
  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    out.print("&");
    getValue().varDump(env, out, depth, valueSet);
  }

  private void setRaw(Value value)
  {
    _value = value;
  }

  private Value getRawValue()
  {
    return _value;
  }
  
  //
  // Java Serialization
  //

  public Object writeReplace()
  {
    return getValue();
  }
}

