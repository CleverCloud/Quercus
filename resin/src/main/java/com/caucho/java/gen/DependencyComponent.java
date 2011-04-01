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
 * @author Scott Ferguson
 */

package com.caucho.java.gen;

import com.caucho.java.JavaWriter;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.L10N;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;
import com.caucho.vfs.PersistentDependency;
import com.caucho.vfs.Vfs;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Basic method generation.
 */
public class DependencyComponent extends ClassComponent {
  private static final L10N L = new L10N(DependencyComponent.class);

  private String _initMethod = "_caucho_init";
  private String _isModifiedMethod = "_caucho_is_modified";
  
  private Path _searchPath;

  private ArrayList<PersistentDependency> _dependList
    = new ArrayList<PersistentDependency>();

  /**
   * Sets the search path.
   */
  public void setSearchPath(Path searchPath)
  {
    _searchPath = searchPath;
  }

  /**
   * Adds a dependency list.
   */
  public void addDependencyList(ArrayList<PersistentDependency> dependList)
  {
    for (int i = 0; i < dependList.size(); i++)
      addDependency(dependList.get(i));
  }

  /**
   * Adds a dependency.
   */
  public void addDependency(PersistentDependency depend)
  {
    if (! _dependList.contains(depend))
      _dependList.add(depend);
  }
  
  /**
   * Generates the code for the dependencies.
   *
   * @param out the writer to the output stream.
   */
  public void generate(JavaWriter out)
    throws IOException
  {
    out.println("private static com.caucho.vfs.Dependency []_caucho_depend;");
    
    out.println();
    out.println("public static void " + _initMethod + "(com.caucho.vfs.Path path)");
    out.println("{");
    out.pushDepth();

    out.println("_caucho_depend = new com.caucho.vfs.Dependency[" +
                _dependList.size() + "];");

    Path searchPath = _searchPath;
    
    for (int i = 0; i < _dependList.size(); i++) {
      PersistentDependency dependency = _dependList.get(i);

      if (dependency instanceof Depend) {
        Depend depend = (Depend) _dependList.get(i);
        Path path = depend.getPath();

        out.print("_caucho_depend[" + i + "] = new com.caucho.vfs.Depend(");
        
        // php/3b33
        String relativePath;
        if (searchPath != null)
          relativePath = searchPath.lookup(path.getRelativePath()).getRelativePath();
        else {
          String fullPath = path.getFullPath();
          String pwd = Vfs.lookup().getFullPath();

          if (fullPath.startsWith(pwd))
            relativePath = "./" + fullPath.substring(pwd.length());
          else
            relativePath = fullPath;
        }

        out.print("path.lookup(\"" + relativePath + "\"), ");
    
        out.println(depend.getDigest() + "L, "
                    + depend.getRequireSource() + ");");
      }
      else {
        out.print("_caucho_depend[" + i + "] = ");
        out.print(dependency.getJavaCreateString());
        out.println(";");
      }
    }

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public static boolean " + _isModifiedMethod + "()");
    out.println("{");
    out.pushDepth();

    //printVersionChange(out);

    out.println("for (int i = _caucho_depend.length - 1; i >= 0; i--) {");
    out.println("  if (_caucho_depend[i].isModified())");
    out.println("    return true;");
    out.println("}");

    out.println();
    out.println("return false;");
    
    out.popDepth();
    out.println("}");
  }

  /**
   * Prints code to detect a version change.
   */
  protected void printVersionChange(JavaWriter out)
    throws IOException
  {
    out.println("if (com.caucho.server.util.CauchoSystem.getVersionId() != " +
                "0x" + Long.toHexString(CauchoSystem.getVersionId()) + "L)");
    out.println("  return true;");
  }
}
