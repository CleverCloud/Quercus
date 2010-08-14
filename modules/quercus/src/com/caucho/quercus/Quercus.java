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

package com.caucho.quercus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.*;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.vfs.Path;
import com.caucho.vfs.StdoutStream;
import com.caucho.vfs.StringPath;
import com.caucho.vfs.WriteStream;

public class Quercus
  extends QuercusContext
{
  private static final Logger log
    = Logger.getLogger(Quercus.class.getName());

  private String _fileName;
  private String []_args;

  public Quercus()
  {
    super();

    init();
  }

  //
  // command-line main
  //

  public static void main(String []args)
    throws IOException
  {
    Quercus quercus = new Quercus();

    if (! quercus.parseArgs(args)) {
      printUsage();
      return;
    }
    
    quercus.init();
    quercus.start();

    if (quercus.getFileName() != null) {
      quercus.execute();
    }
    else {
      throw new RuntimeException("input file not specified");
    }
  }

  public static void printUsage()
  {
    System.out
      .println("usage: com.caucho.quercus.Quercus [flags] <file> [php-args]");
    System.out.println(" -f            : Explicitly set the script filename.");
    System.out.println(" -d name=value : Sets a php ini value.");
  }

  /**
   * Returns the SAPI (Server API) name.
   */
  @Override
  public String getSapiName()
  {
    return "cli";
  }

  public String getFileName()
  {
    return _fileName;
  }

  public void setFileName(String name)
  {
    _fileName = name;
  }

  protected boolean parseArgs(String []args)
  {
    ArrayList<String> phpArgList = new ArrayList<String>();

    int i = 0;
    for (; i < args.length; i++) {
      if ("-d".equals(args[i])) {
        int eqIndex = args[i + 1].indexOf('=');

        String name = "";
        String value = "";

        if (eqIndex >= 0) {
          name = args[i + 1].substring(0, eqIndex);
          value = args[i + 1].substring(eqIndex + 1);
        }
        else {
          name = args[i + 1];
        }

        i++;
        setIni(name, value);
      }
      else if ("-f".equals(args[i])) {
        _fileName = args[++i];
      }
      else if ("-q".equals(args[i])) {
        // quiet
      }
      else if ("-n".equals(args[i])) {
        // no php-pip
      }
      else if ("--".equals(args[i])) {
        break;
      }
      else if ("-h".equals(args[i])) {
        return false;
      }
      else if (args[i].startsWith("-")) {
        System.out.println("unknown option: " + args[i]);
        return false;
      }
      else {
        phpArgList.add(args[i]);
      }
    }

    for (; i < args.length; i++) {
      phpArgList.add(args[i]);
    }

    _args = phpArgList.toArray(new String[phpArgList.size()]);

    if (_fileName == null && _args.length > 0)
      _fileName = _args[0];

    return true;
  }

  public void execute()
    throws IOException
  {
    Path path = getPwd().lookup(_fileName);

    execute(path);
  }

  public void execute(String code)
    throws IOException
  {
    Path path = new StringPath(code);

    execute(path);
  }

  public void execute(Path path)
    throws IOException
  {
    QuercusPage page = parse(path);

    WriteStream os = new WriteStream(StdoutStream.create());

    os.setNewlineString("\n");
    os.setEncoding("iso-8859-1");

    Env env = createEnv(page, os, null, null);
    env.start();

    if (_args.length > 0)
      env.setArgs(_args);

    try {
      env.execute();
    } catch (QuercusDieException e) {
      log.log(Level.FINER, e.toString(), e);
    } catch (QuercusExitException e) {
      log.log(Level.FINER, e.toString(), e);
    } catch (QuercusErrorException e) {
      log.log(Level.FINER, e.toString(), e);
    } finally {
      env.close();

      os.flush();
    }
  }
}
