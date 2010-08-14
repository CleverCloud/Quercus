<!--
   - Formats the top level of the template.  header.xsl assumes the XTP
   - has been read in as HTML.  So it can assume the existence of the
   - html and body tags.
  -->

<xsl:stylesheet>
<xsl:output resin:disable-output-escaping=yes/>
<#@ page import='com.caucho.web.* com.caucho.vfs.* com.caucho.server.http.*' #>
<#@ page import='javax.servlet.jsp.*' #>
<#@ page import='javax.servlet.*' #>
<#@ page import='javax.servlet.http.*' #>
<#@ cache #>
<#!
  String top = "/";
  String title = null;
  Navigation nav = null;

  String topnav(Object a, Object b, Object c) { return ""; }

  void initNavigation(XslWriter out)
    throws IOException
  {
    PageContext page = (PageContext) out.getProperty("caucho.page.context");
    ServletContext app = page.getServletContext();
    HttpServletRequest req = (HttpServletRequest) page.getRequest();

    String url = req.getRequestURI();
    int p = url.lastIndexOf('/');
    String base = url.substring(0, p);

    ArrayList paths = new ArrayList();
    while (p >= 0) {
      String prefix = url.substring(0, p + 1);

      CauchoApplication subapp = (CauchoApplication) app.getContext(prefix);
      String rel = prefix.substring(subapp.getContextPath().length());

      String realPath = subapp.getRealPath(rel);
      Path path = ((Path) out.getProperty("caucho.pwd")).lookupNative(realPath);

      paths.add(path);
      if (path.lookup("toc.xml").exists())
        out.addCacheDepend(path.lookup("toc.xml"));

      p = url.lastIndexOf('/', p - 1);
    }

    nav = Navigation.createNested(paths, base);

    if (nav == null)
      nav = new Navigation();

    top = req.getContextPath();
    if (! top.endsWith("/"))
      top = top + "/";
  }

  void writeThreaded(XslWriter out, boolean allowComments)
    throws IOException, SAXException
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
    throws IOException, SAXException
  {
    out.pushElement("img");
    out.setAttribute("name", name);
    out.setAttribute("src", "/images/pixel.gif");
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
<title>$(head/title/@section)<#= title #></title>
<link rel="STYLESHEET" type="text/css">
  <xsl:attribute name='href'>/css/default.css</xsl:attribute>
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
</head>
<xsl:apply-templates select='body'/>
</html>
>>
html/body
<<
  <body bgcolor=white>
    <xsl:attribute name='background'>/images/background.gif</xsl:attribute>

  <table cellpadding="1" cellspacing="0" border="0" width="100%" summary="">
  <tr valign="top"><td width="120">
    <table cellspacing="0" cellpadding="2" border="0" width="120">
    <tr><td colspan="2">
      <img width="120" height="40" alt="caucho">
         <xsl:attribute name='src'>/images/caucho.gif</xsl:attribute>
      </img><br/>
    </td></tr>
    <!-- Left Navigation -->
    <# writeFamilyNavigation(out); #>
    </table>
  </td>
  <td width="20">
    <img alt="" width="20" height="1">
      <xsl:attribute name='src'>/images/pixel.gif</xsl:attribute>
    </img>
  </td>
  <td width="100%">
    <xsl:comment> top navigation </xsl:comment>
    <table width="100%" cellspacing="0" cellpadding="0" border="0" summary="">
    <tr class="toptitle">
      <td rowspan=2 width="90%">
        <xsl:attribute name='background'>/images/hbleed.gif</xsl:attribute>
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
    </tr>
  </table>
  <!-- vspace -->
  <img alt="" width="1" height="20">
    <xsl:attribute name='src'>/images/pixel.gif</xsl:attribute>
  </img>
  <br/>

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

  <center>
    <em>Copyright &copy; 1998-2005 Caucho Technology.  All rights reserved.</em>
  </center>
  <!-- <# if (nav.isThreaded()) nav.writePrevNext(out, filename); #> -->
  <table border=0 cellspacing=0 width='100%'>
  <tr><td><em><small>Copyright &copy; 1998-2005 Caucho Technology, Inc. All rights reserved.<br/>
Resin<sup><font size='-1'>&#174;</font></sup> is a registered trademark,
and HardCore<sup>tm</sup> and Quercus<sup>tm</sup> are trademarks of Caucho Technology, Inc.</small></em>
    </td>
    <td align=right><img width=96 height=32>
      <xsl:attribute name='src'>/images/logo.gif</xsl:attribute>
    </img></td>
  </tr>
  </table>

  </td>
  </tr>
  </table>
  </body>
>>

</xsl:stylesheet>

