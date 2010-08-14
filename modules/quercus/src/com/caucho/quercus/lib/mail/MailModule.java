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

package com.caucho.quercus.lib.mail;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PHP functions implemented from the mail module
 */
public class MailModule extends AbstractQuercusModule {
  private static final Logger log =
    Logger.getLogger(MailModule.class.getName());

  private static final L10N L = new L10N(MailModule.class);

  /**
   * Send mail using JavaMail.
   */
  public static boolean mail(Env env,
                             String to,
                             String subject,
                             StringValue message,
                             @Optional String additionalHeaders,
                             @Optional String additionalParameters)
  {
    Transport smtp = null;

    try {
      HashMap<String,String> headers = splitHeaders(additionalHeaders);

      if (to == null || to.equals(""))
        to = headers.get("to");
      
      Properties props = new Properties();
      
      StringValue host = env.getIni("SMTP");
      if (host != null && ! host.toString().equals(""))
        props.put("mail.smtp.host", host.toString());
      else if (System.getProperty("mail.smtp.host") != null)
        props.put("mail.smtp.host", System.getProperty("mail.smtp.host"));
      
      StringValue port = env.getIni("smtp_port");
      if (port != null && ! port.toString().equals(""))
        props.put("mail.smtp.port", port.toString());
      else if (System.getProperty("mail.smtp.port") != null)
        props.put("mail.smtp.port", System.getProperty("mail.smtp.port"));

      if (System.getProperty("mail.smtp.class") != null)
        props.put("mail.smtp.class", System.getProperty("mail.smtp.class"));

      StringValue user = null;

      if (headers.get("from") != null)
        user = env.createString(headers.get("from"));

      if (user == null)
        user = env.getIni("sendmail_from");
      
      if (user != null && ! user.toString().equals("")) {
        String userString = user.toString();

        props.put("mail.from", userString);
      }
      else if (System.getProperty("mail.from") != null)
        props.put("mail.from", System.getProperty("mail.from"));
      else {
        try {
          InetAddress addr = InetAddress.getLocalHost();
          
          String email = (System.getProperty("user.name")
                         + "@" + addr.getHostName());
          
          
          int index = email.indexOf('@');
          
          // for certain windows smtp servers
          if (email.indexOf('.', index) < 0)
            email += ".com";

          props.put("mail.from", email);
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);
        }
      }

      String username = env.getIniString("smtp_username");
      String password = env.getIniString("smtp_password");

      if (password != null && ! "".equals(password))
        props.put("mail.smtp.auth", "true");

      Session mailSession = Session.getInstance(props, null);
      smtp = mailSession.getTransport("smtp");

      QuercusMimeMessage msg = new QuercusMimeMessage(mailSession);
      
      if (subject == null)
        subject = "";
      
      msg.setSubject(subject);
      msg.setContent(message.toString(), "text/plain");

      ArrayList<Address> addrList = new ArrayList<Address>();
      
      if (to != null && to.length() > 0)
        addRecipients(msg, Message.RecipientType.TO, to, addrList);

      if (headers != null)
        addHeaders(msg, headers, addrList);

      Address []from = msg.getFrom();
      if (from == null || from.length == 0) {
        log.fine(L.l(
          "mail 'From' not set, setting to Java System property 'user.name'"));
        msg.setFrom();
      }

      msg.saveChanges();

      from = msg.getFrom();
      log.fine(L.l("sending mail, From: {0}, To: {1}", from[0], to));

      if (password != null && ! "".equals(password))
        smtp.connect(username, password);
      else
        smtp.connect();

      Address[] addr;
      
      addr = new Address[addrList.size()];
      addrList.toArray(addr);
      
      smtp.sendMessage(msg, addr);

      log.fine("quercus-mail: sent mail to " + to);

      return true;
    } catch (RuntimeException e) {
      log.log(Level.FINER, e.toString(), e);

      throw e;
    } catch (AuthenticationFailedException e) {
      log.warning(L.l(
        "Quercus[] mail could not send mail to '{0}' "
        + "because authentication failed\n{1}",
        to,
        e.getMessage()));

      log.log(Level.FINE, e.toString(), e);

      env.warning(e.toString());

      return false;
    } catch (MessagingException e) {
      Throwable cause = e;

      log.warning(L.l("Quercus[] mail could not send mail to '{0}'\n{1}",
                      to,
                      cause.getMessage()));

      log.log(Level.FINE, cause.toString(), cause);
      
      env.warning(cause.getMessage());

      return false;
    } catch (Exception e) {
      Throwable cause = e;
      
      log.warning(L.l("Quercus[] mail could not send mail to '{0}'\n{1}",
                  to,
                  cause.getMessage()));

      log.log(Level.FINE, cause.toString(), cause);

      env.warning(cause.toString());

      return false;
    } finally {
      try {
        if (smtp != null)
          smtp.close();
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
  }

  private static void addRecipients(QuercusMimeMessage msg,
                                    Message.RecipientType type,
                                    String to,
                                    ArrayList<Address> addrList)
    throws MessagingException
  {
    String []split = to.split(",");

    for (int i = 0; i < split.length; i++) {
      String addrStr = split[i];
      
      if (addrStr.length() > 0) {
        int openBracket = addrStr.indexOf('<');
        
        // XXX: javamail may be too strict, so we quote spaces in brackets
        if (openBracket >= 0 && ! addrStr.contains("\"")) {
          int closeBracket = addrStr.indexOf('>', openBracket + 1);
        
          if (closeBracket > openBracket) {
            int space = addrStr.indexOf(' ', openBracket + 1);
            
            if (openBracket < space && space < closeBracket) {
              StringBuilder sb = new StringBuilder();
              
              sb.append(addrStr, 0, openBracket + 1);
              sb.append("\"");
              sb.append(addrStr, openBracket + 1, closeBracket);
              sb.append("\"");
              sb.append(addrStr, closeBracket, addrStr.length());
              
              addrStr = sb.toString();
            }
          }
        }
        
        Address addr = new InternetAddress(addrStr);

        addrList.add(addr);
        msg.addRecipient(type, addr);
      }
    }
  }

  private static void addHeaders(QuercusMimeMessage msg,
                                 HashMap<String,String> headerMap,
                                 ArrayList<Address> addrList)
    throws MessagingException
  {
    for (Map.Entry<String,String> entry : headerMap.entrySet()) {
      String name = entry.getKey();
      String value = entry.getValue();

      if ("".equals(value)) {
      }
      else if (name.equalsIgnoreCase("From")) {
        msg.setFrom(new InternetAddress(value));
      }
      else if (name.equalsIgnoreCase("To")) {
        addRecipients(msg, Message.RecipientType.TO, value, addrList);
      }
      else if (name.equalsIgnoreCase("Bcc")) {
        addRecipients(msg, Message.RecipientType.BCC, value, addrList);
      }
      else if (name.equalsIgnoreCase("Cc")) {
        addRecipients(msg, Message.RecipientType.CC, value, addrList);
      }
      else if (name.equalsIgnoreCase("Message-ID")) {
        msg.setMessageID(value);
      }
      else
        msg.addHeader(name, value);
    }
  }

  private static HashMap<String,String> splitHeaders(String headers)
  {
    HashMap<String,String> headerMap = new HashMap<String,String>();

    if (headers == null)
      return headerMap;
    
    int i = 0;
    int len = headers.length();

    CharBuffer buffer = new CharBuffer();

    while (i < len) {
      char ch;

      for (;
           i < len && Character.isWhitespace(headers.charAt(i));
           i++) {
      }

      if (len <= i)
        return headerMap;

      buffer.clear();

      for (;
           i < len && (! Character.isWhitespace(ch = headers.charAt(i))
                       && ch != ':');
           i++) {
        buffer.append((char) ch);
      }

      for (;
           i < len && ((ch = headers.charAt(i)) == ' '
                       || ch == '\t'
                       || ch == '\f'
                       || ch == ':');
           i++) {
      }

      String name = buffer.toString();
      buffer.clear();

      for (;
           i < len
           && ((ch = headers.charAt(i)) != '\r' && ch != '\n');
           i++) {
        buffer.append((char) ch);
      }

      //
      // check for multi-line values
      //

      for (;
           i < len 
           && ((ch = headers.charAt(i)) == '\r' || ch == '\n');
           i++) {
        buffer.append((char) ch);
      }

      while (i < len 
             && ((ch = headers.charAt(i)) == '\t' || ch == ' ')) {
        for (;
             i < len
             && ((ch = headers.charAt(i)) != '\r' && ch != '\n');
             i++) {
          buffer.append((char) ch);
        }

        for (;
             i < len 
             && ((ch = headers.charAt(i)) == '\r' || ch == '\n');
             i++) {
          buffer.append((char) ch);
        }
      }

      while (buffer.length() > 0
             && ((ch = buffer.getLastChar()) == '\r' || ch == '\n')) {
        buffer.deleteCharAt(buffer.length() - 1);
      }

      String value = buffer.toString();

      if (! "".equals(value))
        headerMap.put(name, value);
    }

    return headerMap;
  }
}

