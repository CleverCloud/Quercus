$output(method=>html, disable-output-escaping=>true);

<xsl:import href='caucho/defaultcopy.xsl'/>

<xsl:import href='com/caucho/xsllib/format.xsl'/>
<xsl:import href='com/caucho/xsllib/lib.xsl'/>
<xsl:import href='com/caucho/xsllib/form.xsl'/>

<!--
<xsl:import href='com/caucho/quercus/quercus.xsl'/>
<xsl:import href='com/caucho/quercus/story/story.xsl'/>
<xsl:import href='com/caucho/quercus/user/user.xsl'/>
<xsl:import href='com/caucho/quercus/comment/comment.xsl'/>
-->

<xsl:import href='caucho/comment.xsl'/>

<xsl:import href='caucho/control.xsl'/>
<xsl:import href='caucho/contents.xsl'/>
<xsl:import href='caucho/format.xsl'/>

html << $apply-templates(); >>
body << $apply-templates(); >>

<#!
  String top = "/";
#>