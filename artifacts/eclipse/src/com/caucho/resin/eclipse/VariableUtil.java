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

package com.caucho.resin.eclipse;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IStringVariable;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.VariablesPlugin;

class VariableUtil {
  public static void setVariable(String key, String value)
    throws CoreException
  {
    IStringVariableManager manager = 
      VariablesPlugin.getDefault().getStringVariableManager();

    IValueVariable variable = manager.getValueVariable(key);

    if (variable == null) {
      variable = manager.newValueVariable(key, key);
      manager.addVariables(new IValueVariable[] {variable});
    }

    variable.setValue(value);
  }

  public static String getDynamicVariable(String key)
    throws CoreException
  {
    return getDynamicVariable(key, null);
  }

  public static String getDynamicVariable(String key, String arg)
    throws CoreException
  {
    IStringVariableManager manager = 
      VariablesPlugin.getDefault().getStringVariableManager();

    IDynamicVariable variable = manager.getDynamicVariable(key);

    if (variable == null)
      return null;

    return variable.getValue(arg);
  }

  public static String getVariable(String key)
    throws CoreException
  {
    IStringVariableManager manager = 
      VariablesPlugin.getDefault().getStringVariableManager();

    IValueVariable variable = manager.getValueVariable(key);

    if (variable == null)
      return null;

    return variable.getValue();
  }

  public static void dumpVariables()
    throws CoreException
  {
    IStringVariableManager manager = 
      VariablesPlugin.getDefault().getStringVariableManager();

    IValueVariable[] valueVariables = manager.getValueVariables();

    System.out.println("Value Variables");

    for (IValueVariable var : valueVariables) {
      System.out.println(var.getName() + ": " + var.getValue());
    }

    IStringVariable[] stringVariables = manager.getVariables();

    System.out.println("String Variables");

    for (IStringVariable var : stringVariables) {
      System.out.println(var.getName());
    }

    IDynamicVariable [] dynVariables = manager.getDynamicVariables();

    System.out.println("Dynamic Variables");

    for (IDynamicVariable var : dynVariables) {
      System.out.println(var.getName());
    }
  }
}
