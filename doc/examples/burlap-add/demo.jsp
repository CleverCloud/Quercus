<%@ page import="com.caucho.burlap.client.BurlapProxyFactory" %>
<%@ page import="example.MathService" %>
<%
BurlapProxyFactory factory = new BurlapProxyFactory();

// http://localhost:8080/resin-doc/tutorial/burlap-add/burlap/math

String url = ("http://" +
              request.getServerName() + ":" + request.getServerPort() +
              request.getContextPath() + "/burlap/math");

MathService math = (MathService) factory.create(MathService.class, url);
%>
<pre>
3 + 2 = <%= math.add(3, 2) %>
3 - 2 = <%= math.sub(3, 2) %>
3 * 2 = <%= math.mul(3, 2) %>
3 / 2 = <%= math.div(3, 2) %>
</pre>
