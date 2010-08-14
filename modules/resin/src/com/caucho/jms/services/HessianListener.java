/*
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Emil Ong
 */

package com.caucho.jms.services;

import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianOutput;
import com.caucho.hessian.io.SerializerFactory;
import com.caucho.hessian.server.HessianSkeleton;
import com.caucho.jms.util.BytesMessageInputStream;
import com.caucho.services.server.GenericService;
import com.caucho.util.NullOutputStream;

import javax.jms.*;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * MessageListener for serving Hessian services.
 */
public class HessianListener {
  protected static Logger log
    = Logger.getLogger(HessianListener.class.getName());

  private Class _homeAPI;
  private Object _homeImpl;
  
  private Class _objectAPI;
  private Object _objectImpl;
  
  private HessianSkeleton _homeSkeleton;
  private HessianSkeleton _objectSkeleton;

  private SerializerFactory _serializerFactory;

  private int _listenerMax = 5;
  private Connection _jmsConnection;
  private ConnectionFactory _connectionFactory;
  private Destination _destination;
  private Session _jmsSession;

  /**
   * Sets the home api.
   */
  public void setHomeAPI(Class api)
  {
    _homeAPI = api;
  }

  /**
   * Sets the home implementation
   */
  public void setHome(Object home)
  {
    _homeImpl = home;
  }

  /**
   * Sets the object api.
   */
  public void setObjectAPI(Class api)
  {
    _objectAPI = api;
  }

  /**
   * Sets the object implementation
   */
  public void setObject(Object object)
  {
    _objectImpl = object;
  }

  /**
   * Sets the service class.
   */
  public void setService(Object service)
  {
    setHome(service);
  }

  /**
   * Sets the api-class.
   */
  public void setAPIClass(Class api)
  {
    setHomeAPI(api);
  }

  /**
   * Gets the api-class.
   */
  public Class getAPIClass()
  {
    return _homeAPI;
  }

  /**
   * Sets the name of the connection factory.
   */
  public void setConnectionFactory(ConnectionFactory connectionFactory)
  {
    _connectionFactory = connectionFactory;
  }

  /**
   * Sets the name of the input queue.
   */
  public void setDestination(Destination destination)
  {
    _destination = destination;
  }

  /**
   * Sets the serializer factory.
   */
  public void setSerializerFactory(SerializerFactory factory)
  {
    _serializerFactory = factory;
  }

  /**
   * Gets the serializer factory.
   */
  public SerializerFactory getSerializerFactory()
  {
    if (_serializerFactory == null)
      _serializerFactory = new SerializerFactory();

    return _serializerFactory;
  }

  /**
   * Sets the serializer send collection java type.
   */
  public void setSendCollectionType(boolean sendType)
  {
    getSerializerFactory().setSendCollectionType(sendType);
  }

  public void init()
  {
    if (_homeImpl == null)
      _homeImpl = this;

    if (_homeImpl != null) {
      _homeAPI = findRemoteAPI(_homeImpl.getClass());

      if (_homeAPI == null)
        _homeAPI = _homeImpl.getClass();
    }

    if (_objectAPI == null && _objectImpl != null)
      _objectAPI = _objectImpl.getClass();

    _homeSkeleton = new HessianSkeleton(_homeImpl, _homeAPI);
    if (_objectAPI != null)
      _homeSkeleton.setObjectClass(_objectAPI);

    if (_objectImpl != null) {
      _objectSkeleton = new HessianSkeleton(_objectImpl, _objectAPI);
      _objectSkeleton.setHomeClass(_homeAPI);
    }
    else
      _objectSkeleton = _homeSkeleton;
  }
  
  public void start() throws Throwable
  {
    _jmsConnection = _connectionFactory.createConnection();

    if (_destination instanceof Topic)
      _listenerMax = 1;

    for (int i = 0; i < _listenerMax; i++) {
      Session session = 
        _jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      MessageConsumer consumer = session.createConsumer(_destination);

      consumer.setMessageListener(new HessianListenerMDB());
    }

    _jmsConnection.start();
  }

  public void stop() throws JMSException
  {
    _jmsConnection.stop();
  }

  private Class findRemoteAPI(Class implClass)
  {
    if (implClass == null || implClass.equals(GenericService.class))
      return null;
    
    Class []interfaces = implClass.getInterfaces();

    if (interfaces.length == 1)
      return interfaces[0];

    return findRemoteAPI(implClass.getSuperclass());
  }

  private class HessianListenerMDB implements MessageListener {
    /**
     * Execute a request.  The "id" property of the request selects the 
     * bean.  Once the bean's selected, it will be applied.
     */
    public void onMessage(Message message)
    {
      try {
        String objectId = message.getStringProperty("id");

        // XXX ??? ServiceContext.begin(req, serviceId, objectId);

        if (! (message instanceof BytesMessage)) {
          log.info("HessianListener expects only BytesMessages");
          return;
        }

        BytesMessageInputStream is = 
          new BytesMessageInputStream((BytesMessage) message);

        NullOutputStream os = new NullOutputStream();

        Hessian2Input in = new Hessian2Input(is);
        AbstractHessianOutput out;

        SerializerFactory serializerFactory = getSerializerFactory();

        in.setSerializerFactory(serializerFactory);

        int code = in.read();

        if (code != 'c') {
          // XXX: deflate
          throw new IOException("expected 'c' in hessian input at " + code);
        }

        int major = in.read();
        int minor = in.read();

        if (major >= 2)
          out = new Hessian2Output(os);
        else
          out = new HessianOutput(os);

        out.setSerializerFactory(serializerFactory);

        if (objectId != null)
          _objectSkeleton.invoke(in, out);
        else
          _homeSkeleton.invoke(in, out);

        out.close();
      } catch (JMSException e) {
        log.warning("Unable to process request: " + e);
      } catch (IOException e) {
        log.warning("Unable to process request: " + e);
      } catch (Throwable e) {
        log.warning("Unable to process request: " + e);
      }
      /* XXX
         finally {
         ServiceContext.end();
         }*/
    }
  }
}
