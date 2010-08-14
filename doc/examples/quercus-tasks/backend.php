<?php
function bam_message($to, $from, $value)
{
  resin_debug("bam_message($to,$from,$value)");

  $doc = new DOMDocument();
  $doc->Load("rss.xml");
  $xpath = new DomXPath($doc);

  $items = $xpath->query("rss/channel/item");

  if ($items->length == 0) {
    $task_number = 1;
  }
  else {
    // take the task number of the last task from its guid
    $guid = $xpath->query("rss/channel/item/guid");
    $task_number = explode("#", $guid->item(0)->nodeValue)[1] + 1;
  }

  // Create the new item 
  $new_item = $doc->createElement("item");
  $new_item->appendChild($doc->createElement("title", "Task $task_number"));
  $new_item->appendChild($doc->createElement("link", "http://www.caucho.com/"));
  $new_item->appendChild($doc->createElement("description", "Task $task_number ($value)"));
  $new_item->appendChild($doc->createElement("pubDate", date("D, j M Y H:i:s e")));
  $new_item->appendChild($doc->createElement("guid", "http://www.caucho.com/quercus-tasks#$task_number"));

  // Insert the new item
  $channel = $xpath->query("rss/channel");
  $channel->item(0)->insertBefore($new_item, $items->item(0));

  // trim off any items beyond 10
  $num_items = $items->length;
  while ($num_items > 10) {
    $num_items--;
    $channel->item(0)->removeChild($items->item($num_items));
  }

  $doc->save("rss.xml");
}

bam_dispatch();
?>
