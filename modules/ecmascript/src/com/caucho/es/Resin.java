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

import com.caucho.VersionFactory;
import com.caucho.es.parser.Parser;
import com.caucho.server.util.CauchoSystem;
import com.caucho.vfs.MergePath;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.VfsStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.HashMap;

/**
 * Resin is the primary public entry to the JavaScript compiler.
 *
 * <p>The script must first be compiled, then executed.
 *
 * <p>JavaScript import uses a script path to located the imported
 * script.  The scriptPath is just an array of Paths.  If the scriptPath
 * is null, Resin will default to the script's directory, followed
 * by $RESIN_HOME/scripts.
 * <p>Here's the default, where 'is' is the input stream:
 *
 * <pre><code>
 * Path scriptPath[] = new Path[] {
 *   is.getPath().getParent(),
 *   CauchoSystem.getResinHome().lookup("scripts")
 * }
 * </code></pre>
 *
 * <p>As described in the Script object, programs set global variables
 * with a hash map.  So a simple call might look like:
 *
 * <pre><code>
 * Script script = Resin.parse(Pwd.lookup("foo.js"), scriptPath);
 *
 * HashMap props = new HashMap();
 * props.put("out", System.out);
 *
 * script.execute(props, null);
 * </code></pre>
 *
 * <p>Executing the Script object is threadsafe. 
 * The ScriptClosure object, of course, is not threadsafe.
 */
public class Resin {
  static final String COPYRIGHT =
    "Copyright (c) 1998-2010 Caucho Technology.  All rights reserved.";
      
  private static WriteStream dbg;

  private Resin()
  {
  }

  public static void init(ESFactory factory)
  {
    ESBase.init(factory);
  }
  
  public static Parser getParser()
    throws IOException, ESException
  {
    init(null);

    return new Parser();
  }

  public static void main(String []argv)
  {
    String resinConf = CauchoSystem.getResinConfig();
    boolean verbose = false;

    try {
      ReadStream is;
      String name;
      int shift = 0;

      while (argv.length > shift) {
        if (argv[shift].equals("-v")) {
          verbose = true;
          shift++;
        } else if (shift + 1 < argv.length && argv[shift].equals("-conf")) {
          resinConf = argv[shift + 1];
          shift += 2;
        } else if (argv[shift].equals("--version")) {
          System.out.println(VersionFactory.getVersion());
          System.exit(0);
        } else
          break;
      }

      if (argv.length == shift) {
        is = VfsStream.openRead(System.in);
        name = "stdin";
      }
      else {
        is = Vfs.lookupNative(argv[shift]).openRead();
        name = argv[shift++];
      }

      Path conf = Vfs.lookup(resinConf);
      // Registry.setDefault(Registry.parse(conf));

      String []args;
      if (argv.length > shift)
        args = new String[argv.length - shift];
      else
        args = new String[0];
      for (int i = 0; i < argv.length - shift; i++)
        args[i] = argv[i + shift];

      Path scriptPath = null;

      int p;
      if ((p = name.lastIndexOf('/')) >= 0) {
        Path subpath = Vfs.lookupNative(name.substring(0, p));

        MergePath mergePath = new MergePath();
        mergePath.addMergePath(Vfs.lookup());
        mergePath.addMergePath(subpath);
        mergePath.addMergePath(CauchoSystem.getResinHome().lookup("scripts"));
        mergePath.addClassPath(Thread.currentThread().getContextClassLoader());
        scriptPath = mergePath;
      }

      Parser parser = new Parser();
      parser.setScriptPath(scriptPath);
      
      Script script = parser.parse(is);

      WriteStream stream = VfsStream.openWrite(System.out);
      HashMap properties = new HashMap();
      properties.put("out", stream);
      properties.put("arguments", args);
      properties.put("File", Vfs.lookup());

      script.execute(properties, null);

      stream.flush();
    } catch (ESParseException e) {
      System.err.println(e.getMessage());
    } catch (ESException e) {
      System.err.println(e.getMessage());
      if (verbose)
        e.printStackTrace();
      else
        e.printESStackTrace();
    } catch (Throwable e) {
      System.out.println("Exception: " + e);
      e.printStackTrace();
    }
  }
}
