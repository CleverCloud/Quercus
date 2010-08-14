<%@ page import='com.caucho.jmx.Jmx, example.TestMBean' %>
<%
TestMBean test = (TestMBean) Jmx.find("example:name=test");

out.println("ObjectName: " + test.getObjectName());
%>
