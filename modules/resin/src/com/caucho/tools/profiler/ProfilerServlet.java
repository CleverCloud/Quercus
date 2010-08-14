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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */


package com.caucho.tools.profiler;

import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.util.Sprintf;
import com.caucho.vfs.XmlWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Html interface to profiling information.
 */
public class ProfilerServlet
  extends HttpServlet
{
  private static final L10N L = new L10N(ProfilerServlet.class);

  // can do this because an instance of Filter is created for each environment
  private final ProfilerManager _profilerManager = ProfilerManager.getLocal();

  public ProfilerManager createProfiler()
  {
    return _profilerManager;
  }

  public void init()
  {
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    handleRequest(req, res);
    handleResponse(req, res);
  }

  protected void doPost(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    handleRequest(req, res);
    handleResponse(req, res);
  }

  protected void handleRequest(HttpServletRequest request,
                               HttpServletResponse response)
    throws ServletException, IOException
  {
  }

  protected void handleResponse(HttpServletRequest request,
                                HttpServletResponse response)
    throws ServletException, IOException
  {
    String format = request.getParameter("format");
    boolean isXml = "xml".equals(format);
    
    response.setContentType("text/html");

    response.setHeader("Cache-Control", "no-cache, post-check=0, pre-check=0");
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Expires", "Thu, 01 Dec 1994 16:00:00 GMT");

    if (isXml)
      writeXml(request, response);
    else
      writeHtml(request, response);
  }

  protected void writeHtml(HttpServletRequest request,
                           HttpServletResponse response)
    throws ServletException, IOException
  {
    response.setContentType("text/html");

    String sort = request.getParameter("sort");

    ProfilerNodeComparator comparator;

    if ("count".equals(sort))
      comparator = new CountComparator();
    else
      comparator = new TimeComparator();

    comparator.setDescending(true);

    XmlWriter out = new XmlWriter(response.getWriter());

    out.setStrategy(XmlWriter.HTML);
    out.setIndenting(false);

    out.println(
      "<!DOCTYPE html  PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");

    String contextPath = request.getContextPath();

    if (contextPath == null || contextPath.length() == 0)
      contextPath = "/";

    String title = L.l("Profiling Results for {0}", contextPath);

    out.startElement("html");

    out.startElement("head");
    out.writeElement("title", title);

    out.startElement("style");
    out.writeAttribute("type", "text/css");

    out.println(
      "h1 { background: #ccddff; margin : 0 -0.5em 0.25em -0.25em; padding: 0.25em 0.25em; }");
    out.println(
      "h2 { background: #ccddff; padding: 0.25em 0.5em; margin : 0.5em -0.5em; }");
    out.println("table { border-collapse : collapse; }");
    out.println("th { background : #c78ae6; border-left : 1px; border-right : 1px}");
    out.println("tr { border-bottom : 1px dotted; }");
    out.println(".number { text-align : right; }");
    out.println("table table tr { border-bottom : none; }");

    out.endElement("style");

    out.endElement("head");

    out.startElement("body");
    out.writeElement("h1", title);

    out.startElement("table");
    out.writeAttribute("border", 0);

    out.startElement("tr");

    out.writeLineElement("th", L.l("Name"));

    out.writeLineElement("th", L.l("Average Time"));
    out.writeLineElement("th", L.l("Min Time"));
    out.writeLineElement("th", L.l("Max Time"));
    out.writeLineElement("th", L.l("Total Time"));

    out.writeLineElement("th", L.l("Invocation Count"));

    out.endElement("tr");

    ProfilerPoint root = _profilerManager.getRoot();
    
    List<ProfilerPoint> children = root.getChildren();
    Collections.sort(children, comparator);
    for (ProfilerPoint child : children)
      display(child, comparator, out, 0);

    out.endElement("table");

    out.endElement("body");

    out.endElement("html");
  }

  private void display(ProfilerPoint node,
                       ProfilerNodeComparator comparator,
                       XmlWriter out,
                       int depth)
  {
    if (node == null)
      return;
    
    List<ProfilerPoint> children = node.getChildren();
    Collections.sort(children, comparator);

    long thisTime = node.getTime();
    long minTime = node.getMinTime();
    long maxTime = node.getMaxTime();

    long childrenTime = 0;

    for (ProfilerPoint child : children) {
      childrenTime += child.getTime();
    }

    long totalTime = childrenTime + thisTime;

    long invocationCount = node.getInvocationCount();
    long averageThisTime;
    long averageTotalTime;
    long averageChildrenTime;

    if (invocationCount <= 0) {
      averageThisTime = -1;
      averageTotalTime = -1;
      averageChildrenTime = -1;
    }
    else {
      averageThisTime = thisTime / invocationCount;
      averageTotalTime = totalTime / invocationCount;
      averageChildrenTime = childrenTime / invocationCount;
    }

    out.startElement("tr");

    out.writeAttribute("class", "level" + depth);

    // Name

    out.startLineElement("td");

    out.startElement("table");
    out.startElement("tr");

    out.startLineElement("td");

    if (depth > 0) {
      for (int i = depth; i > 0; i--) {
        out.write("&nbsp;");
        out.write("&nbsp;");
      }

      out.write("&rarr;");
    }
    out.endLineElement("td");

    out.startLineElement("td");
    out.writeAttribute("class", "text");
    out.writeText(node.getName());
    out.endLineElement("td");

    out.endElement("tr");
    out.endElement("table");

    out.endLineElement("td");

    out.startLineElement("td");
    out.writeAttribute("class", "number");
    if (averageThisTime < 0)
      out.write("&nbsp;");
    else {
      String averageTimeString = createTimeString(averageTotalTime, averageThisTime, averageChildrenTime);
      out.writeAttribute("title", averageTimeString);
      printTime(out, averageTotalTime);
    }

    out.endLineElement("td");

    out.startLineElement("td");
    out.writeAttribute("class", "number");
    if (minTime < Long.MAX_VALUE)
      printTime(out, minTime);
    else
      out.print("&nbsp;");
    out.endLineElement("td");

    out.startLineElement("td");
    out.writeAttribute("class", "number");
    if (Long.MIN_VALUE < maxTime)
      printTime(out, maxTime);
    else
      out.print("&nbsp;");
    out.endLineElement("td");

    out.startLineElement("td");
    out.writeAttribute("class", "number");
    String timeString = createTimeString(totalTime, thisTime, childrenTime);
    out.writeAttribute("title", timeString);
    printTime(out, totalTime);
    out.endLineElement("td");

    out.startLineElement("td");
    out.writeAttribute("class", "number");
    out.print(invocationCount);
    out.endLineElement("td");

    out.endElement("tr");

    // All children
    
    for (ProfilerPoint child : children)
      display(child, comparator, out, depth + 1);
  }

  protected void writeXml(HttpServletRequest request,
                           HttpServletResponse response)
    throws ServletException, IOException
  {
    ProfilerNodeComparator comparator = new TimeComparator();
    comparator.setDescending(true);

    XmlWriter out = new XmlWriter(response.getWriter());

    out.setStrategy(XmlWriter.XML);
    out.setIndenting(false);

    String contextPath = request.getContextPath();

    if (contextPath == null || contextPath.length() == 0)
      contextPath = "/";

    out.startElement("profile");
    out.writeLineElement("name", contextPath);

    List<ProfilerPoint> children
      = _profilerManager.getRoot().getChildren();

    Collections.sort(children, comparator);
    for (ProfilerPoint child : children)
      displayXml(child, comparator, out);

    out.endElement("profile");
  }

  private void displayXml(ProfilerPoint node,
                          ProfilerNodeComparator comparator,
                          XmlWriter out)
  {
    List<ProfilerPoint> children = node.getChildren();
    Collections.sort(children, comparator);

    long thisTime = node.getTime();
    long minTime = node.getMinTime();
    long maxTime = node.getMaxTime();

    long childrenTime = 0;

    for (ProfilerPoint child : children) {
      childrenTime += child.getTime();
    }

    long totalTime = childrenTime + thisTime;

    long invocationCount = node.getInvocationCount();

    out.startBlockElement("node");
    out.writeLineElement("name", node.getName());

    if (minTime < Long.MAX_VALUE)
      out.writeLineElement("min-time", String.valueOf(minTime));
    else
      out.writeLineElement("min-time", "0");
    if (maxTime >= 0)
      out.writeLineElement("max-time", String.valueOf(maxTime));
    else
      out.writeLineElement("max-time", "0");
    out.writeLineElement("time", String.valueOf(thisTime));
    out.writeLineElement("total-time", String.valueOf(totalTime));
    out.writeLineElement("children-time", String.valueOf(childrenTime));
    out.writeLineElement("count", String.valueOf(invocationCount));

    for (ProfilerPoint child : children)
      displayXml(child, comparator, out);
    
    out.endBlockElement("node");
  }

  private String createTimeString(long totalTime,
                                  long thisTime,
                                  long childrenTime)
  {
    CharBuffer cb = new CharBuffer();

    cb.append("totalTime=");
    formatTime(cb, totalTime);

    cb.append(" thisTime=");
    formatTime(cb, thisTime);

    cb.append(" childrenTime=");
    formatTime(cb, childrenTime);

    return cb.toString();
  }

  private void printTime(XmlWriter out, long time)
  {
    CharBuffer cb = new CharBuffer();
    formatTime(cb, time);
    out.writeText(cb.toString());
  }

  private void formatTime(CharBuffer cb, long nanoseconds)
  {
    long milliseconds = nanoseconds / 1000000;

    long minutes = milliseconds /  1000 / 60;

    if (minutes > 0) {
      Sprintf.sprintf(cb, "%d:", minutes);
      milliseconds -= minutes * 60 * 1000;
    }

    long seconds = milliseconds /  1000;

    if (minutes > 0)
      Sprintf.sprintf(cb, "%02d.", seconds);
    else
      Sprintf.sprintf(cb, "%d.", seconds);

    milliseconds -= seconds * 1000;

    Sprintf.sprintf(cb, "%03d", milliseconds);
  }
}
