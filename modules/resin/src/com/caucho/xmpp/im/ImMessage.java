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

package com.caucho.xmpp.im;

import com.caucho.xmpp.im.Text;
import java.io.Serializable;
import java.util.*;

/**
 * IM message - RFC 3921
 *
 * <pre>
 * element message{xmlns="jabber:client"} {
 *   attribute from?
 *   &amp; attribute to?
 *   &amp; attribute id?
 *   &amp; attribute type?
 *
 *   &amp; subject*
 *   &amp; body*
 *   &amp; thread?
 *   &amp; other*
 * }
 *
 * element body {
 *   attribute xml:lang?
 *   &amp; string
 * }
 *
 * element subject {
 *   attribute xml:lang?
 *   &amp; string
 * } 
 *
 * element thread {
 *   &amp; string
 * }
 * </pre>
 */
public class ImMessage implements Serializable {
  private String _to;
  private String _from;

  // chat, error, groupchat, headline, normal
  private String _type = "normal";
  
  private Text []_subject;
  private Text []_body;
  private String _thread;

  private Serializable []_extra;

  private ImMessage()
  {
  }

  public ImMessage(String body)
  {
    _type = "chat";
    _body = new Text[] { new Text(body) };
  }

  public ImMessage(String type, String body)
  {
    _type = type;
    _body = new Text[] { new Text(body) };
  }

  public ImMessage(String to, String from, String body)
  {
    _type = "chat";
    _to = to;
    _from = from;
    _body = new Text[] { new Text(body) };
  }

  public ImMessage(String to, String from, String body, Serializable extra)
  {
    _type = "chat";
    _to = to;
    _from = from;
    _body = new Text[] { new Text(body) };
    _extra = new Serializable[] { extra };
  }

  public ImMessage(String to, String from, String body, Serializable []extra)
  {
    _type = "chat";
    _to = to;
    _from = from;
    _body = new Text[] { new Text(body) };
    _extra = extra;
  }

  public ImMessage(String to, String from, String type,
                   Text []subject,
                   Text []body,
                   String thread,
                   Serializable []extra)
  {
    _to = to;
    _from = from;
    _type = type;

    _subject = subject;
    _body = body;
    _thread = thread;

    _extra = extra;
  }

  public String getTo()
  {
    return _to;
  }

  public String getFrom()
  {
    return _from;
  }

  public String getType()
  {
    return _type;
  }

  public Text []getSubjects()
  {
    return _subject;
  }

  public String getSubjectString()
  {
    if (_subject == null || _subject.length == 0)
      return null;
    else
      return _subject[0].getValue();
  }

  public Text []getBodys()
  {
    return _body;
  }

  public String getBodyString()
  {
    if (_body == null || _body.length == 0)
      return null;
    else
      return _body[0].getValue();
  }

  public void setThread(String thread)
  {
    _thread = thread;
  }
 
  public String getThread()
  {
    return _thread;
  }
  
  public Serializable []getExtra()
  {
    return _extra;
  }
  
  public void setExtra(Serializable []extra)
  {
    _extra = extra;
  }
  
  public void setExtraList(ArrayList<Serializable> extraList)
  {
    if (extraList != null && extraList.size() > 0) {
      _extra = new Serializable[extraList.size()];
      extraList.toArray(_extra);
    }
    else
      _extra = null;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append("[");
    sb.append(_type);

    if (_to != null)
      sb.append(",to=").append(_to);
    
    if (_from != null)
      sb.append(",from=").append(_from);
      
    if (_subject != null) {
      for (Text text : _subject) {
        if (text.getLang() != null) {
          sb.append(",subject(").append(text.getLang()).append(")='");
        }
        else
          sb.append(",subject='");

        sb.append(text.getValue());
        sb.append("'");
      }
    }

    if (_body != null && _body.length != 0) {
      for (Text text : _body) {
        if (text.getLang() != null) {
          sb.append(",body(").append(text.getLang()).append(")='");
        }
        else
          sb.append(",body='");

        sb.append(text.getValue());
        sb.append("'");
      }
    }

    if (_thread != null) {
      sb.append(",thread=");
      sb.append(_thread);
    }

    if (_extra != null) {
      for (Serializable extra : _extra)
        sb.append(",extra=").append(extra);
    }
    
    sb.append("]");
    
    return sb.toString();
  }
}
