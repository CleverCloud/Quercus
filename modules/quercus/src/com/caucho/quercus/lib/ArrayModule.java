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

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReadOnly;
import com.caucho.quercus.annotation.Reference;
import com.caucho.quercus.annotation.UsesSymbolTable;
import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.env.*;
import com.caucho.quercus.env.ArrayValue.AbstractGet;
import com.caucho.quercus.env.ArrayValue.GetKey;
import com.caucho.quercus.env.ArrayValue.KeyComparator;
import com.caucho.quercus.env.ArrayValue.ValueComparator;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;

import java.text.Collator;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PHP array routines.
 */
public class ArrayModule
  extends AbstractQuercusModule
{
  private static final L10N L = new L10N(ArrayModule.class);

  private static final Logger log =
    Logger.getLogger(ArrayModule.class.getName());

  public static final int CASE_UPPER = 2;
  public static final int CASE_LOWER = 1;

  public static final int SORT_REGULAR = 0;
  public static final int SORT_NUMERIC = 1;
  public static final int SORT_STRING = 2;
  public static final int SORT_LOCALE_STRING = 5;
  public static final int SORT_NORMAL = 1;
  public static final int SORT_REVERSE = -1;

  public static final int SORT_DESC = 3;
  public static final int SORT_ASC = 4;
  
  public static final int EXTR_OVERWRITE = 0;
  public static final int EXTR_SKIP = 1;
  public static final int EXTR_PREFIX_SAME = 2;
  public static final int EXTR_PREFIX_ALL = 3;
  public static final int EXTR_PREFIX_INVALID = 4;
  public static final int EXTR_IF_EXISTS = 6;
  public static final int EXTR_PREFIX_IF_EXISTS = 5;
  public static final int EXTR_REFS = 256;
  
  public static final int COUNT_NORMAL = 0;
  public static final int COUNT_RECURSIVE = 1;

  public static final boolean CASE_SENSITIVE = true;
  public static final boolean CASE_INSENSITIVE = false;
  public static final boolean KEY_RESET = true;
  public static final boolean NO_KEY_RESET = false;
  public static final boolean STRICT = true;
  public static final boolean NOT_STRICT = false;

  private static final CompareString CS_VALUE_NORMAL
    = new CompareString(ArrayValue.GET_VALUE, SORT_NORMAL);

  private static final CompareString CS_VALUE_REVERSE
    = new CompareString(ArrayValue.GET_VALUE, SORT_REVERSE);

  private static final CompareString CS_KEY_NORMAL
    = new CompareString(ArrayValue.GET_KEY, SORT_NORMAL);

  private static final CompareString CS_KEY_REVERSE
    = new CompareString(ArrayValue.GET_KEY, SORT_REVERSE);

  private static final CompareNumeric CN_VALUE_NORMAL
    = new CompareNumeric(ArrayValue.GET_VALUE, SORT_NORMAL);

  private static final CompareNumeric CN_VALUE_REVERSE
    = new CompareNumeric(ArrayValue.GET_VALUE, SORT_REVERSE);

  private static final CompareNumeric CN_KEY_NORMAL
    = new CompareNumeric(ArrayValue.GET_KEY, SORT_NORMAL);

  private static final CompareNumeric CN_KEY_REVERSE
    = new CompareNumeric(ArrayValue.GET_KEY, SORT_REVERSE);

  private static final CompareNormal CNO_VALUE_NORMAL
    = new CompareNormal(ArrayValue.GET_VALUE, SORT_NORMAL);

  private static final CompareNormal CNO_VALUE_REVERSE
    = new CompareNormal(ArrayValue.GET_VALUE, SORT_REVERSE);

  private static final CompareNormal CNO_KEY_NORMAL
    = new CompareNormal(ArrayValue.GET_KEY, SORT_NORMAL);

  private static final CompareNormal CNO_KEY_REVERSE
    = new CompareNormal(ArrayValue.GET_KEY, SORT_REVERSE);

  private static final CompareNatural CNA_VALUE_NORMAL_SENSITIVE
    = new CompareNatural(ArrayValue.GET_VALUE, SORT_NORMAL, CASE_SENSITIVE);

  private static final CompareNatural CNA_VALUE_NORMAL_INSENSITIVE
    = new CompareNatural(ArrayValue.GET_VALUE, SORT_NORMAL, CASE_INSENSITIVE);

  public String []getLoadedExtensions()
  {
    return new String[] { "standard" };
  }

  /**
   * Changes the key case
   */
  public static Value array_change_key_case(
      Env env,
      ArrayValue array,
      @Optional("CASE_LOWER") int toCase) {
    if (array == null)
      return BooleanValue.FALSE;

    ArrayValue newArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      Value keyValue = entry.getKey();

      if (keyValue instanceof StringValue) {
        String key = keyValue.toString();

        if (toCase == CASE_UPPER)
          key = key.toUpperCase();
        else
          key = key.toLowerCase();

        newArray.put(env.createString(key), entry.getValue());
      }
      else
        newArray.put(keyValue, entry.getValue());
    }

    return newArray;
  }

  /**
   * Chunks the array
   */
  public static Value array_chunk(Env env,
                                  ArrayValue array,
                                  int size,
                                  @Optional boolean preserveKeys)
  {
    if (array == null)
      return NullValue.NULL;

    ArrayValue newArray = new ArrayValueImpl();
    ArrayValue currentArray = null;

    if (size < 1) {
      env.warning("Size parameter expected to be greater than 0");

      return NullValue.NULL;
    }

    int i = 0;
    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      Value key = entry.getKey();
      Value value = entry.getValue();

      if (i % size == 0) {
        currentArray = new ArrayValueImpl();
        newArray.put(currentArray);
      }

      if (preserveKeys)
        currentArray.put(key, value);
      else
        currentArray.put(LongValue.create(i % size), value);

      i++;
    }

    return newArray;
  }

  /**
   * Combines array
   */
  public static Value array_combine(Env env,
                                    ArrayValue keys,
                                    ArrayValue values)
  {
    if (keys == null || values == null)
      return BooleanValue.FALSE;

    if (keys.getSize() < 1 || values.getSize() < 1) {
      env.warning("Both parameters should have at least 1 element");

      return BooleanValue.FALSE;
    }

    if (keys.getSize() != values.getSize()) {
      env.warning("Both parameters should have equal number of elements");

      return BooleanValue.FALSE;
    }

    Iterator<Value> keyIter = keys.values().iterator();
    Iterator<Value> valueIter = values.values().iterator();

    ArrayValue array = new ArrayValueImpl();

    while (keyIter.hasNext() && valueIter.hasNext()) {
      array.put(keyIter.next(), valueIter.next());
    }

    return array;
  }

  /**
   * Counts the values
   */
  public static Value array_count_values(Env env, ArrayValue array)
  {
    if (array == null)
      return NullValue.NULL;

    ArrayValue result = new ArrayValueImpl();

    for (Value value : array.values()) {
      if (! (value.isLongConvertible()) && ! (value instanceof StringValue))
        env.warning("Can only count STRING and INTEGER values!");
      else {
        Value count = result.get(value);

        if (count == null)
          count = LongValue.create(1);
        else
          count = count.add(1);

        result.put(value, count);
      }
    }

    return result;
  }

  /**
   * Pops off the top element
   */
  public static Value array_pop(Env env, @Reference Value array)
  {
    return array.pop(env);
  }

  /**
   * Returns the size of the array.
   */
  public static long count(Env env,
                                       @ReadOnly Value value,
                                       @Optional int countMethod)
  {
    boolean isRecursive = countMethod == COUNT_RECURSIVE;

    if (! isRecursive)
      return value.getCount(env);
    else
      return value.getCountRecursive(env);
  }

  /**
   * Returns the current value of the array.
   */
  public static Value current(@ReadOnly Value value)
  {
    return value.current();
  }

  /**
   * Returns the current key of the array.
   */
  public static Value key(@ReadOnly Value value)
  {
    return value.key();
  }

  /**
   * Returns the current value of the array.
   */
  public static Value pos(@ReadOnly Value value)
  {
    return current(value);
  }

  /**
   * Returns the next value of the array.
   */
  public static Value next(@Reference Value value)
  {
    return value.next();
  }

  /**
   * Returns the next value of the array.
   */
  public static Value each(Env env, @Reference Value value)
  {
    if (value instanceof Var) {
      value = value.toValue();
      
      if (value.isArray())
        return value.toArrayValue(env).each();
      else {
        env.warning(L.l("each() requires argument to be an array"));

        return NullValue.NULL;
      }
    }
    else {
      return env.error(L.l("each() argument must be a variable"));
    }
  }

  /**
   * Returns the previous value of the array.
   */
  public static Value prev(@Reference Value array)
  {
    return array.prev();
  }

  /**
   * Resets the pointer
   */
  public static Value reset(@Reference Value array)
  {
    return array.reset();
  }

  /**
   * Returns the current value of the array.
   */
  public static Value shuffle(Env env, @Reference Value array)
  {
    return array.shuffle();
  }

  /**
   * Resets the pointer to the end
   */
  public static Value end(@Reference Value value)
  {
    return value.end();
  }

  /**
   * Checks if the key is in the given array
   *
   * @param key a key to check for in the array
   * @param searchArray the array to search for the key in
   * @return true if the key is in the array, and false otherwise
   */
  public static boolean array_key_exists(Env env,
                                         @ReadOnly Value key,
                                         @ReadOnly Value searchArray)
  {

    
    if (! searchArray.isset() || ! key.isset()) {
      return false;
    }

    if (! (searchArray.isArray() || searchArray.isObject())) {
      env.warning(
          L.l("'" + searchArray.toString()
              + "' is an unexpected argument, expected "
              + "ArrayValue or ObjectValue"));
      return false;
    }

    if (! (key.isString() || key.isLongConvertible())) {
      env.warning(
          L.l(
              "The first argument (a '{0}') should be "
                  + "either a string or an integer",
                      key.getType()));
      return false;
    }

    return searchArray.keyExists(key);
  }

  /**
   * Undocumented alias for {@link #array_key_exists}.
   */
  public static boolean key_exists(Env env,
                                   @ReadOnly Value key,
                                   @ReadOnly Value searchArray)
  {
    return array_key_exists(env, key, searchArray);
  }

  /**
   * Returns an array of the keys in the given array
   *
   * @param array the array to obtain the keys for
   * @param searchValue the corresponding value of the returned key array
   * @return an array containing the keys
   */
  public static Value array_keys(Env env,
                                 @ReadOnly ArrayValue array,
                                 @Optional @ReadOnly Value searchValue,
                                 @Optional boolean isStrict)
  {
    if (array == null)
      return NullValue.NULL;

    if (searchValue.isDefault())
      return array.getKeys();
    
    ArrayValue newArray = new ArrayValueImpl(array.getSize());

    int i = 0;
    
    Iterator<Map.Entry<Value,Value>> iter = array.getIterator(env);
    
    while (iter.hasNext()) {
      Map.Entry<Value,Value> entry = iter.next();
      Value entryKey = entry.getKey();
      Value entryValue = entry.getValue();

      if (entryValue.eq(searchValue))
        newArray.append(LongValue.create(i++), entryKey);
    }

    return newArray;
  }

  /**
   * Returns an array with a number of indices filled with the given value,
   * starting at the start index.
   *
   * @param start the index to start filling the array
   * @param num the number of entries to fill
   * @param value the value to fill the entries with
   * @return an array filled with the given value starting from the given start
   *         index
   */
  public static Value array_fill(Env env, long start, long num, Value value)
  {
    if (num < 0) {
      env.warning("Number of elements must be positive");

      return BooleanValue.FALSE;
    }

    ArrayValue array = new ArrayValueImpl();

    for (long k = start; k < num + start; k++)
      array.put(LongValue.create(k), value.copy());

    return array;
  }

  /**
   * Returns an array with the given array's keys as values and its values as
   * keys.  If the given array has matching values, the latest value will be
   * transfered and the others will be lost.
   *
   * @param array the array to flip
   * @return an array with it's keys and values swapped
   */
  public static Value array_flip(Env env,
                                 ArrayValue array)
  {
    if (array == null)
      return BooleanValue.FALSE;

    ArrayValue newArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      Value entryValue = entry.getValue();

      if (entryValue.isLongConvertible()
          || entryValue instanceof StringValue)
        newArray.put(entryValue, entry.getKey());
      else {
        env.warning(L.l("Can only flip string and integer values at '{0}'",
                        entryValue));
      }
    }

    return newArray;
  }

  /**
   * Returns an array with either the front/end padded with the pad value.  If
   * the pad size is positive, the padding is performed on the end.  If
   * negative, then the array is padded on the front.  The pad size is the new
   * array size.  If this size is not greater than the current array size, then
   * the original input array is returned.
   *
   * @param input the array to pad
   * @param padSize the amount to pad the array by
   * @param padValue determines front/back padding and the value to place in the
   * padded space
   * @return a padded array
   */
  public static Value array_pad(Env env,
                                ArrayValue input,
                                long padSize,
                                Value padValue)
  {
    if (input == null)
      return NullValue.NULL;

    long inputSize = input.getSize();

    long size = Math.abs(padSize);

    if (input.getSize() >= size)
      return input;

    if (size - inputSize > 1048576) {
      env.warning("You may only pad up to 1048576 elements at a time");

      return BooleanValue.FALSE;
    }

    ArrayValue paddedArray = new ArrayValueImpl();

    boolean padFront = padSize < 0;

    Iterator<Value> keyIterator = input.keySet().iterator();

    for (long ctr = 0; ctr < size; ctr++) {
      Value newValue;

      if (padFront && ctr < size - inputSize)
        newValue = padValue;
      else if ((! padFront) && ctr >= inputSize)
        newValue = padValue;
      else
        newValue = input.get(keyIterator.next());

      paddedArray.put(LongValue.create(ctr), newValue);
    }

    return paddedArray;
  }

  /**
   * Returns an array that filters out any values that do not hold true when
   * used in the callback function.
   *
   * @param array the array to filter
   * @param callback the function name for filtering
   * @return a filtered array
   */
  public static Value array_filter(Env env,
                                   ArrayValue array,
                                   @Optional Value callbackName)
  {
    if (array == null)
      return NullValue.NULL;

    ArrayValue filteredArray = new ArrayValueImpl();

    if (! callbackName.isDefault()) {
      Callable callback = callbackName.toCallable(env);
      
      if (callback == null || ! callback.isValid(env)) {
        return NullValue.NULL;
      }

      try {
        Iterator<Map.Entry<Value,Value>> iter = array.getIterator(env);

        while (iter.hasNext()) {
          Map.Entry<Value,Value> entry = iter.next();
          
          Value key = entry.getKey();
          Value value;
          
          if (entry instanceof ArrayValue.Entry)
            value = ((ArrayValue.Entry) entry).getRawValue();
          else
            value = entry.getValue();
 
          // php/1740          
          boolean isMatch 
            = callback.callArray(env, array, key, value).toBoolean();
          
          if (isMatch)
            filteredArray.put(key, value);
        }
      }
      catch (Exception t) {
        log.log(Level.WARNING, t.toString(), t);
        env.warning("An error occurred while invoking the filter callback");

        return NullValue.NULL;
      }
    }
    else {
      for (Map.Entry<Value, Value> entry : array.entrySet()) {
        if (entry.getValue().toBoolean())
          filteredArray.put(entry.getKey(), entry.getValue());
      }
    }

    return filteredArray;
  }

  /**
   * Returns the product of the input array's elements as a double.
   *
   * @param array the array for who's product is to be found
   * @return the produce of the array's elements
   */
  public static Value array_product(Env env,
                                    ArrayValue array)
  {
    if (array == null)
      return NullValue.NULL;

    if (array.getSize() == 0)
      return DoubleValue.create(0);

    double product = 1;

    for (Map.Entry<Value, Value> entry : array.entrySet())
      product *= entry.getValue().toDouble();

    return DoubleValue.create(product);
  }

  /**
   * Appends a value to the array
   *
   * @return the number of elements in the final array
   */
  public static int array_push(Env env,
                               @Reference Value array,
                               Value []values)
  {
    for (Value value : values) {
      array.put(value);
    }

    return array.getSize();
  }

  /**
   * Returns num sized array of random keys from the given array
   *
   * @param array the array from which the keys will come from
   * @param num the number of random keys to return
   * @return the produce of the array's elements
   */
  public static Value array_rand(Env env,
                          ArrayValue array,
                          @Optional("1") long num)
  {
    if (array == null)
      return NullValue.NULL;

    if (array.getSize() == 0)
      return NullValue.NULL;

    if (num < 1 || array.getSize() < num) {
      env.warning("Second argument has to be between 1 and the number of "
          + "elements in the array");

      return NullValue.NULL;
    }

    long arraySize = array.getSize();

    Value[] keys = new Value[(int) arraySize];

    array.keySet().toArray(keys);

    if (num == 1) {
      int index = (int) (RandomUtil.getRandomLong() % arraySize);

      if (index < 0)
        index *= -1;

      return keys[index];
    }

    int length = keys.length;
    for (int i = 0; i < length; i++) {
      int rand = RandomUtil.nextInt(length);

      Value temp = keys[rand];
      keys[rand] = keys[i];
      keys[i] = temp;
    }

    ArrayValue randArray = new ArrayValueImpl();

    for (int i = 0; i < num; i++) {
      randArray.put(keys[i]);
    }

    return randArray;
  }

  /**
   * Returns the value of the array when its elements have been reduced using
   * the callback function.
   *
   * @param array the array to reduce
   * @param callback the function to use for reducing the array
   * @param initialValue used as the element before the first element of the
   * array for purposes of using the callback function
   * @return the result from reducing the input array with the callback
   *         function
   */
  public static Value array_reduce(Env env,
                                   ArrayValue array,
                                   Callable callable,
                                   @Optional("NULL") Value initialValue)
  {
    if (array == null)
      return NullValue.NULL;

    if (callable == null || ! callable.isValid(env)) {
      env.warning("The second argument, '" + callable
                  + "', should be a valid callable");

      return NullValue.NULL;
    }

    Value result = initialValue;

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      try {
        // XXX: will this callback modify the array?
        result = callable.call(env, result, entry.getValue());
      }
      catch (Exception t) {
        // XXX: may be used for error checking later
        log.log(Level.WARNING, t.toString(), t);
        env.warning("An error occurred while invoking the reduction callback");

        return NullValue.NULL;
      }
    }

    return result;
  }

  /**
   * Returns the inputted array reversed, preserving the keys if keyed is true
   *
   * @param inputArray the array to reverse
   * @param keyed true if the keys are to be preserved
   * @return the array in reverse
   */
  public static Value array_reverse(Env env,
                                    ArrayValue inputArray,
                                    @Optional("false") boolean keyed)
  {
    if (inputArray == null)
      return NullValue.NULL;

    Map.Entry<Value, Value>[] entryArray =
      new Map.Entry[inputArray.getSize()];

    inputArray.entrySet().toArray(entryArray);

    ArrayValue newArray = new ArrayValueImpl();

    int newIndex = 0;

    for (int index = entryArray.length - 1; index > -1; index--) {
      Value currentKey = entryArray[index].getKey();

      Value currentValue = entryArray[index].getValue();

      if (keyed || (currentKey instanceof StringValue))
        newArray.put(currentKey, currentValue);
      else {
        newArray.put(LongValue.create(newIndex), currentValue);

        newIndex++;
      }
    }

    return newArray;
  }

  /**
   * Returns the key of the needle being searched for or false if it's not
   * found
   *
   * @param needle the value to search for
   * @param array the array to search
   * @param strict checks for type aswell
   * @return the key of the needle
   */
  public static Value array_search(Env env,
                                   @ReadOnly Value needle,
                                   @ReadOnly ArrayValue array,
                                   @Optional("false") boolean strict)
  {
    // php/171i
    // php/172y
    
    if (array == null)
      return BooleanValue.FALSE;

    Iterator<Map.Entry<Value, Value>> iterator = array.getIterator(env);

    while (iterator.hasNext()) {
      Map.Entry<Value, Value> entry = iterator.next();

      Value entryValue = entry.getValue();
      Value entryKey = entry.getKey();

      if (needle.eq(entryValue)) {
        if (strict) {
          if ((entryValue.getType()).equals(needle.getType()))
            return entryKey;
        }
        else
          return entryKey;
      }
    }

    return BooleanValue.FALSE;
  }

  /**
   * Shifts the elements in the array left by one, returning the leftmost value
   *
   * @param array the array to shift
   * @return the left most value in the array
   */
  public static Value array_shift(Env env,
                                  @Reference Value value)
  {
    if (! value.isArray()) {
      env.warning(L.l("cannot shift a non-array"));
      return NullValue.NULL;
    }
    
    ArrayValue array = value.toArrayValue(env);

    if (array.getSize() < 1)
      return NullValue.NULL;

    Iterator<Value> iter = array.getKeyIterator(env);
    
    Value firstValue = array.remove(iter.next());

    array.keyReset(0, NOT_STRICT);

    return firstValue;
  }

  /**
   * Returns a chunk of the array.  The offset is the start index, elements is
   * the number of values to take, and presKeys is if the keys are to be
   * preserved. If offset is negative, then it's that number from the end of the
   * array.  If elements is negative, then the new array will have from offset
   * to elements number of values.
   *
   * @param array the array to take the chunk from
   * @param offset the start index for the new array chunk
   * @param elements the number of elements in the array chunk
   * @param presKeys true if the keys of the elements are to be preserved, false
   * otherwise
   * @return the array chunk
   */
  public static Value array_slice(Env env,
                                  @ReadOnly ArrayValue array,
                                  int offset,
                                  @Optional Value length,
                                  @Optional boolean isPreserveKeys)
  {
    if (array == null)
      return NullValue.NULL;

    int size = array.getSize();

    int startIndex = offset;

    if (offset < 0)
      startIndex = size + offset;

    int endIndex = size;

    if (! length.isDefault()) {
      endIndex = length.toInt();

      if (endIndex < 0)
        endIndex += size;
      else
        endIndex += startIndex;
    }

    return array.slice(env, startIndex, endIndex, isPreserveKeys);
  }

  /**
   * Returns the removed chunk of the arrayV and splices in replace.  If offset
   * is negative, then the start index is that far from the end.  Otherwise, it
   * is the start index.  If length is not given then from start index to the
   * end is removed.  If length is negative, that is the index to stop removing
   * elements.  Otherwise that is the number of elements to remove.  If replace
   * is given, replace will be inserted into the arrayV at offset.
   *
   * @param array the arrayV to splice
   * @param offset the start index for the new arrayV chunk
   * @param length the number of elements to remove / stop index
   * @param replace the elements to add to the arrayV
   * @return the part of the arrayV removed from input
   */
  public static Value array_splice(Env env,
                                   @Reference Value arrayVar,
                                   int offset,
                                   @Optional("NULL") Value length,
                                   @Optional Value replace)
  {
    if (! arrayVar.isset())
      return NullValue.NULL;

    ArrayValue array = arrayVar.toArrayValue(env);

    if (array == null)
      return NullValue.NULL;
    
    int size = array.getSize();

    int startIndex = offset;

    if (startIndex < 0)
      startIndex += size;

    int endIndex = size;

    if (! length.isNull()) {
      endIndex = length.toInt();

      if (endIndex < 0)
        endIndex += size;
      else
        endIndex += startIndex;
    }

    return spliceImpl(env, arrayVar, array, startIndex, endIndex,
                      (ArrayValue) replace.toArray());
  }

  public static Value spliceImpl(Env env,
                                 Value var,
                                 ArrayValue array,
                                 int start,
                                 int end,
                                 ArrayValue replace)
  {
    int index = 0;

    ArrayValue newArray = new ArrayValueImpl();
    ArrayValue result = new ArrayValueImpl();

    var.set(newArray);

    for (Map.Entry<Value,Value> entry : array.entrySet()) {
      Value key = entry.getKey();
      Value value = entry.getValue();
      
      if (start == index && replace != null) {
        Iterator<Value> replaceIter = replace.getValueIterator(env);
        while (replaceIter.hasNext()) {
          newArray.put(replaceIter.next());
        }
      }
      
      if (start <= index && index < end) {
        if (key.isString())
          result.put(key, value);
        else
          result.put(value);
      }
      else {
        if (key.isString())
          newArray.put(key, value);
        else
          newArray.put(value);
      }

      index++;
    }

    if (index <= start && replace != null) {
      Iterator<Value> replaceIter = replace.getValueIterator(env);
      while (replaceIter.hasNext()) {
        newArray.put(replaceIter.next());
      }
    }

    return result;
  }

  /**
   * Returns the sum of the elements in the array
   *
   * @param array the array to sum
   * @return the sum of the elements
   */
  public static Value array_sum(Env env,
                                @ReadOnly ArrayValue array)
  {
    if (array == null)
      return NullValue.NULL;

    double sum = 0;

    for (Map.Entry<Value, Value> entry : array.entrySet())
      sum += entry.getValue().toDouble();

    return DoubleValue.create(sum);
  }

  // XXX: array_udiff
  // XXX: array_udiff_assoc
  // XXX: array_udiff_uassoc

  // XXX: array_uintersect
  // XXX: array_uintersect_assoc
  // XXX: array_uintersect_uassoc

  /**
   * Returns the inputted array without duplicates
   *
   * @param array the array to get rid of the duplicates from
   * @return an array without duplicates
   */
  public static Value array_unique(Env env,
                                   ArrayValue array)
  {
    if (array == null)
      return BooleanValue.FALSE;

    array.sort(CNO_VALUE_NORMAL, NO_KEY_RESET, NOT_STRICT);

    Map.Entry<Value, Value> lastEntry = null;

    ArrayValue uniqueArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      Value entryValue = entry.getValue();

      if (lastEntry == null) {
        uniqueArray.put(entry.getKey(), entryValue);

        lastEntry = entry;

        continue;
      }

      Value lastEntryValue = lastEntry.getValue();

      if (! entryValue.toString().equals(lastEntryValue.toString()))
        uniqueArray.put(entry.getKey(), entryValue);

      lastEntry = entry;
    }

    uniqueArray.sort(CNO_KEY_NORMAL, NO_KEY_RESET, NOT_STRICT);

    return uniqueArray;
  }

  /**
   * Prepends the elements to the array
   *
   * @param array the array to shift
   * @param values
   * @return the left most value in the array
   */
  public static Value array_unshift(Env env,
                                    @Reference Value value,
                                    Value []values)
  {
    ArrayValue array = value.toArrayValue(env);
    
    if (array == null)
      return BooleanValue.FALSE;

    for (int i = values.length - 1; i >= 0; i--) {
      array.unshift(values[i]);
    }

    array.keyReset(0, NOT_STRICT);

    return LongValue.create(array.getSize());
  }

  /**
   * Returns the values in the passed array with numerical indices.
   *
   * @param array the array to get the values from
   * @return an array with the values of the passed array
   */
  public static Value array_values(Env env,
                                   ArrayValue array)
  {
    if (array == null)
      return NullValue.NULL;

    return array.getValues();
  }

  /**
   * Executes a callback on each of the elements in the array.
   *
   * @param array the array to walk along
   * @param callback the callback function
   * @param userData extra parameter required by the callback function
   *
   * @return true if the walk succeeded, false otherwise
   */
  public static boolean array_walk(Env env,
                                   @Reference Value arrayVar,
                                   Callable callback,
                                   @Optional("NULL") Value userData)
  {
    if (callback == null || ! callback.isValid(env)) {
      env.error(L.l("'{0}' is an unknown function.",
                    callback.getCallbackName()));
      return false;
    }
    
    ArrayValue array = arrayVar.toArrayValue(env);

    if (array == null)
      return false;

    try {
      Iterator<Map.Entry<Value,Value>> iter = array.getIterator(env);

      while (iter.hasNext()) {
        Map.Entry<Value,Value> entry = iter.next();
        
        Value key = entry.getKey();
        Value value;
        
        // php/1741
        if (entry instanceof ArrayValue.Entry)
          value = ((ArrayValue.Entry) entry).getRawValue();
        else
          value = entry.getValue();
        
        callback.callArray(env, array, key, value, key, userData);
      }
      
      return true;
    }
    catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      env.warning("An error occured while invoking the callback", e);
      
      return false;
    }
  }

  /**
   * Recursively executes a callback function on all elements in the array,
   * including elements of elements (i.e., arrays within arrays).  Returns true
   * if the process succeeded, otherwise false.
   *
   * @param array the array to walk
   * @param call the name of the callback function
   * @param extra extra parameter required by the callback function
   * @return true if the walk succedded, false otherwise
   */
  public static boolean array_walk_recursive(Env env,
                                             @Reference Value arrayVar,
                                             Callable callback,
                                             @Optional("NULL") Value extra)
  {
    if (callback == null || ! callback.isValid(env)) {
      env.error(
          L.l("'{0}' is an unknown function.", callback.getCallbackName()));
      return false;
    }
    
    ArrayValue array = arrayVar.toArrayValue(env);

    if (array == null)
      return false;

    try {
      Iterator<Map.Entry<Value,Value>> iter = array.getIterator(env);

      while (iter.hasNext()) {
        Map.Entry<Value,Value> entry = iter.next();
        
        Value key = entry.getKey();
        Value value;
        
        // php/1742
        if (entry instanceof ArrayValue.Entry)
          value = ((ArrayValue.Entry) entry).getRawValue();
        else
          value = entry.getValue();

        if (value.isArray()) {
          boolean result = array_walk_recursive(env,
                                                (ArrayValue) value.toValue(),
                                                callback,
                                                extra);

          if (! result)
            return false;
        }
        else
          callback.callArray(env, array, key, value, key, extra);
      }

      return true;
    }
    catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      env.warning("An error occured while invoking the callback", e);

      return false;
    }
  }

  /**
   * Sorts the array based on values in reverse order, preserving keys
   *
   * @param array the array to sort
   * @param sortFlag provides optional methods to process the sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public static boolean arsort(Env env,
                               @Reference Value arrayVar,
                               @Optional long sortFlag)
  {
    ArrayValue array = arrayVar.toArrayValue(env);
    
    if (array == null)
      return false;

    switch ((int) sortFlag) {
    case SORT_STRING:
      array.sort(CS_VALUE_REVERSE, NO_KEY_RESET, NOT_STRICT);
      break;
    case SORT_NUMERIC:
      array.sort(CN_VALUE_REVERSE, NO_KEY_RESET, NOT_STRICT);
      break;
    case SORT_LOCALE_STRING:
      Locale locale = env.getLocaleInfo().getCollate().getLocale();
      array.sort(new CompareLocale(ArrayValue.GET_VALUE, SORT_REVERSE,
                                   Collator.getInstance(locale)),
                 NO_KEY_RESET, NOT_STRICT);
      break;
    default:
      array.sort(CNO_VALUE_REVERSE, NO_KEY_RESET, NOT_STRICT);
      break;
    }

    return true;
  }

  /**
   * Sorts the array based on values in ascending order, preserving keys
   *
   * @param array the array to sort
   * @param sortFlag provides optional methods to process the sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public static boolean asort(Env env,
                              @Reference Value arrayVar,
                              @Optional long sortFlag)
  {
    ArrayValue array = arrayVar.toArrayValue(env);
    
    if (array == null)
      return false;

    switch ((int) sortFlag) {
    case SORT_STRING:
      array.sort(CS_VALUE_NORMAL, NO_KEY_RESET, NOT_STRICT);
      break;
    case SORT_NUMERIC:
      array.sort(CN_VALUE_NORMAL, NO_KEY_RESET, NOT_STRICT);
      break;
    case SORT_LOCALE_STRING:
      Locale locale = env.getLocaleInfo().getCollate().getLocale();
      array.sort(new CompareLocale(ArrayValue.GET_VALUE, SORT_NORMAL,
                                   Collator.getInstance(locale)),
                 NO_KEY_RESET, NOT_STRICT);
      break;
    default:
      array.sort(CNO_VALUE_NORMAL, NO_KEY_RESET, NOT_STRICT);
      break;
    }

    return true;
  }

  /**
   * Sorts the array based on keys in ascending order, preserving keys
   *
   * @param array the array to sort
   * @param sortFlag provides optional methods to process the sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public static boolean ksort(Env env,
                              @Reference Value arrayVar,
                              @Optional long sortFlag)
  {
    ArrayValue array = arrayVar.toArrayValue(env);
    
    if (array == null)
      return false;

    switch ((int) sortFlag) {
    case SORT_STRING:
      array.sort(CS_KEY_NORMAL, NO_KEY_RESET, NOT_STRICT);
      break;
    case SORT_NUMERIC:
      array.sort(CN_KEY_NORMAL, NO_KEY_RESET, NOT_STRICT);
      break;
    case SORT_LOCALE_STRING:
      Locale locale = env.getLocaleInfo().getCollate().getLocale();
      array.sort(new CompareLocale(ArrayValue.GET_KEY, SORT_NORMAL,
                                   Collator.getInstance(locale)),
                 NO_KEY_RESET, NOT_STRICT);
      break;
    default:
      array.sort(CNO_KEY_NORMAL, NO_KEY_RESET, NOT_STRICT);
      break;
    }

    return true;
  }

  /**
   * Sorts the array based on keys in reverse order, preserving keys
   *
   * @param array the array to sort
   * @param sortFlag provides optional methods to process the sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public static boolean krsort(Env env,
                               @Reference Value arrayVar,
                               @Optional long sortFlag)
  {
    ArrayValue array = arrayVar.toArrayValue(env);
    
    if (array == null)
      return false;

    switch ((int) sortFlag) {
    case SORT_STRING:
      array.sort(CS_KEY_REVERSE, NO_KEY_RESET, NOT_STRICT);
      break;
    case SORT_NUMERIC:
      array.sort(CN_KEY_REVERSE, NO_KEY_RESET, NOT_STRICT);
      break;
    case SORT_LOCALE_STRING:
      Locale locale = env.getLocaleInfo().getCollate().getLocale();
      array.sort(new CompareLocale(ArrayValue.GET_KEY, SORT_REVERSE,
                                   Collator.getInstance(locale)),
                 NO_KEY_RESET, NOT_STRICT);
      break;
    default:
      array.sort(CNO_KEY_REVERSE, NO_KEY_RESET, NOT_STRICT);
      break;
    }

    return true;
  }

  /**
   * Sorts the array based on string values using natural order, preserving
   * keys, case sensitive
   *
   * @param array the array to sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public static Value natsort(Env env, @Reference Value arrayVar)
  {
    ArrayValue array = arrayVar.toArrayValue(env);
    
    if (array == null)
      return NullValue.NULL;

    trimArrayStrings(array);

    array.sort(CNA_VALUE_NORMAL_SENSITIVE, NO_KEY_RESET, NOT_STRICT);

    return BooleanValue.TRUE;
  }

  /**
   * Sorts the array based on string values using natural order, preserving
   * keys, case insensitive
   *
   * @param array the array to sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public static Value natcasesort(Env env, @Reference Value arrayVar)
  {
    ArrayValue array = arrayVar.toArrayValue(env);
    
    if (array == null)
      return NullValue.NULL;

    trimArrayStrings(array);

    array.sort(CNA_VALUE_NORMAL_INSENSITIVE, NO_KEY_RESET, NOT_STRICT);

    return BooleanValue.TRUE;
  }

  /**
   * Helper function for natsort and natcasesort to trim the string in the
   * array
   *
   * @param array the array to trim strings from
   */
  private static void trimArrayStrings(ArrayValue array)
  {
    if (array != null) {

      for (Map.Entry<Value, Value> entry : array.entrySet()) {
        Value entryValue = entry.getValue();

        if (entryValue instanceof StringValue)
          array.put(entry.getKey(),
                    StringValue.create(entryValue.toString().trim()));
      }
    }
  }

  // XXX: compact

  /**
   * Determines if the key is in the array
   *
   * @param needle the array to sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public static boolean in_array(@ReadOnly Value needle,
                                 @ReadOnly ArrayValue stack,
                                 @Optional("false") boolean strict)
  {
    if (stack == null)
      return false;

    Value result;
    
    if (strict)
      result = stack.containsStrict(needle);
    else
      result = stack.contains(needle);
    
    return ! result.isNull();
  }

  /**
   * Sorts the array based on values in ascending order
   *
   * @param array the array to sort
   * @param sortFlag provides optional methods to process the sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public static boolean sort(Env env,
                             @Reference Value arrayVar,
                             @Optional long sortFlag)
  {
    ArrayValue array = arrayVar.toArrayValue(env);
    
    if (array == null)
      return false;

    switch ((int) sortFlag) {
    case SORT_STRING:
      array.sort(CS_VALUE_NORMAL, KEY_RESET, STRICT);
      break;
    case SORT_NUMERIC:
      array.sort(CN_VALUE_NORMAL, KEY_RESET, STRICT);
      break;
    case SORT_LOCALE_STRING:
      Locale locale = env.getLocaleInfo().getCollate().getLocale();
      array.sort(new CompareLocale(ArrayValue.GET_VALUE, SORT_NORMAL,
                                   Collator.getInstance(locale)),
                 KEY_RESET, STRICT);
      break;
    default:
      array.sort(CNO_VALUE_NORMAL, KEY_RESET, STRICT);
      break;
    }

    return true;
  }

  /**
   * Sorts the array based on values in reverse order
   *
   * @param array the array to sort
   * @param sortFlag provides optional methods to process the sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public static boolean rsort(Env env,
                              @Reference Value arrayVar,
                              @Optional long sortFlag)
  {
    ArrayValue array = arrayVar.toArrayValue(env);
    
    if (array == null)
      return false;

    switch ((int) sortFlag) {
    case SORT_STRING:
      array.sort(CS_VALUE_REVERSE, KEY_RESET, STRICT);
      break;
    case SORT_NUMERIC:
      array.sort(CN_VALUE_REVERSE, KEY_RESET, STRICT);
      break;
    case SORT_LOCALE_STRING:
      Locale locale = env.getLocaleInfo().getCollate().getLocale();
      array.sort(new CompareLocale(ArrayValue.GET_VALUE, SORT_REVERSE,
                                   Collator.getInstance(locale)),
                 KEY_RESET, STRICT);
      break;
    default:
      array.sort(CNO_VALUE_REVERSE, KEY_RESET, STRICT);
      break;
    }

    return true;
  }

  /**
   * Sorts the array based on values in ascending order using a callback
   * function
   *
   * @param array the array to sort
   * @param func the name of the callback function
   * @param sortFlag provides optional methods to process the sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public static boolean usort(Env env,
                              @Reference Value arrayVar,
                              Callable func,
                              @Optional long sortFlag)
  {
    ArrayValue array = arrayVar.toArrayValue(env);
    
    if (array == null)
      return false;

    if (func == null)
      return false;
    else if (! func.isValid(env)) {
      env.warning(L.l("Invalid comparison function"));
      return false;
    }

    CompareCallBack cmp;

    // XXX: callback needs to be able to modify array?
    cmp = new CompareCallBack(ArrayValue.GET_VALUE, SORT_NORMAL, func, env);

    array.sort(cmp, KEY_RESET, STRICT);

    return true;
  }

  /**
   * Sorts the array based on values in ascending order using a callback
   * function
   *
   * @param array the array to sort
   * @param func the name of the callback function
   * @param sortFlag provides optional methods to process the sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public static boolean uasort(Env env,
                               @Reference Value arrayVar,
                               Callable func,
                               @Optional long sortFlag)
  {
    ArrayValue array = arrayVar.toArrayValue(env);
    
    if (array == null)
      return false;
    
    if (func == null)
      return false;

    if (! func.isValid(env)) {
      env.warning(L.l("Invalid comparison function"));
      return false;
    }

    // XXX: callback needs to be able to modify array?
    array.sort(new CompareCallBack(ArrayValue.GET_VALUE, SORT_NORMAL, func,
                                   env), NO_KEY_RESET, NOT_STRICT);

    return true;
  }

  /**
   * Sorts the array based on values in ascending order using a callback
   * function
   *
   * @param array the array to sort
   * @param func the name of the callback function
   * @param sortFlag provides optional methods to process the sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public static boolean uksort(Env env,
                               @Reference Value arrayVar,
                               Callable func,
                               @Optional long sortFlag)
  {
    ArrayValue array = arrayVar.toArrayValue(env);
    
    if (array == null)
      return false;

    if (!func.isValid(env)) {
      env.warning(L.l("Invalid comparison function"));
      return false;
    }

    CompareCallBack cmp;

    // XXX: callback needs to be able to modify array?
    cmp = new CompareCallBack(ArrayValue.GET_KEY, SORT_NORMAL, func, env);

    array.sort(cmp, NO_KEY_RESET, NOT_STRICT);

    return true;
  }

  /**
   * Creates an array using the start and end values provided
   *
   * @param start the 0 index element
   * @param end the length - 1 index element
   * @param step the new value is increased by this to determine the value for
   * the next element
   * @return the new array
   */
  public static Value range(Env env,
                            @ReadOnly Value start,
                            @ReadOnly Value end,
                            @Optional("1") long step)
  {
    if (step < 1)
      step = 1;

    if (!start.getType().equals(end.getType())) {
      start = LongValue.create(start.toLong());
      end = LongValue.create(end.toLong());
    }
    else if (Character.isDigit(start.toChar())) {
      start = LongValue.create(start.toLong());
      end = LongValue.create(end.toLong());
    }
    else {
      start = rangeIncrement(start, 0);
      end = rangeIncrement(end, 0);
    }

    if (start.eq(end)) {
    }
    else if (start instanceof StringValue
             && (Math.abs(end.toChar() - start.toChar()) < step)) {
      env.warning("steps exceeds the specified range");

      return BooleanValue.FALSE;
    }
    else if (start instanceof LongValue
        && (Math.abs(end.toLong() - start.toLong()) < step)) {
      env.warning("steps exceeds the specified range");

      return BooleanValue.FALSE;
    }

    boolean increment = true;

    if (! end.geq(start)) {
      step *= -1;
      increment = false;
    }

    ArrayValue array = new ArrayValueImpl();

    do {
      array.put(start);

      start = rangeIncrement(start, step);
    } while ((increment && start.leq(end))
        || (!increment && start.geq(end)));

    return array;
  }

  private static Value rangeIncrement(Value value, long step)
  {
    if (value.isString())
      return StringValue.create((char) (value.toChar() + step));

    return LongValue.create(value.toLong() + step);
  }

  // XXX:You'll need to mark the function as XXX:, because I need to add an
  // attribute like @ModifiedSymbolTable and change some analysis of the
  // compilation based on that attribute.
  //
  // Basically, the compiled mode uses Java variables to store PHP
  // variables.  The extract() call messes that up, or at least forces the
  // compiler to synchronize its view of the variables.
  // (email Re:extract: symbol table)

  /**
   * Inputs new variables into the symbol table from the passed array
   *
   * @param array the array contained the new variables
   * @return the number of new variables added from the array to the symbol
   *         table
   */
  @UsesSymbolTable(replace = true)
  public static Value extract(Env env,
                              ArrayValue array)
  {
    if (array == null)
      return NullValue.NULL;

    int completedSymbols = 0;

    for (Value entryKey : array.keySet()) {
      Value entryValue;

      entryValue = array.get(entryKey);

      StringValue symbolName = entryKey.toStringValue();

      if (validVariableName(symbolName)) {
        env.setValue(symbolName, entryValue);

        completedSymbols++;
      }
    }

    return LongValue.create(completedSymbols);
  }

  /**
   * Inputs new variables into the symbol table from the passed array
   *
   * @param array the array contained the new variables
   * @param rawType flag to determine how to handle collisions
   * @param valuePrefix used along with the flag
   * @return the number of new variables added from the array to the symbol
   *         table
   */
  @UsesSymbolTable
  public static Value extract(Env env,
                              ArrayValue array,
                              long rawType,
                              @Optional("NULL") Value valuePrefix)
  {
    if (array == null)
      return NullValue.NULL;

    long extractType = rawType & ~EXTR_REFS;

    boolean extrRefs = (rawType & EXTR_REFS) != 0;

    if (extractType < EXTR_OVERWRITE
        || extractType > EXTR_IF_EXISTS && extractType != EXTR_REFS) {
      env.warning("Unknown extract type");

      return NullValue.NULL;
    }

    if (extractType >= EXTR_PREFIX_SAME
        && extractType <= EXTR_PREFIX_IF_EXISTS
        && (valuePrefix == null || ! (valuePrefix.isString()))) {
      env.warning("Prefix expected to be specified");

      return NullValue.NULL;
    }

    String prefix = "";

    if (valuePrefix instanceof StringValue)
      prefix = valuePrefix.toString() + "_";

    int completedSymbols = 0;

    for (Value entryKey : array.keySet()) {
      Value entryValue;

      if (extrRefs)
        entryValue = array.getVar(entryKey);
      else
        entryValue = array.get(entryKey);

      StringValue symbolName = entryKey.toStringValue();

      Value tableValue = env.getValue(symbolName);

      switch ((int) extractType) {
      case EXTR_SKIP:
        if (! tableValue.isNull())
          symbolName = env.createString("");

        break;
      case EXTR_PREFIX_SAME:
        if (! tableValue.isNull())
          symbolName = env.createString(prefix + symbolName);

        break;
      case EXTR_PREFIX_ALL:
        symbolName = env.createString(prefix + symbolName);

        break;
      case EXTR_PREFIX_INVALID:
        if (! validVariableName(symbolName))
          symbolName = env.createString(prefix + symbolName);

        break;
      case EXTR_IF_EXISTS:
        if (tableValue.isNull())
          symbolName = env.createString("");//entryValue = tableValue;

        break;
      case EXTR_PREFIX_IF_EXISTS:
        if (! tableValue.isNull())
          symbolName = env.createString(prefix + symbolName);
        else
          symbolName = env.createString("");

        break;
      default:

        break;
      }

      if (validVariableName(symbolName)) {
        env.setValue(symbolName, entryValue);

        completedSymbols++;
      }
    }

    return LongValue.create(completedSymbols);
  }

  /**
   * Helper function for extract to determine if a variable name is valid
   *
   * @param variableName the name to check
   * @return true if the name is valid, false otherwise
   */
  private static boolean validVariableName(StringValue variableName)
  {
    if (variableName.length() < 1)
      return false;

    char checkChar = variableName.charAt(0);

    if (! Character.isLetter(checkChar) && checkChar != '_')
      return false;

    for (int k = 1; k < variableName.length(); k++) {
      checkChar = variableName.charAt(k);

      if (!Character.isLetterOrDigit(checkChar) && checkChar != '_')
        return false;
    }

    return true;
  }

  /**
   * Returns an array with everything that is in array and not in the other
   * arrays using a passed callback function for comparing
   *
   * @param array the primary array
   * @param arrays the vector of arrays to check the primary array's values
   * against
   * @return an array with all of the values that are in the primary array but
   *         not in the other arrays
   */
  public static Value array_diff(Env env, ArrayValue array, Value []arrays)
  {
    if (array == null)
      return NullValue.NULL;

    if (arrays.length < 1) {
      env.warning("Wrong parameter count for array_diff()");

      return NullValue.NULL;
    }

    ArrayValue diffArray = new ArrayValueImpl();

    boolean valueFound;

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      valueFound = false;

      Value entryValue = entry.getValue();

      for (int k = 0; k < arrays.length && ! valueFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 2) + " is not an array");

          return NullValue.NULL;
        }

        valueFound =
          ! ((ArrayValue) arrays[k]).contains(entryValue).isNull();
      }

      if (! valueFound)
        diffArray.put(entry.getKey(), entryValue);
    }

    return diffArray;
  }

  /*
   * Returns an array whose keys are the values of the keyArray passed in,
   * and whose values are all the value passed in.
   * 
   * @param keyArray whose values are used to populate the keys of the new
   * array
   * @param value used as the value of the keys
   * 
   * @return newly filled array
   */
  public static ArrayValue array_fill_keys(Env env,
                                           ArrayValue keyArray,
                                           Value value)
  {
    ArrayValue array = new ArrayValueImpl();
    
    Iterator<Value> iter = keyArray.getValueIterator(env);
    
    while (iter.hasNext()) {
      array.put(iter.next(), value.copy());
    }
    
    return array;
  }

  /**
   * Returns an array with everything that is in array and not in the other
   * arrays, keys also used
   *
   * @param array the primary array
   * @param arrays the vector of arrays to check the primary array's values
   * against
   * @return an array with all of the values that are in the primary array but
   *         not in the other arrays
   */
  public static Value array_diff_assoc(Env env,
                                       ArrayValue array,
                                       Value []arrays)
  {
    if (array == null)
      return NullValue.NULL;

    if (arrays.length < 1) {
      env.warning("Wrong parameter count for array_diff()");

      return NullValue.NULL;
    }

    ArrayValue diffArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      boolean valueFound = false;

      Value entryValue = entry.getValue();

      Value entryKey = entry.getKey();

      for (int k = 0; k < arrays.length && ! valueFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 2) + " is not an array");

          return NullValue.NULL;
        }

        valueFound =
          ((ArrayValue) arrays[k]).contains(entryValue).eq(entryKey);
      }

      if (! valueFound)
        diffArray.put(entryKey, entryValue);
    }

    return diffArray;
  }

  /**
   * Returns an array with everything that is in array and not in the other
   * arrays, keys used for comparison
   *
   * @param array the primary array
   * @param arrays the vector of arrays to check the primary array's values
   * against
   * @return an array with all of the values that are in the primary array but
   *         not in the other arrays
   */
  public static Value array_diff_key(Env env,
                                     ArrayValue array,
                                     Value []arrays)
  {
    if (array == null)
      return NullValue.NULL;

    if (arrays.length < 1) {
      env.warning("Wrong parameter count for array_diff()");

      return NullValue.NULL;
    }

    ArrayValue diffArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      boolean keyFound = false;

      Value entryKey = entry.getKey();

      for (int k = 0; k < arrays.length && ! keyFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 2) + " is not an array");

          return NullValue.NULL;
        }

        keyFound = ((ArrayValue) arrays[k]).containsKey(entryKey) != null;
      }

      if (! keyFound)
        diffArray.put(entryKey, entry.getValue());
    }

    return diffArray;
  }

  /**
   * Returns an array with everything that is in array and not in the other
   * arrays, keys used for comparison aswell
   *
   * @param array the primary array
   * @param arrays the vector of arrays to check the primary array's values
   * against.  The last element is the callback function.
   * @return an array with all of the values that are in the primary array but
   *         not in the other arrays
   */
  public static Value array_diff_uassoc(Env env,
                                        ArrayValue array,
                                        Value []arrays)
  {
    if (array == null)
      return NullValue.NULL;

    if (arrays.length < 2) {
      env.warning("Wrong parameter count for array_diff()");

      return NullValue.NULL;
    }

    AbstractFunction func =
      env.findFunction(arrays[arrays.length - 1].toString().intern());

    if (func == null) {
      env.warning("Invalid comparison function");

      return NullValue.NULL;
    }

    ArrayValue diffArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      boolean ValueFound = false;

      Value entryValue = entry.getValue();

      Value entryKey = entry.getKey();

      for (int k = 0; k < arrays.length - 1 && ! ValueFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 2) + " is not an array");

          return NullValue.NULL;
        }

        Value searchKey = ((ArrayValue) arrays[k]).contains(entryValue);

        if (! searchKey.isNull())
          ValueFound = ((int) func.call(env, searchKey, entryKey).toLong())
              == 0;
      }

      if (! ValueFound)
        diffArray.put(entryKey, entryValue);
    }

    return diffArray;
  }

  /**
   * Returns an array with everything that is in array and not in the other
   * arrays, keys used for comparison only
   *
   * @param array the primary array
   * @param arrays the vector of arrays to check the primary array's values
   * against.  The last element is the callback function.
   * @return an array with all of the values that are in the primary array but
   *         not in the other arrays
   */
  public static Value array_diff_ukey(Env env,
                                      ArrayValue array,
                                      Value []arrays)
  {
    if (array == null)
      return NullValue.NULL;

    if (arrays.length < 2) {
      env.warning("Wrong parameter count for array_diff()");

      return NullValue.NULL;
    }

    AbstractFunction func =
      env.findFunction(arrays[arrays.length - 1].toString().intern());

    if (func == null) {
      env.warning("Invalid comparison function");

      return NullValue.NULL;
    }

    ArrayValue diffArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      boolean keyFound = false;

      Value entryKey = entry.getKey();

      for (int k = 0; k < arrays.length - 1 && ! keyFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 2) + " is not an array");

          return NullValue.NULL;
        }

        Iterator<Value> keyItr = ((ArrayValue) arrays[k]).keySet().iterator();

        keyFound = false;

        while (keyItr.hasNext() && ! keyFound) {
          Value currentKey = keyItr.next();

          keyFound = ((int) func.call(env, entryKey, currentKey).toLong()) == 0;
        }
      }

      if (! keyFound)
        diffArray.put(entryKey, entry.getValue());
    }

    return diffArray;
  }

  /**
   * Returns an array with everything that is in array and also in the other
   * arrays
   *
   * @param array the primary array
   * @param arrays the vector of arrays to check the primary array's values
   * against.  The last element is the callback function.
   * @return an array with all of the values that are in the primary array and
   *         in the other arrays
   */
  public static Value array_intersect(Env env,
                                      ArrayValue array,
                                      Value []arrays)
  {
    if (array == null)
      return NullValue.NULL;

    if (arrays.length < 1) {
      env.warning("Wrong parameter count for array_diff()");

      return NullValue.NULL;
    }

    ArrayValue interArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      boolean valueFound = false;

      Value entryValue = entry.getValue();

      for (int k = 0; k < arrays.length; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 2) + " is not an array");

          return NullValue.NULL;
        }

        if (k > 0 && ! valueFound)
          break;

        valueFound =
          ! ((ArrayValue) arrays[k]).contains(entryValue).isNull();
      }

      if (valueFound)
        interArray.put(entry.getKey(), entryValue);
    }

    return interArray;
  }

  /**
   * Returns an array with everything that is in array and also in the other
   * arrays, keys are also used in the comparison
   *
   * @param array the primary array
   * @param arrays the vector of arrays to check the primary array's values
   * against.  The last element is the callback function.
   * @return an array with all of the values that are in the primary array and
   *         in the other arrays
   */
  public static Value array_intersect_assoc(Env env,
                                            ArrayValue array,
                                            Value []arrays)
  {
    if (array == null)
      return NullValue.NULL;

    if (arrays.length < 1) {
      env.warning("Wrong parameter count for array_diff()");

      return NullValue.NULL;
    }

    ArrayValue interArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      boolean valueFound = false;

      Value entryKey = entry.getKey();

      Value entryValue = entry.getValue();

      for (int k = 0; k < arrays.length; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 2) + " is not an array");

          return NullValue.NULL;
        }

        if (k > 0 && ! valueFound)
          break;

        Value searchValue = ((ArrayValue) arrays[k]).containsKey(entryKey);

        if (searchValue != null)
          valueFound = searchValue.eq(entryValue);
        else
          valueFound = false;
      }

      if (valueFound)
        interArray.put(entryKey, entryValue);
    }

    return interArray;
  }

  /**
   * Returns an array with everything that is in array and also in the other
   * arrays, keys are only used in the comparison
   *
   * @param array the primary array
   * @param arrays the vector of arrays to check the primary array's values
   * against.  The last element is the callback function.
   * @return an array with all of the values that are in the primary array and
   *         in the other arrays
   */
  public static Value array_intersect_key(Env env,
                                          ArrayValue array,
                                          Value []arrays)
  {
    if (array == null)
      return NullValue.NULL;

    if (arrays.length < 1) {
      env.warning("Wrong parameter count for array_diff()");

      return NullValue.NULL;
    }

    ArrayValue interArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      boolean keyFound = false;

      Value entryKey = entry.getKey();

      for (int k = 0; k < arrays.length; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 2) + " is not an array");

          return NullValue.NULL;
        }

        if (k > 0 && ! keyFound)
          break;

        keyFound = ((ArrayValue) arrays[k]).containsKey(entryKey) != null;
      }

      if (keyFound)
        interArray.put(entryKey, entry.getValue());
    }

    return interArray;
  }

  /**
   * Returns an array with everything that is in array and also in the other
   * arrays, keys are also used in the comparison.  Uses a callback function for
   * evalutation the keys.
   *
   * @param array the primary array
   * @param arrays the vector of arrays to check the primary array's values
   * against.  The last element is the callback function.
   * @return an array with all of the values that are in the primary array and
   *         in the other arrays
   */
  public static Value array_intersect_uassoc(Env env,
                                             ArrayValue array,
                                             Value []arrays)
  {
    if (array == null)
      return NullValue.NULL;

    if (arrays.length < 2) {
      env.warning("Wrong parameter count for array_diff()");

      return NullValue.NULL;
    }

    AbstractFunction func =
      env.findFunction(arrays[arrays.length - 1].toString().intern());

    if (func == null) {
      env.warning("Invalid comparison function");

      return NullValue.NULL;
    }

    ArrayValue interArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      boolean valueFound = false;

      Value entryKey = entry.getKey();

      Value entryValue = entry.getValue();

      for (int k = 0; k < arrays.length - 1; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 2) + " is not an array");

          return NullValue.NULL;
        }

        if (k > 0 && ! valueFound)
          break;

        Value searchValue = ((ArrayValue) arrays[k]).containsKey(entryKey);

        if (searchValue != null)
          valueFound = func.call(env, searchValue, entryValue).toLong() == 0;
        else
          valueFound = false;
      }

      if (valueFound)
        interArray.put(entryKey, entryValue);
    }

    return interArray;
  }

  /**
   * Returns an array with everything that is in array and also in the other
   * arrays, keys are only used in the comparison.  Uses a callback function for
   * evalutation the keys.
   *
   * @param array the primary array
   * @param arrays the vector of arrays to check the primary array's values
   * against.  The last element is the callback function.
   * @return an array with all of the values that are in the primary array and
   *         in the other arrays
   */
  public static Value array_intersect_ukey(Env env,
                                           ArrayValue array,
                                           Value []arrays)
  {
    if (array == null)
      return NullValue.NULL;

    if (arrays.length < 2) {
      env.warning("Wrong parameter count for array_diff()");

      return NullValue.NULL;
    }

    AbstractFunction func =
      env.findFunction(arrays[arrays.length - 1].toString().intern());

    if (func == null) {
      env.warning("Invalid comparison function");

      return NullValue.NULL;
    }

    ArrayValue interArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      boolean keyFound = false;

      Value entryKey = entry.getKey();

      for (int k = 0; k < arrays.length - 1; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 2) + " is not an array");

          return NullValue.NULL;
        }

        if (k > 0 && ! keyFound)
          break;

        Iterator<Value> keyItr = ((ArrayValue) arrays[k]).keySet().iterator();

        keyFound = false;

        while (keyItr.hasNext() && ! keyFound) {
          Value currentKey = keyItr.next();

          keyFound = ((int) func.call(env, entryKey, currentKey).toLong()) == 0;
        }

      }

      if (keyFound)
        interArray.put(entryKey, entry.getValue());
    }

    return interArray;
  }

  /**
   * Maps the given function with the array arguments.
   * XXX: callback modifying array?
   *
   * @param fun the function name
   * @param args the vector of array arguments
   * @return an array with all of the mapped values
   */
  public static Value array_map(Env env, Callable fun,
                                ArrayValue arg, Value []args)
  {
    // XXX: drupal
    if (arg == null)
      return NullValue.NULL;
    
    // quercus/1730
    Iterator<Map.Entry<Value, Value>> argIter = arg.entrySet().iterator();

    Iterator []iters = new Iterator[args.length];
    for (int i = 0; i < args.length; i++) {
      if (! (args[i] instanceof ArrayValue))
        throw env.createErrorException(L.l("expected array"));

      ArrayValue argArray = (ArrayValue) args[i];

      iters[i] = argArray.values().iterator();
    }

    ArrayValue resultArray = new ArrayValueImpl();

    Value []param = new Value[args.length + 1];
    while (argIter.hasNext()) {
      Map.Entry<Value, Value> entry = argIter.next();

      param[0] = entry.getValue();

      for (int i = 0; i < iters.length; i++) {
        param[i + 1] = (Value) iters[i].next();

        if (param[i + 1] == null)
          param[i + 1] = NullValue.NULL;
      }

      resultArray.put(entry.getKey(), fun.call(env, param));
    }

    return resultArray;
  }

  /**
   * Maps the given function with the array arguments.
   *
   * @param args the vector of array arguments
   * @return an array with all of the mapped values
   */
  public static Value array_merge(Env env, Value []args)
  {
    // php/1731

    ArrayValue result = new ArrayValueImpl();

    for (Value arg : args) {
      if (arg.isNull())
        return NullValue.NULL;

      Value argValue = arg.toValue();
      
      if (! argValue.isArray())
        continue;

      ArrayValue array = argValue.toArrayValue(env);
      
      Iterator<Map.Entry<Value,Value>> iter = array.getIterator(env);
      
      while (iter.hasNext()) {
        Map.Entry<Value,Value> entry = iter.next();
        
        Value key = entry.getKey();
        Value value;
        
        if (entry instanceof ArrayValue.Entry) {
          // php/173z, php/1747
          value = ((ArrayValue.Entry) entry).getRawValue();
        }
        else
          value = entry.getValue();
        
        if (! (value instanceof Var))
          value = value.copy();

        // php/1745
        if (key.isNumberConvertible())
          result.put(value);
        else
          result.append(key, value);
      }
    }

    return result;
  }

  /**
   * Maps the given function with the array arguments.
   *
   * @param args the vector of array arguments
   * @return an array with all of the mapped values
   */
  public static Value array_merge_recursive(Env env, Value []args)
  {
    // quercus/173a

    ArrayValue result = new ArrayValueImpl();

    for (Value arg : args) {
      if (! (arg.toValue() instanceof ArrayValue))
        continue;

      arrayMergeRecursiveImpl(env, result, (ArrayValue) arg.toValue());
    }

    return result;
  }

  private static void arrayMergeRecursiveImpl(Env env,
                                              ArrayValue result,
                                              ArrayValue array)
  {
    Iterator<Map.Entry<Value,Value>> iter = array.getIterator(env);
    
    while (iter.hasNext()) {
      Map.Entry<Value,Value> entry = iter.next();
      
      Value key = entry.getKey();
      Value value;
      
      if (entry instanceof ArrayValue.Entry) {
        // php/1744, php/1746
        value = ((ArrayValue.Entry) entry).getRawValue();
      }
      else
        value = entry.getValue();
      
      if (! (value instanceof Var))
        value = value.copy();

      if (key.isNumberConvertible())
        result.put(value);
      else {
        Value oldValue = result.get(key).toValue();

        if (oldValue != null && oldValue.isset()) {
          if (oldValue.isArray() && value.isArray()) {
            arrayMergeRecursiveImpl(env,
                                    oldValue.toArrayValue(env),
                                    value.toArrayValue(env));
          }
          else if (oldValue.isArray()) {
            oldValue.put(value);
          }
          else if (value.isArray()) {
            // XXX: s/b insert?
            value.put(oldValue);
          }
          else {
            ArrayValue newArray = new ArrayValueImpl();

            newArray.put(oldValue);
            newArray.put(value);

            result.put(key, newArray);
          }
        }
        else
          result.put(key, value);
      }
    }
  }

  /**
   * Sort the arrays like rows in a database.
   * @param arrays  arrays to sort
   *
   * @return true on success, and false on failure
   */
  public static boolean array_multisort(Env env, Value[] arrays)
  {
    boolean isNewKeys = true;
    
    if (arrays.length == 0 || ! arrays[0].isArray()) {
      env.warning("the first argument must be an array");
      
      return false;
    }
    
    Value primary = arrays[0];
    
    Iterator<Value> keyIter = primary.getKeyIterator(env);
    
    while (keyIter.hasNext()) {
      if (! (keyIter.next() instanceof LongValue)) {
        isNewKeys = false;
        break;
      }
    }

    Value []rows = primary.getKeyArray(env);
    
    int maxsize = 0;
    for (int i = 0; i < arrays.length; i++)
      if (arrays[i] instanceof ArrayValue)
        maxsize = Math.max(maxsize, arrays[i].getSize());

    // create the identity permutation [1..n]
    LongValue []p = new LongValue[maxsize];
    for (int i = 0; i < rows.length; i++) {
      p[i] = LongValue.create(i);
    }

    java.util.Arrays.sort(p, new MultiSortComparator(env, rows, arrays));

    // apply the permuation
    for (int i = 0; i < arrays.length; i++) {
      if (arrays[i].isArray()) {
        permute(env, (ArrayValue)arrays[i], p, isNewKeys);
      }
    }

    return true;
  }

  /*
   *  Apply a permutation to an array; on return, each element of
   *  array[i] holds the value that was in array[permutation[i]]
   *  before the call.
   */
  private static void permute(Env env,
                              ArrayValue array,
                              Value[] permutation,
                              boolean isNewKeys)
  {
    Value[] keys = array.getKeyArray(env);
    Value[] values = array.getValueArray(env);

    array.clear();
    
    if (isNewKeys) {
      for (int i = 0; i < permutation.length; i++) {
        int p = permutation[i].toInt();

        Value value = values[p];
        array.put(LongValue.create(i), value.toValue().copy());
      }
    }
    else {
      for (int i = 0; i < permutation.length; i++) {
        int p = permutation[i].toInt();
        
        Value key = keys[p];
        Value value = values[p];
        array.put(key, value.toValue().copy());
      }
    }
  }


  // XXX: Performance Test asort
  /**
   * Sorts the array.
   */
  /*public Value asort(Env env,
                     Value value,
                     @Optional int mode)
  {
    if (! (value instanceof ArrayValue)) {
      env.warning(L.l("asort requires array at '{0}'", value));
      return BooleanValue.FALSE;
    }

    ArrayValue array = (ArrayValue) value;

    array.asort();

    return BooleanValue.TRUE;
  }*/

  // XXX: Performance Test ksort
  /**
   * Sorts the array.
   */
  /*public Value ksort(Env env,
                     Value value,
                     @Optional int mode)
  {
    if (! (value instanceof ArrayValue)) {
      env.warning(L.l("asort requires array at '{0}'", value));
      return BooleanValue.FALSE;
    }

    ArrayValue array = (ArrayValue) value;

    array.ksort();

    return BooleanValue.TRUE;
  }*/

  /**
   * Creates an array with all the values of the first array that are not
   * present in the other arrays, using a provided callback function to
   * determine equivalence.
   *
   * @param arrays first array is checked against the rest.  Last element is the
   * callback function.
   * @return an array with all the values of the first array that are not in the
   *         rest
   */
  public static Value array_udiff(Env env, Value[] arrays)
  {
    if (arrays.length < 3) {
      env.warning("Wrong paremeter count for array_udiff()");

      return NullValue.NULL;
    }

    if (! (arrays[0] instanceof ArrayValue)) {
      env.warning("Argument #1 is not an array");

      return NullValue.NULL;
    }

    ArrayValue array = (ArrayValue) arrays[0];

    Value callbackValue = arrays[arrays.length - 1];

    Callable cmp = callbackValue.toCallable(env);
    
    if (! cmp.isValid(env))
      return NullValue.NULL;

    ArrayValue diffArray = new ArrayValueImpl();

    boolean isFound = false;

    for (Value entryKey : array.keySet()) {
      Value entryValue = array.get(entryKey);

      for (int k = 1; k < arrays.length - 1 && ! isFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 1) + " is not an array");

          return NullValue.NULL;
        }

        ArrayValue checkArray = (ArrayValue) arrays[k];

        for (Map.Entry<Value, Value> entry : checkArray.entrySet()) {
          try {
            isFound = cmp.call(env, entryValue, entry.getValue()).toLong() == 0;
          }
          catch (Exception t) {
            log.log(Level.WARNING, t.toString(), t);

            env.warning("An error occurred while invoking the filter callback");

            return NullValue.NULL;
          }

          if (isFound)
            break;
        }
      }

      if (! isFound)
        diffArray.put(entryKey, entryValue);

      isFound = false;
    }

    return diffArray;
  }

  /**
   * Creates an array with all the values of the first array that are not
   * present in the other arrays, using a provided callback function to
   * determine equivalence.  Also checks the key for equality using an internal
   * comparison function.
   *
   * @param arrays first array is checked against the rest.  Last element is the
   * callback function.
   * @return an array with all the values of the first array that are not in the
   *         rest
   */
  public static Value array_udiff_assoc(Env env, Value[] arrays)
  {
    if (arrays.length < 3) {
      env.warning("Wrong paremeter count for array_udiff_assoc()");

      return NullValue.NULL;
    }

    if (! (arrays[0] instanceof ArrayValue)) {
      env.warning("Argument #1 is not an array");

      return NullValue.NULL;
    }

    ArrayValue array = (ArrayValue) arrays[0];

    Value callbackValue = arrays[arrays.length - 1];

    Callable cmp = callbackValue.toCallable(env);
    
    if (! cmp.isValid(env))
      return NullValue.NULL;

    ArrayValue diffArray = new ArrayValueImpl();

    boolean isFound = false;

    for (Value entryKey : array.keySet()) {
      Value entryValue = array.get(entryKey);

      for (int k = 1; k < arrays.length - 1 && ! isFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 1) + " is not an array");

          return NullValue.NULL;
        }

        ArrayValue checkArray = (ArrayValue) arrays[k];

        for (Map.Entry<Value, Value> entry : checkArray.entrySet()) {
          try {
            boolean keyFound = entryKey.eql(entry.getKey());

            boolean valueFound = false;

            if (keyFound)
              valueFound = cmp.call(env, entryValue, entry.getValue())
                .toLong() == 0;

            isFound = keyFound && valueFound;
          }
          catch (Exception t) {
            log.log(Level.WARNING, t.toString(), t);

            env.warning("An error occurred while invoking the filter callback");

            return NullValue.NULL;
          }

          if (isFound)
            break;
        }
      }

      if (! isFound)
        diffArray.put(entryKey, entryValue);

      isFound = false;
    }

    return diffArray;
  }

  /**
   * Creates an array with all the values of the first array that are not
   * present in the other arrays, using a provided callback function to
   * determine equivalence.  Also checks keys using a provided callback
   * function.
   *
   * @param arrays first array is checked against the rest.  Last two elementare
   * the callback functions.
   * @return an array with all the values of the first array that are not in the
   *         rest
   */
  public static Value array_udiff_uassoc(Env env, Value[] arrays)
  {
    if (arrays.length < 4) {
      env.warning("Wrong paremeter count for array_udiff_uassoc()");

      return NullValue.NULL;
    }

    if (! (arrays[0] instanceof ArrayValue)) {
      env.warning("Argument #1 is not an array");

      return NullValue.NULL;
    }

    ArrayValue array = (ArrayValue) arrays[0];

    Value callbackValue = arrays[arrays.length - 2];

    Callable cmpValue = callbackValue.toCallable(env);

    if (! cmpValue.isValid(env))
      return NullValue.NULL;

    Value callbackKey = arrays[arrays.length - 1];

    Callable cmpKey = callbackKey.toCallable(env);

    if (! cmpKey.isValid(env))
      return NullValue.NULL;

    ArrayValue diffArray = new ArrayValueImpl();

    boolean isFound = false;

    for (Value entryKey : array.keySet()) {
      Value entryValue = array.get(entryKey);

      for (int k = 1; k < arrays.length - 2 && ! isFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 1) + " is not an array");

          return NullValue.NULL;
        }

        ArrayValue checkArray = (ArrayValue) arrays[k];

        for (Map.Entry<Value, Value> entry : checkArray.entrySet()) {
          try {
            boolean valueFound =
              cmpValue.call(env, entryValue, entry.getValue()).toLong() == 0;

            boolean keyFound = false;

            if (valueFound)
              keyFound = cmpKey.call(env, entryKey, entry.getKey()).toLong()
                  == 0;

            isFound = valueFound && keyFound;
          }
          catch (Throwable t) {
            log.log(Level.WARNING, t.toString(), t);

            env.warning("An error occurred while invoking the filter callback");

            return NullValue.NULL;
          }

          if (isFound)
            break;
        }
      }

      if (! isFound)
        diffArray.put(entryKey, entryValue);

      isFound = false;
    }

    return diffArray;
  }

  /**
   * Creates an array with all the values of the first array that are present in
   * the other arrays, using a provided callback function to determine
   * equivalence.
   * XXX: callback modifying arrays?
   *
   * @param arrays first array is checked against the rest.  Last element is the
   * callback function.
   * @return an array with all the values of the first array that are in the
   *         rest
   */
  public static Value array_uintersect(Env env, Value[] arrays)
  {
    if (arrays.length < 3) {
      env.warning("Wrong paremeter count for array_uintersect()");

      return NullValue.NULL;
    }

    if (! (arrays[0] instanceof ArrayValue)) {
      env.warning("Argument #1 is not an array");

      return NullValue.NULL;
    }

    ArrayValue array = (ArrayValue) arrays[0];

    Value callbackValue = arrays[arrays.length - 1];

    Callable cmp = callbackValue.toCallable(env);
    
    if (! cmp.isValid(env))
      return NullValue.NULL;

    ArrayValue interArray = new ArrayValueImpl();

    boolean isFound = true;

    for (Value entryKey : array.keySet()) {
      Value entryValue = array.get(entryKey);

      for (int k = 1; k < arrays.length - 1 && isFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 1) + " is not an array");

          return NullValue.NULL;
        }

        ArrayValue checkArray = (ArrayValue) arrays[k];

        for (Map.Entry<Value, Value> entry : checkArray.entrySet()) {
          try {
            isFound = cmp.call(env, entryValue, entry.getValue()).toLong() == 0;
          }
          catch (Throwable t) {
            log.log(Level.WARNING, t.toString(), t);

            env.warning("An error occurred while invoking the filter callback");

            return NullValue.NULL;
          }

          if (isFound)
            break;
        }
      }

      if (isFound)
        interArray.put(entryKey, entryValue);
    }

    return interArray;
  }

  /**
   * Creates an array with all the values of the first array that are present in
   * the other arrays, using a provided callback function to determine
   * equivalence. Also checks the keys for equivalence using an internal
   * comparison.
   * XXX: callback modifying arrays?
   *
   * @param arrays first array is checked against the rest.  Last element is the
   * callback function.
   * @return an array with all the values of the first array that are in the
   *         rest
   */
  public static Value array_uintersect_assoc(Env env, Value[] arrays)
  {
    if (arrays.length < 3) {
      env.warning("Wrong paremeter count for array_uintersect_assoc()");

      return NullValue.NULL;
    }

    if (! (arrays[0] instanceof ArrayValue)) {
      env.warning("Argument #1 is not an array");

      return NullValue.NULL;
    }

    ArrayValue array = (ArrayValue) arrays[0];

    Value callbackValue = arrays[arrays.length - 1];

    Callable cmp = callbackValue.toCallable(env);
    
    if (! cmp.isValid(env))
      return NullValue.NULL;

    ArrayValue interArray = new ArrayValueImpl();

    boolean isFound = true;

    for (Value entryKey : array.keySet()) {
      Value entryValue = array.get(entryKey);

      for (int k = 1; k < arrays.length - 1 && isFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 1) + " is not an array");

          return NullValue.NULL;
        }

        ArrayValue checkArray = (ArrayValue) arrays[k];

        for (Map.Entry<Value, Value> entry : checkArray.entrySet()) {
          try {
            boolean keyFound = entryKey.eql(entry.getKey());

            boolean valueFound = false;

            if (keyFound)
              valueFound = cmp.call(env, entryValue, entry.getValue())
                .toLong() == 0;

            isFound = keyFound && valueFound;
          }
          catch (Throwable t) {
            log.log(Level.WARNING, t.toString(), t);

            env.warning("An error occurred while invoking the filter callback");

            return NullValue.NULL;
          }

          if (isFound)
            break;
        }
      }

      if (isFound)
        interArray.put(entryKey, entryValue);
    }

    return interArray;
  }

  /**
   * Creates an array with all the values of the first array that are present in
   * the other arrays, using a provided callback function to determine
   * equivalence. Also checks the keys for equivalence using a pass callback
   * function
   * XXX: callback modifying arrays?
   *
   * @param arrays first array is checked against the rest.  Last two elements
   * are the callback functions.
   * @return an array with all the values of the first array that are in the
   *         rest
   */
  public static Value array_uintersect_uassoc(Env env, Value[] arrays)
  {
    if (arrays.length < 4) {
      env.warning("Wrong paremeter count for array_uintersect_uassoc()");

      return NullValue.NULL;
    }

    if (! (arrays[0] instanceof ArrayValue)) {
      env.warning("Argument #1 is not an array");

      return NullValue.NULL;
    }

    ArrayValue array = (ArrayValue) arrays[0];

    Value callbackValue = arrays[arrays.length - 2];

    Callable cmpValue = callbackValue.toCallable(env);
    
    if (! cmpValue.isValid(env))
      return NullValue.NULL;

    Value callbackKey = arrays[arrays.length - 1];

    Callable cmpKey = callbackKey.toCallable(env);
    
    if (! cmpKey.isValid(env))
      return NullValue.NULL;

    ArrayValue interArray = new ArrayValueImpl();

    boolean isFound = true;

    for (Value entryKey : array.keySet()) {
      Value entryValue = array.get(entryKey);

      for (int k = 1; k < arrays.length - 2 && isFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 1) + " is not an array");

          return NullValue.NULL;
        }

        ArrayValue checkArray = (ArrayValue) arrays[k];

        for (Map.Entry<Value, Value> entry : checkArray.entrySet()) {
          try {
            boolean valueFound =
              cmpValue.call(env, entryValue, entry.getValue()).toLong() == 0;

            boolean keyFound = false;

            if (valueFound)
              keyFound = cmpKey.call(env, entryKey, entry.getKey()).toLong()
                  == 0;

            isFound = valueFound && keyFound;
          }
          catch (Throwable t) {
            log.log(Level.WARNING, t.toString(), t);

            env.warning("An error occurred while invoking the filter callback");

            return NullValue.NULL;
          }

          if (isFound)
            break;
        }
      }

      if (isFound)
        interArray.put(entryKey, entryValue);
    }

    return interArray;
  }

  /**
   * Creates an array of corresponding values to variables in the symbol name.
   * The passed parameters are the names of the variables to be added to the
   * array.
   *
   * @param variables contains the names of variables to add to the array
   * @return an array with the values of variables that match those passed
   */
  @UsesSymbolTable
  public static ArrayValue compact(Env env, Value[] variables)
  {
    ArrayValue compactArray = new ArrayValueImpl();

    for (Value variableName : variables) {
      if (variableName.isString()) {
        Var var = env.getRef(variableName.toStringValue(), false);

        if (var != null)
          compactArray.put(variableName, var.toValue());
      }
      else if (variableName instanceof ArrayValue) {
        ArrayValue array = (ArrayValue) variableName;

        ArrayValue innerArray = compact(env, array.valuesToArray());

        compactArray.putAll(innerArray);
      }
    }

    return compactArray;
  }

  /**
   * Returns the size of the array.
   */
  public static long sizeof(Env env,
                            @ReadOnly Value value,
                            @Optional int countMethod)
  {
    return count(env, value, countMethod);
  }

  private static class CompareString
    implements
    Comparator<Map.Entry<Value, Value>>
  {
    private AbstractGet _getter;

    private int _order;

    CompareString(AbstractGet getter, int order)
    {
      _getter = getter;
      _order = order;
    }

    public int compare(Map.Entry<Value, Value> aEntry,
                       Map.Entry<Value, Value> bEntry)
    {
      String aElement = _getter.get(aEntry).toString();
      String bElement = _getter.get(bEntry).toString();

      return aElement.compareTo(bElement) * _order;
    }
  }

  private static class CompareNumeric
    implements
    Comparator<Map.Entry<Value, Value>>
  {
    private AbstractGet _getter;

    private int _order;

    CompareNumeric(AbstractGet getter, int order)
    {
      _getter = getter;
      _order = order;
    }

    public int compare(Map.Entry<Value, Value> aEntry,
                       Map.Entry<Value, Value> bEntry)
    {
      try {
        // php/1756
        double aElement = _getter.get(aEntry).toDouble();
        double bElement = _getter.get(bEntry).toDouble();

        if (aElement == bElement)
          return 0;
        else if (aElement < bElement)
          return -1 * _order;
        else
          return _order;
      }
      catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class CompareLocale
    implements
    Comparator<Map.Entry<Value, Value>>
  {
    private AbstractGet _getter;

    private int _order;

    private Collator _collator;

    CompareLocale(AbstractGet getter, int order, Collator collator)
    {
      _getter = getter;
      _order = order;
      _collator = collator;
    }

    public int compare(Map.Entry<Value, Value> aEntry,
                       Map.Entry<Value, Value> bEntry)
    {
      String aElement = _getter.get(aEntry).toString();
      String bElement = _getter.get(bEntry).toString();

      return _collator.compare(aElement, bElement) * _order;
    }
  }

  private static class CompareNormal
    implements Comparator<Map.Entry<Value, Value>>
  {
    private AbstractGet _getter;

    private int _order;

    CompareNormal(AbstractGet getter, int order)
    {
      _getter = getter;
      _order = order;
    }

    public int compare(Map.Entry<Value, Value> aEntry,
                       Map.Entry<Value, Value> bEntry)
    {
      if (_getter instanceof GetKey) {
        KeyComparator k = KeyComparator.CMP;

        return k.compare(aEntry, bEntry) * _order;
      }

      ValueComparator c = ValueComparator.CMP;

      return c.compare(aEntry, bEntry) * _order;
    }
  }

  private static class CompareNatural
    implements
    Comparator<Map.Entry<Value, Value>>
  {
    private AbstractGet _getter;

    private int _order;

    private boolean _isCaseSensitive;

    CompareNatural(AbstractGet getter, int order, boolean isCaseSensitive)
    {
      _getter = getter;
      _order = order;
      _isCaseSensitive = isCaseSensitive;
    }

    public int compare(Map.Entry<Value, Value> aEntry,
                       Map.Entry<Value, Value> bEntry)
    {
      try {
        String aElement = _getter.get(aEntry).toString();
        String bElement = _getter.get(bEntry).toString();

        if (! _isCaseSensitive) {
          aElement = aElement.toLowerCase();
          bElement = bElement.toLowerCase();
        }

        StringParser aParser = new StringParser(aElement);
        StringParser bParser = new StringParser(bElement);

        while (aParser.hasNext() && bParser.hasNext()) {
          String aPart = aParser.next();
          String bPart = bParser.next();

          int comparison;

          try {
            Long aLong = Long.valueOf(aPart);
            Long bLong = Long.valueOf(bPart);

            comparison = aLong.compareTo(bLong);
          }
          catch (NumberFormatException e) {
            comparison = aPart.compareTo(bPart);
          }

          if (comparison < 0)
            return -1;
          else if (comparison > 0)
            return 1;
        }

        if (bParser.hasNext())
          return 1;
        else if (aParser.hasNext())
          return -1;
        else
          return 0;

      }
      catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class CompareCallBack
    implements Comparator<Map.Entry<Value, Value>>
  {
    private AbstractGet _getter;

    private int _order;

    private Callable _func;

    private Env _env;

    CompareCallBack(AbstractGet getter, int order, Callable func,
                    Env env)
    {
      _getter = getter;
      _order = order;
      _func = func;
      _env = env;
    }

    public int compare(Map.Entry<Value, Value> aEntry,
                       Map.Entry<Value, Value> bEntry)
    {
      try {
        Value aElement = _getter.get(aEntry);
        Value bElement = _getter.get(bEntry);

        return (int) _func.call(_env, aElement, bElement).toLong();
      }
      catch (Exception e) {
        throw new QuercusModuleException(e);
      }
    }
  }

  /*
   *  A comparator used to sort a permutation based on a set of
   *  column-arrays.
   */
  private static class MultiSortComparator
    implements Comparator<LongValue>
  {

    private final Env _env;
    private final Value []_rows;
    private final Value []_arrays;

    public MultiSortComparator(Env env, Value[] rows, Value[] arrays)
    {
      this._env = env;
      this._rows = rows;
      this._arrays = arrays;
    }

    /*
     *  Examine the "row" consisting of arrays[x][index1] and
     *  arrays[x][index2] for all indices "x"; the permutation will be
     *  sorted according to this comparison.
     */
    public int compare(LongValue index1, LongValue index2)
    {
      for (int i = 0; i < _arrays.length; i++) {
        // reset direction/mode for each array (per the php.net spec)
        int direction = SORT_ASC;
        int mode      = SORT_REGULAR;
        ArrayValue av = (ArrayValue) _arrays[i];

        // process all flags appearing *after* an array but before the next one
        while ((i + 1) < _arrays.length
            && _arrays[i + 1] instanceof LongValue) {
          i++;

          int flag = _arrays[i].toInt();

          switch (flag) {
            case SORT_ASC:
              direction = SORT_ASC;
              break;

            case SORT_DESC:
              direction = SORT_DESC;
              break;

            case SORT_REGULAR:
              mode = SORT_REGULAR;
              break;

            case SORT_STRING:
              mode = SORT_STRING;
              break;

            case SORT_NUMERIC:
              mode = SORT_NUMERIC;
              break;

            default:
              _env.warning("Unknown sort flag: " + _arrays[i]);
          }
        }

        int cmp;

        Value lValue = av.get(_rows[index1.toInt()]);
        Value rValue = av.get(_rows[index2.toInt()]);

        if (mode == SORT_STRING) {
          // php/173g
          cmp = lValue.toString().compareTo(rValue.toString());
        }
        else if (mode == SORT_NUMERIC) {
          // php/173f
          cmp = NumberValue.compareNum(lValue, rValue);
        }
        else
          cmp = lValue.cmp(rValue);

        if (cmp != 0)
          return direction == SORT_ASC ? cmp : -1 * cmp;
      }

      return 0;
    }
  }

  private static class StringParser {
    private int _current;
    private int _length;

    private String _string;

    private static final int SYMBOL = 1;
    private static final int LETTER = 2;
    private static final int DIGIT = 3;

    StringParser(String string)
    {
      _string = string;
      _length = string.length();
      _current = 0;
    }

    public boolean hasNext()
    {
      return _current < _length;
    }

    public String next()
    {
      int start;
      int type;

      try {
        char character = _string.charAt(_current);

        if (character == '0') {
          _current++;
          return "0";
        }
        else if (Character.isLetter(character))
          type = LETTER;
        else if (Character.isDigit(character))
          type = DIGIT;
        else
          type = SYMBOL;

        for (start = _current; _current < _length; _current++) {
          if (type == LETTER && Character.isLetter(_string.charAt(_current)))
          {
          }
          else if (type == DIGIT && Character.isDigit(_string.charAt(_current)))
          {
          }
          else if (type == SYMBOL
              && !Character.isLetterOrDigit(_string.charAt(_current))) {
          }
          else
            break;
        }

        return _string.substring(start, _current);
      }
      catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
        return null;
      }
    }
  }
}
