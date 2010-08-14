<%@ page import="javax.inject.*" %>
<%@ page import="example.HelloService" %>
<%!
@Inject @Named("hessian") HelloService _hessianHello;
@Inject @Named("burlap") HelloService _burlapHello; 
@Inject @Named("vm") HelloService _vmHello; 
%>
<pre>
From Hessian: <%= _hessianHello.hello() %>
From Burlap: <%= _burlapHello.hello() %>
From VM: <%= _vmHello.hello() %>
</pre>

<ul>
<li><a href="demo.php">PHP</a></li>
<li><a href="index.xtp">Tutorial</a></li>
<ul>
