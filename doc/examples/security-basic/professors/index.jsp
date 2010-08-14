<%@ taglib prefix='c' uri='http://java.sun.com/jsp/jstl/core' %>

<%@ include file="/inc/nobrowsercache.jspf" %>

<html>
  <head>
    <title>Hogwart's::Professor's Home</title>
  </head>

  <body>
    <%@ include file="/inc/buttonbar.jspf" %>
    <h1>Professor's Home</h1>

    Welcome <c:out value="${pageContext.request.remoteUser}"/>.
    <p>
    As a Professor, you also have access to the 
    <a href="<c:url value='/students/'/>">Student</a> and 
    <a href="<c:url value='/staff/'/>">Staff</a> home pages.

    <%@ include file="/inc/footer.jspf" %>
  </body>
</html>
