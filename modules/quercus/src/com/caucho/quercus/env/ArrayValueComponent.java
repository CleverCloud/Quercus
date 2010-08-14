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

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Represents a compileable piece of an array.
 */
public class ArrayValueComponent
{
  public static final int MAX_SIZE = 512;
  public static final int MAX_DYNAMIC_SIZE = 1024;
  
  protected Value []_keys;
  protected Value []_values;
  
  public ArrayValueComponent()
  {
    init();
  }
  
  public ArrayValueComponent(Value []keys, Value []values)
  {
    _keys = keys;
    _values = values;
  }
  
  public static ArrayValueComponent[] create(ArrayValue array)
  {
    int size = array.getSize();
    int bins = size / MAX_SIZE;
    
    if (size % MAX_SIZE > 0)
      bins++;
    
    ArrayValueComponent []components = new ArrayValueComponent[bins];
    
    int bin = 0;
    
    Value []keys = array.keysToArray();
    Value []values = array.valuesToArray();
    
    while (bin < bins) {
      int binSize = MAX_SIZE;
      
      if (bin + 1 == bins)
        binSize = size - bin * MAX_SIZE;
      
      Value []k = new Value[binSize];
      Value []v = new Value[binSize];
      
      System.arraycopy(keys, bin * MAX_SIZE, k, 0, binSize);
      System.arraycopy(values, bin * MAX_SIZE, v, 0, binSize);
      
      components[bin] = new ArrayValueComponent(k, v);
      
      bin++;
    }
    
    return components;
  }
  
  public static void generate(PrintWriter out, ArrayValue array)
    throws IOException
  {
    int size = array.getSize();
    int bins = size / MAX_SIZE;
    
    if (size % MAX_SIZE > 0)
      bins++;
    
    int bin = 0;
    
    Value []keys = array.keysToArray();
    Value []values = array.valuesToArray();
    
    out.print("new ArrayValueComponent[] {");
    
    while (bin < bins) {
      int binSize = MAX_SIZE;
      
      if (bin != 0)
        out.print(", ");
        
      if (bin + 1 == bins)
        binSize = size - bin * MAX_SIZE;
      
      out.println("new ArrayValueComponent() {");
      out.println("    public void init() {");
      
      out.print("      _keys = new Value[] {");
      for (int i = 0; i < binSize; i++) {
        if (i != 0)
          out.print(", ");
        
        keys[i + bin * MAX_SIZE].generate(out);
      }
      out.println("};");
      
      out.print("      _values = new Value[] {");
      for (int i = 0; i < binSize; i++) {
        if (i != 0)
          out.print(", ");
        
        values[i + bin * MAX_SIZE].generate(out);
      }
      out.println("};");
      
      out.println("    }");
      out.println("  }");
      
      bin++;
    }
    
    out.print("}");
  }
  
  public void init()
  {
  }
  
  public void init(Env env)
  {
  }
  
  public final void addTo(ArrayValue array)
  {
    for (int i = 0; i < _keys.length; i++) {
      if (_keys[i] != null)
        array.append(_keys[i], _values[i]);
      else
        array.put(_values[i]);
    }
  }
  
  public Value[] getKeys()
  {
    return _keys;
  }
  
  public Value[] getValues()
  {
    return _values;
  }
}

