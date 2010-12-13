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
 * @author Sam
 */
package com.caucho.quercus.lib.spl;

import com.caucho.quercus.annotation.Name;
import com.caucho.quercus.annotation.This;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.Value;

import java.util.Map;

public class IteratorIterator implements OuterIterator {

    private Env _env;
    private Value _qThis;
    private Value _value = NullValue.NULL;
    private Value _key = NullValue.NULL;
    private java.util.Iterator<Map.Entry<Value, Value>> _iterator;
    private Map.Entry<Value, Value> _current;

    @Name("__construct")
    public void __construct(Env env,
	    @This Value iter) {
	_env = env;
	_qThis = iter;

	_iterator = _qThis.getBaseIterator(_env);
    }

    public Value getInnerIterator() {
	return _qThis;
    }

    public Value current(Env env) {
	return _value;
    }

    public Value key(Env env) {
	return _key;
    }

    public void next(Env env) {
	if (_iterator.hasNext()) {
	    Map.Entry<Value, Value> buffer;
	    buffer = _iterator.next();
	    _value = buffer.getValue();
	    _key = buffer.getKey();
	}
    }

    public void rewind() {
	_iterator = _qThis.getBaseIterator(_env);
    }

    public boolean valid() {
	return true;
    }
}
