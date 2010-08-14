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

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.env.ConstStringValue;
import com.caucho.quercus.env.TraversableDelegate;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

import java.util.Iterator;
import java.util.Map;

/**
 * A delegate that intercepts requests for iterator's and delegates
 * them to the iteerator returned by {@link IteratorAggregate@getIterator()}
 */
public class IteratorAggregateDelegate
  implements TraversableDelegate
{
  private static final L10N L = new L10N(IteratorAggregateDelegate.class);
  
  private static final StringBuilderValue GET_ITERATOR
    = new ConstStringValue("getIterator");
  
  private static final IteratorDelegate _iteratorDelegate
    = new IteratorDelegate();

  public Iterator<Map.Entry<Value, Value>>
    getIterator(Env env, ObjectValue qThis)
  {
    Value target = getTarget(env, qThis);

    if (target instanceof ObjectValue) {
      return target.getIterator(env);
    }
    else
      throw new QuercusException(L.l("'{0}' is not a valid Traversable",
                                     qThis));
  }

  public Iterator<Value> getKeyIterator(Env env, ObjectValue qThis)
  {
    Value target = getTarget(env, qThis);
    
    if (target instanceof ObjectValue)
      return _iteratorDelegate.getKeyIterator(env, (ObjectValue) target);
    else
      throw new QuercusException(L.l("'{0}' is not a valid Traversable",
                                     qThis));
  }

  public Iterator<Value> getValueIterator(Env env, ObjectValue qThis)
  {
    Value target = getTarget(env, qThis);
    
    if (target instanceof ObjectValue)
      return _iteratorDelegate.getValueIterator(env, (ObjectValue) target);
    else
      throw new QuercusException(L.l("'{0}' is not a valid Traversable",
                                     qThis));
  }

  private Value getTarget(Env env, ObjectValue qThis)
  {
    return qThis.callMethod(env, GET_ITERATOR);
  }
}
