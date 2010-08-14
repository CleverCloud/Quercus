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
 * @author Emil Ong
 */

package com.caucho.soa.rest;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class HessianRestProxy extends RestProxy {
  private static final Logger log = 
    Logger.getLogger(HessianRestProxy.class.getName());

  public HessianRestProxy(Class api, String url)
  {
    super(api, url);
  }

  protected void writePostData(OutputStream out, ArrayList<Object> postValues)
    throws IOException, RestException
  {
    HessianOutput hessianOut = new HessianOutput(out);

    for (Object postValue : postValues)
      hessianOut.writeObject(postValue);
  }

  protected Object readResponse(InputStream in)
    throws IOException, RestException
  {
    HessianInput hessianIn = new HessianInput(in);

    return hessianIn.readObject(null);
  }
}
