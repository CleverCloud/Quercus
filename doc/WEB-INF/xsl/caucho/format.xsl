ct:box
<<
<p/><center>
<table>
  <xsl:for-each select='@*'>
     <xsl:attribute name='{name(.)}'><{.}></xsl:attribute>
  </xsl:for-each>
  <xsl:if test='not(@width)'>
     <xsl:attribute name='width'>100%</xsl:attribute>
  </xsl:if>
  <xsl:if test='not(@cellspacing)'>
     <xsl:attribute name='cellspacing'>0</xsl:attribute>
  </xsl:if>
  <xsl:if test='not(@border)'>
     <xsl:attribute name='border'>0</xsl:attribute>
  </xsl:if>
<tr><td><xsl:apply-templates/></td></tr>
</table>
</center>
>>

<!--
ct:example
<<
<p/>
<center>
<table width='80%' class='{if(@class,@class,"example")}' cellspacing=0 cellpadding=3 border=0>
<tr><td><code><pre>
<xsl:apply-templates/></pre></code></td></tr>
</table></center>
>>
-->
ct:results
<<
<center>
<table width='80%' class='{if(@class,@class,"result")}' cellspacing=0 cellpadding=3 border=0>
<tr><td><code><pre>
<xsl:apply-templates/></pre></code></td></tr>
</table></center>
>>

ct:def
<<
<p/>
<center>
<table width='80%' class='{if(@class,@class,"def")}' cellspacing=0 cellpadding=3 border=0>
<tr><td><code><pre>
<xsl:apply-templates/></pre></code></td></tr>
</table></center>
>>

ct:section[title]
<<
<p/>
<table width='100%' class='section' cellspacing=0 cellpadding=3 border=0>
<tr><td>
  <font size='+1'><xsl:apply-templates select='title'/></font>
</td></tr>
</table>

<xsl:apply-templates select='node()[name(.)!="title"]'/>
>>

ct:section[@title]
<<
<p/>
<table width='100%' class='section' cellspacing=0 cellpadding=3 border=0>
<tr><td>
  <font size='+1'><{@title}></font>
</td></tr>
</table>

<xsl:apply-templates/>
>>

ct:subsection[@title]
<<
<h3><{@title}></h3>
<xsl:apply-templates/>
>>

ct:deftable
<<
<p/><center>
<table width='80%' class='deftable'>
<xsl:apply-templates/>
</table>
</center>
>>

ct:var
<<
<span class=meta><xsl:apply-templates/></span>
>>
