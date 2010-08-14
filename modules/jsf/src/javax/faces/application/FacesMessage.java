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

package javax.faces.application;

import java.util.*;

public class FacesMessage implements java.io.Serializable
{
  public static final Severity SEVERITY_INFO
    = new Severity("info", 0);
  public static final Severity SEVERITY_WARN
    = new Severity("warn", 1);
  public static final Severity SEVERITY_ERROR
    = new Severity("error", 2);
  public static final Severity SEVERITY_FATAL
    = new Severity("fatal", 3);
  
  public static final String FACES_MESSAGES = "javax.faces.Messages";

  public static final List VALUES;
  public static final Map VALUES_MAP;

  private Severity severity;
  private String summary;
  private String detail;

  public FacesMessage()
  {
    this.severity = SEVERITY_INFO;
  }

  public FacesMessage(String summary)
  {
    this.severity = SEVERITY_INFO;
    this.summary = summary;
  }

  public FacesMessage(String summary, String detail)
  {
    this.severity = SEVERITY_INFO;
    this.summary = summary;
    this.detail = detail;
  }

  public FacesMessage(Severity severity, String summary, String detail)
  {
    this.severity = severity;
    this.summary = summary;
    this.detail = detail;
  }

  /**
   * Return the localized detail text. If no localized detail text has been
   * defined for this message, return the localized summary text instead.
   * @return
   */
  public String getDetail()
  {
    if (this.detail == null)
      return this.summary;

    return this.detail;
  }

  public void setDetail(String detail)
  {
    this.detail = detail;
  }

  public Severity getSeverity()
  {
    return this.severity;
  }

  public void setSeverity(Severity severity)
  {
    this.severity = severity;
  }

  public String getSummary()
  {
    return this.summary;
  }

  public void setSummary(String summary)
  {
    this.summary = summary;
  }
  
  public static class Severity implements Comparable {
    private String _name;
    private int _value;

    Severity(String name, int value)
    {
      _name = name;
      _value = value;
    }

    public int getOrdinal()
    {
      return _value;
    }

    public int compareTo(Object other)
    {
      if (! (other instanceof Severity))
        return -1;

      Severity severity = (Severity) other;

      if (_value < severity._value)
        return -1;
      else if (severity._value < _value)
        return 1;
      else
        return 0;
    }

    public String toString()
    {
      return _name;
    }
  }

  static {
    ArrayList<Severity> list
      = new ArrayList<Severity>();

    list.add(SEVERITY_INFO);
    list.add(SEVERITY_WARN);
    list.add(SEVERITY_ERROR);
    list.add(SEVERITY_FATAL);

    VALUES = Collections.unmodifiableList(list);
    
    HashMap<String,Severity> map
      = new HashMap<String,Severity>();

    map.put(SEVERITY_INFO.toString(), SEVERITY_INFO);
    map.put(SEVERITY_WARN.toString(), SEVERITY_WARN);
    map.put(SEVERITY_ERROR.toString(), SEVERITY_ERROR);
    map.put(SEVERITY_FATAL.toString(), SEVERITY_FATAL);

    VALUES_MAP = Collections.unmodifiableMap(map);
  }

  public String toString()
  {
    if (this.detail != null)
      return "FacesMessage[" + this.severity + ",\"" + this.summary + "\",\"" + this.detail + "\"]";
    else if (this.summary != null)
      return "FacesMessage[" + this.severity + ",\"" + this.summary + "\"]";
    else
      return "FacesMessage[" + this.severity + "]";
  }
}
