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
 * @author Scott Ferguson
 */

package com.caucho.loader.enhancer;

import com.caucho.bytecode.ByteCodeParser;
import com.caucho.bytecode.JavaClass;
import com.caucho.bytecode.JavaField;
import com.caucho.bytecode.JavaMethod;
import com.caucho.java.WorkDir;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.util.L10N;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Prepares a class for enhancement.
 */
public class EnhancerPrepare {
  private static final L10N L = new L10N(EnhancerPrepare.class);
  private static final Logger log =
    Logger.getLogger(EnhancerPrepare.class.getName());

  private static final int ACC_PUBLIC = 0x1;
  private static final int ACC_PRIVATE = 0x2;
  private static final int ACC_PROTECTED = 0x4;

  private ClassLoader _loader;

  private Path _workPath;

  private ArrayList<ClassEnhancer> _enhancerList =
    new ArrayList<ClassEnhancer>();

  private String _baseSuffix = "";
  private String _extSuffix = "__ResinExt";

  private boolean _isParentStarted;

  /**
   * Sets the class loader.
   */
  public void setClassLoader(ClassLoader loader)
  {
    _loader = loader;
  }

  /**
   * Gets the work path.
   */
  public Path getWorkPath()
  {
    if (_workPath != null)
      return _workPath;
    else
      return WorkDir.getLocalWorkDir(_loader);
  }

  /**
   * Sets the work path.
   */
  public void setWorkPath(Path workPath)
  {
    _workPath = workPath;
  }

  /**
   * Gets the work path.
   */
  public final Path getPreWorkPath()
  {
    return getWorkPath().lookup("pre-enhance");
  }

  /**
   * Gets the work path.
   */
  public final Path getPostWorkPath()
  {
    return getWorkPath().lookup("post-enhance");
  }

  /**
   * Adds a class enhancer.
   */
  public void addEnhancer(ClassEnhancer enhancer)
  {
    _enhancerList.add(enhancer);
  }

  /**
   * Moves the old class.
   */
  public void renameClass(String sourceClass, String targetClass)
  {
    Path source = getSource(sourceClass);

    if (source == null || source.getLastModified() <= 0)
      return;

    Path path = getPreWorkPath();
    Path target = path.lookup(targetClass.replace('.', '/') + ".class");

    if (target.getLastModified() <= 0 ||
        target.getLastModified() < source.getLastModified()) {
      try {
        target.remove();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      try {
        ByteCodeParser parser = new ByteCodeParser();
        ReadStream is = source.openRead();
        WriteStream os = null;

        try {
          JavaClass cl = parser.parse(is);

          String cpOldName = sourceClass.replace('.', '/');
          String cpClassName = targetClass.replace('.', '/');

          int utf8Index = cl.getConstantPool().addUTF8(cpClassName).getIndex();
          cl.getConstantPool().getClass(cpOldName).setNameIndex(utf8Index);

          cl.setThisClass(cpClassName);

          // need to set descriptors, too

          // set private fields to protected
          ArrayList<JavaField> fields = cl.getFieldList();
          for (int i = 0; i < fields.size(); i++) {
            JavaField field = fields.get(i);

            int accessFlags = field.getAccessFlags();

            if ((accessFlags & ACC_PRIVATE) != 0) {
              accessFlags = (accessFlags & ~ ACC_PRIVATE) | ACC_PROTECTED;
              field.setAccessFlags(accessFlags);
            }
          }

          // set private methods to protected
          ArrayList<JavaMethod> methods = cl.getMethodList();
          for (int i = 0; i < methods.size(); i++) {
            JavaMethod method = methods.get(i);

            int accessFlags = method.getAccessFlags();

            if ((accessFlags & ACC_PRIVATE) != 0) {
              accessFlags = (accessFlags & ~ ACC_PRIVATE) | ACC_PROTECTED;
              method.setAccessFlags(accessFlags);
            }
          }

          for (int i = 0; i < _enhancerList.size(); i++) {
            _enhancerList.get(i).preEnhance(cl);
          }

          target.getParent().mkdirs();

          os = target.openWrite();

          cl.write(os);
        } finally {
          if (is != null)
            is.close();

          if (os != null)
            os.close();
        }
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);

      }
    }
  }

  private Path getSource(String className)
  {
    ClassLoader loader = _loader;
    if (loader == null)
      loader = Thread.currentThread().getContextClassLoader();

    URL url = loader.getResource(className.replace('.', '/') + ".class");

    // XXX: workaround for tck
    String s = url.toString();
    int index = s.indexOf("jar!/");
    if (index > 0) {
      s = s.substring(9, index+3);
      Path path = JarPath.create(Vfs.lookup(s));
      path = path.lookup(className.replace('.', '/') + ".class");
      return path;
    }

    if (url != null)
      return Vfs.lookup(url.toString());
    else
      return null;
  }

  /**
   * Moves the old class.
   */
  protected JavaClass renameClass(JavaClass jClass, String targetClass)
  {
    String cpOldName = jClass.getThisClass();
    String cpClassName = targetClass.replace('.', '/');

    int utf8Index = jClass.getConstantPool().addUTF8(cpClassName).getIndex();
    jClass.getConstantPool().getClass(cpOldName).setNameIndex(utf8Index);

    jClass.setThisClass(cpClassName);

    // need to set descriptors, too

    // set private fields to protected
    ArrayList<JavaField> fields = jClass.getFieldList();
    for (int i = 0; i < fields.size(); i++) {
      JavaField field = fields.get(i);

      int accessFlags = field.getAccessFlags();

      if ((accessFlags & ACC_PRIVATE) != 0) {
        accessFlags = (accessFlags & ~ ACC_PRIVATE) | ACC_PROTECTED;
        field.setAccessFlags(accessFlags);
      }
    }

    // set private methods to protected
    ArrayList<JavaMethod> methods = jClass.getMethodList();
    for (int i = 0; i < methods.size(); i++) {
      JavaMethod method = methods.get(i);

      int accessFlags = method.getAccessFlags();

      if ((accessFlags & ACC_PRIVATE) != 0) {
        accessFlags = (accessFlags & ~ ACC_PRIVATE) | ACC_PROTECTED;
        method.setAccessFlags(accessFlags);
      }
    }

    return jClass;
  }
}
