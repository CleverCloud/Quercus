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

package com.caucho.bayeux;

import java.util.*;

class ChannelTree {
  private final String _segment;

  private final HashMap<String,ChannelTree> _children = 
    new HashMap<String,ChannelTree>();

  private final ArrayList<BayeuxClient> _subscribers = 
    new ArrayList<BayeuxClient>();

  public ChannelTree()
  {
    this("");
  }

  private ChannelTree(String segment)
  {
    _segment = segment;
  }

  /**
   * Add a subscriber to this tree.  Intended only to be called on the 
   * root of the tree. (i.e. the tree with segment "")
   **/
  public boolean subscribe(String channel, BayeuxClient subscriber)
  {
    String[] segments = channel.split("\\/");
    return subscribe(segments, 1, subscriber);
  }

  protected boolean subscribe(String[] segments, int i, BayeuxClient subscriber)
  {
    if (segments.length >= i) {
      synchronized(_subscribers) {
        _subscribers.add(subscriber);
      }
      return true;
    }

    String segment = segments[i];

    synchronized(_children) {
      if ("*".equals(segments[i])) {
        // "*" must be a trailing wildcard
        if (i != segments.length - 1)
          return false;

        for (ChannelTree child : _children.values())
          child.subscribe(segments, i + 1, subscriber);
      }
      else if ("**".equals(segments[i])) {
        // "**" must be a trailing wildcard
        if (i != segments.length - 1)
          return false;

        // minor hack here: not incrementing will force 
        // propagation through all subchildren
        for (ChannelTree child : _children.values())
          child.subscribe(segments, i, subscriber);
      }
      else {
        ChannelTree child = _children.get(segment);

        if (child == null) {
          child = new ChannelTree(segment);
          _children.put(segment, child);
        }

        child.subscribe(segments, i + 1, subscriber);
      }
    }

    return true;
  }

  /**
   * Publish a message to this tree.  Intended only to be called on the 
   * root of the tree. (i.e. the tree with segment "")
   **/
  public boolean publish(String channel, Object object)
  {
    String[] segments = channel.split("\\/");
    return publish(segments, 1, object);
  }

  protected boolean publish(String[] segments, int i, Object object)
  {
    if (segments.length >= i) {
      synchronized(_subscribers) {
        for (int j = 0; j < _subscribers.size(); j++)
          _subscribers.get(j).publish(object);
      }

      return true;
    }

    String segment = segments[i];

    synchronized(_children) {
      if ("*".equals(segments[i])) {
        // "*" must be a trailing wildcard
        if (i != segments.length - 1)
          return false;

        for (ChannelTree child : _children.values())
          child.publish(segments, i + 1, object);
      }
      else if ("**".equals(segments[i])) {
        // "**" must be a trailing wildcard
        if (i != segments.length - 1)
          return false;

        // minor hack here: not incrementing will force 
        // propagation through all subchildren
        for (ChannelTree child : _children.values())
          child.publish(segments, i, object);
      }
      else {
        ChannelTree child = null;

        child = _children.get(segment);

        // no child means nobody to publish to
        if (child == null)
          return false;

        child.publish(segments, i + 1, object);
      }
    }

    return true;
  }
}

