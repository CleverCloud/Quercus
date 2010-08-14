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

package com.caucho.xml.stream.events;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.ProcessingInstruction;
import java.io.IOException;
import java.io.Writer;

public class ProcessingInstructionImpl extends XMLEventImpl 
  implements ProcessingInstruction 
{
  private final String _target;
  private final String _data;

  public ProcessingInstructionImpl(String target, String data)
  {
    _target = target;
    _data = data;
  }

  public String getData()
  {
    return _data;
  }

  public String getTarget()
  {
    return _target;
  }

  public int getEventType()
  {
    return PROCESSING_INSTRUCTION;
  }

  public void writeAsEncodedUnicode(Writer writer) 
    throws XMLStreamException
  {
    try {
      writer.write("<?" + _target);

      if (_data != null && ! "".equals(_data))
        writer.write(" " + _data);

      writer.write("?>");
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public boolean equals(Object o) 
  {
    if (! (o instanceof ProcessingInstruction))
      return false;
    if (o == null)
      return false;
    if (this == o)
      return true;

    ProcessingInstruction instruction = (ProcessingInstruction) o;
    
    return getData().equals(instruction.getData()) &&
           getTarget().equals(instruction.getTarget());
  }
}

