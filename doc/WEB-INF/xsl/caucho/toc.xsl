<!-- generates a table of contents type page based on toc.xml
     author: sam@caucho.com
     -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">


       <!-- 
                
       toc.xml
       =======
       @atoc-descend on an item means descend into the linked file the
       specified amount.  This will cause the section headers in the linked
       file to become part of the toc, or if the linked file is a contents
       file the atoc for that file is used.

         'true' means descend indefinately
         'false' means do not descend (the default)
         '3' means descend 3 levels only


       toc.generate
       ============

       generate a temporary tree based on toc.xml

       results:

       The result tree looks like toc.xml but is modified in the
       following ways:

       @id is added to each <item/> if not already there.  
       It is based on the first of:
         1) @link, with everything from the last "." to the end removed and
            all '/',' ', and '.' characters becoming '-'
         2) @title, with all '/',' ', and '.' characters becoming '-'

       @link is modified to be relative to the location of the calling
       .xtp (now suitable for sending to browser).  If no @link is specified,
       the @link of the most recent parent with an @link is used with
       '#id' appended

       @reallink is set to be an absolute filesysytem path 

       @current is set to '1' if the item is the one for which
       a page is currently being displayed

       @depth is set to the depth level for this item (first item is 1)

       a <title/> child element is provided for each <item/>.  It
       comes from the toc.xml 'item/title', 'item/header/title', 'item/@title',
       or 'document(@reallink)//header[1]/title', in that order.

       a <description/> child element is provided for each <item/>
       a <brief/> child element is provided for each <item/> if available

       -->

  <xsl:template name="toc.generate">
    <xsl:param name="toc" select="'toc.xml'"/>

    <xsl:param name="linkprefix"/>
    <xsl:param name="xtp.realdir" select="fn:base-uri(.)"/>

    <xsl:variable name="toc.doc" select="html_document($toc, .)"/>

    <xsl:apply-templates select="$toc.doc" mode="toc.generate">
      <xsl:with-param name="linkprefix" select="$linkprefix"/>
      <xsl:with-param name="xtp.realdir" select="$xtp.realdir"/>
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="node()|@*" mode="toc.generate">
    <xsl:param name="xtp.realdir"/>
    <xsl:param name="linkprefix"/>

    <xsl:copy>
      <xsl:apply-templates select="node()|@*" mode="toc.generate">
        <xsl:with-param name="xtp.realdir" select="$xtp.realdir"/>
        <xsl:with-param name="linkprefix" select="$linkprefix"/>
      </xsl:apply-templates>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="navigation" mode="toc.generate">
    <xsl:param name="xtp.realdir"/>
    <xsl:param name="linkprefix"/>
    <xsl:copy> <!-- file="fn:base-uri(.)"> -->
      <xsl:attribute name="file"><xsl:value-of select="fn:base-uri(.)"/></xsl:attribute>
      <xsl:copy-of select="@*"/>

      <xsl:apply-templates mode="toc.generate">
        <xsl:with-param name="xtp.realdir" select="$xtp.realdir"/>
        <xsl:with-param name="linkprefix" select="$linkprefix"/>
      </xsl:apply-templates>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="item" mode="toc.generate">
    <xsl:param name="xtp.realdir"/>
    <xsl:param name="linkprefix"/>

    <xsl:param name="depth" select="1"/>
    <xsl:param name="parent.link"/>
    <xsl:param name="parent.reallink"/>
      
    <xsl:if test="doc-should-display()">
    <xsl:variable name="id">
      <xsl:choose>
        <xsl:when test="@id">
          <xsl:value-of select="@id"/>
        </xsl:when>
        <xsl:when test="@link">
          <xsl:value-of select="translate(file-removeext(@link),' ./','---')"/>
        </xsl:when>
        <xsl:when test="@title">
          <xsl:value-of select="translate(@title,' ./','---')"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:message terminate="yes">
            <xsl:text>toc: Could not determine id for item in </xsl:text>
            <xsl:value-of select="fn:base-uri(.)"/>
          </xsl:message>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="link" select="if(@link,if($linkprefix,file-mergepaths($linkprefix,@link),@link),if($parent.link,concat($parent.link,'#',$id),''))"/>
    <xsl:variable name="reallink" select="if(@link,fn:resolve-uri(@link),if($parent.reallink,concat($parent.reallink,'#',$id),''))"/>

    <xsl:variable name="header">
      <xsl:copy-of select="header/*"/>
      <xsl:copy-of select="*"/>
      <xsl:if test="@link">
        <xsl:copy-of select="html_document(@link)//header[1]/*"/>
      </xsl:if>
    </xsl:variable>
    <xsl:variable name="title">
      <xsl:call-template name="m_title">
        <xsl:with-param name="header" select="$header"/>
      </xsl:call-template>
    </xsl:variable>

    <xsl:if test="not($title)">
      <xsl:message terminate="yes">
        <xsl:text>Could not obtain title for item id='</xsl:text>
        <xsl:value-of select="$id"/>
        <xsl:text>' in toc '</xsl:text>
        <xsl:value-of select="fn:base-uri($toc.doc)"/>
        <xsl:text>'</xsl:text>
      </xsl:message>
    </xsl:if>

    <!-- put the <item/> out -->
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:attribute name="link">
        <xsl:value-of select="$link"/>
      </xsl:attribute>
      <xsl:attribute name="id">
        <xsl:value-of select="$id"/>
      </xsl:attribute>
      <xsl:attribute name="depth">
        <xsl:value-of select="$depth"/>
      </xsl:attribute>
      <xsl:if test="$xtp.realdir = $reallink">
        <xsl:attribute name="current">
          <xsl:value-of select="'1'"/>
        </xsl:attribute>
      </xsl:if>
      <title><xsl:value-of select="$title"/></title>
      <xsl:copy-of select="if(header/description,header/description,if(description,description,$header/description))"/>
      <xsl:copy-of select="if(header/brief,header/brief,if(brief,brief,$header/brief))"/>

      <!-- children -->
      <xsl:apply-templates mode="toc.generate">
        <xsl:with-param name="xtp.realdir" select="$xtp.realdir"/>
        <xsl:with-param name="linkprefix" select="$linkprefix"/>
        <!-- XXX:
        <xsl:with-param name="depth" select="$depth + 1"/>
        <xsl:with-param name="config" select="$config"/>
        <xsl:with-param name="parent.link" select="if(@link,@link,$parent.link)"/>
        <xsl:with-param name="parent.reallink" select="if(@link,$reallink,$parent.reallink)"/>
        -->
      </xsl:apply-templates>
    </xsl:copy>

    </xsl:if>
  </xsl:template>

</xsl:stylesheet>

