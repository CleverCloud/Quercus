<%@ page import='javax.naming.*' %>

<h1>Test Resource</h1>

<%= new InitialContext().lookup("java:comp/env/jca/test") %>
