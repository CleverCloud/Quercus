<%@ taglib prefix='c' uri='http://java.sun.com/jsp/jstl/core' %>

<html>
  <head>
    <title>Login</title>
  </head>

  <body>
    <h1>Login</h1>

    <%-- this form-login-page form is also used as the 
         form-error-page to ask for a login again.
         --%>
    <c:if test="${not empty param.login_error}">
      <font color="red">
        Your login attempt was not successful, try again.
      </font>
    </c:if>

    <form action="<c:url value='j_security_check'/>" method="POST">
      <table>
        <tr><td>User:</td><td><input type='text' name='j_username'></td></tr>
        <tr><td>Password:</td><td><input type='password' name='j_password'></td></tr>

        <tr><td colspan='2'><input type=submit></td></tr>
      </table>

      <!--
        -  In case the user got here without a session, redirect
        -  successful requests to the home page for authenticated
        -  users.  (This is a non-standard, but useful field.)
        -->
      <input type='hidden' name='j_uri' value='/home.jsp'/>
    </form>



    For this example, you can try the following user/password
    combinations:
    <table border='1'>
      <tr><td><b>User</b></td><td><b>Password</b></td><td><b>Roles</b></td>
      <tr><td>harry<td>quidditch<td>student,gryffindor
      <tr><td>filch<td>mrsnorris<td>staff
      <tr><td>pince<td>quietplease<td>staff,website
      <tr><td>snape<td>potion<td>professor,slytherin
      <tr><td>mcgonagall<td>quidditch<td>professor,gryffindor
      <tr><td>dmalfoy<td>pureblood<td>student,slytherin
      <tr><td>lmalfoy<td>myself<td>alumni,gryffindor
    </table>

    <%@ include file="/inc/footer.jspf" %>
  </body>
</html>
