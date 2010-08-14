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

package com.caucho.mail.smtp;

import com.caucho.server.util.CauchoSystem;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.SocketStream;
import com.caucho.vfs.WriteStream;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resin's SMTP transport implementation.
 */
public class SmtpTransport extends Transport {
  private static final L10N L = new L10N(SmtpTransport.class);
  private static final Logger log
    = Logger.getLogger(SmtpTransport.class.getName());

  private Socket _socket;

  private ReadStream _is;
  private WriteStream _os;
  
  public SmtpTransport(Session session, URLName urlName)
  {
    super(session, urlName);
  }
  /**
   * Connect for the protocol.
   */
  protected boolean protocolConnect(String host,
                                    int port,
                                    String user,
                                    String password)
    throws MessagingException
  {
    if (host == null)
      host = "localhost";

    if (port < 0)
      port = 25;

    // XXX: pooling
    if (_socket != null)
      throw new MessagingException(L.l("Attempted to connect to open connection."));

    try {
      _socket = new Socket(host, port);
      _socket.setSoTimeout(10000);
      SocketStream s = new SocketStream(_socket);
    
      _os = new WriteStream(s);
      _is = new ReadStream(s, _os);

      String line = _is.readLine();
      
      log.fine("smtp connection to " + host + ":" + port + " succeeded");
      log.fine("smtp: " + line);

      _os.print("EHLO " + CauchoSystem.getLocalHost() + "\r\n");
      _os.flush();

      readResponse();

      setConnected(true);
    } catch (IOException e) {
      log.fine("smtp connection to " + host + ":" + port + " failed: " + e);

      log.log(Level.FINER, e.toString(), e);
      
      throw new MessagingException("smtp connection to " + host + ":" + port + " failed.\n" + e);
    }

    return true;
  }

  /**
   * Sends a message to the specified recipients.
   *
   * @param msg the message to send
   * @param addresses the destination addresses
   */
  public void sendMessage(Message msg, Address []addresses)
    throws MessagingException
  {
    if (! isConnected())
      throw new MessagingException("Transport does not have an active connection.");

    if (! (msg instanceof MimeMessage))
      throw new MessagingException("message must be a MimeMessage at '"
                                   + msg.getClass().getName() + "'");

    MimeMessage mimeMsg = (MimeMessage) msg;

    try {
      // XXX: EHLO to resync? or RSET?
      // XXX: FROM

      String []fromList = mimeMsg.getHeader("From");
      String from;
      
      if (fromList == null || fromList.length < 1) {
        // XXX: possible should have a default
        throw new MessagingException("message should have a sender");
      }
      else
        from = fromList[0];
      
      _os.print("MAIL FROM:<" + from + ">\r\n");
      _os.flush();

      if (log.isLoggable(Level.FINER))
        log.finer("mail from:<" + from + ">");

      readResponse();

      for (int i = 0; i < addresses.length; i++) {
        InternetAddress addr = (InternetAddress) addresses[i];

        if (log.isLoggable(Level.FINER))
          log.finer("mail to:<" + addr.getAddress() + ">");

        _os.print("RCPT TO:<" + addr.getAddress() + ">\r\n");
        _os.flush();

        readResponse();
      }

      _os.print("DATA\r\n");
      _os.flush();

      String line = _is.readLine();
      if (! line.startsWith("354 "))
        throw new MessagingException("Data not accepted: " + line);

      mimeMsg.writeTo(new DataFilter(_os));

      _os.print("\r\n.\r\n");
      _os.flush();
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);

      throw new MessagingException(e.toString());
    }
  }

  private int readResponse()
    throws IOException, MessagingException
  {
    while (true) {
      String line = _is.readLine();
      if (line.length() < 4)
        throw new MessagingException(line);

      int status = 0;
      for (int i = 0; i < 3; i++) {
        char ch;

        if ('0' <= (ch = line.charAt(i))  && ch <= '9')
          status = 10 * status + ch - '0';
      }

      if ((status / 100) % 10 != 2)
        throw new MessagingException(line);

      if (line.charAt(3) != '-')
        return status;
    }
  }

  /**
   * Close connection.
   */
  public void close()
    throws MessagingException
  {
    Socket socket = _socket;
    _socket = null;

    WriteStream os = _os;
    _os = null;
    
    setConnected(false);

    try {
      if (os != null) {
        os.print("QUIT\r\n");
        os.flush();
      }
    } catch (IOException e) {
    }

    try {
      if (socket != null)
        socket.close();
    } catch (IOException e) {
    }
  }

  private class DataFilter extends OutputStream {
    private OutputStream _os;
    
    private boolean _isCr;
    private boolean _isLf;

    DataFilter(OutputStream os)
    {
      _os = os;
    }
    
    public void write(int ch)
      throws IOException
    {
      switch (ch) {
      case '\r':
        _isCr = true;
        _isLf = false;
        break;
      case '\n':
        _isLf = _isCr;
        _isCr = false;
        break;
      case '.':
        if (_isLf)
          _os.write('.');
        _isLf = false;
        _isCr = false;
        break;
      default:
        _isLf = false;
        _isCr = false;
        break;
      }

      _os.write(ch);
    }
  }
}
