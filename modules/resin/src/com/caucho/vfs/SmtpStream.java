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

import com.caucho.server.util.CauchoSystem;
import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;
import com.caucho.util.Log;
import com.caucho.util.NullIterator;
import com.caucho.util.QDate;
import com.caucho.util.StringCharCursor;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the SMTP/RFC822 protocol to handle mailto:
 *
 * <p>SmtpStream extends MemoryStream so the results will be buffered
 * until the close().  When the stream is finally closed, it will
 * send the results to the SMTP server.
 */
class SmtpStream extends MemoryStream {
  static final Logger log = Log.open(SmtpStream.class);

  // list of recipients
  ArrayList<MailtoPath.Recipient> _to;
  // list of copied recipients
  ArrayList<MailtoPath.Recipient> _cc;
  // list of blind copied recipients
  ArrayList<MailtoPath.Recipient> _bcc;

  private String _from;
  private String _sender;
  
  private HashMap<String,Object> _attributes;

  // true if it's already been written
  boolean _isClosed;


  /**
   * Creates a new SmtpStream
   *
   * @param to list of recipients
   * @param attributes list of attributes set in the path
   */
  SmtpStream(ArrayList<MailtoPath.Recipient> to,
             HashMap<String,Object> attributes)
    throws IOException
  {
    if (to.size() <= 0)
      throw new IOException("No recipients in mailto");
    
    _to = new ArrayList<MailtoPath.Recipient>();
    for (int i = 0; i < to.size(); i++)
      _to.add(to.get(i));

    // the 'to' and 'cc' attributes need to be converted.
    // so set them as if they were set by setAttribute
    if (attributes != null) {
      Iterator<String> iter = attributes.keySet().iterator();
 
      while (iter.hasNext()) {
         String key = iter.next();
 
         try {
           setAttribute(key, attributes.get(key));
         } catch (IOException e) {
         }
      }
    }
  }

  /**
   * Return the named attribute
   */
  public Object getAttribute(String name)
    throws IOException
  {
    if (_attributes != null)
      return _attributes.get(name.toLowerCase());
    else
      return null;
  }

  public Iterator<String> getAttributeNames()
  {
    if (_attributes != null)
      return _attributes.keySet().iterator();
    else
      return NullIterator.create();
  }

  /**
   * Sets an attribute.  Some attributes, like "date" and "sender" cannot
   * be set.  Any unknown attribute will be treated as a user RFC822
   * header.
   */
  public void setAttribute(String name, Object value)
    throws IOException
  {
    name = name.toLowerCase();
    if (name.equals("date") || 
        name.equals("received") || name.equals("return-path") ||
        name.equals("message-id"))
      throw new IOException("cannot set property `" + name + "'");

    if (name.equals("to")) {
      addTo((String) value);
      return;
    }
    if (name.equals("cc")) {
      addCc((String) value);
      return;
    }
    
    if (name.equals("bcc")) {
      addBcc((String) value);
      return;
    }
    
    if (name.equals("from")) {
      _from = (String) value;
      return;
    }

    if (name.equals("sender")) {
      _sender = (String) value;
      return;
    }

    if (_attributes == null)
      _attributes = new HashMap<String,Object>();

    _attributes.put(name, value);
  }

  /**
   * Add new recipients
   *
   * @param to a list of new recipients
   */
  public void addTo(String to)
    throws IOException
  {
    StringCharCursor cursor = new StringCharCursor(to);

    ArrayList<MailtoPath.Recipient> list = MailtoPath.parseAddressList(cursor);

    for (int i = 0; i < list.size(); i++)
      _to.add(list.get(i));
  }

  /**
   * Add new copied recipients
   *
   * @param to a list of new recipients
   */
  public void addCc(String to)
    throws IOException
  {
    StringCharCursor cursor = new StringCharCursor(to);

    ArrayList<MailtoPath.Recipient> list = MailtoPath.parseAddressList(cursor);

    if (_cc == null)
      _cc = list;
    else {
      for (int i = 0; i < list.size(); i++)
        _cc.add(list.get(i));
    }
  }

  /**
   * Add new blind copied recipients
   *
   * @param to a list of new recipients
   */
  public void addBcc(String to)
    throws IOException
  {
    StringCharCursor cursor = new StringCharCursor(to);

    ArrayList<MailtoPath.Recipient> list = MailtoPath.parseAddressList(cursor);

    if (_bcc == null)
      _bcc = list;
    else {
      for (int i = 0; i < list.size(); i++)
        _bcc.add(list.get(i));
    }
  }

  /**
   * Returns the "from" user
   */
  public String getFrom()
  {
    if (_from != null)
      return _from;
    else
      return null;
    
    // return Registry.getString("/caucho.com/smtp.vfs/sender", null);
  }

  /**
   * Returns the sender
   */
  public String getSender()
  {
    if (_sender != null)
      return _sender;
    else
      return null;
    
    // return Registry.getString("/caucho.com/smtp.vfs/sender", null);
  }

  /**
   * Reads a response from the SMTP server, returning the status code.
   *
   * @param is the input stream to read the response from
   * @param msg CharBuffer holding the server's response
   * @return the status code read from the server
   */
  private int readResponse(InputStream is, CharBuffer msg)
    throws IOException
  {
    int value;

    do {
      msg.clear();
      value = 0;
      int ch;
      if ((ch = is.read()) >= '0' && ch <= '9') {
        for (; ch >= '0' && ch <= '9'; ch = is.read()) {
          msg.append((char) ch);
          value = 10 * value + ch - '0';
        }
      }

      // Multiline responses, e.g. "200-Foo", indicate there will be a
      // following line.  So the value should be zeroed to force another
      // iteration. (fixed by Michael Kolfman)
      if (ch == '-')
        value = 0;

      for (; ch != '\r' && ch != '\n'; ch = is.read())
        msg.append((char) ch);

      if (ch == '\r')
        ch = is.read();
    } while (value == 0);

    if (log.isLoggable(Level.FINE))
      log.fine(msg.toString());

    return value;
  }

  /**
   * Send the recipient list to the server.
   *
   * @param is ReadStream from the server
   * @param os WriteStream to the server
   * @param to list of recipients
   * @param msg CharBuffer to receive the response
   */
  void sendRecipients(ReadStream is, WriteStream os,
                      ArrayList<MailtoPath.Recipient> to, CharBuffer msg)
    throws IOException
  {
    if (to == null)
      return;

    for (int i = 0; i < to.size(); i++) {
      MailtoPath.Recipient rcpt = to.get(i);

      os.print("RCPT TO: ");
      os.print(rcpt.user);
      if (rcpt.host != null) {
        os.print("@");
        os.print(rcpt.host);
      }
      os.print("\r\n");

      if (log.isLoggable(Level.FINE))
        log.fine("RCPT TO: " + rcpt.user + "@" + rcpt.host);

      if (readResponse(is, msg) / 100 != 2)
        throw new IOException("Expected '221' from SMTP: " + msg);
    }
  }

  /**
   * Writes the message body to the server, escaping as necessary.
   *
   * @param os WriteStream to the server
   */
  private void writeMessageBody(WriteStream os)
    throws IOException
  {
    int ch;

    ReadStream is = openReadAndSaveBuffer();

    ch = is.read();
    if (ch < 0) {
      os.print(".\r\n");
      return;
    }

    while (ch >= 0) {
      if (ch == '\n') {
        ch = is.read();
        if (ch == '.') {
          os.print("\r\n..");
          ch = is.read();
        }
        else if (ch <= 0) {
          os.print("\r\n.\r\n");
          return;
        } else {
          os.print("\r\n");
        }
      } else {
        os.write(ch);
        ch = is.read();
      }
    }

    os.print("\r\n.\r\n");
  }

  /**
   * Writes the message and the RFC822 headers to the SMTP server.
   */
  private void writeMessage(WriteStream os)
    throws IOException
  {
    String from = getFrom();
    os.print("From: ");
    if (from != null)
      os.print(from);
    else {
      os.print(CauchoSystem.getUserName());
      os.print("@");
      os.print(CauchoSystem.getLocalHost());
    }
    os.print("\r\n");
    
    String date = QDate.formatLocal(Alarm.getCurrentTime(),
                                    "%a, %d %b %Y %H:%M:%S %z");

    os.print("Date: " + date + "\r\n");
      
    os.print("To: ");
    writeMessageRecipients(os, _to);

    if (_cc != null && _cc.size() > 0) {
      os.print("Cc: ");
      writeMessageRecipients(os, _cc);
    }

    Iterator<String> iter = getAttributeNames();
    while (iter != null && iter.hasNext()) {
      String key = iter.next();
      Object value = getAttribute(key);

      if (value != null) {
        os.print(key);
        os.print(": ");
        os.print(String.valueOf(value));
        os.print("\r\n");
      }
    }

    String sender = getSender();
    if (_from != sender && ! _from.equals(sender)) {
      os.print("Sender: ");

      if (sender != null)
        os.print(sender);
      else {
        os.print(CauchoSystem.getUserName());
        os.print("@");
        os.print(CauchoSystem.getLocalHost());
      }
      os.print("\r\n");
    }

    os.print("\r\n");

    writeMessageBody(os);
  }

  /**
   * Utility to write a list of mail addresses
   */
  private void writeMessageRecipients(WriteStream os,
                                      ArrayList<MailtoPath.Recipient> list)
    throws IOException
  {
    for (int i = 0; i < list.size(); i++) {
      MailtoPath.Recipient rcpt = list.get(i);

      if (i != 0)
        os.print(", ");
      
      os.print(rcpt.user);
      if (rcpt.host != null) {
        os.print("@");
        os.print(rcpt.host);
      }
    }

    os.print("\r\n");
  }

  /**
   * On close, send the mail.
   */
  public void close()
    throws IOException
  {
    if (_isClosed)
      return;

    _isClosed = true;

    String host = System.getProperty("mail.smtp.host");
    if (host == null)
      host = "127.0.0.1";
    
    String portName = System.getProperty("mail.smtp.port");
    
    int port = 25;
    if (portName != null)
      port = Integer.parseInt(portName);

    Socket sock = new Socket(host, port);
    CharBuffer msg = new CharBuffer();
    ReadStream is = null;
    WriteStream os = null;
    try {
      ReadWritePair s = VfsStream.openReadWrite(sock.getInputStream(),
                                                sock.getOutputStream());
      is = s.getReadStream();
      os = s.getWriteStream();

      if (readResponse(is, msg) / 100 != 2)
        throw new IOException("Expected '220' from SMTP");

      os.print("HELO ");
      os.print(CauchoSystem.getLocalHost());
      os.print("\r\n");
      if (readResponse(is, msg) / 100 != 2)
        throw new IOException("Expected '220' from SMTP");

      os.print("MAIL FROM: ");
      String sender = getSender();
      if (sender != null)
        os.print(sender);
      else {
        os.print(CauchoSystem.getUserName());
        os.print("@");
        os.print(CauchoSystem.getLocalHost());
      }
      os.print("\r\n");
      if (readResponse(is, msg) / 100 != 2)
        throw new IOException("Expected '250' from SMTP: " + msg);

      sendRecipients(is, os, _to, msg);
      if (_cc != null)
        sendRecipients(is, os, _cc, msg);
      if (_bcc != null)
        sendRecipients(is, os, _bcc, msg);

      os.print("DATA\r\n");
      if (readResponse(is, msg) / 100 != 3)
        throw new IOException("Expected '354' from SMTP: " + msg);

      writeMessage(os);

      if (readResponse(is, msg) / 100 != 2)
        throw new IOException("Expected '200' from SMTP: " + msg);

      os.print("QUIT\r\n");
      if (readResponse(is, msg) / 100 != 2)
              throw new IOException("Expected '250' from SMTP: " + msg);
    } finally {
      try {
        if (is != null)
          is.close();
        if (os != null)
          os.close();
      } finally {
        sock.close();
      }
      destroy();
    }
  }
}
