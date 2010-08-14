<%@ page import='com.caucho.jmx.Jmx' %>
<%@ page import='example.EmitterMBean' %>
<%@ page import='example.ListenerMBean' %>
<%
EmitterMBean emitter = (EmitterMBean) Jmx.find("example:name=emitter");

emitter.send();

ListenerMBean listener = (ListenerMBean) Jmx.find("example:name=listener");
out.println("count: " + listener.getNotificationCount());
%>
