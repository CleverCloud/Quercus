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

package javax.jcr;

import java.io.InputStream;
import java.util.Calendar;

public interface ValueFactory {
  /**
   * Creates a value based on a string.
   */
  public Value createValue(String value);

  /**
   * Creates a value based on a string, coerced to the expected PropertyType.
   *
   * @param value the new value
   * @param type the expected PropertyType.
   */
  public Value createValue(String value, int type)
    throws ValueFormatException;

  /**
   * Creates a value based on a long.
   */
  public Value createValue(long value);
  
  /**
   * Creates a value based on a double.
   */
  public Value createValue(double value);
  
  /**
   * Creates a value based on a boolean.
   */
  public Value createValue(boolean value);
  
  /**
   * Creates a value based on a date.
   */
  public Value createValue(Calendar value);
  
  /**
   * Creates a value based on a binary stream.
   */
  public Value createValue(InputStream value);
  
  /**
   * Creates a value based on a node reference
   */
  public Value createValue(Node value)
    throws RepositoryException;
}
