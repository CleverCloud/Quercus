/*
 * Copyright (c) 1998-2001 Caucho Technology -- all rights reserved
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

package com.caucho.web.webmail;

import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MboxArchive {
  private Path path;

  private ArrayList threads;
  private ArrayList messages;

  public MboxArchive(Path path)
    throws IOException
  {
    this.path = path;
  }

  public ArrayList getThreads()
  {
    return threads;
  }

  public ArrayList getMessages()
  {
    return messages;
  }

  public void analyzeMessages()
    throws IOException
  {
    threads = new ArrayList();
    messages = new ArrayList();

    int count = 0;
    HashMap map = new HashMap();
    HashMap subjectMap = new HashMap();

    ReadStream rawIs = path.openRead();
    MboxStream mboxStream = new MboxStream(rawIs);
    ReadStream message;

    while ((message = mboxStream.openRead()) != null) {
      MboxMessage msg = new MboxMessage(count++);

      String id = (String) message.getAttribute("Message-Id");

      id = normalizeMessageId(id);
      
      msg.setMessageId(id);
      
      msg.setFrom((String) message.getAttribute("From"));
      String subject = (String) message.getAttribute("Subject");
      msg.setSubject(subject);

      messages.add(msg);

      String inReplyTo = (String) message.getAttribute("In-Reply-To");

      inReplyTo = normalizeMessageId(inReplyTo);

      MboxMessage parent = null;
      if (inReplyTo != null)
        parent = (MboxMessage) map.get(inReplyTo);

      if (parent == null)
        parent = getParentBySubject(subjectMap, subject);

      if (parent != null)
        msg.setParent(parent);
      else
        threads.add(msg);
          
      map.put(id, msg);
      setParentBySubject(subjectMap, subject, msg);
    }

    rawIs.close();
  }

  private void generateThread(WriteStream os, MboxMessage threadHeader)
  {
    
  }

  private String normalizeMessageId(String id)
  {
    if (id == null)
      return id;

    int start = id.indexOf(id, '<');
    if (start < 0)
      return id;
    
    int end = id.indexOf(id, '>');
    if (end < start)
      return id;

    return id.substring(start, end);
  }

  private MboxMessage getParentBySubject(Map subjectMap, String subject)
  {
    if (subject == null || subject.equals(""))
      return null;

    MboxMessage message = (MboxMessage) subjectMap.get(subject);
    if (message != null)
      return message;

    while (subject.length() > 4) {
      String prefix = subject.substring(0, 4).toLowerCase();
      
      if (! prefix.equals("re: ") && ! prefix.equals("aw: "))
        return null;

      subject = subject.substring(4);

      message = (MboxMessage) subjectMap.get(subject);
      if (message != null)
        return message;
    }

    return null;
  }

  private void setParentBySubject(Map subjectMap, String subject,
                                  MboxMessage message)
  {
    if (subject == null || subject.equals(""))
      return;

    while (subject.length() > 4) {
      String prefix = subject.substring(0, 4).toLowerCase();
      
      if (! prefix.equals("re: ") && ! prefix.equals("aw: ")) {
        subjectMap.put(subject, message);
        return;
      }

      subject = subject.substring(4);
    }

    if (subject == null || subject.equals(""))
      return;

    subjectMap.put(subject, message);
  }
}
