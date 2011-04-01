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
* @author Emil Ong
*/

package com.caucho.xml.stream.events;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Comment;
import java.io.IOException;
import java.io.Writer;

public class CommentImpl extends XMLEventImpl implements Comment {
  private final String _text;

  public CommentImpl(String text)
  {
    _text = text;
  }

  public String getText()
  {
    return _text;
  }

  public int getEventType()
  {
    return COMMENT;
  }

  public void writeAsEncodedUnicode(Writer writer) 
    throws XMLStreamException
  {
    try {
      writer.write("<!--" + _text + "-->");
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public boolean equals(Object o) 
  {
    if (! (o instanceof Comment))
      return false;
    if (o == null)
      return false;
    if (this == o)
      return true;

    Comment comment = (Comment) o;

    return getText().equals(comment.getText());
  }
}

