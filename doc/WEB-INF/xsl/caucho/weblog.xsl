<xsl:stylesheet>

<xsl:template name='ct:weblog-edit'>
<% if (request.getAttribute("editor") != null) { %>
<hr>
<form method='POST' action='<%= request.getRequestURI() %>'>
<table>
<!--
<tr><td>Title</td>
    <td><input name=title size=40 value='<%= request.attribute.title %>'></td>
<tr><td>Link</td>
    <td><input name=link size=40  value='<%= request.attribute.link %>'></td>
-->
<tr><!-- <td>Description</td> -->
    <td><textarea name=description cols=60 rows=10><%=
  request.getAttribute("description")
%></textarea></td>
</table>
<input type=hidden name='edit' value='<%= request.getAttribute("edit") %>'>
<input type=submit value='Add Entry'>
</form>
<% } %>
</xsl:template>

ct:weblog
<<
<%@ page language='java' import='com.caucho.web.weblog.*, java.util.*'
         import='com.caucho.util.*'
%>
<%
  Iterator _ct_weblog_iter = WebLog.processRequest(pageContext);

  while (_ct_weblog_iter.hasNext()) {
    WebLog _ct_weblog = (WebLog) _ct_weblog_iter.next();
%>
<xsl:apply-templates/>
<%
  }
%>
>>

ct:weblog//ct:date
<<
<%= QDate.format(_ct_weblog.getDate()) %>
>>

ct:weblog//ct:date[@format]
<<
<%= QDate.format(_ct_weblog.getDate(), "<{format}>") %>
>>

ct:webitem
<<
<%
  Iterator _ct_webitem_iter = _ct_weblog.iterator();
  for (int _ct_webitem_count = 0;
       _ct_webitem_iter.hasNext();
       _ct_webitem_count++) {
    WebLogItem _ct_weblog_item = (WebLogItem) _ct_webitem_iter.next();
%>
<xsl:apply-templates/>
<%
    if (request.getAttribute("editor") != null &&
        _ct_weblog_item.getIndex() == 1)
      out.print("\<a href='" + request.getRequestURI() +
                "?edit=" + _ct_weblog_item.getIndex() + "'>edit</a>");
  }
%>
>>

ct:webitem//ct:title
<<
<%
/*
  var _ct_weblog_title = _ct_weblog_item.title;
  if (! _ct_weblog_title)
    _ct_weblog_title = _ct_weblog_item.link;

  if (_ct_weblog_item.link)
    out.print("\<a href='" + _ct_weblog_item.link + "'>" +
              _ct_weblog_title + "</a>");
  else if (_ct_weblog_title)
    out.print(_ct_weblog_title);
*/
%>
>>

ct:webitem//ct:description
<<
<%
  if (_ct_weblog_item.getDescription() != null)
    out.print(_ct_weblog_item.getDescription());
%>
>>

ct:webitem//ct:date
<<
<%= QDate.format(_ct_weblog_item.getDate()) %>
>>

ct:webitem//ct:date[@format]
<<
<%= QDate.format(_ct_weblog_item.getDate(), "<{@format}>") %>
>>

</xsl:stylesheet>
