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

import java.util.ArrayList;

/**
 * The exit class is used to automatically clean state.  For example,
 * Resin JavaScript will close open files when the script exits.
 *
 * Here's a sample use:
 * <pre>
 *   boolean isExit = addExit();
 *   try {
 *     ...
 *   } finally {
 *     if (isExit)
 *       exit();
 *   }
 * </pre>
 *
 * <p>Objects register for the exit callback by calling
 * <code>addExit(listener, object)</code>.
 *
 * <p>Exits are intrinsically tied to a thread.
 */
public class Exit {
  static private ThreadLocal<Queue> _waiting = new ThreadLocal<Queue>();

  private Exit() {}

  /**
   * Registers the object and listener.  The listener will be called when
   * the thread exits the scope.
   *
   * <p>If there is no protection scope, i.e. addExit() hasn't been called,
   * then this function does nothing.
   *
   * @param listener the exit handler
   * @param object the object which needs cleanup
   */
  static public void addExit(ExitListener listener, Object object)
  {
    Queue queue = _waiting.get();

    if (queue != null)
      queue.add(listener, object);
  }

  /**
   * Starts a protection scope.  Only the outermost scope is important, so
   * protecting routines must check the return value to see if they are the
   * outermost scope.
   *
   * @return true if this is the outermost scope.
   */
  static public boolean addExit()
  {
    Queue queue = _waiting.get();

    if (queue == null) {
      queue = Queue.allocate();
      _waiting.set(queue);
      return true;
    } else 
      return false;
  }

  /**
   * Calls all registered listeners.
   */
  static public void exit()
  {
    Queue queue = null;

    queue = _waiting.get();
    _waiting.set(null);

    if (queue == null)
      return;
    
    int size = queue._listeners.size();
    for (int i = 0; i < size; i++) {
      ExitListener listener = queue._listeners.get(i);
      Object object = queue._objects.get(i);

      if (listener != null) {
        try {
          listener.handleExit(object);
        } catch (Exception e) {
        }
      }
    }

    queue.free();
  }

  private static class Queue {
    static Queue _freeList;

    Queue _next;
    ArrayList<ExitListener> _listeners = new ArrayList<ExitListener>();
    ArrayList<Object> _objects = new ArrayList<Object>();

    private Queue()
    {
    }

    void add(ExitListener listener, Object object)
    {
      _listeners.add(listener);
      _objects.add(object);
    }

    static Queue allocate()
    {
      if (_freeList == null) 
        return new Queue();

      Queue queue = _freeList;
      _freeList = _freeList._next;

      return queue;
    }

    void free()
    {
      _listeners.clear();
      _objects.clear();
      _next = _freeList;
      _freeList = this;
    }
  }
}
