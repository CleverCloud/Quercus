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

package com.caucho.jsp.java;

import com.caucho.VersionFactory;
import com.caucho.config.types.Signature;
import com.caucho.jsp.JspParseException;
import com.caucho.jsp.ParseTagManager;
import com.caucho.jsp.TempTagInfo;
import com.caucho.jsp.cfg.TldAttribute;
import com.caucho.jsp.cfg.TldTag;
import com.caucho.jsp.cfg.TldVariable;
import com.caucho.util.L10N;

import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagLibraryInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generates JSP code.  JavaGenerator, JavaScriptGenerator, and
 * StaticGenerator specialize the JspGenerator for language-specific
 * requirements.
 *
 * <p>JspParser parses the JSP file into an XML-DOM tree.  JspGenerator
 * generates code from that tree.
 */
public class JavaTagGenerator extends JavaJspGenerator {
  static final L10N L = new L10N(JavaTagGenerator.class);
  static final Logger log
    = Logger.getLogger(JavaTagGenerator.class.getName());

  private static HashSet<String> _reserved
    = new HashSet<String>();

  private String _description = null;
  private String _displayName = null;
  private String _smallIcon = null;
  private String _largeIcon = null;
  private String _example = null;
  private String _bodyContent = null;
  private String _dynamicAttributes = null;

  private ArrayList<TldAttribute> _attributes = new ArrayList<TldAttribute>();
  private ArrayList<TldVariable> _variables = new ArrayList<TldVariable>();

  private TempTagInfo _tagInfo = new TempTagInfo();

  public JavaTagGenerator(ParseTagManager tagManager)
  {
    super(tagManager);

    setOmitXmlDeclaration(true);
  }

  public void init()
  {
    super.init();
    
    setOmitXmlDeclaration(true);
  }

  /**
   * Returns true if the XML declaration should be ignored.
   */
  /*
  boolean isOmitXmlDeclaration()
  {
    // tags always omit the declaration
    return true;
  }
  */

  public void setDescription(String description)
  {
    _description = description;
  }

  public String getDescription()
  {
    return _description;
  }

  public void setDisplayName(String displayName)
  {
    _displayName = displayName;
  }

  public String getDisplayName()
  {
    return _displayName;
  }

  public void setSmallIcon(String smallIcon)
  {
    _smallIcon = smallIcon;
  }

  public String getSmallIcon()
  {
    return _smallIcon;
  }

  public void setLargeIcon(String largeIcon)
  {
    _largeIcon = largeIcon;
  }

  public String getLargeIcon()
  {
    return _largeIcon;
  }

  public void setExample(String example)
  {
    _example = example;
  }

  public String getExample()
  {
    return _example;
  }

  /**
   * Sets the body content.
   */
  public void setBodyContent(String bodyContent)
  {
    _bodyContent = bodyContent;
  }

  /**
   * Gets the body content.
   */
  public String getBodyContent()
  {
    return _bodyContent;
  }

  /**
   * Sets the name of the dynamic attributes map
   */
  public void setDynamicAttributes(String dynamicAttributes)
  {
    _dynamicAttributes = dynamicAttributes;
  }

  /**
   * Gets the body content.
   */
  public String getDynamicAttributes()
  {
    return _dynamicAttributes;
  }

  /**
   * Adds an attribute.
   */
  public void addAttribute(TldAttribute attribute)
  {
    _attributes.add(attribute);
  }

  /**
   * Returns the attributes.
   */
  public ArrayList<TldAttribute> getAttributes()
  {
    return _attributes;
  }

  /**
   * Finds an attribute.
   */
  public TldAttribute findAttribute(String name)
  {
    for (int i = 0; i < _attributes.size(); i++) {
      TldAttribute attr = _attributes.get(i);

      if (name.equals(attr.getName()))
        return attr;
    }

    return null;
  }

  /**
   * Adds a variable.
   */
  public void addVariable(TldVariable var)
  {
    _variables.add(var);
  }

  /**
   * Finds a variable.
   */
  public TldVariable findVariable(String name)
  {
    for (int i = 0; i < _variables.size(); i++) {
      TldVariable var = _variables.get(i);

      // jsp/1071, jsp/106g (tck)
      if (name.equals(var.getNameGiven())
          || name.equals(var.getAlias()))
        return var;
    }

    return null;
  }

  /**
   * Finds a variable.
   */
  public TldVariable findNameFromAttributeVariable(String name)
  {
    for (int i = 0; i < _variables.size(); i++) {
      TldVariable var = _variables.get(i);

      // jsp/106g (tck)
      if (name.equals(var.getNameFromAttribute()))
        return var;
    }

    return null;
  }

  /**
   * Returns the variables.
   */
  public ArrayList<TldVariable> getVariables()
  {
    return _variables;
  }

  public boolean isTag()
  {
    return true;
  }

  /**
   * Returns true for XML.
   */
  boolean isXml()
  {
    return _parseState.isXml();
  }

  /**
   * Generates the Java code.
   */
  protected void generate(JspJavaWriter out)
    throws Exception
  {
    out.setLineMap(_lineMap);
    
    generateClassHeader(out);

    generateAttributes(out);

    if (_dynamicAttributes != null)
      generateDynamicAttributes(out);

    generateDoTag(out, _rootNode);

    // if (isStaticDoTag())
    //  generateStaticDoTag(out, _rootNode);
    
    generateDoTagImpl(out, _rootNode);

    generateTagInfo(out);
    
    generateClassFooter(out);
  }

  protected boolean isStaticDoTag()
  {
    // return ! hasScripting();
    return false;
  }

  /**
   * Generates the class header.
   *
   * @param doc the XML document representing the JSP page.
   */
  protected void generateClassHeader(JspJavaWriter out)
    throws IOException, JspParseException
  {
    out.println("/*");
    out.println(" * JSP-Tag generated by " + VersionFactory.getFullVersion());
    out.println(" */" );
    out.println();

    if (_pkg != null && ! _pkg.equals(""))
      out.println("package " + _pkg + ";");

    out.println("import javax.servlet.*;");
    out.println("import javax.servlet.jsp.*;");
    out.println("import javax.servlet.http.*;");

    fillSingleTaglibImports();

    ArrayList<String> imports = _parseState.getImportList();
    for (int i = 0; i < imports.size(); i++) {
      String name = imports.get(i);
      out.print("import ");
      out.print(name);
      out.println(";");
    }
    _parseState.addImport("javax.servlet.*");
    _parseState.addImport("javax.servlet.jsp.*");
    _parseState.addImport("javax.servlet.http.*");
    _parseState.addImport("java.lang.*");
    out.println();

    out.print("public class ");
    out.print(_className);

    if (hasScripting())
      out.print(" extends com.caucho.jsp.java.JspTagSupport");
    else
      out.print(" extends com.caucho.jsp.java.JspTagFileSupport");

    if (_dynamicAttributes != null)
      out.print(" implements javax.servlet.jsp.tagext.DynamicAttributes");

    out.println(" {");
    out.pushDepth();

    // jsp/102e, jsp/102g
    if (_rootNode != null && _rootNode.hasCustomTag()) {
      out.println("public static final boolean _caucho_hasCustomTag = true;");
    } else {
      out.println("public static final boolean _caucho_hasCustomTag = false;");
    }
    
    out.print("static "); // XXX: this shouldn't be created each tag
    out.println("private final java.util.HashMap<String,java.lang.reflect.Method> _jsp_functionMap = new java.util.HashMap<String,java.lang.reflect.Method>();");
    // jsp/107{0,1}
    out.println("private static com.caucho.jsp.PageManager _jsp_pageManager;");

    out.println("private boolean _caucho_isDead;");
    out.println("private boolean _caucho_isNotModified;");

    for (int i = 0; i < _declarations.size(); i++) {
      JspDeclaration decl = _declarations.get(i);

      out.println();
      decl.generateDeclaration(out);
    }
  }

  /**
   * Generates the attribute definitions.
   *
   * @param out the writer to the .java source
   */
  protected void generateAttributes(JspJavaWriter out)
    throws IOException, JspParseException
  {
    for (int i = 0; i < _attributes.size(); i++) {
      TldAttribute attr = _attributes.get(i);

      String name = attr.getName();

      String upperName;
      char ch = name.charAt(0);
      upperName = Character.toUpperCase(ch) + name.substring(1);

      Class cl = attr.getType();
      if (cl == null)
        cl = String.class;
      String type = cl.getName();

      String isSetName = "_jsp_" + name + "_isSet";

      String fieldName = toFieldName(name);

      out.println();
      out.print("private ");
      out.printClass(cl);
      out.println(" " + fieldName + ";");
      out.println("private boolean " + isSetName + ";");
      
      out.println();
      out.print("public void set" + upperName + "(");
      out.printClass(cl);
      out.println(" value)");
      out.println("{");
      out.pushDepth();
      out.println("this." + isSetName + " = true;");
      out.println("this." + fieldName + " = value;");
      out.popDepth();
      out.println("}");

      /*
      // jsp/101f
      out.println();
      out.println("public " + type + " get" + upperName + "()");
      out.println("{");
      out.pushDepth();
      out.println("return " + fieldName + ";");
      out.popDepth();
      out.println("}");
      */
    }
  }

  /**
   * Generates the attribute definitions.
   *
   * @param out the writer to the .java source
   */
  protected void generateDynamicAttributes(JspJavaWriter out)
    throws IOException, JspParseException
  {
    String dyn = toFieldName(_dynamicAttributes);

    out.println();
    out.println("java.util.HashMap " + dyn + " = new java.util.HashMap();");
    out.println();
    out.println("public void setDynamicAttribute(String uri, String localName, Object value)");
    out.println("  throws javax.servlet.jsp.JspException");
    out.println("{");
    out.println("  if (uri == null || \"\".equals(uri))");
    out.println("    " + dyn + ".put(localName, value);");
    out.println("}");
  }

  /**
   * Prints the _jspService header
   */
  protected void generateDoTag(JspJavaWriter out, JspNode node)
    throws Exception
  {
    out.println();
    out.println("public void doTag()");
    out.println("  throws javax.servlet.jsp.JspException, java.io.IOException");
    out.println("{");
    out.pushDepth();

    out.println("javax.servlet.jsp.JspContext _jsp_parentContext = getJspContext();");
    out.println("com.caucho.jsp.PageContextWrapper pageContext = _jsp_pageManager.createPageContextWrapper(_jsp_parentContext);");
    // jsp/1056
    out.println("setJspContext(pageContext);");

    if (false && hasScripting()) {
      out.println("javax.servlet.http.HttpServletRequest request = (javax.servlet.http.HttpServletRequest) pageContext.getRequest();");
      out.println("javax.servlet.http.HttpServletResponse response = (javax.servlet.http.HttpServletResponse) pageContext.getResponse();");
      out.println("javax.servlet.http.HttpSession session = pageContext.getSession();");
      out.println("javax.servlet.ServletContext application = pageContext.getServletContext();");
      out.println("javax.servlet.ServletConfig config = pageContext.getServletConfig();");
    }

    out.println("com.caucho.jsp.PageContextWrapper jspContext = pageContext;");
    out.println("javax.el.ELContext _jsp_env = pageContext.getELContext();");
    out.println("javax.servlet.jsp.JspWriter out = pageContext.getOut();");
    
    // generateTagAttributes(out);
    
    //if (hasScripting())
    //  generatePrologue(out);

    out.println("try {");
    out.pushDepth();
     
    // jsp/10a2
    if (false && hasScripting()) {
      out.println("TagState _jsp_state = new TagState();");
      // jsp/100h
      out.println("javax.servlet.jsp.tagext.JspTag _jsp_parent_tag");
      out.println("  = new javax.servlet.jsp.tagext.TagAdapter(this);");

      node.generate(out);
      
      generateTagVariablesAtEnd(out);
    } else {
      out.println("doTag(_jsp_parentContext, pageContext, out, null, this);");
    }
    
    out.popDepth();
    out.println("} catch (Throwable e) {");
    out.println("  if (e instanceof java.io.IOException)");
    out.println("    throw (java.io.IOException) e;");
    out.println("  throw com.caucho.jsp.QJspException.createJspException(e);");
    out.println("}");

    if (hasScripting() && _variables.size() > 0) {
      out.println("finally {");
      out.pushDepth();
      
      // generateTagVariablesAtEnd(out);
      
      out.println("setJspContext(_jsp_parentContext);");
      out.println("_jsp_pageManager.freePageContextWrapper(pageContext);");
      
      out.popDepth();
      out.println("}");
    }
  
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the attribute definitions.
   *
   * @param out the writer to the .java source
   */
  protected void generateTagAttributes(JspJavaWriter out)
    throws IOException, JspParseException
  {
    for (int i = 0; i < _attributes.size(); i++) {
      TldAttribute attr = _attributes.get(i);

      String name = attr.getName();

      String upperName;
      char ch = name.charAt(0);
      upperName = Character.toUpperCase(ch) + name.substring(1);

      Class cl = attr.getType();
      if (cl == null)
        cl = String.class;
      String type = cl.getName();

      String isSetName = "_jsp_" + name + "_isSet";
      String fieldName = toFieldName(name);
      
      out.println("if (" + isSetName + ")");
      out.println("  pageContext.setAttribute(\"" + name + "\", " +
                  JspNode.toELObject(fieldName, cl) + ");");
    }

    // jsp/10a1
    if (_dynamicAttributes != null) {
      out.println("pageContext.setAttribute(\"" + _dynamicAttributes + "\"," +
                  toFieldName(_dynamicAttributes) + ");");
    }
  }

  /**
   * Prints the _jspService header
   */
  protected void generateDoTagImpl(JspJavaWriter out, JspNode node)
    throws Exception
  {
    out.println();
    out.println("public void doTag(javax.servlet.jsp.JspContext _jsp_parentContext,");
    out.println("                         com.caucho.jsp.PageContextWrapper pageContext,");
    out.println("                         javax.servlet.jsp.JspWriter out,");
    out.println("                         javax.servlet.jsp.tagext.JspFragment _jspBody,");
    out.println("                         javax.servlet.jsp.tagext.JspTag jsp_parent_tag)");
    out.println("  throws Throwable");
    out.println("{");
    out.pushDepth();

    out.println("javax.el.ELContext _jsp_env = pageContext.getELContext();");

    if (node.hasScripting()) {
      out.println("javax.servlet.http.HttpServletRequest request = (javax.servlet.http.HttpServletRequest) pageContext.getRequest();");
      out.println("javax.servlet.http.HttpServletResponse response = (javax.servlet.http.HttpServletResponse) pageContext.getResponse();");
      out.println("javax.servlet.http.HttpSession session = pageContext.getSession();");
      out.println("javax.servlet.ServletContext application = pageContext.getServletContext();");
      out.println("javax.servlet.ServletConfig config = pageContext.getServletConfig();");
      out.println("com.caucho.jsp.PageContextWrapper jspContext = pageContext;");
    }
    
    /*
    out.println("if (_jspBody != null)");
    out.println("  setJspBody(_jspBody);"); // jsp/1025 vs jsp/102h
    */
    
    out.println("TagState _jsp_state = new TagState();");
    out.println("javax.servlet.jsp.tagext.JspTag _jsp_parent_tag = jsp_parent_tag;");

    generateTagAttributes(out);
    
    generatePrologue(out);

    //out.println("try {");
    //out.pushDepth();
    
    node.generate(out);

    //out.popDepth();
    //out.println("} finally {");
    //out.pushDepth();
      
    generateTagVariablesAtEnd(out);
      
    //out.println("_jsp_pageManager.freePageContextWrapper(pageContext);");
      
    //out.popDepth();
    //out.println("}");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates prologue stuff
   *
   * @param out the writer to the .java source
   */
  protected void generatePrologue(JspJavaWriter out)
    throws Exception
  {
    _rootNode.generatePrologue(out);
    
    for (int i = 0; i < _variables.size(); i++) {
      TldVariable var = _variables.get(i);

      if (var.getNameFromAttribute() != null) {
        out.print("String _jsp_var_from_attribute_" + i + " = (String) ");
        out.println("pageContext.getAttribute(\"" +
                    var.getNameFromAttribute() + "\");");
      }
      
      if ("AT_END".equals(var.getScope()))
        continue;

      String srcName = var.getNameGiven();
      if (srcName == null)
        srcName = var.getAlias();
      
      String dstName;
      if (var.getNameGiven() != null)
        dstName = "\"" + var.getNameGiven() + "\"";
      else
        dstName = "_jsp_var_from_attribute_" + i;

      if ("NESTED".equals(var.getScope())) {
        out.print("Object _jsp_nested_var_" + i + " = ");
        out.println("_jsp_parentContext.getAttribute(" + dstName + ");");
      }
      /*
      else {
        out.print("pageContext.setAttribute(\"" + srcName + "\",");
        out.println("_jsp_parentContext.getAttribute(" + dstName + "));");
      }
      */
    }
  }

  /**
   * Generates the variable setting.
   *
   * @param out the writer to the .java source
   */
  protected void generateTagVariablesAtEnd(JspJavaWriter out)
    throws IOException, JspParseException
  {
    for (int i = 0; i < _variables.size(); i++) {
      TldVariable var = _variables.get(i);
      
      String srcName = var.getNameGiven();
      if (srcName == null)
        srcName = var.getAlias();
      
      String dstName;
      if (var.getNameGiven() != null)
        dstName = "\"" + var.getNameGiven() + "\"";
      else
        dstName = "_jsp_var_from_attribute_" + i;

      if ("NESTED".equals(var.getScope())) {
        out.println("_jsp_parentContext.setAttribute(" + dstName + ", _jsp_nested_var_" + i + ");");
      }
      else {
        out.print("_jsp_parentContext.setAttribute(" + dstName + ",");
        out.println("pageContext.getAttribute(\"" + srcName + "\"));");
      }
    }
  }

  public TagInfo generateTagInfo(String className, TagLibraryInfo taglib)
  {
    init(className);
    
    TldTag tag = new TldTag();
    
    for (int i = 0; i < _attributes.size(); i++) {
      TldAttribute attr = _attributes.get(i);

      tag.addAttribute(attr);
    }
    
    for (int i = 0; i < _variables.size(); i++) {
      TldVariable var = _variables.get(i);

      try {
        tag.addVariable(var);
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
    
    String bodyContent = _bodyContent;
    if (bodyContent == null)
      bodyContent = "scriptless";
    
    return new TagInfoExt(tag.getName(),
                          _fullClassName,
                          bodyContent,
                          getDescription(),
                          taglib,
                          null,
                          tag.getAttributes(),
                          getDisplayName(),
                          getSmallIcon(),
                          getLargeIcon(),
                          tag.getVariables(),
                          _dynamicAttributes != null,
                          _dynamicAttributes,
                          null);
  }
  
  /**
   * Generates the attribute definitions.
   *
   * @param out the writer to the .java source
   */
  protected void generateTagInfo(JspJavaWriter out)
    throws IOException, JspParseException
  {
    /*
    out.println();
    out.println("public javax.servlet.jsp.tagext.TagInfo _caucho_getTagInfo()");
    out.println("  throws com.caucho.config.ConfigException");
    out.println("{");
    out.pushDepth();
    out.println("  return _caucho_getTagInfo(_caucho_getTagLibrary());");
    out.popDepth();
    out.println("}");
    */
    
    out.println();
    out.println("public javax.servlet.jsp.tagext.TagInfo _caucho_getTagInfo(javax.servlet.jsp.tagext.TagLibraryInfo taglib)");
    out.println("  throws com.caucho.config.ConfigException");
    out.println("{");
    out.pushDepth();
    out.println("com.caucho.jsp.cfg.TldTag tag = new com.caucho.jsp.cfg.TldTag();");

    out.println("tag.setName(\"test\");");

    out.println("com.caucho.jsp.cfg.TldAttribute attr;");
    for (int i = 0; i < _attributes.size(); i++) {
      TldAttribute attr = _attributes.get(i);

      out.println("attr = new com.caucho.jsp.cfg.TldAttribute();");
      out.println("attr.setName(\"" + attr.getName() + "\");");

      Class type = attr.getType();
      if (type != null) {
        out.print("attr.setType(");
        out.printClass(type);
        out.println(".class);");
      }
      out.println("attr.setRtexprvalue(" + attr.getRtexprvalue() + ");");
      out.println("attr.setRequired(" + attr.getRequired() + ");");

      if (attr.getDeferredValue() != null) {
        out.println("attr.setDeferredValue(new com.caucho.jsp.cfg.TldAttribute.DeferredValue());");

        if (attr.getDeferredValue().getType() != null) {
          out.print("attr.getDeferredValue().setType(\"");
          out.printJavaString(attr.getDeferredValue().getType());
          out.println("\");");
        }
      }

      if (attr.getDeferredMethod() != null) {
        out.println("attr.setDeferredMethod(new com.caucho.jsp.cfg.TldAttribute.DeferredMethod());");

        Signature sig = attr.getDeferredMethod().getMethodSignature();

        if (sig != null) {
          out.print("attr.getDeferredMethod().setMethodSignature(");
          out.print("new com.caucho.config.types.Signature(\"");
          out.printJavaString(sig.getSignature());
          out.println("\"));");
        }
      }

      out.println("tag.addAttribute(attr);");
    }

    out.println("com.caucho.jsp.cfg.TldVariable var;");
    for (int i = 0; i < _variables.size(); i++) {
      TldVariable var = _variables.get(i);

      out.println("var = new com.caucho.jsp.cfg.TldVariable();");

      if (var.getNameGiven() != null)
        out.println("var.setNameGiven(\"" + var.getNameGiven() + "\");");
      
      if (var.getNameFromAttribute() != null)
        out.println("var.setNameFromAttribute(\"" + var.getNameFromAttribute() + "\");");

      String type = var.getVariableClass();
      if (type != null)
        out.println("var.setVariableClass(\"" + type + "\");");
      out.println("var.setDeclare(" + var.getDeclare() + ");");
      if (var.getScope() != null)
        out.println("var.setScope(\"" + var.getScope() + "\");");

      out.println("tag.addVariable(var);");
    }
    
    String bodyContent = _bodyContent;
    if (bodyContent == null)
      bodyContent = "scriptless";

    out.println("return new com.caucho.jsp.java.TagInfoExt(tag.getName(),");
    out.println("                   getClass().getName(),");
    out.println("                   \"" + bodyContent + "\",");
    out.print("                   \"");
    if (_description != null)
      out.printJavaString(_description);
    else
      out.printJavaString("A simple tag");
    out.println("\",");
    out.println("                   taglib,");
    out.println("                   null,");
    out.println("                   tag.getAttributes(),");
    
    if (_displayName != null) {
      out.print("                   \"");
      out.printJavaString(_displayName);
      out.println("\",");
    }
    else {
      out.println("                   null,");
    }
    
    if (_smallIcon != null) {
      out.print("                   \"");
      out.printJavaString(_smallIcon);
      out.println("\",");
    }
    else {
      out.println("                   null,");
    }
    
    if (_largeIcon != null) {
      out.print("                   \"");
      out.printJavaString(_largeIcon);
      out.println("\",");
    }
    else {
      out.println("                   null,");
    }
    
    out.println("                   tag.getVariables(),");
    out.println("                   " + (_dynamicAttributes != null) + ",");
    if (_dynamicAttributes != null)
      out.println("                   \"" + _dynamicAttributes + "\",");
    else
      out.println("                   null,");
    out.println("                   _caucho_depends.getDependencies());");

    out.popDepth();
    out.println("}");
    
    out.println();
    out.println("public String _caucho_getDynamicAttributes()");
    out.println("{");
    out.pushDepth();

    if (_dynamicAttributes != null)
      out.println("return \"" + _dynamicAttributes + "\";");
    else
      out.println("return null;");
    
    out.popDepth();
    out.println("}");

    generateTagLibrary(out);
  }
  
  /**
   * Generates the attribute definitions.
   *
   * @param out the writer to the .java source
   */
  protected void generateTagLibrary(JspJavaWriter out)
    throws IOException, JspParseException
  {
    out.println();
    out.println("private javax.servlet.jsp.tagext.TagLibraryInfo _caucho_getTagLibrary()");
    out.println("  throws com.caucho.config.ConfigException");
    out.println("{");
    out.pushDepth();

    out.println("return new com.caucho.jsp.java.TagTaglib(\"x\", \"http://test.com\");");
    out.popDepth();
    out.println("}");
  }

  private String toFieldName(String name)
  {
    /*n
    if (hasScripting() && ! _reserved.contains(name))
      return name;
    else
      return "_" + name;
      */
    
    if (! _reserved.contains(name))
      return name;
    else
      return "_" + name;
  }

  static {
    _reserved.add("public");
    _reserved.add("private");
    _reserved.add("protected");
    _reserved.add("static");
    _reserved.add("final");
    _reserved.add("class");
    _reserved.add("module");
    _reserved.add("interface");
    _reserved.add("extends");
    _reserved.add("implements");
    _reserved.add("package");
    _reserved.add("import");
    _reserved.add("new");
    _reserved.add("if");
    _reserved.add("else");
    _reserved.add("for");
    _reserved.add("do");
    _reserved.add("while");
    _reserved.add("break");
    _reserved.add("continue");
    _reserved.add("switch");
    _reserved.add("case");
    _reserved.add("default");
    _reserved.add("throw");
    _reserved.add("enum");
    _reserved.add("throws");
    
    _reserved.add("void");
    _reserved.add("boolean");
    _reserved.add("byte");
    _reserved.add("char");
    _reserved.add("short");
    _reserved.add("int");
    _reserved.add("long");
    _reserved.add("float");
    
    _reserved.add("true");
    _reserved.add("false");
    _reserved.add("null");
  }
}
