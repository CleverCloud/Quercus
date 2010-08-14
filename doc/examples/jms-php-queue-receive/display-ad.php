<table width=100%>
<tr><td align=center>
<?php

$ad_queue = java_bean("AdQueue");
$control_queue = java_bean("ControlQueue");

if (! $ad_queue) {
  echo "Unable to get ad queue!\n";
} elseif (! $control_queue) {
  echo "Unable to get control queue!\n";
} else {
  $ad = $ad_queue->poll();

  if ($ad == null) {
    echo "No ads available on the queue\n";
  } else {
    echo "$ad";
  }

  if (! $control_queue->offer(0)) {
    echo "Unable to send control message\n";
  }
}

?>
</td></tr>
<tr><td align=center>Content</td></tr>
</table>
