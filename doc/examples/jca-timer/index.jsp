<%@ page import="javax.naming.*" %>
<%= new InitialContext().lookup("java:comp/env/jca/timer") %>
