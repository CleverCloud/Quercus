<?php

function bam_message($to, $from, $value)
{
  $service = java_bean("bam-java-queue");

  $msg = $value->body;
  $service->addMessage($msg . " from=$from (php)");
}

bam_dispatch();

?>
