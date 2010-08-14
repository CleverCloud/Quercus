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

import java.util.Vector;

public final class JspNameUtil
{

  private static final String javaKeywords[] = {"abstract",
                                                "boolean",
                                                "break",
                                                "byte",
                                                "case",
                                                "catch",
                                                "char",
                                                "class",
                                                "const",
                                                "continue",
                                                "default",
                                                "do",
                                                "double",
                                                "else",
                                                "extends",
                                                "final",
                                                "finally",
                                                "float",
                                                "for",
                                                "goto",
                                                "if",
                                                "implements",
                                                "import",
                                                "instanceof",
                                                "int",
                                                "interface",
                                                "long",
                                                "native",
                                                "new",
                                                "package",
                                                "private",
                                                "protected",
                                                "public",
                                                "return",
                                                "short",
                                                "static",
                                                "strictfp",
                                                "super",
                                                "switch",
                                                "synchronized",
                                                "this",
                                                "throws",
                                                "transient",
                                                "try",
                                                "void",
                                                "volatile",
                                                "while"};

  // XXX: change

  /**
   * The default package name for compiled jsp pages.
   */
  public static final String JSP_PACKAGE_NAME = "org.apache.jsp";


  /**
   * Converts the given path to a Java package or fully-qualified class name
   *
   * @param path Path to convert
   *
   * @return Java package corresponding to the given path
   */
  public static final String makeJavaPackage(String path)
  {
    String classNameComponents[] = split(path, "/");
    StringBuffer legalClassNames = new StringBuffer();
    for (int i = 0; i < classNameComponents.length; i++) {
      legalClassNames.append(makeJavaIdentifier(classNameComponents[i]));
      if (i < classNameComponents.length - 1) {
        legalClassNames.append('.');
      }
    }
    return legalClassNames.toString();
  }

  /**
   * Splits a string into it's components.
   *
   * @param path String to split
   * @param pat  Pattern to split at
   *
   * @return the components of the path
   */
  private static final String[] split(String path, String pat)
  {
    Vector comps = new Vector();
    int pos = path.indexOf(pat);
    int start = 0;
    while (pos >= 0) {
      if (pos > start) {
        String comp = path.substring(start, pos);
        comps.add(comp);
      }
      start = pos + pat.length();
      pos = path.indexOf(pat, start);
    }
    if (start < path.length()) {
      comps.add(path.substring(start));
    }
    String[] result = new String[comps.size()];
    for (int i = 0; i < comps.size(); i++) {
      result[i] = (String) comps.elementAt(i);
    }
    return result;
  }

  /**
   * Converts the given identifier to a legal Java identifier
   *
   * @param identifier Identifier to convert
   *
   * @return Legal Java identifier corresponding to the given identifier
   */
  public static final String makeJavaIdentifier(String identifier)
  {
    StringBuffer modifiedIdentifier = new StringBuffer(identifier.length());
    if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
      modifiedIdentifier.append('_');
    }
    for (int i = 0; i < identifier.length(); i++) {
      char ch = identifier.charAt(i);
      if (Character.isJavaIdentifierPart(ch) && ch != '_') {
        modifiedIdentifier.append(ch);
      }
      else if (ch == '.') {
        modifiedIdentifier.append('_');
      }
      else {
        modifiedIdentifier.append(mangleChar(ch));
      }
    }
    if (isJavaKeyword(modifiedIdentifier.toString())) {
      modifiedIdentifier.append('_');
    }
    return modifiedIdentifier.toString();
  }

  /**
   * Mangle the specified character to create a legal Java class name.
   */
  public static final String mangleChar(char ch)
  {
    char[] result = new char[5];
    result[0] = '_';
    result[1] = Character.forDigit((ch >> 12) & 0xf, 16);
    result[2] = Character.forDigit((ch >> 8) & 0xf, 16);
    result[3] = Character.forDigit((ch >> 4) & 0xf, 16);
    result[4] = Character.forDigit(ch & 0xf, 16);
    return new String(result);
  }

  /**
   * Test whether the argument is a Java keyword
   */
  public static boolean isJavaKeyword(String key)
  {
    int i = 0;
    int j = javaKeywords.length;
    while (i < j) {
      int k = (i + j) / 2;
      int result = javaKeywords[k].compareTo(key);
      if (result == 0) {
        return true;
      }
      if (result < 0) {
        i = k + 1;
      }
      else {
        j = k;
      }
    }
    return false;
  }

}
