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

package com.caucho.jsf.lifecycle;

import com.caucho.util.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.lang.reflect.*;

import javax.el.*;
import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.context.*;
import javax.faces.event.*;
import javax.faces.lifecycle.*;
import javax.faces.render.*;

import javax.servlet.http.*;
import javax.servlet.jsp.JspException;
import javax.servlet.ServletException;

import com.caucho.jsf.application.*;

/**
 * The default lifecycle implementation
 */
public class LifecycleImpl extends Lifecycle
{
  private static final L10N L = new L10N(LifecycleImpl.class);
  private static final Logger log
    = Logger.getLogger(LifecycleImpl.class.getName());
  
  private ArrayList<PhaseListener> _phaseList = new ArrayList<PhaseListener>();
  private PhaseListener []_phaseListeners = new PhaseListener[0];
  
  public LifecycleImpl()
  {
  }

  public void addPhaseListener(PhaseListener listener)
  {
    if (listener == null)
      throw new NullPointerException();
    
    synchronized (_phaseList) {
      _phaseList.add(listener);
      _phaseListeners = new PhaseListener[_phaseList.size()];
      _phaseList.toArray(_phaseListeners);
    }
  }
  
  public PhaseListener []getPhaseListeners()
  {
    return _phaseListeners;
  }
  
  public void removePhaseListener(PhaseListener listener)
  {
    if (listener == null)
      throw new NullPointerException();
    
    synchronized (_phaseList) {
      _phaseList.remove(listener);
      _phaseListeners = new PhaseListener[_phaseList.size()];
      _phaseList.toArray(_phaseListeners);
    }
  }

  public void execute(FacesContext context)
    throws FacesException
  {
    boolean isFiner = log.isLoggable(Level.FINER);
    
    if (context.getResponseComplete() || context.getRenderResponse())
      return;

    beforePhase(context, PhaseId.RESTORE_VIEW);

    try {
      if (isFiner)
        log.finer("JSF[] before restore view");
      
      restoreView(context);
    } finally {
      afterPhase(context, PhaseId.RESTORE_VIEW);
    }

    if (context.getResponseComplete() || context.getRenderResponse())
      return;

    UIViewRoot viewRoot = context.getViewRoot();

    beforePhase(context, PhaseId.APPLY_REQUEST_VALUES);

    try {
      if (isFiner)
        log.finer(context.getViewRoot() + " before process decodes");
      
      viewRoot.processDecodes(context);
    }
    catch (RuntimeException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
    finally {
      afterPhase(context, PhaseId.APPLY_REQUEST_VALUES);
    }

    //
    // Process Validations (processValidators)
    //
    
    if (context.getResponseComplete() || context.getRenderResponse())
      return;
    
    beforePhase(context, PhaseId.PROCESS_VALIDATIONS);

    try {
      if (isFiner)
        log.finer(context.getViewRoot() + " before process validators");
      
      viewRoot.processValidators(context);
    } finally {
      afterPhase(context, PhaseId.PROCESS_VALIDATIONS);
    }

    //
    // Update Model Values (processUpdates)
    //
    
    if (context.getResponseComplete() || context.getRenderResponse())
      return;
    
    beforePhase(context, PhaseId.UPDATE_MODEL_VALUES);

    try {
      if (isFiner)
        log.finer(context.getViewRoot() + " before process updates");
      
      viewRoot.processUpdates(context);
    } catch (RuntimeException e) {
      if (sendError(context, "processUpdates", e))
        return;
    } finally {
      afterPhase(context, PhaseId.UPDATE_MODEL_VALUES);
    }

    //
    // Invoke Application (processApplication)
    //
    
    if (context.getResponseComplete() || context.getRenderResponse())
      return;
    
    beforePhase(context, PhaseId.INVOKE_APPLICATION);

    try {
      if (isFiner)
        log.finer(context.getViewRoot() + " before process application");
      
      viewRoot.processApplication(context);
    } finally {
      afterPhase(context, PhaseId.INVOKE_APPLICATION);
    }
  }

  private void restoreView(FacesContext context)
    throws FacesException
  {
    Application app = context.getApplication();

    if (app instanceof ApplicationImpl)
      ((ApplicationImpl) app).initRequest();
    
    ViewHandler view = app.getViewHandler();

    view.initView(context);

    UIViewRoot viewRoot = context.getViewRoot();
    
    if (viewRoot != null) {
      ExternalContext extContext = context.getExternalContext();
      
      viewRoot.setLocale(extContext.getRequestLocale());

      doSetBindings(context.getELContext(), viewRoot);

      return;
    }

    String viewId = calculateViewId(context);

    String renderKitId = view.calculateRenderKitId(context);

    RenderKitFactory renderKitFactory
      = (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
    
    RenderKit renderKit = renderKitFactory.getRenderKit(context, renderKitId);

    ResponseStateManager stateManager = renderKit.getResponseStateManager();

    if (stateManager.isPostback(context)) {
      viewRoot = view.restoreView(context,  viewId);

      if (viewRoot != null) {
        doSetBindings(context.getELContext(), viewRoot);
      }
      else {
        // XXX: backward compat issues with ViewHandler and StateManager

        // throw new ViewExpiredException(L.l("{0} is an expired view", viewId));

        context.renderResponse();
      
        viewRoot = view.createView(context, viewId);

        context.setViewRoot(viewRoot);
      }
      
      context.setViewRoot(viewRoot);
    }
    else {
      context.renderResponse();
      
      viewRoot = view.createView(context, viewId);

      context.setViewRoot(viewRoot);
    }
  }

  private void doSetBindings(ELContext elContext, UIComponent component)
  {
    if (component == null)
      return;

    ValueExpression binding = component.getValueExpression("binding");

    if (binding != null)
      binding.setValue(elContext, component);

    Iterator<UIComponent> iter = component.getFacetsAndChildren();
    while (iter.hasNext())
      doSetBindings(elContext, iter.next());
  }

  private String calculateViewId(FacesContext context)
  {
     Map map = context.getExternalContext().getRequestMap();

    String viewId = (String)
      map.get("javax.servlet.include.path_info");

    if (viewId == null)
      viewId = context.getExternalContext().getRequestPathInfo();

    if (viewId == null)
      viewId = (String) map.get("javax.servlet.include.servlet_path");

    if (viewId == null)
      viewId = context.getExternalContext().getRequestServletPath();

    return viewId;
  }
  
  public void render(FacesContext context)
    throws FacesException
  {
    if (context.getResponseComplete())
      return;

    Application app = context.getApplication();
    ViewHandler view = app.getViewHandler();

    beforePhase(context, PhaseId.RENDER_RESPONSE);

    try {
      if (log.isLoggable(Level.FINER))
        log.finer(context.getViewRoot() + " before render view");
      
      view.renderView(context, context.getViewRoot());
    } catch (java.io.IOException e) {
      if (sendError(context, "renderView", e))
        return;
      
      throw new FacesException(e);
    } catch (RuntimeException e) {
      if (sendError(context, "renderView", e))
        return;

      throw e;
    } finally {
      afterPhase(context, PhaseId.RENDER_RESPONSE);

      logMessages(context);
    }
  }

  private void beforePhase(FacesContext context, PhaseId phase)
  {
    for (int i = 0; i < _phaseListeners.length; i++) {
      PhaseListener listener = _phaseListeners[i];
      PhaseId id = listener.getPhaseId();

      if (id == phase || id == PhaseId.ANY_PHASE) {
        PhaseEvent event = new PhaseEvent(context, phase, this);

        listener.beforePhase(event);
      }
    }
  }

  private void afterPhase(FacesContext context, PhaseId phase)
  {
    for (int i = _phaseListeners.length - 1; i >= 0; i--) {
      PhaseListener listener = _phaseListeners[i];
      PhaseId id = listener.getPhaseId();

      if (phase == id || id == PhaseId.ANY_PHASE) {
        PhaseEvent event = new PhaseEvent(context, phase, this);

        listener.afterPhase(event);
      }
    }
  }

  private void logMessages(FacesContext context)
  {
    UIViewRoot root = context.getViewRoot();
    String viewId = "";

    if (root != null)
      viewId = root.getViewId();

    Iterator<FacesMessage> iter = context.getMessages();

    while (iter != null && iter.hasNext()) {
      FacesMessage msg = iter.next();

      if (log.isLoggable(Level.FINE)) {
        if (msg.getDetail() != null)
          log.fine(viewId + " [ " + msg.getSeverity() + "] " + msg.getSummary() + " " + msg.getDetail());
        else
          log.fine(viewId + " [ " + msg.getSeverity() + "] " + msg.getSummary());
      }
    }
  }

  private boolean sendError(FacesContext context,
                            String lifecycle,
                            Exception e)
  {
    for (Throwable cause = e; cause != null; cause = cause.getCause()) {
      if (cause instanceof DisplayableException) {
        if (e instanceof RuntimeException)
          throw (RuntimeException) e;
        else
          throw new FacesException(e);
      }
      else if (cause instanceof ServletException)
        throw new FacesException(e);
      else if (cause instanceof JspException)
        throw new FacesException(e);
    }

    ExternalContext extContext = context.getExternalContext();
    Object response = extContext.getResponse();

    if (! (response instanceof HttpServletResponse)) {
      context.renderResponse();

      if (e instanceof RuntimeException)
        throw (RuntimeException) e;
      else
        throw new RuntimeException(e);
    }

    log.log(Level.WARNING, e.toString(), e);
    
    HttpServletResponse res = (HttpServletResponse) response;

    try {
      context.renderResponse();
      context.responseComplete();
      
      res.setStatus(500, "JSF Exception");
      res.setContentType("text/html");

      PrintWriter out = res.getWriter();

      out.println("<body>");

      out.println("<h3>JSF exception detected in " + lifecycle + " phase</h3>");

      String msg = e.getMessage();
      out.println("<span style='color:red;font:bold'>" + Html.escapeHtml(msg) + "</span><br/>");

      out.println("<h3>Context: " + context.getViewRoot() + "</h3>");
      out.println("<code><pre>");

      String errorId = null;
      
      if (e instanceof FacesException && msg.startsWith("id=")) {
        int p = msg.indexOf(' ');
        errorId = msg.substring(3, p);
      }

      printComponentTree(out, errorId, context, context.getViewRoot(), 0);
      
      out.println("</pre></code>");

      if (! Alarm.isTest()) {
        out.println("<h3>Stack Trace</h3>");
        out.println("<pre>");
        if (e.getCause() != null)
          e.getCause().printStackTrace(out);
        else
          e.printStackTrace(out);
        out.println("</pre>");
      }
      
      out.println("</body>");
      
      // clear, so we don't just loop
      Application app = context.getApplication();
    
      ViewHandler view = app.getViewHandler();
      
      UIViewRoot viewRoot = context.getViewRoot();
      
      viewRoot = view.createView(context, viewRoot.getViewId());

      context.setViewRoot(viewRoot);

      //view.writeState(context); // XXX: no need to output state, but review.

      return true;
    } catch (IOException e1) {
      throw new RuntimeException(e);
    }
  }

  private void printComponentTree(PrintWriter out,
                                  String errorId,
                                  FacesContext context,
                                  UIComponent comp,
                                  int depth)
  {
    for (int i = 0; i < depth; i++)
      out.print(' ');

    boolean isError = false;
    if (errorId != null && errorId.equals(comp.getClientId(context))) {
      isError = true;
      out.print("<span style='color:red'>");
    }

    out.print("&lt;" + comp.getClass().getSimpleName());
    if (comp.getId() != null)
      out.print(" id=\"" + comp.getId() + "\"");

    for (Method method : comp.getClass().getMethods()) {
      if (! method.getName().startsWith("get")
          && ! method.getName().startsWith("is"))
        continue;
      else if (method.getParameterTypes().length != 0)
        continue;

      String name;

      if (method.getName().startsWith("get"))
        name = method.getName().substring(3);
      else if (method.getName().startsWith("is"))
        name = method.getName().substring(2);
      else
        continue;

      // XXX: getURL
      name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
      
      ValueExpression expr = comp.getValueExpression(name);

      Class type = method.getReturnType();

      if (expr != null) {
        out.print(" " + name + "=\"" + expr.getExpressionString() + "\"");
      }
      else if (method.getDeclaringClass().equals(UIComponent.class)
               || method.getDeclaringClass().equals(UIComponentBase.class)) {
      }
      else if (name.equals("family")) {
      }
      else if (String.class.equals(type)) {
        try {
          Object value = method.invoke(comp);

          if (value != null)
            out.print(" " + name + "=\"" + value + "\"");
        } catch (Exception e) {
        }
      }
    }

    int facetCount = comp.getFacetCount();
    int childCount = comp.getChildCount();

    if (facetCount == 0 && childCount == 0) {
      out.print("/>");

      if (isError)
        out.print("</span>");

      out.println();
      return;
    }
    out.println(">");

    if (isError)
      out.print("</span>");

    for (int i = 0; i < childCount; i++) {
      printComponentTree(out, errorId, context,
                         comp.getChildren().get(i), depth + 1);
    }
    
    for (int i = 0; i < depth; i++)
      out.print(' ');

    if (isError)
      out.print("<span style='color:red'>");
    
    out.println("&lt;/" + comp.getClass().getSimpleName() + ">");

    if (isError)
      out.print("</span>");
  }

  public String toString()
  {
    return "DefaultLifecycleImpl[]";
  }
}
