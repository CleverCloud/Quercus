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

package com.caucho.jcr.svn;

import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.ReadWritePair;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Subversion Client class.
 */
public class SubversionClient {
  private final L10N L = new L10N(SubversionClient.class);
  private final Logger log
    = Logger.getLogger(SubversionClient.class.getName());;

  private Path _path;
  private ReadStream _is;
  private WriteStream _os;
  
  private SubversionInput _in;
  private long _rev = 1;

  public SubversionClient(String host, int port)
    throws IOException
  {
    _path = Vfs.lookup("tcp://" + host + ":" + port);

    ReadWritePair pair = _path.openReadWrite();

    _is = pair.getReadStream();
    _os = pair.getWriteStream();

    _in = new SubversionInput(_is);

    readHello();

    // anonymous login (?)
    String svnurl = "svn://" + host + ":" + port;
    println("( 2 ( edit-pipeline ) "+svnurl.length()+":"+svnurl+ " )");

    readLoginResponse();
    
    println("( ANONYMOUS ( 0: ) )");

    readSuccess();

    _in.expect('(');
    expectSuccess();
    _in.expect('(');

    String uuid = _in.readString();
    String url = _in.readString();
    
    _in.expect(')');
    _in.expect(')');
  }

  /**
   * Sends a get-latest-rev request to the client, returning the latest
   * version.
   */
  public long getLatestRev()
    throws IOException
  {
    println("( get-latest-rev ( ) )");
    
    readSuccess();

    _in.expect('(');
    expectSuccess();
    _in.expect('(');

    long value = _in.readLong();

    _rev = value;
    
    _in.expect(')');
    _in.expect(')');
    
    return value;
  }

  public String checkPath(String s)
    throws IOException
  {
    println("( check-path ( " + s.length() + ":" + s + " ( ) ) )");
    
    readSuccess();

    _in.expect('(');
    expectSuccess();
    _in.expect('(');

    String type = _in.readLiteral();
    
    _in.expect(')');
    _in.expect(')');
    
    return type;
  }

  public Object getDir(String s)
    throws IOException
  {
    // wantProps, wantContents
    println("( get-dir ( " + s.length() + ":" + s + " ( " + _rev + " ) false true ) )");
    
    readSuccess();

    _in.expect('(');
    expectSuccess();

    _in.expect('(');

    long dirVersion = _in.readLong();

    Object props = _in.readSexp();

    ArrayList<SubversionNode> results = new ArrayList<SubversionNode>();

    _in.expect('(');

    while (true) {
      _in.skipWhitespace();

      int ch = _in.read();

      if (ch == '(') {
        String name = _in.readString();
        String type = _in.readLiteral();
        long length = _in.readLong();
        boolean bValue = ! "false".equals(_in.readLiteral());
        long version = _in.readLong();

        _in.expect('(');
        String modified = _in.readString();
        _in.expect(')');

        _in.expect('(');
        String user = _in.readString();
        _in.expect(')');
        _in.expect(')');

        SubversionNode node;

        if ("dir".equals(type))
          node = new SubversionFolder(name);
        else if ("file".equals(type)) {
          SubversionFile file = new SubversionFile(name);
          file.setLength(length);
          node = file;
        }
        else
          node = new SubversionNode(name);

        node.setVersion(version);
        node.setUser(user);

        results.add(node);
      }
      else if (ch == ')')
        break;
      else {
        throw error(L.l("Expected '(' at {0} (0x{1})",
                        String.valueOf(ch),
                        Integer.toHexString(ch)));
      }
    }
      
    _in.expect(')');
    _in.expect(')');
    
    return results;
  }

  public Object update(long version, String s)
    throws IOException
  {
    boolean recurse = true;
    
    println("( update ( " +
            " ( " + version + " ) " +
            s.length() + ":" + s + " " +
            recurse + " ) )");
    
    readSuccess();
    
    boolean startEmpty = true;
    
    println("( set-path ( " +
            s.length() + ":" + s + " " +
            version + " " +
            startEmpty + " ) )");
    
    println("( finish-report ( ) )");
    
    readSuccess();

    while (true) {
      _in.expect('(');
      String cmd = _in.readLiteral();

      Object arg = _in.readSexp();
      _in.expect(')');

      System.out.println("CMD: " + cmd);

      if ("close-edit".equals(cmd))
        break;
    }
    
    return "ok";
  }

  public Object setPath(long version, String s)
    throws IOException
  {
    boolean startEmpty = true;
    
    println("( set-path ( " +
            s.length() + ":" + s + " " +
            version + " " +
            startEmpty + " ) )");
    
    println("( finish-report ( ) )");
    
    readSuccess();

    return _in.readSexp();
  }

  public Object finishReport()
    throws IOException
  {
    println("( finish-report ( ) )");
    
    readSuccess();

    return _in.readSexp();
  }

  public void doMore()
    throws IOException
  {
    println("( check-path ( 0: ( ) ) )");
    
    readSuccess();
    
    System.out.println(_in.readSexp());

    println("( get-dir ( 0: ( 1 ) false true ) )");
    
    readSuccess();
    
    System.out.println(_in.readSexp());
  }

  private void readHello()
    throws IOException
  {
    _in.expect('(');
    expectSuccess();
    _in.expect('(');

    long major = _in.readLong();
    long minor = _in.readLong();

    Object auth = _in.readSexp();
    Object cap = _in.readSexp();

    _in.expect(')');
    _in.expect(')');
  }

  private void readLoginResponse()
    throws IOException
  {
    _in.expect('(');
    expectSuccess();
    _in.expect('(');

    Object cap = _in.readSexp();

    String code = _in.readString();

    _in.expect(')');
    _in.expect(')');
  }

  public void readSuccess()
    throws IOException
  {
    _in.expect('(');
    
    expectSuccess();

    _in.readSexp();
    
    _in.expect(')');
  }

  public void expectSuccess()
    throws IOException
  {
    String token = _in.readLiteral();

    if (! "success".equals(token))
      throw error(L.l("Expected 'success' at {0}",
                      token));
  }

  private void println(String msg)
    throws IOException
  {
    if (log.isLoggable(Level.FINER))
      log.finer(msg);
    
    _os.println(msg);
  }

  private IOException error(String msg)
  {
    return new IOException(msg);
  }

  public void close()
  {
    ReadStream is = _is;
    _is = null;
    
    WriteStream os = _os;
    _os = null;

    _in.close();

    if (os != null) {
      try {
        os.close();
      } catch (IOException e) {
      }
    }

    if (is != null) {
      is.close();
    }
  }
}
