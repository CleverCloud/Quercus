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

package com.caucho.jms.message;

import com.caucho.jms.connection.JmsSession;
import com.caucho.hessian.io.*;
import com.caucho.vfs.*;
import com.caucho.util.*;

import javax.jms.*;
import java.lang.ref.WeakReference;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A basic message.
 */
public class MessageImpl implements Message, java.io.Serializable
{
  protected static final Logger log
    = Logger.getLogger(MessageImpl.class.getName());
  protected static final L10N L = new L10N(MessageImpl.class);

  private static final HashSet<String> _reserved;

  private transient volatile WeakReference<JmsSession> _sessionRef;
  
  private String _messageId;
  private String _correlationId;
  
  private long _timestamp;
  private long _expiration;
  
  //XXX: 
  private transient Destination _destination;
  private transient Destination _replyTo;

  private int _deliveryMode = DeliveryMode.PERSISTENT;
  private boolean _isRedelivered;

  private String _messageType;
  private int _priority = 4;

  private long _sequence;

  private HashMap<String,Object> _properties;
  
  private transient boolean _isHeaderWriteable = true;
  private transient boolean _isBodyWriteable = true;

  public MessageImpl()
  {
  }

  /**
   * Create a message, copying the properties
   */
  public MessageImpl(Message msg)
    throws JMSException
  {
    _messageId = msg.getJMSMessageID();
    _correlationId = msg.getJMSCorrelationID();
    
    _timestamp = msg.getJMSTimestamp();
    _expiration = msg.getJMSExpiration();
    
    _destination = msg.getJMSDestination();
    _replyTo = msg.getJMSReplyTo();

    _deliveryMode = msg.getJMSDeliveryMode();
    _isRedelivered = msg.getJMSRedelivered();
    
    _messageType = msg.getJMSType();
    _priority = msg.getJMSPriority();
    
    Enumeration e = msg.getPropertyNames();

    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();

      setObjectProperty(name, msg.getObjectProperty(name));
    }

    _isHeaderWriteable = true;
    _isBodyWriteable = true;
  }
  
  public MessageImpl(MessageImpl msg)
  {
    if (msg._properties != null) {
      _properties = new HashMap<String,Object>(msg._properties);
    }

    _messageId = msg._messageId;
    _correlationId = msg._correlationId;
    
    _timestamp = msg._timestamp;
    _expiration = msg._expiration;
    
    _destination = msg._destination;
    _replyTo = msg._replyTo;

    _deliveryMode = msg._deliveryMode;
    _isRedelivered = msg._isRedelivered;
    
    _messageType = msg._messageType;
    _priority = msg._priority;
    
    _isHeaderWriteable = false;
    _isBodyWriteable = false;
  }

  /**
   * Sets the session.
   */
  public void setSession(JmsSession session)
  {
    _sessionRef = new WeakReference<JmsSession>(session);
  }

  /**
   * Returns the type enumeration.
   */
  public MessageType getType()
  {
    return MessageType.NULL;
  }

  /**
   * Returns the message id.
   */
  public String getJMSMessageID()
  {
    return _messageId;
  }

  /**
   * Sets the message id.
   *
   * @param id the new message id
   */
  public void setJMSMessageID(String id)
  {
    _messageId = id;
  }

  /**
   * Returns the time the message was sent.
   */
  public long getJMSTimestamp()
    throws JMSException
  {
    return _timestamp;
  }

  /**
   * Sets the time the message was sent.
   *
   * @param time the message timestamp
   */
  public void setJMSTimestamp(long time)
    throws JMSException
  {
    _timestamp = time;
  }

  /**
   * Returns the correlation id.
   */
  public byte []getJMSCorrelationIDAsBytes()
    throws JMSException
  {
    try {
      if (_correlationId == null)
        return null;
      else
        return _correlationId.getBytes("UTF8");
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
      
      return null;
    }
  }

  /**
   * Sets the correlation id.
   *
   * @param id the correlation id
   */
  public void setJMSCorrelationIDAsBytes(byte []id)
    throws JMSException
  {
    try {
      _correlationId = new String(id, 0, id.length, "UTF8");
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Returns the correlation id.
   */
  public String getJMSCorrelationID()
    throws JMSException
  {
    return _correlationId;
  }

  /**
   * Sets the correlation id.
   *
   * @param id the correlation id
   */
  public void setJMSCorrelationID(String id)
    throws JMSException
  {
    _correlationId = id;
  }

  /**
   * Gets the reply-to destination
   */
  public Destination getJMSReplyTo()
    throws JMSException
  {
    return _replyTo;
  }

  /**
   * Sets the reply-to destination
   *
   * @param replyTo the destination
   */
  public void setJMSReplyTo(Destination replyTo)
    throws JMSException
  {
    _replyTo = replyTo;
  }

  /**
   * Gets the destination
   */
  public Destination getJMSDestination()
    throws JMSException
  {
    return _destination;
  }

  /**
   * Sets the reply-to destination
   *
   * @param destination the destination
   */
  public void setJMSDestination(Destination destination)
    throws JMSException
  {
    _destination = destination;
  }

  /**
   * Gets the delivery model
   */
  public int getJMSDeliveryMode()
    throws JMSException
  {
    return _deliveryMode;
  }

  /**
   * Sets the delivery mode
   *
   * @param deliveryMode the delivery mode
   */
  public void setJMSDeliveryMode(int deliveryMode)
    throws JMSException
  {
    _deliveryMode = deliveryMode;
  }

  /**
   * Returns if the message is being redelivered.
   */
  public boolean getJMSRedelivered()
  {
    return _isRedelivered;
  }

  /**
   * Sets if the message is being redelivered.
   *
   * @param deliveryMode the delivery mode
   */
  public void setJMSRedelivered(boolean isRedelivered)
  {
    _isRedelivered = isRedelivered;
  }

  /**
   * Returns the message type
   */
  public String getJMSType()
    throws JMSException
  {
    return _messageType;
  }

  /**
   * Sets the message type.
   *
   * @param type the delivery mode
   */
  public void setJMSType(String type)
    throws JMSException
  {
    _messageType = type;
  }

  /**
   * Returns the message expiration time.
   */
  public long getJMSExpiration()
    throws JMSException
  {
    return _expiration;
  }

  /**
   * Sets the message expiration type.
   *
   * @param time the expiration time
   */
  public void setJMSExpiration(long time)
    throws JMSException
  {
    _expiration = time;
  }

  /**
   * Returns the message priority.
   */
  public int getJMSPriority()
  {
    return _priority;
  }

  /**
   * Sets the message priority.
   *
   * @param priority the priority
   */
  public void setJMSPriority(int priority)
  {
    _priority = priority;
  }

  /**
   * Clears the message properties, making them writeable.
   */
  public void clearProperties()
    throws JMSException
  {
    if (_properties != null)
      _properties.clear();
    _isHeaderWriteable = true;
  }

  /**
   * Returns true if the property exists.
   */
  public boolean propertyExists(String name)
    throws JMSException
  {
    if (_properties == null)
      return false;
    
    return _properties.keySet().contains(name);
  }

  /**
   * Returns a boolean property with the given name.
   */
  public boolean getBooleanProperty(String name)
    throws JMSException
  {
    return ObjectConverter.toBoolean(getObjectProperty(name));
  }

  /**
   * Returns a property as a byte
   */
  public byte getByteProperty(String name)
    throws JMSException
  {
    return ObjectConverter.toByte(getObjectProperty(name));
  }

  /**
   * Returns a property as a short
   */
  public short getShortProperty(String name)
    throws JMSException
  {
    return ObjectConverter.toShort(getObjectProperty(name));
  }

  /**
   * Returns a property as an integer
   */
  public int getIntProperty(String name)
    throws JMSException
  {
    return ObjectConverter.toInt(getObjectProperty(name));
  }

  /**
   * Returns a property as a long
   */
  public long getLongProperty(String name)
    throws JMSException
  {
    return ObjectConverter.toLong(getObjectProperty(name));
  }

  /**
   * Returns a property as a float
   */
  public float getFloatProperty(String name)
    throws JMSException
  {
    return ObjectConverter.toFloat(getObjectProperty(name));
  }

  /**
   * Returns a property as a double
   */
  public double getDoubleProperty(String name)
    throws JMSException
  {
    return ObjectConverter.toDouble(getObjectProperty(name));
  }

  /**
   * Returns a string property.
   */
  public String getStringProperty(String name)
    throws JMSException
  {
    Object prop = getObjectProperty(name);

    if (prop == null)
      return null;

    return String.valueOf(prop);
  }

  /**
   * Returns a string property.
   */
  public Object getObjectProperty(String name)
    throws JMSException
  {
    if (_properties == null || name == null)
      return null;
    else
      return _properties.get(name);
  }

  /**
   * Returns an enumeration of the message's properties.
   */
  public Enumeration getPropertyNames()
    throws JMSException
  {
    if (_properties == null)
      return NullEnumeration.create();
    else
      return Collections.enumeration(_properties.keySet());
  }

  /**
   * Sets a boolean property.
   *
   * @param name the property name
   * @param value the property's value
   */
  public void setBooleanProperty(String name, boolean value)
    throws JMSException
  {
    setObjectProperty(name, new Boolean(value));
  }

  /**
   * Sets a byte property.
   *
   * @param name the property name
   * @param value the property's value
   */
  public void setByteProperty(String name, byte value)
    throws JMSException
  {
    setObjectProperty(name, new Byte(value));
  }

  /**
   * Sets a short property.
   *
   * @param name the property name
   * @param value the property's value
   */
  public void setShortProperty(String name, short value)
    throws JMSException
  {
    setObjectProperty(name, new Short(value));
  }

  /**
   * Sets an integer property.
   *
   * @param name the property name
   * @param value the property's value
   */
  public void setIntProperty(String name, int value)
    throws JMSException
  {
    setObjectProperty(name, new Integer(value));
  }

  /**
   * Sets a long property.
   *
   * @param name the property name
   * @param value the property's value
   */
  public void setLongProperty(String name, long value)
    throws JMSException
  {
    setObjectProperty(name, new Long(value));
  }

  /**
   * Sets a float property.
   *
   * @param name the property name
   * @param value the property's value
   */
  public void setFloatProperty(String name, float value)
    throws JMSException
  {
    setObjectProperty(name, new Float(value));
  }

  /**
   * Sets a double property.
   *
   * @param name the property name
   * @param value the property's value
   */
  public void setDoubleProperty(String name, double value)
    throws JMSException
  {
    setObjectProperty(name, new Double(value));
  }

  /**
   * Sets a string property.
   *
   * @param name the property name
   * @param value the property's value
   */
  public void setStringProperty(String name, String value)
    throws JMSException
  {
    setObjectProperty(name, value);
  }

  /**
   * Sets an object property.
   *
   * @param name the property name
   * @param value the property's value
   */
  public void setObjectProperty(String name, Object value)
    throws JMSException
  {
    checkPropertyWriteable();

    if (name == null)
      throw new NullPointerException();
    else if ("".equals(name))
      throw new IllegalArgumentException();
    if (isReserved(name))
      throw new JMSException(L.l("'{0}' is a reserved property name.",
                                 name));

    if (! (value == null
           || value instanceof Number
           || value instanceof String
           || value instanceof Boolean))
      throw new MessageFormatException(L.l("{0} is an illegal object property value",
                                           value.getClass().getName()));
    
    if (_properties == null)
      _properties = new HashMap<String,Object>();

    _properties.put(name, value);
  }
  
  /**
   * Acknowledge receipt of this message.
   */
  public void acknowledge()
    throws JMSException
  {
    WeakReference<JmsSession> sessionRef = _sessionRef;
    _sessionRef = null;
    
    JmsSession session;

    if (sessionRef != null && (session = sessionRef.get()) != null) {
      session.acknowledge();
    }
  }
  
  /**
   * Clears the body, setting write mode.
   */
  public void clearBody()
    throws JMSException
  {
    _isBodyWriteable = true;
  }

  /**
   * Sets the body for reading.
   */
  public void setReceive()
    throws JMSException
  {
    _isHeaderWriteable = false;
    _isBodyWriteable = false;
  }

  /**
   * Sets the body for reading.
   */
  protected void setBodyReadOnly()
  {
    _isBodyWriteable = false;
  }

  /**
   * Returns the properties.
   */
  public HashMap<String,Object> getProperties()
  {
    return _properties;
  }

  public long getSequence()
  {
    return _sequence;
  }

  public void setSequence(long seq)
  {
    _sequence = seq;
  }
  
  public MessageImpl copy()
  {
    MessageImpl msg = new MessageImpl();

    copy(msg);

    return msg;
  }

  /**
   * Serialize the properties to an input stream.
   */
  public InputStream propertiesToInputStream()
    throws IOException
  {
    if (_properties == null || _properties.size() == 0)
      return null;
    
    TempOutputStream out = new TempOutputStream();

    writeProperties(out);
    
    out.close();

    return out.openRead();
  }

  /**
   * Serialize the properties to an input stream.
   */
  public void writeProperties(OutputStream os)
    throws IOException
  {
    if (_properties == null || _properties.size() == 0)
      return;

    Hessian2Output out = new Hessian2Output(os);

    out.writeString(_messageId);
    out.writeBoolean(_isRedelivered);
    out.writeInt(_priority);
    out.writeLong(_timestamp);
    out.writeInt(_deliveryMode);
    if (_destination instanceof java.io.Serializable)
      out.writeObject(_destination);
    else
      out.writeObject(null);
    
    if (_replyTo instanceof java.io.Serializable)
      out.writeObject(_replyTo);
    else
      out.writeObject(null);

    for (Map.Entry<String,Object> entry : _properties.entrySet()) {
      out.writeString(entry.getKey());
      out.writeObject(entry.getValue());
    }

    out.close();
  }

  /**
   * Read the properties from an input stream.
   */
  public void readProperties(InputStream is)
    throws IOException, JMSException
  {
    if (is == null)
      return;

    Hessian2Input in = new Hessian2Input(is);

    _messageId = in.readString();
    _isRedelivered = in.readBoolean();
    _priority = in.readInt();
    _timestamp = in.readLong();
    _deliveryMode = in.readInt();
    _destination = (Destination) in.readObject();
    _replyTo = (Destination) in.readObject();

    while (! in.isEnd()) {
      String key = in.readString();
      Object value = in.readObject();

      setObjectProperty(key, value);
    }

    in.close();
  }

  /**
   * Serialize the body to an input stream.
   */
  public InputStream bodyToInputStream()
    throws IOException
  {
    return null;
  }

  /**
   * Serialize the body to an output stream.
   */
  public void writeBody(OutputStream os)
    throws IOException
  {
  }

  /**
   * Read the body from an input stream.
   */
  public void readBody(InputStream is)
    throws IOException, JMSException
  {
  }

  protected void checkHeaderWriteable()
    throws JMSException
  {
    if (! _isHeaderWriteable)
      throw new MessageNotWriteableException(L.l("received messages can't be written."));
  }

  protected void checkPropertyWriteable()
    throws JMSException
  {
    if (! _isHeaderWriteable)
      throw new MessageNotWriteableException(L.l("properties for received messages are read-only."));
  }

  protected void checkBodyWriteable()
    throws JMSException
  {
    if (! _isBodyWriteable)
      throw new MessageNotWriteableException(L.l("received messages can't be written."));
  }

  protected void checkBodyReadable()
    throws JMSException
  {
    if (_isBodyWriteable)
      throw new MessageNotReadableException(L.l("write-only messages can't be read until reset() is called."));
  }

  protected void copy(MessageImpl newMsg)
  {
    if (_properties != null) {
      newMsg._properties = new HashMap<String,Object>(_properties);
    }

    newMsg._messageId = _messageId;
    newMsg._correlationId = _correlationId;
    
    newMsg._timestamp = _timestamp;
    newMsg._expiration = _expiration;
    
    newMsg._destination = _destination;
    newMsg._replyTo = _replyTo;

    newMsg._deliveryMode = _deliveryMode;
    newMsg._isRedelivered = _isRedelivered;
    
    newMsg._messageType = _messageType;
    newMsg._priority = _priority;
  }

  public String toString()
  {
    if (_messageId != null)
      return getClass().getSimpleName() + "[" + _messageId + "]";
    else if (Alarm.isTest())
      return getClass().getSimpleName() + "[]";
    else
      return getClass().getSimpleName() + "@" + System.identityHashCode(this);
  }

  public static boolean isReserved(String name)
  {
    return _reserved.contains(name.toUpperCase());
  }

  static {
    _reserved = new HashSet<String>();
    _reserved.add("TRUE");
    _reserved.add("FALSE");
    _reserved.add("NULL");
    _reserved.add("NOT");
    _reserved.add("AND");
    _reserved.add("OR");
    _reserved.add("BETWEEN");
    _reserved.add("LIKE");
    _reserved.add("IN");
    _reserved.add("IS");
    _reserved.add("ESCAPE");
  }
}

