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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.util;

/**
 * Cons-cell slist of objects
 */
public class Slist {
  public Slist next;
  public Object first;

  public Slist rest() { return next; }
  public Object value() { return first; }
  /**
   * Copies a slist
   */
  public static Slist copy(Slist slist)
  {
    if (slist == null)
      return null;

    Slist head = new Slist(null, slist.first);
    Slist ptr = head;
    for (slist = slist.next; slist != null; slist = slist.next) {
      ptr.next = new Slist(null, slist.first);
      ptr = ptr.next;
    }

    return head;
  }

  /**
   * Destructively reverse
   */
  public static Slist reverse(Slist slist)
  {
    if (slist == null)
      return null;

    Slist prev = null;
    Slist next = slist.next;
    while (next != null) {
      slist.next = prev;

      prev = slist;
      slist = next;
      next = next.next;
    }

    slist.next = prev;

    return slist;
  }

  /**
   * Destructively append
   */
  public static Slist append(Slist a, Slist b)
  {
    if (a == null)
      return b;

    Slist ptr = a;
    for (; ptr.next != null; ptr = ptr.next) {
    }

    ptr.next = b;

    return a;
  }

  /**
   * Remove if ==
   */
  public static Slist remove(Slist slist, Object object)
  {
    if (slist == null)
      return null;
    else if (slist.first == object)
      return slist.next;
      
    for (Slist ptr = slist; ptr.next != null; ptr = ptr.next) {
      if (ptr.next.first == object) {
        ptr.next = ptr.next.next;
        break;
      }
    }

    return slist;
  }

  /**
   * Create a new cons-cell
   */
  public Slist(Slist next, Object first)
  {    
    this.next = next;
    this.first = first;
  }
}
