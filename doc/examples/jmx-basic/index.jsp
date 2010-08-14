<%@ page import='com.caucho.jmx.Jmx, example.BasicMBean' %>
<%
BasicMBean basic = (BasicMBean) Jmx.find("example:name=basic");

out.println("data: " + basic.getData());
%>
