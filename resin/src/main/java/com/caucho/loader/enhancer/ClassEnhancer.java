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

package com.caucho.loader.enhancer;

import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JavaClass;
import com.caucho.inject.Module;
import com.caucho.java.gen.GenClass;

/**
 * Interface for a class enhancer.
 */
@Module
public interface ClassEnhancer {
  /**
   * Returns true if the class will be enhanced.
   */
  public boolean shouldEnhance(String className);

  /**
   * Fixups for the pre-enhancement class.
   */
  public void preEnhance(JavaClass baseClass)
    throws Exception;

  /**
   * Enhances the class by adding to the GenClass.
   */
  public void enhance(GenClass genClass,
                      JClass baseClass,
                      String extClassName)
    throws Exception;
  
  /**
   * Any post compilation fixups.
   */
  public void postEnhance(JavaClass extClass)
    throws Exception;
}
