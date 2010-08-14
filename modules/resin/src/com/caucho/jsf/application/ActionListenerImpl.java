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

package com.caucho.jsf.application;

import javax.el.*;

import java.util.logging.*;

import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.context.*;
import javax.faces.el.*;
import javax.faces.event.*;

public class ActionListenerImpl implements ActionListener
{
  private static final Logger log
    = Logger.getLogger(ActionListenerImpl.class.getName());
  
  private static final Object[] NULL_ARG = new Object[0];
  
  public void processAction(ActionEvent event)
    throws AbortProcessingException
  {
    FacesContext context = FacesContext.getCurrentInstance();
    
    context.renderResponse();

    UIComponent comp = event.getComponent();

    String fromAction = null;
    String logicalOutcome = null;

    MethodExpression actionExpr = null;
    MethodBinding action = null;
    
    if (comp instanceof ActionSource2) {
      ActionSource2 actionComp = (ActionSource2) comp;

      actionExpr = actionComp.getActionExpression();
      action = actionComp.getAction();
    }
    else if (comp instanceof ActionSource) {
      ActionSource actionComp = (ActionSource) comp;

      action = actionComp.getAction();
    }

    if (actionExpr != null) {
      fromAction = actionExpr.getExpressionString();

      ELContext elContext = context.getELContext();
      
      Object value;

      // jsf/4401
      value = actionExpr.invoke(elContext, NULL_ARG);

      if (value != null)
        logicalOutcome = value.toString();
    }
    else if (action != null) {
      fromAction = action.getExpressionString();

      Object value = action.invoke(context, NULL_ARG);

      if (value != null)
        logicalOutcome = value.toString();
    }

    Application app = context.getApplication();

    NavigationHandler handler = app.getNavigationHandler();

    if (handler != null)
      handler.handleNavigation(context, fromAction, logicalOutcome);
    else {
      if (log.isLoggable(Level.FINE))
        log.fine("Jsf[" + context.getViewRoot().getViewId() + "] has no navigationHandler for " + app);
    }
  }

  public String toString()
  {
    return "ActionListenerImpl[]";
  }
}
