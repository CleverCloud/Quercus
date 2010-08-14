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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.quercus.env.CgiEnv;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.vfs.Path;
import com.caucho.vfs.StdoutStream;
import com.caucho.vfs.WriteStream;

public class CgiQuercus
  extends CliQuercus
{
  @Override
  public Env createEnv(QuercusPage page,
                       WriteStream out,
                       HttpServletRequest request,
                       HttpServletResponse response)
  {
    return new CgiEnv(this, page, out, request, response);
  }
  
  public static void main(String []args)
    throws IOException
  {
    CgiQuercus quercus = new CgiQuercus();
    
    quercus.parseArgs(args);
    
    quercus.init();
    quercus.start();
    
    if (quercus.getFileName() != null) {
      quercus.execute();
    }
    else {
      throw new RuntimeException("input file not specified");
    }
  }
  
  /**
   * Returns the SAPI (Server API) name.
   */
  @Override
  public String getSapiName()
  {
    return "cgi";
  }

  @Override
  public void execute()
    throws IOException
  {
    Path path = getPwd().lookup(getFileName());
    
    QuercusPage page = parse(path);
    
    WriteStream os = new WriteStream(StdoutStream.create());
      
    os.setNewlineString("\n");
    os.setEncoding("iso-8859-1");
    
    Env env = createEnv(page, os, null, null);
    env.start();
    
    try {
      env.execute();
    } catch (QuercusDieException e) {
    } catch (QuercusExitException e) {
    }
    
    env.close();
    
    os.flush();
  }
}
