<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

  <!-- handle file viewing, used in tutorials -->

  <!-- viewfile-link

       insert HTML that makes a link which causes source to be viewed

       'file' (required) is the file to view

       if 're-start' is specified then it indicates a regexp that
       on matching a source line will start a highlighted section of
       the source.

       if 're-end' is specified then it indicates the line to end the
       highlighting.  If 're-start' is given but no 're-end', then 
       only the 're-start' line is highlighted.

       if 're-marker' is specified then it indicates a regexp that
       on matching a source line will place an anchor that the web
       browser is directed to.  If not given, then re-start is used.

       WARNING:  don't put a double-quote character in your regexp
       -->
  <xsl:template match="viewfile-link">
    <xsl:call-template name="viewfile-link"/>
  </xsl:template>

  <xsl:template name="viewfile-link">
    <xsl:param name="file" select="if(@viewfile,@viewfile,@file)"/>
    <xsl:param name="re-marker" select="@re-marker"/>
    <xsl:param name="re-start" select="@re-start"/>
    <xsl:param name="re-end" select="@re-end"/>

    <!-- find the top-level documentation web-app -->
    <xsl:variable name="toplevel" select="file-dirname(file-find('href-map.xml'))"/>

    <!-- encode all param -->
    <xsl:variable name="cp" select="java:java.net.URLEncoder.encode($xtp:context_path)"/>
    <xsl:variable name="sp" select="java:java.net.URLEncoder.encode($xtp:servlet_path)"/>
    <xsl:variable name="f" select="java:java.net.URLEncoder.encode($file)"/>
    <xsl:variable name="re-m" select="java:java.net.URLEncoder.encode($re-marker)"/>
    <xsl:variable name="re-st" select="java:java.net.URLEncoder.encode($re-start)"/>
    <xsl:variable name="re-nd" select="java:java.net.URLEncoder.encode($re-end)"/>

    <code class="viewfile">
      <xsl:variable name="url"  select="concat($toplevel,'viewfile/?contextpath=',$cp,'&servletpath=',$sp,'&file=',$f,'&re-marker=',$re-m,'&re-start=',$re-st,'&re-end=',$re-nd,'#code-highlight')"/>
      <a class="viewfile" href="{$url}">
        <xsl:value-of select="$file"/>
      </a>
    </code>
  </xsl:template>

</xsl:stylesheet>
