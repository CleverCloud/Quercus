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
 * @author Alex Rojkov
 */

package com.caucho.jsf.dev;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.util.L10N;
import com.caucho.jsf.webapp.*;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.FactoryFinder;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class JsfDeveloperAidServlet
  extends GenericServlet
{

  private static final Logger log
    = Logger.getLogger(FacesServletImpl.class.getName());

  private static final L10N L
    = new L10N(com.caucho.jsf.dev.JsfDeveloperAid.class);

  private ServletContext _webApp;

  private FacesContextFactory _facesContextFactory;
  private Lifecycle _lifecycle;

  public void init(ServletConfig config)
    throws ServletException
  {
    _webApp = config.getServletContext();

    _facesContextFactory = (FacesContextFactory)
      FactoryFinder.getFactory(FactoryFinder.FACES_CONTEXT_FACTORY);

    LifecycleFactory lifecycleFactory = (LifecycleFactory)
      FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);

    String name = config.getInitParameter("javax.faces.LIFECYCLE_ID");

    if (name == null)
      name = _webApp.getInitParameter("javax.faces.LIFECYCLE_ID");

    if (name == null)
      name = LifecycleFactory.DEFAULT_LIFECYCLE;

    _lifecycle = lifecycleFactory.getLifecycle(name);
  }

  private void printControls(PrintWriter out,
                             HttpServletRequest request,
                             HttpSession session)
  {
    if (request.getParameter("viewId") != null)
      out.println(" <br/><br/><a href=\"" +
                  request.getContextPath() +
                  "/caucho.jsf.developer.aid" +
                  (session == null ? "" : ";jsessionid=" + session.getId()) +
                  "\"><em>" +
                  "Show Available Views" +
                  "</em></a>");

    out.println(" <br/><br/><a href=\"" +
                request.getContextPath() +
                "/caucho.jsf.developer.aid" +
                (session == null ? "" : ";jsessionid=" + session.getId()) +
                "?save=\"><em>" +
                "Save to file" +
                "</em></a>");

    out.println(
      " <br/><br/><form enctype=\"multipart/form-data\" method=\"POST\"><em>Load from file</em><input name=\"file\" type=\"file\"/><input type=\"submit\" value=\"Upload\"/></form>");

  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    ServletInputStream in = request.getInputStream();

    byte []data = null;
    int position = 0;

    Scan scan = new Scan();

    int count;

    boolean found = false;
    byte []buffer = new byte[4096];
    while ((count = in.read(buffer, position, buffer.length - position)) >
           0) {
      if (found)
        continue;

      if (count + position == buffer.length) {
        final byte []newData = new byte[buffer.length * 2];
        System.arraycopy(buffer, 0, newData, 0, buffer.length);
        buffer = newData;
      }

      if (scan.boundaryEnd == -1) {
        for (int i = 0; i < position + count; i++) {
          if (buffer[i] == '\n') {
            scan.boundaryEnd = i - 1;
            scan.pointer = i;
            break;
          }
        }
      }

      position = position + count;

      found = find(buffer, scan, position);

      if (found) {
        data = buffer;
        buffer = new byte[256];
        position = 0;
      }
    }

    if (found) {
      int start = -1;

      for (int i = scan.boundaryEnd; i < data.length; i++) {
        if (data[i] == '\n' && data[i - 2] == '\n') {
          start = i;

          break;
        }
      }

      Hessian2Input input = new Hessian2Input(new ByteArrayInputStream(data,
                                                                       start +
                                                                       1,
                                                                       scan
                                                                         .pointer -
                                                                                  start -
                                                                                  3));

      Object obj = input.readObject();

      HttpSession session = request.getSession(true);

      session.setAttribute("caucho.jsf.developer.aid", obj);
    }
    else {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  private class Scan {
    private int pointer = 0;
    private int boundaryIdx = 0;
    private int boundaryEnd = -1;
  }

  public boolean find(byte []data, Scan scan, int limit)
  {
    for (; scan.pointer < limit; scan.pointer++) {
      for (; scan.boundaryIdx < scan.boundaryEnd; scan.boundaryIdx++) {
        if (limit >= (scan.pointer + scan.boundaryIdx)) {
          if (data[scan.pointer + scan.boundaryIdx] !=
              data[scan.boundaryIdx]) {
            scan.boundaryIdx = 0;

            break;
          }
        }
        else
          break;
      }
      if (scan.boundaryIdx == scan.boundaryEnd)
        return true;
    }

    return false;
  }

  public void service(ServletRequest req, ServletResponse res)
    throws IOException, ServletException
  {
    final HttpServletRequest request = (HttpServletRequest) req;
    final HttpServletResponse response = (HttpServletResponse) res;

    final PrintWriter out = res.getWriter();

    if ("POST".equalsIgnoreCase(request.getMethod())) {
      doPost(request, response);
    }

    final HttpSession session = request.getSession();

    final Map<String, com.caucho.jsf.dev.JsfDeveloperAid.JsfRequestSnapshot> aidMap;

    if (session != null)
      aidMap = (Map<String, JsfDeveloperAid.JsfRequestSnapshot>)
        session.getAttribute("caucho.jsf.developer.aid");
    else
      aidMap = null;

    if (req.getParameter("save") != null) {

      if (aidMap != null)
        serveAidMap(aidMap, res);
      else
        response.sendError(HttpServletResponse.SC_NO_CONTENT);

      return;
    }

    final FacesContext oldContext = FacesContext.getCurrentInstance();
    FacesContext context = null;
    try {
      context = _facesContextFactory.getFacesContext(_webApp,
                                                     req,
                                                     res,
                                                     _lifecycle);

      res.setCharacterEncoding("UTF-8");
      res.setContentType("text/html");

      out.println(
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");

      out.println(
        "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");

      if (session == null || aidMap == null) {
        out.println("<body>");

        printControls(out, request, null);

        out.println("</body>");
        out.println("</html>");

        return;
      }

      final String viewId = req.getParameter("viewId");

      if (viewId == null) {
        out.println(" <head>");
        out.print("  <title>");
        out.print("Available Views");
        out.println("</title>");
        out.println(" </head>");
        out.println(" <body>");
        out.println("  <em>Available Views</em>");

        out.println("  <ul>");

        for (String view : aidMap.keySet()) {
          out.println("   <li>");
          out.println("    <a href=\"" +
                      request.getContextPath() +
                      "/caucho.jsf.developer.aid;jsessionid=" +
                      session.getId() +
                      "?viewId=" +
                      view + "\">" +
                      view +
                      "</a>");
          out.println("   </li>");
        }
        out.println(" </ul>");
        printControls(out, request, session);
        out.println(" </body>");
        out.println("</html>");

        return;
      }
      else {
        out.println("<head>");
        out.print("<title>View: ");
        out.print(viewId);
        out.println("</title>");
        out.println("<style type=\"text/css\" media=\"all\">");
        out.println("#header ul {list-style: none;padding: 0;margin: 0;}");
        out.println(
          "#header li {float: left;border: 1px solid;border-bottom-width: 0;margin: 0 0.5em 0 0; font-weight: bold}");
        out.println("#header a {display: block;padding: 0 1em;}");
        out.println(
          "#header #selected {position: relative;top: 1px;background: white; font-weight: normal}");
        out.println("#content {border: 1px solid;clear: both;}");
        out.println("#view {padding: 10px, 10px, 10px, 10px}");
        out.println("table {width: 100%}");
        out.println("td {border: 1px dotted}");

        out.println("h1 {margin: 0;padding: 0 0 1em 0;}");
        out.println("</style>");
        out.println("</head>");
        //
        out.println("<body>");
        out.println(" <div id=\"header\">");
        out.println("  <ul>");

        final String phaseId = req.getParameter("phaseId");
        String valueExpression = req.getParameter("valueExpression");

        if (valueExpression != null)
          valueExpression = URLDecoder.decode(valueExpression, "UTF-8");

        com.caucho.jsf.dev.JsfDeveloperAid.JsfRequestSnapshot snapshot
          = aidMap.get(viewId);

        out.print("   <li" + (phaseId == null ? " id=\"selected\"" : "") + ">");
        out.print("<a href=\"" +
                  request.getContextPath() +
                  "/caucho.jsf.developer.aid;jsessionid=" +
                  session.getId() +
                  "?viewId=" +
                  viewId +
                  "\">" +
                  "Request Info" +
                  "</a>");
        out.println("</li>");

        com.caucho.jsf.dev.JsfDeveloperAid.ViewRoot viewRoot = null;

        com.caucho.jsf.dev.JsfDeveloperAid.ViewRoot []viewRoots
          = snapshot.getPhases();

        for (com.caucho.jsf.dev.JsfDeveloperAid.ViewRoot root : viewRoots) {
          String phase = root.getPhase();

          boolean selected = false;

          if (phase.equals(phaseId) && valueExpression == null) {
            selected = true;
            viewRoot = root;
          }

          out.print("   <li" + (selected ? " id=\"selected\"" : "") + ">");
          out.print("<a href=\"" +
                    request.getContextPath() +
                    "/caucho.jsf.developer.aid;jsessionid=" +
                    session.getId() +
                    "?viewId=" +
                    viewId +
                    "&phaseId=" +
                    phase +
                    "\">" +
                    phase +
                    "</a>");
          out.println("</li>");
        }

        if (valueExpression != null) {
          out.print("   <li id=\"selected\">");
          out.print("<a href=\"" +
                    request.getContextPath() +
                    "/caucho.jsf.developer.aid;jsessionid=" +
                    session.getId() +
                    "?viewId=" + viewId +
                    "&phaseId=" + phaseId +
                    "&valueExpression=" +
                    URLEncoder.encode(valueExpression, "UTF-8") +
                    "\">" +
                    valueExpression +
                    "</a>");
          out.println("</li>");
        }

        out.println("  </ul>");
        out.println(" </div>");
        out.println(" <div id=\"content\">");


        if (valueExpression != null) {
          com.caucho.jsf.dev.JsfDeveloperAid.ViewRoot root = viewRoots[1];

          UIViewRoot uiViewRoot = new UIViewRoot();
          uiViewRoot.setLocale(root.getLocale());
          uiViewRoot.setRenderKitId(root.getRenderKitId());
          //need view for resolving property bundles.
          context.setViewRoot(uiViewRoot);

          printEvaluated(context,
                         request,
                         out,
                         valueExpression,
                         viewId,
                         phaseId);

          out.println("<br/>");

          out.println("<em><a href=\"" +
                      request.getContextPath() +
                      "/caucho.jsf.developer.aid;jsessionid=" +
                      session.getId() +
                      "?viewId=" + viewId +
                      "&phaseId=" + phaseId + "\">" +
                      "<<< Back" +
                      "</a></em>");

        }
        else if (phaseId == null) {
          out.println("<table>");
          out.println("<thead>");
          out.println(
            "<tr><td colspan=\"2\" align=\"center\"><strong>Snoop</strong></td></tr>");
          out.println(
            "<tr><td><strong>Name</strong></td><td><strong>Value</strong></td></tr>");
          out.println("</thead>");

          out.println("<tbody>");

          //headers
          out.println(
            "<tr><td colspan=\"2\" align=\"center\"><em>Headers</em></td></tr>");

          Map<String, String> headers = snapshot.getHeaderMap();
          for (String header : headers.keySet()) {
            String value = headers.get(header);
            out.println("<tr><td><em>" +
                        header +
                        "</em></td><td><em>" +
                        value +
                        "</em></td></tr>");
          }

          //parameters
          out.println(
            "<tr><td colspan=\"2\" align=\"center\"><em>Parameters</em></td></tr>");

          Map<String, String> parameters = snapshot.getParameterMap();
          for (String parameter : parameters.keySet()) {
            String value = parameters.get(parameter);
            out.println("<tr><td><em>" +
                        parameter +
                        "</em></td><td><em>" +
                        value +
                        "</em></td></tr>");
          }


          out.println("</tbody>");
          out.println("</table>");
        }
        else {
          out.println(" <div id=\"view\">");
          printComponentTree(request, out, viewRoot, null, viewId, phaseId, 0);
          out.println(" </div>");
          //snoop
          out.println(" <table>");
          out.println(" <thead>");
          out.println(
            " <tr><td colspan=\"2\" align=\"center\"><strong>Snoop</strong></td></tr>");
          out.println(
            " <tr><td><strong>Name</strong></td><td><strong>Value</strong></td></tr>");
          out.println(" </thead>");
          out.println(" <tbody>");

          //request
          out.println(
            " <tr><td colspan=\"2\" align=\"center\"><em>Request</em></td></tr>");
          printBeanMap(out, viewRoot.getRequestMap());

          //session
          out.println(
            " <tr><td colspan=\"2\" align=\"center\"><em>Session</em></td></tr>");
          printBeanMap(out, viewRoot.getSessionMap());

          //application
          out.println(
            " <tr><td colspan=\"2\" align=\"center\"><em>Application</em></td></tr>");
          printBeanMap(out, viewRoot.getApplicationMap());

          out.println(" </tbody>");
          out.println(" </table>");
        }
        out.println(" </div>");

        printControls(out, request, session);
        out.println(" </body>");
        out.println("</html>");
      }

      out.flush();
    }
    catch (IOException e) {
      throw e;
    }
    finally {
      if (context != null)
        context.release();
    }
  }

  private void printBeanMap(PrintWriter out, Map<String, JsfDeveloperAid.Bean> map)
  {
    for (String key : map.keySet()) {
      JsfDeveloperAid.Bean bean = map.get(key);

      if (bean == null) {
        out.println(" <tr><td><em>" +
                    key +
                    "</em></td><td><em>null</em></td></tr>");
      }
      else if (bean.isSimple()) {
        out.println(" <tr><td><em>" +
                    key +
                    "</em></td><td><em>" +
                    bean.getClassName() + '(' + bean.getToString() + ')' +
                    "</em></td></tr>");
      }
      else if (bean.isArray()) {
        out.println(" <tr><td><em>" +
                    key +
                    "</em></td><td><em>" +
                    bean.getClassName() + '[' + bean.getLength() + ']' +
                    "</em></td></tr>");

      }
      else {
        out.print(" <tr><td><em>" +
                  key +
                  "</em></td><td><em>" +
                  bean.getClassName() + '(' + bean.getToString() + ')');
        out.print(": </em>");

        Map<String, String> beanAttributes = bean.getAttributes();

        if (beanAttributes != null) {
          for (String attribute : beanAttributes.keySet()) {
            out.print("<br/>&nbsp;&nbsp;&nbsp;<em>" +
                      attribute +
                      "</em>=");

            String value = beanAttributes.get(attribute);

            if (value == null) {
              out.print("<em>null</em>");
            }
            else {
              out.print("<em>");
              printEscaped(out, value);
              out.print("</em>");
            }
          }
        }
        out.println("</td></tr>");
      }
    }
  }

  private void serveAidMap(Map<String, com.caucho.jsf.dev.JsfDeveloperAid.JsfRequestSnapshot> aidMap,
                           ServletResponse res)
    throws IOException
  {
    res.setContentType("application/x-caucho-jsf-developer-aid");

    Hessian2Output out = new Hessian2Output(res.getOutputStream());

    out.writeObject(aidMap);

    out.flush();
  }


  private void printEvaluated(FacesContext context,
                              HttpServletRequest request,
                              PrintWriter out,
                              String expression,
                              String viewId,
                              String phaseId
  )
    throws UnsupportedEncodingException
  {
    ELContext elContext = context.getELContext();

    ValueExpression valueExpression = context.getApplication()
      .getExpressionFactory()
      .createValueExpression(elContext, expression, Object.class);

    Object obj = valueExpression.getValue(elContext);

    out.print("<strong>");
    out.print(expression);
    out.print("</strong>=");

    if (obj == null) {
      out.println("null");
      out.println("<br/>");
    }
    else {

      if (obj instanceof String
          || obj instanceof Boolean
          || obj instanceof Character
          || obj instanceof Number
          || obj instanceof Date
        ) {
        out.println(obj.toString());
      }
      else {
        out.print("<strong>");
        out.print(obj.getClass().toString());
        out.print("[" + obj.toString() + "]");
        out.println("</strong>");

        Field []fields = obj.getClass().getDeclaredFields();

        out.println("<br/>");
        for (Field field : fields) {
          try {
            field.setAccessible(true);

            Object value = field.get(obj);
            out.print("&nbsp;&nbsp;&nbsp;");

            printAttribute(request,
                           out,
                           field.getName(),
                           String.valueOf(value),
                           viewId,
                           phaseId);

            out.println("<br/>");
          }
          catch (IllegalAccessException e) {
          }
        }
      }

    }
  }

  private void printComponentTree(HttpServletRequest request,
                                  PrintWriter out,
                                  com.caucho.jsf.dev.JsfDeveloperAid.Component component,
                                  String facetName,
                                  String viewId,
                                  String phaseId,
                                  int depth)
    throws UnsupportedEncodingException
  {
    for (int i = 0; i < depth * 3; i++)
      out.print("&nbsp;");

    out.print("&lt;<strong>" + component.getUiComponentClass() + "</strong>");
    printAttribute(request,
                   out,
                   "clientId",
                   component.getClientId(),
                   viewId,
                   phaseId);

    if (component.isValueHolder()) {
      printAttribute(request,
                     out,
                     "value",
                     component.getValue(),
                     viewId,
                     phaseId);
      printAttribute(request,
                     out,
                     "localValue",
                     component.getLocalValue(),
                     viewId, phaseId);
    }

    if (component.isEditableValueHolder())
      printAttribute(request, out, "submittedValue",
                     component.getSubmittedValue(), viewId, phaseId
      );

    if (facetName != null)
      printAttribute(request,
                     out,
                     "enclosingFacet",
                     facetName,
                     viewId,
                     phaseId);

    Map<String, String> attributes = component.getAttributes();
    if (attributes != null)
      for (String attr : attributes.keySet()) {
        String value = attributes.get(attr);

        if (value != null)
          printAttribute(request, out, attr, value, viewId, phaseId);
      }

    out.println("><br/>");

    List<com.caucho.jsf.dev.JsfDeveloperAid.Component> children
      = component.getChildren();
    Map<String, com.caucho.jsf.dev.JsfDeveloperAid.Component> facets = component
      .getFacets();

    if (children != null)
      for (com.caucho.jsf.dev.JsfDeveloperAid.Component child : children)
        printComponentTree(request,
                           out,
                           child,
                           null,
                           viewId,
                           phaseId,
                           depth + 1);

    if (facets != null)
      for (String facet : facets.keySet())
        printComponentTree(request,
                           out,
                           facets.get(facet),
                           facet,
                           viewId,
                           phaseId,
                           depth + 1);

  }

  private void printAttribute(HttpServletRequest request,
                              PrintWriter out,
                              String name,
                              String value,
                              String viewId,
                              String phaseId)
    throws UnsupportedEncodingException
  {
    out.print(' ');
    out.print("<em>" + name);
    out.print("</em>=\"");
    if (value == null)
      out.print("null");
    else if (value.indexOf("#{") > -1 && value.indexOf("}") > -1) {
      out.print("<a href=\"" +
                request.getContextPath() +
                "/caucho.jsf.developer.aid;jsessionid=" +
                request.getSession().getId() +
                "?viewId=" +
                viewId +
                "&phaseId=" +
                phaseId +
                "&valueExpression=" +
                URLEncoder.encode(value, "UTF-8") +
                "\">" +
                value +
                "</a>");
    }
    else
      printEscaped(out, value);

    out.print("\"");
  }

  public void printEscaped(PrintWriter out, String value)
  {
    char []valueChars = value.toCharArray();

    boolean wasSpace = false;

    for (char valueChar : valueChars) {
      switch (valueChar) {
      case ' ':
        wasSpace = true;

        break;
      case '\n':
        if (wasSpace)
          out.print(' ');

        wasSpace = false;
        out.print('\n');

        break;
      case '\r':
        if (wasSpace)
          out.print(' ');

        wasSpace = false;
        out.print('\r');

        break;
      case '<':
        if (wasSpace)
          out.print(' ');
        wasSpace = false;

        out.print("&lt;");
        break;
      default:
        if (wasSpace)
          out.print(' ');

        wasSpace = false;
        out.write(valueChar);
      }
    }
  }
}