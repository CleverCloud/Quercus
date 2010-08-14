<%@ page import='java.util.*, com.caucho.util.*, java.io.*'
%><%!

%><html>
<head><title>Environment Script</title></head>
<body bgcolor=#ffffff>

<h1>Requested URL:</h1>
<!--
  -- Java classes, including static functions are available through
  -- the special 'Packages' object. 
  -->

<pre>
<%= HttpUtils.getRequestURL(request) %>
</pre>

<!-- The Request and Response objects conform to the Servlet API -->
 
<h1>Request Information:</h1>
<table>
<tr><td>Request method      <td><%= request.getMethod() %>
<tr><td>Request URI         <td><%= request.getRequestURI() %>
<tr><td>Request protocol    <td><%= request.getProtocol() %>
<tr><td>Servlet path        <td><%= request.getServletPath() %>
<tr><td>Path info           <td><%= request.getPathInfo() %>
<tr><td>Path translated     <td><%= request.getPathTranslated() %>
<tr><td>Query string        <td><%= request.getQueryString() %>
<tr><td>Content length      <td><%= request.getContentLength() %>
<tr><td>Content type        <td><%= request.getContentType() %>
<tr><td>Server name         <td><%= request.getServerName() %>
<tr><td>Server port         <td><%= request.getServerPort() %>
<tr><td>Remote user         <td><%= request.getRemoteUser() %>
<tr><td>Remote address      <td><%= request.getRemoteAddr() %>
<tr><td>Remote host         <td><%= request.getRemoteHost() %>
<tr><td>Remote port         <td><%= request.getRemotePort() %>
<tr><td>Authorization scheme<td><%= request.getAuthType() %>
</table>

<%
Object cert = (Object) request.getAttribute("javax.servlet.request.X509Certificate");
if (cert != null)
  out.println("cert: " + cert);
%>

<h1>Request Headers:</h1>
<table>
<%

Enumeration e = request.getHeaderNames();
while (e.hasMoreElements()) {
  String name = (String) e.nextElement();
  %><tr><td><%= name %><td><%= request.getHeader(name) %>
  <%
}

%>
</table>

<h1>Context Init Parameters:</h1>
<table>
<%

e = application.getInitParameterNames();
while (e.hasMoreElements()) {
  String name = (String) e.nextElement();
  %><tr><td><%= name %><td><%= application.getInitParameter(name) %>
  <%
}

%>
</table>

<%
Cookie []cookies = request.getCookies();
if (cookies != null && cookies.length > 0) {
  out.println("<h1>Cookies:</h1>");
  out.println("<table>");
  for (int i = 0; i < cookies.length; i++) {
    out.println("<tr><td>" + cookies[i].getName() +
                    "<td>" + cookies[i].getValue());
  }
  out.println("</table>");
}
%>
<h1>Form Values:</h1>
<table>

<%
//
// The query property returns form values.
//
e = request.getParameterNames();
while (e.hasMoreElements()) {
  String name = (String) e.nextElement();
  String []values = (String []) request.getParameterValues(name);
  String value = values[0];
  for (int i = 1; i < values.length; i++)
    value += ", " + values[i];

  %><tr><td><%= name %><td><%= value %>
  <%
}

%>

</table>

<h1>Request Attributes:</h1>
<table>

<%
//
// The query property returns form values.
//
e = request.getAttributeNames();
while (e.hasMoreElements()) {
  String name = (String) e.nextElement();
  Object value = request.getAttribute(name);

  %><tr><td><%= name %><td><%= value %></td></tr>
  <%
}
%>

</table>

<h1>Session Attributes:</h1>
<table>
<tr><td>session id<td><%= session.getId() %>
<%
//
// The query property returns form values.
//
e = session.getAttributeNames();
while (e.hasMoreElements()) {
  String name = (String) e.nextElement();
  Object value = session.getAttribute(name);

  %><tr><td><%= name %><td><%= value %></td></tr>
  <%
}
%>

</table>

<h1>Context Attributes:</h1>
<table>

<%
e = application.getAttributeNames();
while (e.hasMoreElements()) {
  String name = (String) e.nextElement();
  Object value = application.getAttribute(name);

  %><tr><td><%= name %><td><%= value %></td></tr>
  <%
}
%>

</table>

<%
InputStream is = request.getInputStream();
if (is != null && is.available() > 0) {
  out.println("<h1>Post</h1>");
  int ch;
  while ((ch = is.read()) >= 0)
    out.print((char) ch);
}
%>
</body>
</html>
