<%@ page session="false" import="java.util.Iterator" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>

<jsp:useBean id="jsputil" class="com.caucho.doc.javadoc.JspUtil" scope="request">
  <jsp:setProperty name="jsputil" property="request"
                  value="${pageContext.request}"/>
  <jsp:setProperty name="jsputil" property="response"
                  value="${pageContext.response}"/>
</jsp:useBean>

Javadoc search not available.
<p>

The following api's are available:<p>

<c:set var="allApi" value="${jsputil.store.allApi}"/>
<c:forEach var="api" items="${allApi}">
<h3>${api.name}</h3>
Location:
<code>
<c:choose>
<c:when test="${api.local}">
local
</c:when>
<c:otherwise>
${api.location}
</c:otherwise>
</c:choose>
</code><br>
<%--
<c:url target var="overviewUrl" value="${api.location}/overview-summary.html"/>
<c:url var="treeUrl" value="${api.location}/overview-tree.html"/>
  <c:when>
<br>
<c:choose>
  <c:when test="${empty param.noframes}">
[<a target="classFrame" href="${overviewUrl}">Overview</a>]
[<a target="classFrame" href="${treeUrl}">Tree</a>]
  </c:when>
  <c:otherwise>
[<a href="${overviewUrl}">Overview</a>]
[<a href="${treeUrl}">Tree</a>]
  </c:otherwise>
--%>
<c:if test="${not empty api.description}">
<p>${api.description}<p>
</c:if>

</c:forEach>

<p><hr/>
<p>Javadoc search not available: ${jsputil.store.initError}
<c:if test="${jsputil.store.showAddHelp}">
<p>To configure the search capability, edit the file
${pageContext.servletContext.getRealPath("/WEB-INF/web.xml")}
</c:if>
