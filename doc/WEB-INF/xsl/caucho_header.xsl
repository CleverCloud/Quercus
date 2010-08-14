<!--
   - Formats the top level of the template.  header.xsl assumes the XTP
   - has been read in as HTML.  So it can assume the existence of the
   - html and body tags.
  -->

<xsl:stylesheet>
<xsl:output disable-output-escaping="yes"/>
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
  String section = "";
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

    if (false && allowComments && ! nav.getAttribute("comment").equals("")) {
      out.pushElement("td");
      out.setAttribute("width", "100%");

      out.pushElement("jsp:scriptlet");
      out.print("String headerUrl = request.getContextPath() + request.getServletPath();");
      out.popElement();

      out.pushElement("center");
      out.pushElement("a");
      out.setAttribute("href", "/quercus/comment/CommentServlet?cmd=add_comment&comment_url=<%= headerUrl %>");
      out.print("add documentation note");
      out.popElement();
      out.popElement();
      out.popElement();
    }
    else if (item.getParent() != null) {
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
    PageContext page = out.getPage();
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
        out.pushElement("hr");
        out.popElement();
      }
      else {
        String link = child.getLink();
        /*
        if (link.startsWith("/"));
          link = link.substring(1);
        */

        out.pushElement("a");
        out.setAttribute("class", "leftnav");
        out.setAttribute("href", link);
        out.println(child.getTitle());
        out.popElement();

        out.pushElement("br");
        out.popElement();
      }
    }
  }

  void topnav(XslWriter out, String name, String href)
    throws IOException, SAXException
  {
    out.pushElement("a");
    out.setAttribute("class", "topnav");
    out.setAttribute("href", "/" + href);
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
          if (ch == '<' && i + 1 < str.length() && str.charAt(i + 1) == '%') {
            for (; i + 1 < str.length(); i++) {
              if (str.charAt(i) == '%' && str.charAt(i + 1) == '>') {
                i += 1;
                break;
              }
            }
          }
          else if (ch == '<')
            cb.append("&lt;");
	  else if (ch == '>')
            cb.append("&gt;");
	  else if (ch == '&')
            cb.append("&amp;");
	  else if (Character.isWhitespace(ch) && i < 0 &&
                   Character.isWhitespace(str.charAt(i - 1))) {
          }
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

<xsl:comment>ook</xsl:comment>

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
<!--
  <xsl:call-template name="m_title">
    <xsl:with-param name="current" select="(head|body/s1|body/document|body/section)[1]"/>
  </xsl:call-template>
-->
  <xsl:variable name="title" select="(head|body/s1|body/document|body/section)[1]"/>
  <xsl:choose>
  <xsl:when test="$title/title/@browser-title">
    <xsl:value-of select="$title/title/@browser-title"/>
  </xsl:when>
  <xsl:otherwise>
    <xsl:value-of select="$title/@title|$title/title"/>
  </xsl:otherwise>
  </xsl:choose>
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
        <xsl:with-param name="current" select="(body/s1|body/document|body/section)[1]"/>
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

<xsl:template match="title/subtitle">
</xsl:template>

html/body
<<
  <body bgcolor="white" leftmargin="0">
  <xsl:apply-templates select="@*"/>

  <table cellpadding="0" cellspacing="0" border="0" width="100%" summary="">
  <tr>
    <td width="2">
    <img alt="" width="2" height="1">
      <xsl:attribute name='src'>/images/pixel.gif</xsl:attribute>
    </img>
    </td>
    <td width="150">
      <img width="150" height="63">
         <xsl:attribute name='src'>/images/caucho-white.jpg</xsl:attribute>
      </img>
  </td>
  <td width="10">
    <img alt="" width="10" height="1">
      <xsl:attribute name='src'>/images/pixel.gif</xsl:attribute>
    </img>
  </td>

  <xsl:variable name="topsection" select="(/html/head|s1|document|section)[1]"/>
  <td width="100%">
    <xsl:comment> top navigation </xsl:comment>
    <table width="100%" cellspacing="0" cellpadding="0" border="0" summary="">
    <tr class="toptitle">
      <td>
        <xsl:attribute name='background'>
          <xsl:value-of select="file-find('images/hbleed.gif')"/>
        </xsl:attribute>
        <font class=toptitle size="+3">
          &nbsp;
          <xsl:for-each select="$topsection">
            <xsl:call-template name="m_title"/>
          </xsl:for-each>
        </font>

        <!-- subtitles -->
        <xsl:for-each select="$topsection/@subtitle | $topsection/subtitle | $topsection/header/subtitle | $topsection/title/subtitle"> 
          <br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<xsl:apply-templates/>
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

      <td align=right>
      <table cellspacing="0" cellpadding="0" border="0" summary="">
      <tr>
        <td align="left">&nbsp;&nbsp;<# topnav(out, "Home", "index.xtp"); #></td>
        <td align="left">&nbsp;&nbsp;<# topnav(out, "Site&nbsp;Map", "sitemap.xtp"); #></td>
        <td align="left">&nbsp;&nbsp;<# topnav(out, "FAQ", "quercus/faq/overview.xtp"); #></td>
      </tr>
      <tr>
        <td align="left">&nbsp;&nbsp;<# topnav(out, "Download", "download/"); #></td>
        <td align="left">&nbsp;&nbsp;<# topnav(out, "Documentation", "resin-3.0/"); #></td>
        <td align="left">&nbsp;&nbsp;<# topnav(out, "Sales", "sales/"); #></td>
      </tr>
      </table>
    </td>
    </tr>
  </table>
  </td></tr>

  <tr>
  <td colspan="4">
  <!-- vspace -->
  <img alt="" width="1" height="5">
    <xsl:attribute name='src'>/images/pixel.gif</xsl:attribute>
  </img>
  </td></tr>
  <tr>
  <td colspan="2" background="/images/left_background.gif">
  <!-- vspace -->
  <img alt="" width="1" height="20">
    <xsl:attribute name='src'>/images/pixel.gif</xsl:attribute>
  </img>
  </td>
  <td colspan="2">
  <!-- vspace -->
  <img alt="" width="1" height="20">
    <xsl:attribute name='src'>/images/pixel.gif</xsl:attribute>
  </img>
  </td></tr>
  <tr valign="top">
  <td bgcolor="#b9cef7"></td>
  <td bgcolor="#b9cef7">
    <table cellspacing="0" cellpadding="2" border="0" width="100%">
    <tr><td>

    <!-- search -->
    <a href="http://www.google.com/advanced_search?q=+site:www.caucho.com&hl=en&as_qdr=all">Search</a>
    <hr/>
<!--
    Search<br/>
    <form action='/search'>
    <input name='query' size='12'/>
    </form>
-->

    <!-- Left Navigation -->
    <# writeFamilyNavigation(out, env); #>
    </td></tr>
    </table>
  </td>
  <td width="10">
    <img alt="" width="10" height="1">
      <xsl:attribute name='src'>/images/pixel.gif</xsl:attribute>
    </img>
  </td>
  <td>

  <!-- Actual Contents -->
  <xsl:apply-templates/>

  <!-- comments -->
  <# if (nav != null && ! nav.getAttribute("comment").equals("")) { #>
    <% try {
        out.flush();
        ServletContext root = application.getContext("/");
        RequestDispatcher disp;
        disp = root.getRequestDispatcher("/quercus/comment/list.xtp");
        disp.include(request, response);
       } catch (Exception e) { application.log("comment error", e); } %>
  <# } #>

  <# if (nav != null && nav.getAttribute("threaded").equals("true")) {
       out.println("<hr>");
       writeThreaded(out, true);
     }
  #>

  </td>
  </tr>
  </table>

  <!-- footer -->

  <table border="0" cellspacing="0" width="100%" bgcolor="#b9cef7" class="website">
  <tr>
  <td width="160">&nbsp;</td>
  <td>
  <center>
    <a href="/index.xtp">Home</a> |
    <a href="/resin-3.0/">Documentation</a> | 
    <a href="/download/index.xtp">Download</a> | 
    <a href="/sales/">Sales</a> | 
    <a href="/quercus/faq/overview.xtp">FAQ</a> |
    <a href="/sitemap.xtp">Site Map</a> |
    <a href="/sales/contact.xtp">Contact Caucho</a>
  </center>
    <p>
     <em><small>Copyright &copy; 1998-2005 Caucho Technology, Inc. All rights reserved.<br/>
Resin<sup><font size='-1'>&#174;</font></sup> is a registered trademark,
and Amber<sup>tm</sup> and Quercus<sup>tm</sup> are trademarks of Caucho Technology, Inc.</small></em>
    </p>
    </td>
  </tr>
  </table>
  </body>
>>

</xsl:stylesheet>

