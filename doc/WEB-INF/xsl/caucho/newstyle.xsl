<?xml version="1.0"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:output resin:disable-output-escaping="yes"/>

<!-- 
   - <body> 
  -->
<xsl:template match='body'>
  <xsl:apply-templates>
    <xsl:with-param name="section.depth" select="$section.depth"/>
  </xsl:apply-templates>
</xsl:template>

<!-- 
   - <summary> determines an appropriate summary for the section
   -
   - @description='none|brief|paragraph|full' type of description to show, default is 'full'
   - @objsummary     if present, change default displaying of objsummary
   -                 'none' - don't display
   -                 'localtoc' - display as items in local toc, as if each 
   -                              defun was a section
   -                 'string' - use string as the title of the objsummary
   -
   - @atoc        If  'none', suppress annotated table of contents 
   -              If 'toplevel' page get's a section for each <item/> and 
   -              a paragraph description.
   -              Otherwise, display using a <DL><DT><DD></DL> style list,
   -              and use the value of @atoc as the class attribute for the elements
   -              'yes' results in 'atoc' for the class name
   -              default is 'yes' if the type of the document is 'contents', 
   -              'none' otherwise
   -     child element: atoc-control
   -       control the output of an atoc
   -       <atoc-control>
   -         <item depth="1" description="paragraph"/>
   -         <item depth="2" description="brief"/>
   -         <item depth="3" description="none"/>
   -       </atoc-control>
   -
   -       Prune can be used to prune selected files:
   -       <atoc-control>
   -         <item depth="1" description="paragraph"/>
   -         <item depth="2" description="brief"/>
   -         <item depth="3" description="none">
   -           <prune>faq.xtp</prune>
   -           <prune>scrapbook.xtp</prune>
   -           <prune>tutorial.xtp</prune>
   -         </item>
   -       </atoc-control>
   -        the pruning occurs if the full link path to the item in question
   -        _contains_ one of the values in <prune></prune>
   -
   - @localtoc    if 'none', suppress local table of contents
   -              otherwise a number that indicates the maximum depth to go to.
   -              default (which is the same as 'yes' is to display to level '4' 
   -              if this is the outermost section, 'none' otherwise,
   -              and only if there are more than 2 sections in the page.
  -->

<xsl:template match="summary">
  <xsl:param name="section.depth"/>
  <!-- this template goes through great hoops to make it so that in
       most cases you just have to put a <summary/> and it will do what you
       want.  Unfortunately, it makes for some ugly code ... 
       -->
  <xsl:variable name="parent" select="ancestor::*[doc-is-section()]"/> <!-- TODO, should match first parent section etc. -->
  <xsl:variable name="header" select="$parent[1]/header"/>

  <!-- defaults may depend on section.depth -->
  <xsl:variable name="description" select="if(@description, @description, 'full')"/>
  <xsl:variable name="objsummary" select="if(@objsummary, @objsummary, if($section.depth &lt; 2, 'Index','none'))"/>
  <xsl:variable name="atoc" select="if(@atoc, @atoc, if($section.depth &lt; 2 and $header/type[text() = 'contents'], 'yes','none'))"/>
  <xsl:variable name="localtoc" select="if(@localtoc, @localtoc, if($section.depth &lt; 2, 'yes','none'))"/>

  <!-- description -->
  <xsl:if test="not($description = 'none')">
    <xsl:variable name="trapdescription">
      <xsl:call-template name="m_description">
        <xsl:with-param name="current" select="$parent[1]"/>
        <xsl:with-param name="type" select="if($description = 'yes' or not($description),'full', $description)"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:apply-templates select="$trapdescription"/>
  </xsl:if>

  <!-- atoc -->
  <xsl:if test="not($atoc = 'none')">
    <!-- atoc: a annotated toc based on toc.xml -->
    <xsl:call-template name="atoc">
      <xsl:with-param name="header" select="$header"/>
      <xsl:with-param name="type" select="$atoc"/>
      <xsl:with-param name="section.depth" select="$section.depth"/>
      <xsl:with-param name="atoc.control" select="atoc-control"/>
    </xsl:call-template>
  </xsl:if>

  <!-- objsummary is supplied if there are any defun -->
  <xsl:variable name="objsummary.body">
    <!-- trap so it is included in table of contents -->
    <xsl:if test="not($objsummary = 'none') and not($objsummary = 'localtoc') and following::defun[1]">
      <xsl:call-template name="objsummary">
        <xsl:with-param name="title" select="$objsummary"/>
        <xsl:with-param name="section.depth" select="$section.depth"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:variable>

  <xsl:variable name="localsummarydoc">
    <xsl:copy-of select="$objsummary.body"/>
    <xsl:copy-of select="../section | ../document | ../faq"/>
  </xsl:variable>

  <!-- localtoc -->
  <xsl:variable name="localtoc.maxdepth"
                select="if($localtoc = 'yes', 
                          if((count($localsummarydoc//section | $localsummarydoc//s2|$localsummarydoc//faq) + if($objsummary = 'localtoc', count($localsummarydoc//defun), 0)) &gt; 1, 4, 0), if($localtoc = 'none',0,$localtoc)))"/>
  <xsl:if test="not($localtoc.maxdepth = 0)">
    <xsl:call-template name="localtoc">
      <xsl:with-param name="parent" select="$localsummarydoc"/>
      <xsl:with-param name="section.depth" select="$section.depth"/>
      <xsl:with-param name="maxdepth" select="$localtoc.maxdepth"/>
      <xsl:with-param name="defun" select="if($objsummary = 'localtoc','yes','no')"/>
    </xsl:call-template>
  </xsl:if>

  <!-- objsummary goes out -->
  <xsl:apply-templates select="$objsummary.body">
    <xsl:with-param name="section.depth" select="$section.depth"/>
  </xsl:apply-templates>

</xsl:template>

<!-- DEBUG -->
<xsl:template match="@*" mode="debug">
  <xsl:value-of select="concat(' ',name(.),'=',.)"/>
</xsl:template>

<xsl:template match="*" mode="debug">
  <xsl:text>&lt;</xsl:text><xsl:value-of select="name(.)"/>
    <xsl:apply-templates select="@*" mode="debug"/>
  <xsl:text>&gt;</xsl:text>
    <xsl:apply-templates select="node()" mode="debug"/>

  <xsl:text>&lt;</xsl:text><xsl:value-of select="name(.)"/><xsl:text>&gt;</xsl:text>
</xsl:template>
   
<!--
   - <atoc> makes an annotated toc based on toc.xml
  -->
<xsl:template name="atoc">
  <xsl:param name="toc" select="if(@toc,@toc,'toc.xml')"/>
  <xsl:param name="type" select="'yes'"/>
  <xsl:param name="header" select="if(ancestor::*/header,ancestor::*/header,//header)"/>
  <xsl:param name="atoc.control"/>
  <xsl:param name="section.depth"/>

  <xsl:variable name="atoc.control.defaults">
    <atoc-control>
      <item depth="1" description="paragraph"/>
      <item depth="2" description="brief"/>
      <item depth="3" description="none"/>
    </atoc-control>
  </xsl:variable>

  <xsl:variable name="atoctype" select="if($type = 'yes', 'atoc',$type)"/>

  <xsl:variable name="toc">
    <xsl:call-template name="toc.generate">
      <xsl:with-param name="toc" select="if(@toc,@toc,'toc.xml')"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="out">

    <!-- start with all children of 'current' item -->
    <xsl:variable name="curr" select="$toc//item[@current]/item"/>
    <xsl:variable name="type" select="if($atoctype = 'atoc',$atoctype,concat('atoc-',$atoctype))"/>
    <xsl:if test="$curr">
      <dl class="{$type} {$type}-top">
        <xsl:apply-templates select="$curr" mode="atoc.fmt">
          <xsl:with-param name="type" select="$type"/>
          <xsl:with-param name="item.depthremaining" select="-1"/>
          <xsl:with-param name="atoc.control" select="if($atoc.control,$atoc.control,$atoc.control.defaults)"/>
        </xsl:apply-templates>
      </dl>
    </xsl:if>
  </xsl:variable>

  <!-- transform again to get formatting done -->
  <xsl:apply-templates select="$out">
    <xsl:with-param name="section.depth" select="$section.depth"/>
  </xsl:apply-templates>

</xsl:template>

<xsl:template match="*" mode="atoc.fmt">
  <xsl:param name="type"/>
  <xsl:param name="item.depth"/>
  <xsl:param name="item.depthremaining"/>
  <xsl:param name="atoc.control"/>
  <xsl:param name="link.prefix"/>
  <xsl:apply-templates mode="atoc.fmt">
    <xsl:with-param name="type" select="$type"/>
    <xsl:with-param name="item.depth" select="$item.depth"/>
    <xsl:with-param name="item.depthremaining" select="$item.depthremaining"/>
    <xsl:with-param name="atoc.control" select="$atoc.control"/>
    <xsl:with-param name="link.prefix" select="$link.prefix"/>
    <xsl:with-param name="link.file" select="$link.file"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="item | section | faq" mode="atoc.fmt">
  <xsl:param name="type"/>
  <xsl:param name="item.depth" select="1"/>
  <xsl:param name="item.depthremaining"/>
  <xsl:param name="atoc.control"/>
  <xsl:param name="link.prefix"/>
  <xsl:param name="link.file"/>

  <xsl:variable name="atoc-descend" select="if(@atoc-descend = 'true' or @atoc-descend = 'yes',9999,if(@atoc-descend = 'false' or @atoc-descend = 'no',0,@atoc-descend))"/>
  <xsl:variable name="link">
    <xsl:choose>
      <xsl:when test="name() = 'item'">
        <xsl:if test="@link">
          <xsl:value-of select="if($link.prefix,file-mergepaths($link.prefix,@link),@link)"/>
        </xsl:if>
      </xsl:when>
      <xsl:otherwise>
        <!-- descending into a document and have matched `section' or `faq' etc. -->
        <xsl:value-of select="if($link.prefix,file-mergepaths($link.prefix,$link.file),$link.file)"/>
        <xsl:text>#</xsl:text>
        <xsl:call-template name="an"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="control.item" select="$atoc.control//item[@depth=$item.depth]"/>

  <xsl:variable name="prune">
    <xsl:choose>
      <xsl:when test="$control.item">
        <!-- check child prune elements that indicate pruning by filename -->
        <xsl:for-each select="$control.item/prune">
          <!-- does $link contain the value? -->
          <xsl:if test="contains(string($link),string(.))">true</xsl:if>
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>true</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:if test="not($prune)">
    <dt class="{$type} {$type}-{$item.depth}">
      <b>
        <a name="{@id}">
          <xsl:if test="$link">
            <xsl:attribute name="href">
              <xsl:value-of select="$link"/>
            </xsl:attribute>
          </xsl:if>

          <xsl:call-template name="m_title"/>
        </a>
      </b>
    </dt>
    <dd class="{$type} {$type}-{$item.depth}">
      <xsl:variable name="descr">
        <xsl:call-template name="m_description">
          <xsl:with-param name="type" select="string($control.item/@description)"/>
        </xsl:call-template>
      </xsl:variable>

      <xsl:if test="$descr">
        <p><xsl:copy-of select="$descr"/></p>
      </xsl:if>


      <xsl:if test="(item or $atoc-descend) and ($item.depthremaining != 0)">
        <dl class="{$type} {$type}-{$item.depth}">
          <xsl:if test="$atoc-descend">
            <xsl:variable name="link.basename" select="file-basename(@link)"/>
            <xsl:variable name="link.origfile" select="file-realpath(file-mergepaths(file-mergepaths(file-dirname(request-servletpath()),$link.prefix),@link)"/>
            <xsl:variable name="descend.usetoc" select="html_document($link.origfile)//type[1] = 'contents'"/>
            <xsl:variable name="descend.file" 
              select="if($descend.usetoc, file-mergepaths(file-dirname(@link),'toc.xml'), @link)"/>
            <xsl:variable name="descend.realfile" select="file-realpath(file-mergepaths(file-mergepaths(file-dirname(request-servletpath()),$link.prefix),$descend.file)"/>
            <xsl:variable name="descend.doc" select="html_document($descend.realfile)"/>
            <xsl:variable name="link.prefix" select="if($link.prefix,file-mergepaths($link.prefix,file-dirname(@link)),file-dirname(@link))"/>
            <xsl:variable name="link.file" select="file-basename($descend.file)"/>

            <xsl:choose>
              <xsl:when test="$descend.usetoc">
                <xsl:variable name="items">
                  <xsl:call-template name="toc.generate">
                  </xsl:call-template>
                </xsl:variable>
                <xsl:apply-templates select="$items//item[@link=$link.basename]/*" mode="atoc.fmt">
                  <xsl:with-param name="type" select="$type"/>
                  <xsl:with-param name="item.depth" select="$item.depth + 1"/>
                  <xsl:with-param name="item.depthremaining" select="$atoc-descend - 1"/>
                  <xsl:with-param name="atoc.control" select="$atoc.control"/>
                  <xsl:with-param name="link.prefix" select="$link.prefix"/>
                  <xsl:with-param name="link.file" select="$link.file"/>
                </xsl:apply-templates>
              </xsl:when>
              <xsl:otherwise>
                <xsl:apply-templates select="$descend.doc/*" mode="atoc.fmt">
                  <xsl:with-param name="type" select="$type"/>
                  <xsl:with-param name="item.depth" select="$item.depth + 1"/>
                  <xsl:with-param name="item.depthremaining" select="$atoc-descend - 1"/>
                  <xsl:with-param name="atoc.control" select="$atoc.control"/>
                  <xsl:with-param name="link.prefix" select="$link.prefix"/>
                  <xsl:with-param name="link.file" select="$link.file"/>
                </xsl:apply-templates>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:if>
          <xsl:apply-templates mode="atoc.fmt">
            <xsl:with-param name="type" select="$type"/>
            <xsl:with-param name="item.depth" select="$item.depth + 1"/>
            <xsl:with-param name="item.depthremaining" select="$item.depthremaining - 1"/>
            <xsl:with-param name="atoc.control" select="$atoc.control"/>
            <xsl:with-param name="link.prefix" select="$link.prefix"/>
            <xsl:with-param name="link.file" select="$link.file"/>
          </xsl:apply-templates>
        </dl>
      </xsl:if>
    </dd>
  </xsl:if>
</xsl:template>

<xsl:template match="atoc">
  <xsl:param name="type"/>
  <xsl:param name="section.depth"/>
  <xsl:param name="atoc-descriptions"/>
  <xsl:param name="atoc-descend"/>
  <xsl:call-template name="atoc">
    <xsl:with-param name="type" select="$type"/>
    <xsl:with-param name="section.depth" select="$section.depth"/>
    <xsl:with-param name="atoc-descend" select="$atoc-descend"/>
    <xsl:with-param name="atoc-descriptions" select="$atoc-descriptions"/>
  </xsl:call-template>
</xsl:template>

<!--
   - <localtoc> makes a table of contents for this section
   - @defun if 'yes', include defun in localtoc (default no)
   - @maxdepth the maximum depth, defaults to 1
  -->

<xsl:template match="localtoc">
  <xsl:call-template name="localtoc">
    <xsl:with-param name="parent" select=".."/>
  </xsl:call-template>
</xsl:template>

<xsl:template match='text()' mode='localtoc'/>

<xsl:template match='section|faq|defun|s2|s3' mode='localtoc'>
  <xsl:param name="depth" select="1"/>
  <xsl:param name="maxdepth" select="1"/>
  <xsl:param name="defun"/>
  <xsl:if test="if(name() = 'defun', $defun = 'yes' and doc-should-display(), doc-should-display())">
  <li>
    <xsl:variable name="an">
      <xsl:call-template name="an"/>
    </xsl:variable>
    <xsl:variable name="title">
      <xsl:call-template name="m_title"/>
    </xsl:variable>
    <xsl:if test="@localtoc-indent">
      <xsl:call-template name="string-repeat">
        <xsl:with-param name="string" select="'&#160;'"/> <!-- &nbsp; -->
        <xsl:with-param name="repeat" select="@localtoc-indent * 4"/>
      </xsl:call-template>
    </xsl:if>
    <a href="#{$an}"><xsl:value-of select="$title"/></a>

    <xsl:if test="$depth != $maxdepth and (section|defun|faq|s2|s3 or ($defun = 'yes' and defun))">
      <ol>
        <xsl:apply-templates mode='localtoc'>
          <xsl:with-param name="depth" select="$depth + 1"/>
          <xsl:with-param name="maxdepth" select="$maxdepth"/>
          <xsl:with-param name="defun" select="$defun"/>
        </xsl:apply-templates>
      </ol>
    </xsl:if>
  </li>
  </xsl:if>
</xsl:template>

<xsl:template name='localtoc'>
  <xsl:param name="parent" select=".."/>
  <xsl:param name="maxdepth" select="if(@maxdepth,@maxdepth,1)"/>
  <xsl:param name="defun" select="if(@defun,@defun,'no')"/>

  <p/>
  <center>
  <table width="90%" class='toc' border='3'>
  <tr><td>
    <ol>
      <xsl:apply-templates select='$parent/*' mode='localtoc'> 
        <xsl:with-param name="defun" select="$defun"/>
        <xsl:with-param name="maxdepth" select="$maxdepth"/>
      </xsl:apply-templates>
    </ol></td>
  </tr>
  </table>
  </center>
  <p/>
</xsl:template>

<!--
   - Sections
  -->

<!--
   - section start's a new section.  You can have nested sections, their
   - titles are formatted appropriately.  
   -
   - @title the title for the section, if not specified then header/title is used
   - @href a url to link the section heading to
   -
   - @depth internal use, override the $section.depth param
   -
   - note: <document/> is essentially just a outermost <section/>
  -->

<xsl:template match="section|document">
  <xsl:param name="section.depth" select="if(name()='section',1,0)"/>

  <xsl:if test="doc-should-display()">

  <xsl:variable name="depth" select="if(@depth,@depth,$section.depth + 1)"/>
  <xsl:variable name="parent" select="ancestor-or-self::*[doc-is-section()]"/>
  <xsl:variable name="header" select="$parent[1]/header"/>

  <xsl:variable name="tutorial" 
    select="$header/type = 'tutorial'"/>
  <xsl:variable name="tutorial-startpage" 
    select="$header/tutorial-startpage"/>

  <xsl:variable name="an">
    <xsl:call-template name="an"/>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="$depth = 1">
      <!-- title display handled in new_doc_header.xsl -->

      <!-- display extra header for tutorials -->
      <xsl:if test="$tutorial">
        <xsl:if test="installed('caucho-dev') or not(installed('caucho-website'))">
          <xsl:text>Find this tutorial in: </xsl:text>
          <code>
            <#
              PageContext page = (PageContext) out.getProperty("caucho.page.context");
              HttpServletRequest request = (HttpServletRequest) page.getRequest();
              Path tutpath = Vfs.lookupNative(request.getRealPath(request.getServletPath())).getParent();
              out.print(tutpath.toString());
             #>
         </code>
       </xsl:if>
       <xsl:if test="$tutorial-startpage">
         <br/>
         <a href="{$tutorial-startpage}">Try the Tutorial</a>
       </xsl:if>
       <hr/>
      </xsl:if>
    </xsl:when>
    <xsl:when test="$depth = 2">
      <p/>
      <table border=0 cellpadding=5 cellspacing=0 width='100%'>
        <tr class='section'>
          <td>
            <font size="+2">
              <b>
                <a name="{$an}">
                  <xsl:if test="@href">
                    <xsl:attribute name="href">
                      <xsl:value-of select="@href"/>
                    </xsl:attribute>
                  </xsl:if>
                  <xsl:call-template name="m_title"/>
                </a>
              </b>
            </font>
          </td>
          
          <xsl:choose>
            <xsl:when test="@version or header/product  = 'resin-ee' or @product = 'resin-ee'">
              <td align="right">
                <xsl:if test="header/product = 'resin-ee' or @product = 'resin-ee'">
                  <xsl:text>Resin-EE </xsl:text>
                </xsl:if>
                <xsl:value-of select="@version"/>
              </td>
            </xsl:when>
            <xsl:otherwise>
            </xsl:otherwise>
          </xsl:choose>
        </tr>
      </table>
    </xsl:when>
    <xsl:when test="$depth = 3">
      <h3>
        <a name="{$an}">
          <xsl:if test="@href">
            <xsl:attribute name="href">
              <xsl:value-of select="@href"/>
            </xsl:attribute>
          </xsl:if>
          <xsl:call-template name="m_title"/>
        </a>
      </h3>
    </xsl:when>
    <xsl:when test="$depth = 4">
      <h4>
        <a name="{$an}">
          <xsl:if test="@href">
            <xsl:attribute name="href">
              <xsl:value-of select="@href"/>
            </xsl:attribute>
          </xsl:if>
          <xsl:call-template name="m_title"/>
        </a>
      </h4>
    </xsl:when>
    <xsl:otherwise>
      <h5>
        <a name="{$an}">
          <xsl:if test="@href">
            <xsl:attribute name="href">
              <xsl:value-of select="@href"/>
            </xsl:attribute>
          </xsl:if>
          <xsl:call-template name="m_title"/>
        </a>
      </h5>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:apply-templates>
    <xsl:with-param name="section.depth" select="$depth"/>
  </xsl:apply-templates>
    
  <!-- if it's a tutorial, put a link to try it at the end of the page -->
  <xsl:if test="$depth = 1">
    <xsl:if test="$tutorial-startpage">
      <p><a href="{$tutorial-startpage}">Try the Tutorial</a></p>
    </xsl:if>
  </xsl:if>

</xsl:if> <!-- doc-should-display -->
</xsl:template>

<!--
   - s1 is a separate web page, i.e. a main section.
  -->
<xsl:template match="s1">
  <xsl:apply-templates select="*"/>
</xsl:template>

<!--
   - s2 is the main section of a page
  -->
<xsl:template match="s2[@title or title or header/title]">
  <xsl:call-template name="aname"/>
  <p/>
  <table border=0 cellpadding=5 cellspacing=0 width='100%'>
    <tr class=section>
      <td>
        <font size="+2">
          <b>
            <xsl:choose>
              <xsl:when test="@href">
                <a href="{@href}"><xsl:call-template name="m_title"/></a>
              </xsl:when>
              <xsl:otherwise>
                <xsl:call-template name="m_title"/>
              </xsl:otherwise>
            </xsl:choose>
          </b>
        </font>
      </td>
      
      <xsl:if test="@version or (header/product  = 'resin-ee')">
        <td align="right">
          <xsl:if test="header/product = 'resin-ee'">
            <xsl:text>Resin-EE </xsl:text>
          </xsl:if>
          <xsl:value-of select="@version"/>
        </td>
      </xsl:if>
      
    </tr>
  </table>

  <xsl:apply-templates select='node()[name(.)!="title"]'/>
</xsl:template>

<!--
   - s3 is a subsection of a section
  -->
<xsl:template match="s3[@title or title or header/title]">
  <xsl:call-template name=aname/>
  <h3><xsl:call-template name="m_title"/></h3>

  <xsl:apply-templates select='node()[name(.)!="title"]'/>
</xsl:template>

<!--
   - s4 is a subsubsection of a section
  -->
<xsl:template match="s4[@title or title or header/title]">
  <xsl:call-template name=aname/>
  <h4><xsl:call-template name="m_title"/></h4>

  <xsl:apply-templates select='node()[name(.)!="title"]'/>
</xsl:template>

<!--
   - defun is a definition.
  -->
<xsl:template match="defun[@title or title]">
  <xsl:param name="section.depth"/>
  <xsl:if test="doc-should-display()">
  <xsl:call-template name=aname/>
  <xsl:choose>
    <xsl:when test="not(@version)">
      <h4><xsl:call-template name="m_title"/></h4>
    </xsl:when>
    <xsl:otherwise>
      <p/>
      <table border="0" cellspacing="0" cellpadding="0" width="100%">
        <tr>
          <td><h4><xsl:call-template name="m_title"/></h4></td>
          <td align="right"><xsl:value-of select="@version"/></td>
        </tr>
      </table>
    </xsl:otherwise>
  </xsl:choose>

  <div class="desc">
    <xsl:if test="parents">
      <b>child of:</b><xsl:text> </xsl:text><xsl:value-of select="parents"/><br/>
    </xsl:if>
    <xsl:if test="default | @default">
      <b>default:</b><xsl:text> </xsl:text><xsl:value-of select="default|@default"/><br/>
    </xsl:if>

    <xsl:apply-templates select='node()[name(.)!="title" and name(.)!="parents" and name(.)!="summary" and name(.)!="default"]'>
      <xsl:with-param name="section.depth" select="$section.depth"/>
    </xsl:apply-templates>
  </div>
  </xsl:if>
</xsl:template>

<!--
   - faq
   - child 'title' is the question
   - child 'description' is more of the question
   - contents (other than description) are the answer
  -->
<xsl:template match="faq[@title or title]">
  <xsl:param name="section.depth"/>
  <xsl:if test="doc-should-display()">
  <xsl:call-template name=aname/>
  <xsl:choose>
    <xsl:when test="not(@version)">
      <h4><xsl:call-template name="m_title"/></h4>
    </xsl:when>
    <xsl:otherwise>
      <p/>
      <table border="0" cellspacing="0" cellpadding="0" width="100%">
        <tr>
          <td><h4><xsl:call-template name="m_title"/></h4></td>
          <td align="right"><xsl:value-of select="@version"/></td>
        </tr>
      </table>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:variable name="d" select="description"/>
  <xsl:if test="$d">
    <div class="faqdesc">
      <xsl:apply-templates select="$d/node() | $d/text()"/>
    </div>
  </xsl:if>

  <div class="desc">
    <xsl:apply-templates select='node()[name(.)!="title" and name(.)!="description"]'>
      <xsl:with-param name="section.depth" select="$section.depth"/>
    </xsl:apply-templates>
  </div>
  </xsl:if>
</xsl:template>

<xsl:template match="section/title|s1/title|s2/title|s3/title|s4/title"/>

<!--
   - Website-specific
  -->
<xsl:template match="website">
  <xsl:apply-templates select="node()"/>
</xsl:template>


<!--
   - TABLES
  -->

<!-- plain tables -->
<xsl:template match="table">
  <xsl:copy>
    <xsl:apply-templates select="node()|@*"/>
  </xsl:copy>
</xsl:template>

<!-- definition tables -->
<xsl:template match="deftable">
  <p/>
  <xsl:variable name="title">
    <xsl:call-template name="m_title"/>
  </xsl:variable>
  <table width="{if(@width,@width,'90%'}" cellpadding=2 cellspacing=0 class=deftable border>
    <xsl:if test='$title'>
      <caption><font size="+1"><xsl:apply-templates select="$title"/></font></caption>
    </xsl:if>
    <xsl:apply-templates/>
  </table>
</xsl:template>

<xsl:template match="deftable-childtags">
  <p/>
  <xsl:variable name="title">
    <xsl:call-template name="m_title"/>
  </xsl:variable>
  <table width="{if(@width,@width,'90%'}" cellpadding=2 cellspacing=0 class=deftable border>
    <xsl:if test='$title'>
      <caption><font size="+1"><xsl:apply-templates select="$title"/></font></caption>
    </xsl:if>
    <tr><th>Attribute</th><th>Meaning</th><th>default</th></tr>
    <xsl:apply-templates/>
  </table>
</xsl:template>

<xsl:template match="deftable-parameters">
  <p/>
  <xsl:variable name="title">
    <xsl:call-template name="m_title"/>
  </xsl:variable>
  <table width="{if(@width,@width,'90%'}" cellpadding=2 cellspacing=0 class=deftable border>
    <xsl:if test='$title'>
      <caption><font size="+1"><xsl:apply-templates select="$title"/></font></caption>
    </xsl:if>
    <tr><th>Parameter</th><th>Meaning</th><th>default</th></tr>
    <xsl:apply-templates/>
  </table>
</xsl:template>


<xsl:template match="table/tr|deftable/tr|deftable-childtags/tr">
  <tr>
    <xsl:apply-templates select="@*|node()"/>
  </tr>
</xsl:template>

<xsl:template match="tr/th|tr/td">
  <xsl:copy>
  <xsl:apply-templates select="@*|node()"/>
  </xsl:copy>
</xsl:template>

<xsl:template match="table/caption">
  <xsl:copy>
  <xsl:apply-templates select="@*|node()"/>
  </xsl:copy>
</xsl:template>

<!--
   - LISTS
  -->

<!--
   - ul is an unordered list
  -->
<xsl:template match="ul">
  <ul>
    <xsl:copy-of select="@class"/>
    <xsl:apply-templates select="li"/>
  </ul>
</xsl:template>

<!--
   - ol is an ordered list
  -->
<xsl:template match="ol">
  <ol>
    <xsl:copy-of select="@class"/>
    <xsl:apply-templates select="li"/>
  </ol>
</xsl:template>

<!--
   - li is a list item
  -->
<xsl:template match="ul/li|ol/li">
  <li>
    <xsl:copy-of select="@class"/>
    <xsl:apply-templates select="node()"/>
  </li>
</xsl:template>

<!--
   - dl is a definition list
  -->
<xsl:template match="dl">
  <dl>
    <xsl:copy-of select="@class"/>
    <xsl:for-each select="dt|dd">
      <xsl:copy>
        <xsl:copy-of select="@class"/>
        <xsl:apply-templates/>
      </xsl:copy>
    </xsl:for-each>
  </dl>
</xsl:template>

<!--
   - BOXES and FIGURES
  -->

<!--
   - figure is an image
  -->
<xsl:template match="figure">
  <center>
  <#
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
  </center>
</xsl:template>

<!--
   - <example>
  -->
<xsl:template match="example">
  <xsl:variable name="title">
    <xsl:call-template name="m_title"/>
  </xsl:variable>

  <xsl:variable name="seeitin">
    <xsl:if test="@file">
      <xsl:if test="$title">
        <br/>
      </xsl:if>
      <xsl:text>See it in: </xsl:text>
      <xsl:call-template name="viewfile-link"/>
    </xsl:if>
  </xsl:variable>
  
  <p/>
  <table class='egpad' cellspacing='0' width='90%'>
    <xsl:if test='$title or $seeitin'>
      <caption>
        <font size="+1">
          <xsl:copy-of select="$title"/>
          <xsl:copy-of select="$seeitin"/>
        </font>
      </caption>
    </xsl:if>

    <tr><td class="{if(@size,concat('example-',@size),'example')}" bgcolor="#ffeecc">
      <pre><xsl:apply-templates/></pre>
    </td></tr>
  </table>
</xsl:template>

<!--
   - <results>
  -->
<xsl:template match="results">
  <xsl:variable name="title">
    <xsl:call-template name="m_title"/>
  </xsl:variable>
  <p/>
  <table class='egpad' cellspacing='0' width='90%'>
    <xsl:if test='$title'>
      <caption><font size="+1"><xsl:copy-of select="$title"/></font></caption>
    </xsl:if>

    <tr><td class="result" bgcolor="#ffccdd">
      <pre><xsl:apply-templates/></pre>
    </td></tr>
  </table>
</xsl:template>

<!--
   - <def>
  -->
<xsl:template match="def">
  <xsl:if test='not(@title)'>
    <p/>
  </xsl:if>
  <table class='egpad' cellspacing='0' width='90%'>
    <xsl:if test='@title'>
      <caption><font size="+1"><{@title}></font></caption>
    </xsl:if>

    <tr><td class="def" bgcolor="#cccccc">
      <pre><xsl:apply-templates/></pre>
    </td></tr>
  </table>
</xsl:template>

<xsl:template match="center">
  <xsl:copy>
  <xsl:apply-templates select="@*|node()"/>
  </xsl:copy>
</xsl:template>

<!--
   - <objsummary>
  -->

<!--
   - objsummary makes an index of fun marked with <defun/>
  -->
<xsl:template match="objsummary">
  <xsl:variable name="out">
    <xsl:call-template name="objsummary"/>
  </xsl:variable>
  <xsl:apply-templates select="$out"/>
</xsl:template>

<#!
String href(String name)
{
  if (name == null)
    return null;

  CharBuffer cb = CharBuffer.allocate();
  for (int i = 0; i < name.length(); i++) {
    char ch = name.charAt(i);
    if (Character.isWhitespace(ch))
      cb.append("-");
    else
      cb.append(ch);
  }

  return cb.close();
}

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

void writeSummary(XslWriter out, Node node)
  throws IOException, SAXException
{

}
#>

<xsl:template match='node()' mode=objsummary/>
<xsl:template match='node()' mode=defunsummary/>

<xsl:template match='defun' mode=objsummary>
  <xsl:if test="doc-should-display()">
    <tr><td class=code width="40%">
    <# writeSummaryName(out, node); #>
    </td><td>
    <xsl:value-of select="substring-before(p[1],'.')"/>
    </td></tr>
  </xsl:if>
</xsl:template>

<xsl:template name='objsummary'>
  <xsl:param name="title" select="@title"/>
<!--   <p/> -->
<!--   <table border="1" cellpadding="3" cellspacing="0" width="100%"> -->
<!--   <tr class=section><td colspan="2"> -->
<!--     <font size="+2"><b><xsl:value-of select="if($title,$title,'Index')"/></b></font> -->
<!--   </td></tr> -->
  <section title="{if($title,$title,'Index')}" 
           depth="{count(ancestor::document|section|subsection|s1|s2|s3|s4|s5|s6) + 1}">
    <table border="1" cellpadding="3" cellspacing="0" width="100%">
      <xsl:apply-templates select='following::defun' mode='objsummary'>
        <xsl:sort select="@title"/>
      </xsl:apply-templates>
    </table>
  </section>

<!--   </table> -->
</xsl:template>


<!-- index page -->
<xsl:template match="ixx">
  <dl class="index" compact="COMPACT">
    <xsl:apply-templates select="ix">
      <xsl:sort select="@id"/>
    </xsl:apply-templates>
  </dl>
</xsl:template>

<xsl:template match="ix">
  <dt><a name="{@id}" href="{@h}"><xsl:value-of select="if(@t,@t,@id)"/></a></dt>
  <dd><xsl:apply-templates select="d/node() | d/text()"/></dd>
</xsl:template>
  
<!--
   - INLINE FORMATTING
  -->

<!--
   - p is the main paragraph text
  -->
<xsl:template match="p">
  <p>
    <xsl:copy-of select="@class"/>
    <xsl:apply-templates select="node()"/>
  </p>
</xsl:template>

<!--
   - note adds a note
  -->
<xsl:template match="note">
  <p>
<b class="note">Note: </b> <xsl:apply-templates/>
  </p>
</xsl:template>

<!--
   - warn adds a warning note
  -->
<xsl:template match="warn">
  <p>
<b class="note warn">Warning: </b> <xsl:apply-templates/>
  </p>
</xsl:template>


<!--
   - TODO - adds a todo note
  -->
<xsl:template match="TODO">
  <xsl:if test="not(@hide) and installed('caucho-dev')">
    <xsl:choose>
      <!-- inline -->
      <xsl:when test="parent::p or parent::td">
        <xsl:choose>
          <xsl:when test="text() | node()">
            <span class="todo">[</span>
            <b class="note todo">TODO: </b> <xsl:apply-templates/>
            <span class="todo">]</span>
          </xsl:when>
          <xsl:otherwise>
            <span class="todo">[</span>
            <b class="todo">TODO</b>
            <span class="todo">]</span>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>

      <!-- makes p -->
      <xsl:otherwise>
        <xsl:choose>
          <xsl:when test="text() | node()">
            <p><b class="note todo">TODO: </b> <xsl:apply-templates/></p>
          </xsl:when>
          <xsl:otherwise>
            <p><b class="todo">TODO</b></p>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>

    </xsl:choose>
  </xsl:if>
</xsl:template>

<!--
   - blockquote
  -->
<xsl:template match="blockquote">
  <xsl:copy>
    <xsl:if test="@title">
      <xsl:value-of select="@title"/>
      <xsl:text>: </xsl:text>
    </xsl:if>
    <xsl:apply-templates/>
  </xsl:copy>
</xsl:template>

<!--
   - sidebar/callout
  -->
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

<!--
   - glossary
   -
   - @type one of 'inline', 'sidebar' (default), 'sidebar-left'
  -->
<xsl:template match="glossary">
<xsl:choose>
  <xsl:when test="@type = 'inline'">
    <div class='glossary'>
        <table cellspacing=0 border=0 width='100%'>
          <xsl:if test="@title">
            <tr><th><xsl:value-of select="@title"/></th></tr>
          </xsl:if>
          <tr><td>
            <xsl:apply-templates/>
          </td></tr>
        </table>
    </div>
  </xsl:when>
  <xsl:otherwise>
    <xsl:variable name="sideclass" select="if(@type = 'sidebar-left','leftside','side')"/>
    <div class='{$sideclass} glossary'>
<table cellspacing=0 border=0 width='100%'>
  <xsl:if test="@title">
    <tr><th class=sidetitle><xsl:value-of select="@title"/></th></tr>
  </xsl:if>
  <tr><td class=sidebody>
  <xsl:apply-templates/>
  </td></tr>
</table>
</div>
</xsl:otherwise>
</xsl:choose>
</xsl:template>

<!--
   - br is a line breaksup/font/small is any code
  -->
<xsl:template match="br">
  <xsl:copy>
    <xsl:apply-templates/>
  </xsl:copy>
</xsl:template>

<!--
   - var is a variable name
  -->
<xsl:template match="var">
  <span class=meta><xsl:apply-templates/></span>
</xsl:template>

<!--
   - ct:img is an image
  -->
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

<!--
   - code is any code
  -->
<xsl:template match="code">
  <tt><xsl:apply-templates/></tt>
</xsl:template>

<!--
   - sup/font/small is any code
  -->
<xsl:template match="sup|sub|font|small|em|b|jsp:expression|h3">
  <xsl:copy>
    <xsl:apply-templates select="node()|@*"/>
  </xsl:copy>
</xsl:template>

<!--
   - a[@href] is a link
  -->
<xsl:template match="a[@href]">
  <a href="{@href}">
    <xsl:apply-templates select="node()"/>
  </a>
</xsl:template>

<!--
   - header
   -
   - not displayed, information is pulled from it
  -->

<xsl:template match="header">
</xsl:template>

<xsl:template name="string-repeat">
  <xsl:param name="string"/>
  <xsl:param name="repeat"/>
  <xsl:if test="$repeat">
    <xsl:value-of select="$string"/>
    <xsl:call-template name="string-repeat">
      <xsl:with-param name="string" select="$string"/>
      <xsl:with-param name="repeat" select="$repeat - 1"/>
    </xsl:call-template>
  </xsl:if>
</xsl:template>
  
<!-- only if forwarded from the default homepage -->
<xsl:template match="default-homepage-only">
  <jsp:scriptlet>if ("true".equals(request.getParameter("default"))) { </jsp:scriptlet>
    <xsl:apply-templates/>
  <jsp:scriptlet>}</jsp:scriptlet>
</xsl:template>

</xsl:stylesheet>
