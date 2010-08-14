<?php

$service_queue = new JMSQueue("jms/ServiceQueue");

if (! $service_queue) {
  exit("Unable to get service queue!");
} 

$log_service = jndi_lookup("example/LogService");

echo "<a href=\"\">Refresh</a><br/>\n";
echo "Logged messages:<br/>\n";
echo "<pre>\n";
echo $log_service->getLog();
echo "</pre>\n";

$request = 
  "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-enveloper\">" .
  "<env:Body>" .
  "<m:log xmlns:m=\"urn:log\">" .
  "  <s>Hello, World</s>" .
  "</m:log>" .
  "</env:Body>" .
  "</env:Envelope>";

if (! $service_queue->send($request)) {
  echo "Unable to send request\n";
}

?>
