<%@ taglib prefix='c' uri='http://java.sun.com/jsp/jstl/core' %>

<%@ include file="/inc/nobrowsercache.jspf" %>

<%-- invalidating the session causes a loss of all session
     information, including the identity of the user
     --%>

<% session.invalidate(); %>

<html>
  <head>
    <title>Hogwart's::Logout</title>
  </head>

  <body>
    <%@ include file="/inc/buttonbar.jspf" %>

    <h1>Logout</h1>

    You have successfully logged out.

    <%@ include file="/inc/footer.jspf" %>
  </body>
</html>
