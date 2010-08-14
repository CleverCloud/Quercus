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

package javax.xml.stream.util;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 *  Wrapper around an XMLEventReader
 */
public class EventReaderDelegate implements XMLEventReader {

  private XMLEventReader _parent;

  public EventReaderDelegate()
  {
    this(null);
  }

  public EventReaderDelegate(XMLEventReader reader)
  {
    _parent = reader;
  }

  public void close() throws XMLStreamException
  {
    _parent.close();
  }

  public String getElementText() throws XMLStreamException
  {
    return _parent.getElementText();
  }

  public XMLEventReader getParent()
  {
    return _parent;
  }

  public Object getProperty(String name) throws IllegalArgumentException
  {
    return _parent.getProperty(name);
  }

  public boolean hasNext()
  {
    return _parent.hasNext();
  }

  public Object next()
  {
    return _parent.next();
  }

  public XMLEvent nextEvent() throws XMLStreamException
  {
    return _parent.nextEvent();
  }

  public XMLEvent nextTag() throws XMLStreamException
  {
    return _parent.nextTag();
  }

  public XMLEvent peek() throws XMLStreamException
  {
    return _parent.peek();
  }

  public void remove()
  {
    _parent.remove();
  }

  public void setParent(XMLEventReader reader)
  {
    _parent = reader;
  }

}

