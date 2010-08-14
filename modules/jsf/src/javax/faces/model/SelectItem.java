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

package javax.faces.model;

public class SelectItem implements java.io.Serializable
{
  private String description;
  private boolean disabled;
  private boolean escape;
  private String label;
  private Object value;

  public SelectItem()
  {
  }

  public SelectItem(Object value)
  {
    setValue(value);
    if (value != null)
      setLabel(value.toString());
    setEscape(true);
  }

  public SelectItem(Object value,
                    String label)
  {
    setValue(value);
    setLabel(label);
    setEscape(true);
  }

  public SelectItem(Object value,
                    String label,
                    String description)
  {
    setValue(value);
    setLabel(label);
    setDescription(description);
    setEscape(true);
  }

  public SelectItem(Object value,
                    String label,
                    String description,
                    boolean disabled)
  {
    setValue(value);
    setLabel(label);
    setDescription(description);
    setDisabled(disabled);
    setEscape(true);
  }

  public SelectItem(Object value,
                    String label,
                    String description,
                    boolean disabled,
                    boolean escape)
  {
    setValue(value);
    setLabel(label);
    setDescription(description);
    setDisabled(disabled);
    setEscape(escape);
  }

  public String getDescription()
  {
    return this.description;
  }

  public void setDescription(String description)
  {
    this.description = description;
  }

  public boolean isDisabled()
  {
    return this.disabled;
  }

  public void setDisabled(boolean disabled)
  {
    this.disabled = disabled;
  }

  public boolean isEscape()
  {
    return this.escape;
  }

  public void setEscape(boolean escape)
  {
    this.escape = escape;
  }

  public String getLabel()
  {
    return this.label;
  }

  public void setLabel(String label)
  {
    this.label = label;
  }

  public Object getValue()
  {
    return this.value;
  }

  public void setValue(Object value)
  {
    this.value = value;
  }

  public String toString()
  {
    if (this.label != null)
      return "SelectItem[" + this.label + "]";
    else
      return "SelectItem[]";
  }
}
