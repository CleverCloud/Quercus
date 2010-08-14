<%@ page session="false" import="java.util.Iterator" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>

<jsp:useBean id="jsputil" class="com.caucho.doc.javadoc.JspUtil" scope="request">
  <jsp:setProperty name="jsputil" property="request"
                  value="${pageContext.request}"/>
  <jsp:setProperty name="jsputil" property="response"
                  value="${pageContext.response}"/>
</jsp:useBean>

${jsputil.sendHttpCacheHeaders()}

<html>
<head>
<title>Resin Javadoc @VERSION@ Overview</title>
<link rel="stylesheet" href="default.css" type="text/css">
<LINK rel="icon" href="images/dragonfly-tiny.png" type="image/png">
<LINK rel="shortcut icon" href="images/dragonfly-icon.png" type="image/png">
</head>

<body>
<h1>Resin Javadoc @VERSION@ Overview</h1>
</body>

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
</code>
<c:url var="overviewUrl" value="${api.location}/overview-summary.html"/>
<c:url var="treeUrl" value="${api.location}/overview-tree.html"/>
<br>
[<a href="${overviewUrl}">Overview</a>]
[<a href="${treeUrl}">Tree</a>]
<c:if test="${not empty api.description}">
<p>${api.description}<p>
</c:if>

</c:forEach>

<c:if test="${jsputil.store.showAddHelp}">
<hr/>
<p>To make more api's available for searching, edit the file
${pageContext.servletContext.getRealPath("/WEB-INF/web.xml")}
</c:if>

</html>

