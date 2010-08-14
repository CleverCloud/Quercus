<%@ page import="javax.naming.Context" %>
<%@ page import="javax.naming.InitialContext" %>

<%@ page import="com.caucho.hessian.client.HessianProxyFactory" %>

<%@ page import="example.LogService" %>

<%
// Check the log

Context context = (Context) new InitialContext().lookup("java:comp/env");
LogService logService = (LogService) context.lookup("example/LogService");

out.println("<a href=\"\">Refresh</a><br/>");
out.println("Logged messages:<br/>");
out.println("<pre>");
out.println(logService.getLog());
out.println("</pre>");

// Make a request

HessianProxyFactory factory = new HessianProxyFactory();

String url = "jms:jms/ServiceQueue";

LogService log = (LogService) factory.create(LogService.class, url);

log.log("Hello, World");
%>

