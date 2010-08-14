<%@ page import="com.caucho.bam.LocalActorClient, java.util.*, example.*"
%><%!
@javax.inject.Inject
ExampleService _service;

@javax.inject.Inject
ExampleMessages _messages;

LocalActorClient _client = new LocalActorClient();
%>
<%

out.println("<h3>Recent Messages</h3>");

ArrayList<String> messages = _messages.getMessages();

out.println("<ol>");
for (String msg : messages) {
  out.println("<li>" + msg);
}
out.println("</ol>");

String color1 = request.getParameter("color1");
String noun1 = request.getParameter("noun1");
String adj1 = request.getParameter("adj1");

if (color1 != null && noun1 != null && adj1 != null) {
  String msg = "The " + color1 + " " + noun1 + " is " + adj1 + ".";

  ExampleMessage exMsg = new ExampleMessage(msg);

  _client.message("bam-php-queue@localhost", exMsg);
}

%>

<h3>Compose new message:</h3>

<form method="POST">
The
<select name='color1'>
<option>red</option>
<option>blue</option>
<option>turquoise</option>
<option>burnt umber</option>
</select>

<select name='noun1'>
<option>car</option>
<option>house</option>
<option>canary</option>
<option>elephant</option>
</select>

is

<select name='adj1'>
<option>big</option>
<option>happy</option>
<option>overcaffinated</option>
<option>invisible</option>
</select>.

<p><input type='submit'>

</form>

<a href="index.xtp">tutorial</a> <a href="demo.php">demo.php</a>
