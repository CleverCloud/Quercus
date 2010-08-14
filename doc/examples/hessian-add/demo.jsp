<%@ page import="com.caucho.hessian.client.HessianProxyFactory" %>
<%@ page import="example.MathService" %>
<%
HessianProxyFactory factory = new HessianProxyFactory();

// http://localhost:8080/resin-doc/protocols/tutorial/hessian-add/hessian/math

String url = ("http://" +
              request.getServerName() + ":" + request.getServerPort() +
              request.getContextPath() + "/hessian/math");

MathService math = (MathService) factory.create(MathService.class, url);
%>
<pre>
3 + 2 = <%= math.add(3, 2) %>
3 - 2 = <%= math.sub(3, 2) %>
3 * 2 = <%= math.mul(3, 2) %>
3 / 2 = <%= math.div(3, 2) %>
</pre>
