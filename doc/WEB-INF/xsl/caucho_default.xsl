<xsl:output method="html" disable-output-escaping="true" resin:disable-output-escaping="yes"/>

<xsl:param name="xtp:context_path"/>
<xsl:param name="xtp:servlet_path"/>

<xsl:import href='caucho/defaultcopy.xsl'/>
<xsl:import href='caucho/nodeinfo.xsl'/>
<xsl:import href='caucho/newstyle.xsl'/>
<xsl:import href='caucho/fun.xsl'/>
<xsl:import href='caucho/toc.xsl'/>
<xsl:import href='caucho/href.xsl'/>
<xsl:import href='caucho/include.xsl'/>
<xsl:import href='caucho/viewfile.xsl'/>

<!-- website specific -->
<xsl:import href='website/caucho_header.xsl'/>
<xsl:import href='website/xsllib/format.xsl'/>
<xsl:import href='website/xsllib/lib.xsl'/>
<xsl:import href='website/xsllib/form.xsl'/>
<xsl:import href='website/control.xsl'/>
<xsl:import href='website/contents.xsl'/>
<xsl:import href='website/format.xsl'/>
<xsl:import href='website/comment.xsl'/>


