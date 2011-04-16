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
package com.caucho.quercus.marshal;

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.Expr;

public class NullAsFalseMarshal extends Marshal {

   protected Marshal _marshal;

   public NullAsFalseMarshal(Marshal marshal) {
      _marshal = marshal;
   }

   @Override
   public boolean isBoolean() {
      return _marshal.isBoolean();
   }

   @Override
   public boolean isString() {
      return _marshal.isString();
   }

   @Override
   public boolean isLong() {
      return _marshal.isLong();
   }

   @Override
   public boolean isDouble() {
      return _marshal.isDouble();
   }

   @Override
   public boolean isReadOnly() {
      return _marshal.isReadOnly();
   }

   @Override
   public boolean isReference() {
      return _marshal.isReference();
   }

   @Override
   public Object marshal(Env env, Expr expr, Class argClass) {
      return _marshal.marshal(env, expr, argClass);
   }

   @Override
   public Object marshal(Env env, Value value, Class argClass) {
      return _marshal.marshal(env, value, argClass);
   }

   @Override
   public Value unmarshal(Env env, Object value) {
      // php/1427
      if (value == null) {
         return BooleanValue.FALSE;
      }

      Value result = _marshal.unmarshal(env, value);

      return (result == null || result.isNull()) ? BooleanValue.FALSE : result;
   }

   @Override
   public String toString() {
      return "NullAsFalseMarshal[" + _marshal + "]";
   }
}
