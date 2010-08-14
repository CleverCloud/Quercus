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

package com.caucho.netbeans.util;

import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.openide.ErrorManager;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Annotation;
import org.openide.text.Line;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Logger support class for creating links in the output window.
 */
public final class LogSupport
{
  private final Map/*<Link, Link>*/ links
    = Collections.synchronizedMap(new HashMap());
  private Annotation errAnnot;
  private String prevMessage;

  public LineInfo analyzeLine(String logLine)
  {
    String path = null;
    int line = -1;
    String message = null;
    boolean error = false;
    boolean accessible = false;

    logLine = logLine.trim();
    int lineLenght = logLine.length();

    // look for unix file links (e.g. /foo/bar.java:51: 'error msg')
    if (logLine.startsWith("/")) { // NOI18N
      error = true;
      int colonIdx = logLine.indexOf(':');
      if (colonIdx > -1) {
        path = logLine.substring(0, colonIdx);
        accessible = true;
        if (lineLenght > colonIdx) {
          int nextColonIdx = logLine.indexOf(':', colonIdx + 1);
          if (nextColonIdx > -1) {
            String lineNum = logLine.substring(colonIdx + 1, nextColonIdx);
            try {
              line = Integer.valueOf(lineNum).intValue();
            }
            catch (NumberFormatException nfe) { // ignore it
            }
            if (lineLenght > nextColonIdx) {
              message = logLine.substring(nextColonIdx + 1, lineLenght);
            }
          }
        }
      }
    }
    // look for windows file links (e.g. c:\foo\bar.java:51: 'error msg')
    else if (lineLenght > 3 &&
             Character.isLetter(logLine.charAt(0)) &&
             (logLine.charAt(1) == ':') &&
             (logLine.charAt(2) == '\\'))
    {
      error = true;
      int secondColonIdx = logLine.indexOf(':', 2);
      if (secondColonIdx > -1) {
        path = logLine.substring(0, secondColonIdx);
        accessible = true;
        if (lineLenght > secondColonIdx) {
          int thirdColonIdx = logLine.indexOf(':', secondColonIdx + 1);
          if (thirdColonIdx > -1) {
            String lineNum = logLine.substring(secondColonIdx + 1,
                                               thirdColonIdx);
            try {
              line = Integer.valueOf(lineNum).intValue();
            }
            catch (NumberFormatException nfe) { // ignore it
            }
            if (lineLenght > thirdColonIdx) {
              message = logLine.substring(thirdColonIdx + 1, lineLenght);
            }
          }
        }
      }
    }
    // look for stacktrace links (e.g. at java.lang.Thread.run(Thread.java:595)
    //                                 at t.HyperlinkTest$1.run(HyperlinkTest.java:24))
    else if (logLine.startsWith("at ") && lineLenght > 3) {
      error = true;
      int parenthIdx = logLine.indexOf('(');
      if (parenthIdx > -1) {
        String classWithMethod = logLine.substring(3, parenthIdx);
        int lastDotIdx = classWithMethod.lastIndexOf('.');
        if (lastDotIdx > -1) {
          int lastParenthIdx = logLine.lastIndexOf(')');
          int lastColonIdx = logLine.lastIndexOf(':');
          if (lastParenthIdx > -1 && lastColonIdx > -1) {
            String lineNum = logLine.substring(lastColonIdx + 1,
                                               lastParenthIdx);
            try {
              line = Integer.valueOf(lineNum).intValue();
            }
            catch (NumberFormatException nfe) { // ignore it
            }
            message = prevMessage;
          }
          int firstDolarIdx
            = classWithMethod.indexOf('$'); // > -1 for inner classes
          String className = classWithMethod.substring(0,
                                                       firstDolarIdx > -1
                                                       ? firstDolarIdx
                                                       : lastDotIdx);
          path = className.replace('.', '/') + ".java"; // NOI18N
          accessible = GlobalPathRegistry.getDefault().findResource(path) !=
                       null;
        }
      }
    }
    // every other message treat as normal info message
    else {
      prevMessage = logLine;
    }
    return new LineInfo(path, line, message, error, accessible);
  }

  /**
   * Return a link which implements <code>OutputListener</code> interface.
   * Link is then used to represent a link in the output window. This class
   * also handles error annotations which are shown after a line is clicked.
   *
   * @return link which implements <code>OutputListener</code> interface. Link
   *         is then used to represent a link in the output window.
   */
  public Link getLink(String errorMsg, String path, int line)
  {
    Link newLink = new Link(errorMsg, path, line);
    Link cachedLink = (Link) links.get(newLink);
    if (cachedLink != null) {
      return cachedLink;
    }
    links.put(newLink, newLink);
    return newLink;
  }

  /**
   * Detach error annotation.
   */
  public void detachAnnotation()
  {
    if (errAnnot != null) {
      errAnnot.detach();
    }
  }

  /**
   * <code>LineInfo</code> is used to store info about the parsed line.
   */
  public static class LineInfo
  {
    private String path;
    private int line;
    private String message;
    private boolean error;
    private boolean accessible;

    /**
     * <code>LineInfo</code> is used to store info about the parsed line.
     *
     * @param path       path to file
     * @param line       line number where the error occurred
     * @param message    error message
     * @param error      represents the line an error?
     * @param accessible is the file accessible?
     */
    public LineInfo(String path,
                    int line,
                    String message,
                    boolean error,
                    boolean accessible)
    {
      this.path = path;
      this.line = line;
      this.message = message;
      this.error = error;
      this.accessible = accessible;
    }

    public String path()
    {
      return path;
    }

    public int line()
    {
      return line;
    }

    public String message()
    {
      return message;
    }

    public boolean isError()
    {
      return error;
    }

    public boolean isAccessible()
    {
      return accessible;
    }

    public String toString()
    {
      return "path=" +
             path +
             " line=" +
             line +
             " message=" +
             message
             // NOI18N
             +
             " isError=" +
             error +
             " isAccessible=" +
             accessible;      // NOI18N
    }
  }

  /**
   * Error annotation.
   */
  static class ErrorAnnotation
    extends Annotation
  {
    private String shortDesc = null;

    public ErrorAnnotation(String desc)
    {
      shortDesc = desc;
    }

    public String getAnnotationType()
    {
      return "com-caucho-netbeans-error";
    }

    public String getShortDescription()
    {
      return shortDesc;
    }

  }

  /**
   * <code>Link</code> is used to create a link in the output window. To
   * create a link use the <code>getLink</code> method of the
   * <code>LogSupport</code> class. This prevents from memory vast by
   * returning already existing instance, if one with such values exists.
   */
  public class Link
    implements OutputListener
  {
    private String msg;
    private String path;
    private int line;

    private int hashCode = 0;

    Link(String msg, String path, int line)
    {
      this.msg = msg;
      this.path = path;
      this.line = line;
    }

    public int hashCode()
    {
      if (hashCode == 0) {
        int result = 17;
        result = 37 * result + line;
        result = 37 * result + (path != null ? path.hashCode() : 0);
        result = 37 * result + (msg != null ? msg.hashCode() : 0);
        hashCode = result;
      }
      return hashCode;
    }

    public boolean equals(Object obj)
    {
      if (this == obj) {
        return true;
      }
      if (obj instanceof Link) {
        Link anotherLink = (Link) obj;
        if ((((msg != null) && msg.equals(anotherLink.msg)) ||
             (msg == anotherLink.msg)) &&
                                       (((path != null) &&
                                         path.equals(anotherLink.path)) ||
                                                                        (path ==
                                                                         anotherLink
                                                                           .path)) &&
                                                                                   line ==
                                                                                   anotherLink
                                                                                     .line)
        {
          return true;
        }
      }
      return false;
    }

    /**
     * If the link is clicked, required file is opened in the editor and an
     * <code>ErrorAnnotation</code> is attached.
     */
    public void outputLineAction(OutputEvent ev)
    {
      FileObject sourceFile = GlobalPathRegistry.getDefault()
        .findResource(path);
      if (sourceFile == null) {
        sourceFile = FileUtil.toFileObject(new File(path));
      }
      DataObject dataObject = null;
      if (sourceFile != null) {
        try {
          dataObject = DataObject.find(sourceFile);
        }
        catch (DataObjectNotFoundException ex) {
          ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
        }
      }
      if (dataObject != null) {
        EditorCookie editorCookie = (EditorCookie) dataObject.getCookie(
          EditorCookie.class);
        if (editorCookie == null) {
          return;
        }
        editorCookie.open();
        Line errorLine = null;
        try {
          errorLine = editorCookie.getLineSet().getCurrent(line - 1);
        }
        catch (IndexOutOfBoundsException iobe) {
          return;
        }
        if (errAnnot != null) {
          errAnnot.detach();
        }
        String errorMsg = msg;
        if (errorMsg == null || errorMsg.equals("")) {
          errorMsg = "Exception occured";
        }
        errAnnot = new ErrorAnnotation(errorMsg);
        errAnnot.attach(errorLine);
        errAnnot.moveToFront();
        errorLine.show(Line.SHOW_TRY_SHOW);
      }
    }

    /**
     * If a link is cleared, error annotation is detached and link cache is
     * clared.
     */
    public void outputLineCleared(OutputEvent ev)
    {
      if (errAnnot != null) {
        errAnnot.detach();
      }
      if (!links.isEmpty()) {
        links.clear();
      }
    }

    public void outputLineSelected(OutputEvent ev)
    {
    }
  }
}
