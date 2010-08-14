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

package javax.xml.bind.util;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import java.util.ArrayList;

/** ValidationEventHandler implementation that collects all events */
public class ValidationEventCollector implements ValidationEventHandler {

  private boolean _dead = false;
  private ArrayList<ValidationEvent> _events = 
    new ArrayList<ValidationEvent>();

  public ValidationEventCollector()
  {
  }

  public ValidationEvent[] getEvents()
  {
    ValidationEvent[] events = new ValidationEvent[_events.size()];
    _events.toArray(events);

    return events;
  }

  public boolean handleEvent(ValidationEvent event)
  {
    if (event != null)
      _events.add(event);

    if (event == null || event.getSeverity() == ValidationEvent.FATAL_ERROR)
      _dead = true;

    return ! _dead;
  }

  public boolean hasEvents()
  {
    return _events.size() > 0;
  }

  public void reset()
  {
    _events.clear();
    _dead = false;
  }
}

