<xsl:output method="html" disable-output-escaping="true" resin:disable-output-escaping="yes"/>

<xsl:param name="xtp:context_path"/>
<xsl:param name="xtp:servlet_path"/>

  <xtp:directive.page import='org.xml.sax.*'/>
  <xtp:directive.page import='org.w3c.dom.*'/>
  <xtp:directive.page import='java.io.*'/>
  <xtp:directive.page import='com.caucho.vfs.*'/>
  <xtp:directive.page import='javax.xml.transform.*'/>
  <xtp:directive.page import='javax.xml.parsers.*'/>
  <xtp:directive.page import='java.util.*'/>
  <xtp:directive.page import='java.net.*'/>
  <xtp:directive.page import='com.caucho.server.webapp.*'/>
  <xtp:directive.page import='com.caucho.server.connectionAbstract.*'/>

<xsl:include href='caucho/defaultcopy.xsl'/>
<xsl:include href='caucho/nodeinfo.xsl'/>
<xsl:include href='caucho/newstyle.xsl'/>
<xsl:include href='caucho/fun.xsl'/>
<xsl:include href='caucho/toc.xsl'/>
<xsl:include href='caucho/href.xsl'/>
<!--
<xsl:include href='caucho/contents.xsl'/>
-->

<xsl:template name="viewfile-jspcode"/>
<xsl:template name="viewfile-link"/>

<!--

-->
<xsl:include href='caucho_header.xsl'/>

item <<
<p/><a href='{url}'><{title}></a><br/>
<{description}>
>>
