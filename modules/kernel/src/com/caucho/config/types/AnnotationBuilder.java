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

package com.caucho.config.types;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import java.lang.annotation.Annotation;

/**
 * Configuration for an annotation
 */
public class AnnotationBuilder {
  private static L10N L = new L10N(AnnotationBuilder.class);

  private String _signature;
  private String _className;
  private String []_parameterTypes;

  private Annotation _annotation;
  
  public AnnotationBuilder()
  {
  }

  /**
   * Returns the signature.
   */
  public String getSignature()
  {
    return _signature;
  }
  
  /**
   * Adds the text value to the signature.
   */
  public void addText(String value)
  {
    _signature = value;
  }

  /**
   * Initialize the signature.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_signature == null)
      throw new ConfigException(L.l("Annotation requires a constructor syntax"));

    parseSignature();
  }

  /**
   * Parses the function signature.
   */
  private void parseSignature()
    throws ConfigException
  {
    int p = _signature.indexOf('(');
    
    String className;

    if (p > 0)
      className = _signature.substring(0, p);
    else
      className = _signature;

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      Class cl = Class.forName(className, false, loader);

      _annotation = (Annotation) cl.newInstance();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public Object replaceObject()
  {
    return _annotation;
  }

  public String toString()
  {
    return "AnnotationBuilder[" + _signature + "]";
  }
}

