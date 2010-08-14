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
 * @author Emil Ong
 */

package com.caucho.xml.schema;

import java.io.*;
import java.util.*;
import static javax.xml.XMLConstants.*;
import javax.xml.bind.annotation.*;

import com.caucho.java.JavaWriter;

@XmlAccessorType(XmlAccessType.NONE)
public abstract class Type {
  public abstract Schema getSchema();
  public abstract void setSchema(Schema schema);
  public abstract String getName();
  public abstract String getClassname();
  public abstract String getFaultWrapperClassname();
  public abstract String getArgumentName(int index);
  public abstract String getJavaType(int index);
  public abstract void writeJava(File outputDirectory, String pkg)
    throws IOException;
  public abstract void setEmit(boolean emit);
  public abstract void setEmitFaultWrapper(boolean emitFaultWrapper);
}
