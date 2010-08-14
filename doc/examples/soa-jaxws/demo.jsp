<%@ page import="java.util.List" %>
<%@ page import="javax.naming.*" %>
<%@ page import="javax.xml.ws.Holder" %>
<%@ page import="example.UserService" %>
<%@ page import="example.User" %>
<%
Context context = (Context) new InitialContext().lookup("java:comp/env");

UserService service = (UserService) context.lookup("soap/UserService");
List<User> users = service.getUsers(2);

Exception invalid = null;

try {
  service.getUsers(0);
}
catch (Exception e) {
  invalid = e;
}
%>
<pre>
UserService.getUsers(1): <%= users %>
UserService.getUsers(0): <%= invalid %>
</pre>
