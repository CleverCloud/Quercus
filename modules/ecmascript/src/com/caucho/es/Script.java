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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.es;

import com.caucho.java.LineMap;
import com.caucho.util.Exit;
import com.caucho.vfs.Path;

import java.util.HashMap;

/**
 * The Script object represents a compiled JavaScript.  Executing it
 * is thread safe.  To create a Script, use the Parser class to parse
 * a file.
 *
 * <p>Java programs set JavaScript Global properties by adding objects to
 * a HashMap.  Typically you will at least assign the 'File' and 
 * the 'out' objects.  The running script will 
 * see these objects as properties of the Global object.  If you set the
 * 'out' object, the script can use the bare bones 'writeln("foo")' to
 * write to 'out'.
 *
 * <pre><code>
 * HashMap map = new HashMap();
 * map.put("File", Vfs.lookup());
 * map.put("out", System.out);
 * map.put("myObject", myObject);
 *
 * script.execute(map, null);
 * </code></pre>
 *
 * <p>You can also make any Java object be the global prototype.
 * Essentially, the effect is similar to the HashMap technique, but
 * it's a little simpler.
 *
 * <p>Scripts are thread-safe.  Multiple script instances can
 * safely execute in separate threads.  Script.execute creates the
 * entire JavaScript global object hierarchy fresh for each execution.
 * So one Script execution cannot interfere with another, even by doing
 * evil things like modifying the Object prototype.
 *
 * <p>Of course, wrapped Java objects shared by script invocations
 * must still be synchronized.
 */

abstract public class Script {
  protected Path scriptPath;
  protected Path classDir;
  
  /**
   * Internal method to check if the source files have changed.
   */
  public boolean isModified()
  {
    return true;
  }

  /**
   * Internal method to set the script search path for imported
   * scripts.
   */
  public void setScriptPath(Path scriptPath)
  {
    this.scriptPath = scriptPath;
  }

  /**
   * Internal method to set the work directory for generated *.java
   * and *.class.
   */
  public void setClassDir(Path classDir)
  {
    this.classDir = classDir;
  }

  /**
   * Returns the map from source file line numbers to the generated
   * java line numbers.
   */
  public LineMap getLineMap()
  {
    return null;
  }

  /**
   * Execute the script; the main useful entry.
   *
   * <p>Calling programs can make Java objects available as properties
   * of the global object by creating a property hash map or assigning
   * a global prototype.
   *
   * <pre><code>
   * HashMap map = new HashMap();
   * map.put("File", Vfs.lookup());
   * map.put("out", System.out);
   * map.put("myObject", myObject);
   * script.execute(map, null);
   * </code></pre>
   *
   * Then the JavaScript can use the defined objects:
   * <pre><code>
   * out.println(myObject.myMethod("foo"));
   * </code></pre>
   *
   * @param properties A hash map of global properties.
   * @param proto Global prototype.  Gives the script direct access to
   * the java methods of the object.
   *
   * @return String value of the last expression, like the JavaScript eval.
   * This is useful only for testing.
   */
  public String execute(HashMap properties, Object proto) throws Throwable
  {
    Global oldGlobal = Global.getGlobalProto();
    boolean doExit = Exit.addExit();

    try {
      Global resin = new Global(properties, proto, classDir,
                                scriptPath, getClass().getClassLoader());
    
      resin.begin();
      
      ESGlobal global = initClass(resin);

      ESBase value = global.execute();

      if (value == null)
        return null;
      else
        return value.toStr().toString();
    } finally {
      Global.end(oldGlobal);
      
      if (doExit)
        Exit.exit();
    }
  }
  
  /**
   * Execute the program, returning a closure of the global state.
   * <code>executeClosure</code> will execute the global script.
   *
   * <p>Later routines can then call into the closure.  The closure
   * is not thread-safe.  So only a single thread may execute the
   * closure.
   *
   * @param properties A hash map of global properties.
   * @param proto Global prototype.  Gives the script direct access to
   * the java methods of the object.
   *
   * @return the closure
   */
  public ScriptClosure executeClosure(HashMap properties, Object proto)
    throws Throwable
  {
    Global resin = new Global(properties, proto, classDir,
                              scriptPath, getClass().getClassLoader());
    boolean doExit = Exit.addExit();
    Global oldGlobal = resin.begin();

    try {
      ESGlobal global = initClass(resin);

      global.execute();

      return new ScriptClosure(resin, global, this);
    } finally {
      resin.end(oldGlobal);
      if (doExit)
        Exit.exit();
    }
  }

  /**
   * Internal method to initialize the script after loading it.
   */
  public ESGlobal initClass(Global resin, ESObject global)
    throws Throwable
  {
    return initClass(resin);
  }

  /**
   * Internal method implemented by the generated script for initialization.
   */
  public abstract ESGlobal initClass(Global resin)
    throws Throwable;

  /**
   * Internal method to export objects.
   */
  public void export(ESObject dest, ESObject src)
    throws Throwable
  {
  }
}
