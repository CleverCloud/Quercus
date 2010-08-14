<head>
  <link rel='STYLESHEET' type='text/css' href='../../css/default.css'/>
  <title>Resin WebSocket Tutorial</title>
</head>

<div class='breadcrumb'>
  <a href='../../'>docs</a>
 / <a href='../'>examples</a>
 / <a href='index.xtp'>Resin WebSocket pattern tutorial</a>
</div>

<h1>Resin WebSocket Tutorial</h1>

<table style='border: 1 solid #0080d0'>
<tr>
  <th>Sent: <th>
  <td><p id="send"> </p></td>
</tr>
<tr>
  <th>Receive: <th>
  <td style='color:#c00000'><p id="receive"> </p></td>
</tr>
</table>

<?php

//
// construct a websocket URL to connect back to Resin's WebSocket servlet.
// the URL:
//
//   ws://localhost:8080/resin-doc/examples/websocket-php/websocket.php

$host = $_SERVER['SERVER_NAME'];
$port = $_SERVER['SERVER_PORT'];

$uri = $_SERVER['REQUEST_URI'];
$uri = preg_replace("|/client.php|", "/websocket.php", $_SERVER['REQUEST_URI']);

$url = "ws://$host:$port$uri";

?>

<script language="javascript">

function wsopen(event)
{

  item = document.getElementById("send");
  
  item.firstChild.nodeValue += " [hello]";
  ws.send("hello");
  
  item.firstChild.nodeValue += " [server]";
  ws.send("server");

}

function wsmessage(event)
{
  data = event.data;

  item = document.getElementById("receive");
  item.firstChild.nodeValue += " [" + data + "]";
  
  alert("received: [" + data + "]");
}

function wsclose(event)
{
  alert("websocket " + event + " is closed");
}

ws = new WebSocket("<?= $url ?>");
wsopen.ws = ws;
ws.onopen = wsopen;
ws.onmessage = wsmessage;
ws.onclose = wsclose;

</script>

<h1>Description</h1>

<p>The JavaScript client sends two messages to the WebSocket servlet: "hello",
and "server". When the servlet receives "hello", it returns "world". When
it receives "server" it returns "Resin".</p>

<p>When the client receives the response, it adds the received text to the
"Received: " area.</p>

<h1>Links</h1>

<a href="index.xtp">WebSocket Resin tutorial</a>
