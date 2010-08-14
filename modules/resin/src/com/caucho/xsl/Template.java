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

package com.caucho.xsl;

import com.caucho.xpath.pattern.AbstractPattern;
import com.caucho.xpath.pattern.FromAny;

public class Template {
  AbstractPattern pattern;
  String mode;
  int minImportance;
  int maxImportance;
  double priority;
  int count;
  String function;
  public int funId;

  public Template(AbstractPattern pattern, String mode,
                  int minImportance, int maxImportance,
                  double priority, int count,
                  String function, int funId)
  {
    if (mode == null)
      mode = "";
    this.mode = mode;
    if (pattern == null)
      pattern = new FromAny();
    this.pattern = pattern;
    this.function = function;
    this.funId = funId;
    this.minImportance = minImportance;
    this.maxImportance = maxImportance;
    this.priority = priority;
    this.count = count;
  }

  public AbstractPattern getPattern()
  {
    return pattern;
  }
  
  public int getId()
  {
    return funId;
  }

  public String getMode()
  {
    return mode;
  }

  public int getMin()
  {
    return minImportance;
  }

  public int getMax()
  {
    return maxImportance;
  }

  public double getPriority()
  {
    return priority;
  }

  public int getCount()
  {
    return count;
  }

  public String getFunction()
  {
    return function;
  }
    
  int compareTo(Template right)
  {
    if (this.maxImportance < right.maxImportance)
      return -1;
    if (this.maxImportance > right.maxImportance)
      return 1;
    if (this.priority < right.priority)
      return -1;
    if (this.priority > right.priority)
      return 1;
    
    return this.count - right.count;
  }

  public String toString()
  {
    return "[Template " + pattern + (mode != null ? (" mode:" + mode) : "") + "]";
  }
}
