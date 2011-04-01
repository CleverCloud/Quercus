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

package com.caucho.config.type;

import com.caucho.config.*;
import com.caucho.el.ELParser;
import com.caucho.el.Expr;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import org.w3c.dom.Node;

import java.lang.annotation.Annotation;
import javax.el.ELContext;
import javax.el.ELException;

public class AnnotationType extends ConfigType {
  protected static final L10N L = new L10N(AnnotationType.class);

  public Class getType()
  {
    return Annotation.class;
  }
  
  /**
   * Returns the type's configured value
   *
   * @param builder the context builder
   * @param node the configuration node
   * @param parent
   */
  public Object valueOf(String text)
  {
    return parseAnnotation(text);
  }

  /**
   * Parses the function signature.
   */
  private Annotation parseAnnotation(String signature)
    throws ConfigException
  {
    int p = signature.indexOf('(');
    
    String className;

    if (p > 0)
      className = signature.substring(0, p);
    else
      className = signature;

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      Class cl = Class.forName(className, false, loader);

      Annotation ann = (Annotation) cl.newInstance();

      return ann;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}
