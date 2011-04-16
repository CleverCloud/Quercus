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
package com.caucho.quercus.env;

import com.caucho.quercus.*;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.vfs.WriteStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Represents the Quercus environment.
 */
public class GoogleEnv extends Env {

   private int _timeoutCount;

   public GoogleEnv(QuercusContext quercus,
           QuercusPage page,
           WriteStream out,
           HttpServletRequest request,
           HttpServletResponse response) {
      super(quercus, page, out, request, response);
   }

   public GoogleEnv(QuercusContext quercus) {
      super(quercus);
   }

   /**
    * Checks for the program timeout.
    */
   @Override
   public void checkTimeout() {
      // since GoogleAppEngine doesn't allow Threads, the normal Alarm
      // optimization doesn't work.  Instead use a timeout count to limit
      // the calls
      if (_timeoutCount-- > 0) {
         return;
      }

      _timeoutCount = 8192;

      super.checkTimeout();
   }

   @Override
   public void resetTimeout() {
      super.resetTimeout();

      _timeoutCount = 8192;
   }
}
