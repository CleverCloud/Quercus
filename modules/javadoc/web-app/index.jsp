<%@ page session="false" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>


<jsp:useBean id="jsputil" class="com.caucho.doc.javadoc.JspUtil" scope="request">
  <jsp:setProperty name="jsputil" property="request"
                  value="${pageContext.request}"/>
  <jsp:setProperty name="jsputil" property="response"
                  value="${pageContext.response}"/>
</jsp:useBean>

${jsputil.sendHttpCacheHeaders()}

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Frameset//EN" "http://www.w3.org/TR/html4/frameset.dtd">
<HTML>
<HEAD>
<TITLE>Resin Javadoc @VERSION@</TITLE>
<LINK rel="icon" href="images/dragonfly-tiny.png" type="image/png">
<LINK rel="shortcut icon" href="images/dragonfly-icon.png" type="image/png">
</HEAD>

<c:url var="searchUrl" value="search.jsp">
  <c:if test="${not empty param.query}">
    <c:param name="query">${param.query}</c:param>
  </c:if>
  <c:if test="${not empty param.limit}">
    <c:param name="limit">${param.limit}</c:param>
  </c:if>
</c:url>

<c:choose>
<c:when test="${empty param.noframes}">

<FRAMESET cols="20%,80%">
<FRAME src="${searchUrl}" name="packageListFrame" title="Search">
<FRAME src="${jsputil.store.startPage}" name="classFrame" title="Overview">
<NOFRAMES>
<c:url var="noframesSearchUrl" value="search.jsp">
  <c:param name="noframes">true</c:param>
  <c:if test="${not empty param.query}">
    <c:param name="query">${param.query}</c:param>
  </c:if>
  <c:if test="${not empty param.limit}">
    <c:param name="limit">${param.limit}</c:param>
  </c:if>
</c:url>
Frames unavailable - use the <a href="${noframesSearchUrl}">no frames</a> version.
</NOFRAMES>
</FRAMESET>
</c:when>
<c:otherwise>
<jsp:include page="search.jsp"/>
<jsp:include page="${jsputil.store.startPage}"/>
</c:otherwise>
</c:choose>
</HTML>
