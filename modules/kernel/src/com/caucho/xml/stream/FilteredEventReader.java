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

package com.caucho.xml.stream;

import java.util.NoSuchElementException;

import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

class FilteredEventReader implements XMLEventReader {
  private XMLEventReader _reader;
  private EventFilter _filter;

  private XMLEvent _current;
  private XMLEvent _next;

  public FilteredEventReader(XMLEventReader reader, EventFilter filter)
  {
    _reader = reader;
    _filter = filter;
  }

  public void close() throws XMLStreamException
  {
    _reader.close();
  }

  public String getElementText() throws XMLStreamException
  {
    return _reader.getElementText();
  }

  public Object getProperty(String name) throws IllegalArgumentException
  {
    return _reader.getProperty(name);
  }

  public boolean hasNext()
  {
    try {
      peek();

      return _next != null;
    } 
    catch (XMLStreamException e) {
      return false;
    }
  }

  public XMLEvent nextEvent() throws XMLStreamException
  {
    if (_next != null) {
      _current = _next;
      _next = null;
    }
    else {
      while (_reader.hasNext()) {
        _current = _reader.nextEvent(); 

        if (_filter.accept(_current))
          break;

        _current = null;
      }
    }

    if (_current == null)
      throw new NoSuchElementException();

    return _current;
  }

  public XMLEvent nextTag() throws XMLStreamException
  {
    if (_next != null) {
      _current = _next;
      _next = null;
    }
    else {
      while (_reader.hasNext()) {
        _current = _reader.nextTag(); 

        if (_filter.accept(_current))
          break;

        _current = null;
      }
    }

    if (_current == null)
      throw new NoSuchElementException();

    return _current;
  }

  public XMLEvent peek() throws XMLStreamException
  {
    if (_next == null) {
      while (_reader.hasNext()) {
        _next = _reader.nextEvent(); 

        if (_filter.accept(_next))
          break;

        _next = null;
      }
    }

    return _next;
  }

  public void remove()
  {
    throw new UnsupportedOperationException();
  }

  public XMLEvent next()
  {
    try {
      return nextEvent();
    }
    catch (XMLStreamException e) {
      return null;
    }
  }
}
