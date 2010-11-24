/*
 * Copyright (c) 2010 Clever Cloud -- all rights reserved
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
 * @author Kevin Decherf
 */
package com.caucho.quercus.env;

public class CompiledConstUnicodeStringValue extends UnicodeBuilderValue {

    protected long _long;
    protected double _double;
    protected String _string;
    protected Value _key;
    protected ValueType _valueType;
    protected char[] _serializeValue;
    private final int _compiledHashCode;

    public CompiledConstUnicodeStringValue(StringValue s) {
	super(s);

	_long = s.toLong();
	_double = s.toDouble();
	_string = s.toString();

	_valueType = s.getValueType();
	_compiledHashCode = s.hashCode();
	_key = s.toKey();
    }

    public CompiledConstUnicodeStringValue(String s) {
	super(s);

	_long = super.toLong();
	_double = super.toDouble();
	_string = s;
	_valueType = super.getValueType();
	_compiledHashCode = super.hashCode();
	_key = super.toKey();
    }

    public boolean isStatic() {
	return true;
    }

    /**
     * Converts to a long value
     */
    @Override
    public LongValue toLongValue() {
	return LongValue.create(_long);
    }

    /**
     * Converts to a double value
     */
    @Override
    public DoubleValue toDoubleValue() {
	return DoubleValue.create(_double);
    }

    /**
     * Converts to a long.
     */
    @Override
    public long toLong() {
	return _long;
    }

    /**
     * Converts to a double.
     */
    @Override
    public double toDouble() {
	return _double;
    }

    /**
     * Returns the ValueType.
     */
    @Override
    public ValueType getValueType() {
	return _valueType;
    }

    /**
     * Converts to a key.
     */
    @Override
    public final Value toKey() {
	return _key;
    }

    @Override
    public final int hashCode() {
	return _compiledHashCode;
    }

    public final String toString() {
	return _string;
    }
}
