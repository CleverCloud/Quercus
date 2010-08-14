/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package javax.faces.component;

import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.validator.*;

public interface EditableValueHolder extends ValueHolder
{
  public Object getSubmittedValue();

  public void setSubmittedValue(Object submittedValue);

  public boolean isLocalValueSet();

  public void setLocalValueSet(boolean isSet);

  public boolean isValid();
  
  public void setValid(boolean valid);

  public boolean isRequired();

  public void setRequired(boolean required);

  public boolean isImmediate();

  public void setImmediate(boolean immediate);

  /**
   * @deprecated
   */
  public MethodBinding getValidator();

  /**
   * @deprecated
   */
  public void setValidator(MethodBinding validator);

  /**
   * @deprecated
   */
  public MethodBinding getValueChangeListener();

  /**
   * @deprecated
   */
  public void setValueChangeListener(MethodBinding binding);

  public void addValidator(Validator validator);

  public void removeValidator(Validator validator);

  public Validator []getValidators();

  public void addValueChangeListener(ValueChangeListener listener);
  
  public void removeValueChangeListener(ValueChangeListener listener);
  
  public ValueChangeListener []getValueChangeListeners();
}

