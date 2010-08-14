<html>
<head>
  <style type="text/css">
  dt {
    font-weight: bold;
    background-color: #ccddff;
  }
  dd {
    background-color: #ffeecc;
  }
  </style>
</head>
<body>
<%@ page import="javax.naming.*" %>
<%@ page import="example.FlickrAPI" %>
<%@ page import="example.data.*" %>
<%
Context context = (Context) new InitialContext().lookup("java:comp/env");
FlickrAPI flickr = (FlickrAPI) context.lookup("rest/flickr");
%>
<dl>
  <dt>flickr.findByEmail("", "resin@caucho.com")</dt>
  <dd><%= flickr.findByEmail("", "resin@caucho.com") %></dd>

  <br/>
  <dt>flickr.findByUsername("", "resin-caucho")</dt>
  <dd><%= flickr.findByUsername("", "resin-caucho") %></dd>

  <br/>
  <dt>flickr.getInfo("", "12345678901@N01")</dt>
  <dd><%= flickr.getInfo("", "12345678901@N01") %></dd>

  <br/>
  <dt>flickr.getPublicGroups("", "12345678901@N01")</dt>
  <dd><%= flickr.getPublicGroups("", "12345678901@N01") %></dd>

  <br/>
  <dt>flickr.getPublicPhotos("", "12345678901@N01", "", 10, 1)</dt>
  <dd><%= flickr.getPublicPhotos("", "12345678901@N01", "", 10, 1) %></dd>

  <br/>
  <dt>flickr.findByUsername("", "unknown-user")</dt>
  <dd><%= flickr.findByUsername("", "unknown-user") %></dd>
</dl>
</body>
</html>
