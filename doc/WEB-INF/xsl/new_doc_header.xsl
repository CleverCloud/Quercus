<!--
   - Formats the top level of the template.  header.xsl assumes the XTP
   - has been read in as HTML.  So it can assume the existence of the
   - html and body tags.
  -->

<xsl:stylesheet>

<#@ page import='com.caucho.web.*' #>
<#@ page import='com.caucho.vfs.*' #>
<#@ page import='com.caucho.server.webapp.*' #>
<#@ page import='javax.xml.parsers.*' #>
<#@ page import='javax.servlet.jsp.*' #>
<#@ page import='javax.servlet.*' #>
<#@ page import='javax.servlet.http.*' #>
<#@ cache #>
<#!
  String top = "/";
  String title = null;
  Navigation nav = null;

  String topnav(Object a, Object b, Object c) { return ""; }

  void initNavigation(Env env, XslWriter out)
    throws Exception
  {
    PageContext page = (PageContext) out.getProperty("caucho.page.context");

    if (page == null)
      return;

    ServletContext app = page.getServletContext();
    HttpServletRequest req = (HttpServletRequest) page.getRequest();

    String url = req.getRequestURI();
    int p = url.lastIndexOf('/');
    String base = p > 0 ? url.substring(0,p) : "/";

    ArrayList paths = new ArrayList();
    while (p >= 0) {
      String prefix = url.substring(0, p + 1);

      Application subapp = (Application) app.getContext(prefix);
      if (subapp != null) {
        String rel = prefix.substring(subapp.getContextPath().length());

        String realPath = subapp.getRealPath(rel);
        Path path = ((Path) out.getProperty("caucho.pwd")).lookupNative(realPath);

        paths.add(path);
        if (path.lookup("toc.xml").exists())
          out.addCacheDepend(path.lookup("toc.xml"));

      }
      p = url.lastIndexOf('/', p - 1);
    }

    nav = Navigation.createNested(env, paths, base);

    if (nav == null)
      nav = new Navigation();

    top = req.getContextPath();
    if (! top.endsWith("/"))
      top = top + "/";
  }

  void writeThreaded(XslWriter out, boolean allowComments)
    throws IOException, SAXException, XPathException
  {
    PageContext page = out.getPage();
    HttpServletRequest req = (HttpServletRequest) page.getRequest();

    NavItem item = nav.findURL(req.getRequestURI());
    if (item == null)
      return;

    out.pushElement("table");
    out.setAttribute("border", "0");
    out.setAttribute("cellspacing", "0");
    out.setAttribute("width", "100%");
    out.pushElement("tr");
    out.pushElement("td");

    NavItem prev = item.getPreviousPreorder();
    if (prev == null)
      out.print("&nbsp;");
    else {
      out.pushElement("a");
      out.setAttribute("href", prev.getLink());
      out.print(prev.getTitle());
      out.popElement();
    }

    out.popElement();

    if (item.getParent() != null) {
      NavItem parent = item.getParent();

      out.pushElement("td");
      out.setAttribute("width", "100%");

      out.pushElement("center");
      out.pushElement("a");
      out.setAttribute("href", parent.getLink());
      out.print(parent.getTitle());
      out.popElement();
      out.popElement();
      out.popElement();
    }
    else {
      out.pushElement("td");
      out.setAttribute("width", "100%");
      out.print("&nbsp;");
      out.popElement();
    }

    out.pushElement("td");
    out.setAttribute("align", "right");

    NavItem next = item.getNextPreorder();
    if (next == null)
      out.print("&nbsp;");
    else {
      out.pushElement("a");
      out.setAttribute("href", next.getLink());
      out.print(next.getTitle());
      out.popElement();
    }

    out.popElement();
    out.popElement();
    out.popElement();
  }

  boolean isInstalled(String product, Env env)
    throws XPathException
  {
    if (product == null || product.equals(""))
      return true;

    Expr expr = XPath.parseExpr("installed('" + product + "')");

    return expr.evalBoolean(null, env);
  }

  void writeFamilyNavigation(XslWriter out, Env env)
    throws Exception
  {
    PageContext page = (PageContext) out.getProperty("caucho.page.context");
    if (page == null)
      return;
    HttpServletRequest req = (HttpServletRequest) page.getRequest();

    NavItem item = nav.findURL(req.getRequestURI());

    ArrayList list = null;
    if (item != null)
      list = item.familyNavigation();

    if (list == null || list.size() == 0)
      return;

    for (int i = 0; i < list.size(); i++) {
      NavItem child = (NavItem) list.get(i);

      if (child == null) {
        out.pushElement("tr");
        out.pushElement("td");
        out.pushElement("hr");
        out.popElement();
        out.popElement();
        out.popElement();
      } 
      else {
        out.pushElement("tr");
        out.pushElement("td");

        String link = child.getLink();

        out.pushElement("a");
        out.setAttribute("class", "leftnav");
        out.setAttribute("href", link);
        out.println(child.getTitle());

        out.popElement();
        out.popElement();
        out.popElement();
      }
    }
  }

  void topnav(XslWriter out, String name, String href)
    throws IOException, SAXException, XPathException
  {
    out.pushElement("img");
    out.setAttribute("name", name);
    out.setAttribute("src", fileFind(out,false,"images/pixel.gif"));
    out.setAttribute("alt", "");
    out.setAttribute("width", "8");
    out.setAttribute("height", "8");
    out.popElement();

    out.println("&nbsp;");

    out.pushElement("a");
    out.setAttribute("class", "topnav");
    out.setAttribute("href", top + href);
    out.println(name);
    out.popElement();
  }

  void printDescription(XslWriter out, Node node, int count)
    throws IOException, SAXException, XPathException
  {
    if (node == null)
      return;

    CharBuffer cb = CharBuffer.allocate();

    Iterator iter = XPath.select(".//text()|.//resin|.//resintm", node);
    while (iter.hasNext()) {
      Node subnode = (Node) iter.next();

      if (subnode.getNodeName().equals("#text")) {
        String str = subnode.getNodeValue();
        for (int i = 0; i < str.length(); i++) {
          char ch = str.charAt(i);
          if (ch == '<')
            cb.append("&amp;");
          else if (ch != '"' && ch != '\'')
            cb.append(ch);
        }
      }
      else if (subnode.getNodeName().equals("resin"))
        cb.append("Resin");
      else if (subnode.getNodeName().equals("resintm"))
        cb.append("Resin(tm)");

      if (cb.length() >= count)
        break;
    }

    if (cb.length() > count)
      cb.setLength(count);

    if (cb.length() > 0) {
      out.pushElement("meta");
      out.setAttribute("name", "description");
      out.setAttribute("content", cb.close());
      out.popElement();
    }
  }
#>

html
<<
<!-- check for moved document, do redirect -->
<xsl:choose>
  <xsl:when test="//document/header/moved">
<xsl:for-each select="//document/header/moved">
  <xsl:variable name="href">
    <xsl:choose>
      <xsl:when test="contains(@href,'|')">
        <xsl:variable name="url">
          <xsl:call-template name="href.map">
          <xsl:with-param name="href" select="$href"/>
          <xsl:with-param name="text" select="$text"/>
        </xsl:call-template>
        </xsl:variable>
        <xsl:value-of select="$url/href"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="@href"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>


  <% 
  String redirectUrl = "<xsl:value-of select="$href"/>";
  response.sendRedirect(response.encodeRedirectURL(redirectUrl));
  %>
</xsl:for-each>
</xsl:when> <!-- moved document, do redirect -->
<xsl:otherwise> <!-- not [moved document, do redirect] -->

<xsl:call-template name="fun-init"/>

<# 
  initNavigation(env,out);
  title = XPath.evalString("if((/html/body/s1|/html/body/document|/html/body/section)/@title,(/html/body/s1|/html/body/document|/html/body/section)/@title,if((/html/body/s1|/html/body/document|/html/body/section)/title,(/html/body/s1|/html/body/document|/html/body/section)/title,(/html/body/s1|/html/body/document|/html/body/section)/header/title))", node);
  if (title == null)
    title = "";
#>

<html>
<head>
  <xsl:variable name="css" select="file-find('css/default.css')"/>
  <xsl:if test="$css">
    <link rel="STYLESHEET" type="text/css" href="{$css}">
    </link>
  </xsl:if>
<title>
  <xsl:call-template name="m_title">
    <xsl:with-param name="current" select="(body/s1|body/document|body/section)[1]"/>
  </xsl:call-template>
</title>

<xsl:variable name="descr">
  <xsl:choose>
    <xsl:when test="head/@description">
      <xsl:value-of select="head/@description"/>
    </xsl:when>
    <xsl:when test="head/description">
      <xsl:value-of select="head/description"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="m_description">
        <xsl:with-param name="type" select="'paragraph'"/>
        <xsl:with-param name="current" select="(head|body/s1|body/document|body/section)[1]"/>
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>
</xsl:variable>

<xsl:choose>
  <xsl:when test="$descr">
    <meta name="description" content="{normalize-space($descr)}"/>
  </xsl:when>
  <xsl:when test="head/meta[@name='description']">
  </xsl:when>
  <xsl:otherwise>
    <#
      printDescription(out, XPath.find("/html/body", node), 256);
    #>
  </xsl:otherwise>
</xsl:choose>

<xsl:variable name="keywords">
  <xsl:text>j2ee caucho </xsl:text>
  <xsl:for-each select="//header/product">
    <xsl:sort select="text()"/>
    <xsl:if test="not(text() = preceding-sibling::text())">
      <xsl:value-of select="concat(text(), ' ')"/>
    </xsl:if>
  </xsl:for-each>
  <xsl:value-of select="concat(head/title/@keywords, ' ')"/>

  <xsl:for-each select='//header/keywords/keyword'>
    <xsl:value-of select="concat(.,' ')"/>
  </xsl:for-each>
  <xsl:text> </xsl:text>
  <xsl:variable name="bn" select="file-removeext(file-basename(request-servletpath()))"/>
  <xsl:value-of select="if($bn = 'ref','reference',$bn)"/>
  <xsl:text> </xsl:text>
  <xsl:value-of select="file-basename(file-dirname(request-servletpath()))"/>
  <xsl:text> </xsl:text>
</xsl:variable>
<meta name="keywords" content="{normalize-space($keywords)}"/>

$for-each(head/meta) <<
  $copy() << $apply-templates(@*|node()); >>
>>
</head>
<xsl:apply-templates select='body'>
  <xsl:with-param name="section.depth" select="$section.depth"/>
</xsl:apply-templates>
</html>

</xsl:otherwise> <!-- not [moved document, do redirect] -->
</xsl:choose>
>>

html/body
<<
  <body bgcolor="white" leftmargin="0">
  <table cellpadding="0" cellspacing="0" border="0" width="100%" summary="">
  <tr>
    <td width="2">
    <img alt="" width="2" height="1">
      <xsl:attribute name='src'><xsl:value-of select="file-find('images/pixel.gif')"/></xsl:attribute>
    </img>
    </td>
    <td width="150">
      <img width="150" height="63">
         <xsl:attribute name='src'><xsl:value-of select="file-find('images/caucho-white.jpg')"/></xsl:attribute>
      </img>
  </td>
  <td width="10">
    <img alt="" width="10" height="1">
      <xsl:attribute name='src'><xsl:value-of select="file-find('images/pixel.gif')"/></xsl:attribute>
    </img>
  </td>
  <xsl:variable name="topsection" select="(s1|document|section)[1]"/>
  <td width="100%">
    <xsl:comment> top navigation </xsl:comment>
    <table width="100%" cellspacing="0" cellpadding="0" border="0" summary="">
    <tr class="toptitle">
      <td rowspan=2 width="90%">
      <xsl:attribute name='background'><xsl:value-of select="file-find('images/hbleed.gif')"/></xsl:attribute>
        <font class=toptitle size="+3">
          &nbsp;
          <xsl:for-each select="(s1|document|section)">
            <xsl:call-template name="m_title"/>
          </xsl:for-each>
        </font>


        <!-- subtitles -->
        <xsl:for-each select="$topsection/@subtitle | $topsection/subtitle | $topsection/header/subtitle"> 
          <br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<{.}>
        </xsl:for-each>

        <!-- authors -->
        <xsl:for-each select='$topsection/@author | $topsection/header/authors/*'>
          <br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<em>By <{.}></em>
        </xsl:for-each>

        <!-- date -->
        <xsl:for-each select='$topsection/header/@date | $topsection/header/date'>
          <br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<em><{.}></em>
        </xsl:for-each>
      </td>
      <!-- version -->
      <xsl:if test="$topsection/@version | $topsection/header/version"> 
        <td align="right" valign="top">
          <xsl:value-of select="@version"/>
          <xsl:for-each select="$topsection/header/version"> 
            <xsl:if test="position() != 1"><br/></xsl:if>
            <xsl:value-of select="text()"/>
          </xsl:for-each>
        </td>
      </xsl:if>
    </tr>
    </table>
  </td></tr>

  <tr>
  <td colspan="4">
  <!-- vspace -->
  <img alt="" width="1" height="5">
    <xsl:attribute name='src'><xsl:value-of select="file-find('images/pixel.gif')"/></xsl:attribute>
  </img>
  </td></tr>
  <tr>
  <td colspan="2">
    <xsl:attribute name='background'><xsl:value-of select="file-find('images/left_background.gif')"/></xsl:attribute>
  <!-- vspace -->
  <img alt="" width="1" height="20">
    <xsl:attribute name='src'><xsl:value-of select="file-find('images/pixel.gif')"/></xsl:attribute>
  </img>
  </td>
  <td colspan="2">
  <!-- vspace -->
  <img alt="" width="1" height="20">
    <xsl:attribute name='src'><xsl:value-of select="file-find('images/pixel.gif')"/></xsl:attribute>
  </img>
  </td></tr>
  <tr valign="top">
  <td bgcolor="#b9cef7"></td>
  <td bgcolor="#b9cef7">
    <table cellspacing="0" cellpadding="2" border="0" width="100%">
    <!-- Left Navigation -->
    <# writeFamilyNavigation(out, env); #>
    </table>
  </td>
  <td width="10">
    <img alt="" width="10" height="1">
      <xsl:attribute name='src'><xsl:value-of select="file-find('images/pixel.gif')"/></xsl:attribute>
    </img>
  </td>
  <td>

  <#
     if (nav != null && nav.getAttribute("threaded").equals("true")) {
       writeThreaded(out, false);
       out.pushElement("hr");
       out.popElement();
     }
  #>

  <!-- Actual Contents -->
  <xsl:apply-templates/>

  <!-- footer -->
  <hr/>

  <# if (nav != null && nav.getAttribute("threaded").equals("true"))
       writeThreaded(out, true);
  #>

  <table border=0 cellspacing=0 width='100%'>
  <tr><td><em><small>Copyright &copy; 1998-2006 Caucho Technology, Inc. All rights reserved.<br/>
Resin<sup><font size='-1'>&#174;</font></sup> is a registered trademark,
and HardCore<sup>tm</sup> and Quercus<sup>tm</sup> are trademarks of Caucho Technology, Inc.</small></em>
    </td>
    <td align=right><img width=96 height=32>
        <xsl:attribute name='src'>
          <xsl:value-of select="file-find('images/resin-powered.gif')"/>
        </xsl:attribute>
    </img></td>
  </tr>
  </table>

  </td>
  </tr>
  </table>
  </body>
>>

</xsl:stylesheet>

