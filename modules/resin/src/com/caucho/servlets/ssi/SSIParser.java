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

package com.caucho.servlets.ssi;

import com.caucho.util.ByteBuffer;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Serves server-side include files.
 */
public class SSIParser {
  private int _line;

  private final SSIFactory _factory;

  public SSIParser()
  {
    _factory = new SSIFactory();
  }

  public SSIParser(SSIFactory factory)
  {
    _factory = factory;
  }

  Statement parse(Path path)
    throws IOException
  {
    ReadStream is = path.openRead();

    try {
      ArrayList<Statement> statements = new ArrayList<Statement>();
      
      parse(is, statements);

      return new BlockStatement(statements);
    } finally {
      is.close();
    }
  }

  Statement parse(ReadStream is)
    throws IOException
  {
    ArrayList<Statement> statements = new ArrayList<Statement>();
      
    parse(is, statements);

    return new BlockStatement(statements);
  }

  /**
   * Parses a list of statements from the ssi stream.
   */
  private void parse(ReadStream is, ArrayList<Statement> statements)
    throws IOException
  {
    ByteBuffer bb = new ByteBuffer();
    int ch;

    while ((ch = is.read()) >= 0) {
      if (ch != '<') {
        if (ch == '\n')
          _line++;

        bb.append(ch);
      }
      else if ((ch = is.read()) != '!') {
        bb.append('<');

        is.unread();
      }
      else if ((ch = is.read()) != '-') {
        bb.append('<');
        bb.append('!');

        is.unread();
      }
      else if ((ch = is.read()) != '-') {
        bb.append('<');
        bb.append('!');
        bb.append('-');

        is.unread();
      }
      else if ((ch = is.read()) != '#') {
        bb.append('<');
        bb.append('!');
        bb.append('-');
        bb.append('-');

        is.unread();
      }
      else {
        if (bb.getLength() > 0) {
          TextStatement text;

          text = new TextStatement(bb.getBuffer(), 0, bb.getLength());

          statements.add(text);
          bb.clear();
        }

        Statement stmt = parseCommand(is);

        statements.add(stmt);
        
        if (stmt instanceof IfStatement) {
          parseIf(is, (IfStatement) stmt);
        }
        
        if (stmt instanceof ElifStatement) {
          return;
        }
        else if (stmt instanceof ElseStatement) {
          return;
        }
        else if (stmt instanceof EndifStatement) {
          return;
        }
      }
    }

    if (bb.getLength() > 0) {
      statements.add(new TextStatement(bb.getBuffer(), 0, bb.getLength()));
      bb.clear();
    }
  }

  private void parseIf(ReadStream is, IfStatement ifStmt)
    throws IOException
  {
    ArrayList<Statement> trueBlock = new ArrayList<Statement>();

    parse(is, trueBlock);
    int size = trueBlock.size();

    if (size > 0 && trueBlock.get(size - 1) instanceof ElifStatement) {
      ElifStatement elifBlock = (ElifStatement) trueBlock.get(size - 1);
      trueBlock.remove(size - 1);

      ifStmt.setFalseBlock(elifBlock);
    }
    else if (size > 0 && trueBlock.get(size - 1) instanceof ElseStatement) {
      ArrayList<Statement> falseBlock = new ArrayList<Statement>();
      
      parse(is, falseBlock);

      ifStmt.setFalseBlock(new BlockStatement(falseBlock));
    }
    
    ifStmt.setTrueBlock(new BlockStatement(trueBlock));
  }

  private Statement parseCommand(ReadStream is)
    throws IOException
  {
    StringBuilder sb = new StringBuilder();

    int ch;

    while (Character.isLetterOrDigit((ch = is.read()))) {
      sb.append((char) ch);
    }

    String cmd = sb.toString();

    HashMap<String,String> attr = parseAttributes(is);

    if ((ch = is.read()) != '-') {
    }
    else if ((ch = is.read()) != '-') {
    }
    else if ((ch = is.read()) != '>') {
    }

    Statement statement = _factory.createStatement(cmd, attr, is.getPath());

    if (statement == null)
      statement = new ErrorStatement("['" + cmd + "' is an unknown command.]");

    return statement;
  }

  private HashMap<String,String> parseAttributes(ReadStream is)
    throws IOException
  {
    HashMap<String,String> attr = new HashMap<String,String>();

    while (true) {
      int ch;

      while (Character.isWhitespace((ch = is.read()))) {
      }

      StringBuilder key = new StringBuilder();

      for (; Character.isLetterOrDigit(ch); ch = is.read()) {
        key.append((char) ch);
      }

      for (; Character.isWhitespace(ch); ch = is.read()) {
      }

      if (ch != '=')
        return attr;

      for (ch = is.read(); Character.isWhitespace(ch); ch = is.read()) {
      }

      StringBuilder value = new StringBuilder();

      if (ch == '\'' || ch == '"') {
        int end = ch;

        for (ch = is.read(); ch > 0 && ch != end; ch = is.read()) {
          if (ch == '\\') {
            ch = is.read();

            if (ch == '\'' || ch == '\"')
              value.append((char) ch);
            else {
              value.append('\\');
              is.unread();
            }
          }
          else
            value.append((char) ch);
        }
      }
      else {
        for (; ch > 0 && ! Character.isWhitespace(ch); ch = is.read()) {
          value.append((char) ch);
        }
      }

      attr.put(key.toString(), value.toString());
    }
  }
}
