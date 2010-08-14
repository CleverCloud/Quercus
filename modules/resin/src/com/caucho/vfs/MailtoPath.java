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

package com.caucho.vfs;

import com.caucho.util.CharBuffer;
import com.caucho.util.StringCharCursor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The mailto: scheme sends mail using the SMTP protocol.
 * Attributes set headers.  Headers can be set as long as no data
 * has been flushed.
 *
 * <code><pre>
 * WriteStream os = Vfs.openWrite("mailto:nobody@foo.com");
 * os.setAttribute("subject", "Reminder message");
 *
 * os.println("This is just a simple reminder.");
 * os.close();
 * </pre></code>
 *
 * <p>The attributes set SMTP headers:
 * <ul>
 * <li>subject - message subject
 * <li>to - recipient
 * <li>cc - copy list
 * <li>bcc - blind copy list
 * <li><em>X-foo</em> - user-specified header
 * </ul>
 *
 * <p>You can also set attributes in the URL as query parameters.
 *
 * <code><pre>
 * Vfs.openWrite("mailto:nobody@foo.com?subject=dinner");
 * </pre></code>
 */
public class MailtoPath extends Path {
  protected String url;
  private ArrayList<Recipient> _to;
  private ArrayList cc;
  private ArrayList bcc;
  private HashMap<String,Object> _attributes;

  MailtoPath(MailtoPath parent, String path,
             ArrayList<Recipient> to,
             HashMap<String,Object> attr)
  {
    super(parent);

    this.url = path;
    _to = to;
    _attributes = attr;
  }

  /**
   * Parse the scheme for the recipient and the attributes.
   */
  protected Path schemeWalk(String userPath, Map<String,Object> attributes,
                            String uri, int offset)
  {
    StringCharCursor cursor = new StringCharCursor(uri, offset);
    
    ArrayList<Recipient> to = parseAddressList(cursor);
    HashMap<String,Object> attr = new HashMap<String,Object>();

    CharBuffer buf = new CharBuffer();
    if (cursor.current() == '?') {
      char ch = cursor.next();
      while (isUserChar(ch)) {
        buf.clear();
        for (; isUserChar(ch); ch = cursor.next())
          buf.append(ch);
        String key = buf.toString();

        if (ch != '=')
          throw new RuntimeException("broken attribute at: " + ch);
        buf.clear();
        for (ch = cursor.next();
             ch != cursor.DONE && ch != '&';
             ch = cursor.next())
          buf.append(ch);

        attr.put(key, buf.toString());

        while (ch == '&' || ch == ' ' || ch == '\t')
          ch = cursor.next();
      }
    }

    return new MailtoPath(this, userPath, to, attr);
  }

  /**
   * Parses the address list from the URL.
   *
   * @param cursor parse cursor for the URL
   * @return a list of recipient addresses
   */
  static ArrayList<Recipient> parseAddressList(StringCharCursor cursor)
  {
    ArrayList<Recipient> to = new ArrayList<Recipient>();

    char ch = cursor.current();
    CharBuffer buf = new CharBuffer();

    while (Character.isWhitespace(ch))
      ch = cursor.next();
    
    while (isUserChar(ch)) {
      buf.clear();
      for (; isUserChar(ch); ch = cursor.next())
        buf.append(ch);

      Recipient rcpt = new Recipient();
      to.add(rcpt);
      rcpt.user = buf.toString();

      if (ch == '@') {
        ch = cursor.next();
        if (! isUserChar(ch))
          throw new RuntimeException("bad url");

        buf.clear();
        for (; isUserChar(ch); ch = cursor.next())
          buf.append(ch);

        rcpt.host = buf.toString();
      }

      while (ch == ',' || ch == ' ' || ch == '\t' ||
             ch == '\n' || ch == '\r') {
        ch = cursor.next();
      }
    }

    return to;
  }

  /**
   * Returns true if the char is a valid recipient character.
   */
  private static boolean isUserChar(int ch)
  {
    switch (ch) {
    case '.': case '-': case '_': case '!': case '$':
    case '~': case '^': case '*': case '/': case '+':
      return true;

    default:
      return (ch >= 'a' && ch <= 'z' ||
              ch >= 'A' && ch <= 'Z' ||
              ch >= '0' && ch <= '9');
    }
  }

  /**
   * The URL looks like "mailto:user@host.com"
   */
  public String getURL()
  {
    return getPath();
  }

  /**
   * The scheme is "mailto:"
   */
  public String getScheme()
  {
    return "mailto";
  }

  /**
   * The path looks like "mailto:user@host.com"
   */
  public String getPath()
  {
    return "mailto:" + url;
  }

  /**
   * Gets the value of the RFC822 message headers.
   */
  public Object getAttribute(String name)
  {
    return _attributes.get(name);
  }

  /**
   * Implementation to open a WriteStream.
   */
  public StreamImpl openWriteImpl()
    throws IOException
  {
    return new SmtpStream(_to, _attributes);
  }

  static class Recipient {
    String user;
    String host;
  }
}
