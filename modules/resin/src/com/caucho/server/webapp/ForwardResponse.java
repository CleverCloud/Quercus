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

package com.caucho.server.webapp;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.caucho.server.http.CauchoResponseWrapper;
import com.caucho.util.L10N;

/**
 * Internal response for an include() or forward()
 */
class ForwardResponse extends CauchoResponseWrapper
{
  private static final L10N L = new L10N(ForwardResponse.class);

  ForwardResponse(ForwardRequest request)
  {
    super(request);
  }

  ForwardResponse(ForwardRequest request, HttpServletResponse response)
  {
    super(request, response);
  }

  void startRequest()
  {
  }

  void finishRequest()
    throws IOException
  {
    // server/106f, server/12b2, ioc/0310
    // XXX remove all on a good regression run
    //AbstractResponseStream stream = getResponseStream();
    /*
    if (stream != null)
      stream.close();
    */
  }
}
