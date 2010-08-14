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

package javax.script;

/**
 * Represents script engines that can invoke a previously executed method.
 */
public interface Invocable {
  /**
   * Calls the named method.
   *
   * @param name the name of the function
   * @param thisObject the class object
   * @param args the arguments
   */
  public Object invokeMethod(Object thisObject,
                             String name,
                             Object... args)
    throws ScriptException, NoSuchMethodException;
  
  /**
   * Calls the named method.
   *
   * @param name the name of the function
   * @param args the arguments
   */
  public Object invokeFunction(String name,
                               Object... args)
    throws ScriptException, NoSuchMethodException;
  
  /**
   * Returns an implementation of an interface.
   *
   * @param proxyClass the name of the function
   */
  public <T> T getInterface(Class<T> apiClass);
  
  /**
   * Returns an implementation of an interface.
   *
   * @param proxyClass the name of the function
   */
  public <T> T getInterface(Object thisObj,
                            Class<T> apiClass);
}

