<?php

function server_form()
{
}

function server_print()
{
  echo "\n";
  echo "    <server-default>\n";
  echo "      <http port='8080'/>\n";
  echo "    </server-default>\n";
  echo "\n";
  echo "    <server id='' address='localhost' port='6800'/>\n"
}

?>