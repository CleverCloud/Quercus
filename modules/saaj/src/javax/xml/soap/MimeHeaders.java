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

package javax.xml.soap;
import java.util.*;

public class MimeHeaders {
  private final HashMap<String,ArrayList<MimeHeader>> _headers 
    = new HashMap<String,ArrayList<MimeHeader>>();

  public MimeHeaders()
  {
  }

  public void addHeader(String name, String value)
  {
    if (name == null || "".equals(name))
      throw new IllegalArgumentException();

    ArrayList<MimeHeader> list = _headers.get(name);

    if (list == null) {
      list = new ArrayList<MimeHeader>();
      _headers.put(name, list);
    }

    list.add(new MimeHeader(name, value));
  }

  public Iterator getAllHeaders()
  {
    return new BlacklistHeaderIterator();
  }

  public String[] getHeader(String name)
  {
    ArrayList<MimeHeader> list = _headers.get(name);

    if (list == null)
      return new String[0];

    String[] values = new String[list.size()];

    for (int i = 0; i < list.size(); i++)
      values[i] = list.get(i).getValue();

    return values;
  }

  public Iterator getMatchingHeaders(String[] names)
  {
    return new MatchingHeaderIterator(names);
  }

  public Iterator getNonMatchingHeaders(String[] names)
  {
    return new BlacklistHeaderIterator(names);
  }

  public void removeAllHeaders()
  {
    _headers.clear();
  }

  public void removeHeader(String name)
  {
    _headers.remove(name);
  }

  public void setHeader(String name, String value)
  {
    if (name == null || "".equals(name))
      throw new IllegalArgumentException();

    ArrayList<MimeHeader> list = _headers.get(name);

    if (list == null) {
      list = new ArrayList<MimeHeader>();
      _headers.put(name, list);
    }

    if (list.size() > 0) 
      list.set(0, new MimeHeader(name, value));
    else
      list.add(new MimeHeader(name, value));
  }

  private class BlacklistHeaderIterator implements Iterator
  {
    private String[] _blackList;
    private Iterator<Map.Entry<String,ArrayList<MimeHeader>>> _topIterator;
    private Iterator<MimeHeader> _bottomIterator;

    public BlacklistHeaderIterator()
    {
      this(null);
    }

    public BlacklistHeaderIterator(String[] blackList)
    {
      _blackList = blackList;
      _topIterator = _headers.entrySet().iterator();
    }

    private void prepareIterator()
    {
      if (_bottomIterator == null || ! _bottomIterator.hasNext()) {
        _bottomIterator = null;

        while (_topIterator.hasNext()) {
          Map.Entry<String,ArrayList<MimeHeader>> entry = _topIterator.next();

          boolean ok = true;

          if (_blackList != null) {
            for (int i = 0; i < _blackList.length; i++) {
              if (entry.getKey().equals(_blackList[i]))
                ok = false;
            }
          }

          if (ok) {
            _bottomIterator = entry.getValue().iterator();
            break;
          }
        }
      }
    }

    public Object next()
    {
      prepareIterator();

      if (_bottomIterator == null)
        throw new NoSuchElementException();

      return _bottomIterator.next();
    }

    public boolean hasNext()
    {
      prepareIterator();

      if (_bottomIterator == null)
        return false;

      return _bottomIterator.hasNext();
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  private class MatchingHeaderIterator implements Iterator
  {
    private int _name = -1;
    private String[] _names;
    private Iterator<MimeHeader> _iterator;

    public MatchingHeaderIterator(String[] names)
    {
      _names = names;
    }

    private void prepareIterator()
    {
      if (_iterator == null || ! _iterator.hasNext()) {
        _iterator = null;

        for (_name++; _name < _names.length; _name++) {
          ArrayList<MimeHeader> list = _headers.get(_names[_name]);

          if (list != null) {
            _iterator = list.iterator();

            if (_iterator.hasNext())
              break;

            _iterator = null;
          }
        }
      }
    }

    public Object next()
    {
      prepareIterator();

      if (_iterator == null)
        throw new NoSuchElementException();

      return _iterator.next();
    }

    public boolean hasNext()
    {
      prepareIterator();

      if (_iterator == null)
        return false;

      return _iterator.hasNext();
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}

