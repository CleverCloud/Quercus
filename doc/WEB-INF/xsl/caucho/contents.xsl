$output(disable-output-escaping=>true);
<!-- $param(xtp:path_info); -->

<#@ page import='com.caucho.util.*' #>
<#@ page import='javax.servlet.*' #>
<#@ page import='javax.servlet.http.*' #>
<#@ page import='javax.servlet.jsp.*' #>
<#@ page import='org.xml.sax.*' #>
<#!
/*
String href(String name)
{
  if (name == null)
    return null;

  CharBuffer cb = CharBuffer.allocate();
  for (int i = 0; i < name.length(); i++) {
    char ch = name.charAt(i);
    if (Character.isWhitespace(ch))
      cb.append("_");
    else
      cb.append(ch);
  }

  return cb.close();
}
*/

String eltHref(Node node)
{
  Element elt = (Element) node;
  String name = elt.getAttribute("name");
  if (name.equals(""))
    name = elt.getAttribute("title");

  return href(name);
}
#>

$template(name=>old_aname) <<
  <#
     String name = eltHref(node);

     if (name != null) { #>
       <a>$attribute(name) <#= name #></a>
  <# } #>
>>

resin <<
Resin
>>

resintm <<
Resin<sup>tm</sup>
>>

regtrade <<
<sup><font size='-1'>#174;</font></sup>
>>

resinscript <<
JavaScript
>>

<xsl:template match='ed'>
  <!--
  <p><div class=ed><b>Ed:</b>
    <xsl:apply-templates/>
  </div>
  -->
</xsl:template>

eg <<
<span class=example>
  <xsl:apply-templates/>
</span>
>>

eg-em <<
<span class='eg-em'>
  <xsl:apply-templates/>
</span>
>>

<xsl:template match='*' mode=pre>
<xsl:apply-templates select='.'/>
</xsl:template>

<xsl:template match='eg-em' mode=pre
><span class='eg-em'><xsl:apply-templates mode=pre/></span
></xsl:template>

warn <<
<span class=warn>
  <xsl:apply-templates/>
</span>
>>

var <<
<span class=meta><xsl:apply-templates/></span>
>>

note <<
<br/>
<b>Note: </b>
<xsl:apply-templates/>
>>

href <<
<a href='{.}'><xsl:apply-templates/></a>
>>

ct:img <#
   String name = ((Element) node).getAttribute("src");
   PageContext page = (PageContext) out.getProperty("caucho.page.context");
   ServletContext cxt = page.getServletContext();
   String realPath = cxt.getRealPath("/images/" + name);

   com.caucho.graphics.Images img = null;
   // img = com.caucho.graphics.Images.getImage(realPath);

   out.pushElement("img");
   out.setAttribute("src", top + "images/" + name);
   if (img != null) {
     out.setAttribute("width", String.valueOf(img.getWidth()));
     out.setAttribute("height", String.valueOf(img.getHeight()));
   }
   out.popElement();
#>

ct:anchor <<
<a href='#{.}'><{.}></a>
>>

callout <<
<div class=callout>
  <xsl:apply-templates/>
</div>
>>

sidebar <<
<div class=side>
<table cellspacing=0 border=0 width='100%'>
$if (@title) <<
  <tr><th class=sidetitle>$(@title)</th></tr>
>>
<tr><td class=sidebody>
  $apply-templates();
</td></tr>
</table>
</div>
>>

<!-- Examples and other boxed things -->

<xsl:template name=box>
  <xsl:if test='not(@title)'>
    <p/>
  </xsl:if>
  <center>
  <table class='egpad' cellspacing='0' width='80%'>
    <xsl:if test='@title'>
      <caption><font size="+1"><{@title}></font></caption>
    </xsl:if>
    <xsl:copy-of select='$contents'/>
  </table>
  </center>
</xsl:template>

<xsl:template match='text()' mode=pre>
<#
  String str = node.getNodeValue();

  if (node.getPreviousSibling() == null && str.charAt(0) == '\n')
    str = str.substring(1);

  CharBuffer cb = CharBuffer.allocate();
  for (int i = 0; i < str.length(); i++) {
    char ch = str.charAt(i);
    if (ch == '\n')
      cb.append("<br>");
    else if (ch == ' ')
      cb.append("&nbsp;");
    else
      cb.append(ch);
  }

  out.print(cb);
  cb.free();
  #>
</xsl:template>

<xsl:template match='example' mode=egtable>
  <tr><td class="example" bgcolor="#ffeecc" width='50%'>
    <xsl:apply-templates mode='pre'/>
  </td></tr>
</xsl:template>

<xsl:template match='results' mode=egtable>
  <tr><td class="result" bgcolor="#ffccdd">
    <xsl:apply-templates mode='pre'/>
  </td></tr>
</xsl:template>

<xsl:template match='def' mode=egtable>
  <tr><td class="def" bgcolor="#cccccc">
    <xsl:apply-templates mode='pre'/>
  </td></tr>
</xsl:template>

<xsl:template match='egtable'>
  <xsl:call-template name=box>
    <xsl:with-param name='contents'>
      <xsl:apply-templates select='example|results|def' mode=egtable/>
    </xsl:with-param>
  </xsl:call-template>
</xsl:template>

example|ct:example <<
$if (@name) <<
  <a name='{@name}'/>
>>
$call-template(box) <<
  $with-param(contents) <<
    <tr><td class="example" bgcolor="#ffeecc">
      <xsl:apply-templates mode='pre'/>
    </td></tr>
  >>
>>
>>

badexample
<<
  <xsl:call-template name=box>
    <xsl:with-param name='contents'>
    <tr><td class="badexample" color=red>
      <xsl:apply-templates mode='pre'/>
    </td></tr>
    </xsl:with-param>
  </xsl:call-template>
>>

box <<
  <xsl:call-template name=box>
    <xsl:with-param name='contents'>
    <tr><td class="{@class}">
      <xsl:apply-templates/>
    </td></tr>
    </xsl:with-param>
  </xsl:call-template>
>>

results <<
<xsl:call-template name=box>
  <xsl:with-param name='contents'>
   <tr><td class="result" bgcolor="#ffccdd">
    <xsl:apply-templates mode='pre'/>
  </td></tr>
  </xsl:with-param>
</xsl:call-template>
>>

<xsl:template match='example[example|results|def]'>
  <xsl:call-template name=box>
    <xsl:with-param name='contents'>
    <xsl:for-each select='example|results|def'>
      <tr><td>
      <xsl:if test='name(.)="results"'>
        <xsl:attribute name='class'>result</xsl:attribute>
        <xsl:attribute name='bgcolor'>#ffeecc</xsl:attribute>
      </xsl:if>
      <xsl:if test='not(name(.)="results")'>
        <xsl:attribute name='class'><{name()}></xsl:attribute>
        <xsl:attribute name='bgcolor'>#ffcccc</xsl:attribute>
      </xsl:if>
      <xsl:apply-templates mode=pre/>
      </td></tr>
    </xsl:for-each>
    </xsl:with-param>
  </xsl:call-template>
</xsl:template>

<xsl:template match='deftable'>
  <p/><center>
  <table width='80%' cellpadding=2 cellspacing=0 class=deftable border>
    <xsl:if test='@title'>
      <caption><font size="+1"><{@title}></font></caption>
    </xsl:if>
    <xsl:apply-templates/>
  </table>
  </center>
</xsl:template>

<xsl:template match='def'>
  <xsl:call-template name=box>
    <xsl:with-param name='contents'>
    <tr><td class=def bgcolor='#ffeecc'>
      <xsl:apply-templates mode='pre'/>
    </td></tr>
    </xsl:with-param>
  </xsl:call-template>
</xsl:template>

<!-- table of contents -->

toc|navigation <<
<center>
<table width='80%' class=toc><tr><td>
<ul>
<xsl:apply-templates/>
</ul>
</td></tr></table>
</center>
>>

page
<<
<li><a href='{@href}'><{@name}></a></li>
>>

page[page]
<<
<li><a href='{@href}'><{@name}></a>
<ul>
   <xsl:apply-templates select='page'/>
</ul>
</li>
>>

<!-- columns -->

columns
<<
<table cellpadding=0 width='100%'>
<tr>
  <xsl:apply-templates select='column'/>
</tr>
</table>
>>

columns[@class]
<<
<table cellpadding=0 class='{@class}' width='100%'>
<tr>
  <xsl:apply-templates select='column'/>
</tr>
</table>
>>

column <<
<td width='50%' valign=top>
  <xsl:apply-templates/>
</td>
>>

<!-- news stuff -->

newsitem <<
<p/>
<xsl:if test='@date'>
  <em><{@date}></em> -
</xsl:if>
<xsl:apply-templates/>
>>

newsitem[1] <<
<xsl:if test='@date'>
  <em><{@date}></em> -
</xsl:if>
<xsl:apply-templates/>
>>

<!-- sections -->

<#!
String getVersion(String version)
{
  if (version == null)
    return null;
  else if (version.equals("ecma"))
    return null;
  else if (version.equals("js1.2"))
    return "JavaScript 1.2";
  else if (version.equals("js1.3"))
    return "JavaScript 1.3";
  else if (version.equals("resin1.0"))
    return "Resin 1.0";
  else if (version.equals("resin1.1"))
    return "Resin 1.1";
  else if (version.equals("resin1.2"))
    return "Resin 1.2";
  else
    return version;
}
#>

<xsl:template match='sum'>
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match='teaser'>
  <p/>
  <xsl:if test='@title'>
    <h3><{@title}></h3>
    <font size=3>
      <xsl:apply-templates/>
    </font>
  </xsl:if>
</xsl:template>

border-box <<
  <table border=0 cellspacing=0 cellpadding=1 width='100%' bgcolor='silver'>
    $if(@border) <<
      $attribute(cellpadding) << $(@border) >>
    >>

    $for-each(@*[name(.)!="border" and name(.)!="cellpadding"]) <<
      $copy();
    >>

  $if(@title|title) <<
  <tr><td>
    <table border=0 cellpadding=0 cellspacing=0 width='100%'>
    <tr><td><font size="+1">&nbsp;<b>
        $if(@title) <<
          $(@title)
        >> $else <<
          $apply-templates(title/node());
        >>
    </b></font></td></tr>
    </table>
  </td></tr>
  >>
  <tr><td>
    <table border=0 cellpadding=3 cellspacing=0 width='100%'>

      $for-each(@cellpadding) << $copy(); >>

    <tr><td bgcolor="white">
      $apply-templates(node()[name(.)!="title"]);
    </td></tr>
    </table>
  </td></tr>
  </table>
>>

example-box <<
  <table border=0 cellpadding=1 cellspacing=0 width='100%'>
  <tr bgcolor="{if(@bgcolor,@bgcolor,'silver')}">
  <td>
    <table border=0 cellpadding=3 cellspacing=0 width='100%'>

    $if(@title) <<
      <tr><td><font size="+1"><b>$(@title)</b></font></td></tr>
    >>
    $else $if (title) <<
      <tr><td><font size="+1"><b>$apply-templates(title/node());</b></font></td></tr>
    >>
    <tr><td bgcolor="white">
      $apply-templates();
    </td></tr>
    </table>
  </td></tr>
  </table>
>>

section[@title or title]|s2[@title or title] <<
  <xsl:call-template name=old_aname/>
    <p/>
    <table border=0 cellpadding=5 cellspacing=0 width='100%'>
    <tr class=section>
    <td><font size="+2"><b>
    $if (@title) <<
      $(@title)
    >>
    $else <<
      $apply-templates(title/node());
    >>
    </b></font></td>
    <#
      String version = getVersion(((Element) node).getAttribute("version"));
      if (! version.equals("")) {
        out.pushElement("td");
        out.setAttribute("align", "right");
        out.println(version);
        out.popElement();
      }
    #>
    </tr>
    </table>
    <p/>
  $apply-templates(node()[name(.)!="title"]);
>>

subsection|s3 <<
  $call-template(old_aname);
  $if (@title) <<
    <h3>$(@title)</h3>
  >>
  $else $if (title) <<
    <h3>$apply-templates(title/node());</h3>
  >>

  $apply-templates(node()[name(.)!="title"]);
>>

defun <<
  $call-template(old_aname);
  $if (@name|@title) <<
  <p/>
  <table cellspacing=0 border=0 width="100%">
  <tr><td><h3 class=code>$(if(@title,@title,@name))</h3></td>
  <#
     String version = getVersion(((Element) node).getAttribute("version"));
     if (version != null)
       out.println("<td align=right><h3>" + version + "</h3></td>");
  #></tr></table>
  >>
  <div class=desc>
    <xsl:apply-templates/>
  </div>
>>
<!--
defun[$xtp:path_info and ($xtp:path_info!=concat("/", @name))] <<>>
-->

decl <<
  <table class=def><tr><td>
    $apply-templates();
  </td></tr></table>
>>

dt <<
<a name="{.}"/>
<dt><b>$apply-templates();</b></dt>
>>

<!-- objsummary -->

<#!
/*
void writeSummaryName(XslWriter out, Node node)
  throws IOException, SAXException
{
  Element elt = (Element) node;

  String name = elt.getAttribute("name");

  String title = elt.getAttribute("title");
  if (name.equals(""))
    name = title;

  if (title.equals(""))
    title = name;

  int p1 = title.indexOf('(');
  int p2 = title.lastIndexOf(')');

  String head = null;
  String tail = null;
  if (p1 < 0 || p1 < 0) {
    if (name.equals(""))
      name = elt.getAttribute("title");

    out.pushElement("a");
    out.setAttribute("href", "#" + href(name));
    out.print(title);
    out.popElement();
  }
  else if (p1 == 0) {
    out.pushElement("a");
    out.setAttribute("href", "#" + href(name));
    out.print(title.substring(p1 + 1, p2));
    out.popElement();
  }
  else {
    out.pushElement("a");
    out.setAttribute("href", "#" + href(name));
    out.print(title.substring(0, p1));
    out.popElement();
    out.print(title.substring(p1));
  }
}
*/
#>

<xsl:template match='text()|er()' mode=objsummary/>
<xsl:template match='text()|er()' mode=defunsummary/>

<xsl:template match='defun' mode=defunsummary>
  <tr><td class=code width="40%">
  <# writeSummaryName(out, node); #>
  </td><td>
  <xsl:apply-templates/>
  </td></tr>
</xsl:template>

<xsl:template match='defun[sum]' mode=defunsummary>
  <tr><td class=code width="40%">
  <# writeSummaryName(out, node); #>
  </td><td>
  <xsl:apply-templates select='sum'/>
  </td></tr>
</xsl:template>

<xsl:template match='section|s2' mode=defunsummary>
</xsl:template>

<xsl:template match='section|s2' mode=objsummary>
  <xsl:if test='.//defun'>
  <p/>
  <table border="1" cellpadding="3" cellspacing="0" width="100%">
  <tr class=section><td colspan="2">
    <font size="+2"><b><{@title}></b></font>
  </td></tr>
  <xsl:apply-templates mode=defunsummary/>
  </table>
  <xsl:apply-templates mode=objsummary/>
  </xsl:if>
</xsl:template>

<xsl:template match='objsummary'>
  <xsl:apply-templates select='../section|../s2' mode=objsummary/>
</xsl:template>

<!-- summary list -->

<xsl:template match='text()|er()' mode=summarylist/>

<xsl:template match='section|subsection|s2|s3' mode=summarylist>
  <li><a>
         <xsl:attribute name='href'>#<#= eltHref(node) #></xsl:attribute>
         <{@title}></a>
  <xsl:if test='section|subsection|s2|s3'>
    <ol>
    <xsl:apply-templates mode=summarylist/>
    </ol>
  </xsl:if></li>
</xsl:template>

<xsl:template match='summarylist'>
  <center>
  <table width="90%" class=toc border=3>
  <tr><td>
    <ol>
    <xsl:apply-templates select='..' mode=summarylist/> 
    </ol></td>
  </tr>
  </table>
  </center>
</xsl:template>

<!-- rss -->

rss <<
<xsl:apply-templates/>
>>

channel <<
<a href='{href}'><{title}></a>&nbsp;<{description}><br/>
>>
