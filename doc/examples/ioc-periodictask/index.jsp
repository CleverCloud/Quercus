<%@ page session="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%
  // stop browser from caching the page
  response.setHeader("Cache-Control","no-cache,post-check=0,pre-check=0,no-store");
  response.setHeader("Pragma","no-cache");
  response.setHeader("Expires","Thu,01Dec199416:00:00GMT");

  // refresh every 5 seconds
  response.setHeader("refresh","5");
%>

<html>
<head><title>PeriodicTask typical application page</title></head>

<body>
<h1>PeriodicTask typical application page</h1>

<p>
This page is representative of any page in the web application.
It will refresh automatically in 5 seconds.  When the Periodic task is running,
this page will not be shown.  
</p>

<p>
The <a href="admin/periodictask">PeriodicTaskServlet</a> provides an
administration interface to the periodic task.
</p>

<p>
<a href="admin/mbean.jsp">admin/mbean.jsp</a> provides a very simple
example of using Jmx to access the PeriodicTask instance.
<p>

</body>
</html>

