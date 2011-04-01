/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.env.meter;

import java.util.concurrent.ConcurrentHashMap;

import com.caucho.env.service.AbstractResinService;

public class MeterService extends AbstractResinService {
  private static MeterService _manager = new MeterService();

  private final ConcurrentHashMap<String,AbstractMeter> _meterMap
    = new ConcurrentHashMap<String,AbstractMeter>();

  protected MeterService()
  {
  }

  protected void setManager(MeterService manager)
  {
    if (manager == null)
      manager = new MeterService();
    else {
      manager._meterMap.putAll(_meterMap);
    }

    _manager = manager;
  }

  public static MeterService getCurrent()
  {
    return _manager;
  }

  public static MeterService create()
  {
    return _manager;
  }

  public static AbstractMeter getMeter(String name)
  {
    return create().getMeterImpl(name);
  }

  private AbstractMeter getMeterImpl(String name)
  {
    return _meterMap.get(name);
  }

  public static AverageTimeMeter createAverageTimeMeter(String name)
  {
    return create().createAverageTimeMeterImpl(name);
  }

  private AverageTimeMeter createAverageTimeMeterImpl(String name)
  {
    AbstractMeter meter = _meterMap.get(name);

    if (meter == null) {
      meter = createMeter(new AverageTimeMeter(name));
    }

    return (AverageTimeMeter) meter;
  }

  public static SampleCountMeter createSampleCountMeter(String name)
  {
    return create().createSampleCountMeterImpl(name);
  }

  private SampleCountMeter createSampleCountMeterImpl(String name)
  {
    AbstractMeter meter = _meterMap.get(name);

    if (meter == null) {
      meter = createMeter(new SampleCountMeter(name));
    }

    return (SampleCountMeter) meter;
  }

  public static CountMeter createCountMeter(String name)
  {
    return create().createCountMeterImpl(name);
  }

  private CountMeter createCountMeterImpl(String name)
  {
    AbstractMeter meter = _meterMap.get(name);

    if (meter == null) {
      meter = createMeter(new CountMeter(name));
    }

    return (CountMeter) meter;
  }

  public static AbstractMeter createJmx(String name,
                                        String objectName,
                                        String attribute)
  {
    return create().createJmxImpl(name, objectName, attribute);
  }

  private AbstractMeter createJmxImpl(String name, 
                                      String objectName,
                                      String attribute)
  {
    AbstractMeter meter = _meterMap.get(name);

    if (meter == null) {
        meter = createMeter(new JmxAttributeMeter(name, objectName, attribute));
    }

    return meter;
  }

  public static AbstractMeter createJmxDelta(String name,
                                             String objectName,
                                             String attribute)
  {
    return create().createJmxDeltaImpl(name, objectName, attribute);
  }

  private AbstractMeter createJmxDeltaImpl(String name,
                                           String objectName,
                                           String attribute)
  {
    AbstractMeter meter = _meterMap.get(name);

    if (meter == null) {
      meter = createMeter(new JmxDeltaMeter(name, objectName, attribute));
    }

    return meter;
  }

  public static TimeMeter createTimeMeter(String name)
  {
    return create().createTimeMeterImpl(name);
  }

  private TimeMeter createTimeMeterImpl(String name)
  {
    AbstractMeter meter = _meterMap.get(name);

    if (meter == null) {
      meter = createMeter(new TimeMeter(name));
    }

    return (TimeMeter) meter;
  }

  public static TimeRangeMeter createTimeRangeMeter(String baseName)
  {
    return create().createTimeRangeMeterImpl(baseName);
  }

  private TimeRangeMeter createTimeRangeMeterImpl(String baseName)
  {
    String timeName = baseName + " Time";

    AbstractMeter meter = _meterMap.get(timeName);

    if (meter == null) {
      meter = createMeter(new TimeRangeMeter(timeName));

      TimeRangeMeter timeRangeMeter = (TimeRangeMeter) meter;

      String countName = baseName + " Count";
      createMeter(timeRangeMeter.createCount(countName));

      String maxName = baseName + " Max";
      createMeter(timeRangeMeter.createMax(maxName));
    }

    return (TimeRangeMeter) meter;
  }

  public static AverageMeter createAverageMeter(String name, String type)
  {
    return create().createAverageMeterImpl(name, type);
  }

  private AverageMeter createAverageMeterImpl(String baseName, String type)
  {
    String name;

    if (! "".equals(type))
      name = baseName + " " + type;
    else
      name = baseName;

    AbstractMeter meter = _meterMap.get(name);

    if (meter == null) {
      meter = createMeter(new AverageMeter(name));

      AverageMeter averageMeter = (AverageMeter) meter;

      String countName = baseName + " Count";
      createMeter(averageMeter.createCount(countName));

      String sigmaName = name + " 95%";
      createMeter(averageMeter.createSigma(sigmaName, 3));

      String maxName = name + " Max";
      createMeter(averageMeter.createMax(maxName));
    }

    return (AverageMeter) meter;
  }

  public static ActiveTimeMeter createActiveTimeMeter(String name)
  {
    return create().createActiveTimeMeterImpl(name, "Time", null);
  }

  public static ActiveTimeMeter createActiveTimeMeter(String name,
                                                      String type,
                                                      String subName)
  {
    return create().createActiveTimeMeterImpl(name, type, subName);
  }

  private ActiveTimeMeter
    createActiveTimeMeterImpl(String baseName,
                              String type,
                              String subName)
  {
    if (subName == null || subName.equals(""))
      subName = "";
    else if (! subName.startsWith("|"))
      subName = "|" + subName;

    String name = baseName + " " + type + subName;

    AbstractMeter meter = _meterMap.get(name);

    if (meter == null) {
      meter = createMeter(new ActiveTimeMeter(name));

      ActiveTimeMeter activeTimeMeter = (ActiveTimeMeter) meter;

      /*
      String activeCountName = baseName + " Active" + subName;
      createMeter(activeTimeMeter.createActiveCount(activeCountName));
      */

      String sigmaName = baseName + " " + type + " 95%" + subName;
      createMeter(activeTimeMeter.createSigma(sigmaName, 3));

      String maxName = baseName + " " + type + " Max" + subName;
      createMeter(activeTimeMeter.createMax(maxName));

      String activeMaxName = baseName + " Active" + subName;
      createMeter(activeTimeMeter.createActiveCountMax(activeMaxName));

      String totalCountName = baseName + " Count" + subName;
      createMeter(activeTimeMeter.createTotalCount(totalCountName));
    }

    return (ActiveTimeMeter) meter;
  }

  /**
   * An ActiveMeter counts the number of an active resource, e.g. the
   * number of active connections.
   */
  public static ActiveMeter createActiveMeter(String name)
  {
    return create().createActiveMeterImpl(name, null);
  }

  public static ActiveMeter createActiveMeter(String name,
                                              String subName)
  {
    return _manager.createActiveMeterImpl(name, subName);
  }

  private ActiveMeter
    createActiveMeterImpl(String baseName,
                          String subName)
  {
    if (subName == null || subName.equals(""))
      subName = "";
    else if (! subName.startsWith("|"))
      subName = "|" + subName;

    String name = baseName + " Count" + subName;

    AbstractMeter meter = _meterMap.get(name);

    if (meter == null) {
      meter = createMeter(new ActiveMeter(name));

      ActiveMeter activeMeter = (ActiveMeter) meter;

      String maxName = baseName + " Active" + subName;
      createMeter(activeMeter.createMax(maxName));

      /*
      String totalName = baseName + " Total" + subName;
      createMeter(activeMeter.createTotal(totalName));
      */
    }

    return (ActiveMeter) meter;
  }

  public static SemaphoreMeter createSimpleSemaphoreMeter(String name)
  {
    return create().createSemaphoreMeterImpl(name, false);
  }

  /**
   * Creates a semaphore meter and generate Count, Min, and Max meter.
   */
  public static SemaphoreMeter createSemaphoreMeter(String name)
  {
    return create().createSemaphoreMeterImpl(name, true);
  }

  private SemaphoreMeter createSemaphoreMeterImpl(String baseName,
                                                  boolean isExtended)
  {
    String name = baseName;

    AbstractMeter meter = _meterMap.get(name);

    if (meter == null)
      meter = createMeter(new SemaphoreMeter(name));

    SemaphoreMeter semaphoreMeter = (SemaphoreMeter) meter;

    if (! isExtended)
      return semaphoreMeter;

    String countName = baseName + " Acquire";
    createMeter(semaphoreMeter.createCount(countName));

    String maxName = name + " Max";
    createMeter(semaphoreMeter.createMax(maxName));

    String minName = name + " Min";
    createMeter(semaphoreMeter.createMin(minName));

    return (SemaphoreMeter) meter;
  }

  protected AbstractMeter createMeter(AbstractMeter newMeter)
  {
    AbstractMeter meter = _meterMap.putIfAbsent(newMeter.getName(), newMeter);

    if (meter != null) {
      return meter;
    }
    else {
      registerMeter(newMeter);

      return newMeter;
    }
  }

  protected void registerMeter(AbstractMeter meter)
  {
  }
}
