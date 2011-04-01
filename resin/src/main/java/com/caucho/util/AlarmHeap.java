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

package com.caucho.util;

/**
 * The alarm class provides a lightweight event scheduler.  This allows
 * an objects to schedule a timeout without creating a new thread.
 *
 * <p>A separate thread periodically tests the queue for alarms ready.
 */
public class AlarmHeap {
  private final Object _queueLock = new Object();

  private Alarm []_heap = new Alarm[4096];
  private int _heapTop;

  /**
   * Create a new wakeup alarm with a designated listener as a callback.
   * The alarm is not scheduled.
   */
  public AlarmHeap()
  {
  }

  /**
   * Returns the next alarm ready to run
   */
  public Alarm extractAlarm(long now)
  {
    synchronized (_queueLock) {
      // #3548 - getCurrentTime for consistency

      Alarm []heap = _heap;

      Alarm alarm = heap[1];

      if (alarm == null)
        return null;
      else if (now < alarm.getWakeTime())
        return null;

      dequeueImpl(alarm);

      return alarm;
    }
  }

  /**
   * Returns the next alarm ready to run
   */
  long nextAlarmTime()
  {
    Alarm []heap = _heap;

    Alarm alarm = heap[1];

    if (alarm != null)
      return alarm.getWakeTime();
    else
      return -1;
  }

  /**
   * Queue the alarm for wakeup.
   *
   * @param delta time in milliseconds to wake
   */
  public boolean queueAt(Alarm alarm, long wakeTime)
  {
    synchronized (_queueLock) {
      if (alarm.getHeapIndex() > 0)
        dequeueImpl(alarm);

      // #3548 - getCurrentTime for consistency
      alarm.setWakeTime(wakeTime);

      return insertImpl(alarm);
    }
  }

  /**
   * Adds the alarm item.  Must be called from within the heap lock.
   */
  private boolean insertImpl(Alarm alarm)
  {
    if (alarm.getHeapIndex() != 0)
      throw new IllegalStateException();

    // resize if necessary
    if (_heap.length <= _heapTop + 2) {
      Alarm []newHeap = new Alarm[2 * _heap.length];
      System.arraycopy(_heap, 0, newHeap, 0, _heap.length);
      _heap = newHeap;
    }

    Alarm[]heap = _heap;

    int i = ++_heapTop;
    int parent = 0;
    Alarm item = null;
    long wakeTime = alarm.getWakeTime();

    while (i > 1 && wakeTime < (item = heap[parent = (i >> 1)]).getWakeTime()) {
      heap[i] = item;
      item.setHeapIndex(i);
      i = parent;
    }

    heap[i] = alarm;
    alarm.setHeapIndex(i);

    if (_heapTop < i)
      throw new IllegalStateException("i=" + i + " top=" + _heapTop);
    if (i < 1)
      throw new IllegalStateException("i=" + i);
    
    return (i == 1);
  }

  public void dequeue(Alarm alarm)
  {
    synchronized (_queueLock) {
      if (alarm.getHeapIndex() < 0)
        return;
      
      dequeueImpl(alarm);
    }
  }

  /**
   * Removes the alarm item.  Must be called from within the heap lock.
   */
  private void dequeueImpl(Alarm item)
  {
    int i = item.getHeapIndex();

    if (i < 1)
      return;

    if (_heapTop < i)
      throw new IllegalStateException("bad heap: " + _heapTop + " index:" + i);

    Alarm []heap = _heap;

    if (_heapTop < 1)
      throw new IllegalStateException();

    int size = _heapTop--;

    heap[i] = heap[size];
    heap[i].setHeapIndex(i);
    heap[size] = null;

    item.setHeapIndex(0);

    if (size == i)
      return;

    if (item.getWakeTime() < heap[i].getWakeTime()) {
      while (i < size) {
        item = heap[i];

        int minIndex = i;
        long minWakeTime = item.getWakeTime();

        int left = i << 1;
        if (left < size && heap[left].getWakeTime() < minWakeTime) {
          minIndex = left;
          minWakeTime = heap[left].getWakeTime();
        }

        int right = left + 1;
        if (right < size && heap[right].getWakeTime() < minWakeTime)
          minIndex = right;

        if (i == minIndex)
          return;

        heap[i] = heap[minIndex];
        heap[i].setHeapIndex(i);
        heap[minIndex] = item;
        item.setHeapIndex(minIndex);

        i = minIndex;
      }
    }
    else {
      int parent;
      Alarm alarm;
      item = heap[i];
      long wakeTime = item.getWakeTime();

      while (i > 1 && wakeTime < (alarm = heap[parent = (i >> 1)]).getWakeTime()) {
        heap[i] = alarm;
        alarm.setHeapIndex(i);
        i = parent;
      }

      heap[i] = item;
      item.setHeapIndex(i);
    }
  }
  
  public Alarm []toArray()
  {
    int heapTop = _heapTop;
    Alarm []heap = _heap;
    
    Alarm []array = new Alarm[heapTop + 1];
    
    for (int i = 0; i <= heapTop; i++) {
      array[i] = heap[i];
    }
    
    return array;
  }

  // test

  void testClear()
  {
    for (; _heapTop > 0; _heapTop--) {
      Alarm alarm = _heap[_heapTop];
      alarm.setHeapIndex(0);
      _heap[_heapTop] = null;
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName();
  }
}
