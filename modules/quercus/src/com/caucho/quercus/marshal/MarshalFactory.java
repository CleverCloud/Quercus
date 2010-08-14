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

package com.caucho.quercus.marshal;

import com.caucho.quercus.env.*;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.lib.file.BinaryInput;
import com.caucho.quercus.lib.file.BinaryOutput;
import com.caucho.quercus.lib.regexp.Ereg;
import com.caucho.quercus.lib.regexp.Eregi;
import com.caucho.quercus.lib.regexp.Regexp;
import com.caucho.quercus.lib.regexp.UnicodeEreg;
import com.caucho.quercus.lib.regexp.UnicodeEregi;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Code for marshaling (PHP to Java) and unmarshaling (Java to PHP) arguments.
 */
public class MarshalFactory {
  private static final HashMap<Class<?>,Marshal> _marshalMap
    = new HashMap<Class<?>,Marshal>();
  
  protected ModuleContext _moduleContext;

  public MarshalFactory(ModuleContext moduleContext)
  {
    _moduleContext = moduleContext;
  }

  public Marshal create(Class<?> argType)
  {
    return create(argType, false);
  }

  public Marshal create(Class<?> argType,
                        boolean isNotNull)
  {
    return create(argType, isNotNull, false);
  }

  public Marshal create(Class<?> argType,
                        boolean isNotNull,
                        boolean isNullAsFalse)
  {
    Marshal marshal;
    
    marshal = _marshalMap.get(argType);
    
    // optimized cases, new types should be added to JavaMarshal
    // XXX: put the static classes in _marshalMap

    if (marshal != null) {
      
    }
    else if (boolean.class.equals(argType)) {
      marshal = BooleanMarshal.MARSHAL;
    }
    else if (Boolean.class.equals(argType)) {
      marshal = BooleanObjectMarshal.MARSHAL;
    }
    else if (byte.class.equals(argType)) {
      marshal = ByteMarshal.MARSHAL;
    }
    else if (Byte.class.equals(argType)) {
      marshal = ByteObjectMarshal.MARSHAL;
    }
    else if (short.class.equals(argType)) {
      marshal = ShortMarshal.MARSHAL;
    }
    else if (Short.class.equals(argType)) {
      marshal = ShortObjectMarshal.MARSHAL;
    }
    else if (int.class.equals(argType)) {
      marshal = IntegerMarshal.MARSHAL;
    }
    else if (Integer.class.equals(argType)) {
      marshal = IntegerObjectMarshal.MARSHAL;
    }
    else if (long.class.equals(argType)) {
      marshal = LongMarshal.MARSHAL;
    }
    else if (Long.class.equals(argType)) {
      marshal = LongObjectMarshal.MARSHAL;
    }
    else if (LongValue.class.equals(argType)) {
      marshal = LongValueMarshal.MARSHAL;
    }
    else if (float.class.equals(argType)) {
      marshal = FloatMarshal.MARSHAL;
    }
    else if (Float.class.equals(argType)) {
      marshal = FloatObjectMarshal.MARSHAL;
    }
    else if (double.class.equals(argType)) {
      marshal = DoubleMarshal.MARSHAL;
    }
    else if (Double.class.equals(argType)) {
      marshal = DoubleObjectMarshal.MARSHAL;
    }
    else if (DoubleValue.class.equals(argType)) {
      marshal = DoubleValueMarshal.MARSHAL;
    }
    else if (BigDecimal.class.equals(argType)) {
      marshal = BigDecimalMarshal.MARSHAL;
    }
    else if (BigInteger.class.equals(argType)) {
      marshal = BigIntegerMarshal.MARSHAL;
    }
    else if (char.class.equals(argType)) {
      marshal = CharacterMarshal.MARSHAL;
    }
    else if (Character.class.equals(argType)) {
      marshal = CharacterObjectMarshal.MARSHAL;
    }
    else if (Path.class.equals(argType)) {
      marshal = PathMarshal.MARSHAL;
    }
    else if (StringValue.class.equals(argType)) {
      marshal = StringValueMarshal.MARSHAL;
    }
    else if (UnicodeValue.class.equals(argType)) {
      marshal = UnicodeValueMarshal.MARSHAL;
    }
    else if (BinaryValue.class.equals(argType)) {
      marshal = BinaryValueMarshal.MARSHAL;
    }
    else if (BinaryBuilderValue.class.equals(argType)) {
      marshal = BinaryValueMarshal.MARSHAL;
    }
    else if (InputStream.class.equals(argType)) {
      marshal = InputStreamMarshal.MARSHAL;
    }
    else if (BinaryInput.class.equals(argType)) {
      marshal = BinaryInputMarshal.MARSHAL;
    }
    else if (BinaryOutput.class.equals(argType)) {
      marshal = BinaryOutputMarshal.MARSHAL;
    }
    else if (ArrayValue.class.equals(argType)) {
      marshal = ArrayValueMarshal.MARSHAL;
    }
    else if (Value.class.equals(argType)) {
      marshal = ValueMarshal.MARSHAL;
    }
    else if (Value.class.isAssignableFrom(argType)) {
      marshal = new ExtValueMarshal(argType);
    }
    else if (void.class.equals(argType)) {
      marshal = VoidMarshal.MARSHAL;
    }
    else if (Calendar.class.equals(argType)) {
      marshal = CalendarMarshal.MARSHAL;
    }
    else if (Date.class.equals(argType)) {
      marshal = DateMarshal.MARSHAL;
    }
    else if (Regexp.class.equals(argType)) {
      marshal = RegexpMarshal.MARSHAL;
    }
    else if (Regexp[].class.equals(argType)) {
      marshal = RegexpArrayMarshal.MARSHAL;
    }
    else if (Ereg.class.equals(argType)) {
      marshal = EregMarshal.MARSHAL;
    }
    else if (Eregi.class.equals(argType)) {
      marshal = EregiMarshal.MARSHAL;
    }
    else if (UnicodeEreg.class.equals(argType)) {
      marshal = UnicodeEregMarshal.MARSHAL;
    }
    else if (UnicodeEregi.class.equals(argType)) {
      marshal = UnicodeEregiMarshal.MARSHAL;
    }
    else if (URL.class.equals(argType)) {
      marshal = URLMarshal.MARSHAL;
    }
    else if (byte[].class.equals(argType)) {
      marshal = JavaByteArrayMarshal.MARSHAL;
    }
    else if (char[].class.equals(argType)) {
      marshal = JavaCharacterArrayMarshal.MARSHAL;
    }
    else if (argType.isArray()) {
      marshal = new JavaArrayMarshal(argType);
    }
    else if (Map.class.isAssignableFrom(argType)) {
      String typeName = argType.getName();

      JavaClassDef javaDef = _moduleContext.getJavaClassDefinition(typeName);

      marshal = new JavaMapMarshal(javaDef, isNotNull, isNullAsFalse);
    }
    else if (List.class.isAssignableFrom(argType)) {
      String typeName = argType.getName();

      JavaClassDef javaDef = _moduleContext.getJavaClassDefinition(typeName);

      marshal = new JavaListMarshal(javaDef, isNotNull, isNullAsFalse);
    }
    else if (Collection.class.isAssignableFrom(argType)) {
      String typeName = argType.getName();

      JavaClassDef javaDef = _moduleContext.getJavaClassDefinition(typeName);

      marshal = new JavaCollectionMarshal(javaDef, isNotNull, isNullAsFalse);
    }
    else if (Enum.class.isAssignableFrom(argType)) {
      marshal = new EnumMarshal(argType);
    }
    else {
      String typeName = argType.getName();

      JavaClassDef javaDef = _moduleContext.getJavaClassDefinition(typeName);

      marshal = new JavaMarshal(javaDef, isNotNull, isNullAsFalse);
    }

    if (!isNullAsFalse)
      return marshal;
    else {
      if (Value.class.equals(argType)
          || Boolean.class.equals(argType)
          || Byte.class.equals(argType)
          || Short.class.equals(argType)
          || Integer.class.equals(argType)
          || Long.class.equals(argType)
          || Float.class.equals(argType)
          || Double.class.equals(argType)
          || Character.class.equals(argType)) {

        String shortName = argType.getSimpleName();
        throw new UnsupportedOperationException(
          "@ReturnNullAsFalse cannot be used with return type `"
          + shortName + "'");
      }

      return new NullAsFalseMarshal(marshal);
    }
  }

  public Marshal createReference()
  {
    return ReferenceMarshal.MARSHAL;
  }

  public Marshal createValuePassThru()
  {
    return ValueMarshal.MARSHAL_PASS_THRU;
  }
  
  public Marshal createExpectString()
  {
    return ExpectMarshal.MARSHAL_EXPECT_STRING;
  }
  
  public Marshal createExpectNumeric()
  {
    return ExpectMarshal.MARSHAL_EXPECT_NUMERIC;
  }
  
  public Marshal createExpectBoolean()
  {
    return ExpectMarshal.MARSHAL_EXPECT_BOOLEAN;
  }
  
  static {
    _marshalMap.put(String.class, StringMarshal.MARSHAL);
    _marshalMap.put(Callable.class, CallableMarshal.MARSHAL);
    _marshalMap.put(Class.class, ClassMarshal.MARSHAL);
  }
}

