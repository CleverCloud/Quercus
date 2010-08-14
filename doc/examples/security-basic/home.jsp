<%@ taglib prefix='c' uri='http://java.sun.com/jsp/jstl/core' %>

<%@ include file="/inc/nobrowsercache.jspf" %>

<%--
  Some types of users have customized home pages.

  This jsp determines which type of user is making a request, and
  redirects the user's browser to the appropriate home.
  
  If a customized home page does not exist for this kind of user,
  then this page is used.
--%>

<%
  /** redirect to a more specific homepage if one is available */

  String home_url = null;

  if (request.isUserInRole("professor")) {
      home_url = "professors/";
  } else if (request.isUserInRole("staff")) {
      home_url = "staff/";
  } else if (request.isUserInRole("student")) {
      home_url = "students/";
  }

  if (home_url != null) {
      home_url = response.encodeRedirectUrl(home_url);
      response.sendRedirect(home_url);
      return; // don't do any more of the page
  }
%>

<html>
  <head>
    <title>Hogwart's::Home</title>
  </head>

  <body>
    <%@ include file="/inc/buttonbar.jspf" %>
    <h1>Home</h1>

    Welcome <c:out value="${pageContext.request.remoteUser}"/>.
    You are not a professor, a member of staff, or a student.

    <%@ include file="/inc/footer.jspf" %>
  </body>
</html>
