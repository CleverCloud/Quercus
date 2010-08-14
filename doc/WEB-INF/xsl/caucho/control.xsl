<xsl:stylesheet>

<#@ cache #>
<#!
  int unique = 0;

  /**
   * Calculate a runtime attribute or a string.
   */
  String _ct_rta(Node node, String attr)
  {
    Element elt = (Element) node;
    String value = elt.getAttribute(attr);
    if (value == null)
      return "\"\"";
    if (value.startsWith("<%=") && value.endsWith("%>"))
      return value.substring(3, value.length() - 2);
    else
      return "\"" + value + "\""; 
  }
#>

<!--
   - An if statement
   -
   - <ct:if expr='request.getParameter("color").equals("blue")'>
   - boy
   - </ct:if>
  -->

ct:if[@expr] <<
<% if (<{@expr}>) { %>
  <xsl:apply-templates/>
<% } %>
>>

<!--
   - A parameter test statement
   -
   - <ct:if param='color' value='blue'>
   - boy
   - </ct:if>
  -->

ct:if[@param] <<
<% if ((<#= _ct_rta(node, "value") #>).equals(request.getParameter(<#= _ct_rta(node, "param") #>))) { %>
  <xsl:apply-templates/>
<% } %>
>>

<!--
   - A generic switch statement
   -
   - <ct:switch select='<%= request.getParameter("color") %>'>
   - <ct:case value='blue'>boy</ct:case>
   - <ct:case value='ping'>girl</ct:case>
   - <ct:default>furry creature from alpha centauri</ct:default>
   - </ct:switch>
  -->

ct:switch[@select] <<
<# String id = "_ct_tmp" + unique++; #>
<% Object <#= id #> = <#= _ct_rta(node, "select") #>; %>

<% if (<#= id #> == null) { %>
  <xsl:apply-templates select='ct:default[1]/node()'/>
<% }

<xsl:for-each select='ct:case'>
  else if (<#= id #>.equals(<#= _ct_rta(node1, "value") #>)) { %>
  <xsl:apply-templates/>
<% }
</xsl:for-each>

<xsl:for-each select='ct:default[1]'>
  else { %>
  <xsl:apply-templates/>
<% }
</xsl:for-each>
%>
>>

<!--
   - An enumeration statement
   -
   - <ct:enum name='header' type='String' expr='request.getHeaderNames()'>
   -   <jsp:get name='header'/>: <jsp:get
   - </ct:enum>
  -->

ct:enum <<
<%@ page import='java.util.*' %>
<# String id = "_ct_tmp" + unique++; #>
<%
   Enumeration <#= id #> = <{@expr}>; 
   while (<#= id #>.hasMoreElements()) {
     <{@type}> <{@name}> = (<{@type}>) <#= id #>.nextElement();
%>
  <xsl:apply-templates/>
<%
  }
%>
>>

</xsl:stylesheet>
