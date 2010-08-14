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

package com.caucho.quercus.resources;

import com.caucho.quercus.env.*;

/**
 * Represents a PHP stream context.
 */
public class StreamContextResource extends ResourceValue {
  private ArrayValue _options;
  private ArrayValue _parameters;
  
  public StreamContextResource()
  {
    this(null);
  }

  public StreamContextResource(ArrayValue options)
  {
    this(options, null);
  }

  public StreamContextResource(ArrayValue options, ArrayValue parameters)
  {
    if (options == null)
      options = new ArrayValueImpl();

    if (parameters == null)
      parameters = new ArrayValueImpl();
    
    _options = options;
    _parameters = parameters;
  }

  /**
   * Returns the options.
   */
  public ArrayValue getOptions()
  {
    return _options;
  }

  /**
   * Sets the options.
   */
  public void setOptions(ArrayValue options)
  {
    _options = options;
  }

  /**
   * Sets an option
   */
  public void setOption(Env env, StringValue wrapper,
                        StringValue option, Value value)
  {
    _options.getArray(wrapper).put(option, value);
  }

  /**
   * Sets the parameters.
   */
  public void setParameters(ArrayValue parameters)
  {
    _parameters = parameters;
  }
  
  /**
   * Converts to a string.
   * @param env
   */
  public StringValue toString(Env env)
  {
    return env.createString("StreamContextResource[]");
  }
}

