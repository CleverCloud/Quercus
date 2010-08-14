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

import java.util.ArrayList;

/*
 * Represents a single mbox message.
 */
public class MboxMessage {
  private int index;

  private String messageId;
  
  private String from;
  private String subject;
  private String dateString;
  private long date;

  private MboxMessage parent;
  private MboxMessage root;

  private ArrayList children;
  private ArrayList descendants;

  public MboxMessage(int index)
  {
    this.index = index;

    this.root = this;
  }

  public int getIndex()
  {
    return index;
  }

  public void setMessageId(String id)
  {
    this.messageId = id;
  }

  public String getMessageId()
  {
    return messageId;
  }

  public void setFrom(String from)
  {
    this.from = from;
  }

  public String getFrom()
  {
    return from;
  }

  public void setSubject(String subject)
  {
    this.subject = subject;
  }

  public String getSubject()
  {
    return subject;
  }

  public void setDateString(String date)
  {
    dateString = date;
  }

  public String getDateString()
  {
    return dateString;
  }

  public void setDate(long date)
  {
    this.date = date;
  }

  public long getDate()
  {
    return date;
  }

  public void setParent(MboxMessage parent)
  {
    this.parent = parent;
    this.root = parent.root;

    parent.addChild(this);
  }

  public void addChild(MboxMessage child)
  {
    if (children == null)
      children = new ArrayList();
    
    children.add(child);
    
    if (root.descendants == null)
      root.descendants = new ArrayList();

    root.descendants.add(child);
  }

  public ArrayList getChildren()
  {
    return children;
  }

  public int getChildSize()
  {
    if (children == null)
      return 0;
    else
      return children.size();
  }

  public MboxMessage getChild(int index)
  {
    return (MboxMessage) children.get(index);
  }

  public ArrayList getDescendants()
  {
    return descendants;
  }
}
