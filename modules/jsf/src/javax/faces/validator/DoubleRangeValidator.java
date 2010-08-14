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

package javax.faces.validator;

import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.context.*;

public class DoubleRangeValidator
  implements Validator, StateHolder
{
  public static final String MAXIMUM_MESSAGE_ID
    = "javax.faces.validator.DoubleRangeValidator.MAXIMUM";
  public static final String MINIMUM_MESSAGE_ID
    = "javax.faces.validator.DoubleRangeValidator.MINIMUM";
  public static final String NOT_IN_RANGE_MESSAGE_ID
    = "javax.faces.validator.DoubleRangeValidator.NOT_IN_RANGE";
  public static final String TYPE_MESSAGE_ID
    = "javax.faces.validator.DoubleRangeValidator.TYPE";
  public static final String VALIDATOR_ID
    = "javax.faces.DoubleRange";

  private double _minimum = Double.MIN_VALUE;
  private double _maximum = Double.MAX_VALUE;
  private boolean _isTransient;

  public DoubleRangeValidator()
  {
  }

  public DoubleRangeValidator(double maximum)
  {
    _maximum = maximum;
  }

  public DoubleRangeValidator(double maximum, double minimum)
  {
    _maximum = maximum;
    _minimum = minimum;
  }

  public double getMinimum()
  {
    return _minimum;
  }

  public void setMinimum(double min)
  {
    _minimum = min;
  }

  public double getMaximum()
  {
    return _maximum;
  }

  public void setMaximum(double max)
  {
    _maximum = max;
  }

  public boolean isTransient()
  {
    return _isTransient;
  }

  public void setTransient(boolean isTransient)
  {
    _isTransient = isTransient;
  }

  public void validate(FacesContext context,
                       UIComponent component,
                       Object value)
    throws ValidatorException
  {
    if (context == null || component == null)
      throw new NullPointerException();

    if (value == null)
      return;

    if (value instanceof String) {
      try {
        value = Double.parseDouble((String) value);
      } catch (Exception e) {
        String summary = Util.l10n(context, TYPE_MESSAGE_ID,
                                   "{0}: Validation Error: Value is not of the correct type.",
                                   Util.getLabel(context, component));

        String detail = summary;

        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                            summary,
                                            detail);

        throw new ValidatorException(msg, e);
      }
    }

    if (! (value instanceof Number)) {
      String summary = Util.l10n(context, TYPE_MESSAGE_ID,
                                 "{0}: Validation Error: Value is not of the correct type.",
                                 Util.getLabel(context, component));

      String detail = summary;

      FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                          summary,
                                          detail);

      throw new ValidatorException(msg);
    }

    Number v = (Number) value;

    if (v.doubleValue() < getMinimum()) {
      String summary = Util.l10n(context, MINIMUM_MESSAGE_ID,
                                 "{1}: Validation Error: Value is less than allowable minimum of '{0}'.",
                                 getMinimum(),
                                 Util.getLabel(context, component));

      String detail = summary;

      FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                          summary,
                                          detail);

      throw new ValidatorException(msg);
    }

    if (getMaximum() < v.doubleValue()) {
      String summary = Util.l10n(context, MAXIMUM_MESSAGE_ID,
                                 "{1}: Validation Error: Value is greater than allowable maximum of '{0}'.",
                                 getMaximum(),
                                 Util.getLabel(context, component));

      String detail = summary;

      FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                          summary,
                                          detail);

      throw new ValidatorException(msg);
    }
  }

  public Object saveState(FacesContext context)
  {
    return new Object[] { _minimum, _maximum };
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object []stateV = (Object []) state;

    _minimum = (Double) stateV[0];
    _maximum = (Double) stateV[1];
  }

  public int hashCode()
  {
    return 65521 * (int) _minimum + (int) _maximum;
  }
  
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof DoubleRangeValidator))
      return false;

    DoubleRangeValidator validator = (DoubleRangeValidator) o;

    return (_minimum == validator._minimum
            && _maximum == validator._maximum);
  }
}
