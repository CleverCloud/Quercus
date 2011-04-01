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

import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bytecode.Analyzer;
import com.caucho.bytecode.Attribute;
import com.caucho.bytecode.ByteCodeParser;
import com.caucho.bytecode.CodeAttribute;
import com.caucho.bytecode.CodeEnhancer;
import com.caucho.bytecode.CodeVisitor;
import com.caucho.bytecode.ConstantPool;
import com.caucho.bytecode.ConstantPoolEntry;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JavaClass;
import com.caucho.bytecode.JavaClassLoader;
import com.caucho.bytecode.JavaField;
import com.caucho.bytecode.JavaMethod;
import com.caucho.bytecode.MethodRefConstant;
import com.caucho.bytecode.Utf8Constant;
import com.caucho.inject.Module;
import com.caucho.java.WorkDir;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 *
 * <p>DynamicClassLoaders can be chained creating one virtual class loader.
 * From the perspective of the JDK, it's all one classloader.  Internally,
 * the class loader chain searches like a classpath.
 */
@Module
public class EnhancerFixup {
  private static final Logger log = Logger.getLogger(EnhancerFixup.class.getName());

  private static final int ACC_PUBLIC = 0x1;
  private static final int ACC_PRIVATE = 0x2;
  private static final int ACC_PROTECTED = 0x4;

  private JavaClassLoader _jClassLoader;
  private DynamicClassLoader _loader;

  private Path _workPath;

  private String _baseSuffix = "";
  private String _extSuffix = "__ResinExt";

  private ArrayList<ClassEnhancer> _enhancerList =
    new ArrayList<ClassEnhancer>();

  private boolean _isParentStarted;

  /**
   * Sets the class loader.
   */
  public void setClassLoader(DynamicClassLoader loader)
  {
    _loader = loader;
  }

  /**
   * Returns the parsed class loader.
   */
  public void setJavaClassLoader(JavaClassLoader jClassLoader)
  {
    _jClassLoader = jClassLoader;
  }

  /**
   * Returns the parsed class loader.
   */
  public JavaClassLoader getJavaClassLoader()
  {
    return _jClassLoader;
  }

  /**
   * Gets the work path.
   */
  public Path getWorkPath()
  {
    if (_workPath != null)
      return _workPath;
    else
      return WorkDir.getLocalWorkDir();
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

  protected void fixup(String className, String extClassName)
    throws Exception
  {
    Path prePath = getPreWorkPath();
    Path postPath = getPostWorkPath();

    Path source = getSource(className);
    
    if (source == null || ! source.canRead())
      return;

    Path ext = prePath.lookup(extClassName.replace('.', '/') + ".class");
    Path target = postPath.lookup(className.replace('.', '/') + ".class");

    try {
      target.getParent().mkdirs();
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }

    if (source != null)
      mergeClasses(className, target, source, ext);
    else
      mergeClasses(className, target, ext);

    int p = className.lastIndexOf('.');

    Path preTargetDir;
    Path targetDir;
    String classSuffix;
    String prefix = "";

    if (p > 0) {
      prefix = className.substring(0, p).replace('.', '/');
      preTargetDir = prePath.lookup(prefix);
      targetDir = postPath.lookup(prefix);
      classSuffix = className.substring(p + 1);
    }
    else {
      preTargetDir = prePath;
      targetDir = postPath;
      classSuffix = className;
    }

    prefix = prefix.replace('/', '.');
    if (! prefix.equals(""))
      prefix = prefix + ".";

    String extSuffix;
    p = extClassName.lastIndexOf('.');
    if (p > 0)
      extSuffix = extClassName.substring(p + 1);
    else
      extSuffix = extClassName;

    fixupPreSubClasses(preTargetDir, targetDir,
                       extSuffix, classSuffix);

    fixupPostSubClasses(targetDir, prefix, classSuffix);
  }

  private void fixupPreSubClasses(Path preTargetDir, Path targetDir,
                                  String extSuffix, String classSuffix)
    throws Exception
  {
    String []list = preTargetDir.list();

    for (int i = 0; i < list.length; i++) {
      String name = list[i];

      if (name.startsWith(extSuffix + '$') &&
          name.endsWith(".class")) {
        int p = name.indexOf('$');
        String targetClass = (classSuffix + '$' +
                              name.substring(p + 1, name.length() - 6));

        Path subTarget = targetDir.lookup(targetClass + ".class");

        Path extPath = preTargetDir.lookup(name);

        renameSubClass(classSuffix, subTarget, extPath);

        //if (_loader != null)
        //  _loader.addPathClass(prefix + targetClass, subTarget);
      }
      else if (name.startsWith(extSuffix + '-') &&
               name.endsWith(".class")) {
        int p = name.indexOf('-');
        String targetClass = (classSuffix + '-' +
                              name.substring(p + 1, name.length() - 6));

        Path subTarget = targetDir.lookup(targetClass + ".class");

        Path extPath = preTargetDir.lookup(name);

        renameSubClass(classSuffix, subTarget, extPath);

        //if (_loader != null)
        //  _loader.addPathClass(prefix + targetClass, subTarget);
      }
    }
  }

  private void fixupPostSubClasses(Path targetDir,
                                   String prefix,
                                   String classSuffix)
    throws Exception
  {
    String []list = targetDir.list();

    for (int i = 0; i < list.length; i++) {
      String name = list[i];

      if (! name.endsWith(".class"))
        continue;

      String className = name.substring(0, name.length() - ".class".length());

      if (name.startsWith(classSuffix + '$')) {
        if (_loader != null)
          _loader.addPathClass(prefix + className, targetDir.lookup(name));
      }
      else if (name.startsWith(classSuffix + '-')) {
        if (_loader != null)
          _loader.addPathClass(prefix + className, targetDir.lookup(name));
      }
      else if (name.startsWith(classSuffix + '+')) {
        if (_loader != null)
          _loader.addPathClass(prefix + className, targetDir.lookup(name));
      }
    }
  }

  /**
   * Merges the two classes.
   */
  protected void renameSubClass(String className,
                                Path targetPath,
                                Path extPath)
    throws Exception
  {
    JavaClass extClass = null;

    ByteCodeParser parser = new ByteCodeParser();

    parser = new ByteCodeParser();
    parser.setClassLoader(new JavaClassLoader());

    ReadStream is = extPath.openRead();
    try {
      extClass = parser.parse(is);
    } finally {
      if (is != null)
        is.close();
    }

    cleanExtConstantPool(className, extClass);

    WriteStream os = targetPath.openWrite();
    try {
      extClass.write(os);
    } finally {
      os.close();
    }
  }

  /**
   * Renamed the super() methods
   */
  protected void renameExtSuperMethods(String className,
                                       JavaClass baseClass,
                                       JavaClass extClass)
    throws Exception
  {
    ArrayList<ConstantPoolEntry> entries;
    entries = extClass.getConstantPool().getEntries();

    className = className.replace('.', '/');
    String baseName = className + _baseSuffix;
    String extName = className + "__ResinExt";

    for (int i = 0; i < entries.size(); i++) {
      ConstantPoolEntry entry = entries.get(i);

      if (entry instanceof MethodRefConstant) {
        MethodRefConstant methodRef = (MethodRefConstant) entry;

        if (! methodRef.getClassName().equals(baseName))
          continue;

        String methodName = methodRef.getName();
        String type = methodRef.getType();

        if (findMethod(baseClass, methodName, type) == null)
          continue;

        if (findMethod(extClass, methodName, type) == null)
          continue;

        if (methodName.equals("<init>")) {
          methodName = "__init__super";
          // methodRef.setNameAndType(methodName, type);
        }
        /* jpa/0h28, shouldn't automatically change this
        else {
          methodName = methodName + "__super";
          methodRef.setNameAndType(methodName, type);
        }
        */
      }
    }
  }


  /**
   * Moves all methods in the base to the __super methods
   */
  private void moveSuperMethods(String className,
                                JavaClass baseClass,
                                JavaClass extClass)
  {
    className = className.replace('.', '/');

    ArrayList<JavaMethod> methods = baseClass.getMethodList();

    // Move the __super methods
    ArrayList<JavaMethod> extMethods = extClass.getMethodList();
    for (int i = 0; i < extMethods.size(); i++) {
      JavaMethod extMethod = extMethods.get(i);

      fixupExtMethod(baseClass, extClass, extMethod);

      String baseName = extMethod.getName();

      if (baseName.endsWith("__super"))
        continue;

      String superName = baseName + "__super";

      int j;
      for (j = 0; j < methods.size(); j++) {
        JavaMethod method = methods.get(j);

        String type = method.getDescriptor();

        if (! method.getName().equals(baseName)
            || ! method.getDescriptor().equals(extMethod.getDescriptor()))
          continue;

        if (baseName.equals("<init>")) {
          baseClass.getConstantPool().addUTF8("__init__super");
          mergeInitMethods(baseClass, method, extClass, extMethod);
          break;
        }

        if (baseName.equals("<clinit>")) {
          concatenateMethods(baseClass, method, extClass, extMethod);
          break;
        }

        baseClass.getConstantPool().addUTF8(superName);
        method.setName(superName);
        baseClass.getConstantPool().addUTF8(type);
        method.setDescriptor(type);

        // set the super methods private
        int flags = method.getAccessFlags();
        flags = (flags & ~ACC_PUBLIC & ~ACC_PROTECTED) | ACC_PRIVATE;
        method.setAccessFlags(flags);
        break;
      }
    }
  }

  /**
   * Concatenates methods
   */
  private void concatenateMethods(JavaClass baseClass, JavaMethod baseMethod,
                                  JavaClass extClass, JavaMethod extMethod)
  {
    extMethod = extMethod.export(extClass, baseClass);

    baseMethod.concatenate(extMethod);
  }

  /**
   * Merges the init methods
   */
  private void mergeInitMethods(JavaClass baseClass, JavaMethod baseMethod,
                                JavaClass extClass, JavaMethod extMethod)
  {
    extMethod = extMethod.export(extClass, baseClass);

    baseMethod.setName("__init__super");

    baseClass.getMethodList().add(extMethod);

    try {
      InitAnalyzer initAnalyzer = new InitAnalyzer();
      CodeEnhancer baseEnhancer = new CodeEnhancer(baseClass, baseMethod.getCode());
      baseEnhancer.analyze(initAnalyzer);

      int offset = initAnalyzer.getOffset();
      byte []code = new byte[offset];
      byte []oldCode = baseEnhancer.getCode();
      System.arraycopy(oldCode, 0, code, 0, offset);

      baseEnhancer.remove(0, offset);
      baseEnhancer.update();

      CodeEnhancer extEnhancer = new CodeEnhancer(baseClass, extMethod.getCode());

      extEnhancer.add(0, code, 0, code.length);

      ExtMethodAnalyzer extMethodAnalyzer
        = new ExtMethodAnalyzer(baseClass, extMethod, offset);
      extEnhancer.analyze(extMethodAnalyzer);
      extEnhancer.update();

      CodeAttribute baseCode = baseMethod.getCode();
      CodeAttribute extCode = extMethod.getCode();

      if (extCode.getMaxStack() < baseCode.getMaxStack())
        extCode.setMaxStack(baseCode.getMaxStack());

      // XXX: needs tests badly
      extCode.removeAttribute("LocalVariableTable");
      extCode.removeAttribute("LineNumberTable");
      baseCode.removeAttribute("LocalVariableTable");
      baseCode.removeAttribute("LineNumberTable");

      /*
        baseMethod.concatenate(extMethod);
      */
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Merges the init methods
   */
  private void fixupExtMethod(JavaClass baseClass,
                              JavaClass extClass,
                              JavaMethod extMethod)
  {
    try {
      if (extMethod.getName().endsWith("__super"))
        return;
      
      CodeEnhancer extEnhancer
        = new CodeEnhancer(extClass, extMethod.getCode());

      ExtMethodAnalyzer extMethodAnalyzer
        = new ExtMethodAnalyzer(baseClass, extMethod, 0);
      extEnhancer.analyze(extMethodAnalyzer);
      extEnhancer.update();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Adds the methods from the ext to the base
   */
  private void addExtInterfaces(JavaClass baseClass, JavaClass extClass)
  {
    for (String name : extClass.getInterfaceNames()) {
      baseClass.getConstantPool().addClass(name);

      baseClass.addInterface(name);
    }
  }

  /**
   * Adds the methods from the ext to the base
   */
  private void addExtMethods(JavaClass baseClass, JavaClass extClass)
  {
    ArrayList<JavaMethod> methods = baseClass.getMethodList();
    ArrayList<JavaMethod> extMethods = extClass.getMethodList();

    for (int i = 0; i < extMethods.size(); i++) {
      JavaMethod extMethod = extMethods.get(i);

      if (extMethod.getName().equals("<clinit>") &&
          findMethod(baseClass, "<clinit>",
                     extMethod.getDescriptor()) != null) {
        continue;
      }
      else if (extMethod.getName().equals("<init>"))
        continue;
      else if (extMethod.getName().endsWith("__super"))
        continue;

      log.finest("adding extension method: " + extClass.getThisClass() + ":" + extMethod.getName());

      JavaMethod method = extMethod.export(extClass, baseClass);

      methods.add(method);
    }
  }

  /**
   * Adds the inner classes from the ext to the base
   */
  private void addExtClasses(JavaClass baseClass, JavaClass extClass)
  {
    /*
      ArrayList<JavaMethod> methods = baseClass.getMethodList();
      ArrayList<JavaMethod> extMethods = extClass.getMethodList();

      for (int i = 0; i < extMethods.size(); i++) {
      JavaMethod extMethod = extMethods.get(i);

      if (extMethod.getName().equals("<clinit>") &&
      findMethod(baseClass, "<clinit>",
      extMethod.getDescriptor()) != null) {
      continue;
      }
      else if (extMethod.getName().equals("<init>"))
      continue;
      else if (extMethod.getName().endsWith("__super"))
      continue;

      log.finest("adding extension method: " + extClass.getThisClass() + ":" + extMethod.getName());

      JavaMethod method = extMethod.export(extClass, baseClass);

      methods.add(method);
      }
    */
  }

  /**
   * Finds a matching method.
   */
  private static JavaMethod findMethod(JavaClass cl,
                                       String name,
                                       String descriptor)
  {
    ArrayList<JavaMethod> methods = cl.getMethodList();

    int j;
    for (j = 0; j < methods.size(); j++) {
      JavaMethod method = methods.get(j);

      if (method.getName().equals(name) &&
          method.getDescriptor().equals(descriptor))
        return method;
    }

    return null;
  }

  /**
   * Adds all fields from the ext to the base
   */
  private void moveSuperFields(JavaClass baseClass, JavaClass extClass)
  {
    ArrayList<JavaField> fields = baseClass.getFieldList();
    ArrayList<JavaField> extFields = extClass.getFieldList();

    for (int i = 0; i < extFields.size(); i++) {
    }
  }

  private Path getSource(String className)
  {
    ClassLoader loader = _loader;
    if (loader == null)
      loader = Thread.currentThread().getContextClassLoader();

    URL url = loader.getResource(className.replace('.', '/') + ".class");

    // XXX: workaround for tck
    // jpa/0g0s, #3574
    String s = URLDecoder.decode(url.toString());
    int index = s.indexOf("jar!/");
    if (index > 0) {
      s = s.substring(9, index+3);
      Path path = JarPath.create(Vfs.lookup(s));
      path = path.lookup(className.replace('.', '/') + ".class");
      return path;
    }

    return Vfs.lookup(s);
  }

  /**
   * Merges the two classes.
   */
  protected void mergeClasses(String className,
                              Path targetPath,
                              Path sourcePath,
                              Path extPath)
    throws Exception
  {
    JavaClass baseClass = null;
    JavaClass extClass = null;

    ByteCodeParser parser = new ByteCodeParser();
    parser.setClassLoader(getJavaClassLoader());

    ReadStream is = sourcePath.openRead();
    try {
      baseClass = parser.parse(is);
    } finally {
      if (is != null)
        is.close();
    }

    parser = new ByteCodeParser();
    parser.setClassLoader(getJavaClassLoader());

    is = extPath.openRead();
    try {
      extClass = parser.parse(is);
    } finally {
      if (is != null)
        is.close();
    }

    // jpa/0j26
    // XXX: later, need to see if it's possible to keep some of this
    // information for debugging
    fixupLocalVariableTable(extClass);
    fixupLocalVariableTable(baseClass);
    
    // The base class will have the modified class
    mergeClasses(className, baseClass, extClass);

    postEnhance(baseClass);

    WriteStream os = targetPath.openWrite();
    try {
      baseClass.write(os);
    } finally {
      os.close();
    }
  }

  /**
   * Merges the two classes.
   */
  protected void mergeClasses(String className,
                              Path targetPath,
                              Path extPath)
    throws Exception
  {
    JavaClass baseClass = null;
    JavaClass extClass = null;

    ByteCodeParser parser = new ByteCodeParser();
    parser.setClassLoader(getJavaClassLoader());

    ReadStream is = extPath.openRead();
    try {
      extClass = parser.parse(is);
    } finally {
      if (is != null)
        is.close();
    }

    cleanExtConstantPool(className, extClass);

    postEnhance(baseClass);

    WriteStream os = targetPath.openWrite();
    try {
      extClass.write(os);
    } finally {
      os.close();
    }
  }

  /**
   * After enhancement fixup.
   */
  protected void postEnhance(JavaClass baseClass)
    throws Exception
  {
    for (int i = 0; i < _enhancerList.size(); i++) {
      _enhancerList.get(i).postEnhance(baseClass);
    }

    fixupJdk16Methods(baseClass);
  }

  /**
   * Merges the two classes.
   */
  protected void mergeClasses(String className,
                              JavaClass baseClass,
                              JavaClass extClass)
    throws Exception
  {
    if (baseClass.getMajor() < extClass.getMajor()) {
      baseClass.setMajor(extClass.getMajor());
      baseClass.setMinor(extClass.getMinor());
    }

    cleanExtConstantPool(className, extClass);
    renameExtSuperMethods(className, baseClass, extClass);

    cleanExtConstantPool(className, baseClass);

    addExtInterfaces(baseClass, extClass);

    addExtFields(baseClass, extClass);

    moveSuperMethods(className, baseClass, extClass);

    addExtMethods(baseClass, extClass);

    copyExtAnnotations(baseClass);

    addExtClasses(baseClass, extClass);
  }

  /**
   * Cleans the ext constant pool, renaming
   */
  protected void cleanExtConstantPool(String className, JavaClass extClass)
    throws Exception
  {
    extClass.setThisClass(replaceString(className, extClass.getThisClass()));
    extClass.setSuperClass(replaceString(className, extClass.getSuperClassName()));

    ArrayList<ConstantPoolEntry> entries;
    entries = extClass.getConstantPool().getEntries();

    int t = className.lastIndexOf('.');
    if (t > 0)
      className = className.substring(t + 1);

    String baseName = className + _baseSuffix;
    String extName = className + "__ResinExt";

    for (int i = 0; i < entries.size(); i++) {
      ConstantPoolEntry entry = entries.get(i);

      if (entry instanceof Utf8Constant) {
        Utf8Constant utf8 = (Utf8Constant) entry;

        String string = utf8.getValue();

        string = replaceString(className, string);

        utf8.setValue(string);
      }
    }

    ArrayList<JavaField> fields = extClass.getFieldList();
    for (int i = 0; i < fields.size(); i++) {
      JavaField field = fields.get(i);

      field.setName(replaceString(className, field.getName()));
      field.setDescriptor(replaceString(className, field.getDescriptor()));
    }

    ArrayList<JavaMethod> methods = extClass.getMethodList();
    for (int i = 0; i < methods.size(); i++) {
      JavaMethod method = methods.get(i);

      method.setName(replaceString(className, method.getName()));
      method.setDescriptor(replaceString(className, method.getDescriptor()));
    }
  }

  /**
   * Adds the methods from the ext to the base
   */
  private void copyExtAnnotations(JavaClass baseClass)
  {
    for (JavaMethod method : baseClass.getMethodList()) {
      if (method.getName().endsWith("__super")) {
        Attribute ann = method.getAttribute("RuntimeVisibleAnnotations");

        if (ann != null) {
          String name = method.getName();
          name = name.substring(0, name.length() - "__super".length());

          JavaMethod baseMethod;
          baseMethod = findMethod(baseClass, name, method.getDescriptor());

          if (baseMethod != null)
            baseMethod.addAttribute(ann);
        }
      }
    }
  }

  /**
   * Adds the methods from the ext to the base
   */
  private void addExtFields(JavaClass baseClass, JavaClass extClass)
  {
    ArrayList<JavaField> fields = baseClass.getFieldList();

    for (JavaField extField : extClass.getFieldList()) {
      JavaField field = extField.export(extClass, baseClass);

      if (! fields.contains(field))
        fields.add(field);
    }
  }

  /**
   * Remove the StackMapTable
   */
  private void fixupJdk16Methods(JavaClass baseClass)
  {
    for (JavaMethod method : baseClass.getMethodList()) {
      CodeAttribute code = method.getCode();

      code.removeAttribute("StackMapTable");
    }
  }

  /**
   * Remove the LocalVariableTable
   */
  private void fixupLocalVariableTable(JavaClass extClass)
  {
    for (JavaMethod method : extClass.getMethodList()) {
      CodeAttribute code = method.getCode();

      code.removeAttribute("LocalVariableTable");
      code.removeAttribute("LocalVariableTypeTable");
    }
  }

  private String replaceString(String className, String string)
  {
    string = replaceStringInt(className.replace('.', '/'), string);
    string = replaceStringInt(className.replace('.', '$'), string);
    string = replaceStringInt(className.replace('.', '-'), string);

    return string;
  }

  private String replaceStringInt(String className, String string)
  {
    int t = className.lastIndexOf('.');
    if (t > 0)
      className = className.substring(t + 1);

    String baseName = className + _baseSuffix;
    // String extName = className + "__ResinExt";
    String extName = "__ResinExt";

    int p;
    if (! baseName.equals(className)) {
      while ((p = string.indexOf(baseName)) >= 0) {
        String prefix = string.substring(0, p);
        String suffix = string.substring(p + baseName.length());

        string = prefix + className + suffix;
      }
    }

    while ((p = string.indexOf(extName)) >= 0) {
      String prefix = string.substring(0, p);
      String suffix = string.substring(p + extName.length());

      // string = prefix + className + suffix;
      string = prefix + suffix;
    }

    return string;
  }

  private static class InitAnalyzer extends Analyzer {
    int _offset = -1;

    /**
     * Returns the analyzed offset.
     */
    public int getOffset()
    {
      return _offset;
    }

    /**
     * Analyzes the opcode.
     */
    public void analyze(CodeVisitor visitor)
      throws Exception
    {
      if (_offset >= 0)
        return;

      switch (visitor.getOpcode()) {
      case CodeVisitor.INVOKESPECIAL:
        JavaClass javaClass = visitor.getJavaClass();
        ConstantPool cp = javaClass.getConstantPool();
        MethodRefConstant ref = cp.getMethodRef(visitor.getShortArg());

        // ejb/0l00
        // handler "super()" and "this()"
        if (ref.getName().equals("<init>")
            && (ref.getClassName().equals(javaClass.getThisClass())
                || ref.getClassName().equals(javaClass.getSuperClassName()))) {
          _offset = visitor.getOffset() + 3;
        }
        break;
      }
    }
  }

  //
  // convert super.foo() calls to foo__super() where appropriate
  //
  private static class ExtMethodAnalyzer extends Analyzer {
    JClass _baseClass;
    JMethod _method;
    int _startOffset;
    boolean _isEnhanced;

    ExtMethodAnalyzer(JClass baseClass, JMethod method, int length)
    {
      _baseClass = baseClass;
      _method = method;
      _startOffset = length;
    }

    /**
     * Analyzes the opcode.
     */
    public void analyze(CodeVisitor visitor)
      throws Exception
    {
      if (_isEnhanced)
        return;

      if (visitor.getOffset() < _startOffset)
        return;

      switch (visitor.getOpcode()) {
      case CodeVisitor.INVOKESPECIAL:
        int index = visitor.getShortArg();

        JavaClass jClass = visitor.getJavaClass();
        ConstantPool cp = jClass.getConstantPool();
        MethodRefConstant ref;
        ref = cp.getMethodRef(index);

        if (ref.getName().endsWith("__super")) {
          return;
        }
        else if (ref.getName().equals("<init>")
                 && (! ref.getClassName().equals(jClass.getSuperClassName())
                     || ! _method.getName().equals("<init>"))) {
          return;
        }
        else if (! ref.getName().equals("<init>")) {
          // private methods are called with invokespecial, but shouldn't
          // be modified
          JMethod method = findMethod(jClass,
                                      ref.getName(),
                                      ref.getType());

          if (method != null && method.isPrivate())
            return;
        }

        String superName;
        if (ref.getName().equals("<init>"))
          superName = "__init__super";
        else
          superName = ref.getName() + "__super";

        MethodRefConstant newRef;
        newRef = cp.addMethodRef(ref.getClassName(),
                                 superName,
                                 ref.getType());

        visitor.setShortArg(1, newRef.getIndex());

        _isEnhanced = true;
        break;
      }
    }
  }
}
