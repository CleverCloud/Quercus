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


package com.caucho.quercus.lib.resin;

import com.caucho.quercus.annotation.Name;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.Value;

import javax.management.Attribute;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MBean {
  private static final Logger log = Logger.getLogger(MBean.class.getName());

  private static final HashMap<String,Marshall> _marshallMap
    = new HashMap<String,Marshall>();

  private MBeanServerConnection _server;
  private ObjectName _name;
  private MBeanInfo _info;

  MBean(MBeanServerConnection server, ObjectName name)
  {
    _server = server;
    _name = name;
  }

  /**
   * Returns the mbean's canonical name.
   */
  public String getMbean_name()
  {
    return _name.getCanonicalName();
  }

  /**
   * Returns the MBeanInfo for the mbean.
   */
  @Name("mbean_info")
  public MBeanInfo getMbean_info()
  {
    try {
      if (_info == null)
        _info = _server.getMBeanInfo(_name);

      return _info;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }

  /**
   * Returns an attribute.
   */
  public Object __getField(String attrName)
  {
    try {
      return unmarshall(_server.getAttribute(_name, attrName));
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }

  /**
   * Sets an attribute.
   */
  public boolean __setField(String attrName, Object value)
  {
    try {
      _server.setAttribute(_name, new Attribute(attrName, value));

      return true;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  /**
   * Calls a method.
   */
  public Object __call(String name, Value values)
  {
    try {
      int size = values.getSize();
      
      Object []args = new Object[values.getSize()];

      for (int i = 0; i < size; i++) {
        args[i] = values.get(LongValue.create(i)).toJavaObject();
      }

      MBeanOperationInfo opInfo = findClosestOperation(name, args);
      
      if (opInfo != null) {
        String []mbeanSig = createMBeanSig(opInfo);

        marshall(args, mbeanSig);

        Object value = _server.invoke(_name, name, args, mbeanSig);

        return unmarshall(value);
      }
      else {
        return _server.invoke(_name, name, args, null);
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }

  private String []createMBeanSig(MBeanOperationInfo opInfo)
  {
    MBeanParameterInfo []paramInfo = opInfo.getSignature();
    String []sig = new String[paramInfo.length];

    for (int i = 0; i < paramInfo.length; i++) {
      sig[i] = paramInfo[i].getType();
    }

    return sig;
  }

  protected MBeanOperationInfo findClosestOperation(String name, Object []args)
    throws Exception
  {
    MBeanInfo info = getMbean_info();

    if (info == null)
      return null;
    
    MBeanOperationInfo []ops = info.getOperations();
    
    MBeanOperationInfo bestOp = null;
    long bestCost = Long.MAX_VALUE;

    for (int i = 0; i < ops.length; i++) {
      MBeanOperationInfo op = ops[i];
      
      if (! name.equals(op.getName()))
        continue;

      if (op.getSignature().length == args.length) {
        long cost = calculateCost(op.getSignature(), args);

        if (cost < bestCost) {
          bestCost = cost;
          bestOp = op;
        }
      }
    }

    return bestOp;
  }

  private static long calculateCost(MBeanParameterInfo []paramInfo,
                                    Object []args)
  {
    long cost = 0;
    
    for (int i = 0; i < paramInfo.length; i++) {
      String param = paramInfo[i].getType();
      String arg;

      if (args[i] != null)
        arg = args[i].getClass().getName();
      else
        arg = "java.lang.Object";
      
      if (param.equals(arg)) {
      }
      else if ((param.indexOf('[') >= 0) != (arg.indexOf('[') >= 0)) {
        cost += 100;
      }
      else
        cost += 1;
    }

    return cost;
  }

  private void marshall(Object []args, String []sig)
  {
    for (int i = 0; i < sig.length; i++) {
      args[i] = findMarshall(sig[i]).marshall(args[i]);
    }
  }

  private Object unmarshall(Object value)
  {
    if (value instanceof ObjectName) {
      ObjectName name = (ObjectName) value;

      return new MBean(_server, name);
    }
    else if (value instanceof ObjectName[]) {
      ObjectName []names = (ObjectName []) value;

      MBean []mbeans = new MBean[names.length];

      for (int i = 0; i < names.length; i++)
        mbeans[i] = new MBean(_server, names[i]);

      return mbeans;
    }
    else if (value instanceof CompositeData) {
      CompositeData compositeValue = (CompositeData) value;

      CompositeType type = compositeValue.getCompositeType();

      if (type != null) {
        String typeName = type.getTypeName();

        try {
          ClassLoader loader = Thread.currentThread().getContextClassLoader();

          Class typeClass = Class.forName(typeName, false, loader);

          Method from = typeClass.getMethod("from", new Class[] { CompositeData.class });

          if (from != null)
            return from.invoke(null, compositeValue);
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);
        }
      }

      return new CompositeDataBean(compositeValue);
    }
    else
      return value;
  }

  private Marshall findMarshall(String sig)
  {
    Marshall marshall = _marshallMap.get(sig);

    if (marshall != null)
      return marshall;
    else
      return Marshall.MARSHALL;
  }

  public String toString()
  {
    if (_name == null)
      return "MBean[]";
    else
      return "MBean[" + _name.getCanonicalName() + "]";
  }

  static class Marshall {
    static final Marshall MARSHALL = new Marshall();
    
    public Object marshall(Object value)
    {
      return value;
    }
  }

  static class IntMarshall extends Marshall {
    static final Marshall MARSHALL = new IntMarshall();
    
    public Object marshall(Object value)
    {
      if (value instanceof Integer)
        return value;
      else if (value instanceof Number)
        return new Integer(((Number) value).intValue());
      else if (value == null)
        return new Integer(0);
      else {
        try {
          return new Integer(Integer.parseInt(String.valueOf(value)));
        } catch (Exception e) {
          return new Integer(0);
        }
      }
    }
  }

  static class LongMarshall extends Marshall {
    static final Marshall MARSHALL = new LongMarshall();
    
    public Object marshall(Object value)
    {
      if (value instanceof Long)
        return value;
      else if (value instanceof Number)
        return new Long(((Number) value).longValue());
      else if (value == null)
        return new Long(0);
      else {
        try {
          return new Long(Long.parseLong(String.valueOf(value)));
        } catch (Exception e) {
          return new Long(0);
        }
      }
    }
  }

  static class StringMarshall extends Marshall {
    static final Marshall MARSHALL = new StringMarshall();
    
    public Object marshall(Object value)
    {
      if (value == null)
        return null;
      else
        return value.toString();
    }
  }

  static {
    _marshallMap.put("int", IntMarshall.MARSHALL);
    _marshallMap.put("java.lang.Integer", IntMarshall.MARSHALL);
    
    _marshallMap.put("long", LongMarshall.MARSHALL);
    _marshallMap.put("java.lang.Long", LongMarshall.MARSHALL);
    
    _marshallMap.put("java.lang.String", StringMarshall.MARSHALL);
  }
}
