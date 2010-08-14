<%@ page session="false" import="javax.management.* com.caucho.jmx.Jmx java.util.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%
  // stop browser from caching the page
  response.setHeader("Cache-Control","no-cache,post-check=0,pre-check=0,no-store");
  response.setHeader("Pragma","no-cache");
  response.setHeader("Expires","Thu,01Dec199416:00:00GMT");

  // refresh every 5 seconds
  response.setHeader("refresh","5");

  // prepare objects
  ObjectName query = new ObjectName("resin:type=PeriodicTask,*");
  pageContext.setAttribute("mbeans",Jmx.query(query));
%>

<html>
<head><title>mbean</title></head>

<body>
<h1>mbean</h1>

This page is automatically refreshed every 5 seconds.

<c:forEach var="mbean" items="${mbeans}">

<hr/>
<dl>

<dt>estimatedAverageTime
<dd>${mbean.estimatedAverageTime}

<dt>active
<dd>${mbean.active}

<dt>estimatedTimeRemaining
<dd>${mbean.estimatedTimeRemaining}

<dt>lastActiveTime
<dd>${mbean.lastActiveTime}

<dt>totalActiveCount
<dd>${mbean.totalActiveCount}

<dt>totalActiveTime
<dd>${mbean.totalActiveTime}

<dt>averageActiveTime
<dd>${mbean.averageActiveTime}

</dl>
</c:forEach>

<hr/>

</body>
</html>

