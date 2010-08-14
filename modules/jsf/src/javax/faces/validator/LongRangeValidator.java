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

public class LongRangeValidator
  implements Validator, StateHolder
{
  public static final String MAXIMUM_MESSAGE_ID
    = "javax.faces.validator.LongRangeValidator.MAXIMUM";
  public static final String MINIMUM_MESSAGE_ID
    = "javax.faces.validator.LongRangeValidator.MINIMUM";
  public static final String NOT_IN_RANGE_MESSAGE_ID
    = "javax.faces.validator.LongRangeValidator.NOT_IN_RANGE";
  public static final String TYPE_MESSAGE_ID
    = "javax.faces.validator.LongRangeValidator.TYPE";
  public static final String VALIDATOR_ID
    = "javax.faces.LongRange";

  private long _minimum = Long.MIN_VALUE;
  private long _maximum = Long.MAX_VALUE;
  private boolean _isTransient;

  public LongRangeValidator()
  {
  }

  public LongRangeValidator(long maximum)
  {
    _maximum = maximum;
  }

  public LongRangeValidator(long maximum, long minimum)
  {
    _maximum = maximum;
    _minimum = minimum;
  }

  public long getMinimum()
  {
    return _minimum;
  }

  public void setMinimum(long min)
  {
    _minimum = min;
  }

  public long getMaximum()
  {
    return _maximum;
  }

  public void setMaximum(long max)
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
        value = Long.parseLong((String) value);
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

    if (v.longValue() < getMinimum()) {
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

    if (getMaximum() < v.longValue()) {
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

    _minimum = (Long) stateV[0];
    _maximum = (Long) stateV[1];
  }

  public int hashCode()
  {
    return 65521 * (int) _minimum + (int) _maximum;
  }
  
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof LongRangeValidator))
      return false;

    LongRangeValidator validator = (LongRangeValidator) o;

    return (_minimum == validator._minimum
            && _maximum == validator._maximum);
  }
}
