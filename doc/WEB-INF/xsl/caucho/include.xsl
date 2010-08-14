<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">
<!--
  <xsl:template match="/">
    <xsl:variable name="c">
      <xsl:apply-templates select="node()|text()" mode="include"/>
    </xsl:variable>

    <xsl:apply-templates select="$c/node()|@*"/>
  </xsl:template>
->

  <xsl:template match="*|@*" mode="include">
    <xsl:copy>
      <xsl:apply-templates select='node()|@*' mode="include"/>
    </xsl:copy>
  </xsl:template>

  <!--
     - include allows you to include another html-style file
     - @file is relative to the current document
     - the optional @xpath allows selection of nodes to include
    -->
  <xsl:template match="include[@file]" mode="include">
    <xsl:if test="doc-should-display()">
      <xsl:variable name="x">
        <xsl:text>html_document('</xsl:text>
        <xsl:value-of select="@file"/>
        <xsl:text>',.)</xsl:text>
        <xsl:value-of select="@xpath"/>
      </xsl:variable>
    
      <xsl:variable name="c" select="evaluate($x)"/>
    
      <xsl:apply-templates select="$c" mode="include"/>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
