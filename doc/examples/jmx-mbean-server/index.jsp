<%@ page import='javax.inject.Inject, javax.management.*, example.TestAdmin' %>
<%!
@Inject MBeanServer _server;
%><%
ObjectName name = new ObjectName("example:name=test");

out.println("data: " + _server.getAttribute(name, "Data"));
%>
