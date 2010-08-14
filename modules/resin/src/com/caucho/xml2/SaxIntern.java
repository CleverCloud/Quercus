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

package com.caucho.xml2;

import javax.xml.namespace.QName;

/**
 * Interning names
 */
public class SaxIntern {
  private static final int SIZE = 203;
  
  private final Entry []_entries = new Entry[SIZE];

  private final NamespaceContextImpl _namespaceContext;

  SaxIntern(NamespaceContextImpl namespaceContext)
  {
    _namespaceContext = namespaceContext;
  }

  Entry add(char []buffer, int offset, int length, int colon,
            boolean isAttribute)
  {
    int hash = 0;

    for (int i = length - 1; i >= 0; i--) {
      hash = 37 * hash + buffer[offset + i];
    }

    int bucket = (hash & 0x7fffffff) % SIZE;

    Entry entry;
    
    for (entry = _entries[bucket];
         entry != null;
         entry = entry._next) {
      if (entry.match(buffer, offset, length, isAttribute))
        return entry;
    }

    entry = new Entry(_entries[bucket],
                      buffer, offset, length,
                      colon,
                      isAttribute);
    _entries[bucket] = entry;

    return entry;
  }

  final class Entry {
    final Entry _next;
    
    final char []_buf;
    final boolean _isAttribute;

    final String _prefix;
    final String _localName;

    final boolean _isXmlns;
    
    String _name;

    NamespaceBinding _namespace;
    int _version;
    QName _qName;

    Entry(Entry next,
          char []buf, int offset, int length,
          int colon,
          boolean isAttribute)
    {
      _next = next;
      
      _buf = new char[length];
      System.arraycopy(buf, offset, _buf, 0, length);

      _isAttribute = isAttribute;

      if (colon > offset) {
        _prefix = new String(buf, offset, colon - offset);
        _localName = new String(buf, colon + 1, length - colon - 1);

        _isXmlns = isAttribute && colon == 5 && "xmlns".equals(_prefix);
      }
      else {
        _prefix = null;
        _localName = new String(buf, 0, length);

        _isXmlns = isAttribute && length == 5 && "xmlns".equals(_localName);
      }

      if (_isAttribute)
        _namespace = _namespaceContext.getAttributeNamespace(_prefix);
      else
        _namespace = _namespaceContext.getElementNamespace(_prefix);

      fillQName();
    }

    public final boolean match(char []buf, int offset, int length,
                               boolean isAttribute)
    {
      if (length != _buf.length || _isAttribute != isAttribute)
        return false;

      char []entryBuf = _buf;
      
      for (length--; length >= 0; length--) {
        if (entryBuf[length] != buf[offset + length])
          return false;
      }

      return true;
    }

    String getName()
    {
      if (_name == null)
        _name = new String(_buf, 0, _buf.length);

      return _name;
    }

    String getLocalName()
    {
      return _localName;
    }

    String getPrefix()
    {
      return _prefix;
    }

    String getUri()
    {
      return _namespace.getUri();
    }

    QName getQName()
    {
      if (_version != _namespace.getVersion())
        fillQName();
      
      return _qName;
    }

    boolean isXmlns()
    {
      return _isXmlns;
    }

    private void fillQName()
    {
      _version = _namespace.getVersion();

      String prefix = _prefix;

      if (prefix == null)
        prefix = "";

      _qName = new QName(_namespace.getUri(), _localName, prefix);
    }
  }
}
