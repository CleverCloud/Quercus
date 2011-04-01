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

package com.caucho.config.gen;

import com.caucho.config.inject.DecoratorBean;
import com.caucho.config.inject.InterceptorBean;
import com.caucho.config.inject.InjectManager;
import com.caucho.util.L10N;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.SessionSynchronization;
import javax.transaction.Synchronization;
import javax.transaction.Status;

/**
 * Utilities
 */
public class SynchronizationAdapter implements Synchronization {
  private SessionSynchronization _sync;
  
  public SynchronizationAdapter(SessionSynchronization sync)
  {
    _sync = sync;
  }
  
  public void beforeCompletion()
  {
    try {
      _sync.beforeCompletion();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void afterCompletion(int status)
  {
    try {
      _sync.afterCompletion(status == Status.STATUS_COMMITTING);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
