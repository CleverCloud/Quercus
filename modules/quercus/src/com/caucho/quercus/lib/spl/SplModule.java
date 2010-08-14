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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.util.CharBuffer;

/*
 * XXX: Not finished.
 */
public class SplModule extends AbstractQuercusModule
{
  private static String DEFAULT_EXTENSIONS = ".php,.inc";
  
  public String []getLoadedExtensions()
  {
    return new String[] { "SPL" };
  }
  
  public static Value class_implements(Env env,
                                        Value obj,
                                        @Optional boolean autoload)
  {
    QuercusClass cls;
    
    if (obj.isObject())
      cls = ((ObjectValue) obj.toObject(env)).getQuercusClass();
    else
      cls = env.findClass(obj.toString(), autoload,  true);

    if (cls != null)
      return cls.getInterfaces(env, autoload);
    else
      return BooleanValue.FALSE;
  }
  
  public static Value class_parents(Env env,
                                    Value obj,
                                    @Optional boolean autoload)
  {
    QuercusClass cls;

    if (obj.isObject())
      cls = ((ObjectValue) obj.toObject(env)).getQuercusClass();
    else
      cls = env.findClass(obj.toString(), autoload, true);

    if (cls != null) {
      ArrayValue array = new ArrayValueImpl();

      QuercusClass parent = cls;

      while ((parent = parent.getParent()) != null) {
        String name = parent.getName();
        
        array.put(name, name);
      }

      return array;
    }
    else
      return BooleanValue.FALSE;
  }
  
  public static boolean spl_autoload_register(Env env,
                                              @Optional Callable fun)
  {
    if (fun == null)
      fun = new CallbackFunction(env, "spl_autoload");
    
    env.addAutoloadFunction(fun);
    
    return true;
  }
  
  public static boolean spl_autoload_unregister(Env env,
                                                Callable fun)
  {
    env.removeAutoloadFunction(fun);
    
    return true;
  }
  
  public static Value spl_autoload_functions(Env env)
  {
    ArrayList<Callable> funList = env.getAutoloadFunctions();
    
    if (funList == null)
      return BooleanValue.FALSE;
    
    ArrayValue array = new ArrayValueImpl();

    int size = funList.size();
    for (int i = 0; i < size; i++) {
      Callable cb = funList.get(i);
      
      array.put(env.createString(cb.toString()));
    }
    
    return array;
  }
  
  public static String spl_autoload_extensions(Env env,
                                               @Optional String extensions)
  {
    String oldExtensions = getAutoloadExtensions(env);
    
    if (extensions != null)
      env.setSpecialValue("caucho.spl_autoload", extensions);
    
    return oldExtensions;
  }
  
  private static String getAutoloadExtensions(Env env)
  {
    Object obj = env.getSpecialValue("caucho.spl_autoload");
    
    if (obj == null)
      return DEFAULT_EXTENSIONS;
    else
      return (String) obj;
  }
  
  public static void spl_autoload(Env env,
                                  String className,
                                  @Optional String extensions)
  {
    if (env.findClass(className, false, true) != null)
      return;
    
    String []extensionList;
    
    if (extensions == null || "".equals(extensions))
      extensionList = new String[] { ".php", ".inc" };
    else
      extensionList = extensions.split("[,\\s]+");
    
    String filePrefix = className.toLowerCase();

    for (String ext : extensionList) {
      StringValue filename = new StringBuilderValue(filePrefix).append(ext);

      env.include(filename);
      
      QuercusClass cls = env.findClass(className, false, true);
      
      if (cls != null)
        return;
    }
  }
}
