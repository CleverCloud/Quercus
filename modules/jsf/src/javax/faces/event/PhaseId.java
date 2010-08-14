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

package javax.faces.event;

import java.util.*;

public class PhaseId implements Comparable
{
  public static final PhaseId ANY_PHASE
    = new PhaseId("any", 0);
  public static final PhaseId RESTORE_VIEW
    = new PhaseId("restore-view", 1);
  public static final PhaseId APPLY_REQUEST_VALUES
    = new PhaseId("apply-request-values", 2);
  public static final PhaseId PROCESS_VALIDATIONS
    = new PhaseId("process-validations", 3);
  public static final PhaseId UPDATE_MODEL_VALUES
    = new PhaseId("update-model-values", 4);
  public static final PhaseId INVOKE_APPLICATION
    = new PhaseId("invoke-application", 5);
  public static final PhaseId RENDER_RESPONSE
    = new PhaseId("render-response", 6);
  
  public static final List VALUES;
  
  private final String _name;
  private final int _value;

  PhaseId(String name, int value)
  {
    _name = name;
    _value = value;
  }

  public int compareTo(Object o)
  {
    if (! (o instanceof PhaseId))
      return -1;

    PhaseId phaseId = (PhaseId) o;

    if (_value < phaseId._value)
      return -1;
    else if (phaseId._value < _value)
      return 1;
    else
      return 0;
  }

  public int getOrdinal()
  {
    return _value;
  }

  public String toString()
  {
    return _name;
  }

  static {
    ArrayList<PhaseId> values = new ArrayList<PhaseId>();

    values.add(ANY_PHASE);
    values.add(RESTORE_VIEW);
    values.add(APPLY_REQUEST_VALUES);
    values.add(PROCESS_VALIDATIONS);
    values.add(UPDATE_MODEL_VALUES);
    values.add(INVOKE_APPLICATION);
    values.add(RENDER_RESPONSE);

    VALUES = Collections.unmodifiableList(values);
  }
}
