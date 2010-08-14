<!--
   - Formats the top level of the template.  header.xsl assumes the XTP
   - has been read in as HTML.  So it can assume the existence of the
   - html and body tags.
  -->

<xsl:stylesheet parsed-content=false>
<#@ page language=javascript #>
<#@ cache #>
<#!
  import Navigation;

  var top = "/";
  var title = null;

  function topnav() { return ""; }

  function initNavigation(document, application)
  {
    nav = Navigation.getNavigation(application);

    top = nav.toc.documentElement.attribute.top;
    if (! top)
      top = "/";
  }
#>

html
<<
  <%@ page language=javascript %>
  <# 
     initNavigation(node.ownerDocument, out.page.servletContext)
     title = node.evalString("head/title")
     if (! title)
       title = "";
  #>
  <html>
  <head>
  <title><#= title #></title>
  <link rel="STYLESHEET" href="<#= top #>css/default.css" type="text/css">
  </head>

  <xsl:apply-templates select='body'/>
  </html>
>>

html/body
<<
  <body bgcolor=white background="<#= top #>images/background.gif">

  <table cellpadding="2" cellspacing="0" border="0" width="100%" summary="">
  <tr valign="top"><td width="120">
    <table cellspacing="0" cellpadding="0" border="0" width="100%">
    <tr><td>
      <img src="<#= top #>images/caucho.gif" width="120" height="40" alt="caucho"><br>
      <!-- Left Navigation -->
      <# nav.writeFamilyNavigation(out, out.page.request.requestURI); #>
    </td></tr>
    </table>
  </td>
  <td width="30">
    <img src="<#= top #>images/pixel.gif" alt="" width="30" height="1">
  </td>
  <td width="*">
    <xsl:comment> top navigation </xsl:comment>
    <table width="100%" cellspacing="0" cellpadding="0" border="0" summary="">
    <tr class="toptitle">
      <td rowspan=2 width="90%" background="<#= top#>images/hbleed.gif">
        <font class=toptitle size="+3">
          &nbsp;<#= title #>
        </font>
        <xsl:for-each select='/html/head/title/@subtitle'>
          <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<#= node1.nodeValue #>
        </xsl:for-each>
        <xsl:for-each select='/html/head/title/@author'>
          <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<em>By <#= node1.nodeValue #></em>
        </xsl:for-each>
      </td>
      <td align="left">&nbsp;<# topnav(out, 'Home', 'index.html') #></td>
      <td align="left">&nbsp;<# topnav(out, 'Site&nbsp;Map', 'sitemap.html') #></td>
    </tr>
    <tr>
      <td align="left">&nbsp;<# topnav(out, 'Products', 'products/index.html') #></td>
      <td align="left">&nbsp;<# topnav(out, 'Download', 'download/index.html') #></td>
    </tr>
  </table>
  <!-- vspace -->
  <img src="<#= top #>pixel.gif" alt="" width="1" height="20"><br>

  <!-- Actual Contents -->
  <xsl:apply-templates/>

  <!-- footer -->
  <hr>
  <!-- <# if (nav.isThreaded()) nav.writePrevNext(out, filename); #> -->
  <center>
    <a href="<#= top #>index.html">Home</a> |
    <a href="<#= top #>products/index.html">Products</a> | 
    <a href="<#= top #>download/index.html">Download</a> | 
    <a href="<#= top #>about.html">About Caucho</a> |
    <a href="<#= top #>support/index.html">Support</a> |
    <a href="<#= top #>sitemap.html">Site Map</a>
    <br>
    <em>Copyright &copy; 1998-2000 Caucho Technology.  All rights reserved.</em>
  </center>

  <table border=0 cellspacing=0 width='100%'>
  <tr><td>Write us at <a href='mailto:info@caucho.com'>info@caucho.com</a>
    </td>
    <td align=right><img src="<#= top #>images/logo.gif" width=96 height=32></td>
  </tr>
  </table>

  </td>
  <td width="20%"></td>
  </tr>
  </table>
  </body>
>>

</xsl:stylesheet>
