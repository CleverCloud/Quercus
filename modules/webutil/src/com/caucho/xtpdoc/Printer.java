/*
 * Copyright (c) 1998-2000 Caucho Technology -- all rights reserved
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
 * @author Emil Ong
 */

package com.caucho.xtpdoc;

import com.caucho.config.Config;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class Printer {
  public static void main(String []args)
  {
    Config config = new Config();
    config.setEL(false);

    if (args.length == 0) {
      System.out.println("usage: " + Printer.class.getName() + " <book.xml>");
      System.exit(1);
    }

    Path xtpFile = Vfs.lookup(args[0]);
    Book book = new Book();

    try {
      config.configure(book, xtpFile);

      OutputStreamWriter osw = new OutputStreamWriter(System.out);
      PrintWriter out = new PrintWriter(osw);

      book.writeLaTeX(out);

      osw.close();
      out.close();
    } catch (IOException e) {
      System.err.println("Error writing HTML: " + e);
    } catch (Exception e) {
      System.err.println("Error configuring document: " + e);

      e.printStackTrace();
    }
  }
}
