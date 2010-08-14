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

package com.caucho.loader.ivy;

import java.util.*;

import com.caucho.config.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

/**
 * Pattern used in ivy
 */
public class IvyPattern {
  private static final L10N L = new L10N(IvyPattern.class);
  
  private String _pattern;
  private ArrayList<Segment> _segments = new ArrayList<Segment>();

  public IvyPattern(String pattern)
  {
    _pattern = pattern;

    parse(pattern);
  }

  public String resolve(Map<String,String> props)
  {
    StringBuilder sb = new StringBuilder();

    for (Segment segment : _segments) {
      segment.resolve(sb, props);
    }

    return sb.toString();
  }

  public String resolveRevisionPath(Map<String,String> props)
  {
    StringBuilder sb = new StringBuilder();

    for (Segment segment : _segments) {
      if (segment instanceof VarSegment
          && "revision".equals(((VarSegment) segment).getVar())) {
        sb.append("[revision]");
      }
      else
        segment.resolve(sb, props);
    }

    return sb.toString();
  }

  private void parse(String pattern)
  {
    int len = pattern.length();

    int i = 0;

    while (i < len) {
      int head = pattern.indexOf('[', i);

      if (head < 0) {
        _segments.add(new TextSegment(pattern.substring(i)));
        return;
      }

      int tail = pattern.indexOf(']', head);
      if (tail < 0) {
        throw new ConfigException(L.l("'{0}' is an invalid ivy pattern",
                                      pattern));
      }

      _segments.add(new TextSegment(pattern.substring(i, head)));
      _segments.add(new VarSegment(pattern.substring(head + 1, tail)));

      i = tail + 1;
    }
  }

  @Override
    public String toString()
  {
    return getClass().getSimpleName() + "[" + _pattern + "]";
  }

  abstract static class Segment {
    abstract public void resolve(StringBuilder sb, Map<String,String> props);
  }

  static class TextSegment extends Segment {
    private final String _text;

    TextSegment(String text)
    {
      _text = text;
    }
    
    public void resolve(StringBuilder sb, Map<String,String> props)
    {
      sb.append(_text);
    }
  }

  static class VarSegment extends Segment {
    private final String _var;

    VarSegment(String var)
    {
      _var = var;
    }

    String getVar()
    {
      return _var;
    }
    
    public void resolve(StringBuilder sb, Map<String,String> props)
    {
      sb.append(props.get(_var));
    }
  }
}
