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

package com.caucho.jms;

import com.caucho.config.Service;
import com.caucho.config.Configurable;
import com.caucho.jms.memory.MemoryTopicImpl;

/**
 * A JMS topic stored in memory.
 *
 * It is configured as:
 * <pre>
 * &lt;web-app xmlns="http://caucho.com/ns/resin"
 *             xmlns:resin="urn:java:com.caucho.resin">
 *
 *   &lt;resin:MemoryTopic>
 *     &lt;resin:JndiName>jms/my-name&lt;/resin:JndiName>
 *
 *     &lt;resin:Name>my-name&lt;/resin:Name>
 *   &lt;/resin:MemoryTopic>
 *
 * &lt;/web-app>
 * </pre>
 */

@Service
@Configurable  
public class MemoryTopic extends MemoryTopicImpl
{
  /**
   * Standard Injection constructor
   */
  public MemoryTopic()
  {
  }

  /**
   * Programmatic constructor, automatically initializes the topic.
   */
  public MemoryTopic(String name)
  {
    this();
    
    setName(name);

    init();
  }
}

