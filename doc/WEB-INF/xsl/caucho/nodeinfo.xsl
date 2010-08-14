<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

<!--
   - named templates for getting information about nodes
  -->

<!-- anchor name handling -->
<xsl:template name="an">
  <xsl:variable name="an">
    <xsl:choose>
      <xsl:when test="@name">
        <xsl:value-of select="@name"/>
      </xsl:when>
      <xsl:when test="@title">
        <xsl:value-of select="@title"/>
      </xsl:when>
      <xsl:when test="header/@id">
        <xsl:value-of select="header/@id"/>
      </xsl:when>
      <xsl:when test="header/title">
        <xsl:value-of select="header/title"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="generate-id()"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:value-of select="translate($an,' ', '-')"/>
</xsl:template>

<xsl:template name="aname">
  <a>
    <xsl:attribute name="name">
      <xsl:call-template name="an"/>
    </xsl:attribute>
  </a>
</xsl:template>

<!-- title handling 
   -
   - param current should be a document, section, or s1 .. s6, or header element
   - param link: if given, is a file to get the header information
   - from if 'current' does not supply it.

  -->
<xsl:template name="m_title">
  <xsl:param name="current" select="."/>
  <xsl:param name="link"/>
  <xsl:param name="header">
    <xsl:copy-of select="if($current/header,$current/header/*,$current/*)"/>
    <xsl:if test="$link">
      <xsl:copy-of select="html_document($link)//header/*"/>
    </xsl:if>
  </xsl:param>

  <xsl:choose>
    <xsl:when test="$header/title">
      <xsl:apply-templates select="$header/title[1]/node()"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="if($current/title,$current/title[1],if($header[title],$header/title[1],$current/@title))"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- description handling 
   -
   - param type: is one of
   -        brief
   -        paragraph   - just one paragraph (brief if no paragraph available)
   -        full        - full description, may be multiple paragraphs
   -
   - param current: should be a document, section, s1 .. s6, or header element
   - param link: if given, is a file to get the header information
   - from if 'current' does not supply it.
   -
   - returns: returns nothing or one or more <p></p> elements
  -->
<xsl:template name="m_description">
  <xsl:param name="type" select="'full'"/>
  <xsl:param name="textonly"/>
  <xsl:param name="current" select="."/>
  <xsl:param name="link"/>

  <xsl:variable name="header">
    <xsl:copy-of select="if($current/header,$current/header/*,$current/*)"/>
    <xsl:if test="$link">
      <xsl:copy-of select="html_document($link)//header/*"/>
    </xsl:if>
  </xsl:variable>

  <xsl:variable name="descr" select="if($header/description,$header/description[1],p[1]"/>

  <xsl:variable name="full" select="$descr/node()|$descr/text()"/>
  <xsl:variable name="paragraph" select="if($descr/p,$descr/p[1]/node()|$descr/p[1]/text(),$descr/node()|$descr/text()"/>
  <xsl:variable name="pcollapsed">
    <xsl:apply-templates select="$paragraph" mode="textify"/>
  </xsl:variable>
  <xsl:variable name="brief" select="if($header/brief,$header/brief/node()|$header/brief/text(),if(substring-before($pcollapsed,'. '),concat(substring-before($pcollapsed,'. '),'.'),$pcollapsed))"/>

  <xsl:choose>
    <xsl:when test="$type='none'">
    </xsl:when>
    <xsl:when test="$type='no'">
    </xsl:when>
    <xsl:when test="$type='brief'">
      <xsl:copy-of select="$brief"/>
    </xsl:when>
    <xsl:when test="$type='paragraph'">
      <p>
        <xsl:copy-of select="if($paragraph,$paragraph,$brief)"/>
      </p>
    </xsl:when>
    <xsl:when test="$type='full'">
      <xsl:choose>
        <xsl:when test="$full">
          <xsl:copy-of select="$full"/>
        </xsl:when>
        <xsl:otherwise>
          <p>
            <xsl:copy-of select="if($paragraph,$paragraph,$brief)"/>
          </p>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$brief"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="text()" mode="textify">
  <xsl:value-of select="."/>
</xsl:template>

<xsl:template match="*" mode="textify">
  <xsl:text> </xsl:text>
  <xsl:apply-templates mode="textify()"/>
  <xsl:text> </xsl:text>
</xsl:template>

</xsl:stylesheet>
