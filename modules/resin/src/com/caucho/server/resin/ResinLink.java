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

package com.caucho.server.resin;

import com.caucho.hmtp.HmtpLink;

import java.io.*;
import java.util.logging.*;

/**
 * HMTP client protocol
 */
public class ResinLink extends HmtpLink implements Runnable {
  private static final Logger log
    = Logger.getLogger(ResinLink.class.getName());

  private ResinActor _resinActor;
  
  protected InputStream _is;
  protected OutputStream _os;

  public ResinLink(ResinActor resinActor, InputStream is, OutputStream os)
  {
    super(resinActor, is, os);
    
    _resinActor = resinActor;
  }

  /**
   * Receive messages from the client
   */
  @Override
  public void run()
  {
    try {
      Thread.currentThread().setName("resin-main-link");
      ClassLoader loader = ClassLoader.getSystemClassLoader();
      Thread.currentThread().setContextClassLoader(loader);
      
      super.run();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      if (log.isLoggable(Level.FINE))
        log.fine(this + " finishing main thread");
      
      _resinActor.destroy();
    }
  }
}
