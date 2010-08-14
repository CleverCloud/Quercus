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

package com.caucho.quercus.env;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.caucho.util.RandomUtil;

/**
 * Represents a PHP array value.
 */
public class ArrayValueImpl extends ArrayValue
  implements Serializable
{
  private static final Logger log
    = Logger.getLogger(ArrayValueImpl.class.getName());

  private static final StringValue KEY = new ConstStringValue("key");
  private static final StringValue VALUE = new ConstStringValue("value");

  private static final int DEFAULT_SIZE = 16;

  private static final int SORT_REGULAR = 0;
  private static final int SORT_NUMERIC = 1;
  private static final int SORT_STRING = 2;
  private static final int SORT_LOCALE_STRING = 5;

  // save memory on short arrays
  private static final int MIN_HASH = 4;

  private Entry []_entries;
  private int _hashMask;

  private int _size;
  private long _nextAvailableIndex;
  private boolean _isDirty;

  private Entry _head;
  private Entry _tail;

  private ConstArrayValue _constSource;

  public ArrayValueImpl()
  {
    /*
    _entries = new Entry[DEFAULT_SIZE];
    _hashMask = _entries.length - 1;
    */
  }

  public ArrayValueImpl(int size)
  {
    /*
    int capacity = DEFAULT_SIZE;

    while (capacity < 4 * size)
      capacity *= 2;

    _entries = new Entry[capacity];
    _hashMask = _entries.length - 1;
    */
  }

  public ArrayValueImpl(ArrayValue source)
  {
    // this(copy.getSize());

    for (Entry ptr = source.getHead(); ptr != null; ptr = ptr.getNext()) {
      // php/0662 for copy
      Entry entry = createNewEntry(ptr.getKey());

      /*
      if (ptr._var != null)
        entry._var = ptr._var;
      else
        entry._value = ptr._value.copyArrayItem();
      */
      entry.setValue(ptr.getValue().copyArrayItem());
    }
  }

  public ArrayValueImpl(ArrayValueImpl source)
  {
    copyFrom(source);
  }
  
  protected void copyFrom(ArrayValueImpl source)
  {
    if (! source._isDirty)
      source._isDirty = true;

    _isDirty = true;

    _size = source._size;
    _entries = source._entries;
    _hashMask = source._hashMask;

    _head = source._head;
    setCurrent(source.getCurrent());

    _tail = source._tail;
    _nextAvailableIndex = source._nextAvailableIndex;
  }

  public ArrayValueImpl(ConstArrayValue source)
  {
    _constSource = source;

    _isDirty = true;

    _size = source.getSize();
    _entries = source.getEntries();
    _hashMask = source.getHashMask();

    _head = source.getHead();
    setCurrent(source.getCurrent());
    _tail = source.getTail();
    _nextAvailableIndex = source.getNextAvailableIndex();
  }

  public ArrayValueImpl(Env env,
                        IdentityHashMap<Value,Value> map,
                        ArrayValue copy)
  {
    this();

    map.put(copy, this);

    for (Entry ptr = copy.getHead(); ptr != null; ptr = ptr.getNext()) {
      // Value value = ptr._var != null ? ptr._var.toValue() : ptr._value;
      Value value = ptr.toValue();

      append(ptr.getKey(), value.copy(env, map));
    }
  }

  /**
   * Copy for unserialization.
   *
   * XXX: need to update for references
   */
  protected ArrayValueImpl(Env env, ArrayValue copy, CopyRoot root)
  {
    this();

    root.putCopy(copy, this);

    for (Entry ptr = copy.getHead(); ptr != null; ptr = ptr.getNext()) {
      // Value value = ptr._var != null ? ptr._var.toValue() : ptr._value;
      Value value = ptr.toValue();

      append(ptr.getKey(), value.copyTree(env, root));
    }
  }

  public ArrayValueImpl(Value []keys, Value []values)
  {
    this();

    for (int i = 0; i < keys.length; i++) {
      if (keys[i] != null)
        append(keys[i], values[i]);
      else
        put(values[i]);
    }
  }

  public ArrayValueImpl(Value []values)
  {
    this();

    for (int i = 0; i < values.length; i++) {
      put(values[i]);
    }
  }

  public ArrayValueImpl(Env env, ArrayValueComponent[] components)
  {
    for (int i = 0; i < components.length; i++) {
      components[i].init(env);
      components[i].addTo(this);
    }
  }

  public ArrayValueImpl(ArrayValueComponent[] components)
  {
    for (int i = 0; i < components.length; i++) {
      components[i].init();
      components[i].addTo(this);
    }
  }
  
  protected Entry []getEntries()
  {
    return _entries;
  }
  
  protected int getHashMask()
  {
    return _hashMask;
  }
  
  protected long getNextAvailableIndex()
  {
    return _nextAvailableIndex;
  }
  

  private void copyOnWrite()
  {
    if (! _isDirty)
      return;

    _constSource = null;

    _isDirty = false;

    Entry []entries = _entries;

    if (entries != null)
      entries = new Entry[entries.length];
    else
      entries = null;
    
    Entry prev = null;
    for (Entry ptr = _head; ptr != null; ptr = ptr.getNext()) {
      // Entry ptrCopy = new Entry(ptr._key, ptr._value.copyArrayItem());
      Entry ptrCopy = new Entry(ptr);

      if (entries != null) {
        int hash = ptr.getKey().hashCode() & _hashMask;

        Entry head = entries[hash];

        if (head != null) {
          ptrCopy.setNextHash(head);
        }

        entries[hash] = ptrCopy;
      }
      else if (prev != null) {
        prev.setNextHash(ptrCopy);
      }

      if (prev == null) {
        setCurrent(ptrCopy);
      
        _head = ptrCopy;
      }
      else {
        prev.setNext(ptrCopy);
        ptrCopy.setPrev(prev);
      }

      prev = ptrCopy;
    }

    _tail = prev;

    _entries = entries;
  }

  /**
   * Returns the type.
   */
  public String getType()
  {
    return "array";
  }

  /**
   * Converts to a boolean.
   */
  public boolean toBoolean()
  {
    return _size != 0;
  }

  /**
   * Converts to a string.
   * @param env
   */
  public StringValue toString(Env env)
  {
    return env.createString("Array");
  }

  /**
   * Converts to an object.
   */
  public Object toObject()
  {
    return null;
  }

  /**
   * Copy the value.
   */
  public Value copy()
  {
    // php/1704
    reset();

    Value copy = new ArrayValueImpl(this);
    // copy.reset();
    
    return copy;
  }

  /**
   * Copy for return.
   */
  @Override
  public Value copyReturn()
  {
    return new ArrayValueImpl(this);
  }

  /**
   * Copy for serialization
   */
  public Value copy(Env env, IdentityHashMap<Value,Value> map)
  {
    Value oldValue = map.get(this);

    if (oldValue != null)
      return oldValue;

    return new ArrayValueImpl(env, map, this);
  }

  /**
   * Copy for serialization
   */
  @Override
  public Value copyTree(Env env, CopyRoot root)
  {
    // php/420d

    Value copy = root.getCopy(this);

    if (copy != null)
      return copy;
    else
      return new ArrayCopyValueImpl(env, this, root);
  }

  /**
   * Copy for saving a method's arguments.
   */
  public Value copySaveFunArg()
  {
    return new ArrayValueImpl(this);
  }

  /**
   * Convert to an argument value.
   */
  @Override
  public Value toLocalValue()
  {
    // php/1708

    Value copy = new ArrayValueImpl(this);
    copy.reset();

    return copy;
  }

  /**
   * Convert to an argument value.
   */
  @Override
  public Value toLocalRef()
  {
    // php/1708

    Value copy = new ArrayValueImpl(this);
    copy.reset();
    
    return copy;
  }

  /**
   * Convert to an argument declared as a reference
   */
  @Override
  public Value toRefValue()
  {
    return this;
  }

  /**
   * Returns the size.
   */
  public int size()
  {
    return _size;
  }

  /**
   * Returns the size.
   */
  public int getSize()
  {
    return size();
  }

  /**
   * Clears the array
   */
  public void clear()
  {
    if (_isDirty) {
      _isDirty = false;
    }

    _entries = null;

    _size = 0;
    _head = _tail = null;
    setCurrent(null);

    _nextAvailableIndex = 0;
  }

  /**
   * Returns true for an array.
   */
  public boolean isArray()
  {
    return true;
  }

  /**
   * Adds a new value.
   */
  public ArrayValue append(Value key, Value value)
  {
    if (_isDirty) {
      copyOnWrite();
    }

    if (key instanceof UnsetValue) // php/4a4h
      key = createTailKey();

    Entry entry = createEntry(key);

    // php/0434
    // Var oldVar = entry._var;

    entry.set(value);

    return this;
  }

  /**
   * Add to the beginning
   */
  public ArrayValue unshift(Value value)
  {
    if (_isDirty)
      copyOnWrite();

    _size++;

    Entry []entries = _entries;
    if ((entries == null && _size >= MIN_HASH)
        || (entries != null && entries.length <= 2 * _size)) {
      expand();
    }

    Value key = createTailKey();

    Entry entry = new Entry(key, value.toLocalValue());

    addEntry(entry);

    if (_head != null) {
      _head._prev = entry;
      entry.setNext(_head);
      _head = entry;
    }
    else {
      _head = _tail = entry;
    }

    return this;
  }

  /**
   * Replace a section of the array.
   */
  public ArrayValue splice(int start, int end, ArrayValue replace)
  {
    if (_isDirty)
      copyOnWrite();

    int index = 0;

    ArrayValueImpl result = new ArrayValueImpl();

    Entry ptr = _head;
    Entry nextPtr = null;
    for (; ptr != null; ptr = nextPtr) {
      nextPtr = ptr.getNext();

      Value key = ptr.getKey();

      if (index < start) {
      }
      else if (index < end) {
        _size--;
        
        Entry prev = ptr.getPrev();
        Entry next = ptr.getNext();

        if (prev != null)
          prev.setNext(next);
        else
          _head = next;

        if (next != null)
          next.setPrev(prev);
        else
          _tail = prev;

        if (key.isString())
          result.put(key, ptr.getValue());
        else
          result.put(ptr.getValue());
      }
      else if (replace == null) {
        return result;
      }
      else {
        for (Entry replaceEntry = replace.getHead();
             replaceEntry != null;
             replaceEntry = replaceEntry.getNext()) {
          _size++;

          Entry []entries = _entries;
          if ((entries == null && _size >= MIN_HASH)
              || (entries != null && entries.length <= 2 * _size)) {
            expand();
          }

          Entry entry = new Entry(createTailKey(), replaceEntry.getValue());

          addEntry(entry);

          Entry prev = ptr.getPrev();
          
          entry.setNext(ptr);
          entry.setPrev(prev);

          if (prev != null)
            prev.setNext(entry);
          else
            _head = entry;

          ptr.setPrev(entry);
        }

        return result;
      }

      index++;
    }

    if (replace != null) {
      for (Entry replaceEntry = replace.getHead();
           replaceEntry != null;
           replaceEntry = replaceEntry.getNext()) {
        put(replaceEntry.getValue());
      }
    }

    return result;
  }

  /**
   * Slices.
   */
  @Override
  public ArrayValue slice(Env env, int start, int end, boolean isPreserveKeys)
  {
    ArrayValueImpl array = new ArrayValueImpl();

    int i = 0;
    for (Entry ptr = _head; i < end && ptr != null; ptr = ptr.getNext()) {
      if (start > i++)
        continue;

        Value key = ptr.getKey();
        Value value = ptr.getValue();

        if (isPreserveKeys || key.isString())
          array.put(key, value);
        else
          array.put(value);
    }

    return array;
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  @Override
  public Value getArg(Value index, boolean isTop)
  {
    if (_isDirty) // XXX: needed?
      copyOnWrite();

    // php/3d42
    //if (isTop)
      //return new ArgGetValue(this, index);

    Entry entry = getEntry(index);

    if (entry != null) {
      // php/3d48, php/39aj
      Value value = entry.getValue();

      // php/3d42
      if (! isTop && value.isset())
        return value;
      else
        return entry.toArg();
    }
    else {
      // php/3d49
      return new ArgGetValue(this, index);
    }
  }

  /**
   * Returns the field value, creating an object if it's unset.
   */
  @Override
  public Value getObject(Env env, Value fieldName)
  {
    Value value = get(fieldName);

    if (! value.isset()) {
      value = env.createObject();

      put(fieldName, value);
    }

    return value;
  }

  /**
   * Returns the value as an array.
   */
  public Value getArray(Value index)
  {
    // php/3482, php/3483

    if (_isDirty)
      copyOnWrite();

    Entry entry = createEntry(index);

    Value value = entry.toValue();
    Value array = value.toAutoArray();

    if (value != array) {
      value = array;

      entry.set(array);
    }

    return array;
  }

  /**
   * Returns the value as an array, using copy on write if necessary.
   */
  public Value getDirty(Value index)
  {
    if (_isDirty)
      copyOnWrite();

    return get(index);
  }

  /**
   * Add
   */
  public Value put(Value value)
  {
    if (_isDirty)
      copyOnWrite();

    Value key = createTailKey();
    
    append(key, value);

    return value;
  }

  /**
   * Sets the array ref.
   */
  public Var putVar()
  {
    if (_isDirty)
      copyOnWrite();

    // 0d0d
    Value tailKey = createTailKey();

    return getVar(tailKey);
  }

  /**
   * Creatse a tail index.
   */
  public Value createTailKey()
  {
    if (_nextAvailableIndex < 0)
      updateNextAvailableIndex();

    return LongValue.create(_nextAvailableIndex);
  }

  /**
   * Gets a new value.
   */
  public Value get(Value key)
  {
    key = key.toKey();

    Entry []entries = _entries;
    Entry entry;

    if (entries != null) {
      int hash = key.hashCode() & _hashMask;

      entry = entries[hash];
    }
    else {
      entry = _head;
    }

    for (; entry != null; entry = entry.getNextHash()) {
      Value entryKey = entry.getKey();

      if (key == entryKey || key.equals(entryKey)) {
        //Var var = entry._var;

        //return var != null ? var.toValue() : entry._value;
        // return entry._value.toValue(); // php/39a1

        // 4.0.4 - _value.toValue() is marginally faster than _var
        return entry.toValue();

      }
    }

    return UnsetValue.UNSET;
  }

  /**
   * Returns the value in the array as-is.
   * (i.e. without calling toValue() on it).
   */
  @Override
  public Value getRaw(Value key)
  {
    key = key.toKey();

    Entry []entries = _entries;
    Entry entry;

    if (entries != null) {
      int hashMask = _hashMask;
      int hash = key.hashCode() & hashMask;

      entry = entries[hash];
    }
    else
      entry = _head;

    for (; entry != null; entry = entry.getNextHash()) {
      Value entryKey = entry.getKey();

      if (key == entryKey || key.equals(entryKey)) {
        return entry.getRawValue();
        /*
          Var var = entry._var;

          return var != null ? var : entry._value;
        */
      }
    }

    return UnsetValue.UNSET;
  }

  /**
   * Returns the corresponding key if this array contains the given value
   *
   * @param value to search for in the array
   *
   * @return the key if it is found in the array, NULL otherwise
   */
  @Override
  public Value contains(Value value)
  {
    for (Entry entry = getHead(); entry != null; entry = entry.getNext()) {
      if (entry.getValue().eq(value))
        return entry.getKey();
    }

    return NullValue.NULL;
  }

  /**
   * Returns the corresponding key if this array contains the given value
   *
   * @param value to search for in the array
   *
   * @return the key if it is found in the array, NULL otherwise
   */
  @Override
  public Value containsStrict(Value value)
  {
    for (Entry entry = getHead(); entry != null; entry = entry.getNext()) {
      if (entry.getValue().eql(value))
        return entry.getKey();
    }

    return NullValue.NULL;
  }

  /**
   * Returns the corresponding value if this array contains the given key
   *
   * @param key to search for in the array
   *
   * @return the value if it is found in the array, NULL otherwise
   */
  @Override
  public Value containsKey(Value key)
  {
    Entry entry = getEntry(key);

    if (entry != null)
      return entry.getValue();
    else
      return null;
  }

  /**
   * Gets a new value.
   */
  private Entry getEntry(Value key)
  {
    key = key.toKey();

    Entry []entries = _entries;
    Entry entry;
    
    if (entries != null) {
      int hash = key.hashCode() & _hashMask;

      entry = entries[hash];
    }
    else
      entry = _head;
    
    for (; entry != null; entry = entry.getNextHash()) {
      Value entryKey = entry.getKey();

      if (key == entryKey || key.equals(entryKey))
        return entry;
    }

    return null;
  }

  /**
   * Removes a value.
   */
  @Override
  public Value remove(Value key)
  {
    if (_isDirty)
      copyOnWrite();

    key = key.toKey();

    Entry []entries = _entries;
    Entry entry;

    int hash = key.hashCode() & _hashMask;
    
    if (entries != null) {
      entry = entries[hash];
    }
    else
      entry = _head;
    
    Entry prevHash = null;

    for (; entry != null; entry = entry.getNextHash()) {
      Value entryKey = entry.getKey();
      
      if (key == entryKey || key.equals(entryKey)) {
        if (prevHash != null)
          prevHash.setNextHash(entry.getNextHash());
        else if (entries != null)
          entries[hash] = entry.getNextHash();
        else
          _head = entry.getNextHash();

        return removeEntry(key, entry);
      }

      prevHash = entry;
    }

    return UnsetValue.UNSET;
  }

  private Value removeEntry(Value key, Entry entry)
  {
    Entry next = entry.getNext();
    Entry prev = entry.getPrev();

    if (prev != null)
      prev.setNext(next);
    else
      _head = next;

    if (next != null)
      next.setPrev(prev);
    else
      _tail = prev;

    entry.setPrev(null);
    entry.setNext(null);

    setCurrent(_head);

    _size--;

    Value value = entry.getValue();

    if (key.nextIndex(-1) == _nextAvailableIndex) {
      _nextAvailableIndex = -1;
    }

    return value;
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Var getVar(Value index)
  {
    if (_isDirty)
      copyOnWrite();

    Entry entry = createEntry(index);
    // quercus/0431

    return entry.toVar(); // _value.toSimpleVar();
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Var getRef(Value index)
  {
    if (_isDirty)
      copyOnWrite();

    Entry entry = createEntry(index);
    // quercus/0431

    return entry.toVar(); // _value.toSimpleVar();
  }

  /**
   * Creates the entry for a key.
   */
  private Entry createEntry(Value key)
  {
    // XXX: "A key may be either an integer or a string. If a key is
    //       the standard representation of an integer, it will be
    //       interpreted as such (i.e. "8" will be interpreted as 8,
    //       while "08" will be interpreted as "08")."
    //
    //            http://us3.php.net/types.array

    key = key.toKey();

    int hash = key.hashCode();

    int hashMask = _hashMask;

    hash = hash & hashMask;

    Entry []entries = _entries;
    Entry entry;

    if (entries != null) {
      entry = entries[hash];
    }
    else {
      entry = _head;
    }

    for (; entry != null; entry = entry.getNextHash()) {
      Value entryKey = entry.getKey();

      if (key == entryKey)
        return entry;

      if (key.equals(entryKey))
        return entry;
    }

    _size++;

    Entry newEntry = new Entry(key);
    if (_nextAvailableIndex >= 0)
      _nextAvailableIndex = key.nextIndex(_nextAvailableIndex);

    if (_entries == null && _size < MIN_HASH) {
      if (_tail != null)
        _tail.setNextHash(newEntry);
    }
    else {
      if (_entries == null || _entries.length <= 2 * _size) {
        expand();
        hash = key.hashCode() & _hashMask;
      }

      Entry head = _entries[hash];

      newEntry.setNextHash(head);
      _entries[hash] = newEntry;
    }

    if (_head == null) {
      newEntry.setPrev(null);
      newEntry.setNext(null);

      _head = newEntry;
      _tail = newEntry;
      setCurrent(newEntry);
    }
    else {
      newEntry.setPrev(_tail);
      newEntry.setNext(null);

      _tail.setNext(newEntry);
      _tail = newEntry;
    }

    return newEntry;
  }

  private Entry createNewEntry(Value key)
  {
    key = key.toKey();

    int hashMask = _hashMask;
    int hash = key.hashCode() & hashMask;

    _size++;

    Entry newEntry = new Entry(key);
    if (_nextAvailableIndex >= 0)
      _nextAvailableIndex = key.nextIndex(_nextAvailableIndex);

    if (_entries == null && _size < MIN_HASH) {
      if (_tail != null)
        _tail.setNextHash(newEntry);
    }
    else {
      if (_entries == null || _entries.length <= 2 * _size) {
        expand();
        hash = key.hashCode() & _hashMask;
      }

      Entry head = _entries[hash];

      newEntry.setNextHash(head);
      _entries[hash] = newEntry;
    }

    if (_head == null) {
      newEntry._prev = null;
      newEntry.setNext(null);

      _head = newEntry;
      _tail = newEntry;
      setCurrent(newEntry);
    }
    else {
      newEntry._prev = _tail;
      newEntry.setNext(null);

      _tail.setNext(newEntry);
      _tail = newEntry;
    }

    return newEntry;
  }

  private void expand()
  {
    Entry []entries = _entries;

    if (entries == null)
      _entries = new Entry[8];
    else
      _entries = new Entry[2 * entries.length];

    _hashMask = _entries.length - 1;

    for (Entry entry = _head; entry != null; entry = entry.getNext()) {
      addEntry(entry);
    }
  }

  private void addEntry(Entry entry)
  {
    Value key = entry.getKey();
    
    Entry []entries = _entries;
    
    if (entries != null) {
      int hash = key.hashCode() & _hashMask;

      Entry head = entries[hash];

      entry.setNextHash(head);

      entries[hash] = entry;
    }

    if (_nextAvailableIndex >= 0)
      _nextAvailableIndex = key.nextIndex(_nextAvailableIndex);
  }

  /**
   * Updates _nextAvailableIndex on a remove of the highest value
   */
  private void updateNextAvailableIndex()
  {
    _nextAvailableIndex = 0;

    for (Entry entry = _head; entry != null; entry = entry.getNext()) {
      _nextAvailableIndex = entry.getKey().nextIndex(_nextAvailableIndex);
    }
  }

  /**
   * Pops the top value.
   */
  @Override
  public Value pop(Env env)
  {
    if (_isDirty)
      copyOnWrite();

    if (_tail != null)
      return remove(_tail.getKey());
    else
      return NullValue.NULL;
  }

  public final Entry getHead()
  {
    return _head;
  }

  protected final Entry getTail()
  {
    return _tail;
  }

  /**
   * Shuffles the array
   */
  public Value shuffle()
  {
    if (_isDirty)
      copyOnWrite();

    Entry []values = new Entry[size()];

    int length = values.length;

    if (length == 0)
      return BooleanValue.TRUE;

    int i = 0;
    for (Entry ptr = _head; ptr != null; ptr = ptr.getNext())
      values[i++] = ptr;

    for (i = 0; i < length; i++) {
      int rand = RandomUtil.nextInt(length);

      Entry temp = values[rand];
      values[rand] = values[i];
      values[i] = temp;
    }

    _head = values[0];
    _head._prev = null;

    _tail = values[values.length - 1];
    _tail.setNext(null);

    for (i = 0; i < length; i++) {
      if (i > 0)
        values[i]._prev = values[i - 1];
      if (i < length - 1)
        values[i].setNext(values[i + 1]);
    }

    setCurrent(_head);

    return BooleanValue.TRUE;
  }

  /**
   * Returns the array keys.
   */
  @Override
  public Value getKeys()
  {
    if (_constSource != null)
      return _constSource.getKeys();
    else
      return super.getKeys();
  }

  /**
   * Returns the array keys.
   */
  @Override
  public Value getValues()
  {
    if (_constSource != null)
      return _constSource.getValues();
    else
      return super.getValues();
  }

  //
  // Java serialization code
  //

  private void writeObject(ObjectOutputStream out)
    throws IOException
  {
    out.writeInt(_size);

    for (Map.Entry<Value,Value> entry : entrySet()) {
      out.writeObject(entry.getKey());
      out.writeObject(entry.getValue());
    }
  }

  private void readObject(ObjectInputStream in)
    throws ClassNotFoundException, IOException
  {
    int size = in.readInt();

    int capacity = DEFAULT_SIZE;

    while (capacity < 4 * size) {
      capacity *= 2;
    }

    _entries = new Entry[capacity];
    _hashMask = _entries.length - 1;

    for (int i = 0; i < size; i++) {
      put((Value) in.readObject(), (Value) in.readObject());
    }
  }

  //
  // Java generator code
  //

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PrintWriter out)
    throws IOException
  {
    out.print("new ConstArrayValue(");

    if (getSize() < ArrayValueComponent.MAX_SIZE) {
      out.print("new Value[] {");

      for (Entry entry = getHead(); entry != null; entry = entry.getNext()) {
        if (entry != getHead())
          out.print(", ");

            if (entry.getKey() != null)
          entry.getKey().generate(out);
            else
          out.print("null");
      }

      out.print("}, new Value[] {");

      for (Entry entry = getHead(); entry != null; entry = entry.getNext()) {
        if (entry != getHead())
      out.print(", ");

        entry.getValue().generate(out);
      }

      out.print("}");
    }
    else {
      ArrayValueComponent.generate(out, this);
    }

    out.print(")");
  }
}
