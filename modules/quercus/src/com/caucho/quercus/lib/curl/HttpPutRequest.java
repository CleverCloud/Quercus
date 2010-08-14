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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.curl;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.lib.file.BinaryInput;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ProtocolException;

/**
 * Represents a PUT Http request.
 */
public class HttpPutRequest
  extends HttpRequest
{
  public HttpPutRequest(CurlResource curlResource)
  {
    super(curlResource);
  }

  /**
   * Initializes the connection.
   */
  protected boolean init(Env env)
    throws ProtocolException
  {
    if (! super.init(env))
      return false;
    
    getHttpConnection().setDoOutput(true);
    
    return true;
  }

  /**
   * Transfer data to the server.
   */
  protected void transfer(Env env)
    throws IOException
  {
    super.transfer(env);

    HttpConnection conn = getHttpConnection();
    OutputStream out = conn.getOutputStream();

    CurlResource curl = getCurlResource();

    BinaryInput in = curl.getUploadFile();
    int length = curl.getUploadFileSize();

    for (int i = 0; i < length; i++) {
      int ch = in.read();

      if (ch < 0)
        break;

      out.write(ch);
    }

    out.close();
  }
}
