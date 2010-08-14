<%@ page import="com.caucho.naming.Jndi" %>
<%@ page import="example.HelloService" %>
<%
HelloService hessianHello = (HelloService) Jndi.lookup("hessian/HelloService");
HelloService vmHello = (HelloService) Jndi.lookup("service/HelloService");
%>
<pre>
From Hessian: <%= hessianHello.hello() %>
From VM: <%= vmHello.hello() %>
</pre>
