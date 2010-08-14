<?php

if (isset($_POST["message"])) {
  $queue = java_bean("Queue");

  if (! $queue) {
    echo "Unable to get message queue!\n";
  } else {
    if ($queue->offer($_POST["message"])) {
      echo "Successfully sent message '" . $_POST["message"] . "'";
    } else {
      echo "Unable to send message '" . $_POST["message"] . "'";
    }
  }
}

// get the stored messages from the message store
$messages = java_bean("messageStore");

echo "<p>Received Messages:\n";

echo "<ol>";
foreach ($messages->getMessages() as $message) {
  echo "<li>" . htmlspecialchars($message) . "</li>\n";
}
echo "</ol>";

?>
<form method=POST action="">
  <input type="text" name="message" />
  <br />
  <input type="submit" value="Send message" />
</form>

<p>
<ul>
<li><a href="">See all messages sent so far.</a>
<li><a href="index.xtp">Back to tutorial</a>
</p>
