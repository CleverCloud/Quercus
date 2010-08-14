<!--
   - Formats the top level of the template.  header.xsl assumes the XTP
   - has been read in as HTML.  So it can assume the existence of the
   - html and body tags.
  -->

<xsl:stylesheet>
<xsl:output disable-output-escaping=yes/>
<#@ page import='com.caucho.web.*'
         import='com.caucho.vfs.*'
         import='com.caucho.server.http.*'
         import='javax.servlet.*'
         import='javax.servlet.jsp.*'
         import='javax.servlet.http.*'
#>
<#@ cache #>
<#!
  String top = "/";
  String title = null;
  Navigation nav = null;

  String topnav(Object a, Object b, Object c) { return ""; }

  void initNavigation(XslWriter out)
    throws IOException, SAXException
  {
    PageContext page = (PageContext) out.getProperty("caucho.page.context");
    ServletContext app = page.getServletContext();
    HttpServletRequest req = (HttpServletRequest) page.getRequest();

    String url = ((CauchoRequest) req).getPageServletPath();
    int p = url.lastIndexOf('/');
    String base = url.substring(0, p);
    Path pwd = (Path) out.getProperty("caucho.pwd");

    ArrayList paths = new ArrayList();
    while (p >= 0) {
      String realPath = req.getRealPath(url.substring(0, p + 1));
      Path path = pwd.lookupNative(realPath);

      paths.add(path);
      if (path.lookup("toc.xml").exists())
        out.addCacheDepend(path.lookup("toc.xml"));

      p = url.lastIndexOf('/', p - 1);
    }

    nav = Navigation.createNested(paths, base);
    if (nav == null)
      nav = new Navigation();

    top = nav.getAttribute("top");
    if (top == null || top == "")
      top = "/";
  }

  void writeFamilyNavigation(XslWriter out)
    throws IOException, SAXException
  {
    PageContext page = (PageContext) out.getProperty("caucho.page.context");
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
        out.setAttribute("colspan", "2");
        out.pushElement("hr");
        out.popElement();
        out.popElement();
        out.popElement();
      }
      else {
        out.pushElement("tr");
        out.pushElement("td");

        out.pushElement("img");
        out.setAttribute("alt", "");
        out.setAttribute("width", "8");
        out.setAttribute("height", "8");
        out.setAttribute("name", "n" + i);
        out.setAttribute("src", top + "images/pixel.gif");
        out.popElement();

	out.popElement();

        out.pushElement("td");
          
        String link = child.getLink();
        if (link.startsWith("/"));
          link = link.substring(1);

        out.pushElement("a");
        out.setAttribute("class", "leftnav");
        out.setAttribute("onMouseOut", "hide()");
        out.setAttribute("onMouseover", "bullet_on('n" + i + "')");
        out.setAttribute("href", top + link);
        out.println(child.getTitle());

        out.popElement();
        out.popElement();
        out.popElement();
      }
    }
  }

  void topnav(XslWriter out, String name, String href)
    throws IOException, SAXException
  {
    out.pushElement("img");
    out.setAttribute("name", name);
    out.setAttribute("src", top + "images/pixel.gif");
    out.setAttribute("alt", "");
    out.setAttribute("width", "8");
    out.setAttribute("height", "8");
    out.popElement();

    out.println("&nbsp;");

    out.pushElement("a");
    out.setAttribute("class", "topnav");
    out.setAttribute("onMouseover", "bullet_on('" + name + "')");
    out.setAttribute("onMouseout", "hide()");
    out.setAttribute("href", top + href);
    out.println(name);
    out.popElement();
  }

  void printDescription(XslWriter out, Node node, int count)
    throws IOException, SAXException, XPathException
  {
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
<# 
  initNavigation(out);
  title = XPath.evalString("head/title", node);
  if (title == null)
    title = "";
#>
<html>
<head>
$if (head/title/@browser-title) <<
  <title>$(head/title/@browser-title)</title>
>>
$else $if (head/title/@section) <<
  <title>$(head/title/@section)<#= title #></title>
>>
$else <<
  <title><#= title #></title>
>>
<link rel="STYLESHEET" type="text/css">
  <xsl:attribute name='href'><#= top #>css/default.css</xsl:attribute>
</link>
$if (head/title/@description) <<
  <meta name="description" content="{head/title/@description}"/>
>>
$else $if (head/meta[@name="description"]) <<
>>
$else <<
  <#
     printDescription(out, XPath.find("/html/body", node), 256);
  #>
>>
<xsl:if test='head/title/@keywords'>
  <meta name="keywords" content="{head/title/@keywords}"/>
</xsl:if>
$for-each(head/meta) <<
  $copy() << $apply-templates(@*|node()); >>
>>
<script language="JavaScript" type="text/javascript">
  var lastBullet = "";

  function bullet_on(name) {
    if (document.images) {
      document[name].src = "<#= top #>images/ball8.gif";
      lastBullet = name;
    }
  }

  function hide() {
    if (document.images && lastBullet)
      document[lastBullet].src = "<#= top #>images/pixel.gif";
  }
</script>
</head>
<xsl:apply-templates select='body'/>
</html>
>>

html/body
<<
  <body bgcolor=white>
    <xsl:attribute name='background'><#= top #>images/background.gif</xsl:attribute>

  <table cellpadding="1" cellspacing="0" border="0" width="100%" summary="">
  <tr valign="top"><td width="120">
    <table cellspacing="0" cellpadding="2" border="0" width="100%">
    <tr><td colspan="2">
      <img width="120" height="40" alt="caucho">
         <xsl:attribute name='src'><#= top #>images/caucho.gif</xsl:attribute>
      </img><br/>
    </td></tr>
    <!-- Left Navigation -->
    <# writeFamilyNavigation(out); #>
    <tr><td colspan="2">
    <hr/>
    Search
    </td></tr>
    <tr><td colspan="2">
    <form action='/search'>
    <input name='query' size='12'/>
    </form>
    </td></tr>
    </table>
  </td>
  <td width="30">
    <img alt="" width="30" height="1">
      <xsl:attribute name='src'><#= top #>images/pixel.gif</xsl:attribute>
    </img>
  </td>
  <td width="*">
    <xsl:comment> top navigation </xsl:comment>
    <table width="100%" cellspacing="0" cellpadding="0" border="0" summary="">
    <tr class="toptitle">
      <td rowspan=2 width="90%">
        <xsl:attribute name='background'><#= top #>images/hbleed.gif</xsl:attribute>
        <font class=toptitle size="+3">
          &nbsp;<xsl:apply-templates select='/html/head/title/node()'/>
        </font>
        <xsl:for-each select='/html/head/title/@subtitle'>
          <br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<{.}>
        </xsl:for-each>
        <xsl:for-each select='/html/head/title/@author'>
          <br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<em>By <{.}></em>
        </xsl:for-each>
      </td>
      <td align="left">&nbsp;<# topnav(out, "Home", "index.xtp"); #></td>
      <td align="left">&nbsp;<# topnav(out, "Site&nbsp;Map", "sitemap.xtp"); #></td>
      <td align="left">&nbsp;<# topnav(out, "FAQ", "support/faq.xtp"); #></td>
    </tr>
    <tr>
      <td align="left">&nbsp;<# topnav(out, "Resin", "products/resin/index.xtp"); #></td>
      <td align="left">&nbsp;<# topnav(out, "Download", "download/index.xtp"); #></td>
      <td align="left">&nbsp;<# topnav(out, "Sales", "sales.xtp"); #></td>
    </tr>
  </table>
  <!-- vspace -->
  <img alt="" width="1" height="20">
    <xsl:attribute name='src'><#= top #>images/pixel.gif</xsl:attribute>
  </img>
  <br/>

  <!-- Actual Contents -->
  <xsl:apply-templates/>

  <!-- footer -->
  <hr/>
  <center>
    <a href="/index.xtp">Home</a> |
    <a href="/products/resin/index.xtp">Resin</a> | 
    <a href="/download/index.xtp">Download</a> | 
    <a href="/sales.xtp">Sales</a> | 
    <a href="/support/faq.xtp">FAQ</a> |
    <a href="/sitemap.xtp">Site Map</a>
    <br/>
    <em>Copyright &copy; 1998-2000 Caucho Technology.  All rights reserved.</em>
  </center>
  <!-- <# if (nav.isThreaded()) nav.writePrevNext(out, filename); #> -->
  <table border=0 cellspacing=0 width='100%'>
  <tr><td><em>Copyright &copy; 1998-2001 Caucho Technology, Inc. All rights reserved.</em><br/>
Resin<regtrade/> is a registered trademark of Caucho Technology, Inc.
    </td>
    <td align=right><img width=96 height=32>
      <xsl:attribute name='src'><#= top #>images/logo.gif</xsl:attribute>
    </img></td>
  </tr>
  </table>

  </td>
  </tr>
  </table>
  </body>
>>

</xsl:stylesheet>

