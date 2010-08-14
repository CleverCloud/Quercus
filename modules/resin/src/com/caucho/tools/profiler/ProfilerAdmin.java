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
 * @author Sam
 */


package com.caucho.tools.profiler;

import com.caucho.jmx.Jmx;
import com.caucho.util.L10N;

import javax.management.ObjectName;
import javax.management.openmbean.TabularData;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProfilerAdmin
  implements ProfilerMBean
{
  private static final L10N L = new L10N(ProfilerAdmin.class);
  private static final Logger log = Logger.getLogger(ProfilerAdmin.class.getName());

  private final ProfilerManager _profilerManager;
  private final ObjectName _objectName;

  ProfilerAdmin(ProfilerManager profilerManager)
  {
    _profilerManager = profilerManager;

    ObjectName objectName;

    try {
      objectName = Jmx.getObjectName("type=Profiler");

      objectName = new ObjectName(objectName.getCanonicalName());

      Jmx.register(this, objectName);
    }
    catch (Throwable e) {
      objectName = null;

      log.log(Level.FINER, e.toString(), e);
    }

    _objectName = objectName;
  }

  public ObjectName getObjectName()
  {
    return _objectName;
  }

  public boolean isEnabled()
  {
    return _profilerManager.isEnabled();
  }

  public void enable()
  {
    _profilerManager.enable();
  }

  public void disable()
  {
    _profilerManager.disable();
  }

  public void reset()
  {
    _profilerManager.reset();
  }

  // Snapshot - jmx ugh.

  public TabularData snapshot()
  {
    return null;
  }

  /**
   *
   * XXX: need to do aggreagateTime calculation
  private static final CompositeType SNAPSHOT_COMPOSITE_TYPE;

  private static final TabularType SNAPSHOT_TABULAR_TYPE;

  private static final String[] SNAPSHOT_COMPOSITE_TYPE_ITEM_NAMES
    = new String[]{L.l("Name"),
                   L.l("Average Time"),
                   L.l("Total Time"),
                   L.l("Invocation Count")};

  private static final String[] SNAPSHOT_COMPOSITE_TYPE_ITEM_DESCRIPTIONS
    = new String[]{L.l("Name"),
                   L.l("Average Time in nanoseconds"),
                   L.l("Total Time in nanoseconds"),
                   L.l("Invocation Count")};

  private static final OpenType[] SNAPSHOT_COMPOSITE_TYPE_ITEM_TYPES
    = new OpenType[]{SimpleType.STRING,
                     SimpleType.LONG,
                     SimpleType.LONG,
                     SimpleType.LONG};

  static {
    try {
      SNAPSHOT_COMPOSITE_TYPE = new CompositeType(
        L.l("Profiler Snapshot Item"),
        L.l(
          "A snapshot of the current profiler statistics for a named profiler point."),
        SNAPSHOT_COMPOSITE_TYPE_ITEM_NAMES,
        SNAPSHOT_COMPOSITE_TYPE_ITEM_DESCRIPTIONS,
        SNAPSHOT_COMPOSITE_TYPE_ITEM_TYPES
      );

      SNAPSHOT_TABULAR_TYPE = new TabularType(
        L.l("Profiler Snapshot"),
        L.l("A snapshot of the current profiler statistics."),
        SNAPSHOT_COMPOSITE_TYPE,
        new String[]{L.l("Name")});
    }
    catch (OpenDataException e) {
      throw new AssertionError(e);
    }
  }

  public TabularData snapshot()
    throws Exception
  {
    ProfilerNodeComparator comparator = new TimeComparator();
    comparator.setDescending(true);

    TabularDataSupport tabularData = new TabularDataSupport(
      SNAPSHOT_TABULAR_TYPE);

    addChildren(tabularData, comparator, null, null);

    return tabularData;
  }

  private void addChildren(TabularData tabularData,
                           ProfilerNodeComparator comparator,
                           ProfilerNode parent,
                           String parentName)
    throws Exception
  {
    for (ProfilerNode child : _profilerManager.getChildProfilerNodes(parent,
                                                                     comparator)) {

      if (log.isLoggable(Level.FINEST))
        log.finest(L.l("jmx snapshot row for {0}", child));

      String childName = parentName == null
                         ? child.getName()
                         : parentName + " --> " + child.getName();

      Object[] compositeDataValues = new Object[]{
        childName,
        child.getAggregateAverageTime(),
        child.getAggregateTime(),
        child.getInvocationCount()
      };

      CompositeDataSupport compositeData = new CompositeDataSupport(
        SNAPSHOT_COMPOSITE_TYPE,
        SNAPSHOT_COMPOSITE_TYPE_ITEM_NAMES,
        compositeDataValues);

      tabularData.put(compositeData);

      addChildren(tabularData, comparator, child, childName);
    }
  }
   */

}
