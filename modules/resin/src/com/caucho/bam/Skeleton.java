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

package com.caucho.bam;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.util.L10N;

/**
 * The Skeleton introspects and dispatches messages for a
 * {@link com.caucho.bam.SimpleActor}
 * or {@link com.caucho.bam.SimpleActorStream}.
 */
class Skeleton<S extends SimpleActorStream>
{
  private static final L10N L = new L10N(Skeleton.class);
  private static final Logger log
    = Logger.getLogger(Skeleton.class.getName());

  private final static WeakHashMap<Class<?>, SoftReference<Skeleton>> _skeletonRefMap
    = new WeakHashMap<Class<?>, SoftReference<Skeleton>>();

  private Class<?> _cl;

  private final HashMap<Class<?>, Method> _messageHandlers
    = new HashMap<Class<?>, Method>();
  private final HashMap<Class<?>, Method> _messageErrorHandlers
    = new HashMap<Class<?>, Method>();
  private final HashMap<Class<?>, Method> _queryGetHandlers
    = new HashMap<Class<?>, Method>();
  private final HashMap<Class<?>, Method> _querySetHandlers
    = new HashMap<Class<?>, Method>();
  private final HashMap<Class<?>, Method> _queryResultHandlers
    = new HashMap<Class<?>, Method>();
  private final HashMap<Class<?>, Method> _queryErrorHandlers
    = new HashMap<Class<?>, Method>();
  
  private Skeleton(Class<S> cl)
  {
    _cl = cl;

    log.finest(L.l("{0} introspecting class {1}", this, cl.getName()));

    introspect(cl);
  }

  /**
   * Dispatches a message to the actorStream.
   */
  public void message(S actor,
                      String to,
                      String from,
                      Serializable payload)
  {
    Method handler;

    if (payload != null)
      handler = _messageHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
        log.finest(actor + " message " + payload
                   + " {from:" + from + ", to:" + to + "}");
      }

      try {
        handler.invoke(actor, to, from, payload);
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (InvocationTargetException e) {
        Throwable cause = e.getCause();

        throw SkeletonInvocationException.createRuntimeException(cause);
      }
      catch (Exception e) {
        throw SkeletonInvocationException.createRuntimeException(e);
      }
    }
    else {
      actor.messageFallback(to, from, payload);
    }
  }

  public void messageError(S actor,
                           String to,
                           String from,
                           Serializable payload,
                           ActorError error)
  {
    Method handler;

    if (payload != null)
      handler = _messageErrorHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
        log.finest(actor + " messageError " + error + " " + payload
                   + " {from:" + from + ", to:" + to + "}");
      }

      try {
        handler.invoke(actor, to, from, payload, error);
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (InvocationTargetException e) {
        Throwable cause = e.getCause();

        throw SkeletonInvocationException.createRuntimeException(cause);
      }
      catch (Exception e) {
        throw SkeletonInvocationException.createRuntimeException(e);
      }
    }
    else {
      actor.messageErrorFallback(to, from, payload, error);
    }
  }

  public void queryGet(S actor,
                       ActorStream linkStream,
                       long id,
                       String to,
                       String from,
                       Serializable payload)
  {
    Method handler;

    if (payload != null)
      handler = _queryGetHandlers.get(payload.getClass());
    else {
      handler = null;
    }

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
        log.finest(actor + " queryGet " + payload
                   + " {id: " + id + ", from:" + from + ", to:" + to + "}");
      }

      try {
        handler.invoke(actor, id, to, from, payload);
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (InvocationTargetException e) {
        Throwable cause = e.getCause();

        throw SkeletonInvocationException.createRuntimeException(cause);
      }
      catch (Exception e) {
        throw SkeletonInvocationException.createRuntimeException(e);
      }
    }
    else {
      actor.queryGetFallback(id, to, from, payload);
    }
  }

  public void querySet(S actor,
                       ActorStream linkStream,
                       long id,
                       String to,
                       String from,
                       Serializable payload)
  {
    Method handler;

    if (payload != null)
      handler = _querySetHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
        log.finest(actor + " querySet " + payload
                   + " {id: " + id + ", from:" + from + ", to:" + to + "}");
      }

      try {
        handler.invoke(actor, id, to, from, payload);
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (InvocationTargetException e) {
        Throwable cause = e.getCause();

        throw SkeletonInvocationException.createRuntimeException(cause);
      }
      catch (Exception e) {
        throw SkeletonInvocationException.createRuntimeException(e);
      }
    }
    else {
      actor.querySetFallback(id, to, from, payload);
    }
  }

  public void queryResult(S actor,
                          long id,
                          String to,
                          String from,
                          Serializable payload)
  {
    Method handler;

    if (payload != null)
      handler = _queryResultHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
        log.finest(actor + " queryResult " + payload
                   + " {id: " + id + ", from:" + from + ", to:" + to + "}");
      }

      try {
        handler.invoke(actor, id, to, from, payload);
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (InvocationTargetException e) {
        Throwable cause = e.getCause();

        throw SkeletonInvocationException.createRuntimeException(cause);
      }
      catch (Exception e) {
        throw SkeletonInvocationException.createRuntimeException(e);
      }
    }
    else {
      actor.queryResultFallback(id, to, from, payload);
    }
  }

  public void queryError(S actor,
                         long id,
                         String to,
                         String from,
                         Serializable payload,
                         ActorError error)
  {
    Method handler;

    if (payload != null)
      handler = _queryErrorHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
        log.finest(actor + " queryError " + error + " " + payload
                   + " {id: " + id + ", from:" + from + ", to:" + to + "}");
      }

      try {
        handler.invoke(actor, id, to, from, payload, error);
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (InvocationTargetException e) {
        Throwable cause = e.getCause();

        throw SkeletonInvocationException.createRuntimeException(cause);
      }
      catch (Exception e) {
        throw SkeletonInvocationException.createRuntimeException(e);
      }
    }
    else {
      actor.queryErrorFallback(id, to, from, payload, error);
    }
  }

  protected void introspect(Class<?> cl)
  {
    if (cl == null)
      return;

    introspect(cl.getSuperclass());

    Method[] methods = cl.getDeclaredMethods();

    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];

      Class<?> payloadType = getPayloadType(Message.class, method);

      if (payloadType != null) {
        log.finest(L.l("{0} @Message {1} method={2}",
                       this, payloadType.getName(), method));

        method.setAccessible(true);

        _messageHandlers.put(payloadType, method);
        continue;
      }

      payloadType = getPayloadType(MessageError.class, method);

      if (payloadType != null) {
        log.finest(L.l("{0} @MessageError {1} method={2}",
                       this, payloadType.getName(), method));

        method.setAccessible(true);

        _messageErrorHandlers.put(payloadType, method);
        continue;
      }

      payloadType = getQueryPayloadType(QueryGet.class, method);

      if (payloadType != null) {
        log.finest(L.l("{0} @QueryGet {1} method={2}",
                       this, payloadType.getName(), method));

        method.setAccessible(true);

        _queryGetHandlers.put(payloadType, method);
        continue;
      }

      payloadType = getQueryPayloadType(QuerySet.class, method);

      if (payloadType != null) {
        log.finest(L.l("{0} @QuerySet {1} method={2}",
                       this, payloadType.getName(), method));

        method.setAccessible(true);

        _querySetHandlers.put(payloadType, method);
        continue;
      }

      payloadType = getQueryPayloadType(QueryResult.class, method);

      if (payloadType != null) {
        log.finest(L.l("{0} @QueryResult {1} method={2}",
                       this, payloadType.getName(), method));

        method.setAccessible(true);

        _queryResultHandlers.put(payloadType, method);
        continue;
      }

      payloadType = getQueryErrorPayloadType(QueryError.class, method);

      if (payloadType != null) {
        log.finest(L.l("{0} @QueryError {1} method={2}",
                       this, payloadType.getName(), method));

        method.setAccessible(true);

        _queryErrorHandlers.put(payloadType, method);
        continue;
      }
    }
  }

  private Class<?> getPayloadType(Class<? extends Annotation> annotationType, 
                                  Method method)
  {
    Class<?> []paramTypes = method.getParameterTypes();

    if (paramTypes.length < 3)
      return null;

    if (method.isAnnotationPresent(annotationType))
      return paramTypes[2];
    else
      return null;
  }

  private Class<?> getQueryPayloadType(Class<? extends Annotation> annotationType, 
                                       Method method)
  {
    if (! method.isAnnotationPresent(annotationType))
      return null;

    Class<?> []paramTypes = method.getParameterTypes();

    if (paramTypes.length != 4
        || ! long.class.equals(paramTypes[0])
        || ! String.class.equals(paramTypes[1])
        || ! String.class.equals(paramTypes[2])
        || ! Serializable.class.isAssignableFrom(paramTypes[3])) {
      throw new ActorException(method + " is an invalid "
                             + " @" + annotationType.getSimpleName()
                             + " because queries require (long, String, String, MyPayload)");
    }
    /*
    else if (! void.class.equals(method.getReturnType())) {
      throw new ActorException(method + " is an invalid @"
                             + annotationType.getSimpleName()
                             + " because queries must return void");
    }
    */

    return paramTypes[3];
  }

  private Class<?> getQueryErrorPayloadType(Class<? extends Annotation> annotationType, Method method)
  {
    if (! method.isAnnotationPresent(annotationType))
      return null;

    Class<?> []paramTypes = method.getParameterTypes();

    if (paramTypes.length != 5
        || ! long.class.equals(paramTypes[0])
        || ! String.class.equals(paramTypes[1])
        || ! String.class.equals(paramTypes[2])
        || ! Serializable.class.isAssignableFrom(paramTypes[3])
        || ! ActorError.class.isAssignableFrom(paramTypes[4])) {
      throw new ActorException(method + " is an invalid "
                             + " @" + annotationType.getSimpleName()
                             + " because queries require (long, String, String, MyPayload, ActorError)");
    }
    /*
    else if (! void.class.equals(method.getReturnType())) {
      throw new ActorException(method + " is an invalid @"
                             + annotationType.getSimpleName()
                             + " because queries must return void");
    }
    */

    return paramTypes[3];
  }

  @SuppressWarnings("unchecked")
  public static Skeleton<? extends SimpleActorStream>
  getSkeleton(Class<? extends SimpleActorStream> cl)
  {
    synchronized(_skeletonRefMap) {
      SoftReference<Skeleton> skeletonRef = _skeletonRefMap.get(cl);

      Skeleton<? extends SimpleActorStream> skeleton = null;

      if (skeletonRef != null)
        skeleton = skeletonRef.get();

      if (skeleton == null) {
        skeleton = new Skeleton(cl);
        _skeletonRefMap.put(cl, new SoftReference<Skeleton>(skeleton));
      }

      return skeleton;
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _cl.getName() + "]";
  }
}
