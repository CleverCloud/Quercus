<%@ page session="false" import="java.util.Iterator" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>

<jsp:useBean id="jsputil" class="com.caucho.doc.javadoc.JspUtil" scope="request">
  <jsp:setProperty name="jsputil" property="request"
                  value="${pageContext.request}"/>
  <jsp:setProperty name="jsputil" property="response"
                  value="${pageContext.response}"/>
</jsp:useBean>

${jsputil.sendHttpCacheHeaders()}

<c:if test="${not empty param.query}">
<jsp:useBean id="query" class="com.caucho.doc.javadoc.Query">
<jsp:setProperty name="query" property="store" value="${jsputil.store}"/>
<jsp:setProperty name="query" property="query" value="${param.query}"/>
<jsp:setProperty name="query" property="offset" value="${param.offset}"/>
<jsp:setProperty name="query" property="limit" value="${param.limit}"/>
</jsp:useBean>
</c:if>

<c:set var="results" value="${query.results}"/>

<c:if test="${(not empty results) and (not results.empty)}">
  <c:if test="${results.first.exact}">
    <c:url var="firstResultHref" value="${results.first.href}"/>
  </c:if>
</c:if>

<table class="buttonbar" border="0" align="right">
<tr>

<c:choose>
<c:when test="${empty param.noframes}">
<td>[<a target="classFrame" href="<c:url value="overview.jsp"/>">Overview</a>]</td>
</c:when>
<c:otherwise>
<td>[<a href="<c:url value="overview.jsp"/>">Overview</a>]</td>
</c:otherwise>
</c:choose>
</tr>
</table>

<p>

<c:url var="submitUrl" value="search.jsp">
  <c:if test="${not empty param.noframes}">
    <c:param name="noframes">${param.noframes}</c:param>
  </c:if>
  <c:if test="${not empty param.limit}">
    <c:param name="limit">${param.limit}</c:param>
  </c:if>
</c:url>
<div class="search">
<form name="searchForm" action="${submitUrl}">
Search:&nbsp;<input name="query" value="<c:out value="${query.query}"/>">
</form>
</div>

<p>

<c:if test="${empty query}">
<h2>Example Searches:</h2>
"<code>Stream</code>" - anything starting with Stream<br>
"<code>class Stream</code>" - any class starting with Stream<br>
"<code>*Stream*</code>" - anything containing "Stream"<br>
"<code>class *Stream*</code>" - any class containing "Stream"<br>
"<code>java.io.InputStream</code>"<br>
"<code>java.io.*Stream</code>"<br>
<p>
The first word in the search can be used to limit the results to a particular
type.  The valid words are <code>package, class, method, </code> and <code>var</code>.  
</c:if>

<c:if test="${not empty query}">
<script 
<script type="text/javascript">
<!--
document.write("<p>Press SHIFT-/ to do another search<p>");
<c:if test="${not empty firstResultHref}">
  showClassHref("${firstResultHref}");
</c:if>
// -->
</script>

<h2>Results</h2>

<c:choose>
<c:when test="${empty results}">
None
</c:when>
<c:otherwise>
<c:if test="${query.isNextPage or query.isPreviousPage}">
<c:url var="nextUrl" value="search.jsp">
  <c:if test="${not empty param.noframes}">
    <c:param name="noframes">${param.noframes}</c:param>
  </c:if>
  <c:param name="query">${param.query}</c:param>
  <c:param name="offset">${query.nextPageOffset}</c:param>
  <c:if test="${not empty param.limit}">
    <c:param name="limit">${param.limit}</c:param>
  </c:if>
</c:url>
<c:url var="previousUrl" value="search.jsp">
  <c:if test="${not empty param.noframes}">
    <c:param name="noframes">${param.noframes}</c:param>
  </c:if>
  <c:param name="query">${param.query}</c:param>
  <c:if test="${query.previousPageOffset > 0}">
    <c:param name="offset">${query.previousPageOffset}</c:param>
  </c:if>
  <c:if test="${not empty param.limit}">
    <c:param name="limit">${param.limit}</c:param>
  </c:if>
</c:url>

<c:set var="pagesbuttonbar">
<p>
<table class="pagesbuttonbar" border="0" align="right">
<tr>
<c:if test="${query.isPreviousPage}">
<td>[<a href="${previousUrl}">previous</a>]</td>
</c:if>
<c:if test="${query.isNextPage}">
<td>[<a href="${nextUrl}">next</a>]</td>
</c:if>
</tr>
</table>
<p>
</c:set>
</c:if>  <%-- if prev or next --%>

<c:out value="${pagesbuttonbar}" escapeXml="false"/>

<c:forEach var="item" items="${results}">
<div class="r">
<c:choose>
<c:when test="${empty param.noframes}">
<a target="classFrame" href="${item.href}" onClick="showClassHref('${item.href}')"><b>${item.name}</b></a><br>
</c:when>
<c:otherwise>
<a href="${item.href}" onClick="showClassHref('${item.href}')"><b>${item.name}</b></a><br>
</c:otherwise>
</c:choose>
<code>${item.typeString}&nbsp;${item.fullname}</code>
<div>${item.description}</div>
</div>
</c:forEach>

<c:out value="${pagesbuttonbar}" escapeXml="false"/>

</c:otherwise>
</c:choose>  <%-- if results -->

</c:if>  <%-- if not empty query --%>

