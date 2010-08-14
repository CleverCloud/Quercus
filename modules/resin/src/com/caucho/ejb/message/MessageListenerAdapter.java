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
 * @author Sam
 */

package com.caucho.ejb.message;

import javax.jms.MessageListener;
import javax.jms.Message;
import javax.ejb.MessageDrivenBean;
import javax.ejb.EJBException;
import javax.ejb.MessageDrivenContext;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class MessageListenerAdapter
  implements MessageListener, MessageDrivenBean
{
  private final Object _listener;
  private final MessageDrivenBean _messageDrivenBean;
  private final Method _onMessageMethod;
  private final Method _ejbCreateMethod;

  public MessageListenerAdapter(Object listener)
    throws NoSuchMethodException
  {
    // ejb/0f94

    Class cl = listener.getClass();

    _listener = listener;

    _onMessageMethod = cl.getMethod("onMessage", new Class[] { Message.class });

    Method ejbCreateMethod;

    if (listener instanceof MessageDrivenBean) {
      _messageDrivenBean = (MessageDrivenBean) listener;

      try {
        ejbCreateMethod = cl.getMethod("ejbCreate", new Class[0]);
      }
      catch (NoSuchMethodException e) {
        ejbCreateMethod = null;
      }

    }
    else {
      _messageDrivenBean = null;
      ejbCreateMethod = null;
    }

    _ejbCreateMethod = ejbCreateMethod;
  }

  public Object getListener()
  {
    return _listener;
  }

  public void onMessage(Message message)
  {
    try {
      _onMessageMethod.invoke(_listener, message);
    }
    catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public void ejbCreate()
    throws IllegalAccessException, InvocationTargetException
  {
    if (_ejbCreateMethod != null)
      _ejbCreateMethod.invoke(_listener, new Object[] {});
  }

  public void ejbRemove()
    throws EJBException
  {
    if (_messageDrivenBean != null)
      _messageDrivenBean.ejbRemove();
  }

  public void setMessageDrivenContext(MessageDrivenContext ctx)
    throws EJBException
  {
    if (_messageDrivenBean != null)
      _messageDrivenBean.setMessageDrivenContext(ctx);
  }
}
