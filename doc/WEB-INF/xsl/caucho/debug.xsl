<!--
   - catching unconverted stuff
  -->
<xsl:output resin:disable-output-escaping="true"/>

<xsl:template match="*">
  <font color="red">
    <xsl:text>&amp;lt;</xsl:text>$(name(.))<xsl:text>></xsl:text>
  </font>
      <xsl:apply-templates select='*'/>
  <font color="red">
    <xsl:text>&amp;lt;/</xsl:text>$(name(.))<xsl:text>></xsl:text>
  </font>
</xsl:template>

<xsl:template match="text()">
  <font color="red"><xsl:value-of select='.'/></font>
</xsl:template>

<xsl:template match="p/text()|a/text()|li/text()|var/text()|td/text()|th/text()|code/text()|example//text()|note/text()|def/text()|title/text()|em/text()|b/text()|dd/text()|dt/text()|blockquote/text()|results/text()|jsp:expression/text()|small/text()|sup/text()|sub/text()|caption/text()|font/text()|h3/text()">
  <xsl:value-of select='.'/>
</xsl:template>
