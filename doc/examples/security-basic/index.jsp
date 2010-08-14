<%@ taglib prefix='c' uri='http://java.sun.com/jsp/jstl/core' %>

<%@ include file="/inc/nobrowsercache.jspf" %>

<%-- /index.jsp - default page for website. --%>


<html>
  <head>
    <title>Hogwarts</title>
  </head>

  <body>
    <%@ include file="/inc/buttonbar.jspf" %>

    <h1>Welcome to Hogwarts!</h1>

    This is a Defense Against the Dark Arts example of using
    JSP/Servlet security. 
    <a href="<c:url value='index.xtp'/>">Tutorial documentation</a> is 
    available.
    <p>

    Try doing a 
    <c:choose>
      <c:when test="${empty pageContext.request.userPrincipal}">
        <a href="<c:url value='home.jsp'/>">login</a>
      </c:when>
      <c:otherwise>
        <a href="<c:url value='logout.jsp'/>">logout</a>
      </c:otherwise>
    </c:choose>

    <p>
    To get a better understanding of how security works, try using
    the following links both when you are logged in and when you are
    not.
    <p>
    All of the links are in secure areas.  If you are not
    logged in a login procedure is put in by Resin before you get
    to the pages.  If you are logged in, you may be able to see them 
    or you may get a 'Forbidden' error.
    <p>
    Links to different areas:
    <ul>
      <li><a href="<c:url value='students/'/>">
	    Students (available to 'students' and 'professors')
	  </a>
      <li><a href="<c:url value='professors/'/>">
	    Professors (available to 'professors')
	  </a>
      <li><a href="<c:url value='staff/'/>">
	    Staff (available to 'staff' and 'professors')
	  </a>
    </ul>

    In a real application, you wouldn't show links like this -- you
    would get the user to login first and then only display the links
    that are available for their role.

    <%@ include file="/inc/footer.jspf" %>
  </body>
</html>
