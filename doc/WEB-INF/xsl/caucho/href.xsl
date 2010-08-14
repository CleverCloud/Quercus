<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

  <!-- intercept all special <a links and figure out where they really point to -->

  <!--
       a 'special' href is recogized by containing a '|' char

       href='root|topic|page'
       href='topic|page'
       href='topic|'            
       href='|topic|'

       See href-map.
       
       root: changes the root (if not provided, the 'root' for the current context is
       used).

       topic: attribute indicates the topic that is being linked to

       href: indicates a page (and optional anchor) within the topic
       -->
  <xsl:variable name="hrefmap" select="document(file-find(/*[1],'href-map.xml'))"/>
  <xsl:key name="roots" match="$hrefmap//root" use="@id"/>
  <xsl:key name="topics" match="$hrefmap//map" use="concat(ancestor::root/@id,'|',@topic)"/>
  <xsl:key name="topics.href" match="$hrefmap//href" use="concat(ancestor::root/@id,'|',ancestor::map/@topic,'|',@id)"/>
    
  <xsl:template match="a[contains(@href,'|')]">
    <xsl:call-template name="a.href">
      <xsl:with-param name="href" select="@href"/>
      <xsl:with-param name="text" select="text()|node()"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="a.href">
    <xsl:param name="href"/>
    <xsl:param name="text"/>

    <xsl:variable name="url">
      <xsl:call-template name="href.map">
        <xsl:with-param name="href" select="$href"/>
        <xsl:with-param name="text" select="$text"/>
      </xsl:call-template>
    </xsl:variable>
    <a class="{$url/class}" href="{$url/href}">
      <xsl:value-of select="$url/text"/>
    </a>
    <xsl:text> </xsl:text>
  </xsl:template>

  <xsl:template name="href.map">
    <xsl:param name="href"/>
    <xsl:param name="r.orighref"/>
    <xsl:param name="r.allhref"/>
    <xsl:param name="text"/>

    <!-- check for circular recursion -->
    <xsl:variable name="goingaroundincircles"
                  select="$r.allhref/href[text() = $href]"/>
    
    <!-- format of href is [root|]topic|href -->
    <xsl:variable name="in.hrefall" select="href-parse($href,3,'')"/>
    <!-- this should be changed to default to whatever the topic is for the page -->
    <xsl:variable name="in.topic" select="href-parse($href,2)"/>
    <xsl:variable name="in.root" select="href-parse($href,1,'doc')"/>

    <xsl:variable name="in.param" select="substring-after($in.hrefall,'?')"/>
    <xsl:variable name="in.noparam" select="if($in.param,substring-before($in.hrefall,'?'),$in.hrefall)"/>
    <xsl:variable name="in.anchor" select="substring-after($in.noparam,'#')"/>
    <xsl:variable name="in.href" 
                  select="if($in.anchor,substring-before($in.noparam,'#'),$in.noparam)"/>

    <!-- special tratement for 'javadoc' -->
    <xsl:variable name="javadoc" select="$in.root='javadoc'"/>
    <xsl:variable name="javadoc.package" select="if($javadoc,javadoc-package($in.topic),'')"/>
    <xsl:variable name="javadoc.class" select="if($javadoc,javadoc-class($in.topic),'')"/>
    <xsl:variable name="javadoc.method" select="$in.href"/>

    <xsl:variable name="conf">
      <!-- get all configurations that apply, most specific one first -->
      <xsl:copy-of select="key('topics.href',concat($in.root,'|',$in.topic,'|',$in.hrefall))/*"/>
      <xsl:copy-of select="key('topics.href',concat($in.root,'|',$in.topic,'|',$in.noparam))/*"/>
      <xsl:copy-of select="key('topics.href',concat($in.root,'|',$in.topic,'|',$in.href))/*"/>
      <xsl:copy-of select="key('topics',concat($in.root,'|',$in.topic))/*[not(name()='href')]"/>
      <xsl:copy-of select="key('roots',$in.root)/*[not(name()='mappings')]"/>
    </xsl:variable>

    <!-- if any reconsider is seen,  call recursively -->
    <xsl:choose>
      <xsl:when test="$conf/reconsider and not($goingaroundincircles)">
        <xsl:variable name="withroot" 
                      select="if($conf/reconsider[withroot],$conf/reconsider/withroot[1],$in.root)"/>
        <xsl:variable name="withtopic" 
                      select="if($conf/reconsider[withtopic],$conf/reconsider/withtopic[1],$in.topic)"/>
        <xsl:variable name="withhref" 
                      select="if($conf/reconsider[withhref],$conf/reconsider/withhref[1],$in.href)"/>
        <xsl:variable name="withanchor" 
                      select="if($conf/reconsider[withanchor],$conf/reconsider/withanchor[1],$in.anchor)"/>
        <xsl:call-template name="href.map">
          <xsl:with-param name="href"
                          select="concat($withroot,'|',$withtopic,'|',$withhref, if($withanchor,concat('#',$withanchor),''))"/>
          <xsl:with-param name="r.orighref" select="if($r.orighref,$r.orighref,$href)"/>
          <xsl:with-param name="r.allhref">
            <xsl:copy-of select="$r.allhref"/>
            <href><xsl:copy-of select="$href"/></href>
          </xsl:with-param>
          <xsl:with-param name="text" select="$text"/>
        </xsl:call-template>
      </xsl:when>

      <!-- if this is a lookup, find the value to use and reconsider -->
      <xsl:when test="$in.root = 'lookup'">
        <xsl:variable name="lookupmapped">
          <xsl:call-template name="href.map">
            <xsl:with-param name="href" select="concat('doc|',$in.topic,'|',$in.href"/>
          </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="lookupfile" select="file-realpath-absolute($lookupmapped/href)"/>
        <xsl:variable name="lookupdoc" select="html_document($lookupfile)"/>
        <xsl:variable name="byid" select="$lookupdoc//ix[@id=$in.anchor]"/>
        <xsl:variable name="byl" select="if($byid,$byid,$lookupdoc//ix[l=$in.anchor])"/>

        <xsl:call-template name="href.map">
          <xsl:with-param name="href"
                          select="concat('doc|',$in.topic,'|',$byl/@h)"/>
          <xsl:with-param name="r.orighref" select="if($r.orighref,$r.orighref,$href)"/>
          <xsl:with-param name="r.allhref">
            <xsl:copy-of select="$r.allhref"/>
            <href><xsl:copy-of select="$href"/></href>
          </xsl:with-param>
          <xsl:with-param name="text" select="$text"/>
        </xsl:call-template>
      </xsl:when>

      <!-- -->
      <xsl:otherwise>
        <xsl:variable name="prefix">
          <xsl:choose>
            <xsl:when test="'find:doc' = $conf/prefix[1]">
              <xsl:value-of select="file-dirname(file-find('config.xml','doc/config.xml'))"/>
            </xsl:when>
            <xsl:when test="'find:javadoc' = $conf/prefix[1]">
              <xsl:value-of select="$xtp:context_path"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$conf/prefix[1]"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
            
        <xsl:variable name="relpath">
          <xsl:choose>
            <xsl:when test="$in.root='external'">
              <xsl:value-of select="$in.topic"/>
            </xsl:when>
            <xsl:when test="$javadoc">
              <xsl:text>javadoc</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$conf/relpath[1]"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>

        <xsl:variable name="defaultext" select="$conf/defaultext[1]"/>

        <xsl:variable name="href">
          <xsl:choose>
            <xsl:when test="$javadoc">
              <xsl:text>?query=</xsl:text>
              <xsl:choose>
                <xsl:when test="$javadoc.method">
                  <xsl:text>method%20</xsl:text>
                </xsl:when>
                <xsl:when test="$javadoc.class">
                  <xsl:text>class%20</xsl:text>
                </xsl:when>
                <xsl:when test="$javadoc.package">
                  <xsl:text>package%20</xsl:text>
                </xsl:when>
              </xsl:choose>
              <xsl:value-of select="$javadoc.package"/>
              <xsl:if test="$javadoc.class">
                <xsl:text>.</xsl:text>
                <xsl:value-of select="$javadoc.class"/>
                <xsl:if test="$javadoc.method">
                  <xsl:text>.</xsl:text>
                  <xsl:value-of select="$javadoc.method"/>
                </xsl:if>
              </xsl:if>
            </xsl:when>
            <xsl:when test="$in.href">
              <xsl:value-of select="$in.href"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$conf/defaulthref[1]"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>


        <xsl:variable name="anchor">
          <xsl:choose>
            <xsl:when test="$in.root='javadoc'">
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$in.anchor"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
          
        <xsl:variable name="param">
          <xsl:choose>
            <xsl:otherwise>
              <xsl:value-of select="$in.param"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>

        <xsl:variable name="error">
          <xsl:choose>
            <xsl:when test="$goingaroundincircles">
              <xsl:text>circular</xsl:text>
              <xsl:for-each select="$r.allhref/*[not(last())]">
                <xsl:value-of select="concat('/@href=',text())"/>
              </xsl:for-each>
            </xsl:when>
            <xsl:when test="not($prefix)">
              <xsl:text>no_prefix</xsl:text>
            </xsl:when>
            <xsl:when test="not($relpath)">
              <xsl:text>no_relpath</xsl:text>
            </xsl:when>
          </xsl:choose>
        </xsl:variable>

        <xsl:variable name="url">
          <xsl:choose>
            <xsl:when test="$error">
              <xsl:value-of select="concat('error://',$error,'/@href=',$href,'/root=',$in.root,'/topic=',$in.topic,'/href=',$in.hrefall)"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:variable name="justfile" 
                            select="file-mergepaths(file-mergepaths($prefix,$relpath),$href)"/>
              <xsl:value-of select="$justfile"/>

              <!-- add .xtp if needed -->
              <xsl:if test="$href and not(contains(file-basename($justfile),'.'))">
                <xsl:value-of select="$defaultext"/>
              </xsl:if>
              
              <!-- add anchor if needed -->
              <xsl:if test="$anchor">
                <xsl:value-of select="concat('#',$anchor)"/>
              </xsl:if>
              
              <!-- add param if needed -->
              <xsl:if test="$param">
                <xsl:value-of select="concat('?',$param)"/>
              </xsl:if>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>

        <xsl:variable name="linktext">
          <xsl:choose>
            <xsl:when test="$text">
              <xsl:value-of select="$text"/>
            </xsl:when>
            <xsl:when test="$javadoc">
              <xsl:choose>
                <xsl:when test="$javadoc.method">
                  <xsl:text>method </xsl:text>
                  <xsl:value-of select="$javadoc.class"/>
                  <xsl:text>.</xsl:text>
                  <xsl:value-of select="$javadoc.method"/>
                </xsl:when>
                <xsl:when test="$javadoc.class">
                  <xsl:text>class </xsl:text>
                  <xsl:value-of select="$javadoc.package"/>
                  <xsl:text>.</xsl:text>
                  <xsl:value-of select="$javadoc.class"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:text>package </xsl:text>
                  <xsl:value-of select="$javadoc.package"/>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$url"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>

        <class>
          <xsl:choose>
            <xsl:when test="$error">
              <xsl:text>error</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$in.root"/>
            </xsl:otherwise>
          </xsl:choose>
        </class>
        <href>
          <xsl:value-of select="$url"/>
        </href>
        <text>
          <xsl:copy-of select="$linktext"/>
        </text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!--
     - convenience <a ... > tags
    -->
  <xsl:template match="a[@config-tag]">
    <xsl:call-template name="a.href">
      <xsl:with-param name="href" 
                      select="concat('lookup|config|index-tags.xtp#',@config-tag)"/>
      <xsl:with-param name="text" 
                      select="if(node()|text(),node()|text(),concat('&amp;lt;',string(@config-tag),'&amp;gt;')"/>
      <xsl:with-param name="class" 
                      select="'config-tag'"/>
    </xsl:call-template>
  </xsl:template>

</xsl:stylesheet>
